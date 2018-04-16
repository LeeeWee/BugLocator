package edu.whu.pllab.buglocator.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.whu.pllab.buglocator.common.BugReport;

public class ParseXMLForBRTracerData {

	/**  load bug reports data from xml file */
	public static HashMap<Integer, BugReport> parseXML(File xmlFile) {
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
						String openDate = bugNode.getAttributes()
								.getNamedItem("opendate").getNodeValue();
						String fixDate = bugNode.getAttributes()
								.getNamedItem("fixdate").getNodeValue();
						BugReport bugReport = new BugReport();
						bugReport.setBugID(Integer.parseInt(bugID));
						bugReport.setReportTime(sdf.parse(openDate));
						bugReport.setCommitTime(sdf.parse(fixDate));
						for (Node node = bugNode.getFirstChild(); node != null; node = node
								.getNextSibling()) {
							if (node.getNodeType() == Node.ELEMENT_NODE) {
								if (node.getNodeName().equals("buginformation")) {
									NodeList _l = node.getChildNodes();
									for (int j = 0; j < _l.getLength(); j++) {
										Node _n = _l.item(j);
										if (_n.getNodeName().equals("summary")) {
											String summary = _n.getTextContent();
											bugReport.setSummary(summary);
										}
										if (_n.getNodeName().equals("description")) {
											String description = _n.getTextContent();
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
	
}
