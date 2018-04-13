package edu.whu.pllab.buglocator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class Property {
	
	private final static String TIME_STR = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
	
	private final static String BR_TFIDF_PATH = "bugReport.tfidf";
	private final static String CODE_TFIDF_PATH = "sourceCode.tfidf";
	private final static String BR_PARAGRAPH_VECTOR_PATH = "bugReport.v";
	private final static String CODE_PARAGRAPH_VECTOR_PATH = "sourceCode.v";
	private final static String CODE_CHANGE_HISTORY_PATH = "sourceCode.history";
	private final static String TRAINING_FEATURES_PATH = "train.dat";
	private final static String TEST_FEATURES_PATH = "test.dat";
	private final static String SVM_RANK_MODEL_PATH = "model.dat";
	private final static String PREDICTIONS_PATH = "predictions";
	private final static String CODE_REPO_XML_PATH = "codeRepository.xml";
	private final static String FEATURES_EXTREMUM_PATH = "features.params";
	private final static String EVALUATE_LOG_PATH = "evaluate_" + TIME_STR + ".log";
	
	public final static int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
	public final static String STOPWORDS_PATH = Property.readProperty("STOPWORDS_PATH");
	public final static String SVM_RANK_LEARN_TOOL_PATH = Property.readProperty("SVM_RANK_LEARN_TOOL_PATH");
	public final static String SVM_RANK_CLASSIFY_TOOL_PATH = Property.readProperty("SVM_RANK_CLASSIFY_TOOL_PATH");
	public final static boolean USE_STRUCTURED_INFORMATION = Boolean.parseBoolean(Property.readProperty("USE_STRUCTURED_INFORMATION"));

	private static Property p = null;
	
	private String product;
	private String bugFilePath;
	private String sourceCodeDir;
	private String wordVectorPath;
	private String workingDir;
	private String brTfidfModelPath;
	private String codeTfidfModelPath;
	private String brParagraphVectorPath;
	private String codeParagraphVectorPath;
	private String codeChangeHistoryPath;
	private String trainingFeaturesPath;
	private String testFeaturesPath;
	private String svmRankModelPath;
	private String predictionsPath;
	private String codeRepositoryXMLPath;
	private String featuresExtremumPath;
	private String evaluateLogPath;
	
	private int splitNum;
	
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
		String brTfidfModelPath = new File(workingDir, BR_TFIDF_PATH).getAbsolutePath();
		String codeTfidfModelPath = new File(workingDir, CODE_TFIDF_PATH).getAbsolutePath();
		String brParagraphVectorPath = new File(workingDir, BR_PARAGRAPH_VECTOR_PATH).getAbsolutePath();
		String codeParagraphVectorPath = new File(workingDir, CODE_PARAGRAPH_VECTOR_PATH).getAbsolutePath();
		String codeChangeHistoryPath = new File(workingDir, CODE_CHANGE_HISTORY_PATH).getAbsolutePath();
		String trainingFeaturesPath = new File(workingDir, TRAINING_FEATURES_PATH).getAbsolutePath();
		String testFeaturesPath = new File(workingDir, TEST_FEATURES_PATH).getAbsolutePath();
		String svmRankModelPath = new File(workingDir, SVM_RANK_MODEL_PATH).getAbsolutePath();
		String predictionsPath = new File(workingDir, PREDICTIONS_PATH).getAbsolutePath();
		String codeRepositoryXMLPath = new File(workingDir, CODE_REPO_XML_PATH).getAbsolutePath();
		String featuresExtremumPath = new File(workingDir, FEATURES_EXTREMUM_PATH).getAbsolutePath();
		String evaluateLogPath = new File(workingDir, EVALUATE_LOG_PATH).getAbsolutePath();
		int splitNum = Integer.parseInt(Property.readProperty(targetProduct + "_" + "SPLIT_NUM"));
		File workingDirFile = new File(workingDir);
		if (!workingDirFile.exists()) {
			workingDirFile.mkdirs();
		}
		// set properties values
		p.setValues(product, bugFilePath, sourceCodeDir, wordVectorPath, workingDir, brTfidfModelPath,
				codeTfidfModelPath, brParagraphVectorPath, codeParagraphVectorPath, codeChangeHistoryPath,
				trainingFeaturesPath, testFeaturesPath, svmRankModelPath, predictionsPath, codeRepositoryXMLPath,
				featuresExtremumPath, evaluateLogPath, splitNum);
		return p;
	}
	
	
	/**
	 * set values for Property
	 */
	public void setValues(String product, String bugFilePath, String sourceCodeDir, String wordVectorPath,
			String workingDir, String brTfidfModelPath, String codeTfidfModelPath, String brParagraphVectorPath,
			String codeParagraphVectorPath, String codeChangeHistoryPath, String trainingFeaturesPath,
			String testFeaturesPath, String svmRankModelPath, String predictionsPath, String codeRepositoryXMLPath, 
			String featuresExtremumPath, String evaluateLogPath, int splitNum) {
		setProduct(product);
		setBugFilePath(bugFilePath);
		setSourceCodeDir(sourceCodeDir);
		setWordVectorPath(wordVectorPath);
		setWorkingDir(workingDir);
		setBrTfidfModelPath(brTfidfModelPath);
		setCodeTfidfModelPath(codeTfidfModelPath);
		setBrParagraphVectorPath(brParagraphVectorPath);
		setCodeParagraphVectorPath(codeParagraphVectorPath);
		setCodeChangeHistoryPath(codeChangeHistoryPath);
		setTrainingFeaturesPath(trainingFeaturesPath);
		setTestFeaturesPath(testFeaturesPath);
		setSVMRankModelPath(svmRankModelPath);
		setPredictionsPath(predictionsPath);
		setCodeRepositoryXMLPath(codeRepositoryXMLPath);
		setFeaturesExtremumPath(featuresExtremumPath);
		setEvaluateLogPath(evaluateLogPath);
		setSplitNum(splitNum);
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
		System.out.printf("Properties:\n");
		System.out.printf("USE_STRUCTURED_INFORMATION: %s\n", Property.USE_STRUCTURED_INFORMATION);
		System.out.printf("THREAD_COUNT: %d\n", Property.THREAD_COUNT);
		System.out.printf("StopwordsPath: %s\n", Property.STOPWORDS_PATH);
		System.out.printf("SVMRankToolPath: %s\n", Property.SVM_RANK_LEARN_TOOL_PATH);
		System.out.printf("SVMClassifyToolPath: %s\n", Property.SVM_RANK_CLASSIFY_TOOL_PATH);
		System.out.printf("Product: %s\n", getProduct());
		System.out.printf("BugFilePath: %s\n", getBugFilePath());
		System.out.printf("SourceCodeDir: %s\n", getSourceCodeDir());
		System.out.printf("WordVectorPath: %s\n", getWordVectorPath());
		System.out.printf("WorkingDir: %s\n", getWorkingDir());
		System.out.printf("BrTfidfModelPath: %s\n", getBrTfidfModelPath());
		System.out.printf("CodeTfidfModelPath: %s\n", getCodeTfidfModelPath());
		System.out.printf("BrParagraphVectorPath: %s\n", getBrParagraphVectorPath());
		System.out.printf("CodeParagraphVectorPath: %s\n", getCodeParagraphVectorPath());
		System.out.printf("TrainingFeaturesPath: %s\n", getTrainingFeaturesPath());
		System.out.printf("testFeaturesPath: %s\n", getTestFeaturesPath());
		System.out.printf("SVMRankModelPath: %s\n", getSVMRankModelPath());
		System.out.printf("predictionsPath: %s\n", getPredictionsPath());
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

	public String getTrainingFeaturesPath() {
		return trainingFeaturesPath;
	}

	public void setTrainingFeaturesPath(String trainingFeaturesPath) {
		this.trainingFeaturesPath = trainingFeaturesPath;
	}

	public String getTestFeaturesPath() {
		return testFeaturesPath;
	}

	public void setTestFeaturesPath(String testFeaturesPath) {
		this.testFeaturesPath = testFeaturesPath;
	}

	public String getSVMRankModelPath() {
		return svmRankModelPath;
	}

	public void setSVMRankModelPath(String svmRankModelPath) {
		this.svmRankModelPath = svmRankModelPath;
	}

	public String getPredictionsPath() {
		return predictionsPath;
	}

	public void setPredictionsPath(String predictionsPath) {
		this.predictionsPath = predictionsPath;
	}

	public String getCodeRepositoryXMLPath() {
		return codeRepositoryXMLPath;
	}

	public void setCodeRepositoryXMLPath(String codeRepositoryXMLPath) {
		this.codeRepositoryXMLPath = codeRepositoryXMLPath;
	}

	public String getFeaturesExtremumPath() {
		return featuresExtremumPath;
	}

	public void setFeaturesExtremumPath(String featuresExtremumPath) {
		this.featuresExtremumPath = featuresExtremumPath;
	}

	public String getEvaluateLogPath() {
		return evaluateLogPath;
	}

	public void setEvaluateLogPath(String evaluateLogPath) {
		this.evaluateLogPath = evaluateLogPath;
	}

	public int getSplitNum() {
		return splitNum;
	}

	public void setSplitNum(int splitNum) {
		this.splitNum = splitNum;
	}
	
	
	
}
