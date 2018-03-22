package edu.whu.pllab.buglocator.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.common.BugReport;

public class BugReportsSplitter {
	
	private static final Logger logger = LoggerFactory.getLogger(BugReportsSplitter.class);

	private HashMap<Integer, BugReport> bugReports;
	private List<HashMap<Integer, BugReport>> bugReportsMapList;
	private List<String> lastCommitIDList;
	
	private int n;
	
	public BugReportsSplitter(HashMap<Integer, BugReport> bugReports, int n) {
		this.bugReports = bugReports;
		splitBugReportsMap(n);
	}
	
	/** split bug reports map to n subMaps chronologically */
	public void splitBugReportsMap(int n) {
		logger.info("split bug reports map to " + n + " subMaps chromologically...");
		bugReportsMapList = new ArrayList<HashMap<Integer, BugReport>>();
		lastCommitIDList = new ArrayList<String>();
		for (int i = 0; i < n; i++) {
			HashMap<Integer, BugReport> subBugReportMap = new HashMap<Integer, BugReport>();
			bugReportsMapList.add(subBugReportMap);
		}
		List<BugReport> bugReportsList = new ArrayList<BugReport>(bugReports.values());
		bugReportsList.sort(new Comparator<BugReport>() {
			@Override
			public int compare(BugReport o1, BugReport o2) {
				return o1.getCommitTime().compareTo(o2.getCommitTime());
			}
		});
		int averSize = bugReportsList.size() / n;
		logger.info("Average subMaps size: " + averSize);
		int i = 0;
		int index = 0;
		HashMap<Integer, BugReport> subBugReportMap = bugReportsMapList.get(index);
		for (int j = 0; j < bugReportsList.size(); j++) {
			BugReport bugReport = bugReportsList.get(j);
			subBugReportMap.put(bugReport.getBugID(), bugReport);
			i++;
			if (i > averSize && index < n) {
				lastCommitIDList.add(bugReport.getCommitID());
				i = 0;
				index++;
				subBugReportMap = bugReportsMapList.get(index);
			}
		}
	}

	public HashMap<Integer, BugReport> getBugReports() {
		return bugReports;
	}

	public void setBugReports(HashMap<Integer, BugReport> bugReports) {
		this.bugReports = bugReports;
	}

	public List<HashMap<Integer, BugReport>> getBugReportsMapList() {
		return bugReportsMapList;
	}

	public void setBugReportsMapList(List<HashMap<Integer, BugReport>> bugReportsMapList) {
		this.bugReportsMapList = bugReportsMapList;
	}
	
	public List<String> getLastCommitIDList() {
		return lastCommitIDList;
	}

	public void setLastCommitIDList(List<String> lastCommitIDList) {
		this.lastCommitIDList = lastCommitIDList;
	}

	public int getN() {
		return n;
	}

	public void setN(int n) {
		this.n = n;
	}
	
}
