package edu.whu.pllab.buglocator.astparser;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import edu.whu.pllab.buglocator.common.Method;
import edu.whu.pllab.buglocator.utils.Splitter;
import edu.whu.pllab.buglocator.utils.Stemmer;
import edu.whu.pllab.buglocator.utils.Stopword;

public class FileParser {

	private CompilationUnit compilationUnit;
	private String sourceString;
	
	private String allClassNames;
	private String allMethodNames;
	private String allVariableNames;
	private String allComments;
	private ArrayList<Method> allMethodList;
	
	public static final int CLASS_PART = 1;
	public static final int METHOD_PART = 2;
	public static final int VARIABLE_PART = 3;
	public static final int COMMENT_PART = 4;
	
	public FileParser(File file) {
		allClassNames = null;
		allMethodNames = null;
		allVariableNames = null;
		allComments = null;
		allMethodList = null;
		
		ASTCreator creator = new ASTCreator(file);
		compilationUnit = creator.getCompilationUnit();
		sourceString = creator.getContent();
	}

	public int getLinesOfCode() {
		deleteNoNeededNode();
		String lines[] = compilationUnit.toString().split("\n");
		int len = 0;
		String as[];
		int j = (as = lines).length;
		for (int i = 0; i < j; i++) {
			String strLine = as[i];
			if (!strLine.trim().equals(""))
				len++;
		}
		return len;
	}
	
	public String getStructuredContentWithFullyIdentifier(int type) {
		String content = "";
		switch (type) {
		case CLASS_PART:
			content =  getAllClassNames();
			break;
		case METHOD_PART:
			content =  getAllMethodNames();
			break;
		case VARIABLE_PART:
			content =  getAllVariableNames();
			break;
		case COMMENT_PART:
			content =  getAllComments();
			break;
		}
		return content;
	}
	
	public String getStructuredContent(int type) {
		return splitContent(getStructuredContentWithFullyIdentifier(type));
	}
	
	/**
	 * split and stem source code file content, remove stopwords
	 */
	public static String splitContent(String content) {
		String tokensInContent[] = Splitter.splitSourceCode(content);
		StringBuffer sourceCodeContentBuffer = new StringBuffer();
		for (int i = 0; i < tokensInContent.length; i++) {
			String token = Stemmer.stem(tokensInContent[i]);
			if (Stopword.isEnglishStopword(token) || Stopword.isJavaKeyword(token) || Stopword.isProjectKeyword(token))
				continue;
			sourceCodeContentBuffer.append((new StringBuilder(String.valueOf(token))).append(" ").toString());
		}
		String processedContent = sourceCodeContentBuffer.toString().toLowerCase();
		return processedContent;
	}
	
	/**
	 * get source code file content
	 */
	public String getContent() {
		return splitContent(deleteNoNeededNode());
	}
	
	/**
	 * get package name
	 */
	public String getPackageName() {
		return compilationUnit.getPackage() != null ?
				compilationUnit.getPackage().getName().getFullyQualifiedName() : "";
	}
	
	/**
	 * get imported classes list 
	 */
    public ArrayList<String> getImportedClasses() {
    	final ArrayList<String> importedClasses = new ArrayList<String>();
    	
    	compilationUnit.accept(new ASTVisitor() {
            public boolean visit(ImportDeclaration node)
            {
            	importedClasses.add(node.getName().toString());
                return super.visit(node);
            }
    	});
    	
    	return importedClasses;
    }
	
    /**
     * get all class names
     */
	public String getAllClassNames() {
		if (allClassNames != null) {
			return allClassNames;
		} else {
			allClassNames = "";
		   	compilationUnit.accept(new ASTVisitor() {
	    		public boolean visit(TypeDeclaration type) {
	    			allClassNames += type.getName() + " ";
	    		    return super.visit(type);
	    		}
	    	});
			allClassNames = allClassNames.trim();
			return allClassNames;
		}
	}
	
