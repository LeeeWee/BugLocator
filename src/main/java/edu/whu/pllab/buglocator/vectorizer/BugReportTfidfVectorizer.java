package edu.whu.pllab.buglocator.vectorizer;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportCorpus;
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
		BugReportCorpus bugReportCorpus = bugReport.getBugReportCorpus();
		if (usingCodeTfidf) {
			bugReportCorpus.setContentTokens(codeTfidf.vectorize(bugReportCorpus.getContent(), tokenScoreType));
			bugReportCorpus.setContentNorm(codeTfidf.calculateContentNorm(bugReportCorpus.getContentTokens()));
			bugReportCorpus.setSummaryTokens(codeTfidf.vectorize(bugReportCorpus.getSummaryPart(), tokenScoreType));
			bugReportCorpus.setSummaryNorm(codeTfidf.calculateContentNorm(bugReportCorpus.getSummaryTokens()));
			bugReportCorpus.setDescriptionTokens(codeTfidf.vectorize(bugReportCorpus.getDescriptionPart(), tokenScoreType));
			bugReportCorpus.setDescriptionNorm(codeTfidf.calculateContentNorm(bugReportCorpus.getDescriptionTokens()));
		} else {
			bugReportCorpus.setContentTokens(brTfidf.vectorize(bugReportCorpus.getContent(), tokenScoreType));
			bugReportCorpus.setContentNorm(brTfidf.calculateContentNorm(bugReportCorpus.getContentTokens()));
			bugReportCorpus.setSummaryTokens(brTfidf.vectorize(bugReportCorpus.getSummaryPart(), tokenScoreType));
			bugReportCorpus.setSummaryNorm(brTfidf.calculateContentNorm(bugReportCorpus.getSummaryTokens()));
			bugReportCorpus.setDescriptionTokens(brTfidf.vectorize(bugReportCorpus.getDescriptionPart(), tokenScoreType));
			bugReportCorpus.setDescriptionNorm(brTfidf.calculateContentNorm(bugReportCorpus.getDescriptionTokens()));
		}
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
