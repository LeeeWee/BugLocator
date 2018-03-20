package edu.whu.pllab.buglocator.similarity;

import java.util.HashMap;
import java.util.Map.Entry;

import org.nd4j.linalg.ops.transforms.Transforms;

import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.Method;
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
				break;
			case SYMMETRIC:
				sim = symmetricSimilarity(br, code);
				break;
			case ASYMMETRIC:
				sim = asymmetricSimilarity(br, code);
				break;
			case PARAGRAPH_VECTOR:
				sim = paragraphVectorSimilarity(br, code);
				break;
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
				break;
			case SYMMETRIC:
				sim = symmetricSimilarity(br1, br2);
				break;
			case ASYMMETRIC:
				sim = asymmetricSimilarity(br1, br2);
				break;
			case PARAGRAPH_VECTOR:
				sim = paragraphVectorSimilarity(br1, br2);
				break;
			default:
				sim = vsmSimilarity(br1, br2); 
		}
		return sim;
	}
	
	/** similarity between BugReport and SourceCode by specific similarity type */
	public static double similarity(BugReport br, Method method, int similarityType) {
		double sim = 0;
		switch (similarityType) {
			case VSM:
				sim = vsmSimilarity(br, method);
				break;
			case SYMMETRIC:
				sim = symmetricSimilarity(br, method);
				break;
			case ASYMMETRIC:
				sim = asymmetricSimilarity(br, method);
				break;
			case PARAGRAPH_VECTOR:
				sim = paragraphVectorSimilarity(br, method);
				break;
			default:
				sim = vsmSimilarity(br, method); 
		}
		return sim;
	}
	
	/** Vector Space Model similarity between input BugReport and SourceCode */
	public static double vsmSimilarity(BugReport br, SourceCode code) {
		double sim = 0.0;
		HashMap<String, TokenScore> brTokens = br.getBugReportCorpus().getContentTokens();
		HashMap<String, TokenScore> codeTokens = code.getSourceCodeCorpus().getContentTokens();
		// if tokens map is empty return 0
		if (brTokens.size() == 0 || codeTokens.size() ==0)
			return sim;
		
		double brNorm = br.getBugReportCorpus().getContentNorm();
		double codeNorm = code.getSourceCodeCorpus().getContentNorm();
		// if norm value equal 0, return 0
		if (brNorm == 0 || codeNorm == 0)
			return sim;
		
		for (Entry<String, TokenScore> entry0 : brTokens.entrySet()) {
			if (codeTokens.containsKey(entry0.getKey())) 
				sim += entry0.getValue().getTokenWeight() * codeTokens.get(entry0.getKey()).getTokenWeight();
		}
		sim = sim / (brNorm * codeNorm);
		return sim;
	}
	
	/** Vector Space Model similarity between input BugReports */
	public static double vsmSimilarity(BugReport br1, BugReport br2) {
		double sim = 0;
		HashMap<String, TokenScore> br1Tokens = br1.getBugReportCorpus().getContentTokens();
		HashMap<String, TokenScore> br2Tokens = br2.getBugReportCorpus().getContentTokens();
		// if tokens map is empty return 0
		if (br1Tokens.size() == 0 || br2Tokens.size() ==0)
			return sim;
		
		double br1Norm = br1.getBugReportCorpus().getContentNorm();
		double br2Norm = br2.getBugReportCorpus().getContentNorm();
		// if norm value equal 0, return 0
		if (br1Norm == 0 || br2Norm == 0)
			return sim;
		
		for (Entry<String, TokenScore> entry0 : br1Tokens.entrySet()) {
			if (br2Tokens.containsKey(entry0.getKey())) 
				sim += entry0.getValue().getTokenWeight() * br2Tokens.get(entry0.getKey()).getTokenWeight();
		}
		sim = sim / (br1Norm * br2Norm);
		return sim;
	}
	
	/** Vector Space Model similarity between input BugReport and Method */
	public static double vsmSimilarity(BugReport br, Method method) {
		double sim = 0;
		
		HashMap<String, TokenScore> brTokens = br.getBugReportCorpus().getContentTokens();
		HashMap<String, TokenScore> methodTokens = method.getContentTokens();
		// if tokens map is empty return 0
		if (brTokens.size() == 0 || methodTokens.size() ==0)
			return sim;
		
		double brNorm = br.getBugReportCorpus().getContentNorm();
		double methodNorm = method.getContentNorm();
		// if norm value equal 0, return 0
		if (brNorm == 0 || methodNorm == 0)
			return sim;
		
		for (Entry<String, TokenScore> entry0 : brTokens.entrySet()) {
			if (methodTokens.containsKey(entry0.getKey())) 
				sim += entry0.getValue().getTokenWeight() * methodTokens.get(entry0.getKey()).getTokenWeight();
		}
		sim = sim / (br.getBugReportCorpus().getContentNorm() * method.getContentNorm());
		return sim;
	}
	
	/** Vector Space Model similarity between input tokenScoreMaps */
	public static double vsmSimilarity(HashMap<String, TokenScore> tokenScoreMap1,
			HashMap<String, TokenScore> tokenScoreMap2) {
		double sim = 0, norm1 = 0, norm2 = 0;
		for (Entry<String, TokenScore> entry1 : tokenScoreMap1.entrySet()) {
			norm1 += entry1.getValue().getTokenWeight() * entry1.getValue().getTokenWeight();
			if (tokenScoreMap2.containsKey(entry1.getKey())) {
				sim += entry1.getValue().getTokenWeight() * tokenScoreMap2.get(entry1.getKey()).getTokenWeight();
			}
		}
		for (Entry<String, TokenScore> entry2 : tokenScoreMap2.entrySet()) {
			norm2 += entry2.getValue().getTokenWeight() * entry2.getValue().getTokenWeight();
		}
		
		norm1 = Math.sqrt(norm1);
		norm2 = Math.sqrt(norm2);
		
		if (norm1 ==0 || norm2 == 0)
			return 0.0;
		return sim / (norm1 * norm2);
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
	
	/** symmetric similarity between input BugReport and method, sim(T, S) = (sim(T->S) + sim(S->T)) / 2 */
	public static double symmetricSimilarity(BugReport br, Method method) {
		return symmetricSimilarity(br.getBugReportCorpus().getContentTokens(),
				method.getContentTokens());
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
	
	/** symmetric similarity between input BugReport and method, sim(T, S) = sim(T->S) */
	public static double asymmetricSimilarity(BugReport br, Method method) {
		return asymmetricSimilarity(br.getBugReportCorpus().getContentTokens(),
				method.getContentTokens());
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
		if (weights == 0)
			return 0.0;
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
	
	/** paragraph vector between input BugReport and Method */
	public static double paragraphVectorSimilarity(BugReport br, Method method) {
		return Transforms.cosineSim(br.getParagraphVector(), method.getParagraphVector());
	}
	
}