	/**
	 * get all method names, and save method to method list
	 */
	public String getAllMethodNames() {
		if (allMethodNames != null) {
			return allMethodNames;
		} else {
			allMethodList = new ArrayList<Method>();
	    	compilationUnit.accept(new ASTVisitor() {
	    		public boolean visit(MethodDeclaration methodDecl) {
					String methodName = methodDecl.getName().getFullyQualifiedName();
					Type returnType = methodDecl.getReturnType2();
					String returnTypeString = (returnType == null) ? "" : returnType.toString();
					// get parameters
					String parameters = "";
					for (int l = 0; l < methodDecl.parameters().size(); l++) {
						parameters += ((SingleVariableDeclaration) methodDecl.parameters().get(l)).getType().toString();
						parameters += " ";
					}
					parameters = parameters.trim();
					Method method = new Method(methodName, returnTypeString, parameters);
					// split method content
					method.setContent(splitContent(methodDecl.toString()));
					allMethodList.add(method);
					return super.visit(methodDecl);
	    		}
	    	});
			allMethodNames = "";
			for (Iterator<Method> iterator = allMethodList.iterator(); iterator.hasNext();) {
				String methodName = (String) iterator.next().getName();
				allMethodNames = (new StringBuilder(String.valueOf(allMethodNames)))
						.append(methodName).append(" ").toString();
			}
			allMethodNames = allMethodNames.trim();
			return allMethodNames;
		}
	}
	
	/**
	 * get all method list
	 */
	public ArrayList<Method> getAllMethodList() {
		if (allMethodList != null) {
			return allMethodList;
		} else {
			getAllMethodNames();
			return allMethodList;
		}
	}
	
	/**
	 * get all variable names
	 */
	public String getAllVariableNames() {
		if (allVariableNames != null) {
			return allVariableNames;
		} else {
	    	final ArrayList<String> structuredInfoList = new ArrayList<String>();
	    	
	    	compilationUnit.accept(new ASTVisitor() {
	            public boolean visit(SingleVariableDeclaration node)
	            {
	            	structuredInfoList.add(node.getName().getIdentifier());
	                return super.visit(node);
	            }
	    	});
	    	
	    	compilationUnit.accept(new ASTVisitor() {
	            public boolean visit(VariableDeclarationFragment node)
	            {
	            	structuredInfoList.add(node.getName().getIdentifier());
	                return super.visit(node);
	            }
	    	});
	    	
	    	allVariableNames = "";
			for (Iterator<String> iterator = structuredInfoList.iterator(); iterator.hasNext();) {
				String structuredInfoName = (String) iterator.next();
				allVariableNames = (new StringBuilder(String.valueOf(allVariableNames)))
						.append(structuredInfoName).append(" ").toString();
			}
		
			allVariableNames = allVariableNames.trim(); 
			return allVariableNames;	
		}
	}

