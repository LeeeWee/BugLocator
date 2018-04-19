package edu.whu.pllab.buglocator.tests;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class SimpleEvaluate {
	
	public class Feature {
		public boolean isPositive;
		public int qid;
		public double[] features;
		public double score;
		
		public Feature(boolean isPositive, int qid, double[] features) {
			this.isPositive = isPositive;
			this.qid = qid;
			this.features = features;
		}
	}
	
	private double[] weights;
	
	public SimpleEvaluate(String svmModelPath) {
		// load train model weights
		weights = new double[8];
		BufferedReader modelReader;
		try {
			modelReader = new BufferedReader(new FileReader(svmModelPath));
			String lastLine = "", line = "";
			while ((line = modelReader.readLine()) != null) {
				lastLine = line;
			}
			modelReader.close();
			String[] splits = lastLine.split(" ");
			for (int i = 1; i < splits.length - 1; i++) {
				String[] parts = splits[i].split(":");
				int index = Integer.parseInt(parts[0]);
				double weight = Double.parseDouble(parts[1]);
				weights[index - 1] = weight;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String evaluate(String featuresPath) throws Exception {
		return evaluate(featuresPath, weights);
	}
	
	public String directAddingEvaluate(String featuresPath) throws Exception {
		double[] weights = new double[] {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
		return evaluate(featuresPath, weights);
	}
	
	public String evaluate(String featuresPath, double[] weights) throws Exception {
		// load training data features
		HashMap<Integer, ArrayList<Feature>> featuresMap = new HashMap<Integer, ArrayList<Feature>>();
		BufferedReader featuresReader = new BufferedReader(new FileReader(featuresPath));
		String line = "";
		while ((line = featuresReader.readLine()) != null) {
			String[] parts = line.split(" ");
			Integer rank = Integer.parseInt(parts[0]);
			boolean isPositive = (rank > 1) ? true : false;
			int qid = Integer.parseInt(parts[1].split(":")[1]);
			double[] values = new double[8];
			for (int i = 2; i < 10; i++) {
				values[i - 2] = Double.parseDouble(parts[i].split(":")[1]);
			}
			Feature feature = new Feature(isPositive, qid, values);
			if (!featuresMap.containsKey(qid)) {
				ArrayList<Feature> featureList = new ArrayList<Feature>();
				featuresMap.put(qid, featureList);
			}
			featuresMap.get(qid).add(feature);
		}
		featuresReader.close();
		
		// calcuate score for each feature
		for (ArrayList<Feature> featureList : featuresMap.values()) {
			for (Feature feature : featureList) {
				double score = 0.0;
				for (int i = 0; i < weights.length; i++) {
					score += weights[i] * feature.features[i];
				}
				feature.score = score;
			}
			// sort featureList
			featureList.sort(new Comparator<Feature>() {
				@Override
				public int compare(Feature feature0, Feature feature1) {
					if (feature0.score > feature1.score)
						return -1;
					else if (feature0.score < feature1.score)
						return 1;
					else 
						return 0;
				}
			});
		}
		
		// evaluate on training data
		int total = featuresMap.size();
		int[] hitBR = new int[8];
		for (ArrayList<Feature> featureList : featuresMap.values()) {
			for (int i = 0; i < 300; i++) {
				if (featureList.get(i).isPositive) {
					if (i < 1) {
						hitBR[0]++;
					}
					if (i < 5) {
						hitBR[1]++;
					}
					if (i < 10) {
						hitBR[2]++;
					}
					if (i < 15) {
						hitBR[3]++;
					}
					if (i < 20) {
						hitBR[4]++;
					}
					if (i < 100) {
						hitBR[5]++;
					}
					if (i < 200) {
						hitBR[6]++;
					}
					hitBR[7]++;
					break;
				}
			}
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append("================= Simple Evaluating =====================\n");
		builder.append(String.format("Top1: %d / %d = %f", hitBR[0], total, (double)hitBR[0] / total) + "\n");
		builder.append(String.format("Top5: %d / %d = %f", hitBR[1], total, (double)hitBR[1] / total) + "\n");
		builder.append(String.format("Top10: %d / %d = %f", hitBR[2], total, (double)hitBR[2] / total) + "\n");
		builder.append(String.format("Top15: %d / %d = %f", hitBR[3], total, (double)hitBR[3] / total) + "\n");
		builder.append(String.format("Top20: %d / %d = %f", hitBR[4], total, (double)hitBR[4] / total) + "\n");
		builder.append(String.format("Top100: %d / %d = %f", hitBR[5], total, (double)hitBR[5] / total) + "\n");
		builder.append(String.format("Top200: %d / %d = %f", hitBR[6], total, (double)hitBR[6] / total) + "\n");
		builder.append(String.format("Top300: %d / %d = %f", hitBR[7], total, (double)hitBR[7] / total) + "\n");
		builder.append("\n");
		System.out.println(builder.toString());
		return builder.toString();
	}
	
}
