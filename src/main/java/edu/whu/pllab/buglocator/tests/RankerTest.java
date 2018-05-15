package edu.whu.pllab.buglocator.tests;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ciir.umass.edu.eval.Evaluator;
import ciir.umass.edu.features.FeatureManager;
import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RANKER_TYPE;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.learning.RankerFactory;
import ciir.umass.edu.learning.RankerTrainer;
import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.metric.MetricScorerFactory;
import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.evaluation.ExperimentResult;
import edu.whu.pllab.buglocator.evaluation.SimpleEvaluator;

public class RankerTest {
	
	private static final Logger logger = LoggerFactory.getLogger(RankerTest.class);
	
	public static final int MART = 0;
	public static final int RANKNET = 1;
	public static final int RANKBOOST = 2;
	public static final int ADARANK = 3;
	public static final int COORDINATE_ASCENT = 4;
	public static final int LAMBDARANK = 5;
	public static final int LAMBDAMART = 6;
	public static final int LISTNET = 7;
	public static final int RANDOM_FORESTS = 7;
	public static final int LINEAR_REGRESSION = 9;
	public static final int SVMRANK = 10;
	
	// data paths
	public static final String TRAING_DATA_PATH = "train.dat";
	public static final String TEST_DATA_PATH = "test.dat";
	// model paths
	public static final String MART_MODEL_PATH = "MART.model";
	public static final String RANKNET_MODEL_PATH = "RankNet.model";
	public static final String RANKBOOST_MODEL_PATH = "RankBoost.model";
	public static final String ADARANK_MODEL_PATH = "AdaRank.model";
	public static final String COORASCENT_MODEL_PATH = "CoorAscent.model";
	public static final String LAMBDARANK_MODEL_PATH = "LambdaRank.model";
	public static final String LAMBDAMART_MODEL_PATH = "LambdaMART.model";
	public static final String LISTNET_MODEL_PATH = "ListNet.model";
	public static final String RANDOM_FORESTS_MODEL_PATH = "RandomForests.model";
	public static final String LINEAR_REGRESSION_MODEL_PATH = "LinearRegression.model";
	public static final String SVMRANK_MODEL_PATH = "SVMRank.model";
	// predictions paths
	public static final String MART_PREDICTIONS_PATH = "MART.predictions";
	public static final String RANKNET_PREDICTIONS_PATH = "RankNet.predictions";
	public static final String RANKBOOST_PREDICTIONS_PATH = "RankBoost.predictions";
	public static final String ADARANK_PREDICTIONS_PATH = "AdaRank.predictions";
	public static final String COORASCENT_PREDICTIONS_PATH = "CoorAscent.predictions";
	public static final String LAMBDARANK_PREDICTIONS_PATH = "LambdaRank.predictions";
	public static final String LAMBDAMART_PREDICTIONS_PATH = "LambdaMART.predictions";
	public static final String LISTNET_PREDICTIONS_PATH = "ListNet.predictions";
	public static final String RANDOM_FORESTS_PREDICTIONS_PATH = "RandomForests.predictions";
	public static final String LINEAR_REGRESSION_PREDICTIONS_PATH = "LinearRegression.predictions";
	public static final String SVMRANK_PREDICTIONS_PATH = "SVMRank.predictions";
	
	public static final String TRAIN_PREDICTIONS_PATH = "train.predictions";
	
	private static String[] modelPaths = new String[] { MART_MODEL_PATH, RANKNET_MODEL_PATH, RANKBOOST_MODEL_PATH,
			ADARANK_MODEL_PATH, COORASCENT_MODEL_PATH, LAMBDARANK_MODEL_PATH, LAMBDAMART_MODEL_PATH, LISTNET_MODEL_PATH,
			RANDOM_FORESTS_MODEL_PATH, LINEAR_REGRESSION_MODEL_PATH, SVMRANK_MODEL_PATH };
	
