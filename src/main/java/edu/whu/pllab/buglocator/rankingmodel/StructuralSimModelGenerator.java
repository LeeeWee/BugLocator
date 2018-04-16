package edu.whu.pllab.buglocator.rankingmodel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.SourceCode;
import edu.whu.pllab.buglocator.similarity.Similarity;

public class StructuralSimModelGenerator {
	
	private static Logger logger = LoggerFactory.getLogger(StructuralSimModelGenerator.class);
	
	public static final double CANDIDATE_SOURCE_CODE = 300;
	
	public static final int BR_BR_SIMILARITY = Similarity.VSM;
	public static final int BR_CODE_SIMILARITY = Similarity.VSM;
	
	private HashMap<Integer, BugReport> trainingBugReportsMap;
	private HashMap<Integer, BugReport> testBugReportsMap;
	private HashMap<String, SourceCode> sourceCodeMap;
	
	private double[] maxFieldSimilarities = new double[8];
	private double[] minFieldSimilarities = new double[8];
	
	private HashMap<BugReport, List<IntegratedScore>> finals;
	
	public StructuralSimModelGenerator() {
		for (int i = 0; i < maxFieldSimilarities.length; i++)
			maxFieldSimilarities[i] = Double.MIN_VALUE;
		for (int i = 0; i < minFieldSimilarities.length; i++)
			minFieldSimilarities[i] = Double.MIN_VALUE;
		finals = new HashMap<BugReport, List<IntegratedScore>>();
	}
	
	/** save features max min value to given file */
	public void saveParameters(File file) {
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			writer.write("maxFieldSimilarities: ");
			for (int i = 0; i < maxFieldSimilarities.length; i++)
				writer.write((i + 1) + ":" + maxFieldSimilarities[i] + " ");
			writer.write("\n");
			writer.write("minFieldSimilarities: ");
			for (int i = 0; i < minFieldSimilarities.length; i++)
				writer.write((i + 1) + ":" + minFieldSimilarities[i] + " ");
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** load features max min value from input file */
	public void loadParameters(File file) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("maxFieldSimilarities")) {
					String[] values = line.split(" ");
					for (int i = 1; i < values.length; i++) 
						maxFieldSimilarities[i - 1] = Integer.parseInt(values[i].split(":")[1]);
				}
				if (line.startsWith("minFieldSimilarities")) {
					String[] values = line.split(" ");
					for (int i = 1; i < values.length; i++) 
						minFieldSimilarities[i - 1] = Integer.parseInt(values[i].split(":")[1]);
				}
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
