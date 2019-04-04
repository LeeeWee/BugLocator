package edu.whu.pllab.buglocator.techniques;

import java.io.File;
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
import edu.whu.pllab.buglocator.common.TokenScore.ScoreType;
import edu.whu.pllab.buglocator.evaluation.Evaluator;
import edu.whu.pllab.buglocator.rankingmodel.IntegratedScore;
import edu.whu.pllab.buglocator.similarity.Similarity;
import edu.whu.pllab.buglocator.vectorizer.BugReportTfidfVectorizer;
import edu.whu.pllab.buglocator.vectorizer.SourceCodeTfidfVectorizer;

public class BLUiR {

	private static final Logger logger = LoggerFactory.getLogger(BLUiR.class);
	
	// configures
	private static final double alpha = 0.2;
	private static boolean usingSimilarBugReports = false;
	
	private static Similarity sim = new Similarity();
	private static String[] products = {"BugLocator_SWT", "BugLocator_AspectJ", "BugLocator_Eclipse", "BugLocator_ZXing"};
	
	private static HashMap<String, SourceCode> sourceCodeMap;
	private static HashMap<Integer, BugReport> bugReports;
	
	public static void main(String[] args) throws Exception {
		for (String product : products) {
			logger.info("Current Product: " + product);
			locateBug(product);
		}
	}
	
	public static void locateBug(String product) throws Exception {
		Property.USE_STRUCTURED_INFORMATION = true;
		Property property = Property.loadInstance(product);
		property.setCodeRepositoryXMLPath(new File(property.getWorkingDir(), "codeRepository_structured.xml").getAbsolutePath());

		// record evaluate result
		// BufferedWriter logWriter = new BufferedWriter(new FileWriter(property.getEvaluateLogPath()));
		
		// initialize bugReport repository and source code repository
		BugReportRepository brRepo = new BugReportRepository();
		SourceCodeRepository codeRepo = new SourceCodeRepository();
		codeRepo.saveSourceCodeRepoToXML(property.getCodeRepositoryXMLPath(), product);
		
		sourceCodeMap = codeRepo.getSourceCodeMap();
		bugReports = brRepo.getBugReports();
		
		execute();
		
	}
	
	public static void execute() throws InterruptedException, ExecutionException {
		HashMap<BugReport, List<IntegratedScore>> integratedScoreMap = new HashMap<BugReport, List<IntegratedScore>>(); 
		HashMap<BugReport, HashMap<String, Double>> strucSimiMap = null;
		HashMap<BugReport, HashMap<String, Double>> simiScoreMap = null;
		
		// calculate source code tokens weight
		SourceCodeTfidfVectorizer codeVectorizer = new SourceCodeTfidfVectorizer(sourceCodeMap);
		codeVectorizer.setTokenScoreType(ScoreType.OKAPITFIDF);
		codeVectorizer.train();
		codeVectorizer.calculateTokensWeight(sourceCodeMap);
		
		// calculate bug reports tokens weight using code corpus's tfidf model
		BugReportTfidfVectorizer brVectorizer = new BugReportTfidfVectorizer(codeVectorizer.getTfidf());
		brVectorizer.setTokenScoreType(ScoreType.OKAPITFIDF);
		brVectorizer.calculateTokensWeight(bugReports);
		
		// calculate strucSimiMap
		int count = 0;
		strucSimiMap = new HashMap<BugReport, HashMap<String, Double>>();
		ExecutorService vsmES = Executors.newFixedThreadPool(Property.THREAD_COUNT);
		for (Entry<Integer, BugReport> brEntry : bugReports.entrySet()) {
			count++;
			Callable<HashMap<String, Double>> strucSimiCalculator = new StructureSimilarityCalculator(brEntry.getValue(), count);
			Future<HashMap<String, Double>> future = vsmES.submit(strucSimiCalculator);
			strucSimiMap.put(brEntry.getValue(), future.get());
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
				finalScoreCalculator = new FinalScoreCalculator(br, strucSimiMap.get(br), simiScoreMap.get(br));
			else 
				finalScoreCalculator = new FinalScoreCalculator(br, strucSimiMap.get(br), null);
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
	
	// Worker thread for calculating StructureSimilarity
	private static class StructureSimilarityCalculator implements Callable<HashMap<String, Double>> {

		private BugReport br;
		private int index;
		
		public StructureSimilarityCalculator(BugReport br, int index) {
			this.br = br;
			this.index = index;
		}
		
		@Override
		public HashMap<String, Double> call() throws Exception {
			if (index % 100 == 0) 
				logger.info(index + " bug reports handled.");
			HashMap<String, Double> strucSimiMap = calculateStructureSimilarityScore(br, sourceCodeMap);
			return strucSimiMap;
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
		private HashMap<String, Double> strucSimiMap;
		private HashMap<String, Double> simiScoreMap;
		
		public FinalScoreCalculator(BugReport br, HashMap<String, Double> strucSimiMap, HashMap<String, Double> simiScoreMap) {
			this.br = br;
			this.strucSimiMap = strucSimiMap;
			this.simiScoreMap = simiScoreMap;
		}
		
		@Override
		public List<IntegratedScore> call() throws Exception {
			List<IntegratedScore> integratedScores = calculateFinalScore(br, strucSimiMap, simiScoreMap);
			return integratedScores;
		}
		
	}

	public static HashMap<String, Double> calculateStructureSimilarityScore(BugReport br, HashMap<String, SourceCode> sourceCodeMap) {
		HashMap<String, Double> strucSimiEntry = new HashMap<String, Double>();
		double max = Double.MIN_VALUE, min = Double.MAX_VALUE;
		for (Entry<String, SourceCode> entry : sourceCodeMap.entrySet()) {
			double strucSim;
//			strucSim = sim.vsmSimilarity(br, entry.getValue()) * entry.getValue().getLengthScore();
			strucSim = sim.BM25StructuralSimilarity(br, entry.getValue());
			if (strucSim > max)
				max = strucSim;
			if (strucSim < min)
				min = strucSim;
			strucSimiEntry.put(entry.getKey(), strucSim);
		}
		// normalization
		for (Entry<String, Double> entry : strucSimiEntry.entrySet()) {
			strucSimiEntry.put(entry.getKey(), getNormalizedValue(entry.getValue(), max, min));
		}
		return strucSimiEntry;
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
	
	public static List<IntegratedScore> calculateFinalScore(BugReport br, HashMap<String, Double> strucSimiMap, HashMap<String, Double> simiScoreMap) {
		List<IntegratedScore> integratedScores = new ArrayList<IntegratedScore>();
		for (Entry<String, Double> strucSimEntry : strucSimiMap.entrySet()) {
			double[] features = new double[2];
			features[0] = strucSimEntry.getValue();
			if (simiScoreMap != null && simiScoreMap.containsKey(strucSimEntry.getKey())) {
				features[1] = simiScoreMap.get(strucSimEntry.getKey());
			} else {
				features[1] = 0;
			} 
			IntegratedScore integratedScore = new IntegratedScore(strucSimEntry.getKey(), br.isModified(strucSimEntry.getKey()), features);
			integratedScore.setIntegratedScore(calculateIntegratedScore(features));
			integratedScores.add(integratedScore);
		}
		integratedScores.sort(new IntegratedScore.IntegratedScoreComparator());
		return integratedScores;
	}
	
	public static double calculateIntegratedScore(double[] features) {
		return features[0] * (1.0 - alpha) + features[1] * alpha;
	}
	
	/** Get normalized value of x from Max. to min. */
	private static double getNormalizedValue(double x, double max, double min) {
		return (x - min) / (max - min);
	}
}
