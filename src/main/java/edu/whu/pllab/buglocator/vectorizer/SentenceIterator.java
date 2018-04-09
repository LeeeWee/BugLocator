package edu.whu.pllab.buglocator.vectorizer;

import java.util.Map.Entry;

public interface SentenceIterator<T> {
 
	/**
	 * Gets the next id-sentence entry
	 * 
	 * @return the next id-sentence entry in the iterator
	 */
	Entry<T, String> nextEntry();
	
	 /**
     * Same idea as {@link java.util.Iterator}
     * @return whether there's anymore id-sentences left
     */
    boolean hasNext();
    
    /**
     * Sentences set size
     */
    int size();
    
    /**
     * Resets the iterator to the beginning
     */
    void reset();
    
}
