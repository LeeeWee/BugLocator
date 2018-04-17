package edu.whu.pllab.buglocator.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.util.TreeSet;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.utils.Splitter;
import edu.whu.pllab.buglocator.utils.Stemmer;
import edu.whu.pllab.buglocator.utils.Stopword;

public class BugReportRepository {
	
	private static final Logger logger = LoggerFactory.getLogger(BugReportRepository.class);
	
	/** all bug reports in bug repository */
	private HashMap<Integer, BugReport> bugReports;
	
	public BugReportRepository() {
		Property property = Property.getInstance();
		String brFilePath = property.getBugFilePath();
		if (brFilePath.endsWith(".xml"))
			bugReports = parseXML(new File(brFilePath));
		else 
			bugReports = parseExcel(new File(brFilePath));
		cleanText();
		saveSourceCodeChangeHistory(extractSourceCodeChangeHistory(),property.getCodeChangeHistoryPath());
	}
	
	/** get BugReportsMap */
	public HashMap<Integer, BugReport> getBugReports() {
		return bugReports;
	}

	/** get bugReport from bugReportRepository by bugID */
	public BugReport get(Integer bugID) {
		if (!bugReports.containsKey(bugID))
			return null;
		else 
			return bugReports.get(bugID);
	}
	
	/**  load bug reports data from excel file */
	public HashMap<Integer, BugReport> parseExcel(File excelFile) {
		logger.info("Loading bug reports data by parsing excel...");
		HashMap<Integer, BugReport> bugReports = new HashMap<Integer, BugReport>();
		try {
			InputStream stream = new FileInputStream(excelFile);
			Workbook wb = null;
			if (excelFile.getPath().endsWith(".xlsx")) 
				wb = new XSSFWorkbook(stream);
			else
				wb = new HSSFWorkbook(stream);
			Sheet sheet = wb.getSheetAt(0);
	        int rowCount = sheet.getPhysicalNumberOfRows();
	        for (int r = 1; r < rowCount; r++) {
	        	Row row = sheet.getRow(r);
	        	int bugID = Integer.parseInt(cellValue2String(row.getCell(1)));
	        	String summary = cellValue2String(row.getCell(2)).replaceAll("^Bug \\d+ ", "");
	        	String description = cellValue2String(row.getCell(3));
	        	String commitID = cellValue2String(row.getCell(7));
	        	// parse report date
	        	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	        	String reportTimeStr = cellValue2String(row.getCell(4));
	        	Date reportTime = sdf.parse(reportTimeStr);
	        	// parse commit date
	        	Long commitTimestamp = Long.parseLong(cellValue2String(row.getCell(8))) * 1000;
	        	Date commitTime = new Date(commitTimestamp);
	        	// extract modified files
	        	String filesString = cellValue2String(row.getCell(9));
	        	TreeSet<String> fixedFiles = extractFilesFromString(filesString);
	        	BugReport bugReport = new BugReport(bugID, summary, description, reportTime, commitID, commitTime, fixedFiles);
	        	bugReports.put(bugID, bugReport);
	        }
	        logger.info("Finished parsing, total " + bugReports.size() + " bug reports.");
	        wb.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bugReports;
	}
	
	/**  load bug reports data from xml file */
	private HashMap<Integer, BugReport> parseXML(File xmlFile) {
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
						String reportTime = bugNode.getAttributes()
								.getNamedItem("reportTime").getNodeValue();
						BugReport bugReport = new BugReport();
						bugReport.setBugID(Integer.parseInt(bugID));
						bugReport.setReportTime(sdf.parse(reportTime));
						for (Node node = bugNode.getFirstChild(); node != null; node = node
								.getNextSibling()) {
							if (node.getNodeType() == Node.ELEMENT_NODE) {
								if (node.getNodeName().equals("summary")) {
									String summary = node.getTextContent();
									bugReport.setSummary(summary);
								}
								if (node.getNodeName().equals("description")) {
									String description = node.getTextContent();
									bugReport.setDescription(description);
								}
								if (node.getNodeName().equals("commit")) {
									if (node.getAttributes().getNamedItem("id") != null) {
										String commitID = node.getAttributes().getNamedItem("id").getNodeValue();
										bugReport.setCommitID(commitID);
									}
									String commitTime = node.getAttributes()
											.getNamedItem("commitTime").getNodeValue();
									bugReport.setCommitTime(sdf.parse(commitTime));
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
	
	public void saveBugReportRepoToXML(String output) {
		String product = Property.getInstance().getProduct();
		List<BugReport> bugReportsList = getSortedBugReports();
		saveBugReportRepoToXML(bugReportsList, output, product);
	}
	
	/** save bug report repository as xml to output path  */
	public static void saveBugReportRepoToXML(List<BugReport> bugReportsList, String output, String product) {
		logger.info("Saving bug report repository as xml to " + output + "...");
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			DocumentBuilder domBuilder = domFactory.newDocumentBuilder();
			Document doc = domBuilder.newDocument();
			
			Element root = doc.createElement("BugReportRepository");
			root.setAttribute("product", product);
			doc.appendChild(root);
			for (BugReport bugReport : bugReportsList) {
				Element bugElement = doc.createElement("bug");
				bugElement.setAttribute("id", String.valueOf(bugReport.getBugID()));
				bugElement.setAttribute("reportTime", sdf.format(bugReport.getReportTime()));
				
				Element summaryElement = doc.createElement("summary");
				summaryElement.appendChild(doc.createTextNode(bugReport.getSummary()));
				bugElement.appendChild(summaryElement);
				
				Element descriptionElement = doc.createElement("description");
				descriptionElement.appendChild(doc.createTextNode(bugReport.getDescription()));
				bugElement.appendChild(descriptionElement);
				
				Element commitElement = doc.createElement("commit");
				if (!bugReport.getCommitID().isEmpty())
					commitElement.setAttribute("id", bugReport.getCommitID().substring(0, 7));
				commitElement.setAttribute("commitTime", sdf.format(bugReport.getCommitTime()));
				bugElement.appendChild(commitElement);
				
				Element fixedFilesElement = doc.createElement("fixedFiles");
				for (String fixedFile : bugReport.getFixedFiles()) {
					Element fileElement = doc.createElement("file");
					fileElement.appendChild(doc.createTextNode(fixedFile));
					fixedFilesElement.appendChild(fileElement);
				}
				bugElement.appendChild(fixedFilesElement);
				
				// append bug element to root
				root.appendChild(bugElement);
			}
			doc.setXmlStandalone(true);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(output));
			transformer.transform(source, result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** convert excel cell value to string */
	private String cellValue2String(Cell cell) {
		if (cell == null) 
			return "";
		else {
			switch (cell.getCellTypeEnum()) {   //根据cell中的类型来输出数据  
			case STRING:
				return cell.getStringCellValue();
	        case NUMERIC:  
	        	DecimalFormat df = new DecimalFormat("0");
	            return df.format(cell.getNumericCellValue()).toString();  
	        case BOOLEAN:  
	            return cell.getBooleanCellValue() ? "TRUE" : "FALSE"; 
	        case FORMULA:  
	            return cell.getCellFormula(); 
	        default:  
	            return " ";  
	        }  
		}
	}
	
	/** split filesString and get fixed files */
	private TreeSet<String> extractFilesFromString(String filesString) {
		TreeSet<String> filesList = new TreeSet<String>();
		String[] files = filesString.split("\\.java ");
		for (int i = 0; i < files.length - 1; i++) {
			filesList.add(files[i].trim() + ".java");
		}
		filesList.add(files[files.length - 1]);
		return filesList;
	}
	
	/** set bug report corpus for all bug reports */
	public void cleanText() {
		logger.info("Preprocessing bug reports' summary and description...");
		ExecutorService executor = Executors.newFixedThreadPool(Property.THREAD_COUNT);
		for (Entry<Integer, BugReport> entry : bugReports.entrySet()) {
			Runnable worker = new WorkerThread(entry.getValue());
			executor.execute(worker);			
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
		}
	}
	
	/** worker splitting and stemming bugReport summary and description ,and setting BugReportCorpus content */
	private class WorkerThread implements Runnable {
		private BugReport bugReport;
		public WorkerThread(BugReport bugReport) {
			this.bugReport = bugReport;
		}
		public void run() {
			String summaryPart = cleanText(bugReport.getSummary());
			String descriptionPart = cleanText(bugReport.getDescription());
			BugReportCorpus bugReportCorpus = new BugReportCorpus(summaryPart, descriptionPart);
			bugReportCorpus.setContent(summaryPart + " " + descriptionPart);
			bugReport.setBugReportCorpus(bugReportCorpus);
		}
	}
	
	 /** split, stem and remove stopwords for given text */
	public String cleanText(String text) {
//		String[] content = Splitter.splitNatureLanguageEx(text);
		String[] content = Splitter.splitNatureLanguage(text);
		StringBuffer contentBuf = new StringBuffer();
		for (int i = 0; i < content.length; i++) {
			String word = content[i].toLowerCase();
			if (word.length() > 0) {
				String stemWord = Stemmer.stem(word);
				if (!Stopword.isEnglishStopword(stemWord)) {
					contentBuf.append(stemWord);
					contentBuf.append(" ");
				}
			}
		}
		return contentBuf.toString().trim();
	}

	/** extract sourceCode change history points */
	public HashMap<String, List<Date>> extractSourceCodeChangeHistory() {
		logger.info("Extracting source code chang history...");
		HashMap<String, List<Date>> changeHistory = new HashMap<String, List<Date>>();
		for (BugReport br : bugReports.values()) {
			TreeSet<String> changedFiles = br.getFixedFiles();
			for (String file : changedFiles) {
				if (!changeHistory.containsKey(file)) {
					List<Date> dateList = new ArrayList<Date>();
					changeHistory.put(file, dateList);
				}
				List<Date> dateList = changeHistory.get(file);
				dateList.add(br.getCommitTime());
			}
		}
		// sort date of changeHistory
		for (List<Date> dateList : changeHistory.values()) {
			dateList.sort(new Comparator<Date>() {
				public int compare(Date date1, Date date2) {
					return date1.compareTo(date2);
				}
			});
		}
		return changeHistory;
	}
	
	/** save source code change history to local path */
	public void saveSourceCodeChangeHistory(HashMap<String, List<Date>> codeChangeHistory, String output) {
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output)));
			for (Entry<String, List<Date>> entry : codeChangeHistory.entrySet()) {
				writer.write(entry.getKey());
				for (Date date : entry.getValue()) {
					writer.write(", " + date.getTime());
				}
				writer.newLine();
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**get list of bug reports sorted by commit time */
	public List<BugReport> getSortedBugReports() {
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
