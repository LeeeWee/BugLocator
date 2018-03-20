package edu.whu.pllab.buglocator.tests;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Map.Entry;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.common.TokenScore;
import edu.whu.pllab.buglocator.vectorizer.BugReportTfidfVectorizer;

public class BugReportRepositoryTest {
	
	private static BugReportRepository brRepo;
	
	
	public static void main(String[] args) throws Exception {
		String output = "D:\\data\\working\\jdt\\BugReportRepositoryTest.txt";
//		String output = "/Users/liwei/Documents/defect-prediction/working/BugReportTest.txt";
		
		@SuppressWarnings("unused")
		Property property = Property.loadInstance();
		brRepo = new BugReportRepository();
		BugReportTfidfVectorizer vectorizer = new BugReportTfidfVectorizer(brRepo.getBugReports());
		vectorizer.train();
		vectorizer.calculateTokensWeight(brRepo.getBugReports());
		
		writeBugRepository(output);
		
	}
	
	public static void writeBugRepository(String output) throws Exception {
		BufferedWriter writer = new BufferedWriter(new FileWriter(output));
		for (Entry<Integer, BugReport> entry : brRepo.getBugReports().entrySet()) {
			StringBuilder builder = new StringBuilder();
			BugReport bugReport = entry.getValue();
			builder.append("BugID: " + bugReport.getBugID() + "\n");
			builder.append("summary: " + bugReport.getSummary() + "\n");
			builder.append("description: " + bugReport.getDescription() + "\n");
			builder.append("reportTime: " + bugReport.getReportTime() + "\n");
			builder.append("commitID: " + bugReport.getCommitID() + "\t");
			builder.append("commitTime: " + bugReport.getCommitTime() + "\n");
			builder.append("corpus: " + bugReport.getBugReportCorpus().getContent() + "\n");
			builder.append("contentNorm: " + bugReport.getBugReportCorpus().getContentNorm() + "\n");
			builder.append("contentTokens: ");
			for (TokenScore tokenScore : bugReport.getBugReportCorpus().getContentTokens().values()) {
				builder.append(String.format("%s(%f,%f,%f) ", tokenScore.getToken(), tokenScore.getTf(),
						tokenScore.getIdf(), tokenScore.getTokenWeight()));
			}
			builder.append("\nModifiedFiles: \n");
			for (String file : bugReport.getFixedFiles()) {
				builder.append("\t" + file + "\n");
			}
			builder.append("\n");
			writer.write(builder.toString());
		}
		writer.close();
	}
}
