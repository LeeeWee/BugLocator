package edu.whu.pllab.buglocator.tests;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Map.Entry;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.Method;
import edu.whu.pllab.buglocator.common.SourceCode;
import edu.whu.pllab.buglocator.common.SourceCodeRepository;

public class SourceCodeReositoryTest {

	public static void main(String[] args) throws Exception {
		String output = "D:\\data\\sourceCodeRepositoryTest.txt";
		
		@SuppressWarnings("unused")
		Property property = Property.loadInstance();
		SourceCodeRepository repo = new SourceCodeRepository();
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(output));
		int count = 0;
		for (Entry<String, SourceCode> entry : repo.getSourceCodeMaps().entrySet()) {
			count++;
			if (count == 100) 
				break;
			StringBuilder builder = new StringBuilder();
			SourceCode sourceCode = entry.getValue();
			builder.append("Path: " + entry.getKey() + "\n");
			builder.append("FullClassName: " + sourceCode.getFullClassName() + "\n");
			builder.append("Content: " + sourceCode.getSourceCodeCorpus().getContent() + "\n");
			builder.append("LengthScore: " + sourceCode.getLengthScore() + "\n");
			builder.append("Methods: " + "\n");
			for (Method method : sourceCode.getMethodList()) {
				builder.append("\tMethodName: " + method.getName() + "\tParams: " + 
						method.getParams() + "\tReturnType: " + method.getReturnType() + "\n");
				builder.append("\t\tMethodContent: " + method.getContent() + "\n");
			}
			writer.write(builder.toString());
			writer.write("\n");
		}
		writer.close();
	}
	
}
