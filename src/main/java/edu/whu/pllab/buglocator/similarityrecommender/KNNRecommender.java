package edu.whu.pllab.buglocator.similarityrecommender;

import java.util.HashMap;
import java.util.TreeMap;

import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.similarity.Similarity;

/**
 * Similarity Algorithm recommender by K-Nearest Neighbor method.
 * @author Liwei
 *
 */
public class KNNRecommender extends SimAlgoRecommender {
	
	protected static final int DEFAULT_TOP_N = 100;
	
	/** calculate similarity between two bug reports */
	protected Similarity sim; 
	/** calculate recommended result using topN similar training bug reports */
	protected int topN;
	
	public KNNRecommender(HashMap<Integer, BugReport> trainingBugReports) {
		this.trainingBugReports = trainingBugReports;
		this.sim = new Similarity();
		topN = DEFAULT_TOP_N;
	}
	
	public KNNRecommender(HashMap<Integer, BugReport> trainingBugReports, int topN) {
		this.trainingBugReports = trainingBugReports;
		this.sim = new Similarity();
		this.topN = topN;
	}
	
	@Override
	public TreeMap<Integer, Double> recommend(BugReport bugReport) {
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
