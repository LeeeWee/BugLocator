package edu.whu.pllab.buglocator.utils;

import java.util.List;

public class FileUtilTest {

	public static void main(String[] args) {
		String direcotry = "D:\\data\\sources\\eclipse.platform.ui";
		List<String> javaFiles = FileUtil.getAllFiles(direcotry, ".java");
		System.out.println("Length: " + javaFiles.size());
	}
	
}
