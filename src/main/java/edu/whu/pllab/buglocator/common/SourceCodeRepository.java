package edu.whu.pllab.buglocator.common;

import java.io.File;
import java.util.HashMap;
import java.util.List;
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
	
	private HashMap<String, SourceCode> sourceCodeMaps;
	
	/** use file's absolutePath substring(sourceCodeDirNameLength+1) as file path*/
	private int sourceCodeDirNameLength;
	
	/** Constructor */
	public SourceCodeRepository() {
		Property property = Property.getInstance();
		String sourceCodeDir = property.getSourceCodeDir();
		// initialize sourceCodeDirNameLength and sourceCodeMaps before loadSourceCodeFiles
		sourceCodeDirNameLength = new File(sourceCodeDir).getAbsolutePath().length();
		sourceCodeMaps = new HashMap<String, SourceCode>();
		loadSourceCodeFiles(sourceCodeDir);
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
		sourceCodeMaps = new HashMap<String, SourceCode>();
		loadSourceCodeFiles(sourceCodeDir);
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
			synchronized(sourceCodeMaps) {
				sourceCodeMaps.put(sourceCode.getPath(), sourceCode);
			}
		}
	}
	

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public HashMap<String, SourceCode> getSourceCodeMaps() {
		return sourceCodeMaps;
	}

	public void setSourceCodeMaps(HashMap<String, SourceCode> sourceCodeMaps) {
		this.sourceCodeMaps = sourceCodeMaps;
	}

	
}
