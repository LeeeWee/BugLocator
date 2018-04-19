package edu.whu.pllab.buglocator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.common.ExperimentResult;
import edu.whu.pllab.buglocator.common.SourceCodeRepository;
import edu.whu.pllab.buglocator.evaluation.Evaluator;
import edu.whu.pllab.buglocator.rankingmodel.IntegratedScore;
import edu.whu.pllab.buglocator.rankingmodel.SVMRank;
import edu.whu.pllab.buglocator.rankingmodel.StructuralSimModelGenerator;
import edu.whu.pllab.buglocator.tests.SimpleEvaluate;
import edu.whu.pllab.buglocator.utils.BugReportsSplitter;
import edu.whu.pllab.buglocator.vectorizer.BugReportTfidfVectorizer;
import edu.whu.pllab.buglocator.vectorizer.SourceCodeTfidfVectorizer;

public class StructureSimilarityLR {
	
	private static final Logger logger = LoggerFactory.getLogger(BugLocator.class);
	
	public static void main(String[] args) throws Exception {
		
		String[] products = {"ASPECTJ", "SWT", "BIRT", "ECLIPSE_PLATFORM_UI", "TOMCAT", "JDT"};
		
		for (String product : products) {
			logger.info("Current product: " + product);
			Property property = Property.loadInstance(product);
			
			/** record evaluate result */
			BufferedWriter logWriter = new BufferedWriter(new FileWriter(property.getEvaluateLogPath()));
			
			/** keep experiment result on each fold */
			List<ExperimentResult> experimentResultList = new ArrayList<ExperimentResult>();
			
			// initialize bugReport repository and code repository
			BugReportRepository brRepo = new BugReportRepository();
			
			SourceCodeRepository codeRepo = new SourceCodeRepository();
			SourceCodeTfidfVectorizer codeVectorizer = new SourceCodeTfidfVectorizer(codeRepo.getSourceCodeMap());
			codeVectorizer.train();
			codeVectorizer.calculateTokensWeight(codeRepo.getSourceCodeMap());
			
			// train tfidf model using training bug reports
			BugReportTfidfVectorizer brVectorizer = new BugReportTfidfVectorizer(codeVectorizer.getTfidf());
			// TfidfVectorizer training and test bug reports
			brVectorizer.calculateTokensWeight(brRepo.getBugReports());
			
			// split bug reports 
			BugReportsSplitter splitter = new BugReportsSplitter(brRepo.getBugReports(), property.getSplitNum());
			List<HashMap<Integer, BugReport>> bugReportsMapList = splitter.getBugReportsMapList();
			
			StructuralSimModelGenerator generator = new StructuralSimModelGenerator();
			generator.setNormalize(false);
			generator.setSourceCodeMap(codeRepo.getSourceCodeMap());
			
			// train on the k fold and test on the k+1 fold, for k < n, n is folds total number
			for (int i = 0; i < bugReportsMapList.size() - 1; i++) {
				/**
				logger.info(String.format("Training on %d-th fold, test on %d-th fold", i, i + 1));
				HashMap<Integer, BugReport> trainingBugReports =  bugReportsMapList.get(i);
				HashMap<Integer, BugReport> testBugReports =  bugReportsMapList.get(i + 1);
				*/
				
				logger.info(String.format("Test on %d-th fold", i));
				HashMap<Integer, BugReport> trainingBugReports = new HashMap<Integer, BugReport>();
				for (int j = 0; j < bugReportsMapList.size(); j++) {
					if (j == i)
						continue;
					trainingBugReports.putAll(bugReportsMapList.get(j));
				}
				HashMap<Integer, BugReport> testBugReports = bugReportsMapList.get(i); 
				
				// generate training data
				generator.setTrainingBugReportsMap(trainingBugReports);
				generator.generate(true);
				generator.writeRankingFeatures(property.getTrainingFeaturesPath());
				
				// generate test data
				generator.setTestBugReportsMap(testBugReports);
				generator.generate(false);
				generator.writeRankingFeatures(property.getTestFeaturesPath());
				
				// svm rank training and predicting
				SVMRank.train(0.001);
				SVMRank.predict();
				
				// get test integratedScores
				HashMap<BugReport, List<IntegratedScore>> testIntegratedScores = generator.getFinals();
				//evaluate
				Evaluator evaluator = new Evaluator(testIntegratedScores);
				evaluator.loadPredictionsResult();
				evaluator.evaluate();
				
				experimentResultList.add(evaluator.getExperimentResult());
				
				logWriter.write(String.format("test on %d-th fold:", i) + "\n");
				logWriter.write(evaluator.getExperimentResult().toString() + "\n\n");
				logWriter.flush();
				
				SimpleEvaluate simpleEvaluate = new SimpleEvaluate(property.getSVMRankModelPath());
				logWriter.write("Evaluating on training data\n");
				System.out.println("Evaluating on training data\n");
				logWriter.write(simpleEvaluate.evaluate(property.getTrainingFeaturesPath()));
				logWriter.write("Evaluating on test data for direct adding\n");
				System.out.println("Evaluating on test data for direct adding\n");
				logWriter.write(simpleEvaluate.directAddingEvaluate(property.getTestFeaturesPath()));
				logWriter.write("Evaluating on training data for direct adding\n");
				System.out.println("Evaluating on training data for direct adding\n");
				logWriter.write(simpleEvaluate.directAddingEvaluate(property.getTrainingFeaturesPath()));
				
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
}
