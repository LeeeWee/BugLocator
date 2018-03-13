package edu.whu.pllab.buglocator.common;

import java.util.ArrayList;
import java.util.List;

import org.nd4j.linalg.api.ndarray.INDArray;

public class SourceCode {
	
	private String path;
	private String fullClassName;
	
	/** source code change points */
	private long[] changeHistory;
	
	/** a list of bug report related to this code file */
	private List<BugReport> relatedBugReportList;
	
	/** methods in source code */
	private List<Method> methodList;
	
	/** source code length score used in calculating similarity */
	private double lengthScore;
	
	/** source code file corpus info */
	private SourceCodeCorpus sourceCodeCorpus;

	private INDArray paragraphVector;
	
	public SourceCode() {
		this.path = "";
		this.fullClassName = "";
		this.changeHistory = new long[]{};
		this.relatedBugReportList = new ArrayList<BugReport>();
		this.methodList = new ArrayList<Method>();
		this.lengthScore = 0.0;
		this.sourceCodeCorpus = new SourceCodeCorpus();
	}
	
	public SourceCode(String path) {
		this.path = path;
		this.fullClassName = "";
		this.changeHistory = new long[]{};
		this.relatedBugReportList = new ArrayList<BugReport>();
		this.methodList = new ArrayList<Method>();
		this.lengthScore = 0.0;
		this.sourceCodeCorpus = new SourceCodeCorpus();		
	}
	
	/**
	 * Get the index of the latest time of given time.
	 * @param time Input time.
	 * @return The index of the latest time of given time.
	 */
	public int locateChangePoint(long time) {
		if (changeHistory == null || changeHistory[0] > time) 
			return -1;
		int i = 0;
		for (int index = 0; index < changeHistory.length; index++) {
			if (changeHistory[index] > time) {
				i = index - 1;
				break;
			}
		}
		return i;
	}
	
	/**
	 * Given a index of change history, get change frequency.
	 * @param index Input index of change history.
	 */
	public int countChangeFrequency(int index) {
		return index + 1;
	}
	
	/**
	 * Get change point of given index of change history.
	 * @param index Input index of change history.
	 * @return The change point of given index of change history.
	 */
	public long getChangePoint(int index) {
		return changeHistory[index];
	}
	
	
	

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getFullClassName() {
		return fullClassName;
	}

	public void setFullClassName(String fullClassName) {
		this.fullClassName = fullClassName;
	}

	public long[] getChangeHistory() {
		return changeHistory;
	}

	public void setChangeHistory(long[] changeHistory) {
		this.changeHistory = changeHistory;
	}

	public List<BugReport> getRelatedBugReportList() {
		return relatedBugReportList;
	}

	public void setRelatedBugReportList(List<BugReport> relatedBugReportList) {
		this.relatedBugReportList = relatedBugReportList;
	}

	public List<Method> getMethodList() {
		return methodList;
	}

	public void setMethodList(List<Method> methodList) {
		this.methodList = methodList;
	}

	public double getLengthScore() {
		return lengthScore;
	}

	public void setLengthScore(double lengthScore) {
		this.lengthScore = lengthScore;
	}

	public SourceCodeCorpus getSourceCodeCorpus() {
		return sourceCodeCorpus;
	}

	public void setSourceCodeCorpus(SourceCodeCorpus sourceCodeCorpus) {
		this.sourceCodeCorpus = sourceCodeCorpus;
	}

	public INDArray getParagraphVector() {
		return paragraphVector;
	}

	public void setParagraphVector(INDArray paragraphVector) {
		this.paragraphVector = paragraphVector;
	}
	
	
	
}
