package edu.whu.pllab.buglocator.vectorizer;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.Method;
import edu.whu.pllab.buglocator.common.SourceCode;
import edu.whu.pllab.buglocator.common.SourceCodeCorpus;
import edu.whu.pllab.buglocator.common.TokenScore;
import edu.whu.pllab.buglocator.common.TokenScore.ScoreType;

public class SourceCodeTfidfVectorizer {
	
	private static final Logger logger = LoggerFactory.getLogger(SourceCodeTfidfVectorizer.class);
	
	/** whether use structured information */
	private boolean useStructuredInformation;
	
	private TfidfVectorizer<String> tfidf;
	private HashMap<String, SourceCode> sourceCodeMap;
	private ScoreType tokenScoreType = ScoreType.WFIDF;
	
	/** parameters of calculating structural information Okapi tf score*/
	private boolean usingOkapi = false;
	private double k1 = 1.0;
	private double b = 0.3;

	public SourceCodeTfidfVectorizer() {
		useStructuredInformation = Property.USE_STRUCTURED_INFORMATION;
	}
	
	public SourceCodeTfidfVectorizer(HashMap<String, SourceCode> sourceCodeMap) {
		useStructuredInformation = Property.USE_STRUCTURED_INFORMATION;
		this.sourceCodeMap = sourceCodeMap;
	}
	

	/** fit tfidf model for given source code */
	public void train() {
		logger.info("Fitting tfidf model for source code corpus...");
		SentenceIterator<String> iter = new SourceCodeSentenceIterator(sourceCodeMap);
		tfidf = new TfidfVectorizer<String>(iter, 0);
		tfidf.fit();
	}
	
	/** update tfidf model after git checkout */
	public void update(HashMap<String, SourceCode> addedFiles, HashMap<String, SourceCode> modifiedFiles,
			HashMap<String, SourceCode> deletedFiles) {
		// update tfidf
		SentenceIterator<String> addedSentenceIter = new SourceCodeSentenceIterator(addedFiles);
		SentenceIterator<String> modifiedSentenceIter = new SourceCodeSentenceIterator(modifiedFiles);
		SentenceIterator<String> deletedSentenceIter = new SourceCodeSentenceIterator(deletedFiles);
		tfidf.update(addedSentenceIter, modifiedSentenceIter, deletedSentenceIter);
	}	
	
	/** calculate tokens tf, idf and tokensWeight for source code corpus and methods in source code, and set content Norm value */
	public void calculateTokensWeight(HashMap<String, SourceCode> sourceCodeMap) {
		logger.info("Calculating tokens weight for input source code and methods in source code...");
		for (Entry<String, SourceCode> entry : sourceCodeMap.entrySet()) {
			SourceCode sourceCode = entry.getValue();
			calculateTokensWeight(sourceCode);
			for (Method method : sourceCode.getMethodList()) 
				calculateTokensWeight(method);
			
			if (useStructuredInformation && (!usingOkapi)) {
				calculateForStructuredInformation(sourceCode);
			}
		}
		if (useStructuredInformation && usingOkapi) {
			okapiTfidfVectorizerForStrucInfo(sourceCodeMap);
		}
	}
	
	/** calculate tokens weight for source code */
	public void calculateTokensWeight(SourceCode sourceCode) {
		SourceCodeCorpus sourceCodeCorpus = sourceCode.getSourceCodeCorpus();
		HashMap<String, TokenScore> contentTokens = tfidf.vectorize(sourceCodeCorpus.getContent(),
				tokenScoreType);
		sourceCodeCorpus.setContentTokens(contentTokens);
		sourceCodeCorpus.setContentNorm(tfidf.calculateContentNorm(contentTokens));
	}
	
	/** calculate tokens weight for method */
	public void calculateTokensWeight(Method method) {
		method.setContentTokens(tfidf.vectorize(method.getContent(), tokenScoreType));
		method.setContentNorm(tfidf.calculateContentNorm(method.getContentTokens()));
	}
    
	/** calculate structured information tokens weight and norm value */
	public void calculateForStructuredInformation(SourceCode sourceCode) {
		SourceCodeCorpus sourceCodeCorpus = sourceCode.getSourceCodeCorpus();
		sourceCodeCorpus.setClassPartTokens(tfidf.vectorize(sourceCodeCorpus.getClassPart(), tokenScoreType));
		sourceCodeCorpus.setMethodPartTokens(tfidf.vectorize(sourceCodeCorpus.getMethodPart(), tokenScoreType));
		sourceCodeCorpus.setVariablePartTokens(tfidf.vectorize(sourceCodeCorpus.getVariablePart(), tokenScoreType));
		sourceCodeCorpus.setCommentPartTokens(tfidf.vectorize(sourceCodeCorpus.getCommentPart(), tokenScoreType));
		sourceCodeCorpus.setClassCorpusNorm(tfidf.calculateContentNorm(sourceCodeCorpus.getClassPartTokens()));
		sourceCodeCorpus.setMethodCorpusNorm(tfidf.calculateContentNorm(sourceCodeCorpus.getMethodPartTokens()));
		sourceCodeCorpus.setVariableCorpusNorm(tfidf.calculateContentNorm(sourceCodeCorpus.getVariablePartTokens()));
		sourceCodeCorpus.setCommentCorpusNorm(tfidf.calculateContentNorm(sourceCodeCorpus.getCommentPartTokens()));
	}
	
