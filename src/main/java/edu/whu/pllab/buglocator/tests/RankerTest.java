package edu.whu.pllab.buglocator.tests;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import ciir.umass.edu.eval.Evaluator;
import ciir.umass.edu.features.FeatureManager;
import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.learning.RankerFactory;
import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.evaluation.SimpleEvaluator;

public class RankerTest {
	
	public static final int RANKNET = 1;
	public static final int LAMBDAMART = 6;
	
	public static final String TRAING_DATA_PATH = "train.dat";
	public static final String TEST_DATA_PATH = "test.dat";
	public static final String RANKNET_MODEL_PATH = "RankNet.model";
	public static final String LAMBDAMART_MODEL_PATH = "LambdaMART.model";
	public static final String RANKNET_PREDICTIONS_PATH = "RankNet.predictions";
	public static final String LAMBDAMART_PREDICTIONS_PATH = "LambdaMART.predictions";
	
	
	private int rankerType; 
	
	private String workingDir;
	private String trainingFeaturesPath;
	private String testFeaturesPath;
	private String savedModelPath;
	private String predictionsPath;
	
	public RankerTest(int rankerType) {
		this.rankerType = rankerType;
		Property property = Property.getInstance();
		workingDir = property.getWorkingDir();
		trainingFeaturesPath = property.getTrainingFeaturesPath();
		testFeaturesPath = property.getTestFeaturesPath();
		setModelPredictionsPath();
	}
	
	public RankerTest(int rankerType, String workingDir) {
		this.rankerType = rankerType;
		this.workingDir = workingDir;
		trainingFeaturesPath = new File(workingDir, TRAING_DATA_PATH).getAbsolutePath();
		testFeaturesPath = new File(workingDir, TEST_DATA_PATH).getAbsolutePath();
		setModelPredictionsPath();
	}
	
	public void setModelPredictionsPath() {
		switch (rankerType) {
		case RANKNET:
			savedModelPath = new File(workingDir, RANKNET_MODEL_PATH).getAbsolutePath();
			predictionsPath = new File(workingDir, RANKNET_PREDICTIONS_PATH).getAbsolutePath();
			break;
		case LAMBDAMART:
			savedModelPath = new File(workingDir, LAMBDAMART_MODEL_PATH).getAbsolutePath();
			predictionsPath = new File(workingDir, LAMBDAMART_PREDICTIONS_PATH).getAbsolutePath();
			break;
		default:
			savedModelPath = new File(workingDir, RANKNET_MODEL_PATH).getAbsolutePath();
			predictionsPath = new File(workingDir, RANKNET_PREDICTIONS_PATH).getAbsolutePath();
		}
	}
	
	public void train() {
		switch (rankerType) {
		case RANKNET:
			rankNetTrain();
			break;
		case LAMBDAMART:
			lambdaMARTTrain();
			break;
		default:
			rankNetTrain();
		}
	}
	
	public void rankNetTrain() {
		String trainMetric = "NDCG@10";
		// RankNet-specific parameters
		String epoch = "200";
		String layer = "2";
		String node = "10";
		String lr = "0.00005";
		
		String[] args = new String[] {"-train", trainingFeaturesPath, "-ranker", "1", "-metric2t", trainMetric, "-save", savedModelPath,
				"-epoch", epoch, "-layer", layer, "-node", node, "-lr", lr};
		Evaluator.main(args);
	}
	
	public void lambdaMARTTrain() {
		String trainMetric = "NDCG@10";
		// LambdaMART-specific parameters
		String tree = "1000";
		String leaf = "10";
		String shrinkage = "0.1";
		String tc = "256";
		String mls = "1";
		String estop = "100";
		
		String[] args = new String[] {"-train", trainingFeaturesPath, "-ranker", "6", "-metric2t", trainMetric, "-save", savedModelPath,
				"-tree", tree, "-leaf", leaf, "-shrinkage", shrinkage, "-tc", tc, "mls", mls, "-estop", estop};
		Evaluator.main(args);
	}
	
	public void evaluate() {
		RankerFactory rFact = new RankerFactory();
		Ranker ranker = rFact.loadRankerFromFile(savedModelPath);
		
		// read test features
		List<RankList> test = FeatureManager.readInput(testFeaturesPath);
		
		// evaluate and save score 
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(predictionsPath));
			for (RankList rl : test) {
				for (int i = 0; i < rl.size(); i++) {
					DataPoint dataPoint = rl.get(i);
					double score = ranker.eval(dataPoint);
					writer.write(score + "\n");
				}
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String getSavedModelPath() {
		return savedModelPath;
	}

	public void setSavedModelPath(String savedModelPath) {
		this.savedModelPath = savedModelPath;
	}

	public String getPredictionsPath() {
		return predictionsPath;
	}

	public void setPredictionsPath(String predictionsPath) {
		this.predictionsPath = predictionsPath;
	}
	
	public static void main(String[] args) {
		String workingDir = "D:\\data\\working\\AspectJ\\data_folder\\folder#0";
		RankerTest ranker = new RankerTest(RANKNET, workingDir);
		ranker.train();
		ranker.evaluate();
		SimpleEvaluator evaluator = new SimpleEvaluator(ranker.testFeaturesPath, ranker.predictionsPath);
		evaluator.evaluate();
	}
	
	
}
