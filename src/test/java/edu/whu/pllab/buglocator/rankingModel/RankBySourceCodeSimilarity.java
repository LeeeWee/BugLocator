package edu.whu.pllab.buglocator.rankingmodel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.common.SourceCode;
import edu.whu.pllab.buglocator.common.SourceCodeRepository;
import edu.whu.pllab.buglocator.evaluation.Evaluator;
import edu.whu.pllab.buglocator.evaluation.ExperimentResult;
import edu.whu.pllab.buglocator.rankingmodel.IntegratedScore;
import edu.whu.pllab.buglocator.similarity.Similarity;
import edu.whu.pllab.buglocator.utils.BugReportsSplitter;
import edu.whu.pllab.buglocator.utils.FileUtil;
import edu.whu.pllab.buglocator.vectorizer.BugReportTfidfVectorizer;
import edu.whu.pllab.buglocator.vectorizer.SourceCodeTfidfVectorizer;


public class RankBySourceCodeSimilarity {
	
	private static final Logger logger = LoggerFactory.getLogger(RankBySourceCodeSimilarity.class);
	
	public static void main(String[] args) throws Exception {
		rankBySourceCodeSimWithoutSplit();
	}
	
	public static void rankBySourceCodeSimWithoutSplit() throws Exception {
		String[] products = {"ASPECTJ", "SWT", "BIRT", "ECLIPSE_PLATFORM_UI", "TOMCAT", "JDT"};
//		String[] products = {"TOMCAT"};
		
		for (String product : products) {
			logger.info("Current product: " + product);
			Property property = Property.loadInstance(product);
			
			/** record evaluate result */
			BufferedWriter logWriter = new BufferedWriter(new FileWriter(property.getEvaluateLogPath()));
			
			// initialize bugReport Repository
			BugReportRepository brRepo = new BugReportRepository();
//			List<BugReport> sortedBugReports = brRepo.getSortedBugReports();
			SourceCodeRepository codeRepo = new SourceCodeRepository();
//			SourceCodeRepository codeRepo = new SourceCodeRepository(sortedBugReports.get(sortedBugReports.size() - 1).getCommitID());
//			codeRepo.saveSourceCodeRepoToXML(property.getCodeRepositoryXMLPath(), property.getProduct());
			
			// get valid bug reports
//			List<String> sourceCodeFilesList = FileUtil.getAllFiles(property.getSourceCodeDir(), ".java");
//			HashSet<String> sourceCodeFilesSet = new HashSet<String>();
//			int sourceCodeDirNameLength = new File(property.getSourceCodeDir()).getAbsolutePath().length();
//			for (String filePath : sourceCodeFilesList) {
//				String path = filePath.substring(sourceCodeDirNameLength + 1).replaceAll("\\\\", "/");
//				sourceCodeFilesSet.add(path);
//			}
//			HashMap<Integer, BugReport> validBugReports = getValidBugReports(brRepo.getBugReports(), sourceCodeFilesSet);
			HashMap<Integer, BugReport> validBugReports = brRepo.getBugReports();
			logger.info("Valid bug reports count: " + validBugReports.size());
			
			// save to xml
//			codeRepo.saveSourceCodeRepoToXML(property.getCodeRepositoryXMLPath(), property.getProduct());
			
			SourceCodeTfidfVectorizer codeTfidfVectorizer = new SourceCodeTfidfVectorizer(codeRepo.getSourceCodeMap());
			codeTfidfVectorizer.setUsingOkapi(true);
			codeTfidfVectorizer.train();
			codeTfidfVectorizer.calculateTokensWeight(codeRepo.getSourceCodeMap());
			
			// calculate bug report tokens weight
			BugReportTfidfVectorizer brTfidfVectorizer = new BugReportTfidfVectorizer(codeTfidfVectorizer.getTfidf());
			brTfidfVectorizer.setUsingOkapi(true);
			brTfidfVectorizer.calculateTokensWeight(validBugReports);
			
			// all results using to evaluate
			HashMap<BugReport, List<IntegratedScore>> integratedScoresMap = new HashMap<BugReport, List<IntegratedScore>>();
			
			List<BugReport> bugReportsList = new ArrayList<BugReport>(validBugReports.values());
			
			
			for (int i = 0; i < bugReportsList.size(); i++) {
				BugReport bugReport = bugReportsList.get(i);
				
				List<IntegratedScore> integratedScores = calculateVSMSimilarity(bugReport, codeRepo.getSourceCodeMap());
				
				for (int j = 0; j < integratedScores.size(); j++) {
					IntegratedScore integratedScore = integratedScores.get(j);
					if (bugReport.isModified(integratedScore.getPath())) {
						StringBuilder builder = new StringBuilder();
						builder.append("BugID: " + bugReport.getBugID() + "\n");
						builder.append("\tTotal java files: " + codeRepo.getSourceCodeMap().size() + "\n");
						builder.append("\tFirst hit: " + j + " " + integratedScore.getPath() + " "
								+ integratedScore.getIntegratedScore() + "\n");
//						System.out.println(builder.toString());
						logWriter.write(builder.toString());
						logWriter.flush();
						break;
					}
				}
				integratedScoresMap.put(bugReport, integratedScores);
			}
			Evaluator evaluator = new Evaluator(integratedScoresMap);
			evaluator.evaluate();
			
			logWriter.write(evaluator.getExperimentResult().toString() + "\n\n");
			logWriter.close();
		}
	}
	
