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

public class FileParser {

	private CompilationUnit compilationUnit;
	private String sourceString;
	
	private String allClassNames;
	private String allMethodNames;
	private String allInnerMethodNames;
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
		allInnerMethodNames = null;
		allVariableNames = null;
		allComments = null;
		allMethodList = null;
		
		compilationUnit = null;
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
	
	public static String[] splitContent(String content) {
		String tokensInConent[] = Splitter.splitSourceCode(content);
		StringBuffer sourceCodeContentBuffer = new StringBuffer();
		for (int i = 0; i < tokensInConent.length; i++) {
			String token = tokensInConent[i];
			sourceCodeContentBuffer.append((new StringBuilder(String.valueOf(token))).append(" ").toString());
		}

		String processedContent = sourceCodeContentBuffer.toString().toLowerCase();
		return processedContent.split(" ");
	}
	
	public String[] getStructuredContentWithFullyIdentifier(int type) {
		String content = "";
		
		switch (type) {
		case CLASS_PART:
			content =  getAllClassNames();
			break;
		case METHOD_PART:
			content =  getAllMethodNames();
			if (getAllMethodNames().length() != 0) {
				content += " ";
			}
			content += getAllInnerMethodNames();
			break;
		case VARIABLE_PART:
			content =  getAllVariableNames();
			break;
		case COMMENT_PART:
			content =  getAllComments();
			break;
		}
		
		content = content.toLowerCase();
		return content.split(" ");
	}
	
	
	public String[] getStructuredContent(int type) {
		String content = "";
		
		switch (type) {
		case CLASS_PART:
			content =  getAllClassNames();
			break;
		case METHOD_PART:
			content =  getAllMethodNames();
			if (getAllMethodNames().length() != 0) {
				content += " ";
			}
			content += getAllInnerMethodNames();
			break;
		case VARIABLE_PART:
			content =  getAllVariableNames();
			break;
		case COMMENT_PART:
			content =  getAllComments();
			break;
		}
		
		return splitContent(content);
	}
	
	public String[] getContent() {
		String tokensInSourceCode[] = Splitter.splitSourceCode(deleteNoNeededNode());
		StringBuffer sourceCodeContentBuffer = new StringBuffer();
		for (int i = 0; i < tokensInSourceCode.length; i++) {
			String token = tokensInSourceCode[i];
			sourceCodeContentBuffer.append((new StringBuilder(String.valueOf(token))).append(" ").toString());
		}

		String content = sourceCodeContentBuffer.toString().toLowerCase();
		return content.split(" ");
	}
	
	public String[] getClassNameAndMethodName() {
		String content = (new StringBuilder(String.valueOf(getAllClassNames()))).append(" ")
				.append(getAllMethodNames()).append(" ").toString().toLowerCase();
		return content.split(" ");
	}

	public String getPackageName() {
		return compilationUnit.getPackage() != null ?
				compilationUnit.getPackage().getName().getFullyQualifiedName() : "";
	}
	
	private String getAllInnerMethodNames() {
		if (allInnerMethodNames != null) {
			return allInnerMethodNames;
		} else {
	    	final ArrayList<String> innerMethodNameList = new ArrayList<String>();
	    	
	    	compilationUnit.accept(new ASTVisitor() {
	    		public boolean visit(TypeDeclaration type) {
	    		    if (!type.isPackageMemberTypeDeclaration()) {
    		            
    					MethodDeclaration methodDecls[] = type.getMethods();
    					MethodDeclaration methodDeclaration[];
    					int k = (methodDeclaration = methodDecls).length;
    					for (int j = 0; j < k; j++) {
    						MethodDeclaration methodDecl = methodDeclaration[j];
    						String methodName = methodDecl.getName().getFullyQualifiedName();
    						
    						Type returnType = methodDecl.getReturnType2();
    						String returnTypeString = (returnType == null) ? "" : returnType.toString();
    						
    						String parameters = "";
    						for (int l = 0; l < methodDecl.parameters().size(); l++) {
    							parameters += ((SingleVariableDeclaration) methodDecl.parameters().get(l)).getType().toString();
    							parameters += " ";
    						}
    						parameters = parameters.trim();
    						
    						Method method = new Method(methodName, returnTypeString, parameters);
    						allMethodList.add(method);
    						
    						innerMethodNameList.add(methodName);
    					}
	    		    }
	    		    return super.visit(type);
	    		}
	    	});
	    	
	    	allInnerMethodNames = "";
			for (Iterator<String> iterator = innerMethodNameList.iterator(); iterator.hasNext();) {
				String structuredInfoName = (String) iterator.next();
				allInnerMethodNames = (new StringBuilder(String.valueOf(allInnerMethodNames)))
						.append(structuredInfoName).append(" ").toString();
			}
		
			allInnerMethodNames = allInnerMethodNames.trim(); 
			return allInnerMethodNames;	
		}
	}
	
