package edu.whu.pllab.buglocator.tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class SimilarityComparator {
	
	public static class PredictResult {
		public int bugID;
		public HashMap<String, Integer> filesRankMap;
		
		public PredictResult() {
			this.bugID = 0;
			this.filesRankMap = new HashMap<String, Integer>();
		}
		
		public PredictResult(int bugID) {
			this.bugID = bugID;
			this.filesRankMap = new HashMap<String, Integer>();
		}
		
		/** the position of the first relevant document in the ranked list */
		public int getFirstRank() {
			return Collections.min(filesRankMap.values());
		}
		
		/** sort the positions of relevant document */
		public List<Integer> getSortedRank() {
			List<Integer> ranks = new ArrayList<Integer>(filesRankMap.values());
			Collections.sort(ranks);
			return ranks;
		}
		
		public int numOfFiles() {
			return filesRankMap.size();
		}
		
		/**
		 * Compare this predictResult with input predictResult
		 * @return return 1 when this predictResult is better than the second
		 * predictResult, return -1 when the second predictResult is better, and
		 * 0 when this and the second predictResult's rank list are equal
		 */
		public int compareTo(PredictResult pr) {
			List<Integer> mySortedRank = getSortedRank();
			List<Integer> secondSortedRank = pr.getSortedRank();
			int size = mySortedRank.size();
			for (int i = 0; i < size; i++) {
				if (mySortedRank.get(i) < secondSortedRank.get(i))
					return 1;
				if (mySortedRank.get(i) > secondSortedRank.get(i))
					return -1;
			}
			return 0;
		}
	}
	
	private static final Logger logger = LoggerFactory.getLogger(SimilarityComparator.class);

	// similarity type
	public static final int RVSM_NTFIDF		 = 0x000001;
	public static final int RVSM_WFIDF		 = 0x000010;
	public static final int STRUCTURE_NTFIDF = 0x000100;
	public static final int STRUCTURE_WFIDF	 = 0x001000;
	public static final int SYMMETRIC_NTFIDF = 0x010000;
	public static final int SYMMETRIC_WFIDF	 = 0x100000;
	
	public static final String RVSM_NTFIDF_RESULT_PATH = "RVSM(NTFIDF)_Result";
	public static final String RVSM_WFIDF_RESULT_PATH = "RVSM(WFIDF)_Result";
	public static final String STRUCTURE_NTFIDF_RESULT_PATH = "STRUCTURE(NTFIDF)_Result";
	public static final String STRUCTURE_WFIDF_RESULT_PATH = "STRUCTURE(WFIDF)_Result";
	public static final String SYMMETRIC_NTFIDF_RESULT_PATH = "SYMMETRIC(NTFIDF)_Result";
	public static final String SYMMETRIC_WFIDF_RESULT_PATH = "SYMMETRIC(WFIDF)_Result";
	
	public static int[] similarities = new int[] { RVSM_NTFIDF, RVSM_WFIDF, STRUCTURE_NTFIDF, STRUCTURE_WFIDF,
			SYMMETRIC_NTFIDF, SYMMETRIC_WFIDF };
	public static String[] resultPaths = new String[] { RVSM_NTFIDF_RESULT_PATH, RVSM_WFIDF_RESULT_PATH,
			STRUCTURE_NTFIDF_RESULT_PATH, STRUCTURE_WFIDF_RESULT_PATH, SYMMETRIC_NTFIDF_RESULT_PATH,
			SYMMETRIC_WFIDF_RESULT_PATH };
	
	public static final String COMPARISON_RESULT = "comparison_result";

	public static List<PredictResult> loadPredictResult(File file) throws Exception {
		List<PredictResult> predictResultList = new ArrayList<PredictResult>();
		logger.info("Loading predict result data from " + file);
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder domBuilder = domFactory.newDocumentBuilder();
		InputStream is = new FileInputStream(file);
		Document doc = domBuilder.parse(is);
		Element root = doc.getDocumentElement();
		NodeList nodeList = root.getChildNodes();
		if (nodeList != null) {
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node prNode = nodeList.item(i);
				if (prNode.getNodeType() == Node.ELEMENT_NODE) {
					PredictResult predictResult = new PredictResult();
					// parse single predict result
					for (Node node = prNode.getFirstChild(); node != null; node = node.getNextSibling()) {
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							if (node.getNodeName().equals("bug_id")) {
								predictResult.bugID = Integer.parseInt(node.getTextContent());
							} 
							if (node.getNodeName().equals("result")) {
								String result = node.getTextContent();
								String filesRanks[] = result.split("\n");
								for (String rank : filesRanks) {
									String values[] = rank.split(":");
									predictResult.filesRankMap.put(values[1], Integer.parseInt(values[0]));
								}
							}
						}
					}
					predictResultList.add(predictResult);
				}
			}
		}
		return predictResultList;
	}
	
	/**
	 * compare the given predict results map
	 * @param predictResultsMap map similarityType to corresponding predictResult list
	 * @return map bugID to best similarity types, if some different similarity
	 *         type's predict results' are equal, calculate the value by bit or
	 *         operation of these similarity types' binary value
	 */
	public static HashMap<Integer, Integer> compareSimilarity(HashMap<Integer, List<PredictResult>> predictResultsMap) {
		logger.info("Comparating...");
		HashMap<Integer, Integer> bestPredictResult = new HashMap<Integer, Integer>();
		HashMap<Integer, PredictResult> tempPredictResult = new HashMap<Integer, PredictResult>();
		// iterate all similarity type
		for (Entry<Integer, List<PredictResult>> entry : predictResultsMap.entrySet()) {
			// iterate all predict result
			for (PredictResult pr : entry.getValue()) {
				if (!bestPredictResult.containsKey(pr.bugID)) {
					bestPredictResult.put(pr.bugID, entry.getKey());
					tempPredictResult.put(pr.bugID, pr);
				}
				else {
					// compare temp best predictResult to the new predictResult
					int compare = tempPredictResult.get(pr.bugID).compareTo(pr);
					if (compare > 0) {
						// do nothing
					} else if (compare == 0) {
						// add this similarity type to bestSimilarityType
						int bestSimilarityType = bestPredictResult.get(pr.bugID) | entry.getKey();
						bestPredictResult.put(pr.bugID, bestSimilarityType);
					} else {
						// replace current bestSimilarityType 
						bestPredictResult.put(pr.bugID, entry.getKey());
						tempPredictResult.put(pr.bugID, pr);
					}
				}
			}
		}
		
		return bestPredictResult;
	}
	
	/** given the directory containing the predict result files and output the comparison result */
	public static void compareSimilarity(String directory) throws Exception {
		HashMap<Integer, List<PredictResult>> predictResultsMap = new HashMap<Integer, List<PredictResult>>();
		for (int i = 0; i < similarities.length; i++) {
			List<PredictResult> predictResults = loadPredictResult(new File(directory, resultPaths[i]));
			predictResultsMap.put(similarities[i], predictResults);
		}
		HashMap<Integer, Integer> bestPredictResult = compareSimilarity(predictResultsMap);
		// write to comparison result path
		File output = new File(directory, COMPARISON_RESULT);
		BufferedWriter writer = new BufferedWriter(new FileWriter(output));
		// used to get the bug reports order
		List<PredictResult> predictResultList = predictResultsMap.values().iterator().next();
		List<Integer> bugReportsIDList = new ArrayList<Integer>();
		for (PredictResult predictResult : predictResultList) 
			bugReportsIDList.add(predictResult.bugID);
		// save comparison result
		for (Integer bugID : bugReportsIDList) {
			String bestSimilaritiesStr = "";
			int bestSimilarityType = bestPredictResult.get(bugID);
			if ((bestSimilarityType & RVSM_NTFIDF) > 0) 
				bestSimilaritiesStr += " RVSM_NTFIDF";
			if ((bestSimilarityType & RVSM_WFIDF) > 0) 
				bestSimilaritiesStr += " RVSM_WFIDF";
			if ((bestSimilarityType & STRUCTURE_NTFIDF) > 0) 
				bestSimilaritiesStr += " STRUCTURE_NTFIDF";
			if ((bestSimilarityType & STRUCTURE_WFIDF) > 0) 
				bestSimilaritiesStr += " STRUCTURE_WFIDF";
			if ((bestSimilarityType & SYMMETRIC_NTFIDF) > 0) 
				bestSimilaritiesStr += " SYMMETRIC_NTFIDF";
			if ((bestSimilarityType & SYMMETRIC_WFIDF) > 0) 
				bestSimilaritiesStr += " SYMMETRIC_WFIDF";
			writer.write(bugID + ":" + bestSimilaritiesStr + "\n");
		}
		writer.close();
	} 
	
	
	public static void main(String[] args) throws Exception {
		String directory = "D:\\data\\working\\AspectJ\\SimilarityComparison";
		compareSimilarity(directory);
	}
}
