package edu.whu.pllab.buglocator.similarity;

import java.util.HashMap;
import java.util.Map.Entry;

import org.nd4j.linalg.ops.transforms.Transforms;

import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.SourceCode;
import edu.whu.pllab.buglocator.common.TokenScore;

public class Similarity {
	
	public final static int VSM = 1;
	public final static int SYMMETRIC = 2;  // sim(T, S) = (sim(T->S) + sim(S->T)) / 2
	public final static int ASYMMETRIC = 3; // sim(T, S) = sim(T->S)
	public final static int PARAGRAPH_VECTOR = 4;

	/** similarity between BugReport and SourceCode by specific similarity type */
	public static double similarity(BugReport br, SourceCode code, int similarityType) {
		double sim = 0;
		switch (similarityType) {
			case VSM:
				sim = vsmSimilarity(br, code);
			case SYMMETRIC:
				sim = symmetricSimilarity(br, code);
			case ASYMMETRIC:
				sim = asymmetricSimilarity(br, code);
			case PARAGRAPH_VECTOR:
				sim = paragraphVectorSimilarity(br, code);
			default:
				sim = vsmSimilarity(br, code); 
		}
		return sim;
	}
	
	/** similarity between input BugReports by specific similarity type */
	public static double similarity(BugReport br1, BugReport br2, int similarityType) {
		double sim = 0;
		switch (similarityType) {
			case VSM:
				sim = vsmSimilarity(br1, br2);
			case SYMMETRIC:
				sim = symmetricSimilarity(br1, br2);
			case ASYMMETRIC:
				sim = asymmetricSimilarity(br1, br2);
			case PARAGRAPH_VECTOR:
				sim = paragraphVectorSimilarity(br1, br2);
			default:
				sim = vsmSimilarity(br1, br2); 
		}
		return sim;
	}
	
	/** Vector Space Model similarity between input BugReport and SourceCode */
	public static double vsmSimilarity(BugReport br, SourceCode code) {
		return vsmSimilarity(br.getBugReportCorpus().getContentTokens(), code.getSourceCodeCorpus().getContentTokens());
	}
	
	/** Vector Space Model similarity between input BugReports */
	public static double vsmSimilarity(BugReport br1, BugReport br2) {
		return vsmSimilarity(br1.getBugReportCorpus().getContentTokens(), br2.getBugReportCorpus().getContentTokens());
	}
	
	/** Vector Space Model similarity between input tokenScoreMaps */
	public static double vsmSimilarity(HashMap<String, TokenScore> tokenScoreMap1,
			HashMap<String, TokenScore> tokenScoreMap2) {
		double sim = 0;
		for (Entry<String, TokenScore> entry0 : tokenScoreMap1.entrySet()) {
			if (tokenScoreMap2.containsKey(entry0.getKey())) {
				sim += entry0.getValue().getTokenWeight() * tokenScoreMap2.get(entry0.getKey()).getTokenWeight();
			}
		}
		return sim;
	}
	
	/** symmetric similarity between input BugReport and SourceCode, sim(T, S) = (sim(T->S) + sim(S->T)) / 2 */
	public static double symmetricSimilarity(BugReport br, SourceCode code) {
		return symmetricSimilarity(br.getBugReportCorpus().getContentTokens(),
				code.getSourceCodeCorpus().getContentTokens());
	}
	
	/** symmetric similarity between input BugReports, sim(T, S) = (sim(T->S) + sim(S->T)) / 2 */
	public static double symmetricSimilarity(BugReport br1, BugReport br2) {
		return symmetricSimilarity(br1.getBugReportCorpus().getContentTokens(),
				br2.getBugReportCorpus().getContentTokens());
	}
	
	/** symmetric similarity between input tokenScoreMaps, sim(T, S) = (sim(T->S) + sim(S->T)) / 2 */
	public static double symmetricSimilarity(HashMap<String, TokenScore> tokenScoreMap1,
			HashMap<String, TokenScore> tokenScoreMap2) {
		return (asymmetricSimilarity(tokenScoreMap1, tokenScoreMap2)
				+ asymmetricSimilarity(tokenScoreMap2, tokenScoreMap1)) / 2;
	}
	
	/** symmetric similarity between input BugReport and SourceCode, sim(T, S) = sim(T->S) */
	public static double asymmetricSimilarity(BugReport br, SourceCode code) {
		return asymmetricSimilarity(br.getBugReportCorpus().getContentTokens(),
				code.getSourceCodeCorpus().getContentTokens());
	}
	
	/** symmetric similarity between input BugReports, sim(T, S) = sim(T->S) */
	public static double asymmetricSimilarity(BugReport br1, BugReport br2) {
		return asymmetricSimilarity(br1.getBugReportCorpus().getContentTokens(),
				br2.getBugReportCorpus().getContentTokens());
	}
	
	/** symmetric similarity between input tokenScoreMaps, sim(T, S) = sim(T->S) */
	public static double asymmetricSimilarity(HashMap<String, TokenScore> tokenScoreMap1,
			HashMap<String, TokenScore> tokenScoreMap2) {
		double simSum = 0.0;
		double weights = 0.0;
		if (tokenScoreMap1.size() == 0 || tokenScoreMap2.size() == 0) 
			return 0.0;
		for (Entry<String, TokenScore> entry1 : tokenScoreMap1.entrySet()) {
			double sim = 0.0;
			if(tokenScoreMap2.containsKey(entry1.getKey()))
				sim = 1.0;
			simSum += sim;
			weights += entry1.getValue().getTokenWeight();
		}
		return simSum / weights;
	}
	
	/** paragraph vector between input BugReport and SourceCode */
	public static double paragraphVectorSimilarity(BugReport br, SourceCode code) {
		return Transforms.cosineSim(br.getParagraphVector(), code.getParagraphVector());
	}
	
	/** paragraph vector between input BugReports */
	public static double paragraphVectorSimilarity(BugReport br1, BugReport br2) {
		return Transforms.cosineSim(br1.getParagraphVector(), br2.getParagraphVector());
	}
	
}
