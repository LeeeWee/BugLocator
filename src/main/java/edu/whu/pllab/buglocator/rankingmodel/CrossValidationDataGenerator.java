package edu.whu.pllab.buglocator.rankingmodel;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.common.SourceCodeRepository;
import edu.whu.pllab.buglocator.utils.BugReportsSplitter;
import edu.whu.pllab.buglocator.vectorizer.BugReportTfidfVectorizer;
import edu.whu.pllab.buglocator.vectorizer.SourceCodeTfidfVectorizer;

public class CrossValidationDataGenerator {

	private static final Logger logger = LoggerFactory.getLogger(CrossValidationDataGenerator.class);
	
	public static final String TRAIN_DATA_PATH = "train.dat";
	public static final String TRAINBR_INDEX_PATH = "trainBR.i";
	public static final String TEST_DATA_PATH = "test.dat";
	public static final String TESTBR_INDEX_PATH = "testBR.i";
	
	public static void StructuralSimGenerate() {
		Property property = Property.getInstance();
		
		String workingDir = property.getWorkingDir();
		File saveDir = new File(workingDir, "data_folder");
		if (!saveDir.exists())
			saveDir.mkdir();
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
		generator.setUsingOkapi(false);
		generator.setNormalize(false);
		generator.setNormalizePerBugReport(false);
		generator.setSourceCodeMap(codeRepo.getSourceCodeMap());
		
		for (int i = 0; i < bugReportsMapList.size(); i++) {
			// make i-th folder
			File folder = new File(saveDir, "folder#" + i);
			if (!folder.exists())
				folder.mkdir();
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
			generator.writeRankingFeatures(new File(folder, TRAIN_DATA_PATH).getAbsolutePath(),
					new File(folder, TRAINBR_INDEX_PATH).getAbsolutePath());
			// generate test data
			generator.setTestBugReportsMap(testBugReports);
			generator.generate(false);
			generator.writeRankingFeatures(new File(folder, TEST_DATA_PATH).getAbsolutePath(),
					new File(folder, TESTBR_INDEX_PATH).getAbsolutePath());
		}
	}
	
	public static void main(String[] args) {
		String[] products = {"ASPECTJ", "SWT", "BIRT", "ECLIPSE_PLATFORM_UI", "TOMCAT", "JDT"};
		for (String product : products) {
			Property property = Property.loadInstance(product);
			logger.info("Current product: " + property.getProduct());
			StructuralSimGenerate();
		}
	}
	
}
