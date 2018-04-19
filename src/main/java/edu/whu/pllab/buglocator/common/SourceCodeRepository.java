package edu.whu.pllab.buglocator.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.astparser.FileParser;
import edu.whu.pllab.buglocator.utils.FileUtil;
import edu.whu.pllab.buglocator.utils.Stemmer;

public class SourceCodeRepository {

	private static final Logger logger = LoggerFactory.getLogger(BugReportRepository.class);

	/** source code repository commit version */
	private String version;
	
	private HashMap<String, SourceCode> sourceCodeMap;
	
	/** use file's absolutePath substring(sourceCodeDirNameLength+1) as file path*/
	private int sourceCodeDirNameLength;
	
	/** source code dir path */
	private String sourceCodeDir;
	
	/** whether use structured information */
	private boolean useStructuredInformation;
	
	/** git repository of sourceCode */
	private Repository repo;
	
	/** git instance */
	private Git git;
	
	/** store change history of each file to help quickly setting change history for
	 * new source code file after checkout */
	private HashMap<String, long[]> changeHistory;
	
	/** store related BugReports of each file to help quickly setting related BugReports for
	 * new source code file after checkout */
	private HashMap<String, List<BugReport>> relatedBugReports;
	
	/** added, modified, deleted files between two version */
	private HashMap<String, SourceCode> addedFiles;
	private HashMap<String, SourceCode> modifiedFiles;
	private HashMap<String, SourceCode> deletedFiles;
	
	/** Constructor */
	public SourceCodeRepository() {
		Property property = Property.getInstance();
		useStructuredInformation = Property.USE_STRUCTURED_INFORMATION;
		sourceCodeDir = property.getSourceCodeDir();
		
		// initialize git repository
		try {
			repo = FileRepositoryBuilder.create(new File(sourceCodeDir, ".git"));
			git = new Git(repo);
			if (repo.findRef("HEAD") != null)
				this.version = repo.findRef("HEAD").getObjectId().getName();
			logger.info("Current commit version id: " + this.version);
		} catch (IOException e) {
		}
		
		// initialize sourceCodeDirNameLength and sourceCodeMaps before loadSourceCodeFiles
		sourceCodeDirNameLength = new File(sourceCodeDir).getAbsolutePath().length();
		sourceCodeMap = new HashMap<String, SourceCode>();
		// if code repository xml file exists, load code repository from xml file,
		// otherwise, load by parsing source code from source code dir
		File codeRepoXMLFile = new File(property.getCodeRepositoryXMLPath());
		if (codeRepoXMLFile.exists())
			sourceCodeMap = parseXMLOfSourceCodeRepo(property.getCodeRepositoryXMLPath());
		else {
			loadSourceCodeFiles(sourceCodeDir);
//			saveSourceCodeRepoToXML(property.getCodeRepositoryXMLPath(), property.getProduct());
		}
		loadSourceCodeChangeHistory(property.getCodeChangeHistoryPath());
		computeLengthScore();
	}
	
