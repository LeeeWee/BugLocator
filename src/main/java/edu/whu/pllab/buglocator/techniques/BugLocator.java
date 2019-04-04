package edu.whu.pllab.buglocator.techniques;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.common.SourceCode;
import edu.whu.pllab.buglocator.common.SourceCodeRepository;
import edu.whu.pllab.buglocator.evaluation.Evaluator;
import edu.whu.pllab.buglocator.rankingmodel.IntegratedScore;
import edu.whu.pllab.buglocator.similarity.Similarity;
import edu.whu.pllab.buglocator.vectorizer.BugReportTfidfVectorizer;
import edu.whu.pllab.buglocator.vectorizer.SourceCodeTfidfVectorizer;

public class BugLocator {
	
	private static final Logger logger = LoggerFactory.getLogger(BugLocator.class);
	
	private static final double alpha = 0.2;
	private static boolean usingSimilarBugReports = true;
	private static boolean usingRevisedVSMModels = true;  // if false, using classic VSM models (without using length score)
	
	private static Similarity sim = new Similarity();
	private static String[] products = {"BugLocator_SWT", "BugLocator_AspectJ", "BugLocator_Eclipse", "BugLocator_ZXing"};
	
	private static HashMap<String, SourceCode> sourceCodeMap;
	private static HashMap<Integer, BugReport> bugReports;
	
	public static void main(String[] args) throws Exception {
		for (String product : products) {
			logger.info("Current Product: " + product);
			locate(product);
		}
	}
	
	public static void locate(String product) throws Exception {
		Property.USE_STRUCTURED_INFORMATION = false;
		Property property = Property.loadInstance(product);
//		property.setCodeRepositoryXMLPath(new File(property.getWorkingDir(), "codeRepository_structured.xml").getAbsolutePath());

		// record evaluate result
//		BufferedWriter logWriter = new BufferedWriter(new FileWriter(property.getEvaluateLogPath()));
		
		// initialize bugReport repository and source code repository
		BugReportRepository brRepo = new BugReportRepository();
		SourceCodeRepository codeRepo = new SourceCodeRepository();
//		codeRepo.saveSourceCodeRepoToXML(property.getCodeRepositoryXMLPath(), product);
		
		sourceCodeMap = codeRepo.getSourceCodeMap();
		bugReports = brRepo.getBugReports();
		
//		computeLengthScore();
		
		execute();
	}
	
	public static void execute() throws InterruptedException, ExecutionException {
		HashMap<BugReport, List<IntegratedScore>> integratedScoreMap = new HashMap<BugReport, List<IntegratedScore>>(); 
		HashMap<BugReport, HashMap<String, Double>> VSMScoreMap = null;
		HashMap<BugReport, HashMap<String, Double>> simiScoreMap = null;
		
		// calculate source code tokens weight
		SourceCodeTfidfVectorizer codeVectorizer = new SourceCodeTfidfVectorizer(sourceCodeMap);
		codeVectorizer.train();
		codeVectorizer.calculateTokensWeight(sourceCodeMap);
		
		// calculate bug reports tokens weight using code corpus's tfidf model
		BugReportTfidfVectorizer brVectorizer = new BugReportTfidfVectorizer(codeVectorizer.getTfidf());
		brVectorizer.calculateTokensWeight(bugReports);
		
		// calculate VSMScore map
		int count = 0;
		VSMScoreMap = new HashMap<BugReport, HashMap<String, Double>>();
		ExecutorService vsmES = Executors.newFixedThreadPool(Property.THREAD_COUNT);
		for (Entry<Integer, BugReport> brEntry : bugReports.entrySet()) {
			count++;
			Callable<HashMap<String, Double>> vsmScoreCalculator = new VSMScoreCalculator(brEntry.getValue(), count);
			Future<HashMap<String, Double>> future = vsmES.submit(vsmScoreCalculator);
			VSMScoreMap.put(brEntry.getValue(), future.get());
		}
		vsmES.shutdown();
		while (!vsmES.isTerminated()) {
		}
		
		// calculate SimiScore map if needed
		if (usingSimilarBugReports) {
			
			// train bug reports' tfidf model and re-calculate bug reports' weight
			BugReportTfidfVectorizer newBRVectorizer = new BugReportTfidfVectorizer(bugReports);
			newBRVectorizer.train();
			newBRVectorizer.calculateTokensWeight(bugReports);
			
			count = 0;
			simiScoreMap = new HashMap<BugReport, HashMap<String, Double>>();
			ExecutorService simiES = Executors.newFixedThreadPool(Property.THREAD_COUNT);
			for (Entry<Integer, BugReport> brEntry : bugReports.entrySet()) {
				count++;
				Callable<HashMap<String, Double>> simiScoreCalculator = new SimiScoreCalculator(brEntry.getValue(), count);
				Future<HashMap<String, Double>> future = simiES.submit(simiScoreCalculator);
				simiScoreMap.put(brEntry.getValue(), future.get());
			}
			simiES.shutdown();
			while (!simiES.isTerminated()) {
			}
		}
		
		// calculate final score
		ExecutorService finalES = Executors.newFixedThreadPool(Property.THREAD_COUNT);
		for (Entry<Integer, BugReport> brEntry : bugReports.entrySet()) {
			BugReport br = brEntry.getValue();
			Callable<List<IntegratedScore>> finalScoreCalculator;
			if (usingSimilarBugReports)
				finalScoreCalculator = new FinalScoreCalculator(br, VSMScoreMap.get(br), simiScoreMap.get(br));
			else 
				finalScoreCalculator = new FinalScoreCalculator(br, VSMScoreMap.get(br), null);
			Future<List<IntegratedScore>> future = finalES.submit(finalScoreCalculator);
			integratedScoreMap.put(brEntry.getValue(), future.get());
		}
		finalES.shutdown();
		while (!finalES.isTerminated()) {
		}
		
		// evaluate
		Evaluator evaluator = new Evaluator(integratedScoreMap);
		evaluator.evaluate();
	}
	
