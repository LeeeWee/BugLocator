package edu.whu.pllab.buglocator.common;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.astparser.FileParser;
import edu.whu.pllab.buglocator.utils.FileUtil;

public class SourceCodeRepository {

	private static final Logger logger = LoggerFactory.getLogger(BugReportRepository.class);

	/** source code repository commit version */
	private String version;
	
	private HashMap<String, SourceCode> SourceCodeMap;
	
	/** use file's absolutePath substring(sourceCodeDirNameLength+1) as file path*/
	private int sourceCodeDirNameLength;
	
	/** Constructor */
	public SourceCodeRepository() {
		Property property = Property.getInstance();
		String sourceCodeDir = property.getSourceCodeDir();
		// initialize sourceCodeDirNameLength and sourceCodeMaps before loadSourceCodeFiles
		sourceCodeDirNameLength = new File(sourceCodeDir).getAbsolutePath().length();
		SourceCodeMap = new HashMap<String, SourceCode>();
		loadSourceCodeFiles(sourceCodeDir);
		computeLengthScore();
	}
	
	/**
	 * Constructor
	 * @param version input source code repository commit version
	 */
	public SourceCodeRepository(String version) {
		logger.info("Resetting source code repository to version " + version + "...");
		Property property = Property.getInstance();
		String sourceCodeDir = property.getSourceCodeDir();
		// reset to given commitID version
		try {
			// initialize git repository
			Repository repo = FileRepositoryBuilder.create(new File(sourceCodeDir, ".git"));
			@SuppressWarnings("resource")
			Git git = new Git(repo);
			git.reset().setMode(ResetType.HARD).setRef(version).call();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// initialize sourceCodeDirNameLength and sourceCodeMaps before loadSourceCodeFiles
		sourceCodeDirNameLength = new File(sourceCodeDir).getAbsolutePath().length();
		SourceCodeMap = new HashMap<String, SourceCode>();
		loadSourceCodeFiles(sourceCodeDir);
		computeLengthScore();
	}
	
	/**
	 * get all sourceCode files(.java file) from given sourceCode directory with multi thread
	 * @param sourceCodeDir input sourceCode directory path
	 * @return map sourceCode path to sourceCode
	 */
	public void loadSourceCodeFiles(String sourceCodeDir) {
		// get all java files under sourceCode directory
		List<String> javaFiles = FileUtil.getAllFiles(sourceCodeDir, ".java");
		logger.info("Parsing java files...");
		// parse java files with multi thread
		ExecutorService executor = Executors.newFixedThreadPool(Property.THREAD_COUNT);
		for (String javaFilePath : javaFiles) {
			Runnable worker = new WorkerThread(javaFilePath);
			executor.execute(worker);
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
		}
		logger.info("Finished parsing, total " + javaFiles.size() + " java files.");
	}
	
	/** worker parsing source code file */
	private class WorkerThread implements Runnable {
		
		private String filePath;
		
		private WorkerThread(String filePath) {
			this.filePath = filePath;
		}
		
		public void run() {
			FileParser parser = new FileParser(new File(filePath));
			String path = filePath.substring(sourceCodeDirNameLength + 1);
			// create sourceCode
			SourceCode sourceCode = new SourceCode(path);
			// set fullClassName
			String fullClassName = parser.getPackageName();
			if (fullClassName.trim().equals("")) 
				fullClassName = path;
			else {
				fullClassName = (new StringBuilder(String.valueOf(fullClassName)))
						.append(".").append(new File(filePath).getName()).toString();
			}
			fullClassName = fullClassName.substring(0, fullClassName.lastIndexOf("."));
			sourceCode.setFullClassName(fullClassName);
			// set methodList
			List<Method> methodList = parser.getAllMethodList();
			sourceCode.setMethodList(methodList);
			// set sourceCodeCorpus
			SourceCodeCorpus sourceCodeCorpus = new SourceCodeCorpus(parser.getContent());
			sourceCode.setSourceCodeCorpus(sourceCodeCorpus);
			// put to map
			synchronized(SourceCodeMap) {
				SourceCodeMap.put(sourceCode.getPath(), sourceCode);
			}
		}
	}
	
	/** compute length score for source code file */
	public void computeLengthScore() {
		logger.info("Calculating source code file length score...");
		HashMap<String, Integer> corpusLensTable = new HashMap<String, Integer>();
		int max = Integer.MIN_VALUE;
		int count = 0, sum = 0;
		for (Entry<String, SourceCode> entry : SourceCodeMap.entrySet()) {
			String content = entry.getValue().getSourceCodeCorpus().getContent();
			int lens = content.split(" ").length;
			corpusLensTable.put(entry.getKey(), lens);
			if (lens != 0) {
				count++;
			}
			if (lens > max) {
				max = lens;
			}
			sum += lens;
		}
		double average = (double) sum / (double) count;
		double squareDevi = 0.0D;
		for (Integer lens : corpusLensTable.values()) {
			squareDevi += ((double)lens - average) * ((double)lens - average);
		}
		double standardDevi = Math.sqrt(squareDevi / (double) count);
		double low = average - 3D * standardDevi;
		double high = average + 3D * standardDevi;
		int min = 0;
		if (low > 0.0D) {
			min = (int) low;
		}
		for (Entry<String, Integer> entry : corpusLensTable.entrySet()) {
			String filePath = entry.getKey();
			int lens = entry.getValue();
			double score = 0.0D;
			double nor = getNormalizedValue(lens, high, min);
			if (lens != 0) {
				if ((double) lens > low && (double) lens < high) {
					score = getLengthScore(nor);
				} else if ((double) lens < low) {
					score = 0.5D;
				} else {
					score = 1.0D;
				}
			} else {
				score = 0.0D;
			}
			if (nor > 6D) {
				nor = 6D;
			}
			if (score < 0.5D) {
				score = 0.5D;
			}
			SourceCodeMap.get(filePath).setLengthScore(score);
		}
	}
	
	/** Get normalized value of x from Max. to min. */
	private double getNormalizedValue(int x, double max, double min) {
		return (6F * (x - min)) / (max - min);
	}

	/** Get length score */
	public double getLengthScore(double len) {
		return Math.exp(len) / (1.0D + Math.exp(len));
	}
	

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public HashMap<String, SourceCode> getSourceCodeMaps() {
		return SourceCodeMap;
	}

	public void setSourceCodeMaps(HashMap<String, SourceCode> sourceCodeMaps) {
		this.SourceCodeMap = sourceCodeMaps;
	}

	
}
