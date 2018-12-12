package edu.whu.pllab.buglocator.similarityrecommender;

import edu.whu.pllab.buglocator.common.TokenScore.ScoreType;

public class RecommenderProperty {
	
	// similarity type
	public static final int RVSM_NTFIDF		 = 0x000001;
	public static final int RVSM_WFIDF		 = 0x000010;
	public static final int STRUCTURE_NTFIDF = 0x000100;
	public static final int STRUCTURE_WFIDF	 = 0x001000;
	public static final int SYMMETRIC_NTFIDF = 0x010000;
	public static final int SYMMETRIC_WFIDF	 = 0x100000;
	
	// locator result path name
	public static final String RVSM_NTFIDF_RESULT_PATH = "RVSM(NTFIDF)_Result";
	public static final String RVSM_WFIDF_RESULT_PATH = "RVSM(WFIDF)_Result";
	public static final String STRUCTURE_NTFIDF_RESULT_PATH = "STRUCTURE(NTFIDF)_Result";
	public static final String STRUCTURE_WFIDF_RESULT_PATH = "STRUCTURE(WFIDF)_Result";
	public static final String SYMMETRIC_NTFIDF_RESULT_PATH = "SYMMETRIC(NTFIDF)_Result";
	public static final String SYMMETRIC_WFIDF_RESULT_PATH = "SYMMETRIC(WFIDF)_Result";
	
	public static final String COMPARISON_DIR = "SimilarityComparison";
	
	// path to save each bug report and related best similarity method name
	public static final String COMPARISON_RESULT = "comparison_result.txt";
	
	// path to save evaluation result with each similarity method
	public static final String LOCALIZATION_RESULT = "localization_result.txt";
	
	public static final String SELECTION_PATH = "similaritySelection.txt";
	
	// all similarities type
	public static int[] similarities = new int[] { RVSM_NTFIDF, RVSM_WFIDF, STRUCTURE_NTFIDF, STRUCTURE_WFIDF,
			SYMMETRIC_NTFIDF, SYMMETRIC_WFIDF };
	
	// all similarities name
	public static String[] similaritiesNames = new String[] {"RVSM_NTFIDF", "RVSM_WFIDF", "STRUCTURE_NTFIDF", "STRUCTURE_WFIDF",
			"SYMMETRIC_NTFIDF", "SYMMETRIC_WFIDF" };
	
	// paths to predict results of these similarities
	public static String[] resultPaths = new String[] { RVSM_NTFIDF_RESULT_PATH, RVSM_WFIDF_RESULT_PATH,
			STRUCTURE_NTFIDF_RESULT_PATH, STRUCTURE_WFIDF_RESULT_PATH, SYMMETRIC_NTFIDF_RESULT_PATH,
			SYMMETRIC_WFIDF_RESULT_PATH };
	
	// classify similarity methods by scoreType, for the sake of convenience in predicting result for different similarity methods
	public static ScoreType[] scoreTypes = new ScoreType[] {ScoreType.NTFIDF, ScoreType.WFIDF};
	public static int[][] classifiedSimilarities = new int[][] {
			{RVSM_NTFIDF, STRUCTURE_NTFIDF, SYMMETRIC_NTFIDF}, 
			{RVSM_WFIDF, STRUCTURE_WFIDF, SYMMETRIC_WFIDF}};
	public static String[][] classifiedSimilaritiesNames = new String[][] {
			{"RVSM_NTFIDF", "STRUCTURE_NTFIDF", "SYMMETRIC_NTFIDF"},
			{"RVSM_WFIDF", "STRUCTURE_WFIDF", "SYMMETRIC_WFIDF"}};
	public static String[][] classifiedResultPaths = new String[][] {
			{RVSM_NTFIDF_RESULT_PATH, STRUCTURE_NTFIDF_RESULT_PATH, SYMMETRIC_NTFIDF_RESULT_PATH},
			{RVSM_WFIDF_RESULT_PATH, STRUCTURE_WFIDF_RESULT_PATH, SYMMETRIC_WFIDF_RESULT_PATH}};
	
	// get the position of first dimension in classifiedSimilarities array
	public static int getFirstIndex(int similarityType) {
		if (similarityType == RVSM_NTFIDF || similarityType == STRUCTURE_NTFIDF || similarityType == SYMMETRIC_NTFIDF)
			return 0;
		else if (similarityType == RVSM_WFIDF || similarityType == STRUCTURE_WFIDF || similarityType == SYMMETRIC_WFIDF)
			return 1;
		else 
			return -1;
	}
	
	// get the position of second dimension in classifiedSimilarities array
	public static int getSecondIndex(int similarityType, int firstIndex) {
		int index = -1;
		if (firstIndex == 0) {
			for (int i = 0; i < classifiedSimilarities[0].length; i++) {
				if (similarityType == classifiedSimilarities[0][i]) {
					index = i;
					break;
				}
			}
		} else if (firstIndex == 1) {
			for (int i = 0; i < classifiedSimilarities[1].length; i++) {
				if (similarityType == classifiedSimilarities[1][i]) {
					index = i;
					break;
				}
			}
		}
		return index;
	}
}
