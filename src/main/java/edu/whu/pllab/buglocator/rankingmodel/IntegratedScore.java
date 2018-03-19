package edu.whu.pllab.buglocator.rankingmodel;

import java.util.Comparator;

public class IntegratedScore {
	
	private final static int KEY_INDEX = 0;
	private final static double MODIFIED_SCORE = 1000000.0;
	
	private String path;
	private boolean isModified;
	private double[] features;

	public int rank;
	/** integrated score defined as weighted sum of k features */
	public double integratedScore;
	
	public IntegratedScore(String path, boolean isModified, double[] features) {
		this.path = path;
		this.isModified = isModified;
		this.features = features;
	}
	
	public double getScore() {
		if (isModified)
			return MODIFIED_SCORE + features[KEY_INDEX];
		else
			return features[KEY_INDEX];
	}
	
	public static class IntegratedScoreComparator implements Comparator<IntegratedScore> {
		
		public int compare(IntegratedScore r1, IntegratedScore r2) {
			if (r1.getScore() > r2.getScore()) 
				return -1;
			else if (r1.getScore() == r2.getScore()) 
				return 0;
			else 
				return 1;
		}
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isModified() {
		return isModified;
	}

	public void setModified(boolean isModified) {
		this.isModified = isModified;
	}

	public double[] getFeatures() {
		return features;
	}

	public void setFeatures(double[] features) {
		this.features = features;
	}

	public double getIntegratedScore() {
		return integratedScore;
	}

	public void setIntegratedScore(double integratedScore) {
		this.integratedScore = integratedScore;
	}

}
