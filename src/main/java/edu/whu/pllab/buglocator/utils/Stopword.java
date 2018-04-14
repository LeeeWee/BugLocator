package edu.whu.pllab.buglocator.utils;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import edu.whu.pllab.buglocator.Property;

public class Stopword {

	// For checking source file java term
    private static final Set<String> JAVA_KEYWORDS_STOP_WORD_SET;
    // For checking source file project term
    private static final Set<String> PROJECT_KEYWORDS_STOP_WORD_SET;
    // For checking source file term and bug report
    private static final Set<String> ENG_STOP_WORDS_SET;
	
    public Stopword()
    {
    }

    public static boolean isJavaKeyword(String word)
    {
        boolean isKeyword = JAVA_KEYWORDS_STOP_WORD_SET.contains(word);
        return isKeyword;
    }
    
    public static boolean isProjectKeyword(String word)
    {
        boolean isProjectKeyword = PROJECT_KEYWORDS_STOP_WORD_SET.contains(word);
        return isProjectKeyword;
    }

    
    public static boolean isEnglishStopword(String word)
    {
        boolean isEnglishStopword = ENG_STOP_WORDS_SET.contains(word);
        return isEnglishStopword;
    }


    static 
    {
    	// References
    	// http://docs.oracle.com/javase/tutorial/java/nutsandbolts/_keywords.html
    	// http://en.wikipedia.org/wiki/List_of_Java_keywords#Reserved_words_for_literal_values
        String javaKeywords[] = { "abstract", "continue", "for",
				"new", "switch", "assert", "default", "goto", "package",
				"synchronized", "boolean", "do", "if", "private", "this",
				"break", "double", "implements", "protected", "throw", "byte",
				"else", "import", "public", "throws", "case", "enum",
				"instanceof", "return", "transient", "catch", "extends", "int",
				"short", "try", "char", "final", "interface", "static", "void",
				"class", "finally", "long", "strictfp", "volatile", "const",
				"float", "native", "super", "while", "string", "main", "args", 
				"null", "this", "extends", "true", "false" };
        
        JAVA_KEYWORDS_STOP_WORD_SET = new TreeSet<String>();
        for(int i = 0; i < javaKeywords.length; i++)
        {
            String word = javaKeywords[i].trim().toLowerCase();
            word = Stemmer.stem(word);
            JAVA_KEYWORDS_STOP_WORD_SET.add(word);
        }
        
        String projectKeywords[] = {
                "org", "eclipse", "swt"
                };
        
        PROJECT_KEYWORDS_STOP_WORD_SET = new TreeSet<String>();
        for(int i = 0; i < projectKeywords.length; i++)
        {
            String word = projectKeywords[i].trim().toLowerCase();
            word = Stemmer.stem(word);
            PROJECT_KEYWORDS_STOP_WORD_SET.add(word);
        }

        List<String> EngStopWordsList = FileUtil.readFileToList(Property.STOPWORDS_PATH);
        String EngStopWords[] = EngStopWordsList.toArray(new String[EngStopWordsList.size()]);
        ENG_STOP_WORDS_SET = new TreeSet<String>();
        for(int i = 0; i < EngStopWords.length; i++)
        {
            String word = EngStopWords[i].toLowerCase().trim();
            word = Stemmer.stem(word);
            ENG_STOP_WORDS_SET.add(word);
        }
    }
}
