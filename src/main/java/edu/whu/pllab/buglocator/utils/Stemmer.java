package edu.whu.pllab.buglocator.utils;

public class Stemmer {

	public static String stem(String word) {
		PorterStemmer stemmer = new PorterStemmer();
		stemmer.reset();
        stemmer.stem(word);
        return stemmer.toString();
	}
	
	public static String stemContent(String contents) {
		String[] contentsArray = contents.split(" ");
		StringBuffer contentBuf = new StringBuffer();
		for (int i = 0; i < contentsArray.length; i++) {
			String word = contentsArray[i].toLowerCase();
			if (word.length() > 0) {
				String stemWord = stem(word);
				contentBuf.append(stemWord);
				contentBuf.append(" ");
			}
		}
		return contentBuf.toString();
	}
	
}
