package edu.whu.pllab.buglocator.tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.utils.FileUtil;
import jgibblda.Estimator;
import jgibblda.Inferencer;
import jgibblda.LDACmdOption;
import jgibblda.Model;

public class LDATest {

	private static final String LDA_MODEL_DIR = "LDAModelDir";
	private static final String BR_CONTENT_PATH = "BRContent.dat";
	
	public static void main(String[] args) throws Exception {
		
//		String[] products = {"ASPECTJ", "SWT", "BIRT", "ECLIPSE_PLATFORM_UI", "TOMCAT", "JDT"};
		String[] products = {"JDT"};
		
		for (String product : products) {
			Property property = Property.loadInstance(product);
			// initialize bugReport Repository
//			BugReportRepository brRepo = new BugReportRepository();
//			List<BugReport> sortedBugReports = brRepo.getSortedBugReports();
			
			File ldaModelDir = new File(property.getWorkingDir(), LDA_MODEL_DIR);
			if (!ldaModelDir.exists())
				ldaModelDir.mkdirs();
			
			// save bug reports' contents
//			String contentFilePath = new File(ldaModelDir, BR_CONTENT_PATH).getAbsolutePath();
//			BufferedWriter writer = new BufferedWriter(new FileWriter(contentFilePath));
//			writer.write(sortedBugReports.size() + "\n");
//			for (int i = 0; i < sortedBugReports.size(); i++) {
//				writer.write(sortedBugReports.get(i).getBugReportCorpus().getContent() + "\n");
//			}
//			writer.close();
			
			// create an instance of LDACmdOption and initilize it 
			LDACmdOption ldaOption = new LDACmdOption();
			ldaOption.est = true;
			ldaOption.dir = new File(property.getWorkingDir(), LDA_MODEL_DIR).getAbsolutePath();
			ldaOption.dfile = BR_CONTENT_PATH;
			ldaOption.K = 10;
			
			//  initilize an Estimator using ldaOption and estimate
//			Estimator estimator = new Estimator();
//			estimator.init(ldaOption);
//			estimator.estimate();
			
			LDACmdOption option = new LDACmdOption();
			option.inf = true;
			option.dir = new File(property.getWorkingDir(), LDA_MODEL_DIR).getAbsolutePath();
			option.modelName = "model-final";
			option.dfile = BR_CONTENT_PATH;
			
			Inferencer inferencer = new Inferencer();
			inferencer.niters = 1;
			inferencer.init(option);
			inferencer.inference();
		}
		
	}
	
}
