package edu.whu.pllab.buglocator.similarityrecommender;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.common.SourceCode;
import edu.whu.pllab.buglocator.common.SourceCodeRepository;
import edu.whu.pllab.buglocator.common.TokenScore.ScoreType;
import edu.whu.pllab.buglocator.evaluation.Evaluator;
import edu.whu.pllab.buglocator.rankingmodel.IntegratedScore;
import edu.whu.pllab.buglocator.similarity.Similarity;
import edu.whu.pllab.buglocator.vectorizer.BugReportTfidfVectorizer;
import edu.whu.pllab.buglocator.vectorizer.SourceCodeTfidfVectorizer;

public class MultiLocator {
	
	private static final Logger logger = LoggerFactory.getLogger(MultiLocator.class);
	
	public static void main(String[] args) throws IOException {
		locateForAllProducts();
	}
	
	public static void locateForAllProducts() throws IOException {
		String[] products = {"ASPECTJ", "SWT", "BIRT", "ECLIPSE_PLATFORM_UI", "TOMCAT", "JDT"};
		
		for (String product : products) {
			locate(product);
		}
	}
	
	public static void locate(String product) throws IOException {
		logger.info("Locating for product: " + product);
		Property property = Property.loadInstance(product);
		
		// make directory for similarity comparison
		File comparisonDir = new File(property.getWorkingDir(), RecommenderProperty.COMPARISON_DIR);
		if (!comparisonDir.exists())
			comparisonDir.mkdirs();
		
		// initialize bugReport repository 
		BugReportRepository brRepo = new BugReportRepository();
		
		// initialize code repository
		SourceCodeRepository codeRepo = new SourceCodeRepository();
		
		// write evaluation result of localization
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(comparisonDir, RecommenderProperty.LOCALIZATION_RESULT)));
		
		ScoreType[] scoreTypes = RecommenderProperty.scoreTypes;
		for (int i = 0; i < scoreTypes.length; i++) {
			ScoreType scoreType = scoreTypes[i];
			// code tfidf vectorizer
			SourceCodeTfidfVectorizer codeVectorizer = new SourceCodeTfidfVectorizer(codeRepo.getSourceCodeMap());
			codeVectorizer.setTokenScoreType(scoreType);
			codeVectorizer.train();
			codeVectorizer.calculateTokensWeight(codeRepo.getSourceCodeMap());
			
			// bug reports tfidf vectorizer
			BugReportTfidfVectorizer brVectorizer = new BugReportTfidfVectorizer(codeVectorizer.getTfidf());
			// TfidfVectorizer training and test bug reports
			brVectorizer.setTokenScoreType(scoreType);
			brVectorizer.calculateTokensWeight(brRepo.getBugReports());
			
			for (int j = 0; j < RecommenderProperty.classifiedSimilarities[i].length; j++) {
				int similarityType = RecommenderProperty.classifiedSimilarities[i][j];
				String similarityName = RecommenderProperty.classifiedSimilaritiesNames[i][j];
				String resultPath = RecommenderProperty.classifiedResultPaths[i][j];
				
				// integratedScoresMap for evaluation
				HashMap<BugReport, List<IntegratedScore>> integratedScoresMap = new HashMap<BugReport, List<IntegratedScore>>();
				// sort bug reports by commit time
				List<BugReport> sortedBugReports = brRepo.getSortedBugReports();
				List<List<IntegratedScore>> predictResults = new ArrayList<List<IntegratedScore>>();
				
				int count = 0;
				for (BugReport bugReport : sortedBugReports) {
					count++;
					if (count % 100 == 0)
						logger.debug(count + " bug reports handled.");
					// calculate similarities for current bug report and all source code 
					List<IntegratedScore> integratedScoreList = calculateSimilarity(bugReport, codeRepo.getSourceCodeMap(), similarityType);
					integratedScoresMap.put(bugReport, integratedScoreList);
					List<IntegratedScore> predictResult = new ArrayList<IntegratedScore>();
					for (IntegratedScore score : integratedScoreList) {
						if (bugReport.isModified(score.getPath()))
							predictResult.add(score);
					}
					predictResults.add(predictResult);
				}
				// evaluate current similarity method
				Evaluator evaluator = new Evaluator(integratedScoresMap);
				evaluator.evaluate();
				writer.write(similarityName + ":\n" + evaluator.getExperimentResult().toString() + "\n\n");
				
				String output = Paths.get(comparisonDir.getAbsolutePath(), resultPath).toString();
				logger.info("Saving predict results to " + output + "...");
				savePredictResults(sortedBugReports, predictResults, output, similarityName);
			}
		}
		
		writer.close();
	}
	
	public static List<IntegratedScore> calculateSimilarity(BugReport bugReport,
			HashMap<String, SourceCode> sourceCodeMap, int similarityType) {
		List<IntegratedScore> integratedScoreList = new ArrayList<IntegratedScore>();
		Similarity sim = new Similarity();
		for (Entry<String, SourceCode> entry : sourceCodeMap.entrySet()) {
			double similarity = 0;
			if (similarityType == RecommenderProperty.RVSM_NTFIDF || similarityType == RecommenderProperty.RVSM_WFIDF) {
				similarity = sim.similarity(bugReport, entry.getValue(), Similarity.VSM);
				similarity *= entry.getValue().getLengthScore(); // multiply length score
			} else if (similarityType == RecommenderProperty.STRUCTURE_NTFIDF || similarityType == RecommenderProperty.STRUCTURE_WFIDF) {
				similarity = sim.structuralSimilarity(bugReport, entry.getValue());
				similarity *= entry.getValue().getLengthScore();
			} else if (similarityType == RecommenderProperty.SYMMETRIC_NTFIDF || similarityType == RecommenderProperty.SYMMETRIC_WFIDF) {
				similarity = sim.similarity(bugReport, entry.getValue(), Similarity.SYMMETRIC);
				similarity *= entry.getValue().getLengthScore();
			}
			IntegratedScore score = new IntegratedScore(entry.getKey(), false, null);
			score.setIntegratedScore(similarity);
			integratedScoreList.add(score);
		}
		integratedScoreList.sort(new IntegratedScore.IntegratedScoreComparator());
		for (int i = 0; i < integratedScoreList.size(); i++) {
			integratedScoreList.get(i).rank = i;
		}
		return integratedScoreList;
	}
	
	public static void savePredictResults(List<BugReport> bugReports, List<List<IntegratedScore>> predictResults,
			String output, String similarityType) {
		logger.info("Saving bug report repository as xml to " + output + "...");
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder domBuilder = domFactory.newDocumentBuilder();
			Document doc = domBuilder.newDocument();
			
			Element root = doc.createElement("PredictResults");
			root.setAttribute("product", Property.getInstance().getProduct());
			root.setAttribute("SimilarityType", similarityType);
			doc.appendChild(root);
			for (int i = 0; i < bugReports.size(); i++) {
				BugReport bugReport = bugReports.get(i);
				List<IntegratedScore> predictResult = predictResults.get(i);
				// format result string
				String resultStr = "";
				for (IntegratedScore score : predictResult) {
					resultStr += String.valueOf(score.rank) + ":" + score.getPath() + "\n";
				}
				
				Element bugElement = doc.createElement("bug");
				
				Element idElement = doc.createElement("id");
				idElement.appendChild(doc.createTextNode(String.valueOf(i + 1)));
				bugElement.appendChild(idElement);
				
				Element bugIdElement = doc.createElement("bug_id");
				bugIdElement.appendChild(doc.createTextNode(String.valueOf(bugReport.getBugID())));
				bugElement.appendChild(bugIdElement);
				
				Element resultElement = doc.createElement("result");
				resultElement.appendChild(doc.createTextNode(resultStr.trim()));
				bugElement.appendChild(resultElement);
				
				root.appendChild(bugElement);
			}
			doc.setXmlStandalone(true);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(output));
			transformer.transform(source, result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
