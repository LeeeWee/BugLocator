package edu.whu.pllab.buglocator.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeSet;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.Property;

public class BugReportRepository {
	
	private static final Logger logger = LoggerFactory.getLogger(BugReportRepository.class);
	
	/** all bug reports in bug repository */
	private HashMap<Integer, BugReport> bugReports;
	
	public BugReportRepository() {
		Property property = Property.getInstance();
		String brExcelPath = property.getBugFilePath();
		bugReports = parseExcel(new File(brExcelPath));
	}
	
	/** get BugReportsMap */
	public HashMap<Integer, BugReport> get() {
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
	        	String summart = cellValue2String(row.getCell(2));
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
	        	BugReport bugReport = new BugReport(bugID, summart, description, reportTime, commitID, commitTime, fixedFiles);
	        	bugReports.put(bugID, bugReport);
	        }
	        logger.info("Finished parsing, total " + bugReports.size() + " bug reports.");
	        wb.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bugReports;
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
	
	/** split string and get files */
	private TreeSet<String> extractFilesFromString(String filesString) {
		TreeSet<String> filesList = new TreeSet<String>();
		String[] files = filesString.split("\\.java ");
		for (int i = 0; i < files.length; i++) {
				filesList.add(files[i].trim() + ".java");
		}
		return filesList;
	}
		
}