	/** compute length score for source code file */
	public static void computeLengthScore() {
		logger.info("Calculating source code file length score...");
		HashMap<String, Integer> corpusLensTable = new HashMap<String, Integer>();
		int max = Integer.MIN_VALUE, min = Integer.MAX_VALUE;
		for (Entry<String, SourceCode> entry : sourceCodeMap.entrySet()) {
			String content = entry.getValue().getSourceCodeCorpus().getContent();
			int lens = content.split(" ").length;
			corpusLensTable.put(entry.getKey(), lens);
			if (lens > max) 
				max = lens;
			if (lens < min)
				min = lens;
		}
		for (Entry<String, Integer> entry : corpusLensTable.entrySet()) {
			String filePath = entry.getKey();
			int lens = entry.getValue();
			double score = 0.0D;
			double nor = getNormalizedValue(lens, max, min);
			if (lens != 0) {
				score = getLengthScore(nor);
			} else {
				score = 0.0D;
			}
			sourceCodeMap.get(filePath).setLengthScore(score);
		}
	}
	
	/** Get normalized value of x from Max. to min. */
	private static double getNormalizedValue(double x, double max, double min) {
		return (x - min) / (max - min);
	}

	/** Get length score */
	public static double getLengthScore(double len) {
		return 1.0D / (1.0D + Math.exp(-len));
	}
	
	// Worker thread for calculating VSMScore
	private static class VSMScoreCalculator implements Callable<HashMap<String, Double>> {

		private BugReport br;
		private int index;
		
		public VSMScoreCalculator(BugReport br, int index) {
			this.br = br;
			this.index = index;
		}
		
		@Override
		public HashMap<String, Double> call() throws Exception {
			if (index % 100 == 0) 
				logger.info(index + " bug reports handled.");
			HashMap<String, Double> VSMScoreMap = calculateVSMScore(br, sourceCodeMap);
			return VSMScoreMap;
		}
		
	}
	
	// Worker thread for calculating SimiScore
	private static class SimiScoreCalculator implements Callable<HashMap<String, Double>> {

		private BugReport br;
		private int index;
		
		public SimiScoreCalculator(BugReport br, int index) {
			this.br = br;
			this.index = index;
		}
		
		@Override
		public HashMap<String, Double> call() throws Exception {
			if (index % 100 == 0) 
				logger.info(index + " bug reports handled.");
			HashMap<String, Double> simiScoreMap = calculateSimiScore(br, bugReports);
			return simiScoreMap;
		}
		
	}
	
