package edu.whu.pllab.buglocator.similarityrecommender;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import edu.whu.pllab.buglocator.common.BugReport;

/**
 * Abstract class for similarity algorithm recommenders
 * @author Liwei
 *
 */
public abstract class SimAlgoRecommender {
	
	protected HashMap<Integer, BugReport> trainingBugReports;
	
	/** recommend similarity algorithm for single input bug report */
	public abstract TreeMap<Integer, Double> recommend(BugReport bugReport); 
	
	/** recommend similarity algorithm for test bug reports */
	public HashMap<BugReport, TreeMap<Integer, Double>> recommend(HashMap<Integer, BugReport> testBugReports) {
		HashMap<BugReport, TreeMap<Integer, Double>> ret = new HashMap<BugReport, TreeMap<Integer, Double>>();
		for (Entry<Integer, BugReport> entry : testBugReports.entrySet()) {
			ret.put(entry.getValue(), recommend(entry.getValue()));
		}
		return ret;
	}

}
