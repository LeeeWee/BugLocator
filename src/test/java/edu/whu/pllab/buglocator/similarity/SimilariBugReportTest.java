package edu.whu.pllab.buglocator.similarity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.TreeSet;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.common.SimilarBugReport;
import edu.whu.pllab.buglocator.common.TokenScore;
import edu.whu.pllab.buglocator.rankingmodel.RankBySourceCodeSimilarity;
import edu.whu.pllab.buglocator.utils.BugReportsSplitter;
import edu.whu.pllab.buglocator.utils.FileUtil;
import edu.whu.pllab.buglocator.vectorizer.BugReportTfidfVectorizer;

public class SimilariBugReportTest {
	private static final Logger logger = LoggerFactory.getLogger(RankBySourceCodeSimilarity.class);

	public static Property property;
	public static final int TOP_SIMILAR_BUG_REPORTS = 0;
	public static final int BR_BR_SIMILARITY = Similarity.VSM;
	
	private static BugReportRepository brRepo;
	private static HashMap<Integer, BugReport> trainingBugReports;
	private static HashMap<Integer, BugReport> testBugReports;
	
	private static List<String> traingBRsFixedFiles;

	private static String output;
	
	public static void main(String[] args) throws Exception {
//		setup();
//		recallTest();
//		singleBugReportTest();
		recallTestForAllBatch(10);
	}
	
