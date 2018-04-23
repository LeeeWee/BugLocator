package edu.whu.pllab.buglocator.evaluation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.ExperimentResult;
import edu.whu.pllab.buglocator.rankingmodel.IntegratedScore;

public class SimpleEvaluator {
	
	private static final Logger logger = LoggerFactory.getLogger(SimpleEvaluator.class);
	
	private String testFeaturesPath;
	
	private String predictionsPath;
	
	private List<List<IntegratedScore>> intergratedScores;
	
	private ExperimentResult experimentResult;
	
	private final int[] N = ExperimentResult.N_ARRAY;
	private int[] topN = new int[N.length];
	private double[] topNRate = new double[N.length];
	
	private Double sumOfRR = 0.0;
	private Double sumOfAP = 0.0;
	private Double MRR = 0.0;
	private Double MAP = 0.0;
	
	public SimpleEvaluator() {
		Property property = Property.getInstance();
		testFeaturesPath = property.getTestFeaturesPath();
		predictionsPath = property.getPredictionsPath();
		intergratedScores = new ArrayList<List<IntegratedScore>>();
	}
	
	public SimpleEvaluator(String testFeaturesPath, String predictionsPath) {
		this.testFeaturesPath = testFeaturesPath;
		this.predictionsPath = predictionsPath;
		intergratedScores = new ArrayList<List<IntegratedScore>>();
	}
	
	public void loadIntegratedScoreList() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(testFeaturesPath));
			String line = "";
			int currentQid = 1;
			List<IntegratedScore> integratedScoreList = new ArrayList<IntegratedScore>();
			while ((line = reader.readLine()) != null) {
				// parse line
				String[] parts = line.split(" ");
				Integer rank = Integer.parseInt(parts[0]);
				boolean isPositive = (rank > 1) ? true : false;
				int qid = Integer.parseInt(parts[1].split(":")[1]);
				double[] values = new double[8];
				for (int i = 2; i < 10; i++) {
					values[i - 2] = Double.parseDouble(parts[i].split(":")[1]);
				}
				IntegratedScore integratedScore = new IntegratedScore(isPositive, values);
				
				// store to integratedScoreList
				if (qid != currentQid) {
					if (!integratedScoreList.isEmpty()) 
						intergratedScores.add(integratedScoreList);
					integratedScoreList = new ArrayList<IntegratedScore>();
				}
				integratedScoreList.add(integratedScore);
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** load predictions result */
	public void loadPredictionsResult() {
		logger.info("Loading predictions result...");
		try {
			BufferedReader reader = new BufferedReader(new FileReader(predictionsPath));
			String line = "";
			for (List<IntegratedScore> integratedScoreList : intergratedScores) {
				for (IntegratedScore integratedScore : integratedScoreList) {
					line = reader.readLine();
					if (line == null) {
						logger.error("Predictions result doesn't match test IntegratedScores!");
						System.exit(0);
					}
					integratedScore.setIntegratedScore(Double.parseDouble(line));
				}
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		// rank predictions
		logger.info("Ranking predictions result by integratedScore...");
		for (List<IntegratedScore> integratedScoreList : intergratedScores) {
			integratedScoreList.sort(new IntegratedScore.IntegratedScoreComparator());
		}
	}
	
	public void evaluate() {
		if (intergratedScores.isEmpty()) {
			loadIntegratedScoreList();
			loadPredictionsResult();
		}
		logger.info("Evaluating...");
		ExecutorService executor = Executors.newFixedThreadPool(Property.THREAD_COUNT);
		for (List<IntegratedScore> integratedScoreList : intergratedScores) {
			Runnable worker = new WorkerThread(integratedScoreList);
			executor.execute(worker);
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
		}
		
		int testDataSize = intergratedScores.size();
		// catculate topNRate, MRR, MAP
		for (int i = 0; i < topN.length; i++) {
			topNRate[i] = (double) topN[i] / testDataSize;
		}
		MRR = sumOfRR / testDataSize;
		MAP = sumOfAP / testDataSize;
		
		experimentResult = new ExperimentResult(testDataSize, topN, topNRate, sumOfRR, MRR, sumOfAP, MAP);
		System.out.println("");
		System.out.println("=====================Experiment Result =========================");
		System.out.println(experimentResult.toString());
		System.out.println("================================================================");
		System.out.println("");
	}
	
	/** Worker calculate metrics */
	private class WorkerThread implements Runnable {

		private List<IntegratedScore> integratedScores;
		
		public WorkerThread(List<IntegratedScore> integratedScores) {
			this.integratedScores = integratedScores;
		}
		
		@Override
		public void run() {
			calculateTopN();
			calculateMRR();
			calulateMAP();
		}
		
		/** for n in N, calculate accuracy@n(top@n) */
		private void calculateTopN() {
			for (int i = 0; i < integratedScores.size(); i++) {
				if (i >= N[N.length - 1])
					break;
				IntegratedScore integratedScore = integratedScores.get(i);
				if (integratedScore.isModified()) {
					synchronized (topN) {
						for (int j = 0; j < N.length; j++) {
							int n = N[j];
							if (i < n) {
								for (int k = j; k < N.length; k++) {
									topN[k]++;
								}
								break;
							}
						}
					}
					break;
				}
			}
		}
		
		/** calculate sum Of Reciprocal Rank */
		private void calculateMRR() {
			for (int i = 0; i < integratedScores.size(); i++) {
				IntegratedScore integratedScore = integratedScores.get(i);
				if (integratedScore.isModified()) {
					synchronized(sumOfRR) {
						sumOfRR += (1.0 / (i + 1));
					}
					break;
				}
			}
		}
		
		/** calculate sumOf Average Precision */
		private void calulateMAP() {
			double AP = 0.0;
			int numberOfPositiveInstances = 0;
			for (int i = 0; i < integratedScores.size(); i++) {
				IntegratedScore integratedScore = integratedScores.get(i);
				if (integratedScore.isModified()) {
					numberOfPositiveInstances++;
				}
			}
			int numberOfFixedFiles = 0;
			double precision = 0.0;
			for (int i = 0; i < integratedScores.size(); i++) {
				IntegratedScore integratedScore = integratedScores.get(i);
				if (integratedScore.isModified()) {
					numberOfFixedFiles++;
					precision = ((double) numberOfFixedFiles) / (i + 1);
					AP += (precision / numberOfPositiveInstances);
				}
			}
			synchronized(sumOfAP) {
				sumOfAP += AP;
			}
		}
		
	}

	public String getTestFeaturesPath() {
		return testFeaturesPath;
	}

	public void setTestFeaturesPath(String testFeaturesPath) {
		this.testFeaturesPath = testFeaturesPath;
	}

	public String getPredictionsPath() {
		return predictionsPath;
	}

	public void setPredictionsPath(String predictionsPath) {
		this.predictionsPath = predictionsPath;
	}
	
	

}
