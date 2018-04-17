package edu.whu.pllab.buglocator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.common.SourceCode;
import edu.whu.pllab.buglocator.common.SourceCodeRepository;
import edu.whu.pllab.buglocator.evaluation.Evaluator;
import edu.whu.pllab.buglocator.rankingmodel.IntegratedScore;
import edu.whu.pllab.buglocator.similarity.Similarity;
import edu.whu.pllab.buglocator.vectorizer.BugReportTfidfVectorizer;
import edu.whu.pllab.buglocator.vectorizer.SourceCodeTfidfVectorizer;

public class VSMRank {
	
	private static final String INVALID_BUG_REPORT_PATH = "invalidBugReports.txt";
	
	public static void main(String[] args) throws Exception {
		
		String[] products = {"ASPECTJ", "SWT", "BIRT", "ECLIPSE_PLATFORM_UI", "TOMCAT", "JDT"};
//		String[] products = {"ECLIPSE_PLATFORM_UI"};
		
		for (String product : products) {
			Property property = Property.loadInstance(product);
			property.printValues();
			
			/** record evaluate result */
			BufferedWriter logWriter = new BufferedWriter(new FileWriter(property.getEvaluateLogPath()));
			
			BufferedWriter invalidBugReportsWriter = new BufferedWriter(new FileWriter(new File(property.getWorkingDir(), INVALID_BUG_REPORT_PATH)));
			
			// initialize bugReport Repository
			BugReportRepository brRepo = new BugReportRepository();
			
			// sort bug reports by commit time
			List<BugReport> bugReportsList = brRepo.getSortedBugReports();
			
			String earliestCommitID = bugReportsList.get(0).getCommitID();
			
			// reset source code repository to the earliestCommitID~ version, and train tfidf model
			// reset to given commitID version
			try {
				// initialize git repository
				Repository repo = FileRepositoryBuilder.create(new File(property.getSourceCodeDir(), ".git"));
				Git git = new Git(repo);
				git.reset().setMode(ResetType.HARD).setRef(earliestCommitID + "~").call();
				git.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			SourceCodeRepository codeRepo = new SourceCodeRepository(); 
//			SourceCodeRepository codeRepo = new SourceCodeRepository(earliestCommitID + "~");
			SourceCodeTfidfVectorizer codeTfidfVectorizer = new SourceCodeTfidfVectorizer(codeRepo.getSourceCodeMap());
			codeTfidfVectorizer.train();
			codeTfidfVectorizer.calculateTokensWeight(codeRepo.getSourceCodeMap());
			
			// all results using to evaluate
			HashMap<BugReport, List<IntegratedScore>> integratedScoresMap = new HashMap<BugReport, List<IntegratedScore>>();
			
			for (int i = 0; i < bugReportsList.size(); i++) {
				BugReport bugReport = bugReportsList.get(i);
				
				// when i > 0, update code repository
				if (i != 0) {
					String commitID = bugReport.getCommitID();
					codeRepo.checkout(commitID + "~");
					// update tfidf model
					codeTfidfVectorizer.update(codeRepo.getAddedFiles(), codeRepo.getModifiedFiles(), codeRepo.getDeletedFiles());
					// re-calculate code tokens weight 
					codeTfidfVectorizer.calculateTokensWeight(codeRepo.getSourceCodeMap());
				}
				
				// calculate bug report tokens weight
				BugReportTfidfVectorizer brTfidfVectorizer = new BugReportTfidfVectorizer(codeTfidfVectorizer.getTfidf());
				brTfidfVectorizer.calculateTokensWeight(bugReport);
				
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
						System.out.println(builder.toString());
						logWriter.write(builder.toString());
						logWriter.flush();
						break;
					}
				}
				if (!isValid) {
					invalidBugReportsWriter.write(bugReport.getBugID() + "\n");
				}
				integratedScoresMap.put(bugReport, integratedScores);
			}
			invalidBugReportsWriter.close();
			
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
			double similarity = Similarity.similarity(bugReport, entry.getValue(), Similarity.VSM);
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
