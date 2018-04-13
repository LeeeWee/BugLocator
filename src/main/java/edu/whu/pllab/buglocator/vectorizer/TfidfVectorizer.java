package edu.whu.pllab.buglocator.vectorizer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.deeplearning4j.util.MathUtils;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.TokenScore;
import edu.whu.pllab.buglocator.common.TokenScore.ScoreType;

public class TfidfVectorizer<T> {

	
	/** map doc name to temrs set */
	private ConcurrentHashMap<T, HashSet<String>> docTermsMap;
	
	/** <term, <doc-name, term-frequency-in-doc>> */
	private ConcurrentHashMap<String, HashMap<T, Integer>> docTermFrequencyMap;
	
	/** count word occurrence count */
	private HashMap<String, Integer> wordCounts;
	
	/** filter words occurring less than min-word-frequency */
	protected int minWordFrequency;
	
	/** the total of number of documents encountered in the corpus */
	private int totalNumberOfDocs;
	
	/** idf value table */
	private Hashtable<String, Double> inverseDocFrequencyTable;
	
	/** iterate sentence */
	private SentenceIterator<T> iter;
	
	public TfidfVectorizer(SentenceIterator<T> iter) {
		this.iter = iter;
		this.minWordFrequency = 0;
		this.totalNumberOfDocs = 0;
		docTermsMap = new ConcurrentHashMap<T, HashSet<String>>();
		docTermFrequencyMap = new ConcurrentHashMap<String, HashMap<T, Integer>>();
		wordCounts = new HashMap<String, Integer>();
	}
	
	public TfidfVectorizer(SentenceIterator<T> iter, int minWordFrequency) {
		this.iter = iter;
		this.minWordFrequency = minWordFrequency;
		this.totalNumberOfDocs = 0;
		docTermsMap = new ConcurrentHashMap<T, HashSet<String>>();
		docTermFrequencyMap = new ConcurrentHashMap<String, HashMap<T, Integer>>();
		wordCounts = new HashMap<String, Integer>();
	}
	
	/** train the model */
	public void fit() {
		totalNumberOfDocs = 0;
		ExecutorService executor = Executors.newFixedThreadPool(Property.THREAD_COUNT);
		while (iter.hasNext()) {
			VocabRunnable runnable = new VocabRunnable(iter.nextEntry());
			executor.execute(runnable);
			totalNumberOfDocs++;
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
		}
		
		countWords();
			
		if (minWordFrequency > 0) 
			filterVocab(minWordFrequency);
		
		calculateInverseDocFrequencyTable();
	}
	
	
	/** update tfidf model */
	public void update(SentenceIterator<T> addedSentenceIter, SentenceIterator<T> modifiedSentenceIter,
			SentenceIterator<T> deletedSentenceIter) {
		
		// reduce modified sentence size and add in mergeTfidf() method
		totalNumberOfDocs -= modifiedSentenceIter.size();
		totalNumberOfDocs -= deletedSentenceIter.size();
		
		while (modifiedSentenceIter.hasNext()) {
			Entry<T, String> modify = modifiedSentenceIter.nextEntry();
			T label = modify.getKey();
			// get tokens in previous labeled string
			HashSet<String> tokens = docTermsMap.get(label);
			// get tokens count
			for (String token : tokens) {
				// reduce word frequency from wordCounts
				int count = docTermFrequencyMap.get(token).get(label);
				wordCounts.put(token, wordCounts.get(token) - count);
				
				// remove label from docTermFrequencyMap
				docTermFrequencyMap.get(token).remove(label);
			}
			
			// remove label from docTermsMap
			docTermsMap.remove(label);
		}
		
		while (deletedSentenceIter.hasNext()) {
			Entry<T, String> delete = deletedSentenceIter.nextEntry();
			T label = delete.getKey();
			// get tokens in previous labeled string
			HashSet<String> tokens = docTermsMap.get(label);
			// get tokens count
			for (String token : tokens) {
				// reduce word frequency from wordCounts
				int count = docTermFrequencyMap.get(token).get(label);
				wordCounts.put(token, wordCounts.get(token) - count);
				
				// remove label from docTermFrequencyMap
				docTermFrequencyMap.get(token).remove(label);
			}
			
			// remove label from docTermsMap
			docTermsMap.remove(label);
		}
		
		addedSentenceIter.reset();
		TfidfVectorizer<T> addedTfidf = new TfidfVectorizer<T>(addedSentenceIter);
		addedTfidf.fit();
		mergeTfidf(addedTfidf);
		
		modifiedSentenceIter.reset();
		TfidfVectorizer<T> modifiedTfidf = new TfidfVectorizer<T>(modifiedSentenceIter);
		modifiedTfidf.fit();
		mergeTfidf(modifiedTfidf);
		
		calculateInverseDocFrequencyTable();
	}
	
	/** merge given tfidf model to current one */
	public void mergeTfidf(TfidfVectorizer<T> tfidf) {
		// merge docTermsMap
		docTermsMap.putAll(tfidf.docTermsMap);
		//merge docTermFrequencyMap
		for (Entry<String, HashMap<T, Integer>> entry : tfidf.docTermFrequencyMap.entrySet()) {
			if (docTermFrequencyMap.containsKey(entry.getKey()))
				docTermFrequencyMap.get(entry.getKey()).putAll(entry.getValue());
			else 
				docTermFrequencyMap.put(entry.getKey(), entry.getValue());
		}
		// merge wordCounts
		for (Entry<String, Integer> entry : tfidf.wordCounts.entrySet()) {
			if (wordCounts.containsKey(entry.getKey()))
				wordCounts.put(entry.getKey(), wordCounts.get(entry.getKey()) + entry.getValue());
			else 
				wordCounts.put(entry.getKey(), entry.getValue());
		}
		// merge totalNumberOfDocs
		totalNumberOfDocs += tfidf.totalNumberOfDocs;
	}
	
