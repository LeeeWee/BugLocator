package edu.whu.pllab.buglocator.common;

import java.util.HashMap;

public class SourceCodeCorpus {
	/** content after splitting, stemming and removing stopwords */
	private String content;
	
	/** used when calculating similarity */
	private double contentNorm;
	
	/** content tokens that contains tfidf/weight info */
	private HashMap<String, TokenScore> contentTokens;

	/** @SuppressWarnings("unused")
	private String classPart;
	private String methodPart;
	private String variablePart;
	private String commentPart;
	private ArrayList<String> importedClasses;
	
	private double classCorpusNorm;
	private double methodCorpusNorm;
	private double variableCorpusNorm;
	private double commentCorpusNorm; */
	
	public SourceCodeCorpus() {
		this.content = "";
		this.contentNorm = 0.0;
		this.contentTokens = new HashMap<String, TokenScore>();
	}
	
	public SourceCodeCorpus(String content) {
		this.content = content;
		this.contentNorm = 0.0;
		this.contentTokens = new HashMap<String, TokenScore>();
	}
	
	
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	
	public double getContentNorm() {
		return contentNorm;
	}
	
	public void setContentNorm(double contentNorm) {
		this.contentNorm = contentNorm;
	}
	
	public HashMap<String, TokenScore> getContentTokens() {
		return contentTokens;
	}
	
	public void setContentTokens(HashMap<String, TokenScore> contentTokens) {
		this.contentTokens = contentTokens;
	}
	
}
