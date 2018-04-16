package edu.whu.pllab.buglocator.rankingmodel;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.common.ExperimentResult;
import edu.whu.pllab.buglocator.common.SourceCode;
import edu.whu.pllab.buglocator.common.SourceCodeRepository;
import edu.whu.pllab.buglocator.evaluation.Evaluator;
import edu.whu.pllab.buglocator.rankingmodel.IntegratedScore;
import edu.whu.pllab.buglocator.similarity.Similarity;
import edu.whu.pllab.buglocator.utils.BugReportsSplitter;
import edu.whu.pllab.buglocator.vectorizer.BugReportTfidfVectorizer;
import edu.whu.pllab.buglocator.vectorizer.SourceCodeTfidfVectorizer;


public class RankBySourceCodeSimilarity {
	
	private static final Logger logger = LoggerFactory.getLogger(RankBySourceCodeSimilarity.class);
	
	public static void main(String[] args) throws Exception {
		
		String[] products = {"ASPECTJ", "SWT", "BIRT", "ECLIPSE_PLATFORM_UI", "TOMCAT", "JDT"};
		
		for (String product : products) {
			
			Property property = Property.loadInstance(product);
			
			/** record evaluate result */
			BufferedWriter logWriter = new BufferedWriter(new FileWriter(property.getEvaluateLogPath()));
			
			/** keep experiment result on each fold */
			List<ExperimentResult> experimentResultList = new ArrayList<ExperimentResult>();
			
			// initialize bugReport Repository
			BugReportRepository brRepo = new BugReportRepository();
			
			// split bug reports 
			BugReportsSplitter splitter = new BugReportsSplitter(brRepo.getBugReports(), property.getSplitNum());
			List<HashMap<Integer, BugReport>> bugReportsMapList = splitter.getBugReportsMapList();
			List<String> preCommitIDList = splitter.getPreCommitIDList(); 
			
			for (int i = 0; i < bugReportsMapList.size(); i++) {
				HashMap<Integer, BugReport> bugReports = bugReportsMapList.get(i);
			
				SourceCodeRepository codeRepo = new SourceCodeRepository(preCommitIDList.get(i));
				SourceCodeTfidfVectorizer codeVectorizer = new SourceCodeTfidfVectorizer(codeRepo.getSourceCodeMap());
				codeVectorizer.train();
				codeVectorizer.calculateTokensWeight(codeRepo.getSourceCodeMap());
				filterBugReports(bugReports, codeRepo.getSourceCodeMap());
				
				// train tfidf model using training bug reports
				BugReportTfidfVectorizer brVectorizer = new BugReportTfidfVectorizer(codeVectorizer.getTfidf());
				brVectorizer.calculateTokensWeight(bugReports);
				
				HashMap<BugReport, List<IntegratedScore>> testIntegratedScores = sortBySourceCodeSimilarity(bugReports,
						codeRepo.getSourceCodeMap());
				
				Evaluator evaluator = new Evaluator(testIntegratedScores);
				evaluator.evaluate();
				
				experimentResultList.add(evaluator.getExperimentResult());
				logWriter.write(String.format("test on %d-th fold:", i) + "\n");
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
	}
	
	public static HashMap<BugReport, List<IntegratedScore>> sortBySourceCodeSimilarity(
			HashMap<Integer, BugReport> testBugReports, HashMap<String, SourceCode> sourceCodeMap) {
		HashMap<BugReport, List<IntegratedScore>> integratedScoresMap = new HashMap<BugReport, List<IntegratedScore>>();
		for (BugReport bugReport : testBugReports.values()) {
			List<IntegratedScore> integratedScoreList = new ArrayList<IntegratedScore>();
			for (Entry<String, SourceCode> entry : sourceCodeMap.entrySet()) {
				double similarity = Similarity.vsmSimilarity(bugReport, entry.getValue());
				IntegratedScore score = new IntegratedScore(entry.getKey(), false, null);
				score.setIntegratedScore(similarity);
				integratedScoreList.add(score);
			}
			integratedScoreList.sort(new IntegratedScore.IntegratedScoreComparator());
			integratedScoresMap.put(bugReport, integratedScoreList);
		}
		return integratedScoresMap;
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
