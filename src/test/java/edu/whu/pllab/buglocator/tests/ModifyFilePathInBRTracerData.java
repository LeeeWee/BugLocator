package edu.whu.pllab.buglocator.tests;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeSet;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.common.SourceCode;
import edu.whu.pllab.buglocator.common.SourceCodeRepository;
import edu.whu.pllab.buglocator.vectorizer.SourceCodeTfidfVectorizer;

public class ModifyFilePathInBRTracerData {
	
	
	public static void main(String[] args) {
		String[] products = {"swt", "eclipse"};
		String[] bugFilePaths = {"D:\\data\\buglocalization\\BRTracer\\Dataset\\SWTBugRepository.xml",
								"D:\\data\\buglocalization\\BRTracer\\Dataset\\EclipseBugRepository.xml"};
		String[] sourceCodeDirs = {"D:\\data\\buglocalization\\BRTracer\\Dataset\\swt-3.1\\src",
									"D:\\data\\buglocalization\\BRTracer\\Dataset\\eclipse-3.1\\plugins"};
		
		for (int index = 0; index < products.length; index++) {
			Property property = Property.loadInstance("test");
			
			property.setProduct(products[index]);
			property.setBugFilePath(bugFilePaths[index]);
			property.setSourceCodeDir(sourceCodeDirs[index]);
			
			// initialize bugReport Repository
			BugReportRepository brRepo = new BugReportRepository();
			
			SourceCodeRepository codeRepo = new SourceCodeRepository();
			SourceCodeTfidfVectorizer codeTfidfVectorizer = new SourceCodeTfidfVectorizer(codeRepo.getSourceCodeMap());
			codeTfidfVectorizer.train();
			codeTfidfVectorizer.calculateTokensWeight(codeRepo.getSourceCodeMap());
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
						System.err.println("bug :" + bugReport.getBugID() + ", file: " + file);
					}
				}
			}
			
			new File(property.getCodeRepositoryXMLPath()).delete();
		}
		
	}
	

}
