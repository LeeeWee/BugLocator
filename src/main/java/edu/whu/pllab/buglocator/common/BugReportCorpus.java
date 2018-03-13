package edu.whu.pllab.buglocator.common;

import java.util.HashMap;

public class BugReportCorpus {
	
	/** content after splitting, stemming and removing stopwords */
	private String content; // merged by summary part and description part
	
	/** summary after splitting, stemming and removing stopwords*/
	private String summaryPart;
	/** description after splitting, stemming and removing stopwords*/
	private String descriptionPart;
	
	/** used when calculating similarity */
	private double contentNorm;
	
	/** content tokens that contains tfidf and weight info */
	private HashMap<String, TokenScore> contentTokens;
	
	public BugReportCorpus() {
		this.content = "";
		this.summaryPart = "";
		this.descriptionPart = "";
		this.contentNorm = 0.0;
		this.contentTokens = new HashMap<String, TokenScore>();
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getSummaryPart() {
		return summaryPart;
	}

	public void setSummaryPart(String summaryPart) {
		this.summaryPart = summaryPart;
	}

	public String getDescriptionPart() {
		return descriptionPart;
	}

	public void setDescriptionPart(String descriptionPart) {
		this.descriptionPart = descriptionPart;
	}

	public HashMap<String, TokenScore> getContentTokens() {
		return contentTokens;
	}

	public void setContentTokens(HashMap<String, TokenScore> contentTokens) {
		this.contentTokens = contentTokens;
	}

	public double getContentNorm() {
		return contentNorm;
	}

	public void setContentNorm(double contentNorm) {
		this.contentNorm = contentNorm;
	}

}
