package edu.whu.pllab.buglocator.similarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
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
	public final static int WORDVECTORS = 3; 
	public final static int PARAGRAPH_VECTOR = 4;
	
	private WordVectors wordVectors;
	
	public Similarity() {
		this.wordVectors = null;
	}
	
	public Similarity(WordVectors wordVectors) {
		this.wordVectors = wordVectors;
	}
	
	/** similarity between BugReport and SourceCode by specific similarity type */
	public double similarity(BugReport br, SourceCode code, int similarityType) {
		double sim = 0;
		switch (similarityType) {
			case VSM:
				sim = vsmSimilarity(br, code);
				break;
			case SYMMETRIC:
				sim = symmetricSimilarity(br, code);
				break;
			case WORDVECTORS:
				sim = symmetricSimilarityWithWordVectors(br, code);
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
	public double similarity(BugReport br1, BugReport br2, int similarityType) {
		double sim = 0;
		switch (similarityType) {
			case VSM:
				sim = vsmSimilarity(br1, br2);
				break;
			case SYMMETRIC:
				sim = symmetricSimilarity(br1, br2);
				break;
			case WORDVECTORS:
				sim = symmetricSimilarityWithWordVectors(br1, br2);
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
	public double similarity(BugReport br, Method method, int similarityType) {
		double sim = 0;
		switch (similarityType) {
			case VSM:
				sim = vsmSimilarity(br, method);
				break;
			case SYMMETRIC:
				sim = symmetricSimilarity(br, method);
				break;
			case WORDVECTORS:
				sim = symmetricSimilarityWithWordVectors(br, method);
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
	public double structuralSimilarity(BugReport br, SourceCode code) {
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
				double fieldSim = vsmSimilarity(brFieldTokens, codeFieldTokens, false) / (brFieldNorm * codeFieldNorm);
				sim += fieldSim;
			}
		}
		return sim;
	}
	
	/**
	 * sum document scores across all eight combinations similarity
	 */
	public double BM25StructuralSimilarity(BugReport br, SourceCode code) {
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
				// calculate field similarity, do not need to normalize
				double fieldSim = vsmSimilarity(brFieldTokens, codeFieldTokens, false);
				sim += fieldSim;
			}
		}
		return sim;
	}
	
	/** Vector Space Model similarity between input BugReport and SourceCode */
	public double vsmSimilarity(BugReport br, SourceCode code) {
		double sim = 0.0;
		
		double brNorm = br.getBugReportCorpus().getContentNorm();
		double codeNorm = code.getSourceCodeCorpus().getContentNorm();
		// if norm value equal 0, return 0
		if (brNorm == 0 || codeNorm == 0)
			return sim;
		
		sim = vsmSimilarity(br.getBugReportCorpus().getContentTokens(), 
				code.getSourceCodeCorpus().getContentTokens(), false) / (brNorm * codeNorm);
		
		return sim;
	}
	
	/** Vector Space Model similarity between input BugReports */
	public double vsmSimilarity(BugReport br1, BugReport br2) {
		double sim = 0;
		
		double br1Norm = br1.getBugReportCorpus().getContentNorm();
		double br2Norm = br2.getBugReportCorpus().getContentNorm();
		// if norm value equal 0, return 0
		if (br1Norm == 0 || br2Norm == 0)
			return sim;
		
		sim = vsmSimilarity(br1.getBugReportCorpus().getContentTokens(),
				br2.getBugReportCorpus().getContentTokens(), false) / (br1Norm * br2Norm);
		
		return sim;
	}
	
	/** Vector Space Model similarity between input BugReport and Method */
	public double vsmSimilarity(BugReport br, Method method) {
		double sim = 0;
		
		double brNorm = br.getBugReportCorpus().getContentNorm();
		double methodNorm = method.getContentNorm();
		// if norm value equal 0, return 0
		if (brNorm == 0 || methodNorm == 0)
			return sim;
		
		sim = vsmSimilarity(br.getBugReportCorpus().getContentTokens(), method.getContentTokens(), false)
				/ (brNorm * methodNorm);
		
		return sim;
	}
	
	/** Vector Space Model similarity between input tokenScoreMaps */
	public double vsmSimilarity(HashMap<String, TokenScore> tokenScoreMap1,
			HashMap<String, TokenScore> tokenScoreMap2) {
		return vsmSimilarity(tokenScoreMap1, tokenScoreMap2, true);
	}
	
	/** Vector Space Model similarity between input tokenScoreMaps without dividing norm value */
	public double vsmSimilarity(HashMap<String, TokenScore> tokenScoreMap1,
			HashMap<String, TokenScore> tokenScoreMap2, boolean norm) {
		double sim = 0;
		for (Entry<String, TokenScore> entry1 : tokenScoreMap1.entrySet()) {
			if (tokenScoreMap2.containsKey(entry1.getKey())) {
				sim += entry1.getValue().getTokenWeight() * tokenScoreMap2.get(entry1.getKey()).getTokenWeight();
			}
		}
		if (!norm) {
			return sim;
		}
		else {
			double norm1 = 0, norm2 = 0;
			for (Entry<String, TokenScore> entry1 : tokenScoreMap1.entrySet()) {
				norm1 += entry1.getValue().getTokenWeight() * entry1.getValue().getTokenWeight();
			}
			for (Entry<String, TokenScore> entry2 : tokenScoreMap2.entrySet()) {
				norm2 += entry2.getValue().getTokenWeight() * entry2.getValue().getTokenWeight();
			}
			if (norm1 == 0 || norm2 == 0)
				return 0.0;
			return sim / (norm1 * norm2);
		}
	}
	
	/** symmetric similarity between input BugReport and SourceCode, sim(T, S) = (sim(T->S) + sim(S->T)) / 2 */
	public double symmetricSimilarity(BugReport br, SourceCode code) {
		HashMap<String, TokenScore> brTokens = br.getBugReportCorpus().getContentTokens();
		HashMap<String, TokenScore> codeTokens = code.getSourceCodeCorpus().getContentTokens();
		double brContentNorm = br.getBugReportCorpus().getContentNorm();
		double codeContentNorm = code.getSourceCodeCorpus().getContentNorm();
		if (brContentNorm == 0 || codeContentNorm == 0)
			return 0;
		return (asymmetricSimilarity(brTokens, codeTokens, false) / brContentNorm
				+ asymmetricSimilarity(codeTokens, brTokens, false) / codeContentNorm) / 2;
	}
	
	/** symmetric similarity between input BugReports, sim(T, S) = (sim(T->S) + sim(S->T)) / 2 */
	public double symmetricSimilarity(BugReport br1, BugReport br2) {
		HashMap<String, TokenScore> br1Tokens = br1.getBugReportCorpus().getContentTokens();
		HashMap<String, TokenScore> br2Tokens = br2.getBugReportCorpus().getContentTokens();
		double br1ContentNorm = br1.getBugReportCorpus().getContentNorm();
		double br2ContentNorm = br2.getBugReportCorpus().getContentNorm();
		if (br1ContentNorm == 0 || br2ContentNorm == 0)
			return 0;
		return (asymmetricSimilarity(br1Tokens, br2Tokens, false) / br1ContentNorm
				+ asymmetricSimilarity(br2Tokens, br1Tokens, false) / br2ContentNorm) / 2;
	}
	
	/** symmetric similarity between input BugReport and method, sim(T, S) = (sim(T->S) + sim(S->T)) / 2 */
	public double symmetricSimilarity(BugReport br, Method method) {
		HashMap<String, TokenScore> brTokens = br.getBugReportCorpus().getContentTokens();
		HashMap<String, TokenScore> methodTokens = method.getContentTokens();
		double brContentNorm = br.getBugReportCorpus().getContentNorm();
		double methodContentNorm = method.getContentNorm();
		if (brContentNorm == 0 || methodContentNorm == 0)
			return 0;
		return (asymmetricSimilarity(brTokens, methodTokens, false) / brContentNorm
				+ asymmetricSimilarity(methodTokens, brTokens, false) / methodContentNorm) / 2;
	}
	
	/** symmetric similarity between input tokenScoreMaps, sim(T, S) = (sim(T->S) + sim(S->T)) / 2 */
	public double symmetricSimilarity(HashMap<String, TokenScore> tokenScoreMap1,
			HashMap<String, TokenScore> tokenScoreMap2) {
		return (asymmetricSimilarity(tokenScoreMap1, tokenScoreMap2)
				+ asymmetricSimilarity(tokenScoreMap2, tokenScoreMap1)) / 2;
	}
	
	/** symmetric similarity between input tokenScoreMaps, sim(T, S) = sim(T->S) */
	public double asymmetricSimilarity(HashMap<String, TokenScore> tokenScoreMap1,
			HashMap<String, TokenScore> tokenScoreMap2) {
		return asymmetricSimilarity(tokenScoreMap1, tokenScoreMap2, true);
	}
	
	/** symmetric similarity between input tokenScoreMaps without dividing norm value, sim(T, S) = sim(T->S) */
	public double asymmetricSimilarity(HashMap<String, TokenScore> tokenScoreMap1,
			HashMap<String, TokenScore> tokenScoreMap2, boolean norm) {
		double simSum = 0.0;
		double weights = 0.0;
		if (tokenScoreMap1.size() == 0 || tokenScoreMap2.size() == 0) 
			return 0.0;
		for (Entry<String, TokenScore> entry1 : tokenScoreMap1.entrySet()) {
			double sim = 0.0;
			if(tokenScoreMap2.containsKey(entry1.getKey()))
				sim = 1.0;
			simSum += sim;
			if (norm)
				weights += entry1.getValue().getTokenWeight();
		}
		if (norm) {
			if (weights == 0)
				return 0.0;
			return simSum / weights;
		}
		else 
			return simSum;
	}
	
	/** symmetric similarity between input bug report and source code using word vectors, 
	 * sim(T, S) = (sim(T->S) + sim(S->T)) / 2 */
	public double symmetricSimilarityWithWordVectors(BugReport br, SourceCode code) {
		HashMap<String, TokenScore> brTokens = br.getBugReportCorpus().getContentTokens();
		HashMap<String, TokenScore> codeTokens = code.getSourceCodeCorpus().getContentTokens();
		double brContentNorm = br.getBugReportCorpus().getContentNorm();
		double codeContentNorm = code.getSourceCodeCorpus().getContentNorm();
		if (brContentNorm == 0 || codeContentNorm == 0)
			return 0;
		return (asymmetricSimilarityWithWordVectors(brTokens, codeTokens, false) / brContentNorm
				+ asymmetricSimilarityWithWordVectors(codeTokens, brTokens, false) / codeContentNorm) / 2;
	}
	
	/** symmetric similarity between input BugReports using word vectors, sim(T, S) = (sim(T->S) + sim(S->T)) / 2 */
	public double symmetricSimilarityWithWordVectors(BugReport br1, BugReport br2) {
		HashMap<String, TokenScore> br1Tokens = br1.getBugReportCorpus().getContentTokens();
		HashMap<String, TokenScore> br2Tokens = br2.getBugReportCorpus().getContentTokens();
		double br1ContentNorm = br1.getBugReportCorpus().getContentNorm();
		double br2ContentNorm = br2.getBugReportCorpus().getContentNorm();
		if (br1ContentNorm == 0 || br2ContentNorm == 0)
			return 0;
		return (asymmetricSimilarityWithWordVectors(br1Tokens, br2Tokens, false) / br1ContentNorm
				+ asymmetricSimilarityWithWordVectors(br2Tokens, br1Tokens, false) / br2ContentNorm) / 2;
	}
	
	/** symmetric similarity between input BugReport and method using word vectors, sim(T, S) = (sim(T->S) + sim(S->T)) / 2 */
	public double symmetricSimilarityWithWordVectors(BugReport br, Method method) {
		HashMap<String, TokenScore> brTokens = br.getBugReportCorpus().getContentTokens();
		HashMap<String, TokenScore> methodTokens = method.getContentTokens();
		double brContentNorm = br.getBugReportCorpus().getContentNorm();
		double methodContentNorm = method.getContentNorm();
		if (brContentNorm == 0 || methodContentNorm == 0)
			return 0;
		return (asymmetricSimilarityWithWordVectors(brTokens, methodTokens, false) / brContentNorm
				+ asymmetricSimilarityWithWordVectors(methodTokens, brTokens, false) / methodContentNorm) / 2;
	}
	
	/** symmetric similarity between input tokenScoreMaps using word vectors, sim(T, S) = (sim(T->S) + sim(S->T)) / 2 */
	public double symmetricSimilarityWithWordVectors(HashMap<String, TokenScore> tokenScoreMap1,
			HashMap<String, TokenScore> tokenScoreMap2) {
		return (asymmetricSimilarityWithWordVectors(tokenScoreMap1, tokenScoreMap2)
				+ asymmetricSimilarityWithWordVectors(tokenScoreMap2, tokenScoreMap1)) / 2;
	}
	
	/** symmetric similarity between input tokenScoreMaps using word vectors */
	public double asymmetricSimilarityWithWordVectors(HashMap<String, TokenScore> tokenScoreMap1,
			HashMap<String, TokenScore> tokenScoreMap2) {
		return asymmetricSimilarityWithWordVectors(tokenScoreMap1, tokenScoreMap2, true);
	}
	
	/** symmetric similarity between input tokenScoreMaps using word vectors, if norm is true, normalized similairty result*/
	public double asymmetricSimilarityWithWordVectors(HashMap<String, TokenScore> tokenScoreMap1,
			HashMap<String, TokenScore> tokenScoreMap2, boolean norm) {
		double simSum = 0.0;
		double weights = 0.0;
		if (tokenScoreMap1.size() == 0 || tokenScoreMap2.size() == 0) 
			return 0.0;
		for (Entry<String, TokenScore> entry1 : tokenScoreMap1.entrySet()) {
			if (!wordVectors.hasWord(entry1.getKey()))
				continue;
			double maxSim = 0.0;
			for (Entry<String, TokenScore> entry2 : tokenScoreMap2.entrySet()) {
				if (entry1.getKey().equals(entry2.getKey()))
					maxSim = 1.0;
				else {
					double sim = wordVectors.hasWord(entry2.getKey()) ? wordVectors.similarity(entry1.getKey(), entry2.getKey()) : 0;
					if (sim > maxSim)
						maxSim = sim;
				}
			}
			simSum += maxSim;
			if (norm)
				weights += entry1.getValue().getTokenWeight();
		}
		if (norm) {
			if (weights == 0)
				return 0.0;
			return simSum / weights;
		}
		else 
			return simSum;
	}
	
	/** paragraph vector between input BugReport and SourceCode */
	public double paragraphVectorSimilarity(BugReport br, SourceCode code) {
		return Transforms.cosineSim(br.getParagraphVector(), code.getParagraphVector());
	}
	
	/** paragraph vector between input BugReports */
	public double paragraphVectorSimilarity(BugReport br1, BugReport br2) {
		return Transforms.cosineSim(br1.getParagraphVector(), br2.getParagraphVector());
	}
	
	/** paragraph vector between input BugReport and Method */
	public double paragraphVectorSimilarity(BugReport br, Method method) {
		return Transforms.cosineSim(br.getParagraphVector(), method.getParagraphVector());
	}

	public WordVectors getWordVectors() {
		return wordVectors;
	}

	public void setWord2vec(WordVectors wordVectors) {
		this.wordVectors = wordVectors;
	}
	
}
