package edu.whu.pllab.buglocator.tests;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ciir.umass.edu.eval.Evaluator;
import ciir.umass.edu.features.FeatureManager;
import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.learning.RankerFactory;
import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.ExperimentResult;
import edu.whu.pllab.buglocator.evaluation.SimpleEvaluator;

public class RankerTest {
	
	private static final Logger logger = LoggerFactory.getLogger(RankerTest.class);
	
	public static final int RANKNET = 1;
	public static final int COORDINATE_ASCENT = 4;
	public static final int LAMBDAMART = 6;
	
	public static final String TRAING_DATA_PATH = "train.dat";
	public static final String TEST_DATA_PATH = "test.dat";
	public static final String RANKNET_MODEL_PATH = "RankNet.model";
	public static final String COORASCENT_MODEL_PATH = "CoorAscent.model";
	public static final String LAMBDAMART_MODEL_PATH = "LambdaMART.model";
	public static final String RANKNET_PREDICTIONS_PATH = "RankNet.predictions";
	public static final String COORASCENT_PREDICTIONS_PATH = "CoorAscent.predictions";
	public static final String LAMBDAMART_PREDICTIONS_PATH = "LambdaMART.predictions";
	
	public static final String TRAIN_PREDICTIONS_PATH = "train.predictions";
	
	
	private int rankerType; 
	
	private String workingDir;
	private String trainingFeaturesPath;
	private String testFeaturesPath;
	private String savedModelPath;
	private String predictionsPath;
	// for test
	private String trainPredictionsPath;
	
	public RankerTest(int rankerType) {
		this.rankerType = rankerType;
		Property property = Property.getInstance();
		workingDir = property.getWorkingDir();
		trainingFeaturesPath = property.getTrainingFeaturesPath();
		testFeaturesPath = property.getTestFeaturesPath();
		trainPredictionsPath = new File(workingDir, TRAIN_PREDICTIONS_PATH).getAbsolutePath();
		setModelPredictionsPath();
	}
	
	public RankerTest(int rankerType, String workingDir) {
		this.rankerType = rankerType;
		this.workingDir = workingDir;
		trainingFeaturesPath = new File(workingDir, TRAING_DATA_PATH).getAbsolutePath();
		testFeaturesPath = new File(workingDir, TEST_DATA_PATH).getAbsolutePath();
		trainPredictionsPath = new File(workingDir, TRAIN_PREDICTIONS_PATH).getAbsolutePath();
		setModelPredictionsPath();
	}
	
	public void setModelPredictionsPath() {
		switch (rankerType) {
		case RANKNET:
			savedModelPath = new File(workingDir, RANKNET_MODEL_PATH).getAbsolutePath();
			predictionsPath = new File(workingDir, RANKNET_PREDICTIONS_PATH).getAbsolutePath();
			break;
		case COORDINATE_ASCENT:
			savedModelPath = new File(workingDir, COORASCENT_MODEL_PATH).getAbsolutePath();
			predictionsPath = new File(workingDir, COORASCENT_PREDICTIONS_PATH).getAbsolutePath();
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
		case COORDINATE_ASCENT:
			coordinateAscentTrain();
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
		String tree = "200";
		String leaf = "10";
		String shrinkage = "0.1";
		String tc = "256";
		String mls = "1";
		String estop = "100";
		
		String[] args = new String[] {"-train", trainingFeaturesPath, "-ranker", "6", "-metric2t", trainMetric, "-save", savedModelPath,
				"-tree", tree, "-leaf", leaf, "-shrinkage", shrinkage, "-tc", tc, "-mls", mls, "-estop", estop};
		Evaluator.main(args);
	}
	
	public void coordinateAscentTrain() {
		String trainMetric = "NDCG@10";
		// Coordinate Ascent-specific parameters
		String nRestart = "3";
		String iteration  = "25";
		String tolerance = "0.001";
		
		String[] args = new String[] {"-train", trainingFeaturesPath, "-ranker", "4", "-metric2t", trainMetric, "-save", savedModelPath,
				"-r", nRestart, "-i", iteration, "-tolerance", tolerance};
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
	
	public void trainingDataEvaluate() {
		RankerFactory rFact = new RankerFactory();
		Ranker ranker = rFact.loadRankerFromFile(savedModelPath);
		
		// read test features
		List<RankList> test = FeatureManager.readInput(trainingFeaturesPath);
		
		// evaluate and save score 
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(trainPredictionsPath));
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
	
	public static void foldsTest(int rankerType, String direcotry, String output) throws Exception {
		// keep experiment result on each fold 
		List<ExperimentResult> experimentResultList = new ArrayList<ExperimentResult>();
		
		BufferedWriter logWriter = null;
		if (output != null) 
			logWriter = new BufferedWriter(new FileWriter(output));
		String[] foldsName = new File(direcotry).list();
		String[] foldsPath = new String[foldsName.length];
		for (int i = 0; i < foldsName.length; i++) {
			foldsPath[i] = new File(direcotry, foldsName[i]).getAbsolutePath();
		}
		// test fold#i
		for (int i = 0; i < foldsPath.length; i++) {
			File foldFile = new File(foldsPath[i]);
			if (!foldFile.isDirectory())
				continue;
			
			ExperimentResult experimentResult = foldTest(rankerType, foldsPath[i]);
			experimentResultList.add(experimentResult);
			
			if (logWriter != null) {
				logWriter.write(String.format("test on %d-th fold:", i) + "\n");
				logWriter.write(experimentResult.toString() + "\n\n");
				logWriter.flush();
			}
		}
		
		// pool the bug reports from all test folds and compute the overall system performance
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
		
		StringBuilder builder = new StringBuilder();
		builder.append("\n");
		builder.append("===================== Final Experiment Result =========================\n");
		builder.append(finalResult.toString() + "\n");
		builder.append("=======================================================================");
		
		System.out.println(builder.toString());
		logWriter.write(builder.toString());
		
		logWriter.close();
	}
	
	public static ExperimentResult foldTest(int rankerType, String foldPath) throws Exception {
		
		RankerTest ranker = new RankerTest(rankerType, foldPath);
		ranker.train();
		ranker.evaluate();
		ranker.trainingDataEvaluate();
		SimpleEvaluator evaluator = new SimpleEvaluator(ranker.testFeaturesPath, ranker.predictionsPath);
		evaluator.evaluate();
		
		logger.info("Evaluating on training data...");
		SimpleEvaluator trainEvaluator = new SimpleEvaluator(ranker.trainingFeaturesPath, ranker.trainPredictionsPath);
		trainEvaluator.evaluate();
		
		return evaluator.getExperimentResult();
	}
	
	public static void main(String[] args) throws Exception {
		String[] products = {"ASPECTJ", "SWT", "BIRT", "ECLIPSE_PLATFORM_UI", "TOMCAT", "JDT"};
		for (String product : products) {
			Property property = Property.loadInstance(product);
			String directory = new File(property.getWorkingDir(), "data_folder").getAbsolutePath();
			foldsTest(COORDINATE_ASCENT, directory, property.getEvaluateLogPath());
		}
	}
	
	
}
