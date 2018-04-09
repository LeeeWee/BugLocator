package edu.whu.pllab.buglocator.vectorizer;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.deeplearning4j.util.MathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportCorpus;
import edu.whu.pllab.buglocator.common.TokenScore;
import edu.whu.pllab.buglocator.common.TokenScore.ScoreType;

public class BugReportTfidfVectorizer {
	
	private static final Logger logger = LoggerFactory.getLogger(BugReportTfidfVectorizer.class);
	
	/**
	 * if true, using code tfidf model to calculate bug reports content tokens
	 * weight, else train tfidf model using given bugReportsMap
	 */
	private boolean usingCodeTfidf;
	
	private TfidfVectorizer<String> codeTfidf;
	private TfidfVectorizer<Integer> brTfidf;
	private HashMap<Integer, BugReport> bugReportsMap;
	private ScoreType tokenScoreType = ScoreType.NTFIDF;
	
	public BugReportTfidfVectorizer() {
	}
	
	public BugReportTfidfVectorizer(TfidfVectorizer<String> codeTfidf) {
		usingCodeTfidf = true;
		this.codeTfidf = codeTfidf;
	}
	
	public BugReportTfidfVectorizer(HashMap<Integer, BugReport> bugReportsMap) {
		usingCodeTfidf = false;
		this.bugReportsMap = bugReportsMap;
	}
	
	/** fit tfidf model for given bug reports */
	public void train() {
		usingCodeTfidf = false;
		logger.info("Fitting tfidf model for bug reports corpus...");
		SentenceIterator<Integer> iter = new BugReportSentenceIterator(bugReportsMap);
		brTfidf = new TfidfVectorizer<Integer>(iter, 0);
		brTfidf.fit();
	}
	
	/** calculate tokens tf, idf and tokensWeight for bug report corpus, and set content Norm value */
	public void calculateTokensWeight(HashMap<Integer, BugReport> bugReportsMap) {
		logger.info("Calculating tokens weight for input bug reports...");
		for (Entry<Integer, BugReport> entry : bugReportsMap.entrySet()) {
			calculateTokensWeight(entry.getValue());
		}
	}
	
	/** calculate tokens tf, idf and tokensWeight for single bug report, and set content Norm value */
	public void calculateTokensWeight(BugReport bugReport) {
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
		int maxWordCount = 0;
		double aveWordCount = 0.0;
		if (tokenScoreType == ScoreType.NTFIDF) 
			maxWordCount = Collections.max(tokensCount.values());
		if (tokenScoreType == ScoreType.LOGTFIDF) {
			double sumWordCount = 0.0;
			for (Integer count : tokensCount.values())
				sumWordCount += count;
			aveWordCount = sumWordCount / tokensCount.size();
		}
		
		for (Entry<String, Integer> tokenEntry : tokensCount.entrySet()) {
			String token = tokenEntry.getKey();
			double tf;
			switch (tokenScoreType) {
			case TFIDF:
				tf = tfForWord(tokenEntry.getValue(), documentLength);
				break;
			case NTFIDF:
				tf = ntfForWord(tokenEntry.getValue(), maxWordCount);
				break;
			case WFIDF:
				tf = wfForWord(tokenEntry.getValue());
				break;
			case LOGTFIDF:
				tf = logTfForWord(tokenEntry.getValue(), aveWordCount);
				break;
			default : // default type: ntf-idf
				tf = ntfForWord(tokenEntry.getValue(), maxWordCount);
				break;
			}
			double idf = idfForWord(token);
			double tfidf = MathUtils.tfidf(tf, idf);
			TokenScore tokenScore = new TokenScore(token, tf, idf, tfidf);
			contentTokens.put(token, tokenScore);
			contentNorm += tfidf * tfidf;
		}
		contentNorm = Math.sqrt(contentNorm);
		bugReportCorpus.setContentNorm(contentNorm);
	} 
	
    public double tfForWord(long wordCount, long documentLength) {
        return (double) wordCount / (double) documentLength;
    }
    
    public double ntfForWord(long wordCount, long maxWordCount) {
    	return 0.5 + 0.5 * (double) wordCount / maxWordCount;
    }
    
    public double wfForWord(long wordCount) {
    	return 1 + Math.log(wordCount);
    }
    
    public double logTfForWord(long wordCount, double aveWordCount) {
    	return (1 + Math.log(wordCount)) / (1 + Math.log(aveWordCount));
    }

    public double idfForWord(String word) {
    	if (usingCodeTfidf)
    		return codeTfidf.idfForWord(word);
    	else 
    		return brTfidf.idfForWord(word);
    }
    
    
    /** BugReport Sentence Iterator help training tfidf model */
	private class BugReportSentenceIterator implements SentenceIterator<Integer> {

		private Iterator<Entry<Integer, BugReport>> iter;
		private HashMap<Integer, BugReport> bugReportsMap;
		
		public BugReportSentenceIterator(HashMap<Integer, BugReport> bugReportsMap) {
			this.bugReportsMap = bugReportsMap;
			this.iter = bugReportsMap.entrySet().iterator();
		}
		
		public Entry<Integer, String> nextEntry() {
			Entry<Integer, BugReport> entry = iter.next();
			Integer bugID = entry.getKey();
			String content = entry.getValue().getBugReportCorpus().getContent();
			return new AbstractMap.SimpleEntry<Integer, String>(bugID, content);
		}

		public boolean hasNext() {
			return iter.hasNext();
		}

		public void reset() {
			iter = bugReportsMap.entrySet().iterator();
		}

		public int size() {
			return bugReportsMap.size();
		}
	}

	public ScoreType getTokenScoreType() {
		return tokenScoreType;
	}

	public void setTokenScoreType(ScoreType tokenScoreType) {
		this.tokenScoreType = tokenScoreType;
	}
	
	public void setTfidf(TfidfVectorizer<String> codeTfidf) {
		usingCodeTfidf = true;
		this.codeTfidf = codeTfidf;
	}
	
	public void setUsingCodeTfidf(boolean usingCodeTfidf) {
		this.usingCodeTfidf = usingCodeTfidf;
	}
}