	/** count words total frequency and save to wordCounts */
	private void countWords() {
		for (Entry<String, HashMap<T, Integer>> entry : docTermFrequencyMap.entrySet()) {
			String token = entry.getKey();
			int count = 0;
			for (Entry<T, Integer> countsEntry : entry.getValue().entrySet()) {
				count += countsEntry.getValue();
			}
			wordCounts.put(token, count);
		} 
	}
	
	/** filter words occurring less than min-word-frequency */
	public void filterVocab(int minWordFrequency) {
		if (wordCounts.isEmpty())
			countWords();
		Iterator<Entry<String, Integer>> iter = wordCounts.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, Integer> entry = iter.next();
			if (entry.getValue() < minWordFrequency) {
				// remove token from docTermFrequencyMap
				Set<T> labels = docTermFrequencyMap.get(entry.getKey()).keySet();
				docTermFrequencyMap.remove(entry.getKey());
				// remove token from docTermsMap
				for (T label : labels) {
					docTermsMap.get(label).remove(entry.getKey());
				}
				// remove token from wordCounts
				iter.remove();
			}
		}
	}
	
	/** calculate idf value for all words in wordCounts */
	public void calculateInverseDocFrequencyTable() {
		inverseDocFrequencyTable = new Hashtable<String, Double>();
		for (String word : wordCounts.keySet()) {
			double idf = Math.log10(totalNumberOfDocs() / docAppearedIn(word));
			inverseDocFrequencyTable.put(word, idf);
		}
	}
	
	/** Returns true if tfidf vocab cache contains the given word */
	public boolean containsWord(String word) {
		return wordCounts.containsKey(word);
	} 
	
	/** Count of documents a word appeared in */
	public int docAppearedIn(String word) {
		return docTermFrequencyMap.get(word).size();
	}
	
	/** Return the total of number of documents encountered in the corpus */
	public int totalNumberOfDocs() {
		return totalNumberOfDocs;
	}
	
	/** Returns the number of times the word has occurred */
	public int wordFrequency(String word) {
		return wordCounts.get(word);
	}
	
	/** Returns all of the words in the vocab */
	public Set<String> words() {
		return wordCounts.keySet();
	}
	
	/** idf value for given word */
	public double idfForWord(String word) {
		if (inverseDocFrequencyTable == null) 
			calculateInverseDocFrequencyTable();
    	if (!inverseDocFrequencyTable.containsKey(word))
    		return 0.0;
    	return inverseDocFrequencyTable.get(word);
    }
	
	protected class VocabRunnable implements Runnable {

		Entry<T, String> labeledSentence;
		
		public VocabRunnable(Entry<T, String> labeledSentence) {
			this.labeledSentence = labeledSentence;
		}
		
		@Override
		public void run() {
			T label = labeledSentence.getKey();
			String sentence = labeledSentence.getValue();
			String[] tokens = sentence.split(" ");
			
			HashMap<String, AtomicInteger> counts = new HashMap<String, AtomicInteger>();
			for (String token : tokens) {
				// count words
				if (!counts.containsKey(token)) 
					counts.put(token, new AtomicInteger(0));
				counts.get(token).getAndIncrement();
			}
			
			docTermsMap.put(label, new HashSet<String>(counts.keySet()));
			
			for (Entry<String, AtomicInteger> entry : counts.entrySet()) {
				String token = entry.getKey();
				Integer count = entry.getValue().get();
				
				// add token to docTermFrequencyMap
				if (!docTermFrequencyMap.containsKey(token)) {
					HashMap<T, Integer> map = new HashMap<T, Integer>();
					docTermFrequencyMap.put(token, map);
				}
				docTermFrequencyMap.get(token).put(label, count);
			}
			
		}
	}
	
	/**
	 * calculate tokens weight for each token in content 
	 * @param content input content String
	 * @param tokenScoreType given TokenScore Type, including NTFIDF, LOGTFIDF, WFIDF and TFIDF
	 * @return tokensScore map
	 */
	public HashMap<String, TokenScore> vectorize(String content, ScoreType tokenScoreType) {
		HashMap<String, TokenScore> contentTokens = new HashMap<String, TokenScore>();
		String[] tokens = content.split(" ");
		HashMap<String, Integer> tokensCount = new HashMap<String, Integer>();
		int documentLength = 0;
		// get tokens term frequency
		for (String token : tokens) {
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
		if (tokenScoreType == ScoreType.NTFIDF) {
			for (Integer count : tokensCount.values()) {
				if (count > maxWordCount)
					maxWordCount = count;
			}
		}
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
		}
		return contentTokens;
	}
	
	/** calculate tokens content norm for contentTokens map */
	public double calculateContentNorm(HashMap<String, TokenScore> contentTokens) {
		double contentNorm = 0.0;
		for (TokenScore token : contentTokens.values()) {
			contentNorm += token.getTokenWeight();
		}
		return Math.sqrt(contentNorm);
	}
	

	
    public double tfForWord(long wordCount, long documentLength) {
        return (double) wordCount / (double) documentLength;
    }
    
    public double ntfForWord(long wordCount, long maxWordCount) {
    	return 0.5 + 0.5 * (double) wordCount / maxWordCount;
    }
    
    public double wfForWord(long wordCount) {
    	return 1 + Math.log10(wordCount);
    }
    
    public double logTfForWord(long wordCount, double aveWordCount) {
    	return (1 + Math.log10(wordCount)) / (1 + Math.log10(aveWordCount));
    }

}
