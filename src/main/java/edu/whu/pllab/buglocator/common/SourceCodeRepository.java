package edu.whu.pllab.buglocator.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.astparser.FileParser;
import edu.whu.pllab.buglocator.utils.FileUtil;

public class SourceCodeRepository {

	private static final Logger logger = LoggerFactory.getLogger(BugReportRepository.class);

	/** source code repository commit version */
	private String version;
	
	private HashMap<String, SourceCode> sourceCodeMap;
	
	/** use file's absolutePath substring(sourceCodeDirNameLength+1) as file path*/
	private int sourceCodeDirNameLength;
	
	/** Constructor */
	public SourceCodeRepository() {
		Property property = Property.getInstance();
		String sourceCodeDir = property.getSourceCodeDir();
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
			saveSourceCodeRepoToXML(property.getCodeRepositoryXMLPath(), property.getProduct());
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
		sourceCodeMap = new HashMap<String, SourceCode>();
		loadSourceCodeFiles(sourceCodeDir);
		saveSourceCodeRepoToXML(property.getCodeRepositoryXMLPath(), property.getProduct());
		loadSourceCodeChangeHistory(property.getCodeChangeHistoryPath());
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
			SourceCodeCorpus sourceCodeCorpus = new SourceCodeCorpus(parser.getContent());
			sourceCode.setSourceCodeCorpus(sourceCodeCorpus);
			// put to map
			synchronized(sourceCodeMap) {
				sourceCodeMap.put(sourceCode.getPath(), sourceCode);
			}
		}
	}
	
	/** load source code change history from history file */
	public void loadSourceCodeChangeHistory(String historyFilePath) {
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
					SourceCode sourceCode = sourceCodeMap.get(path);
					if (sourceCode != null) 
						sourceCode.setChangeHistory(values);
				}
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
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
				
				Element contentElement = doc.createElement("Content");
				contentElement.appendChild(doc.createTextNode(code.getSourceCodeCorpus().getContent()));
				codeElement.appendChild(contentElement);
				
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
		return sourceCodeMap;
	}

	public void setSourceCodeMaps(HashMap<String, SourceCode> sourceCodeMaps) {
		this.sourceCodeMap = sourceCodeMaps;
	}

	
}
