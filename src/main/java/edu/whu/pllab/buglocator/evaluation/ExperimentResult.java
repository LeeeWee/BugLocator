package edu.whu.pllab.buglocator.evaluation;

import java.util.List;

public class ExperimentResult {

	public static final int[] N_ARRAY = {1, 5, 10, 15, 20, 100, 200, 300};
	
	private int testDataSize;
	
	// experiment records metrics
	private int[] topN = new int[N_ARRAY.length];
	private double[] topNRate = new double[N_ARRAY.length];
	private double sumOfRR;
	private double MRR;
	private double sumOfAP;
	private double MAP;
	
	public ExperimentResult() {
		testDataSize = 0;
		sumOfRR = 0.0;
		MRR = 0.0;
		sumOfAP = 0.0;
		MAP = 0.0;
	}
	
	public ExperimentResult(int testDataSize) {
		this.testDataSize = testDataSize;
		sumOfRR = 0.0;
		MRR = 0.0;
		sumOfAP = 0.0;
		MAP = 0.0;
	}
	
	public ExperimentResult(int testDataSize, int[] topN, double[] topNRate, double sumOfRR, double MRR, double sumOfAP,
			double MAP) {
		this.testDataSize = testDataSize;
		this.topN = topN;
		this.topNRate = topNRate;
		this.sumOfRR = sumOfRR;
		this.MRR = MRR;
		this.sumOfAP = sumOfAP;
		this.MAP = MAP;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < N_ARRAY.length; i++) {
			builder.append(String.format("Top%d: %d / %d = %f \n", N_ARRAY[i], topN[i], testDataSize, topNRate[i]));
		}
		builder.append(String.format("MAP: %f \n", MAP));
		builder.append(String.format("MRR: %f", MRR));
		return builder.toString();
	}
	
	
	// pool the bug reports from all test folds and compute the overall system performance
	public static ExperimentResult pollExperimentResult(List<ExperimentResult> experimentResultList) {
		int testDataSize = 0;
		int[] topN = new int[ExperimentResult.N_ARRAY.length];
		double sumOfRR = 0.0;
		double sumOfAP = 0.0;
		for (ExperimentResult result : experimentResultList) {
			testDataSize += result.getTestDataSize();
			for (int i = 0; i < result.getTopN().length; i++) {
				topN[i] += result.getTopN()[i];
			}
			sumOfRR += result.getSumOfRR();
			sumOfAP += result.getSumOfAP();
		}
		double MRR = sumOfRR / testDataSize;
		double MAP = sumOfAP / testDataSize;
		double[] topNRate = new double[topN.length];
		for (int j = 0; j < topN.length; j++) {
			topNRate[j] = (double) topN[j] / testDataSize;
		}
		ExperimentResult finalResult = new ExperimentResult(testDataSize, topN, topNRate, sumOfRR, MRR, sumOfAP, MAP);
		return finalResult;
	}
	
	public int getTestDataSize() {
		return testDataSize;
	}
	
	public void setTestDataSize(int testDataSize) {
		this.testDataSize = testDataSize;
	}
	
	public int[] getTopN() {
		return topN;
	}
	
	public void setTopN(int[] topN) {
		this.topN = topN;
	}
	
	public double[] getTopNRate() {
		return topNRate;
	}
	
	public void setTopNRate(double[] topNRate) {
		this.topNRate = topNRate;
	}
	
	public double getSumOfRR() {
		return sumOfRR;
	}
	
	public void setSumOfRR(double sumOfRR) {
		this.sumOfRR = sumOfRR;
	}
	
	public double getMRR() {
		return MRR;
	}
	
	public void setMRR(double mRR) {
		MRR = mRR;
	}
	
	public double getSumOfAP() {
		return sumOfAP;
	}
	
	public void setSumOfAP(double sumOfAP) {
		this.sumOfAP = sumOfAP;
	}
	
	public double getMAP() {
		return MAP;
	}
	
	public void setMAP(double mAP) {
		MAP = mAP;
	}
	
}