	@SuppressWarnings("unchecked")
	public String getAllComments() {
		if (allComments != null) {
			return allComments;
		} else {
			final ArrayList<String> structuredInfoList = new ArrayList<String>();
			
		   	for (Comment comment : (List<Comment>) compilationUnit.getCommentList()) {
	    		comment.accept(new ASTVisitor() {
	                public boolean visit(Javadoc node)
	                {
//	                	System.out.printf("ClassNames: %s, comment text: %s\n", getAllClassNames(), node.toString());
	                	String javadocComment = node.toString();
	                	
	                	if (!javadocComment.toLowerCase().contains("copyright")) {
	                    	javadocComment = javadocComment.split("[/][*][*]")[1];
	                    	javadocComment = javadocComment.split("[*][/]")[0];
	                    	String[]  commentLines = javadocComment.split("\n");
	                    	
	                    	for (String line : commentLines) {
	                    		if (line.contains("@author") || line.contains("@version") || line.contains("@since") ) {
	                    			continue;
	                    		}
	                    		
	                    		line = replaceHtmlSpecicalCharacters(line);
	                    		// Split line with space and html tag
	                        	String[] words = line.split("([*\\s]|(?i)\\<[^\\>]*\\>)");
	                        	for (String word : words) {
	                        		if (word.length() > 0) {
	                        			if ( (word.equalsIgnoreCase("@param")) || (word.equalsIgnoreCase("@return")) || (word.equalsIgnoreCase("@exception")) ||
	                        					(word.equalsIgnoreCase("@see")) || (word.equalsIgnoreCase("@serial")) || (word.equalsIgnoreCase("@deprecated")) )  {
	                        				continue;
	                        			}

	                        			structuredInfoList.add(word);                		
//	        	                		System.out.printf("ClassNames: %s, javadocComment text: %s\n", getAllClassNames(), word);
	                        		}
	                        	}
	                    	}
	                	}
	                    return super.visit(node);
	                }
	        	});
	        	
	    		comment.accept(new ASTVisitor() {
	                public boolean visit(LineComment node)
	                {
	                	int beginIndex = node.getStartPosition();
	                	int endIndex = beginIndex + node.getLength(); 
	                	String lineComment = sourceString.substring(beginIndex + 2, endIndex).trim();

	                	if (!lineComment.toLowerCase().contains("copyright")) {
		                	String[] words = lineComment.split("[\\s]");
		                	for (String word : words) {
		                		if (word.length() > 0) {
		//	                		System.out.printf("ClassNames: %s, BlockComment text: %s\n", getAllClassNames(), word);
		                			structuredInfoList.add(word);                		
		                		}
		                	}
	                	}
	                    return super.visit(node);
	                }
	        	});
	        	
	    		comment.accept(new ASTVisitor() {
	                public boolean visit(BlockComment node)
	                {
	                	int beginIndex = node.getStartPosition();
	                	int endIndex = beginIndex + node.getLength(); 
	                	String blockComment = sourceString.substring(beginIndex, endIndex);
	                	
//	                	if (blockComment.contains("/**/")) {
//	                		System.out.printf("original BlockComment text: %s\n", blockComment);
//	                	}
	                	
	                   	if (!blockComment.toLowerCase().contains("copyright")) {
		                	String[] splitComment = blockComment.split("[/][*]");
		                	if (splitComment.length == 2) { 
		                		blockComment = splitComment[1];
		                		
		                		splitComment = blockComment.split("[*][/]");
		                		if (splitComment.length == 1) {
		                			blockComment = splitComment[0];
		
		//                        	System.out.printf("ClassNames: %s, original BlockComment text: %s\n", getAllClassNames(), blockComment);
		                        	String[] words = blockComment.split("[*\\s]");
		                        	for (String word : words) {
		                        		if (word.length() > 0) {
		//        	                		System.out.printf("ClassNames: %s, BlockComment text: %s\n", getAllClassNames(), word);
		                        			structuredInfoList.add(word);                		
		                        		}
		                        	}
		                		}
		                	}
	                   	}
	                    return super.visit(node);
	                }
	        	});
			}
	    
			allComments = "";
			for (Iterator<String> iterator = structuredInfoList.iterator(); iterator.hasNext();) {
				String structuredInfoName = (String) iterator.next();
				allComments = (new StringBuilder(String.valueOf(allComments)))
						.append(structuredInfoName).append(" ").toString();
			}
		
			allComments = allComments.trim();
			return allComments;
		}
	}
	
	private String replaceHtmlSpecicalCharacters(String line) {
		line = line.replace("&quot;", "\"");
		line = line.replace("&amp;", "&");
		line = line.replace("&lt;", "<");
		line = line.replace("&gt;", ">");
		line = line.replace("&nbsp;", " ");
		
		return line;
	}


	/**
	 * delete PackageMemberType, Package and Import declaration
	 * @return
	 */
	private String deleteNoNeededNode() {
		compilationUnit.accept(new ASTVisitor() {
			public boolean visit(AnnotationTypeDeclaration node) {
				if (node.isPackageMemberTypeDeclaration())
					node.delete();
				return super.visit(node);
			}
		});
		
		compilationUnit.accept(new ASTVisitor() {
			public boolean visit(PackageDeclaration node) {
				node.delete();
				return super.visit(node);
			}
		});
		
		compilationUnit.accept(new ASTVisitor() {
			public boolean visit(ImportDeclaration node) {
				node.delete();
				return super.visit(node);
			}
		});
		
		return compilationUnit.toString();
	}
	
}
