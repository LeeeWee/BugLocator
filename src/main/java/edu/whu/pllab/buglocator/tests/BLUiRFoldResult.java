package edu.whu.pllab.buglocator.tests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.evaluation.ExperimentResult;
import edu.whu.pllab.buglocator.evaluation.SimpleEvaluator;

public class BLUiRFoldResult {
	
	public static void main(String[] args) throws Exception {
		String[] products = {"ASPECTJ", "SWT", "BIRT", "ECLIPSE_PLATFORM_UI", "TOMCAT", "JDT"};
		for (String product : products) {
			Property property = Property.loadInstance(product);
			String directory = new File(property.getWorkingDir(), "data_folder").getAbsolutePath();
			bluirPredict(directory, new File(property.getWorkingDir(), "BLUiR_Result.txt").getAbsolutePath());
		}
	}
	
	public static void bluirPredict(String directory, String output) throws Exception {
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(output));
		
		List<ExperimentResult> experimentResultList = new ArrayList<ExperimentResult>();
		
		String[] foldsName = new File(directory).list();
		String[] foldsPath = new String[foldsName.length];
		for (int i = 0; i < foldsName.length; i++) {
			foldsPath[i] = new File(directory, foldsName[i]).getAbsolutePath();
		}
		// test fold#i
		for (int i = 0; i < foldsPath.length; i++) {
			File foldFile = new File(foldsPath[i]);
			if (!foldFile.isDirectory())
				continue;
			String testDataPath = new File(foldFile, "test.dat").getAbsolutePath();
			String predictionsPath = new File(foldFile, "BLUiR.predictions").getAbsolutePath();
			BufferedWriter predictionsWriter = new BufferedWriter(new FileWriter(predictionsPath));
			BufferedReader reader = new BufferedReader(new FileReader(testDataPath));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(" ");
				double sum = 0;
				for (int j = 2; j < 10; j++) {
					sum += Double.parseDouble(parts[j].split(":")[1]);
				}
				predictionsWriter.write(sum + "\n");
			}
			reader.close();
			predictionsWriter.close();
			
			// evaluate 
			SimpleEvaluator evaluator = new SimpleEvaluator(testDataPath, predictionsPath);
			evaluator.evaluate();
			experimentResultList.add(evaluator.getExperimentResult());
			
			writer.write(String.format("test on %d-th fold:", i) + "\n");
			writer.write(evaluator.getExperimentResult().toString() + "\n\n");
			writer.flush();
		}
			
		ExperimentResult finalResult = ExperimentResult.pollExperimentResult(experimentResultList);
		
		StringBuilder builder = new StringBuilder();
		builder.append("\n");
		builder.append("===================== Final Experiment Result =========================\n");
		builder.append(finalResult.toString() + "\n");
		builder.append("=======================================================================");
		System.out.println(builder.toString());
		writer.write(builder.toString());
		
		writer.close();
	}
	
}
