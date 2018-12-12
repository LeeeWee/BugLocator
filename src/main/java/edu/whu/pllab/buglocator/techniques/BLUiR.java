package edu.whu.pllab.buglocator.techniques;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.common.SourceCode;
import edu.whu.pllab.buglocator.common.SourceCodeRepository;
import edu.whu.pllab.buglocator.similarity.Similarity;
import edu.whu.pllab.buglocator.vectorizer.BugReportTfidfVectorizer;
import edu.whu.pllab.buglocator.vectorizer.SourceCodeTfidfVectorizer;

public class BLUiR {

	private static final Logger logger = LoggerFactory.getLogger(BLUiR.class);
	
	private static Similarity sim = new Similarity();
	private static String[] products = {"BugLocator_SWT", "BugLocator_AspectJ", "BugLocator_Eclipse", "BugLocator_ZXing"};
	
	private static HashMap<String, SourceCode> sourceCodeMap;
	private static HashMap<Integer, BugReport> bugReports;
	
	public static void main(String[] args) throws Exception {
		for (String product : products) {
			logger.info("Current Product: " + product);
			locateBug(product);
		}
	}
	
	public static void locateBug(String product) throws Exception {
		Property property = Property.loadInstance(product);

		// record evaluate result
		// BufferedWriter logWriter = new BufferedWriter(new FileWriter(property.getEvaluateLogPath()));
		
		// initialize bugReport repository and source code repository
		BugReportRepository brRepo = new BugReportRepository();
		SourceCodeRepository codeRepo = new SourceCodeRepository();
		
		// calculate source code tokens weight
		SourceCodeTfidfVectorizer codeVectorizer = new SourceCodeTfidfVectorizer(codeRepo.getSourceCodeMap());
		codeVectorizer.train();
		codeVectorizer.calculateTokensWeight(codeRepo.getSourceCodeMap());
		
		// calculate bug reports tokens weight
		BugReportTfidfVectorizer brVectorizer = new BugReportTfidfVectorizer(codeVectorizer.getTfidf());
		brVectorizer.calculateTokensWeight(brRepo.getBugReports());
		
		sourceCodeMap = codeRepo.getSourceCodeMap();
		bugReports = brRepo.getBugReports();
		
//		evaluate();
	}
	
}