	// Worker thread for evaluating
	private static class FinalScoreCalculator implements Callable<List<IntegratedScore>> {

		private BugReport br;
		private HashMap<String, Double> VSMScoreMap;
		private HashMap<String, Double> simiScoreMap;
		
		public FinalScoreCalculator(BugReport br, HashMap<String, Double> VSMScoreMap, HashMap<String, Double> simiScoreMap) {
			this.br = br;
			this.VSMScoreMap = VSMScoreMap;
			this.simiScoreMap = simiScoreMap;
		}
		
		@Override
		public List<IntegratedScore> call() throws Exception {
			List<IntegratedScore> integratedScores = calculateFinalScore(br, VSMScoreMap, simiScoreMap);
			return integratedScores;
		}
		
	}
	
	public static HashMap<String, Double> calculateVSMScore(BugReport br, HashMap<String, SourceCode> sourceCodeMap) {
		HashMap<String, Double> VSMScoreMap = new HashMap<String, Double>();
		double max = Double.MIN_VALUE, min = Double.MAX_VALUE;
		for (Entry<String, SourceCode> entry : sourceCodeMap.entrySet()) {
			double VSMScore;
			if (usingRevisedVSMModels)
				VSMScore = sim.vsmSimilarity(br, entry.getValue()) * entry.getValue().getLengthScore();
			else 
				VSMScore = sim.vsmSimilarity(br, entry.getValue());
			if (VSMScore > max)
				max = VSMScore;
			if (VSMScore < min)
				min = VSMScore;
			VSMScoreMap.put(entry.getKey(), VSMScore);
		}
		// normalization
		for (Entry<String, Double> entry : VSMScoreMap.entrySet()) {
			VSMScoreMap.put(entry.getKey(), getNormalizedValue(entry.getValue(), max, min));
		}
		return VSMScoreMap;
	}
	
	public static HashMap<String, Double> calculateSimiScore(BugReport br, HashMap<Integer, BugReport> bugReports) {
		HashMap<String, Double> simiScoreMap = new HashMap<String, Double>();
		for (Entry<Integer, BugReport> entry : bugReports.entrySet()) {
			if (entry.getKey() == br.getBugID())
				continue;
			int numOfFixedFiles = entry.getValue().getFixedFiles().size();
			double s = sim.vsmSimilarity(br, entry.getValue());
			for (String fixedFile : entry.getValue().getFixedFiles()) {
				if (!simiScoreMap.containsKey(fixedFile)) 
					simiScoreMap.put(fixedFile, 0.0);
				else {
					simiScoreMap.put(fixedFile, simiScoreMap.get(fixedFile) + s / numOfFixedFiles);
				}
			}
		}
		// normalization
		double max = Double.MIN_VALUE;
		for (Entry<String, Double> entry : simiScoreMap.entrySet()) {
			if (entry.getValue() > max)
				max = entry.getValue();
		}
		for (Entry<String, Double> entry : simiScoreMap.entrySet()) {
			simiScoreMap.put(entry.getKey(), getNormalizedValue(entry.getValue(), max, 0.0));
		}
		return simiScoreMap;
	}
	
	public static List<IntegratedScore> calculateFinalScore(BugReport br, HashMap<String, Double> rVSMScoreMap, HashMap<String, Double> simiScoreMap) {
		List<IntegratedScore> integratedScores = new ArrayList<IntegratedScore>();
		for (Entry<String, Double> rVSMScoreEntry : rVSMScoreMap.entrySet()) {
			double[] features = new double[2];
			features[0] = rVSMScoreEntry.getValue();
			if (simiScoreMap != null && simiScoreMap.containsKey(rVSMScoreEntry.getKey())) {
				features[1] = simiScoreMap.get(rVSMScoreEntry.getKey());
			} else {
				features[1] = 0;
			} 
			IntegratedScore integratedScore = new IntegratedScore(rVSMScoreEntry.getKey(), br.isModified(rVSMScoreEntry.getKey()), features);
			integratedScore.setIntegratedScore(calculateIntegratedScore(features));
			integratedScores.add(integratedScore);
		}
		integratedScores.sort(new IntegratedScore.IntegratedScoreComparator());
		return integratedScores;
	}
	
	public static double calculateIntegratedScore(double[] features) {
		return features[0] * (1.0 - alpha) + features[1] * alpha;
	}

}
