package edu.whu.pllab.buglocator.tests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class RenameBLUiRDocName {
	
	public static void main(String[] args) throws IOException {
		String directory = "D:\\data\\buglocation\\BugLocatorData\\work\\BLUiR-Bench4BL\\BLUiR_AspectJ\\docs";
		String outputDir = "D:\\data\\buglocation\\BugLocatorData\\work\\BLUiR-Bench4BL\\BLUiR_AspectJ\\new_docs";
		rename(directory, outputDir);
	}
	
	public static void rename(String directory, String outputDir) throws IOException {
		File dir = new File(directory);
		File[] docs = dir.listFiles();
		for (File doc : docs) {
			BufferedReader reader = new BufferedReader(new FileReader(doc));
			reader.readLine();
			String pathLine = reader.readLine();
			String path = pathLine.substring(7, pathLine.length()-9).replaceAll("/", ".");
			reader.close();
			reader = new BufferedReader(new FileReader(doc));
			File output = new File(outputDir, path);
			BufferedWriter writer = new BufferedWriter(new FileWriter(output));
			String line;
			while ((line = reader.readLine()) != null) 
				writer.write(line + "\n");
			reader.close();
			writer.close();
		}
	}

}