	public static void rankBySourceCodeSimTest() throws Exception {
		String[] products = {"ASPECTJ", "SWT", "BIRT", "ECLIPSE_PLATFORM_UI", "TOMCAT", "JDT"};
//		String[] products = {"JDT"};
		
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
	
	public static HashMap<Integer, BugReport> getValidBugReports(HashMap<Integer, BugReport> bugReports,
			HashSet<String> sourceCodeFiles) {
		logger.info("Total bug reports count: " + bugReports.size());
		HashMap<Integer, BugReport> validBugReports = new HashMap<Integer, BugReport>();
		for (Entry<Integer, BugReport> entry : bugReports.entrySet()) {
			BugReport bugReport = entry.getValue();
			boolean allFixedFilesExist = true;
			for (String fixedFile : bugReport.getFixedFiles()) {
				if (!sourceCodeFiles.contains(fixedFile)) 
					allFixedFilesExist = false;
			}
			if (allFixedFilesExist) 
				validBugReports.put(entry.getKey(), entry.getValue());
		}
		return validBugReports;
	}
	
	/** reset source code repository to given commitID version */
	public static void resetSourceCodeRepository(String sourceCodeDir, String version) {
		try {
			// initialize git repository
			Repository repo = FileRepositoryBuilder.create(new File(sourceCodeDir, ".git"));
			Git git = new Git(repo);
			git.reset().setMode(ResetType.HARD).setRef(version).call();
			git.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static List<IntegratedScore> calculateVSMSimilarity(BugReport bugReport,
			HashMap<String, SourceCode> sourceCodeMap) {
		List<IntegratedScore> integratedScoreList = new ArrayList<IntegratedScore>();
		for (Entry<String, SourceCode> entry : sourceCodeMap.entrySet()) {
//			double similarity = Similarity.similarity(bugReport, entry.getValue(), Similarity.VSM);
//			double similarity = Similarity.structuralSimilarity(bugReport, entry.getValue());
//			similarity *= entry.getValue().getLengthScore();
			
			double similarity = Similarity.BM25StructuralSimilarity(bugReport, entry.getValue());
			
//			for (Method method : entry.getValue().getMethodList()) {
//				double methodSimilarity = Similarity.similarity(bugReport, method, Similarity.VSM);
//				if (methodSimilarity > similarity)
//					similarity = methodSimilarity;
//			}
			IntegratedScore score = new IntegratedScore(entry.getKey(), false, null);
			score.setIntegratedScore(similarity);
			integratedScoreList.add(score);
		}
		integratedScoreList.sort(new IntegratedScore.IntegratedScoreComparator());
		return integratedScoreList;
	}
	
}
