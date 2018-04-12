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
	
	/** calculate similarity with Structured Information Retrieval */
	private double summaryNorm;
	
	private double descriptionNorm;
	
	/** content tokens that contains tfidf and weight info */
	private HashMap<String, TokenScore> contentTokens;
	
	private HashMap<String, TokenScore> summartTokens;
	
	private HashMap<String, TokenScore> descriptionTokens;
	
	public BugReportCorpus() {
		this.content = "";
		this.summaryPart = "";
		this.descriptionPart = "";
		this.contentNorm = 0.0;
		this.summaryNorm = 0.0;
		this.descriptionNorm = 0.0;
		this.contentTokens = new HashMap<String, TokenScore>();
		this.summartTokens = new HashMap<String, TokenScore>();
		this.descriptionTokens = new HashMap<String, TokenScore>();
	}
	
	public BugReportCorpus(String summaryPart, String descriptionPart) {
		this.content = "";
		this.summaryPart = summaryPart;
		this.descriptionPart = descriptionPart;
		this.contentNorm = 0.0;
		this.summaryNorm = 0.0;
		this.descriptionNorm = 0.0;
		this.contentTokens = new HashMap<String, TokenScore>();
		this.summartTokens = new HashMap<String, TokenScore>();
		this.descriptionTokens = new HashMap<String, TokenScore>();
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

	public double getSummaryNorm() {
		return summaryNorm;
	}

	public void setSummaryNorm(double summaryNorm) {
		this.summaryNorm = summaryNorm;
	}

	public double getDescriptionNorm() {
		return descriptionNorm;
	}

	public void setDescriptionNorm(double descriptionNorm) {
		this.descriptionNorm = descriptionNorm;
	}

	public HashMap<String, TokenScore> getSummartTokens() {
		return summartTokens;
	}

	public void setSummartTokens(HashMap<String, TokenScore> summartTokens) {
		this.summartTokens = summartTokens;
	}

	public HashMap<String, TokenScore> getDescriptionTokens() {
		return descriptionTokens;
	}

	public void setDescriptionTokens(HashMap<String, TokenScore> descriptionTokens) {
		this.descriptionTokens = descriptionTokens;
	}
	

}