	private static String[] predictionsPaths = new String[] { MART_PREDICTIONS_PATH, RANKNET_PREDICTIONS_PATH,
			RANKBOOST_PREDICTIONS_PATH, ADARANK_PREDICTIONS_PATH, COORASCENT_PREDICTIONS_PATH,
			LAMBDARANK_PREDICTIONS_PATH, LISTNET_PREDICTIONS_PATH, RANDOM_FORESTS_PREDICTIONS_PATH,
			LINEAR_REGRESSION_PREDICTIONS_PATH, SVMRANK_PREDICTIONS_PATH };
	
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
		if (rankerType < 0 || rankerType > 10)
			rankerType = 0;
		String modelPath = modelPaths[rankerType];
		String predicitonsPath = predictionsPaths[rankerType];
		savedModelPath = new File(workingDir, modelPath).getAbsolutePath();
		predictionsPath = new File(workingDir, predicitonsPath).getAbsolutePath();
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
		case LINEAR_REGRESSION:
			linearRegressionTrain();
			break;
		case SVMRANK:
			svmRankTrain();
			break;
		default:
			rankNetTrain();
		}
	}
	
	public void rankNetTrain() {
		String trainMetric = "NDCG@10";
		// RankNet-specific parameters
		String epoch = "100";
		String layer = "2";
		String node = "15";
		String lr = "0.00005";
		
		String[] args = new String[] { "-train", trainingFeaturesPath, "-ranker", "1", "-metric2t", trainMetric, "-save", savedModelPath,
				"-epoch", epoch, "-layer", layer, "-node", node, "-lr", lr };
		Evaluator.main(args);
	}
	
	public void adaRankTrain() {
		String trainMetric = "NDCG@10";
		// AdaRank-specific parameters
		String round = "500";
		String tolerance = "0.002";
		String maxSelCount = "5";
		
		String[] args = new String[] { "-train", trainingFeaturesPath, "-ranker", "3", "-metric2t", trainMetric, "-round", round,
				"-tolerance", tolerance, "-max", maxSelCount };
		Evaluator.main(args);
	}
	
	public void coordinateAscentTrain() {
		String trainMetric = "NDCG@10";
		// Coordinate Ascent-specific parameters
		String nRestart = "3";
		String iteration  = "25";
		String tolerance = "0.001";
		String norm = "linear";
		
		String[] args = new String[] { "-train", trainingFeaturesPath, "-ranker", "4", "-metric2t", trainMetric, "-save", savedModelPath,
				"-r", nRestart, "-i", iteration, "-tolerance", tolerance, "-norm", norm };
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
		
		String[] args = new String[] { "-train", trainingFeaturesPath, "-ranker", "6", "-metric2t", trainMetric, "-save", savedModelPath,
				"-tree", tree, "-leaf", leaf, "-shrinkage", shrinkage, "-tc", tc, "-mls", mls, "-estop", estop };
		Evaluator.main(args);
	}
	
	public void linearRegressionTrain() {
		String trainMetric = "NDCG@10";
		RankerTrainer trainer = new RankerTrainer();
		List<RankList> train = FeatureManager.readInput(trainingFeaturesPath);
		int[] features = FeatureManager.getFeatureFromSampleVector(train);
		MetricScorer trainScorer = new MetricScorerFactory().createScorer(trainMetric);
		Ranker ranker = trainer.train(RANKER_TYPE.LINEAR_REGRESSION, train, features, trainScorer); 
		ranker.save(savedModelPath);
	}
	
	public void svmRankTrain() {
		double c = 0.0001;
		System.out.println("SVM Rank Training, current c: " + c);
		ProcessBuilder pb = new ProcessBuilder();
		pb.command(Property.SVM_RANK_LEARN_TOOL_PATH, "-c", String.valueOf(c), trainingFeaturesPath, savedModelPath);
	 	Process process;
	    try {
	    	// start process
	    	process = pb.start();
	    	
	    	// output process message
	    	String line = null;
	        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	        BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
	        while ( (line = reader.readLine()) != null) {
	        	logger.info(line);
	        }
	        while ((line = error.readLine()) != null)
	            logger.error(line);
	        reader.close();
	        error.close();
	        
	        process.waitFor();
	        process.destroy();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	public void predict() {
		if (rankerType != SVMRANK) {
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
		} else {
			ProcessBuilder pb = new ProcessBuilder();
			pb.command(Property.SVM_RANK_CLASSIFY_TOOL_PATH, testFeaturesPath, savedModelPath, predictionsPath);
		 	Process process;
		    try {
		    	// start process
		    	process = pb.start();
		    	
		    	// output process message
		    	String line = null;
		        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		        BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
		        while ( (line = reader.readLine()) != null) {
		        	logger.info(line);
		        }
		        while ((line = error.readLine()) != null)
		            logger.error(line);
		        reader.close();
		        error.close();
		        
		        process.waitFor();
		        process.destroy();
		    } catch (Exception e) {
		        e.printStackTrace();
		    }
		}
	}
	
	public void trainingDataPredict() {
		if (rankerType != SVMRANK) {
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
		} else {
			ProcessBuilder pb = new ProcessBuilder();
			pb.command(Property.SVM_RANK_CLASSIFY_TOOL_PATH, trainingFeaturesPath, savedModelPath, trainPredictionsPath);
		 	Process process;
		    try {
		    	// start process
		    	process = pb.start();
		    	
		    	// output process message
		    	String line = null;
		        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		        BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
		        while ( (line = reader.readLine()) != null) {
		        	logger.info(line);
		        }
		        while ((line = error.readLine()) != null)
		            logger.error(line);
		        reader.close();
		        error.close();
		        
		        process.waitFor();
		        process.destroy();
		    } catch (Exception e) {
		        e.printStackTrace();
		    }
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
	
	public int getRankerType() {
		return rankerType;
	}

	public String getTrainingFeaturesPath() {
		return trainingFeaturesPath;
	}

	public void setTrainingFeaturesPath(String trainingFeaturesPath) {
		this.trainingFeaturesPath = trainingFeaturesPath;
	}

	public String getTestFeaturesPath() {
		return testFeaturesPath;
	}

	public void setTestFeaturesPath(String testFeaturesPath) {
		this.testFeaturesPath = testFeaturesPath;
	}

	public static void foldsTest(int rankerType, String directory, String output) throws Exception {
		// keep experiment result on each fold 
		List<ExperimentResult> experimentResultList = new ArrayList<ExperimentResult>();
		
		BufferedWriter logWriter = null;
		if (output != null) 
			logWriter = new BufferedWriter(new FileWriter(output));
		String[] foldsName = new File(directory).list();
		String[] foldsPath = new String[foldsName.length];
		for (int i = 0; i < foldsName.length; i++) {
			foldsPath[i] = new File(directory, foldsName[i]).getAbsolutePath();
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
		
		ExperimentResult finalResult = ExperimentResult.pollExperimentResult(experimentResultList);
		
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
		ranker.predict();
		ranker.trainingDataPredict();
		SimpleEvaluator evaluator = new SimpleEvaluator(ranker.testFeaturesPath, ranker.predictionsPath);
		evaluator.evaluate();
		
		logger.info("Evaluating on training data...");
		SimpleEvaluator trainEvaluator = new SimpleEvaluator(ranker.trainingFeaturesPath, ranker.trainPredictionsPath);
		trainEvaluator.evaluate();
		
		return evaluator.getExperimentResult();
	}
	
	
	public static void main(String[] args) throws Exception {
		
//		String[] products = {"ASPECTJ", "SWT", "BIRT", "ECLIPSE_PLATFORM_UI", "TOMCAT", "JDT"};
		String[] products = {"SWT"};
		for (String product : products) {
			Property property = Property.loadInstance(product);
			String directory = new File(property.getWorkingDir(), "data_folder").getAbsolutePath();
			foldsTest(COORDINATE_ASCENT, directory, property.getEvaluateLogPath());
		}

//		String fold = "D:\\data\\working\\AspectJ\\data_folder\\folder#1";
//		foldTest(COORDINATE_ASCENT, fold);
//		foldTest(LINEAR_REGRESSION, fold);
	}
	
	
}
