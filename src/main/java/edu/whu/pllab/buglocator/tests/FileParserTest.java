package edu.whu.pllab.buglocator.tests;

import java.io.File;
import java.util.ArrayList;

import edu.whu.pllab.buglocator.astparser.FileParser;
import edu.whu.pllab.buglocator.common.Method;

public class FileParserTest {
	
	public static void main(String[] args) {
		String filePath = "D:\\data\\AstView.java";
		FileParser parser = new FileParser(new File(filePath));
		
		System.out.println("PackageName: " + parser.getPackageName());
		System.out.println("ImportedClassNames: " + parser.getImportedClasses());
		System.out.println("AllClassNames: " + parser.getAllClassNames());
		System.out.println("AllMethodsNames: " + parser.getAllMethodNames());
		System.out.println("AllVariableNames: " + parser.getAllVariableNames());
		System.out.println("AllComments: " + parser.getAllComments());
		ArrayList<Method> methodList = parser.getAllMethodList();
		System.out.println("Method list:");
		for (Method method : methodList) {
			System.out.println("MethodName: " + method.getName() + ", Parameters: " + method.getParams() + ", ReturnType: " + method.getReturnType());
			System.out.println("Contents: " + method.getContent());
		}
	}
	
}
