package edu.whu.pllab.buglocator.common;

import java.util.Comparator;

public class SimilarBugReport extends BugReport {

	private Double similarity;
	
	public SimilarBugReport(BugReport br, Double similarity) {
		super(br.getBugID(), br.getSummary(), br.getDescription(), br.getReportTime(), br.getCommitID(),
				br.getCommitTime(), br.getFixedFiles());
		this.setBugReportCorpus(br.getBugReportCorpus());
		this.similarity = similarity;
	}
	
	public static class SimilarityComparator implements Comparator<SimilarBugReport> {

		public int compare(SimilarBugReport br1, SimilarBugReport br2) {
			if (br1.similarity < br2.similarity) 
				return 1;
			else if (br1.similarity > br2.similarity)
				return -1;
			else 
				return 0;
		}
		
	}

	public Double getSimilarity() {
		return similarity;
	}

	public void setSimilarity(Double similarity) {
		this.similarity = similarity;
	} 
	
}
