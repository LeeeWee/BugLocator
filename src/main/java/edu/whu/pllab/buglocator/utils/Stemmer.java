package edu.whu.pllab.buglocator.utils;

public class Stemmer {

	public static String stem(String word) {
		PorterStemmer stemmer = new PorterStemmer();
		stemmer.reset();
        stemmer.stem(word);
        return stemmer.toString();
	}
	
}
