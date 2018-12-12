package edu.whu.pllab.buglocator.preparedata;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.utils.FileUtil;

public class GetBugLocatorData {

	private static final Logger logger = LoggerFactory.getLogger(GetBugLocatorData.class);
	
	public static void main(String[] args) {
		String[] bugFilesPaths = new String[] {"D:\\data\\buglocation\\BugLocator\\BugLocator\\data\\AspectJBugRepository.xml",
											   "D:\\data\\buglocation\\BugLocator\\BugLocator\\data\\EclipseBugRepository.xml",
											   "D:\\data\\buglocation\\BugLocator\\BugLocator\\data\\SWTBugRepository.xml",
											   "D:\\data\\buglocation\\BugLocator\\BugLocator\\data\\ZXingBugRepository.xml"};
		String[] extendBugFilesPaths = new String[] {".\\data\\LearnToRankBugFiles\\AspectJ.xlsx",
				   									 ".\\data\\NewBugFiles\\new_JDT.xml",
				   									 ".\\data\\NewBugFiles\\new_SWT.xml"};
		String[] products = {"AspectJ", "JDT", "SWT"};
		int index = 0;
		HashMap<Integer, BugReport> bugReports = parseXML(new File(bugFilesPaths[index]));
		
		Property property = Property.loadInstance(products[index]);
		property.setBugFilePath(extendBugFilesPaths[index]);
		BugReportRepository brRepo = new BugReportRepository();
		HashMap<Integer, BugReport> extendedBugReports = brRepo.getBugReports();
		bugsCheck(bugReports, extendedBugReports);
		
		System.out.println("Number of BugReports: " + bugReports.size());
		
	}
	
	/**  load bug reports data from xml file */
	private static HashMap<Integer, BugReport> parseXML(File xmlFile) {
		logger.info("Loading bug reports data by parsing xml...");
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
						BugReport bugReport = new BugReport();
						bugReport.setBugID(Integer.parseInt(bugID));
						bugReport.setReportTime(sdf.parse(opendate));
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
	
	public static void bugsCheck(HashMap<Integer, BugReport> oldBugReports, HashMap<Integer, BugReport> extendedBugReports) {
		int n = 0;
		for (Entry<Integer, BugReport> entry : oldBugReports.entrySet()) {
			if (extendedBugReports.containsKey(entry.getKey()))
				n++;
		}
		System.out.println("size: " + oldBugReports.size() + ", contained bug reports: " + n);
	}
	
	/**
	public static void getBugLocatorData() {
		String[] products = {"AspectJ", "SWT", "Birt", "Eclipse_Platform_UI", "JDT"};
		for (String product : products) {
			logger.info("Current product: " + product);
			String newOutput = "D:\\data\\bug_reports\\buglocator_bugreports\\" + product + ".xml";
			Property property = Property.loadInstance(product);
			property.setBugFilePath("D:\\data\\bug_reports\\extended_bugreports\\new_" + product + ".xml");
			BugReportRepository brRepo = new BugReportRepository();
			List<BugReport> sortedBugReports = brRepo.getSortedBugReports();
			
			// get valid bug reports
			List<String> sourceCodeFilesList = FileUtil.getAllFiles(property.getSourceCodeDir(), ".java");
			HashSet<String> sourceCodeFilesSet = new HashSet<String>();
			int sourceCodeDirNameLength = new File(property.getSourceCodeDir()).getAbsolutePath().length();
			for (String filePath : sourceCodeFilesList) {
				String path = filePath.substring(sourceCodeDirNameLength + 1).replaceAll("\\\\", "/");
				sourceCodeFilesSet.add(path);
			}
			HashMap<Integer, BugReport> validBugReports = getValidBugReports(brRepo.getBugReports(), sourceCodeFilesSet, true);
			logger.info("Valid bug reports count:" + validBugReports.size());
			List<BugReport> sortedBugReportsList = new ArrayList<BugReport>(validBugReports.values());
			sortedBugReportsList.sort(new Comparator<BugReport>() {
				@Override
				public int compare(BugReport o1, BugReport o2) {
					return o1.getCommitTime().compareTo(o2.getCommitTime());
				}
			});
			
			BugReportRepository.saveBugReportRepoToXML(sortedBugReportsList,
					newOutput, property.getProduct());
			logger.info("Original bug reports count: " + sortedBugReports.size() + ", New bug reports count: " + sortedBugReportsList.size());
		}
	} */
	
}