	/**
	 * calculate Okapi Tf and smoothed idf value for structural information
	 * tf = k1 * x / (x + k1 * (1 - b + b * ld / lc))
	 * ld represents the structural part content length, lc represents average length for the collection 
	 */
	public void okapiTfidfVectorizerForStrucInfo(HashMap<String, SourceCode> sourceCodeMap) {
		// caulcuate average part content length
		int classPartLengthSum = 0;
		int methodPartLengthSum = 0;
		int variablePartLengthSum = 0;
		int commentPartLengthSum = 0;
		for (SourceCode sourceCode : sourceCodeMap.values()) {
			SourceCodeCorpus corpus = sourceCode.getSourceCodeCorpus();
			classPartLengthSum += corpus.getClassPart().split(" ").length;
			methodPartLengthSum += corpus.getMethodPart().split(" ").length;
			variablePartLengthSum += corpus.getVariablePart().split(" ").length;
			commentPartLengthSum += corpus.getCommentPart().split(" ").length;
		}
		double averClassPartLength = (double) classPartLengthSum / sourceCodeMap.size();
		double averMethodPartLength = (double) methodPartLengthSum / sourceCodeMap.size();
		double averVariablePartLength = (double) variablePartLengthSum / sourceCodeMap.size();
		double averCommentPartLength = (double) commentPartLengthSum / sourceCodeMap.size();
		// calculate Okapi tf and smoothed idf value
		for (SourceCode sourceCode : sourceCodeMap.values()) {
			SourceCodeCorpus corpus = sourceCode.getSourceCodeCorpus();
			corpus.setClassPartTokens(tfidf.okapiTfidfVectorize(corpus.getClassPart(), averClassPartLength, k1, b));
			corpus.setMethodPartTokens(tfidf.okapiTfidfVectorize(corpus.getMethodPart(), averMethodPartLength, k1, b));
			corpus.setVariablePartTokens(tfidf.okapiTfidfVectorize(corpus.getVariablePart(), averVariablePartLength, k1, b));
			corpus.setCommentPartTokens(tfidf.okapiTfidfVectorize(corpus.getCommentPart(), averCommentPartLength, k1, b));
			// unused corpus norm
			corpus.setClassCorpusNorm(tfidf.calculateContentNorm(corpus.getClassPartTokens()));
			corpus.setMethodCorpusNorm(tfidf.calculateContentNorm(corpus.getMethodPartTokens()));
			corpus.setVariableCorpusNorm(tfidf.calculateContentNorm(corpus.getVariablePartTokens()));
			corpus.setCommentCorpusNorm(tfidf.calculateContentNorm(corpus.getCommentPartTokens()));
		}
	}
	
	/** Source Code Sentence Iterator help training tfidf model */
	private class SourceCodeSentenceIterator implements SentenceIterator<String> {

		private Iterator<Entry<String, SourceCode>> iter;
		private HashMap<String, SourceCode> sourceCodeMap;
		
		public SourceCodeSentenceIterator(HashMap<String, SourceCode> sourceCodeMap) {
			iter = sourceCodeMap.entrySet().iterator();
			this.sourceCodeMap = sourceCodeMap;
		}
		
		public Entry<String, String> nextEntry() {
			Entry<String, SourceCode> entry = iter.next();
			String path = entry.getKey();
			// deleted sentenceIter value is null
			if (entry.getKey() == null)
				return new AbstractMap.SimpleEntry<String, String>(path, null);
			else {
				String content = entry.getValue().getSourceCodeCorpus().getContent();
				return new AbstractMap.SimpleEntry<String, String>(path, content);
			}
		}

		public boolean hasNext() {
			return iter.hasNext();
		}

		public void reset() {
			iter = sourceCodeMap.entrySet().iterator();
		}

		public int size() {
			return sourceCodeMap.size();
		}
	}

	// setters and getters
	public ScoreType getTokenScoreType() {
		return tokenScoreType;
	}

	public void setTokenScoreType(ScoreType tokenScoreType) {
		this.tokenScoreType = tokenScoreType;
	}
	
	public TfidfVectorizer<String> getTfidf() {
		return tfidf;
	}

	public double getK1() {
		return k1;
	}

	public void setK1(double k1) {
		this.k1 = k1;
	}

	public double getB() {
		return b;
	}

	public void setB(double b) {
		this.b = b;
	}

	public boolean isUsingOkapi() {
		return usingOkapi;
	}

	public void setUsingOkapi(boolean usingOkapi) {
		this.usingOkapi = usingOkapi;
	}

	public boolean isUseStructuredInformation() {
		return useStructuredInformation;
	}

	public void setUseStructuredInformation(boolean useStructuredInformation) {
		this.useStructuredInformation = useStructuredInformation;
	}
	
}
