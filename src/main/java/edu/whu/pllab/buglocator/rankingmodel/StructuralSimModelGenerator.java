package edu.whu.pllab.buglocator.rankingmodel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportCorpus;
import edu.whu.pllab.buglocator.common.SourceCode;
import edu.whu.pllab.buglocator.common.SourceCodeCorpus;
import edu.whu.pllab.buglocator.common.TokenScore;
import edu.whu.pllab.buglocator.rankingmodel.IntegratedScore.KeyFeatureComparator;
import edu.whu.pllab.buglocator.similarity.Similarity;

public class StructuralSimModelGenerator {
	
	private static Logger logger = LoggerFactory.getLogger(StructuralSimModelGenerator.class);
	
	
	public static final double CANDIDATE_SOURCE_CODE = 300;
	
	public static final int BR_BR_SIMILARITY = Similarity.VSM;
	public static final int BR_CODE_SIMILARITY = Similarity.VSM;
	
	private boolean normalize;
	private boolean normalizePerBugReport = true; 

	private HashMap<Integer, BugReport> trainingBugReportsMap;
	private HashMap<Integer, BugReport> testBugReportsMap;
	private HashMap<String, SourceCode> sourceCodeMap;
	
	private double[] maxFieldSimilarities = new double[9];
	private double[] minFieldSimilarities = new double[9];
	
	private HashMap<BugReport, List<IntegratedScore>> finals;
	
	public StructuralSimModelGenerator() {
		for (int i = 0; i < maxFieldSimilarities.length; i++)
			maxFieldSimilarities[i] = Double.MIN_VALUE;
		for (int i = 0; i < minFieldSimilarities.length; i++)
			minFieldSimilarities[i] = Double.MIN_VALUE;
		finals = new HashMap<BugReport, List<IntegratedScore>>();
	}
	
	/** save features max min value to given file */
	public void saveParameters(File file) {
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			writer.write("maxFieldSimilarities: ");
			for (int i = 0; i < maxFieldSimilarities.length; i++)
				writer.write((i + 1) + ":" + maxFieldSimilarities[i] + " ");
			writer.write("\n");
			writer.write("minFieldSimilarities: ");
			for (int i = 0; i < minFieldSimilarities.length; i++)
				writer.write((i + 1) + ":" + minFieldSimilarities[i] + " ");
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** load features max min value from input file */
	public void loadParameters(File file) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("maxFieldSimilarities")) {
					String[] values = line.split(" ");
					for (int i = 1; i < values.length; i++) 
						maxFieldSimilarities[i - 1] = Integer.parseInt(values[i].split(":")[1]);
				}
				if (line.startsWith("minFieldSimilarities")) {
					String[] values = line.split(" ");
					for (int i = 1; i < values.length; i++) 
						minFieldSimilarities[i - 1] = Integer.parseInt(values[i].split(":")[1]);
				}
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * calculate integrated score of all source code file for all bug report.
	 *  if isTraining is true, only return top N irrelevant source code files, and normalize features when all integrated scores are calculated,
	 *  otherwise return all source code files, and normalize features when calculating integrated scores for every bug report*/
	public void generate(boolean isTraining) {
		logger.info("Generating structural integrated scores for all bug reports... isTraining: " + isTraining);
		// clear finals
		finals.clear();
		HashMap<Integer, BugReport> bugReportsMap = null;
		if (isTraining) 
			bugReportsMap = trainingBugReportsMap;
		else 
			bugReportsMap = testBugReportsMap;
		// create multi threads and iterate bug report, calculate features.
		int lostBr = 0;
		ExecutorService executor = Executors.newFixedThreadPool(Property.THREAD_COUNT);
		for (BugReport bugReport : bugReportsMap.values()) {
			if (bugReport.getFixedFiles().size() == 0) {
				lostBr++;
				continue;
			}
			Runnable worker = new WorkerThread(bugReport, isTraining);
			executor.execute(worker);
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
		}
		
		logger.info("Total bug reports:" + bugReportsMap.size() + ", lost bug reports:" + lostBr);
		// normalize all features
		if (normalize) {
			if (isTraining && !normalizePerBugReport) {
				for (List<IntegratedScore> integratedScoreList : finals.values()) {
					for (IntegratedScore integratedScore : integratedScoreList) {
						normalize(integratedScore.getFeatures());
					}
				}
			}
		}
	}
	
	/** worker parsing source code file */
	private class WorkerThread implements Runnable {
		
		private BugReport bugReport;
		private boolean isTraining;
		
		private WorkerThread(BugReport bugReport, boolean isTraining) {
			this.bugReport = bugReport;
			this.isTraining = isTraining;
		}
		
