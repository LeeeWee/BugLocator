package edu.whu.pllab.buglocator.rankingmodel;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.common.SimilarBugReport;
import edu.whu.pllab.buglocator.common.SourceCode;
import edu.whu.pllab.buglocator.common.SourceCodeRepository;
import edu.whu.pllab.buglocator.evaluation.Evaluator;
import edu.whu.pllab.buglocator.evaluation.ExperimentResult;
import edu.whu.pllab.buglocator.rankingmodel.IntegratedScore;
import edu.whu.pllab.buglocator.similarity.Similarity;
import edu.whu.pllab.buglocator.utils.BugReportsSplitter;
import edu.whu.pllab.buglocator.vectorizer.BugReportTfidfVectorizer;

public class RankByCollaborativeFilteringScore {
	
	public static final int TOP_SIMILAR_BUG_REPORTS = 0;
	public static final int BR_BR_SIMILARITY = Similarity.VSM;
	
	private static HashMap<Integer, BugReport> trainingBugReports;
	private static HashMap<Integer, BugReport> testBugReports;

	private static final Logger logger = LoggerFactory.getLogger(RankBySourceCodeSimilarity.class);
	
	public static void main(String[] args) throws Exception {
		
		Property property = Property.loadInstance();
		
		/** record evaluate result */
		BufferedWriter logWriter = new BufferedWriter(new FileWriter(property.getEvaluateLogPath()));
		
		/** keep experiment result on each fold */
		List<ExperimentResult> experimentResultList = new ArrayList<ExperimentResult>();
		
		// initialize bugReport Repository
		BugReportRepository brRepo = new BugReportRepository();
		
		// split bug reports 
		BugReportsSplitter splitter = new BugReportsSplitter(brRepo.getBugReports(), 10);
		List<HashMap<Integer, BugReport>> bugReportsMapList = splitter.getBugReportsMapList();
		List<String> preCommitIDList = splitter.getPreCommitIDList(); // last committed bug report's commitID for each bug reports map 
		
		// train on the k fold and test on the k+1 fold, for k < n, n is folds total number
		for (int i = 0; i < bugReportsMapList.size() - 1; i++) {
			logger.info(String.format("Training on %d-th fold, test on %d-th fold", i, i+1));
			trainingBugReports = bugReportsMapList.get(i);
			testBugReports = bugReportsMapList.get(i + 1);
			
			// reset source code repository to the i-th lastCommitID version, and train tfidf model
//			SourceCodeRepository codeRepo = new SourceCodeRepository(preCommitIDList.get(i));
//			SourceCodeTfidfVectorizer codeVectorizer = new SourceCodeTfidfVectorizer(codeRepo.getSourceCodeMaps());
//			codeVectorizer.train();
//			codeVectorizer.calculateTokensWeight(codeRepo.getSourceCodeMaps());
			
			// train tfidf model using training bug reports
			BugReportTfidfVectorizer brVectorizer = new BugReportTfidfVectorizer(trainingBugReports);
			brVectorizer.train();
			// TfidfVectorizer training and test bug reports
			brVectorizer.calculateTokensWeight(trainingBugReports);
			brVectorizer.calculateTokensWeight(testBugReports);
			
			SourceCodeRepository codeRepo = new SourceCodeRepository(preCommitIDList.get(i+1));
			filterBugReports(testBugReports, codeRepo.getSourceCodeMap());
			
			HashMap<BugReport, List<IntegratedScore>> testIntegratedScores = sortByCollaborativeFilteringScore();
			
			Evaluator evaluator = new Evaluator(testIntegratedScores);
			evaluator.evaluate();
			
			experimentResultList.add(evaluator.getExperimentResult());
			logWriter.write(String.format("Trained on %d-th fold, test on %d-th fold:", i, i+1) + "\n");
			logWriter.write(evaluator.getExperimentResult().toString() + "\n\n");
			logWriter.flush();
		}
		
		// pool the bug reports from all test folds and compute the overall system performance
		int testDataSize = 0;
		int[] topN = new int[ExperimentResult.N_ARRAY.length];
		double sumOfRR = 0.0;
		double sumOfAP = 0.0;
		for (ExperimentResult result : experimentResultList) {
			testDataSize += result.getTestDataSize();
			for (int i = 0; i < result.getTopN().length; i++) {
				topN[i] += result.getTopN()[i];
			}
			sumOfRR += result.getSumOfRR();
			sumOfAP += result.getSumOfAP();
		}
		double MRR = sumOfRR / testDataSize;
		double MAP = sumOfAP / testDataSize;
		double[] topNRate = new double[topN.length];
		for (int j = 0; j < topN.length; j++) {
			topNRate[j] = (double) topN[j] / testDataSize;
		}
		ExperimentResult finalResult = new ExperimentResult(testDataSize, topN, topNRate, sumOfRR, MRR, sumOfAP, MAP);
		
		StringBuilder builder = new StringBuilder();
		builder.append("\n");
		builder.append("===================== Final Experiment Result =========================\n");
		builder.append(finalResult.toString() + "\n");
		builder.append("=======================================================================");
		
		System.out.println(builder.toString());
		logWriter.write(builder.toString());
		
		logWriter.close();
	}
	
