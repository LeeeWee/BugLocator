package edu.whu.pllab.buglocator.astparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class ASTCreator {
	/**
	 * Code file content
	 */
	private String content;

	public ASTCreator(File file) {
		content = getFileContent(file);
	}
	
	/**
	 * get file content for given java file
	 * @param input file
	 * @return file content
	 */
	public String getFileContent(File file) {
		String content = "";
		try {
			StringBuffer contentBuffer = new StringBuffer();
			String line = null;
			BufferedReader reader = new BufferedReader(new FileReader(file));
			while ((line = reader.readLine()) != null)
				contentBuffer.append((new StringBuilder(String.valueOf(line)))
						.append("\r\n").toString());
			content = contentBuffer.toString();
			reader.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return content;
	}

	/**
	 * get compilation unit
	 * @param content input code content
	 * @return
	 */
	public CompilationUnit getCompilationUnit() {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(content.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);
		return compilationUnit;
	}
	
	/**
	 * @return the content
	 */
	public String getContent() {
		return content;
	}
	
}
