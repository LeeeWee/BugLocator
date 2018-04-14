package edu.whu.pllab.buglocator.similarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.nd4j.linalg.ops.transforms.Transforms;

import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportCorpus;
import edu.whu.pllab.buglocator.common.Method;
import edu.whu.pllab.buglocator.common.SourceCode;
import edu.whu.pllab.buglocator.common.SourceCodeCorpus;
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
	
	/**
	 * representing bug report with two different fields: the summary and verbose description, and 
	 * parsing source code structure to four different document fields: class, method, variable, comments,
	 * then performing a separate similarity calculating for each of the eight (query represent, document field) 
	 * combinations and then sum document scores across all eight similarity
	 */
	public static double structuralSimilarity(BugReport br, SourceCode code) {
		double sim = 0.0;
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
		// sum documents scores across eight combinations
		for (int i = 0; i < brFields.size(); i++) {
			HashMap<String, TokenScore> brFieldTokens = brFields.get(i);
			double brFieldNorm = brFieldsNorm.get(i);
			if (brFieldNorm == 0)
				continue;
			for (int j = 0; j < codeFields.size(); j++) {
				HashMap<String, TokenScore> codeFieldTokens = codeFields.get(j);
				double codeFieldNorm = codeFieldsNorm.get(j);
				if (codeFieldNorm == 0)
					continue;
				// calculate field similarity
				double fieldSim = vsmSimilarityWithoutNorm(brFieldTokens, codeFieldTokens) / (brFieldNorm * codeFieldNorm);
				
				sim += fieldSim;
			}
		}
		return sim;
	}
	
	/** Vector Space Model similarity between input BugReport and SourceCode */
	public static double vsmSimilarity(BugReport br, SourceCode code) {
		double sim = 0.0;
		
		double brNorm = br.getBugReportCorpus().getContentNorm();
		double codeNorm = code.getSourceCodeCorpus().getContentNorm();
		// if norm value equal 0, return 0
		if (brNorm == 0 || codeNorm == 0)
			return sim;
		
		sim = vsmSimilarityWithoutNorm(br.getBugReportCorpus().getContentTokens(), 
				code.getSourceCodeCorpus().getContentTokens()) / (brNorm * codeNorm);
		
		return sim;
	}
	
	/** Vector Space Model similarity between input BugReports */
	public static double vsmSimilarity(BugReport br1, BugReport br2) {
		double sim = 0;
		
		double br1Norm = br1.getBugReportCorpus().getContentNorm();
		double br2Norm = br2.getBugReportCorpus().getContentNorm();
		// if norm value equal 0, return 0
		if (br1Norm == 0 || br2Norm == 0)
			return sim;
		
		sim = vsmSimilarityWithoutNorm(br1.getBugReportCorpus().getContentTokens(),
				br2.getBugReportCorpus().getContentTokens()) / (br1Norm * br2Norm);
		
		return sim;
	}
	
	/** Vector Space Model similarity between input BugReport and Method */
	public static double vsmSimilarity(BugReport br, Method method) {
		double sim = 0;
		
		double brNorm = br.getBugReportCorpus().getContentNorm();
		double methodNorm = method.getContentNorm();
		// if norm value equal 0, return 0
		if (brNorm == 0 || methodNorm == 0)
			return sim;
		
		sim = vsmSimilarityWithoutNorm(br.getBugReportCorpus().getContentTokens(), method.getContentTokens())
				/ (br.getBugReportCorpus().getContentNorm() * method.getContentNorm());
		
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
	
	/** Vector Space Model similarity between input tokenScoreMaps without dividing norm value */
	public static double vsmSimilarityWithoutNorm(HashMap<String, TokenScore> tokenScoreMap1,
			HashMap<String, TokenScore> tokenScoreMap2) {
		double sim = 0;
		for (Entry<String, TokenScore> entry1 : tokenScoreMap1.entrySet()) {
			if (tokenScoreMap2.containsKey(entry1.getKey())) {
				sim += entry1.getValue().getTokenWeight() * tokenScoreMap2.get(entry1.getKey()).getTokenWeight();
			}
		}
		return sim;
	}
	
	/** symmetric similarity between input BugReport and SourceCode, sim(T, S) = (sim(T->S) + sim(S->T)) / 2 */
	public static double symmetricSimilarity(BugReport br, SourceCode code) {
		return (asymmetricSimilarity(br, code) + 
				asymmetricSimilarityWithoutNorm(br.getBugReportCorpus().getContentTokens(),
						code.getSourceCodeCorpus().getContentTokens()) / code.getSourceCodeCorpus().getContentNorm())
				/ 2;
	}
	
	/** symmetric similarity between input BugReports, sim(T, S) = (sim(T->S) + sim(S->T)) / 2 */
	public static double symmetricSimilarity(BugReport br1, BugReport br2) {
		return (asymmetricSimilarity(br1, br2) + asymmetricSimilarity(br2, br1)) / 2;
	}
	
	/** symmetric similarity between input BugReport and method, sim(T, S) = (sim(T->S) + sim(S->T)) / 2 */
	public static double symmetricSimilarity(BugReport br, Method method) {
		return (asymmetricSimilarity(br, method)
				+ asymmetricSimilarityWithoutNorm(br.getBugReportCorpus().getContentTokens(),
						method.getContentTokens()) / method.getContentNorm())
				/ 2;
	}
	
	/** symmetric similarity between input tokenScoreMaps, sim(T, S) = (sim(T->S) + sim(S->T)) / 2 */
	public static double symmetricSimilarity(HashMap<String, TokenScore> tokenScoreMap1,
			HashMap<String, TokenScore> tokenScoreMap2) {
		return (asymmetricSimilarity(tokenScoreMap1, tokenScoreMap2)
				+ asymmetricSimilarity(tokenScoreMap2, tokenScoreMap1)) / 2;
	}
	
	/** symmetric similarity between input BugReport and SourceCode, sim(T, S) = sim(T->S) */
	public static double asymmetricSimilarity(BugReport br, SourceCode code) {
		return asymmetricSimilarityWithoutNorm(br.getBugReportCorpus().getContentTokens(),
				code.getSourceCodeCorpus().getContentTokens()) / br.getBugReportCorpus().getContentNorm();
	}
	
	/** symmetric similarity between input BugReports, sim(T, S) = sim(T->S) */
	public static double asymmetricSimilarity(BugReport br1, BugReport br2) {
		return asymmetricSimilarityWithoutNorm(br1.getBugReportCorpus().getContentTokens(),
				br2.getBugReportCorpus().getContentTokens()) / br1.getBugReportCorpus().getContentNorm();
	}
	
	/** symmetric similarity between input BugReport and method, sim(T, S) = sim(T->S) */
	public static double asymmetricSimilarity(BugReport br, Method method) {
		return asymmetricSimilarityWithoutNorm(br.getBugReportCorpus().getContentTokens(),
				method.getContentTokens()) / br.getBugReportCorpus().getContentNorm();
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

	/** symmetric similarity between input tokenScoreMaps without dividing norm value, sim(T, S) = sim(T->S) */
	public static double asymmetricSimilarityWithoutNorm(HashMap<String, TokenScore> tokenScoreMap1,
			HashMap<String, TokenScore> tokenScoreMap2) {
		double simSum = 0.0;
		if (tokenScoreMap1.size() == 0 || tokenScoreMap2.size() == 0) 
			return 0.0;
		for (Entry<String, TokenScore> entry1 : tokenScoreMap1.entrySet()) {
			double sim = 0.0;
			if(tokenScoreMap2.containsKey(entry1.getKey()))
				sim = 1.0;
			simSum += sim;
		}
		return simSum;
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
