package edu.whu.pllab.buglocator.utils;

public class Stemmer {

	private static PorterStemmer stemmer = new PorterStemmer();
	
	public static String stem(String word) {
		stemmer.reset();
        stemmer.stem(word);
        return stemmer.toString();
	}
	
}
