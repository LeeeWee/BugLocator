package edu.whu.pllab.buglocator.common;

import java.util.ArrayList;
import java.util.HashMap;

public class SourceCodeCorpus {
	/** content after splitting, stemming and removing stopwords */
	private String content;
	
	/** used when calculating similarity */
	private double contentNorm;
	
	/** content tokens that contains tfidf/weight info */
	private HashMap<String, TokenScore> contentTokens;

	private ArrayList<String> importedClasses;
	
	private String classPart;
	private String methodPart;
	private String variablePart;
	private String commentPart;
	
	private double classCorpusNorm;
	private double methodCorpusNorm;
	private double variableCorpusNorm;
	private double commentCorpusNorm; 
	
	private HashMap<String, TokenScore> classPartTokens;
	private HashMap<String, TokenScore> methodPartTokens;
	private HashMap<String, TokenScore> variablePartTokens;
	private HashMap<String, TokenScore> commentPartTokens;
	
	public SourceCodeCorpus() {
		this.content = "";
		this.contentNorm = 0.0;
		this.contentTokens = new HashMap<String, TokenScore>();
		
		this.classPart = "";
		this.methodPart = "";
		this.variablePart = "";
		this.commentPart = "";
		this.importedClasses = new ArrayList<String>();
		
		this.classCorpusNorm = 0.0;
		this.methodCorpusNorm = 0.0;
		this.variableCorpusNorm = 0.0;
		this.commentCorpusNorm = 0.0;
	}
	
	public SourceCodeCorpus(String content) {
		this.content = content;
		this.contentNorm = 0.0;
		this.contentTokens = new HashMap<String, TokenScore>();
		
		this.classPart = "";
		this.methodPart = "";
		this.variablePart = "";
		this.commentPart = "";
		this.importedClasses = new ArrayList<String>();
		
		this.classCorpusNorm = 0.0;
		this.methodCorpusNorm = 0.0;
		this.variableCorpusNorm = 0.0;
		this.commentCorpusNorm = 0.0;
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

	public String getClassPart() {
		return classPart;
	}

	public void setClassPart(String classPart) {
		this.classPart = classPart;
	}

	public String getMethodPart() {
		return methodPart;
	}

	public void setMethodPart(String methodPart) {
		this.methodPart = methodPart;
	}

	public String getVariablePart() {
		return variablePart;
	}

	public void setVariablePart(String variablePart) {
		this.variablePart = variablePart;
	}

	public String getCommentPart() {
		return commentPart;
	}

	public void setCommentPart(String commentPart) {
		this.commentPart = commentPart;
	}

	public ArrayList<String> getImportedClasses() {
		return importedClasses;
	}

	public void setImportedClasses(ArrayList<String> importedClasses) {
		this.importedClasses = importedClasses;
	}

	public double getClassCorpusNorm() {
		return classCorpusNorm;
	}

	public void setClassCorpusNorm(double classCorpusNorm) {
		this.classCorpusNorm = classCorpusNorm;
	}

	public double getMethodCorpusNorm() {
		return methodCorpusNorm;
	}

	public void setMethodCorpusNorm(double methodCorpusNorm) {
		this.methodCorpusNorm = methodCorpusNorm;
	}

	public double getVariableCorpusNorm() {
		return variableCorpusNorm;
	}

	public void setVariableCorpusNorm(double variableCorpusNorm) {
		this.variableCorpusNorm = variableCorpusNorm;
	}

	public double getCommentCorpusNorm() {
		return commentCorpusNorm;
	}

	public void setCommentCorpusNorm(double commentCorpusNorm) {
		this.commentCorpusNorm = commentCorpusNorm;
	}

	public HashMap<String, TokenScore> getClassPartTokens() {
		return classPartTokens;
	}

	public void setClassPartTokens(HashMap<String, TokenScore> classPartTokens) {
		this.classPartTokens = classPartTokens;
	}

	public HashMap<String, TokenScore> getMethodPartTokens() {
		return methodPartTokens;
	}

	public void setMethodPartTokens(HashMap<String, TokenScore> methodPartTokens) {
		this.methodPartTokens = methodPartTokens;
	}

	public HashMap<String, TokenScore> getVariablePartTokens() {
		return variablePartTokens;
	}

	public void setVariablePartTokens(HashMap<String, TokenScore> variablePartTokens) {
		this.variablePartTokens = variablePartTokens;
	}

	public HashMap<String, TokenScore> getCommentPartTokens() {
		return commentPartTokens;
	}

	public void setCommentPartTokens(HashMap<String, TokenScore> commentPartTokens) {
		this.commentPartTokens = commentPartTokens;
	}
	
	
}
