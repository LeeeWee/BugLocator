package edu.whu.pllab.buglocator.common;

import java.util.Date;
import java.util.TreeSet;

import org.nd4j.linalg.api.ndarray.INDArray;

public class BugReport {

	private int bugID;
	private String summary;
	private String description;
	private Date reportTime;
	private String commitID;
	private Date commitTime;
	private TreeSet<String> fixedFiles;
	
	/** bug report corpus tokens info */
	private BugReportCorpus bugReportCorpus;
	
	private INDArray paragraphVector;
	
	public BugReport() {
		this.bugID = 0;
		this.summary = "";
		this.description = "";
		this.reportTime = new Date(System.currentTimeMillis());
		this.commitTime = new Date(System.currentTimeMillis());
		this.commitID = "";
		this.fixedFiles = new TreeSet<String>();
		this.bugReportCorpus = new BugReportCorpus();
	}
	
	public BugReport(int bugID, String summary, String description, Date reportTime, String commitID, Date commitTime,
			TreeSet<String> fixedFiles) {
		this.bugID = bugID;
		this.summary = summary;
		this.description = description;
		this.reportTime = reportTime;
		this.commitID = commitID;
		this.commitTime = commitTime;
		this.fixedFiles = fixedFiles;
	}

	/**
	 * returns whether fixedFiles contains given filePath
	 */
	public boolean isModified(String file) {
		if (fixedFiles.contains(file))
			return true;
		else 
			return false;
	}
	
	public int getBugID() {
		return bugID;
	}

	public void setBugID(int bugID) {
		this.bugID = bugID;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getReportTime() {
		return reportTime;
	}

	public void setReportTime(Date reportTime) {
		this.reportTime = reportTime;
	}

	public String getCommitID() {
		return commitID;
	}

	public void setCommitID(String commitID) {
		this.commitID = commitID;
	}

	public Date getCommitTime() {
		return commitTime;
	}

	public void setCommitTime(Date commitTime) {
		this.commitTime = commitTime;
	}

	public TreeSet<String> getFixedFiles() {
		return fixedFiles;
	}

	public void setFixedFiles(TreeSet<String> fixedFiles) {
		this.fixedFiles = fixedFiles;
	}

	public BugReportCorpus getBugReportCorpus() {
		return bugReportCorpus;
	}

	public void setBugReportCorpus(BugReportCorpus bugReportCorpus) {
		this.bugReportCorpus = bugReportCorpus;
	}

	public INDArray getParagraphVector() {
		return paragraphVector;
	}

	public void setParagraphVector(INDArray paragraphVector) {
		this.paragraphVector = paragraphVector;
	}

	
}
