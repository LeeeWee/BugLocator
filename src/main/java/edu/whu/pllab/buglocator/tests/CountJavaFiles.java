package edu.whu.pllab.buglocator.tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import edu.whu.pllab.buglocator.utils.FileUtil;

public class CountJavaFiles {

	public static void main(String[] args) {
		String openSourceDir = "D:\\data\\open sources";
		File[] products = new File(openSourceDir).listFiles();
		for (File product : products) {
			System.out.println(product.getName() + ":");
			List<String> javaFile = FileUtil.getAllFiles(product.getAbsolutePath(), ".java");
			System.out.print("\tTotal java files: " + javaFile.size() + ", ");
			int[] counts = countAllFilesLines(javaFile);
			System.out.print("Total lines: " + counts[0] + ", single file maximum lines: " + counts[1]);
			System.out.println("\n");
		}
	}
	
	public static int countLines(File file) {
		int count = 0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			while (reader.readLine() != null) 
				count++;
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return count;
	}
	
	public static int[] countAllFilesLines(List<String> files) {
		int total = 0;
		int max = 0;
		for (String file : files) {
			int count = countLines(new File(file));
			if (count > max) 
				max = count;
			total += count;
		}
		int[] ret = new int[] {total, max};
		return ret;
	}
}