	public String getAllVariableNames() {
		if (allVariableNames != null) {
			return allVariableNames;
		} else {
	    	final ArrayList<String> structuredInfoList = new ArrayList<String>();
	    	
	    	compilationUnit.accept(new ASTVisitor() {
	            public boolean visit(SingleVariableDeclaration node)
	            {
//	            	System.out.printf("single variable Node: %s\n", node.getName().getIdentifier());
	            	structuredInfoList.add(node.getName().getIdentifier());
	                return super.visit(node);
	            }
	    	});
	    	
	    	compilationUnit.accept(new ASTVisitor() {
	            public boolean visit(VariableDeclarationFragment node)
	            {
//	            	System.out.printf("variable declaration Node: %s\n", node.getName().getIdentifier());
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
	
	private String replaceHtmlSpecicalCharacters(String line) {
		line = line.replace("&quot;", "\"");
		line = line.replace("&amp;", "&");
		line = line.replace("&lt;", "<");
		line = line.replace("&gt;", ">");
		line = line.replace("&nbsp;", " ");
		
		return line;
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
	
	
	public ArrayList<Method> getAllMethodList() {
		if (allMethodList != null) {
			return allMethodList;
		} else {
			getAllMethodNames();
			getAllInnerMethodNames();
			return allMethodList;
		}
	}

	private String getAllMethodNames() {
		if (allMethodNames != null) {
			return allMethodNames;
		} else {
			ArrayList<String> methodNameList = new ArrayList<String>();
			allMethodList = new ArrayList<Method>();
			for (int i = 0; i < compilationUnit.types().size(); i++) {
				if (compilationUnit.types().get(i) instanceof TypeDeclaration) {
					TypeDeclaration type = (TypeDeclaration) compilationUnit.types().get(i);
					MethodDeclaration methodDecls[] = type.getMethods();
					MethodDeclaration methodDeclaration[];
					int k = (methodDeclaration = methodDecls).length;
					for (int j = 0; j < k; j++) {
						MethodDeclaration methodDecl = methodDeclaration[j];
						String methodName = methodDecl.getName().getFullyQualifiedName();
						
						Type returnType = methodDecl.getReturnType2();
						String returnTypeString = (returnType == null) ? "" : returnType.toString();
						
						String parameters = "";
						for (int l = 0; l < methodDecl.parameters().size(); l++) {
							parameters += ((SingleVariableDeclaration) methodDecl.parameters().get(l)).getType().toString();
							parameters += " ";
						}
						parameters = parameters.trim();
						
						// debug code
						Method method = new Method(methodName, returnTypeString, parameters);
						allMethodList.add(method);
						
						methodNameList.add(methodName);
					}
				}
			}

			allMethodNames = "";
			for (Iterator<String> iterator = methodNameList.iterator(); iterator.hasNext();) {
				String methodName = (String) iterator.next();
				allMethodNames = (new StringBuilder(String.valueOf(allMethodNames)))
						.append(methodName).append(" ").toString();
			}

			allMethodNames = allMethodNames.trim();
			return allMethodNames;
		}
	}
	
	private String getAllClassNames() {
		if (allClassNames != null) {
			return allClassNames;
		} else {
			ArrayList<String> classNameList = new ArrayList<String>();
			
			for (int i = 0; i < compilationUnit.types().size(); i++) {
				if (compilationUnit.types().get(i) instanceof TypeDeclaration) {
					TypeDeclaration type = (TypeDeclaration) compilationUnit.types().get(i);
					String name = type.getName().getFullyQualifiedName();
					classNameList.add(name);
				}
			}

			allClassNames = "";
			for (Iterator<String> iterator = classNameList.iterator(); iterator.hasNext();) {
				String className = (String) iterator.next();
				allClassNames = (new StringBuilder(String.valueOf(allClassNames))).append(className).append(" ").toString();
			}
			
			allClassNames = allClassNames.trim();
			return allClassNames;
		}
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
	
    public ArrayList<String> getImportedClasses()
    {
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
	
}
