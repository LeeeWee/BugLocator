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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.Method;
import edu.whu.pllab.buglocator.common.SimilarBugReport;
import edu.whu.pllab.buglocator.common.SourceCode;
import edu.whu.pllab.buglocator.similarity.Similarity;
import edu.whu.pllab.buglocator.rankingmodel.IntegratedScore.KeyFeatureComparator;

public class RankingModelGenerator {
	
	private static Logger logger = LoggerFactory.getLogger(RankingModelGenerator.class);
	
	public static final double CANDIDATE_SOURCE_CODE = 300;
	public static final int TOP_SIMILAR_BUG_REPORTS = 0;
	
	public static final int BR_BR_SIMILARITY = Similarity.VSM;
	public static final int BR_CODE_SIMILARITY = Similarity.VSM;
	
	private HashMap<Integer, BugReport> trainingBugReportsMap;
	private HashMap<Integer, BugReport> testBugReportsMap;
	private HashMap<String, SourceCode> sourceCodeMap;
	
	/** used to normalize sourceCodeSimilarity */
	private double maxSourceCodeSimilarity;
	private double minSourceCodeSimilarity;
	
	/** used to normalize APISimilarity */
	private double maxAPISimilarity;
	private double minAPISimilarity;
	
	/** used to normalize collaborativeFilteringScore */
	private double maxCollaborativeFilteringScore;
	private double minCollaborativeFilteringScore;

	/** used to normalize classNameSimilarity */
	private double maxClassNameSimilarity;
	private double minClassNameSimilarity;
	
	/** used to normalize recency */
	private double maxRecency;
	private double minRecency;
	
	/** used to normalize frequency */
	private double maxFrequency;
	private double minFrequency;
	
	private HashMap<BugReport, List<IntegratedScore>> finals;
	
	public RankingModelGenerator() {
		maxSourceCodeSimilarity = Double.MIN_VALUE;
		minSourceCodeSimilarity = Double.MAX_VALUE;
		maxAPISimilarity = Double.MIN_VALUE;
		minAPISimilarity = Double.MAX_VALUE;
		maxCollaborativeFilteringScore = Double.MIN_VALUE;
		minCollaborativeFilteringScore = Double.MAX_VALUE;
		maxClassNameSimilarity = Double.MIN_VALUE;
		minClassNameSimilarity = Double.MAX_VALUE;
		maxRecency = Double.MIN_VALUE;
		minRecency = Double.MAX_VALUE;
		maxFrequency = Double.MIN_VALUE;
		minFrequency = Double.MAX_VALUE;
		finals = new HashMap<BugReport, List<IntegratedScore>>();
	}
	
