package edu.whu.pllab.buglocator.rankingmodel;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.Property;

public class SVMRank {
	
	private static Logger logger = LoggerFactory.getLogger(SVMRank.class);
	
	/** svm rank tool path */
	private static String svmRankLearnTool = Property.SVM_RANK_LEARN_TOOL_PATH;
	private static String svmRankClassifyTool = Property.SVM_RANK_CLASSIFY_TOOL_PATH;
	
	public static void train(double c) {
		Property property = Property.getInstance();
		train(property.getTrainingFeaturesPath(), property.getSVMRankModelPath(), c);
	}
	
	public static void predict() {
		Property property = Property.getInstance();
		predict(property.getTestFeaturesPath(), property.getSVMRankModelPath(), property.getPredictionsPath());
	}
	
	/**
	 * svm-linear rank with svm_rank_learn.exe
	 * @param trainingDataPath input trainingData path
	 * @param modelPath output model path
	 * @param c parameter of svm-linear rank
	 */
	public static void train(String trainingDataPath, String modelPath, double c) {
		logger.info("SVM Rank Training...");
		ProcessBuilder pb = new ProcessBuilder();
		pb.command(svmRankLearnTool, "-c", String.valueOf(c), trainingDataPath, modelPath);
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
	
	/**
	 * generate predictions result for test data
	 * @param testDataPath input test data path
	 * @param modelPath input svm model path
	 * @param predictionsPath output predictions path
	 */
	public static void predict(String testDataPath, String modelPath, String predictionsPath) {
		logger.info("SVM Rank predicting...");
		ProcessBuilder pb = new ProcessBuilder();
		pb.command(svmRankClassifyTool, testDataPath, modelPath, predictionsPath);
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
