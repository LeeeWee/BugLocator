package edu.whu.pllab.buglocator.similarity;

import java.io.File;
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

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.common.Method;
import edu.whu.pllab.buglocator.common.SourceCode;
import edu.whu.pllab.buglocator.common.SourceCodeRepository;
import edu.whu.pllab.buglocator.common.TokenScore.ScoreType;
import edu.whu.pllab.buglocator.evaluation.Evaluator;
import edu.whu.pllab.buglocator.rankingmodel.IntegratedScore;
import edu.whu.pllab.buglocator.vectorizer.BugReportTfidfVectorizer;
import edu.whu.pllab.buglocator.vectorizer.SourceCodeTfidfVectorizer;


public class SimilarityComparator {

	private static final Logger logger = LoggerFactory.getLogger(SimilarityComparator.class);

	static String googleWordVectorsPath = "D:\\data\\GoogleNews-vectors-negative300.bin";
	static WordVectors vec;
	
	public static void main(String[] args) throws Exception {
		
		logger.info("loading word vectors...");
		vec = WordVectorSerializer.loadStaticModel(new File(googleWordVectorsPath));
		
		String[] products = {"ASPECTJ"/*, "SWT", "BIRT", "ECLIPSE_PLATFORM_UI", "TOMCAT", "JDT"*/};
		
		for (String product : products) {
			logger.info("Current product: " + product);
			Property property = Property.loadInstance(product);
			
			// initialize bugReport repository and code repository
			BugReportRepository brRepo = new BugReportRepository();
			
			SourceCodeRepository codeRepo = new SourceCodeRepository();
			SourceCodeTfidfVectorizer codeVectorizer = new SourceCodeTfidfVectorizer(codeRepo.getSourceCodeMap());
			codeVectorizer.setUsingOkapi(false);
			codeVectorizer.setTokenScoreType(ScoreType.NTFIDF);
			codeVectorizer.train();
			codeVectorizer.calculateTokensWeight(codeRepo.getSourceCodeMap());
			
			// all results using to evaluate
			HashMap<BugReport, List<IntegratedScore>> integratedScoresMap = new HashMap<BugReport, List<IntegratedScore>>();
		
			// train tfidf model using training bug reports
			BugReportTfidfVectorizer brVectorizer = new BugReportTfidfVectorizer(codeVectorizer.getTfidf());
			// TfidfVectorizer training and test bug reports
			brVectorizer.setTokenScoreType(ScoreType.NTFIDF);
			brVectorizer.calculateTokensWeight(brRepo.getBugReports());
			
			List<BugReport> sortedBugReports = brRepo.getSortedBugReports();
			List<List<IntegratedScore>> predictResults = new ArrayList<List<IntegratedScore>>();
			
			int count = 0;
			for (BugReport bugReport : sortedBugReports) {
				count++;
				if (count % 100 == 0)
					System.out.println(count + " bug reports handled.");
				List<IntegratedScore> integratedScoreList = calculateSimilarity(bugReport, codeRepo.getSourceCodeMap(), Similarity.VSM);
				integratedScoresMap.put(bugReport, integratedScoreList);
				List<IntegratedScore> predictResult = new ArrayList<IntegratedScore>();
				for (IntegratedScore score : integratedScoreList) {
					if (bugReport.isModified(score.getPath()))
						predictResult.add(score);
				}
				predictResults.add(predictResult);
			}
			
			Evaluator evaluator = new Evaluator(integratedScoresMap);
			evaluator.evaluate();
			
			File comparisonDir = new File(property.getWorkingDir(), "SimilarityComparison");
			if (!comparisonDir.exists())
				comparisonDir.mkdirs();
			String output = Paths.get(comparisonDir.getAbsolutePath(), "STRUCTURE(WFIDF)_Result").toString();
			logger.info("Saving predict results to " + output + "...");
			
			savePredictResults(sortedBugReports, predictResults, output, "STRUCTURE(WFIDF)");
			
			System.out.println();
		}
	}
	
	
	
	public static List<IntegratedScore> calculateSimilarity(BugReport bugReport,
			HashMap<String, SourceCode> sourceCodeMap, int SimilarityType) {
		List<IntegratedScore> integratedScoreList = new ArrayList<IntegratedScore>();
		Similarity sim = new Similarity(vec);
		for (Entry<String, SourceCode> entry : sourceCodeMap.entrySet()) {
//			double similarity = sim.similarity(bugReport, entry.getValue(), Similarity.SYMMETRIC);
			double similarity = sim.structuralSimilarity(bugReport, entry.getValue());
//			double similarity = sim.similarity(bugReport, entry.getValue(), Similarity.WORDVECTORS);
			similarity *= entry.getValue().getLengthScore();
			
//			double similarity = sim.BM25StructuralSimilarity(bugReport, entry.getValue());
//			for (Method method : entry.getValue().getMethodList()) {
//				double methodSimilarity = sim.similarity(bugReport, method, Similarity.VSM);
//				if (methodSimilarity > similarity)
//					similarity = methodSimilarity;
//			}
			IntegratedScore score = new IntegratedScore(entry.getKey(), false, null);
			score.setIntegratedScore(similarity);
			integratedScoreList.add(score);
		}
		try {
			integratedScoreList.sort(new IntegratedScore.IntegratedScoreComparator());
			for (int i = 0; i < integratedScoreList.size(); i++) {
				integratedScoreList.get(i).rank = i;
			}
		} catch (Exception e) {
			System.out.println("Exception occurs when handling bug report " + bugReport.getBugID());
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
