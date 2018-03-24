package edu.whu.pllab.buglocator.common;

public class TokenScore {
	
	public enum ScoreType {
		TFIDF, WFIDF, NTFIDF, LOGTFIDF
	}

	private String token;
	private double tf;
	private double idf;
	private double tokenWeight;
	
	public TokenScore() {
		this.token = "";
		this.tf = 0.0;
		this.idf = 0.0;
		this.tokenWeight = 0.0;
	}

	public TokenScore(String token, double tf, double idf, double tokenWeight) {
		this.token = token;
		this.tf = tf;
		this.idf = idf;
		this.tokenWeight = tokenWeight;
	}
	
	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public double getTf() {
		return tf;
	}

	public void setTf(double tf) {
		this.tf = tf;
	}

	public double getIdf() {
		return idf;
	}

	public void setIdf(double idf) {
		this.idf = idf;
	}

	public double getTokenWeight() {
		return tokenWeight;
	}

	public void setTokenWeight(double tokenWeight) {
		this.tokenWeight = tokenWeight;
	}
	
	
}
