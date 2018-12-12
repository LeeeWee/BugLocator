package edu.whu.pllab.buglocator.similarityrecommender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class SimilaritySelector {
	
	private static final Logger logger = LoggerFactory.getLogger(SimilaritySelector.class);
	
	public static void main(String[] args) throws IOException {
		locateBySelectedSimilarityForAllProducts();
	}
	
	public static void locateBySelectedSimilarityForAllProducts() throws IOException {
		String[] products = {"ASPECTJ", "SWT", "BIRT", "ECLIPSE_PLATFORM_UI", "TOMCAT", "JDT"};
		
		for (String product : products) {
			locateBySelectedSimilarity(product);
		}
	}
	
	public static void locateBySelectedSimilarity(String product) throws IOException {
		logger.info("Locating for product: " + product);
		Property property = Property.loadInstance(product);
		// initialize bugReport repository 
		BugReportRepository brRepo = new BugReportRepository();
		
		// initialize code repository
		SourceCodeRepository codeRepo = new SourceCodeRepository();
		
		File comparisonDir = new File(property.getWorkingDir(), RecommenderProperty.COMPARISON_DIR);
		HashMap<Integer, Integer> selections = loadSimilaritySelections(
				new File(comparisonDir, RecommenderProperty.COMPARISON_RESULT).getAbsolutePath());
		
		// integratedScoresMap for evaluation
		HashMap<BugReport, List<IntegratedScore>> integratedScoresMap = new HashMap<BugReport, List<IntegratedScore>>();
		// sort bug reports by commit time
		List<BugReport> sortedBugReports = brRepo.getSortedBugReports();
		// predict result for each bug report
		List<List<IntegratedScore>> predictResults = new ArrayList<List<IntegratedScore>>();
		
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
			
			int count = 0;
			for (BugReport bugReport : sortedBugReports) {
				int similarityType = selections.get(bugReport.getBugID());
				if (RecommenderProperty.getFirstIndex(similarityType) != i) 
					continue;
				else {
					count++;
					if (count % 100 == 0)
						logger.info(count + " bug reports handled.");
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
			}
		}
		
		// evaluate 
		Evaluator evaluator = new Evaluator(integratedScoresMap);
		evaluator.evaluate();
	}
	
	// load similarity selections from selections result
	public static HashMap<Integer, Integer> loadSimilaritySelections(String selectionPath) throws IOException {
		HashMap<Integer, Integer> selections = new HashMap<Integer, Integer>();
		BufferedReader reader = new BufferedReader(new FileReader(selectionPath));
		String line;
		while ((line = reader.readLine()) != null) {
			String[] pairs = line.split(":");
			int bugId = Integer.parseInt(pairs[0]);
			String similarity = pairs[1].trim().split(" ")[0];
			int similarityIntValue = 0;
			// get similarity integer value in SelectorProperty
			for (int i = 0; i < RecommenderProperty.similarities.length; i++) {
				if (RecommenderProperty.similaritiesNames[i].equals(similarity)) {
					similarityIntValue = RecommenderProperty.similarities[i];
				}
			}
			selections.put(bugId, similarityIntValue);
		}
		reader.close();
		return selections;
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

}
