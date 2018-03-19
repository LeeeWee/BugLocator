package edu.whu.pllab.buglocator.vectorizer;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.deeplearning4j.bagofwords.vectorizer.TfidfVectorizer;
import org.deeplearning4j.text.sentenceiterator.BaseSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.util.MathUtils;
import org.nd4j.linalg.util.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.Method;
import edu.whu.pllab.buglocator.common.SourceCode;
import edu.whu.pllab.buglocator.common.SourceCodeCorpus;
import edu.whu.pllab.buglocator.common.TokenScore;

public class SourceCodeTfidfVectorizer {
	
	private static final Logger logger = LoggerFactory.getLogger(BugReportTfidfVectorizer.class);
	
	private TfidfVectorizer tfidf;
	private HashMap<String, SourceCode> sourceCodeMap;
	private String tfidfPath;
	
	public SourceCodeTfidfVectorizer() {
		Property property = Property.getInstance();
		tfidfPath = property.getCodeTfidfModelPath();
	}
	
	public SourceCodeTfidfVectorizer(HashMap<String, SourceCode> sourceCodeMap) {
		this.sourceCodeMap = sourceCodeMap;
		Property property = Property.getInstance();
		tfidfPath = property.getCodeTfidfModelPath();
	}
	
	/** fit tfidf model for given source code */
	public void train() {
		logger.info("Fitting tfidf model for source code corpus...");
		SentenceIterator iter = new SourceCodeSentenceIterator(sourceCodeMap);
		tfidf = new TfidfVectorizer.Builder()
							.setIterator(iter)
							.setMinWordFrequency(0)
							.setTokenizerFactory(new DefaultTokenizerFactory())
							.build();
		tfidf.fit();
		SerializationUtils.saveObject(tfidf, new File(tfidfPath));
	}
	
	/** load tfidf model from tfidf model path */
	public void loadTfidfModel() {
		logger.info("Loading tfidf model for bug reports corpus...");
		tfidf = SerializationUtils.readObject(new File(tfidfPath));
	}
	
	/** calculate tokens tf, idf and tokensWeight for source code corpus and methods in source code, and set content Norm value */
	public void calculateTokensWeight(HashMap<String, SourceCode> sourceCodeMap) {
		logger.info("Calculating tokens weight for input source code and methods in source code...");
		for (Entry<String, SourceCode> entry : sourceCodeMap.entrySet()) {
			SourceCode sourceCode = entry.getValue();
			calculateTokensWeight(sourceCode);
			for (Method method : sourceCode.getMethodList()) 
				calculateTokensWeight(method);
		}
	}
	
	/** calculate tokens weight for source code */
	public void calculateTokensWeight(SourceCode sourceCode) {
		double contentNorm = 0.0;
		int documentLength = 0;
		SourceCodeCorpus sourceCodeCorpus = sourceCode.getSourceCodeCorpus();
		String[] content = sourceCodeCorpus.getContent().split(" ");
		HashMap<String, TokenScore> contentTokens = sourceCodeCorpus.getContentTokens();
		HashMap<String, Integer> tokensCount = new HashMap<String, Integer>();
		// get tokens term frequency
		for (String token : content) {
			if ((token = token.trim()) == "")
				continue;
			if (!tokensCount.containsKey(token))
				tokensCount.put(token, 1);
			else
				tokensCount.put(token, tokensCount.get(token) + 1);
			documentLength++;
		}
		for (Entry<String, Integer> tokenEntry : tokensCount.entrySet()) {
			String token = tokenEntry.getKey();
			double tf = tfForWord(tokenEntry.getValue(), documentLength);
			double idf = idfForWord(token);
			double tfidf = MathUtils.tfidf(tf, idf);
			TokenScore tokenScore = new TokenScore(token, tf, idf, tfidf);
			contentTokens.put(token, tokenScore);
			contentNorm += tfidf * tfidf;
		}
		sourceCodeCorpus.setContentNorm(contentNorm);
	}
	
	/** calculate tokens weight for method */
	public void calculateTokensWeight(Method method) {
		double contentNorm = 0.0;
		int documentLength = 0;
		String[] content = method.getContent().split(" ");
		HashMap<String, TokenScore> contentTokens = method.getContentTokens();
		HashMap<String, Integer> tokensCount = new HashMap<String, Integer>();
		// get tokens term frequency
		for (String token : content) {
			if ((token = token.trim()) == "")
				continue;
			if (!tokensCount.containsKey(token))
				tokensCount.put(token, 1);
			else
				tokensCount.put(token, tokensCount.get(token) + 1);
			documentLength++;
		}
		for (Entry<String, Integer> tokenEntry : tokensCount.entrySet()) {
			String token = tokenEntry.getKey();
			double tf = tfForWord(tokenEntry.getValue(), documentLength);
			double idf = idfForWord(token);
			double tfidf = MathUtils.tfidf(tf, idf);
			TokenScore tokenScore = new TokenScore(token, tf, idf, tfidf);
			contentTokens.put(token, tokenScore);
			contentNorm += tfidf * tfidf;
		}
		method.setContentNorm(contentNorm);
	}
	
	public double tfidfWord(String word, long wordCount, long documentLength) {
        return MathUtils.tfidf(tfForWord(wordCount, documentLength), idfForWord(word));
    }

    public double tfForWord(long wordCount, long documentLength) {
        return (double) wordCount / (double) documentLength;
    }

    public double idfForWord(String word) {
        return MathUtils.idf(tfidf.getVocabCache().totalNumberOfDocs(), tfidf.getVocabCache().docAppearedIn(word));
    }
    
    
	
	/** Source Code Sentence Iterator help training tfidf model */
	private class SourceCodeSentenceIterator extends BaseSentenceIterator {

		private Iterator<SourceCode> iter;
		private HashMap<String, SourceCode> sourceCodeMap;
		
		public SourceCodeSentenceIterator(SentencePreProcessor preProcessor, HashMap<String, SourceCode> sourceCodeMap) {
			super(preProcessor);
			this.sourceCodeMap = sourceCodeMap;
			this.iter = sourceCodeMap.values().iterator();
		}
		
		public SourceCodeSentenceIterator(HashMap<String, SourceCode> sourceCodeMap) {
			this(null, sourceCodeMap);
		}
		
		public String nextSentence() {
			SourceCode sourceCode = iter.next();
			String ret = sourceCode.getSourceCodeCorpus().getContent();
			if (this.getPreProcessor() != null)
				ret = this.getPreProcessor().preProcess(ret);
			return ret;
		}

		public boolean hasNext() {
			return iter.hasNext();
		}

		public void reset() {
			iter = sourceCodeMap.values().iterator();
		}
	}
	
	
}