		@Override
		public void run() {
			List<IntegratedScore> integratedScoreList = generate(bugReport, isTraining);
			synchronized (finals) {
				finals.put(bugReport, integratedScoreList);
			}
		}
	}
	
	/** calculate integrated score of all source code file for given bug report.
	 *  if isTraining is true, only return top N irrelevant source code files, and do not normalize features,
	 *  otherwise return all source code files, and normalize features */
	public List<IntegratedScore> generate(BugReport br, boolean isTraining) {
		List<IntegratedScore> result = new ArrayList<IntegratedScore>();
		List<IntegratedScore> integratedScoreList = new ArrayList<IntegratedScore>();
		
		double[] maxFieldSimilarities = new double[9];
		double[] minFieldSimilarities = new double[9];
		for (int i = 1; i < maxFieldSimilarities.length; i++)
			maxFieldSimilarities[i] = Double.MIN_VALUE;
		for (int i = 1; i < minFieldSimilarities.length; i++)
			minFieldSimilarities[i] = Double.MAX_VALUE;
		
		// Enum code files.
		for (SourceCode code : sourceCodeMap.values()) {
			double[] features = generate(br, code, isTraining);
			if (features == null)
				continue;
			
			if (normalize) {
				if (normalizePerBugReport) {
					updateMaxMinFeatures(features, maxFieldSimilarities, minFieldSimilarities);
				} else {
					if (isTraining) {
						updateMaxMinFeatures(features);
					}
				}
			}
			
			boolean isModified = false;
			if (isTraining) {
				if (br.isModified(code.getPath()))
					isModified = true;
			}
			integratedScoreList.add(new IntegratedScore(code.getPath(), isModified, features));
		}
		if (normalize && normalizePerBugReport) {
			for (IntegratedScore integratedScore : integratedScoreList) {
				normalize(integratedScore.getFeatures(), maxFieldSimilarities, minFieldSimilarities);
			}
		}
		
		// if isTest, return all integratedScores, otherwise, if isTraining, reserve top CANDIDATE_SOURCE_CODE integratedScores
		if (!isTraining) 
			return integratedScoreList;
		else {
			// Sort and add to the final results.
			integratedScoreList.sort(new KeyFeatureComparator());
			Integer count = 0;
			for (IntegratedScore score : integratedScoreList) {
				result.add(score);
				if (!score.isModified())
					count++;
				if (count >= CANDIDATE_SOURCE_CODE)
					break;
			}
			return result;
		}
	}
	
	
	/** calculate features for given bug report and source code file */
	public double[] generate(BugReport br, SourceCode code, boolean isTraining) {
		double[] features = new double[9];
		BugReportCorpus brCorpus = br.getBugReportCorpus();
		SourceCodeCorpus codeCorpus = code.getSourceCodeCorpus();
		List<HashMap<String, TokenScore>> brFields = new ArrayList<HashMap<String, TokenScore>>();
		List<Double> brFieldsNorm = new ArrayList<Double>();
		List<HashMap<String, TokenScore>> codeFields = new ArrayList<HashMap<String, TokenScore>>();
		List<Double> codeFieldsNorm = new ArrayList<Double>();
		brFields.add(brCorpus.getSummaryTokens());
		brFields.add(brCorpus.getDescriptionTokens());
		brFieldsNorm.add(brCorpus.getSummaryNorm());
		brFieldsNorm.add(brCorpus.getDescriptionNorm());
		codeFields.add(codeCorpus.getClassPartTokens());
		codeFields.add(codeCorpus.getMethodPartTokens());
		codeFields.add(codeCorpus.getVariablePartTokens());
		codeFields.add(codeCorpus.getCommentPartTokens());
		codeFieldsNorm.add(codeCorpus.getClassCorpusNorm());
		codeFieldsNorm.add(codeCorpus.getMethodCorpusNorm());
		codeFieldsNorm.add(codeCorpus.getVariableCorpusNorm());
		codeFieldsNorm.add(codeCorpus.getCommentCorpusNorm());
		// calculate documents scores across eight combinations
		double fieldSimilaritySum = 0.0;
		int index = 1;
		for (int i = 0; i < brFields.size(); i++) {
			HashMap<String, TokenScore> brFieldTokens = brFields.get(i);
			double brFieldNorm = brFieldsNorm.get(i);
			for (int j = 0; j < codeFields.size(); j++) {
				HashMap<String, TokenScore> codeFieldTokens = codeFields.get(j);
				double codeFieldNorm = codeFieldsNorm.get(j);
				if (brFieldNorm == 0 || codeFieldNorm == 0)
					features[index] = 0.0;
				else  // if norm value doesn't equal zero, calculate field similarity
					features[index] = Similarity.vsmSimilarityWithoutNorm(brFieldTokens, codeFieldTokens)
							/ (brFieldNorm * codeFieldNorm);
				fieldSimilaritySum += features[index];
				index++;
			}
		}
		// set features[0] fieldSimilaritySum, used to get top N candidate source code
		features[0] = fieldSimilaritySum;
		// if is testing and not normalizePerBugReport, normalize with global min/maxFieldSimilarities 
		if (normalize && !isTraining && !normalizePerBugReport)
			normalize(features);
		
		return features;
	}
	