	/**
	 * Constructor
	 * @param version input source code repository commit version
	 */
	public SourceCodeRepository(String version) {
		logger.info("Resetting source code repository to version " + version + "...");
		useStructuredInformation = Property.USE_STRUCTURED_INFORMATION;
		this.version = version;
		Property property = Property.getInstance();
		sourceCodeDir = property.getSourceCodeDir();
		// reset to given commitID version
		try {
			// initialize git repository
			repo = FileRepositoryBuilder.create(new File(sourceCodeDir, ".git"));
			git = new Git(repo);
			git.reset().setMode(ResetType.HARD).setRef(version).call();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// initialize sourceCodeDirNameLength and sourceCodeMaps before loadSourceCodeFiles
		sourceCodeDirNameLength = new File(sourceCodeDir).getAbsolutePath().length();
		sourceCodeMap = new HashMap<String, SourceCode>();
		loadSourceCodeFiles(sourceCodeDir);
//		saveSourceCodeRepoToXML(property.getCodeRepositoryXMLPath(), property.getProduct());
		loadSourceCodeChangeHistory(property.getCodeChangeHistoryPath());
		computeLengthScore();
	}
	
	/** reset source code repository to given version, and get added, modified, deleted files */
	public void checkout(String version) {
		logger.info("Previous version: " + this.version + ", git checkout " + version + "...");
		
		// initialize or clear added, modified, deleted files list
		if (addedFiles == null)
			addedFiles = new HashMap<String, SourceCode>();
		else
			addedFiles.clear();
		if (modifiedFiles == null)
			modifiedFiles = new HashMap<String, SourceCode>();
		else
			modifiedFiles.clear();
		if (deletedFiles == null)
			deletedFiles = new HashMap<String, SourceCode>();
		else
			deletedFiles.clear();
		
		List<String> addedFilesList = new ArrayList<String>();
		List<String> modifiedFilesList = new ArrayList<String>();
		List<String> deletedFilesList = new ArrayList<String>();
		
		// git process
		try {
			git.reset().setMode(ResetType.HARD).setRef(version).call();
			
			// get added, modified and deleted files
			// Get the id of the tree associated to the two commits
			ObjectId head = repo.resolve(version + "^{tree}");
			ObjectId previousHead = repo.resolve(this.version + "^{tree}");
			// Instanciate a reader to read the data from the Git database
			ObjectReader reader = repo.newObjectReader();
			// Create the tree iterator for each commit
			CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
			oldTreeIter.reset(reader, previousHead);
			CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
			newTreeIter.reset(reader, head);
			List<DiffEntry> listDiffs = git.diff().setOldTree(oldTreeIter).setNewTree(newTreeIter).call();
			// save files to correspond list
			for (DiffEntry diff : listDiffs) {
				if (diff.getChangeType() == ChangeType.ADD) {
					if (!diff.getNewPath().endsWith(".java"))
						continue;
					addedFilesList.add(new File(sourceCodeDir, diff.getNewPath()).getAbsolutePath());
				}
				else if (diff.getChangeType()== ChangeType.MODIFY) {
					if (!diff.getNewPath().endsWith(".java"))
						continue;
					modifiedFilesList.add(new File(sourceCodeDir, diff.getNewPath()).getAbsolutePath());
				}
				else if (diff.getChangeType()== ChangeType.DELETE) {
					if (!diff.getOldPath().endsWith(".java"))
						continue;
					deletedFilesList.add(new File(sourceCodeDir, diff.getOldPath()).getAbsolutePath());
				}
			}
			
			// reset verison
			this.version = version;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.info("added: " + addedFilesList.size() + "\tmodified: " + modifiedFilesList.size() + "\tdeleted: "
				+ deletedFilesList.size());
		
		// remove deletedFiles and change added and modified files from sourceCodeMap
		List<String> addedAndModifiedFiles = new ArrayList<String>();
		addedAndModifiedFiles.addAll(addedFilesList);
		addedAndModifiedFiles.addAll(modifiedFilesList);
		
		for (String filePath : modifiedFilesList) {
			String path = filePath.substring(sourceCodeDirNameLength + 1).replaceAll("\\\\", "/");
			sourceCodeMap.remove(path);
		}
		for (String filePath: deletedFilesList) {
			String path = filePath.substring(sourceCodeDirNameLength + 1).replaceAll("\\\\", "/");
			sourceCodeMap.remove(path);
		}
		
		ExecutorService executor = Executors.newFixedThreadPool(Property.THREAD_COUNT);
		for (String javaFilePath : addedAndModifiedFiles) {
			Runnable worker = new WorkerThread(javaFilePath);
			executor.execute(worker);
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
		}
		
		// update change history and related bugReports
		setSourceCodeChangeHistory(addedAndModifiedFiles);
		attachRelatedBugReports(addedAndModifiedFiles);
	}
	
	/**
	 * get all sourceCode files(.java file) from given sourceCode directory with multi thread
	 * @param sourceCodeDir input sourceCode directory path
	 * @return map sourceCode path to sourceCode
	 */
	public void loadSourceCodeFiles(String sourceCodeDir) {
		// get all java files under sourceCode directory
		List<String> javaFiles = FileUtil.getAllFiles(sourceCodeDir, ".java");
		logger.info("Begining parsing source code, total " + javaFiles.size() + " java files.");
		// parse java files with multi thread
		ExecutorService executor = Executors.newFixedThreadPool(Property.THREAD_COUNT);
		for (String javaFilePath : javaFiles) {
			Runnable worker = new WorkerThread(javaFilePath);
			executor.execute(worker);
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
		}
		logger.info("Finished parsing!");
	}
	
	/** worker parsing source code file */
	private class WorkerThread implements Runnable {
		
		private String filePath;
		
		private WorkerThread(String filePath) {
			this.filePath = filePath;
		}
		
		public void run() {
			FileParser parser = new FileParser(new File(filePath));
			String path = filePath.substring(sourceCodeDirNameLength + 1).replaceAll("\\\\", "/");
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
			SourceCodeCorpus sourceCodeCorpus = new SourceCodeCorpus();
			sourceCodeCorpus.setImportedClasses(parser.getImportedClasses());
			// if useStructuredInformation, set classPart, methodPart, commentPart and
			// variablePart for sourceCodeCorpus, else set content only
			if (!useStructuredInformation) {
				String content = parser.getContent();
				// append class and method names
				String classNameAndMethodName = parser.getClassNameAndMethodName();
				StringBuffer nameBuf = new StringBuffer();
			    for (String word : classNameAndMethodName.split(" ")) {
				    String stemWord = Stemmer.stem(word.toLowerCase());
				    nameBuf.append(stemWord);
				    nameBuf.append(" ");
			    }
			    String names = nameBuf.toString();
				sourceCodeCorpus.setContent(content + " " + names.trim());
			}
			else
				setStructuredInformation(sourceCodeCorpus, parser);
			
			sourceCode.setSourceCodeCorpus(sourceCodeCorpus);
			// put to map
			synchronized(sourceCodeMap) {
				sourceCodeMap.put(sourceCode.getPath(), sourceCode);
			}
		}
		
		/** set structured information for input sourceCOdeCorpus */
		public void setStructuredInformation(SourceCodeCorpus sourceCodeCorpus, FileParser parser) {
			String classPart = parser.getStructuredContentWithFullyIdentifier(FileParser.CLASS_PART) + " " +
					parser.getStructuredContent(FileParser.CLASS_PART);
			classPart = Stemmer.stemContent(classPart);
			String methodPart = parser.getStructuredContentWithFullyIdentifier(FileParser.METHOD_PART) + " " +
					parser.getStructuredContent(FileParser.METHOD_PART);
			methodPart = Stemmer.stemContent(methodPart);
			String variablePart = parser.getStructuredContent(FileParser.VARIABLE_PART);
			String commentPart = parser.getStructuredContent(FileParser.COMMENT_PART);
			sourceCodeCorpus.setClassPart(classPart);
			sourceCodeCorpus.setMethodPart(methodPart);
			sourceCodeCorpus.setVariablePart(variablePart);
			sourceCodeCorpus.setCommentPart(commentPart);
			sourceCodeCorpus.setContent(classPart + " " + methodPart + " " + variablePart + " " + commentPart);
		}
	}
	
	/** load source code change history from history file */
	public void loadSourceCodeChangeHistory(String historyFilePath) {
		changeHistory = new HashMap<String, long[]>();
		logger.info("Load source code files' change history..."); 
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(historyFilePath), "UTF-8"));
			String line;
			while ((line = reader.readLine()) != null) {
				int firstComma = line.indexOf(",");
				String path = line.substring(0, firstComma);
				String[] times = line.substring(firstComma + 1).split(",");
				long[] values = new long[times.length];
				for (int i = 0; i < times.length; i++) {
					values[i] = Long.parseLong(times[i].trim());
				}
				changeHistory.put(path, values);
				SourceCode sourceCode = sourceCodeMap.get(path);
				if (sourceCode != null) 
					sourceCode.setChangeHistory(values);
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setSourceCodeChangeHistory(List<String> newSourceCodeList) {
		for (String filePath : newSourceCodeList) {
			String path = filePath.substring(sourceCodeDirNameLength + 1).replaceAll("\\\\", "/");
			SourceCode sourceCode = sourceCodeMap.get(path);
			sourceCode.setChangeHistory(changeHistory.get(path));
		}
	}
	
	/** compute length score for source code file */
	public void computeLengthScore() {
		logger.info("Calculating source code file length score...");
		HashMap<String, Integer> corpusLensTable = new HashMap<String, Integer>();
		int max = Integer.MIN_VALUE;
		int count = 0, sum = 0;
		for (Entry<String, SourceCode> entry : sourceCodeMap.entrySet()) {
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
			sourceCodeMap.get(filePath).setLengthScore(score);
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
	
	/** save source code repository as xml to output path  */
	public void saveSourceCodeRepoToXML(String output, String product) {
		logger.info("Saving source code repository as xml to " + output + "...");
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder domBuilder = domFactory.newDocumentBuilder();
			Document doc = domBuilder.newDocument();
			
			Element root = doc.createElement("SourceCodeRepository");
			root.setAttribute("product", product);
			doc.appendChild(root);
			
			// iterate source code
			for (SourceCode code : sourceCodeMap.values()) {
				Element codeElement = doc.createElement("SourceCode");
				
				Element pathElement = doc.createElement("Path");
				pathElement.appendChild(doc.createTextNode(code.getPath()));
				codeElement.appendChild(pathElement);
				
				Element classNameElement = doc.createElement("FullClassName");
				classNameElement.appendChild(doc.createTextNode(code.getFullClassName()));
				codeElement.appendChild(classNameElement);
				
				Element contentFieldElement = doc.createElement("Content");
				contentFieldElement.appendChild(doc.createTextNode(code.getSourceCodeCorpus().getContent()));
				codeElement.appendChild(contentFieldElement);
				
				if (Property.USE_STRUCTURED_INFORMATION) {
					Element classFieldElement = doc.createElement("ClassPart");
					classFieldElement.appendChild(doc.createTextNode(code.getSourceCodeCorpus().getClassPart()));
					codeElement.appendChild(classFieldElement);
					
					Element variableFieldElement = doc.createElement("VariablePart");
					variableFieldElement.appendChild(doc.createTextNode(code.getSourceCodeCorpus().getVariablePart()));
					codeElement.appendChild(variableFieldElement);
					
					Element methodFieldElement = doc.createElement("MethodPart");
					methodFieldElement.appendChild(doc.createTextNode(code.getSourceCodeCorpus().getMethodPart()));
					codeElement.appendChild(methodFieldElement);
					
					Element commentElement = doc.createElement("CommentPart");
					commentElement.appendChild(doc.createTextNode(code.getSourceCodeCorpus().getCommentPart()));
					codeElement.appendChild(commentElement);
				}
				
				Element methodsElement = doc.createElement("Methods");
				codeElement.appendChild(methodsElement);
				for (Method method : code.getMethodList()) {
					Element methodElement = doc.createElement("Method");
					
					Element nameElement = doc.createElement("Name");
					nameElement.appendChild(doc.createTextNode(method.getName()));
					methodElement.appendChild(nameElement);
					
					Element returnTypeElement = doc.createElement("ReturnType");
					returnTypeElement.appendChild(doc.createTextNode(method.getReturnType()));
					methodElement.appendChild(returnTypeElement);
					
					Element paramsElement = doc.createElement("Parameters");
					paramsElement.appendChild(doc.createTextNode(method.getParams()));
					methodElement.appendChild(paramsElement);
					
					Element methodContentElement = doc.createElement("MethodContent");
					methodContentElement.appendChild(doc.createTextNode(method.getContent()));
					methodElement.appendChild(methodContentElement);
					
					// add method element to code element
					methodsElement.appendChild(methodElement);
				}
				root.appendChild(codeElement);
			}
			doc.setXmlStandalone(true);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(output));
			transformer.transform(source, result);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	/** read sourceCode repository by parsing xml */
	public HashMap<String, SourceCode> parseXMLOfSourceCodeRepo(String xmlPath) {
		logger.info("Loading source code repository by parsing xml...");
		HashMap<String, SourceCode> sourceCodeMap = new HashMap<String, SourceCode>();
		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder domBuilder = domFactory.newDocumentBuilder();
			InputStream is = new FileInputStream(xmlPath);
			Document doc = domBuilder.parse(is);
			Element root = doc.getDocumentElement();
			NodeList sourceCodeNodeList = root.getChildNodes();
			if (sourceCodeNodeList != null) {
				for (int i = 0; i < sourceCodeNodeList.getLength(); i++) {
					Node codeNode = sourceCodeNodeList.item(i);
					if (codeNode.getNodeName().equals("SourceCode")) {
						SourceCode sourceCode = new SourceCode();
						List<Method> methodList = new ArrayList<Method>();
						for (Node childNode = codeNode.getFirstChild(); childNode != null; childNode = childNode
								.getNextSibling()) {
							if (childNode.getNodeName().equals("Path"))
								sourceCode.setPath(childNode.getTextContent());
							else if (childNode.getNodeName().equals("FullClassName"))
								sourceCode.setFullClassName(childNode.getTextContent());
							else if (childNode.getNodeName().equals("Content"))
								sourceCode.setSourceCodeCorpus(new SourceCodeCorpus(childNode.getTextContent()));
							else if (childNode.getNodeName().equals("ClassPart"))
								sourceCode.getSourceCodeCorpus().setClassPart(childNode.getTextContent());
							else if (childNode.getNodeName().equals("VariablePart"))
								sourceCode.getSourceCodeCorpus().setVariablePart(childNode.getTextContent());
							else if (childNode.getNodeName().equals("MethodPart"))
								sourceCode.getSourceCodeCorpus().setMethodPart(childNode.getTextContent());
							else if (childNode.getNodeName().equals("CommentPart"))
								sourceCode.getSourceCodeCorpus().setCommentPart(childNode.getTextContent());
							else if (childNode.getNodeName().equals("Methods")) {
								NodeList methodNodeList = childNode.getChildNodes();
								for (int j = 0; j < methodNodeList.getLength(); j++) {
									Node methodNode = methodNodeList.item(j);
									if (methodNode.getNodeName().equals("Method")) {
										String methodName = "", returnType = "", params = "", methodContent = "";
										for (Node methodChildNode = methodNode.getFirstChild(); methodChildNode != null;
												methodChildNode = methodChildNode.getNextSibling()) {
											if (methodChildNode.getNodeName().equals("Name"))
												methodName = methodChildNode.getTextContent();
											else if (methodChildNode.getNodeName().equals("ReturnType"))
												returnType = methodChildNode.getTextContent();
											else if (methodChildNode.getNodeName().equals("Parameters"))
												params = methodChildNode.getTextContent();
											else if (methodChildNode.getNodeName().equals("MethodContent"))
												methodContent = methodChildNode.getTextContent();
										}
										Method method = new Method(methodName, returnType, params);
										method.setContent(methodContent);
										methodList.add(method);
									}
								}
							}
							sourceCode.setMethodList(methodList);
						}
						sourceCodeMap.put(sourceCode.getPath(), sourceCode);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		logger.info("Finished parsing, total " + sourceCodeMap.size() + " java files.");
		return sourceCodeMap;
	}
	
	/** attach related bug reports info to source code */
	public void attachRelatedBugReports(HashMap<Integer, BugReport> bugReportMap) {
		relatedBugReports = new HashMap<String, List<BugReport>>();
		for (BugReport bugReport : bugReportMap.values()) {
			for (String fixedFile : bugReport.getFixedFiles()) {
				if (!relatedBugReports.containsKey(fixedFile)) {
					List<BugReport> relatedBugReportsList = new ArrayList<BugReport>();
					relatedBugReports.put(fixedFile, relatedBugReportsList);
				}
				relatedBugReports.get(fixedFile).add(bugReport);
				sourceCodeMap.get(fixedFile).getRelatedBugReportList().add(bugReport);
			}
		}
	}
	
	public void attachRelatedBugReports(List<String> newSourceCodeList) {
		if (relatedBugReports == null)
			return;
		for (String filePath : newSourceCodeList) {
			String path = filePath.substring(sourceCodeDirNameLength + 1).replaceAll("\\\\", "/");
			SourceCode sourceCode = sourceCodeMap.get(path);
			sourceCode.setRelatedBugReportList(relatedBugReports.get(path));
		}
	}
	
	public void attachRelatedBugReportsForNewSourceCode(HashMap<Integer, BugReport> bugReportMap) {
		
	}
	
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public HashMap<String, SourceCode> getSourceCodeMap() {
		return sourceCodeMap;
	}

	public void setSourceCodeMaps(HashMap<String, SourceCode> sourceCodeMaps) {
		this.sourceCodeMap = sourceCodeMaps;
	}

	public HashMap<String, SourceCode> getAddedFiles() {
		return addedFiles;
	}

	public HashMap<String, SourceCode> getModifiedFiles() {
		return modifiedFiles;
	}

	public HashMap<String, SourceCode> getDeletedFiles() {
		return deletedFiles;
	}

	
}
