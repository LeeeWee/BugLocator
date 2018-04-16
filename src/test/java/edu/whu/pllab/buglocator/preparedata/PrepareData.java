package edu.whu.pllab.buglocator.preparedata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.BugLocator;
import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.utils.BugReportsSplitter;
import edu.whu.pllab.buglocator.utils.FileUtil;

public class PrepareData {
	
	private static final Logger logger = LoggerFactory.getLogger(BugLocator.class);
	
	public static void main(String[] args) throws Exception {
		String output = "C:\\Users\\Liwei\\Desktop\\eclipse-dataset\\aspetj_diff.xml";
		Property property = Property.loadInstance("ASPECTJ");
		BugReportRepository brRepo = new BugReportRepository();
		List<BugReport>  bugReports = brRepo.getSortedBugReports();
		property.setBugFilePath("C:\\Users\\Liwei\\Desktop\\eclipse-dataset\\aspectj.xml");
		BugReportRepository newBrRepo = new BugReportRepository();
		List<BugReport> newBugReports = newBrRepo.getSortedBugReports();
		List<BugReport> diffBugReports = new ArrayList<BugReport>();
		for (BugReport newBugReport : newBugReports) {
			boolean diff = true;
			for (BugReport bugReport : bugReports) {
				if (bugReport.getBugID() == newBugReport.getBugID())
					diff = false;
			}
			if (diff)
				diffBugReports.add(newBugReport);
		}
		BugReportRepository.saveBugReportRepoToXML(diffBugReports, output, "ASPECTJ_DIFF");
		
	}
	
	public static void prepareData() {
		
	}
	
	public static void validBugReportsTest() throws Exception {
		String[] products = {"ASPECTJ", "SWT", "BIRT", "ECLIPSE_PLATFORM_UI", "TOMCAT", "JDT"};
		
		for (String product : products) {
			
			Property property = Property.loadInstance(product);
			
			// initialize bugReport Repository
			BugReportRepository brRepo = new BugReportRepository();
			
			// split bug reports 
			BugReportsSplitter splitter = new BugReportsSplitter(brRepo.getBugReports(), property.getSplitNum());
			List<HashMap<Integer, BugReport>> bugReportsMapList = splitter.getBugReportsMapList();
			List<String> preCommitIDList = splitter.getPreCommitIDList(); // last committed bug report's commitID for each bug reports map 
			HashMap<Integer, BugReport> validBugReports = new HashMap<Integer, BugReport>();
			// train on the k fold and test on the k+1 fold, for k < n, n is folds total number
			for (int i = 0; i < bugReportsMapList.size(); i++) {
				HashMap<Integer, BugReport> bugReports = bugReportsMapList.get(i);
				// reset source code repository to the i-th preCommitIDList version, and train tfidf model
				resetSourceCodeRepository(property.getSourceCodeDir(), preCommitIDList.get(i));
				List<String> sourceCodeFilesList = FileUtil.getAllFiles(property.getSourceCodeDir(), ".java");
				HashSet<String> sourceCodeFilesSet = new HashSet<String>();
				int sourceCodeDirNameLength = new File(property.getSourceCodeDir()).getAbsolutePath().length();
				for (String filePath : sourceCodeFilesList) {
					String path = filePath.substring(sourceCodeDirNameLength + 1).replaceAll("\\\\", "/");
					sourceCodeFilesSet.add(path);
				}
				logger.info(i + "-th fold:");
				HashMap<Integer, BugReport> tempValidBugReports = getValidBugReports(bugReports, sourceCodeFilesSet);
				validBugReports.putAll(tempValidBugReports);
				
				writeBugRepository(new File(property.getWorkingDir(), i + "-th validBugReports.txt").getAbsolutePath(),
						tempValidBugReports, sourceCodeFilesSet);
			}
			logger.info(property.getProduct() + " total valid bug reports count: " + validBugReports.size());
		}
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
	
	public static HashMap<Integer, BugReport> getValidBugReports(HashMap<Integer, BugReport> bugReports,
			HashSet<String> sourceCodeFiles) {
		logger.info("Total bug reports count: " + bugReports.size());
		HashMap<Integer, BugReport> validBugReports = new HashMap<Integer, BugReport>();
		int allFixedFilesExistCount = 0, noneFixedFilesExistCount = 0;
		for (Entry<Integer, BugReport> entry : bugReports.entrySet()) {
			BugReport bugReport = entry.getValue();
			boolean allFixedFilesExist = true;
			boolean noneFixedFilesExist = true;
			for (String fixedFile : bugReport.getFixedFiles()) {
				if (sourceCodeFiles.contains(fixedFile)) 
					noneFixedFilesExist = false;
				else 
					allFixedFilesExist = false;
			}
			if (allFixedFilesExist)
				allFixedFilesExistCount++;
			if (noneFixedFilesExist)
				noneFixedFilesExistCount++;
			else 
				validBugReports.put(entry.getKey(), entry.getValue());
		}
		logger.info("bug report (all fixed files exist): " + allFixedFilesExistCount);
		logger.info("bug report (none fixed files exist): " + noneFixedFilesExistCount);
		
		return validBugReports;
	}
	
	public static void writeBugRepository(String output, HashMap<Integer, BugReport> bugReports, HashSet<String> sourceCodeFilesSet) throws Exception {
		BufferedWriter writer = new BufferedWriter(new FileWriter(output));
		for (Entry<Integer, BugReport> entry : bugReports.entrySet()) {
			StringBuilder builder = new StringBuilder();
			BugReport bugReport = entry.getValue();
			builder.append("BugID: " + bugReport.getBugID() + "\n");
			builder.append("summary: " + bugReport.getSummary() + "\n");
			builder.append("description: " + bugReport.getDescription() + "\n");
			builder.append("reportTime: " + bugReport.getReportTime() + "\n");
			builder.append("commitID: " + bugReport.getCommitID() + "\t");
			builder.append("commitTime: " + bugReport.getCommitTime() + "\n");
			builder.append("corpus: " + bugReport.getBugReportCorpus().getContent() + "\n");
			builder.append("contentNorm: " + bugReport.getBugReportCorpus().getContentNorm() + "\n");
//			builder.append("contentTokens: ");
//			for (TokenScore tokenScore : bugReport.getBugReportCorpus().getContentTokens().values()) {
//				builder.append(String.format("%s(%f,%f,%f) ", tokenScore.getToken(), tokenScore.getTf(),
//						tokenScore.getIdf(), tokenScore.getTokenWeight()));
//			}
			builder.append("\nModifiedFiles: \n");
			for (String file : bugReport.getFixedFiles()) {
				if (!sourceCodeFilesSet.contains(file))
					builder.append("\t[NONEXIST] " + file + "\n");
				else
					builder.append("\t" + file + "\n");
			}
			builder.append("\n");
			writer.write(builder.toString());
		}
		writer.close();
	}

}