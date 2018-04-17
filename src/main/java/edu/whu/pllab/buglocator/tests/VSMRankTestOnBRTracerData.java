package edu.whu.pllab.buglocator.tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.common.SourceCode;
import edu.whu.pllab.buglocator.common.SourceCodeRepository;
import edu.whu.pllab.buglocator.evaluation.Evaluator;
import edu.whu.pllab.buglocator.rankingmodel.IntegratedScore;
import edu.whu.pllab.buglocator.similarity.Similarity;
import edu.whu.pllab.buglocator.vectorizer.BugReportTfidfVectorizer;
import edu.whu.pllab.buglocator.vectorizer.SourceCodeTfidfVectorizer;

public class VSMRankTestOnBRTracerData {

	private static final String INVALID_BUG_REPORT_PATH = "invalidBugReports.txt";
	
	public static void main(String[] args) throws Exception {
		
		String[] products = {"swt", "aspectj", "eclipse"};
		String[] bugFilePaths = {"D:\\data\\buglocalization\\BRTracer\\Dataset\\new_xml\\new_SWTBugRepository.xml",
								"D:\\data\\buglocalization\\BRTracer\\Dataset\\new_xml\\new_AspectJBugRepository.xml",
								"D:\\data\\buglocalization\\BRTracer\\Dataset\\new_xml\\new_EclipseBugRepository.xml"};
		String[] sourceCodeDirs = {"D:\\data\\buglocalization\\BRTracer\\Dataset\\swt-3.1\\src",
									"D:\\data\\buglocalization\\BRTracer\\Dataset\\aspectj",
									"D:\\data\\buglocalization\\BRTracer\\Dataset\\eclipse-3.1\\plugins"};
		
		for (int index = 0; index < products.length; index++) {
			Property property = Property.loadInstance("test_" + products[index]);
			
			property.setProduct(products[index]);
			property.setBugFilePath(bugFilePaths[index]);
			property.setSourceCodeDir(sourceCodeDirs[index]);
			
			/** record evaluate result */
			BufferedWriter logWriter = new BufferedWriter(new FileWriter(property.getEvaluateLogPath()));
			
			BufferedWriter invalidBugReportsWriter = new BufferedWriter(new FileWriter(new File(property.getWorkingDir(), INVALID_BUG_REPORT_PATH)));
			
			// initialize bugReport Repository
			BugReportRepository brRepo = new BugReportRepository();
			
			SourceCodeRepository codeRepo = new SourceCodeRepository();
			// save to xml
			codeRepo.saveSourceCodeRepoToXML(property.getCodeRepositoryXMLPath(), property.getProduct());
			
			if (property.getProduct().equals("swt") || property.getProduct().equals("eclipse"))
				modifyFilesPath(brRepo, codeRepo);
			
			
			
			SourceCodeTfidfVectorizer codeTfidfVectorizer = new SourceCodeTfidfVectorizer(codeRepo.getSourceCodeMap());
			codeTfidfVectorizer.train();
			codeTfidfVectorizer.calculateTokensWeight(codeRepo.getSourceCodeMap());
			
			// calculate bug report tokens weight
			BugReportTfidfVectorizer brTfidfVectorizer = new BugReportTfidfVectorizer(codeTfidfVectorizer.getTfidf());
			brTfidfVectorizer.calculateTokensWeight(brRepo.getBugReports());
			
			// all results using to evaluate
			HashMap<BugReport, List<IntegratedScore>> integratedScoresMap = new HashMap<BugReport, List<IntegratedScore>>();
			
			List<BugReport> bugReportsList = new ArrayList<BugReport>(brRepo.getBugReports().values());
			
			int invalidCount = 0;
			for (int i = 0; i < bugReportsList.size(); i++) {
				BugReport bugReport = bugReportsList.get(i);
				
				List<IntegratedScore> integratedScores = calculateVSMSimilarity(bugReport, codeRepo.getSourceCodeMap());
				
				boolean isValid = false;
				for (int j = 0; j < integratedScores.size(); j++) {
					IntegratedScore integratedScore = integratedScores.get(j);
					if (bugReport.isModified(integratedScore.getPath())) {
						isValid = true;
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
				if (!isValid) {
					invalidBugReportsWriter.write(bugReport.getBugID() + "\n");
					invalidCount++;
				}
				integratedScoresMap.put(bugReport, integratedScores);
			}
			invalidBugReportsWriter.close();
			System.out.println("Invalid bug reports count: " + invalidCount);
			
			Evaluator evaluator = new Evaluator(integratedScoresMap);
			evaluator.evaluate();
			
			logWriter.write(evaluator.getExperimentResult().toString() + "\n\n");
			logWriter.close();
		}
		
	}
	
	public static List<IntegratedScore> calculateVSMSimilarity(BugReport bugReport,
			HashMap<String, SourceCode> sourceCodeMap) {
		List<IntegratedScore> integratedScoreList = new ArrayList<IntegratedScore>();
		for (Entry<String, SourceCode> entry : sourceCodeMap.entrySet()) {
//			double similarity = Similarity.similarity(bugReport, entry.getValue(), Similarity.VSM);
			double similarity = Similarity.structuralSimilarity(bugReport, entry.getValue());
			similarity *= entry.getValue().getLengthScore();
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
	
	public static void modifyFilesPath(BugReportRepository brRepo, SourceCodeRepository codeRepo) {
		HashMap<String, SourceCode> sourceCodeMap = codeRepo.getSourceCodeMap();
		
		HashMap<String, String> fileNamesMap = new HashMap<String, String>();
		for (Entry<String, SourceCode> entry : sourceCodeMap.entrySet()) {
			if (!entry.getValue().getFullClassName().endsWith(".java"))
				fileNamesMap.put(entry.getValue().getFullClassName() + ".java", entry.getKey());
			else 
				fileNamesMap.put(entry.getValue().getFullClassName(), entry.getKey());
		}
		
		for (BugReport bugReport : brRepo.getBugReports().values()) {
			TreeSet<String> modifiedFiles = new TreeSet<String>();
			for (String file : bugReport.getFixedFiles()) {
				if (fileNamesMap.containsKey(file)) {
					modifiedFiles.add(fileNamesMap.get(file));
				} else {
					continue;
				}
			}
			bugReport.setFixedFiles(modifiedFiles);
		}
	}
	
	
}