	public static HashMap<BugReport, List<IntegratedScore>> sortByCollaborativeFilteringScore() {
		HashMap<BugReport, List<IntegratedScore>> integratedScoresMap = new HashMap<BugReport, List<IntegratedScore>>();
		for (BugReport bugReport : testBugReports.values()) {
			List<IntegratedScore> integratedScoreList = new ArrayList<IntegratedScore>();
			HashMap<String, Double> collaborativeFilteringScoreMap = calculateCollaborativeFilteringScore(bugReport);
			for (Entry<String, Double> entry : collaborativeFilteringScoreMap.entrySet()) {
				double similarity = entry.getValue();
				IntegratedScore score = new IntegratedScore(entry.getKey(), false, null);
				score.setIntegratedScore(similarity);
				integratedScoreList.add(score);
			}
			integratedScoreList.sort(new IntegratedScore.IntegratedScoreComparator());
			integratedScoresMap.put(bugReport, integratedScoreList);
		}
		return integratedScoresMap;
	}
	
	/** calculate collaborative filter score for given bug report */
	public static HashMap<String, Double> calculateCollaborativeFilteringScore(BugReport br) {
		HashMap<String, Double> collaborativeFilteringScoreMap = new HashMap<String, Double>();
		List<SimilarBugReport> similarBugReports = getSimilarBugReports(br, TOP_SIMILAR_BUG_REPORTS);
		for (SimilarBugReport similarBugReport : similarBugReports) {
			for (String fixedFile : similarBugReport.getFixedFiles()) {
				if (!collaborativeFilteringScoreMap.containsKey(fixedFile))
					collaborativeFilteringScoreMap.put(fixedFile, 0.0);
				else 
					collaborativeFilteringScoreMap.put(fixedFile,
							collaborativeFilteringScoreMap.get(fixedFile) + similarBugReport.getSimilarity());
			}
		}
		return collaborativeFilteringScoreMap;
	}
	
	/**
	 * get top similar bug report for input br, if top <= 0, get all bug reports 
	 * @param br input bug report
	 * @param top input similar bug reports' size
	 * @return list of similarBugReport.
	 */
	public static List<SimilarBugReport> getSimilarBugReports(BugReport br, int top) {
		List<SimilarBugReport> similarBugReports = new ArrayList<SimilarBugReport>();
		PriorityQueue<SimilarBugReport> heap = new PriorityQueue<SimilarBugReport>(trainingBugReports.size(),
				new SimilarBugReport.SimilarityComparator());
		for (Entry<Integer, BugReport> entry : trainingBugReports.entrySet()) {
			if (entry.getKey().equals(br.getBugID()))
				continue;
			double similarity = Similarity.similarity(br, entry.getValue(), BR_BR_SIMILARITY);
			heap.add(new SimilarBugReport(entry.getValue(), similarity));
		}
		if (top <= 0) {
			while (!heap.isEmpty()) 
				similarBugReports.add(heap.poll());
		} else {
			int left = top;
			while (left >= 0 && (!heap.isEmpty())) {
				similarBugReports.add(heap.poll());
				left--;
			}
		}
		return similarBugReports;
	}
	
	
	/** filter bug reports whose all fixed files do not exist in sourceCodeMap */
	public static void filterBugReports(HashMap<Integer, BugReport> bugReportsMap, HashMap<String, SourceCode> sourceCodeMap) {
		Iterator<Entry<Integer, BugReport>> iter = bugReportsMap.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Integer, BugReport> entry = iter.next();
			BugReport bugReport = entry.getValue();
			boolean invalid = true;
			for (String fixedFile : bugReport.getFixedFiles()) {
				if (sourceCodeMap.containsKey(fixedFile))
					invalid = false;
			}
			if (invalid) 
				iter.remove();
		}
	}
	
}