	/** save features max min value to given file */
	public void saveParameters(File file) {
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			writer.write("maxSourceCodeSimilarity:" + maxSourceCodeSimilarity + "\n");
			writer.write("minSourceCodeSimilarity:" + minSourceCodeSimilarity + "\n");
			writer.write("maxAPISimilarity:" + maxAPISimilarity + "\n");
			writer.write("minAPISimilarity:" + minAPISimilarity + "\n");
			writer.write("maxCollaborativeFilteringScore:" + maxCollaborativeFilteringScore + "\n");
			writer.write("minCollaborativeFilteringScore:" + minCollaborativeFilteringScore + "\n");
			writer.write("maxClassNameSimilarity:" + maxClassNameSimilarity + "\n");
			writer.write("minClassNameSimilarity:" + minClassNameSimilarity + "\n");
			writer.write("maxRecency:" + maxRecency + "\n");
			writer.write("minRecency:" + minRecency + "\n");
			writer.write("maxFrequency:" + maxFrequency + "\n");
			writer.write("minFrequency:" + minFrequency + "\n");
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
				String[] words = line.split(":");
				double value = Double.parseDouble(words[1]);
				switch (words[0]) {
				case "maxSourceCodeSimilarity":
					maxSourceCodeSimilarity = value;
					break;
				case "minSourceCodeSimilarity":
					minSourceCodeSimilarity = value;
					break;
				case "maxAPISimilarity":
					maxAPISimilarity = value;
					break;
				case "minAPISimilarity":
					minAPISimilarity = value;
					break;
				case "maxCollaborativeFilteringScore":
					maxCollaborativeFilteringScore = value;
					break;
				case "minCollaborativeFilteringScore":
					minCollaborativeFilteringScore = value;
					break;
				case "maxClassNameSimilarity":
					maxClassNameSimilarity = value;
					break;
				case "minClassNameSimilarity":
					minClassNameSimilarity = value;
					break;
				case "maxRecency":
					maxRecency = value;
					break;
				case "minRecency":
					minRecency = value;
					break;
				case "maxFrequency":
					maxFrequency = value;
					break;
				case "minFrequency":
					minFrequency = value;
					break;
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
		logger.info("Generating integrated scores for all bug reports... isTraining: " + isTraining);
		
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
		for (List<IntegratedScore> integratedScoreList : finals.values()) {
			for (IntegratedScore integratedScore : integratedScoreList) {
				normalize(integratedScore.getFeatures());
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
		List<IntegratedScore> result = new LinkedList<IntegratedScore>();
		List<IntegratedScore> integratedScoreList = new ArrayList<IntegratedScore>();
		
		// calculate clooaborativeFiltering score for all source code file
		HashMap<String, Double> collaborativeFilteringScoreMap = calculateCollaborativeFilteringScore(br);
		
		// Enum code files.
		for (SourceCode code : sourceCodeMap.values()) {
			double[] features = generate(br, code, collaborativeFilteringScoreMap, isTraining);
			if (features == null)
				continue;
			boolean isModified = false;
			if (isTraining) {
				if (br.isModified(code.getPath()))
					isModified = true;
			}
			integratedScoreList.add(new IntegratedScore(code.getPath(), isModified, features));
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
	
	/** calculate features for given bug report and source code file, if isTraining is false, normalize features */
	public double[] generate(BugReport br, SourceCode code, HashMap<String, Double> collaborativeFilteringScoreMap,
			boolean isTraining) {
		double sourceCodeSimilarity = calculateSourceCodeSimilarity(br, code);
		double APISimilarity = calculateAPISimilarity(br, code);
		double collaborativeFilteringScore = collaborativeFilteringScoreMap.containsKey(code.getPath())
				? collaborativeFilteringScoreMap.get(code.getPath()) : 0.0;
		double classNameSimilarity = calculateClassNameSimilarity(br ,code);
		double recency = calculateRecency(br, code);
		double frequency = calculateFrequency(br, code);
		
		/** update max-min value */
		if (isTraining) {
			updateMaxMinFeatures(sourceCodeSimilarity, APISimilarity, collaborativeFilteringScore,
					classNameSimilarity, recency, frequency);
		}
		
		double[] features = new double[] { sourceCodeSimilarity, APISimilarity, collaborativeFilteringScore,
				classNameSimilarity, recency, frequency };
		
		if (!isTraining) 
			normalize(features);
		
		return features;
	} 
	
	/** calculate source code similarity between given bug report and source code */
	public double calculateSourceCodeSimilarity(BugReport br, SourceCode code) {
		double sourceCodeSimilarity = Similarity.similarity(br, code, BR_CODE_SIMILARITY);
		for (Method method : code.getMethodList()) {
			double methodSimilarity = Similarity.similarity(br, method, BR_CODE_SIMILARITY);
			if (methodSimilarity > sourceCodeSimilarity)
				sourceCodeSimilarity = methodSimilarity;
		}
		return sourceCodeSimilarity;
	}
	
	//TODO
	public double calculateAPISimilarity(BugReport br, SourceCode code) {
		return 0.0;
	}
	
	/** calculate class name similarity */
	public double calculateClassNameSimilarity(BugReport br, SourceCode code) {
		double classNameSimilarity = 0.0;
		String fullClassName = code.getFullClassName();
		String className;
		if (!fullClassName.contains("."))
			className = fullClassName.contains("/") ? fullClassName.substring(fullClassName.lastIndexOf("/")) : fullClassName;
		else 
			className = fullClassName.substring(fullClassName.lastIndexOf("."));
		if (br.getSummary().toLowerCase().contains(className.toLowerCase())) 
			classNameSimilarity = className.length();
//		 if (br.getDescription().toLowerCase().contains(className.toLowerCase()))
//			classNameSimilarity = className.length();
		return classNameSimilarity;
	}
	
	/** calculate source code recency value */
	public double calculateRecency(BugReport br, SourceCode code) {
		double recency = 0.0;
		long reportTime = br.getReportTime().getTime();
		int index = code.locateChangePoint(reportTime);
		if (index > 0) {
			long lastModifiedTime = code.getChangePoint(index);
			Double monthDurationTime = ((double)(reportTime - lastModifiedTime)) / 1000.0 / 3600.0 / 24.0 / 30.0;
			// get the integral part of month duration time and calculate inverse distance
			recency = 1.0 / (1.0 + monthDurationTime.intValue());
		}
		return recency;
	}
	
	/** calculate source code frequency value */
	public double calculateFrequency(BugReport br, SourceCode code) {
		double frequency = 0.0;
		long reportTime = br.getReportTime().getTime();
		int index = code.locateChangePoint(reportTime);
		if (index > 0) {
			frequency = (double)code.countChangeFrequency(index);
		}
		return frequency;
	}
	
	/** calculate collaborative filter score for given bug report */
	public HashMap<String, Double> calculateCollaborativeFilteringScore(BugReport br) {
		HashMap<String, Double> collaborativeFilteringScoreMap = new HashMap<String, Double>();
		List<SimilarBugReport> similarBugReports = getSimilarBugReports(br, TOP_SIMILAR_BUG_REPORTS);
		for (SimilarBugReport similarBugReport : similarBugReports) {
			for (String fixedFile : similarBugReport.getFixedFiles()) {
				if (!collaborativeFilteringScoreMap.containsKey(fixedFile))
					collaborativeFilteringScoreMap.put(fixedFile, 0.0);
				else 
					collaborativeFilteringScoreMap.put(fixedFile, collaborativeFilteringScoreMap.get(fixedFile)
							+ similarBugReport.getSimilarity() / similarBugReport.getFixedFiles().size());
			}
		}
		return collaborativeFilteringScoreMap;
	}
	
	/**
	 * get top similar bug report for input br, if top <= 0, get all bug reports 
	 * @param br input bug report
	 * @param top input similar bug reports' size
	 * @return list of similarBugReport.
	 */
	public List<SimilarBugReport> getSimilarBugReports(BugReport br, int top) {
		List<SimilarBugReport> similarBugReports = new ArrayList<SimilarBugReport>();
		PriorityQueue<SimilarBugReport> heap = new PriorityQueue<SimilarBugReport>(trainingBugReportsMap.size(),
				new SimilarBugReport.SimilarityComparator());
		for (Entry<Integer, BugReport> entry : trainingBugReportsMap.entrySet()) {
			if (entry.getKey().equals(br.getBugID()))
				continue;
			double similarity = Similarity.similarity(br, entry.getValue(), BR_BR_SIMILARITY);
			heap.add(new SimilarBugReport(entry.getValue(), similarity));
		}
		if (top <= 0) {
			while (!heap.isEmpty()) 
				similarBugReports.add(heap.poll());
		} else {
			int left = top;
			while (left >= 0 && (!heap.isEmpty())) {
				similarBugReports.add(heap.poll());
				left--;
			}
		}
		return similarBugReports;
	}
	
	/** update features max min value */
	public synchronized void updateMaxMinFeatures(double sourceCodeSimilarity, double APISimilarity,
			double collaborativeFilteringScore, double classNameSimilarity, double recency, double frequency) {
		if (sourceCodeSimilarity > maxSourceCodeSimilarity)
			maxSourceCodeSimilarity = sourceCodeSimilarity;
		if (sourceCodeSimilarity < minSourceCodeSimilarity)
			minSourceCodeSimilarity = sourceCodeSimilarity;
		if (APISimilarity > maxAPISimilarity) 
			maxAPISimilarity = APISimilarity;
		if (APISimilarity < minAPISimilarity)
			minAPISimilarity = APISimilarity;
		if (collaborativeFilteringScore > maxCollaborativeFilteringScore)
			maxCollaborativeFilteringScore = collaborativeFilteringScore;
		if (collaborativeFilteringScore < minCollaborativeFilteringScore)
			minCollaborativeFilteringScore = collaborativeFilteringScore;
		if (classNameSimilarity > maxClassNameSimilarity)
			maxClassNameSimilarity = classNameSimilarity;
		if (classNameSimilarity < minClassNameSimilarity)
			minClassNameSimilarity = classNameSimilarity;
		if (recency > maxRecency)
			maxRecency = recency;
		if (recency < minRecency)
			minRecency = recency;
		if (frequency > maxFrequency)
			maxFrequency = frequency;
		if (frequency < minFrequency)
			minFrequency = frequency;
	}
	
	/**
	 * Normalize an array of features.
	 * @param features The feature array to normalize.
	 */
	public void normalize(double[] features) {
		features[0] = this.maxMinNormalize(maxSourceCodeSimilarity, minSourceCodeSimilarity, features[0]);
		features[1] = this.maxMinNormalize(maxAPISimilarity, minAPISimilarity, features[1]);
		features[2] = this.maxMinNormalize(maxCollaborativeFilteringScore, minCollaborativeFilteringScore, features[2]);
		features[3] = this.maxMinNormalize(maxClassNameSimilarity, minClassNameSimilarity, features[3]);
		features[4] = this.maxMinNormalize(maxRecency, minRecency, features[4]);
		features[5] = this.maxMinNormalize(maxFrequency, minFrequency, features[5]);
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
					for (Integer column = 1; column <= features.length; column++) {
						writer.write(" " + column.toString() + ":" + features[column - 1]);
					}
					writer.write("\n");
				}
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** filter bug reports whose all fixed files do not exist in sourceCodeMap */
	public void filterBugReports(HashMap<Integer, BugReport> bugReportsMap) {
		Iterator<Entry<Integer, BugReport>> iter = bugReportsMap.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Integer, BugReport> entry = iter.next();
			BugReport bugReport = entry.getValue();
			boolean invalid = true;
			for (String fixedFile : bugReport.getFixedFiles()) {
				if (sourceCodeMap.containsKey(fixedFile))
					invalid = false;
			}
			if (invalid) 
				iter.remove();
		}
	}
	
	// setters and getters
	public HashMap<String, SourceCode> getSourceCodeMap() {
		return sourceCodeMap;
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

	public void setSourceCodeMap(HashMap<String, SourceCode> sourceCodeMap) {
		this.sourceCodeMap = sourceCodeMap;
	}
	
	public HashMap<BugReport, List<IntegratedScore>> getFinals() {
		return finals;
	}

}
