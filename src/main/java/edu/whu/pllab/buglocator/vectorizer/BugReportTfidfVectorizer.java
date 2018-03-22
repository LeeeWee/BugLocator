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
import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportCorpus;
import edu.whu.pllab.buglocator.common.TokenScore;

public class BugReportTfidfVectorizer {
	
	private static final Logger logger = LoggerFactory.getLogger(BugReportTfidfVectorizer.class);
	
	private TfidfVectorizer tfidf;
	private HashMap<Integer, BugReport> bugReportsMap;
	private String tfidfPath;
	
	public BugReportTfidfVectorizer() {
		Property property = Property.getInstance();
		tfidfPath = property.getBrTfidfModelPath();
	}
	
	public BugReportTfidfVectorizer(HashMap<Integer, BugReport> bugReportsMap) {
		this.bugReportsMap = bugReportsMap;
		Property property = Property.getInstance();
		tfidfPath = property.getBrTfidfModelPath();
	}
	
	/** fit tfidf model for given bug reports */
	public void train() {
		logger.info("Fitting tfidf model for bug reports corpus...");
		SentenceIterator iter = new BugReportSentenceIterator(bugReportsMap);
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
	
	/** calculate tokens tf, idf and tokensWeight for bug report corpus, and set content Norm value */
	public void calculateTokensWeight(HashMap<Integer, BugReport> bugReportsMap) {
		logger.info("Calculating tokens weight for input bug reports...");
		for (Entry<Integer, BugReport> entry : bugReportsMap.entrySet()) {
			BugReport bugReport = entry.getValue();
			double contentNorm = 0.0;
			int documentLength = 0;
			BugReportCorpus bugReportCorpus = bugReport.getBugReportCorpus();
			String[] content = bugReportCorpus.getContent().split(" ");
			HashMap<String, TokenScore> contentTokens = bugReportCorpus.getContentTokens();
			HashMap<String, Integer> tokensCount = new HashMap<String, Integer>();
			// get tokens term frequency
			for (String token : content) {
				if ((token = token.trim()).equals(""))
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
			contentNorm = Math.sqrt(contentNorm);
			bugReportCorpus.setContentNorm(contentNorm);
		}
	}
	
    public double tfidfWord(String word, long wordCount, long documentLength) {
        return MathUtils.tfidf(tfForWord(wordCount, documentLength), idfForWord(word));
    }

    public double tfForWord(long wordCount, long documentLength) {
        return (double) wordCount / (double) documentLength;
    }

    public double idfForWord(String word) {
    	if (!tfidf.getVocabCache().containsWord(word))
    		return 0.0;
        return MathUtils.idf(tfidf.getVocabCache().totalNumberOfDocs(), tfidf.getVocabCache().docAppearedIn(word));
    }
	
    /** BugReport Sentence Iterator help training tfidf model */
	private class BugReportSentenceIterator extends BaseSentenceIterator {

		private Iterator<BugReport> iter;
		private HashMap<Integer, BugReport> bugReportsMap;
		
		public BugReportSentenceIterator(SentencePreProcessor preProcessor, HashMap<Integer, BugReport> bugReportsMap) {
			super(preProcessor);
			this.bugReportsMap = bugReportsMap;
			this.iter = bugReportsMap.values().iterator();
		}
		
		public BugReportSentenceIterator(HashMap<Integer, BugReport> bugReportsMap) {
			this(null, bugReportsMap);
		}
		
		public String nextSentence() {
			BugReport bugReport = iter.next();
			String ret = bugReport.getBugReportCorpus().getContent();
			if (this.getPreProcessor() != null)
				ret = this.getPreProcessor().preProcess(ret);
			return ret;
		}

		public boolean hasNext() {
			return iter.hasNext();
		}

		public void reset() {
			iter = bugReportsMap.values().iterator();
		}
	}
	
}
