package edu.whu.pllab.buglocator.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.TreeSet;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.common.SourceCode;
import edu.whu.pllab.buglocator.common.SourceCodeRepository;

public class ModifyFilePathInBRTracerData {
	
	
	public static void main(String[] args) {
		String[] products = {"SWT", "Eclipse", "ZXing", "AspectJ"};
		String[] bugFilePaths = {"D:\\data\\buglocation\\BugLocatorData\\BugReports\\SWTBugRepository.xml",
								"D:\\data\\buglocation\\BugLocatorData\\BugReports\\EclipseBugRepository.xml",
								"D:\\data\\buglocation\\BugLocatorData\\BugReports\\ZXingBugRepository.xml",
								"D:\\data\\buglocation\\BugLocatorData\\BugReports\\AspectJBugRepository.xml"};
		String[] sourceCodeDirs = {"D:\\data\\buglocation\\BugLocatorData\\SourceCode\\swt-3.1\\src",
								   "D:\\data\\buglocation\\BugLocatorData\\SourceCode\\eclipse-3.1\\plugins",
								   "D:\\data\\buglocation\\BugLocatorData\\SourceCode\\ZXing-1.6",
								   "D:\\data\\buglocation\\BugLocatorData\\SourceCode\\aspectj"};
		String[] newBugFilePaths = {"D:\\data\\buglocation\\BugLocatorData\\BugReports\\NewBugReports\\SWTBugRepository.xml",
									"D:\\data\\buglocation\\BugLocatorData\\BugReports\\NewBugReports\\EclipseBugRepository.xml",
									"D:\\data\\buglocation\\BugLocatorData\\BugReports\\NewBugReports\\ZXingBugRepository.xml",
									"D:\\data\\buglocation\\BugLocatorData\\BugReports\\NewBugReports\\AspectJBugRepository.xml"};
		
		for (int index = 3; index < products.length; index++) {
			System.out.println("Current product: " + products[index]);
			Property property = Property.loadInstance("test");
			
			property.setProduct(products[index]);
			property.setBugFilePath(bugFilePaths[index]);
			property.setSourceCodeDir(sourceCodeDirs[index]);
			
			// initialize bugReport Repository
			HashMap<Integer, BugReport> bugReports = parseXML(new File(bugFilePaths[index]));
			
			SourceCodeRepository codeRepo = new SourceCodeRepository();
			HashMap<String, SourceCode> sourceCodeMap = codeRepo.getSourceCodeMap();
			
//			modifyFilePath(bugReports, sourceCodeMap);
			
			checkFixedFilesExistance(bugReports, sourceCodeMap);
			
			BugReportRepository.saveBugReportRepoToXML(getSortedBugReports(bugReports), newBugFilePaths[index], products[index]);
		} 
		
		for (int index = 3; index < products.length; index++) {
			System.out.println("Current product: " + products[index]);
			Property property = Property.loadInstance("test");
			
			property.setProduct(products[index]);
			property.setBugFilePath(newBugFilePaths[index]);
			property.setSourceCodeDir(sourceCodeDirs[index]);
			
			BugReportRepository repo = new BugReportRepository();
		}
		
	}
	
	/**  load bug reports data from xml file */
	private static HashMap<Integer, BugReport> parseXML(File xmlFile) {
		HashMap<Integer, BugReport> bugReports = new HashMap<Integer, BugReport>();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		DocumentBuilderFactory domFactory = DocumentBuilderFactory
				.newInstance();
		try {
			DocumentBuilder domBuilder = domFactory.newDocumentBuilder();
			InputStream is = new FileInputStream(xmlFile);
			Document doc = domBuilder.parse(is);
			Element root = doc.getDocumentElement();
			NodeList bugRepository = root.getChildNodes();
			if (bugRepository != null) {
				for (int i = 0; i < bugRepository.getLength(); i++) {
					Node bugNode = bugRepository.item(i);
					if (bugNode.getNodeType() == Node.ELEMENT_NODE) {
						String bugID = bugNode.getAttributes()
								.getNamedItem("id").getNodeValue();
						String opendate = bugNode.getAttributes()
								.getNamedItem("opendate").getNodeValue();
						String fixdate = bugNode.getAttributes()
								.getNamedItem("fixdate").getNodeValue();
						BugReport bugReport = new BugReport();
						bugReport.setBugID(Integer.parseInt(bugID));
						bugReport.setReportTime(sdf.parse(opendate));
						bugReport.setCommitTime(sdf.parse(fixdate));
						bugReport.setCommitID("");
						for (Node node = bugNode.getFirstChild(); node != null; node = node
								.getNextSibling()) {
							if (node.getNodeType() == Node.ELEMENT_NODE) {
								if (node.getNodeName().equals("buginformation")) {
									NodeList _l = node.getChildNodes();
									for (int j = 0; j < _l.getLength(); j++) {
										Node _n = _l.item(j);
										if (_n.getNodeName().equals("summary")) {
											String summary  = _n.getTextContent();
											bugReport.setSummary(summary);
										}
										if (_n.getNodeName().equals("description")) {
											String description  = _n.getTextContent();
											bugReport.setDescription(description);
										}
									}
								}
								if (node.getNodeName().equals("fixedFiles")) {
									NodeList _l = node.getChildNodes();
									for (int j = 0; j < _l.getLength(); j++) {
										Node _n = _l.item(j);
										if (_n.getNodeName().equals("file")) {
											String fileName = _n.getTextContent();
											bugReport.getFixedFiles().add(fileName);
										}
									}
								}
							}
						}
						bugReports.put(bugReport.getBugID(), bugReport);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return bugReports;
	}
	
	public static void modifyFilePath(HashMap<Integer, BugReport> bugReports, HashMap<String, SourceCode> sourceCodeMap) {
		HashMap<String, String> fileNamesMap = new HashMap<String, String>();
		for (Entry<String, SourceCode> entry : sourceCodeMap.entrySet()) {
			if (!entry.getValue().getFullClassName().endsWith(".java"))
				fileNamesMap.put(entry.getValue().getFullClassName() + ".java", entry.getKey());
			else 
				fileNamesMap.put(entry.getValue().getFullClassName(), entry.getKey());
		}
		for (BugReport bugReport : bugReports.values()) {
			TreeSet<String> modifiedFiles = new TreeSet<String>();
			for (String file : bugReport.getFixedFiles()) {
				if (fileNamesMap.containsKey(file)) {
					modifiedFiles.add(fileNamesMap.get(file));
				} else {
					System.err.println("bug :" + bugReport.getBugID() + ", file: " + file);
				}
			}
			bugReport.setFixedFiles(modifiedFiles);
		}
	}
	
	public static void checkFixedFilesExistance(HashMap<Integer, BugReport> bugReports, HashMap<String, SourceCode> sourceCodeMap) {
		for (BugReport bugReport : bugReports.values()) {
			for (String file : bugReport.getFixedFiles()) {
				if (sourceCodeMap.containsKey(file)) {
					// do nothing
				} else {
					System.err.println("bug :" + bugReport.getBugID() + ", file: " + file);
				}
			}
		}
	}
	
	public static List<BugReport> getSortedBugReports(HashMap<Integer, BugReport> bugReports) {
		List<BugReport> bugReportsList = new ArrayList<BugReport>(bugReports.values());
		bugReportsList.sort(new Comparator<BugReport>() {
			@Override
			public int compare(BugReport o1, BugReport o2) {
				return o1.getCommitTime().compareTo(o2.getCommitTime());
			}
		});
		return bugReportsList;
	}
	

}