	public static void setup() throws Exception {
		
		property = Property.loadInstance();
		
		output = new File(property.getWorkingDir(), "similarBugReportsTest.txt").getAbsolutePath();
		
		// initialize bugReport Repository
		brRepo = new BugReportRepository();
		
		// split bug reports 
		BugReportsSplitter splitter = new BugReportsSplitter(brRepo.getBugReports(), 10);
		List<HashMap<Integer, BugReport>> bugReportsMapList = splitter.getBugReportsMapList();
		List<String> preCommitIDList = splitter.getPreCommitIDList(); // last committed bug report's commitID for each bug reports map 
	
		int i = 0;
		logger.info(String.format("Training on %d-th fold, test on %d-th fold", i, i+1));
		trainingBugReports = bugReportsMapList.get(i);
		testBugReports = bugReportsMapList.get(i + 1);
		
		
		// train tfidf model using training bug reports
		BugReportTfidfVectorizer brVectorizer = new BugReportTfidfVectorizer(trainingBugReports);
		brVectorizer.train();
		// TfidfVectorizer training and test bug reports
		brVectorizer.calculateTokensWeight(trainingBugReports);
		brVectorizer.calculateTokensWeight(testBugReports);
		
		HashSet<String> sourceCodeFiles = getAllSourceCodeFiles(preCommitIDList.get(i+1));
		filterBugReports(testBugReports, sourceCodeFiles);
		
		traingBRsFixedFiles = new ArrayList<String>();
		for (BugReport bugReport : trainingBugReports.values()) {
			traingBRsFixedFiles.addAll(bugReport.getFixedFiles());
		}
		
	}
	
	
	public static void singleBugReportTest() throws Exception {
		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.println("Input bug report id to get similar bug reports and candidate files...");
			String line = scanner.nextLine();
			if (line.equals("q"))
				break;
			Integer bugID = 0;
			try {
				 bugID = Integer.parseInt(line);
			} catch (Exception e) {
				System.err.println("Input string need to be integer type!");
				continue;
			}
			if (!(trainingBugReports.containsKey(bugID) || testBugReports.containsKey(bugID))) {
				System.err.println("Training and test bug reports do not contain input bug report!");
				continue;
			}
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(output));
			// bug report info
			BugReport bugReport = trainingBugReports.containsKey(bugID) ? trainingBugReports.get(bugID) : testBugReports.get(bugID);
			List<SimilarBugReport> similarBugReports = getSimilarBugReports(bugReport, 20);
			writer.write(bugReportInfo(bugReport));
			writer.write("isRecalled: " + isRecalled(bugReport) + "\n\n");
			// similar bug reports info
			writer.write("\nSimilari Bug Reports:\n");
			for (SimilarBugReport similarBugReport : similarBugReports) {
				writer.write("similarity: " + similarBugReport.getSimilarity() + "\n");
				writer.write(bugReportInfo(similarBugReport));
				writer.write("\n");
			}
			// candidate files info
			HashMap<String, Double> candidates = calculateCollaborativeFilteringScore(bugReport);
			writer.write("Candidate files:\n");
			List<Entry<String, Double>> sortedCandidates = sortMapByValue(candidates, false);
			for (Entry<String, Double> entry : sortedCandidates.subList(0, 20)) {
				boolean isModified = bugReport.isModified(entry.getKey());
				writer.write("\t" + isModified + "\t" + entry.getValue() + "\t" + entry.getKey() + "\n");
			}
			writer.close();
		}
		scanner.close();
	}
	
	
	/** calculate collaborative filter score for given bug report */
	public static HashMap<String, Double> calculateCollaborativeFilteringScore(BugReport br) {
		HashMap<String, Double> collaborativeFilteringScoreMap = new HashMap<String, Double>();
		List<SimilarBugReport> similarBugReports = getSimilarBugReports(br, TOP_SIMILAR_BUG_REPORTS);
		for (SimilarBugReport similarBugReport : similarBugReports) {
			for (String fixedFile : similarBugReport.getFixedFiles()) {
				if (!collaborativeFilteringScoreMap.containsKey(fixedFile))
					collaborativeFilteringScoreMap.put(fixedFile, 0.0);
				else 
					collaborativeFilteringScoreMap.put(fixedFile,
							collaborativeFilteringScoreMap.get(fixedFile) + similarBugReport.getSimilarity());
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
	public static List<SimilarBugReport> getSimilarBugReports(BugReport br, int top) {
		List<SimilarBugReport> similarBugReports = new ArrayList<SimilarBugReport>();
		Similarity sim = new Similarity();
		PriorityQueue<SimilarBugReport> heap = new PriorityQueue<SimilarBugReport>(trainingBugReports.size(),
				new SimilarBugReport.SimilarityComparator());
		for (Entry<Integer, BugReport> entry : trainingBugReports.entrySet()) {
			if (entry.getKey().equals(br.getBugID()))
				continue;
			double similarity = sim.similarity(br, entry.getValue(), BR_BR_SIMILARITY);
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
	
	public static HashSet<String> getAllSourceCodeFiles(String version) {
		// reset source code repository to the i-th lastCommitID version, and get all files
		String sourceCodeDir = property.getSourceCodeDir();
		int sourceCodeDirNameLength = new File(sourceCodeDir).getAbsolutePath().length();
		// reset to given commitID version
		try {
			// initialize git repository
			Repository repo = FileRepositoryBuilder.create(new File(sourceCodeDir, ".git"));
			@SuppressWarnings("resource")
			Git git = new Git(repo);
			git.reset().setMode(ResetType.HARD).setRef(version).call();
		} catch (Exception e) {
			e.printStackTrace();
		}
		HashSet<String> sourceCodeFiles = new HashSet<String>();
		for (String filePath : FileUtil.getAllFiles(sourceCodeDir, ".java")) 
			sourceCodeFiles.add(filePath.substring(sourceCodeDirNameLength + 1).replaceAll("\\\\", "/"));
		return sourceCodeFiles;
	}
	
	
	/** filter bug reports whose all fixed files do not exist in sourceCodeMap */
	public static void filterBugReports(HashMap<Integer, BugReport> bugReportsMap, HashSet<String> sourceCodeFiles) {
		Iterator<Entry<Integer, BugReport>> iter = bugReportsMap.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Integer, BugReport> entry = iter.next();
			BugReport bugReport = entry.getValue();
			boolean invalid = true;
			for (String fixedFile : bugReport.getFixedFiles()) {
				if (sourceCodeFiles.contains(fixedFile))
					invalid = false;
			}
			if (invalid) 
				iter.remove();
		}
	}
	
	public static void recallTest() {
		int recalledBR = 0;
		int allRecalledBR = 0;
		int recall = 0;
		int total = 0;
		for (BugReport bugReport : testBugReports.values()) {
			TreeSet<String> fixedFiles = bugReport.getFixedFiles();
			int n = 0;
			for (String fixedFile : fixedFiles) {
				total++;
				if (traingBRsFixedFiles.contains(fixedFile)) {
					n++;
					recall++;
				}
			}
			
			if (n > 0) {
				recalledBR++;
				if (n == fixedFiles.size()) 
					allRecalledBR++;
			}
		}
		System.out.println("Test bug reports size: " + testBugReports.size());
		System.out.println("Recalled bug reports: " + recalledBR);
		System.out.println("All recalled bug reports: " + allRecalledBR);
		System.out.println("Recall files / total files : " + recall + " / " + total + " = " + (double) recall / total);
	}
	
	public static void recallTestForAllBatch(int batches) {
		property = Property.loadInstance();
		// initialize bugReport Repository
		brRepo = new BugReportRepository();
		BugReportsSplitter splitter = new BugReportsSplitter(brRepo.getBugReports(), batches);
		List<HashMap<Integer, BugReport>> bugReportsMapList = splitter.getBugReportsMapList();
		List<String> preCommitIDList = splitter.getPreCommitIDList(); // last committed bug report's commitID for each bug reports map 
		for (int i = 0; i < batches - 1; i++) {
			System.out.println("Test for " + (i+1) + "-th batch...");
			trainingBugReports = bugReportsMapList.get(i);
			testBugReports = bugReportsMapList.get(i + 1);
			
			HashSet<String> sourceCodeFiles = getAllSourceCodeFiles(preCommitIDList.get(i + 1));
			filterBugReports(testBugReports, sourceCodeFiles);
			
			traingBRsFixedFiles = new ArrayList<String>();
			for (BugReport bugReport : trainingBugReports.values()) {
				traingBRsFixedFiles.addAll(bugReport.getFixedFiles());
			}
			
			recallTest();
		}
	}
	
	public static boolean isRecalled(BugReport bugReport) {
		boolean isRecalled = false;
		TreeSet<String> fixedFiles = bugReport.getFixedFiles();
		for (String fixedFile : fixedFiles) {
			if (traingBRsFixedFiles.contains(fixedFile)) {
				isRecalled = true;
			}
		}
		return isRecalled;
	} 
	
	public static String bugReportInfo(BugReport bugReport) {
		StringBuilder builder = new StringBuilder();
		builder.append("BugID: " + bugReport.getBugID() + "\n");
		builder.append("\tsummary: " + bugReport.getSummary() + "\n");
		builder.append("\tdescription: " + bugReport.getDescription() + "\n");
		builder.append("\tcorpus: " + bugReport.getBugReportCorpus().getContent() + "\n");
		builder.append("\tcontentNorm: " + bugReport.getBugReportCorpus().getContentNorm() + "\n");
		builder.append("\tcontentTokens: ");
		for (TokenScore tokenScore : bugReport.getBugReportCorpus().getContentTokens().values()) {
			builder.append(String.format("%s(%f,%f,%f) ", tokenScore.getToken(), tokenScore.getTf(),
					tokenScore.getIdf(), tokenScore.getTokenWeight()));
		}
		builder.append("\n\tModifiedFiles: \n");
		for (String file : bugReport.getFixedFiles()) {
			builder.append("\t\t" + file + "\n");
		}
		return builder.toString();
	}
	
	public static List<Entry<String, Double>> sortMapByValue(Map<String, Double> map, final boolean isAscending) {
		  Comparator<Entry<String, Double>> ValueComparator = new Comparator<Entry<String, Double>>() {
		        @Override
		        public int compare(Entry<String, Double> o1,
		                Entry<String, Double> o2) {
		        	if (isAscending)
		        		return o1.getValue().compareTo(o2.getValue());
		        	else 
		        		return o2.getValue().compareTo(o1.getValue());
		        }
		    };
		 // convert map to list
	    List<Entry<String, Double>> list = new ArrayList<Entry<String,Double>>(map.entrySet());
	    
		Collections.sort(list, ValueComparator);
		
		return list;
	}
}