	/** update features max min value */
	public void updateMaxMinFeatures(double features[], double maxFieldSimilarities[], double[] minFieldSimilarities) {
		for (int i = 1; i < features.length; i++) {
			if (features[i] > maxFieldSimilarities[i])
				maxFieldSimilarities[i] = features[i];
			if (features[i] < minFieldSimilarities[i])
				minFieldSimilarities[i] = features[i];
		}
	}
	
	/** update global max min features value */
	public synchronized void updateMaxMinFeatures(double features[]) {
		for (int i = 1; i < features.length; i++) {
			if (features[i] > maxFieldSimilarities[i])
				maxFieldSimilarities[i] = features[i];
			if (features[i] < minFieldSimilarities[i])
				minFieldSimilarities[i] = features[i];
		}
	}
	
	/**
	 * Normalize an array of features using max/minFieldSimilarities per record
	 * @param features The feature array to normalize.
	 */
	public void normalize(double[] features, double[] maxFieldSimilarities, double[] minFieldSimilarities) {
		for (int i = 1; i < features.length; i++) 
			features[i] = this.maxMinNormalize(maxFieldSimilarities[i], minFieldSimilarities[i], features[i]);
	}
	
	/**
	 * Normalize an array of features using global max/minFieldSimilarities
	 * @param features The feature array to normalize.
	 */
	public void normalize(double[] features) {
		for (int i = 1; i < features.length; i++) 
			features[i] = this.maxMinNormalize(maxFieldSimilarities[i], minFieldSimilarities[i], features[i]);
	}
	
	/**
	 * Min-max normalization.
	 * @param max The max value.
	 * @param min The min value.
	 * @param value The value to normalize.
	 * @return The normalized value.
	 */
	private double maxMinNormalize(double max, double min, double value) {
		if (max == min || value <= min) 
			return 0.0;
		if (value >= max)
			return 1.0;
		return (value - min) / (max - min);
	}
	
	/** write ranking features for svm learn-to-rank */
	public void writeRankingFeatures(String output) {
		logger.info("Writing ranking features to " + output);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(output));
			int qid = 0;
			for (Entry<BugReport, List<IntegratedScore>> entry : finals.entrySet()) {
				qid++;
				for (IntegratedScore score : entry.getValue()) {
					int rank = 1;
					if (entry.getKey().isModified(score.getPath()))
						rank = (int) CANDIDATE_SOURCE_CODE;
					writer.write(rank + " qid:" + qid);
					double[] features = score.getFeatures();
					for (Integer column = 1; column < features.length; column++) {
						writer.write(" " + column.toString() + ":" + features[column]);
					}
					writer.write("\n");
				}
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean isNormalizePerBugReport() {
		return normalizePerBugReport;
	}

	public void setNormalizePerBugReport(boolean normalizePerBugReport) {
		this.normalizePerBugReport = normalizePerBugReport;
	}

	public HashMap<Integer, BugReport> getTrainingBugReportsMap() {
		return trainingBugReportsMap;
	}

	public void setTrainingBugReportsMap(HashMap<Integer, BugReport> trainingBugReportsMap) {
		this.trainingBugReportsMap = trainingBugReportsMap;
	}

	public HashMap<Integer, BugReport> getTestBugReportsMap() {
		return testBugReportsMap;
	}

	public void setTestBugReportsMap(HashMap<Integer, BugReport> testBugReportsMap) {
		this.testBugReportsMap = testBugReportsMap;
	}

	public HashMap<String, SourceCode> getSourceCodeMap() {
		return sourceCodeMap;
	}

	public void setSourceCodeMap(HashMap<String, SourceCode> sourceCodeMap) {
		this.sourceCodeMap = sourceCodeMap;
	}

	public HashMap<BugReport, List<IntegratedScore>> getFinals() {
		return finals;
	}

	public void setFinals(HashMap<BugReport, List<IntegratedScore>> finals) {
		this.finals = finals;
	}

	public boolean isNormalize() {
		return normalize;
	}

	public void setNormalize(boolean normalize) {
		this.normalize = normalize;
	}
	
}
