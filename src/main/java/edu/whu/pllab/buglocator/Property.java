package edu.whu.pllab.buglocator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Property {
	
	public final static String BR_INDEX_PATH = "bugReport.i";
	public final static String BR_CORPUS_PATH = "bugReport.s";
	public final static String BR_TFIDF_PATH = "bugReport.tfidf";
	public final static String BR_PARAGRAPH_VECTOR_PATH = "bugReport.v";
	public final static String CODE_INDEX_PATH = "sourceCode.i";
	public final static String CODE_CORPUS_PATH = "sourceCode.s";
	public final static String CODE_TFIDF_PATH = "sourceCode.tfidf";
	public final static String CODE_PARAGRAPH_VECTOR_PATH = "sourceCode.v";
	public final static String CODE_CHANGE_HISTORY_PATH = "sourceCode.history";
	
	
	public final static int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
	public final static String STOPWORDS_PATH = Property.readProperty("STOPWORDS_PATH");

	private static Property p = null;
	
	private String product;
	private String bugFilePath;
	private String sourceCodeDir;
	private String wordVectorPath;
	private String workingDir;
	private String brIndexPath;
	private String codeIndexPath;
	private String brCorpusPath;
	private String codeCorpusPath;
	private String brTfidfModelPath;
	private String codeTfidfModelPath;
	private String brParagraphVectorPath;
	private String codeParagraphVectorPath;
	private String codeChangeHistoryPath;

	/**
	 * Constructor
	 */
	private Property() {
		// do nothing
	}
	
	/**
	 * read property by key from buglocator.properties file
	 * @return value corresponded to given key
	 */
	private static String readProperty(String key) {
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream("buglocator.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return properties.getProperty(key);
	}
	
	
	//// singleton pattern
	/**
	 * load property by default product
	 */
	public static Property loadInstance() {
		String targetProduct = Property.readProperty("DEFAULT_PRODUCT");
		return loadInstance(targetProduct);
	}
	
	/**
	 * load Property instance for target Product
	 */
	public static Property loadInstance(String targetProduct) {
		if (p == null) 
			p = new Property();
		
		// load properties
		targetProduct = targetProduct.toUpperCase();
		String product = Property.readProperty(targetProduct + "_" + "PRODUCT");
		String bugFilePath = Property.readProperty(targetProduct + "_" + "BUG_REPO_FILE");
		String sourceCodeDir = Property.readProperty(targetProduct + "_" + "SOURCE_DIR");
		String wordVectorPath = Property.readProperty(targetProduct + "_" + "WORD_VECTOR_PATH");
		String workingDir = Property.readProperty(targetProduct + "_" + "WORK_DIR");
		String brIndexPath = new File(workingDir, BR_INDEX_PATH).getAbsolutePath();
		String codeIndexPath = new File(workingDir, CODE_INDEX_PATH).getAbsolutePath();
		String brCorpusPath = new File(workingDir, BR_CORPUS_PATH).getAbsolutePath();
		String codeCorpusPath = new File(workingDir, CODE_CORPUS_PATH).getAbsolutePath();
		String brTfidfModelPath = new File(workingDir, BR_TFIDF_PATH).getAbsolutePath();
		String codeTfidfModelPath = new File(workingDir, CODE_TFIDF_PATH).getAbsolutePath();
		String brParagraphVectorPath = new File(workingDir, BR_PARAGRAPH_VECTOR_PATH).getAbsolutePath();
		String codeParagraphVectorPath = new File(workingDir, CODE_PARAGRAPH_VECTOR_PATH).getAbsolutePath();
		String codeChangeHistoryPath = new File(workingDir, CODE_CHANGE_HISTORY_PATH).getAbsolutePath();
		// set properties values
		p.setValues(product, bugFilePath, sourceCodeDir, wordVectorPath, workingDir, brIndexPath, codeIndexPath,
				brCorpusPath, codeCorpusPath, brTfidfModelPath, codeTfidfModelPath, brParagraphVectorPath,
				codeParagraphVectorPath, codeChangeHistoryPath);

		return p;
	}
	

	
	/**
	 * set values for Property
	 */
	public void setValues(String product, String bugFilePath, String sourceCodeDir, String wordVectorPath,
			String workingDir, String brIndexPath, String codeIndexPath, String brCorpusPath, String codeCorpusPath,
			String brTfidfModelPath, String codeTfidfModelPath, String brParagraphVectorPath,
			String codeParagraphVectorPath, String codeChangeHistoryPath) {
		setProduct(product);
		setBugFilePath(bugFilePath);
		setSourceCodeDir(sourceCodeDir);
		setWordVectorPath(wordVectorPath);
		setWorkingDir(workingDir);
		setBrIndexPath(brIndexPath);
		setCodeIndexPath(codeIndexPath);
		setBrCorpusPath(brCorpusPath);
		setCodeCorpusPath(codeCorpusPath);
		setBrTfidfModelPath(brTfidfModelPath);
		setCodeTfidfModelPath(codeTfidfModelPath);
		setBrParagraphVectorPath(brParagraphVectorPath);
		setCodeParagraphVectorPath(codeParagraphVectorPath);
		setCodeChangeHistoryPath(codeChangeHistoryPath);
	}

	/**
	 * get property instatnce
	 */
	public static Property getInstance() {
		return p;
	}

	/**
	 * print values of properties
	 */
	public void printValues() {
		System.out.printf("THREAD_COUNT: %d\n", Property.THREAD_COUNT);
		
		System.out.printf("BugFilePath: %s\n", getBugFilePath());
		System.out.printf("SourceCodeDir: %s\n", getSourceCodeDir());
		System.out.printf("WordVectorPath: %s\n", getWordVectorPath());
		System.out.printf("WorkingDir: %s\n", getWorkingDir());
		System.out.printf("brCorpusPath: %s\n", getBrCorpusPath());
		System.out.printf("codeCorpusPath: %s\n", getCodeCorpusPath());
		System.out.printf("brTfidfModelPath: %s\n", getBrTfidfModelPath());
		System.out.printf("codeTfidfModelPath: %s\n", getCodeTfidfModelPath());
	}
	
	
	//// setter and getter of properties 
	public String getProduct() {
		return product;
	}

	public void setProduct(String product) {
		this.product = product;
	}

	public String getBugFilePath() {
		return bugFilePath;
	}

	public void setBugFilePath(String bugFilePath) {
		this.bugFilePath = bugFilePath;
	}

	public String getSourceCodeDir() {
		return sourceCodeDir;
	}

	public void setSourceCodeDir(String sourceCodeDir) {
		this.sourceCodeDir = sourceCodeDir;
	}

	public String getWordVectorPath() {
		return wordVectorPath;
	}

	public void setWordVectorPath(String wordVectorPath) {
		this.wordVectorPath = wordVectorPath;
	}

	public String getWorkingDir() {
		return workingDir;
	}

	public void setWorkingDir(String workingDir) {
		this.workingDir = workingDir;
	}

	public String getBrIndexPath() {
		return brIndexPath;
	}

	public void setBrIndexPath(String brIndexPath) {
		this.brIndexPath = brIndexPath;
	}

	public String getCodeIndexPath() {
		return codeIndexPath;
	}

	public void setCodeIndexPath(String codeIndexPath) {
		this.codeIndexPath = codeIndexPath;
	}
	
	public String getBrCorpusPath() {
		return brCorpusPath;
	}

	public void setBrCorpusPath(String brCorpusPath) {
		this.brCorpusPath = brCorpusPath;
	}

	public String getCodeCorpusPath() {
		return codeCorpusPath;
	}

	public void setCodeCorpusPath(String codeCorpusPath) {
		this.codeCorpusPath = codeCorpusPath;
	}

	public String getBrTfidfModelPath() {
		return brTfidfModelPath;
	}

	public void setBrTfidfModelPath(String brTfidfModelPath) {
		this.brTfidfModelPath = brTfidfModelPath;
	}

	public String getCodeTfidfModelPath() {
		return codeTfidfModelPath;
	}

	public void setCodeTfidfModelPath(String codeTfidfModelPath) {
		this.codeTfidfModelPath = codeTfidfModelPath;
	}
	
	public String getBrParagraphVectorPath() {
		return brParagraphVectorPath;
	}

	public void setBrParagraphVectorPath(String brParagraphVectorPath) {
		this.brParagraphVectorPath = brParagraphVectorPath;
	}

	public String getCodeParagraphVectorPath() {
		return codeParagraphVectorPath;
	}

	public void setCodeParagraphVectorPath(String codeParagraphVectorPath) {
		this.codeParagraphVectorPath = codeParagraphVectorPath;
	}
	public String getCodeChangeHistoryPath() {
		return codeChangeHistoryPath;
	}

	public void setCodeChangeHistoryPath(String codeChangeHistoryPath) {
		this.codeChangeHistoryPath = codeChangeHistoryPath;
	}

}
