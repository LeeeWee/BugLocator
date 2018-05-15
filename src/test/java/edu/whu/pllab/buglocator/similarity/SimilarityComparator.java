package edu.whu.pllab.buglocator.similarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.common.SourceCode;
import edu.whu.pllab.buglocator.common.SourceCodeRepository;
import edu.whu.pllab.buglocator.rankingmodel.IntegratedScore;
import edu.whu.pllab.buglocator.vectorizer.BugReportTfidfVectorizer;
import edu.whu.pllab.buglocator.vectorizer.SourceCodeTfidfVectorizer;


public class SimilarityComparator {

private static final Logger logger = LoggerFactory.getLogger(SimilarityComparator.class);
	
	public static void main(String[] args) throws Exception {
		
		String[] products = {"ASPECTJ", "SWT", "BIRT", "ECLIPSE_PLATFORM_UI", "TOMCAT", "JDT"};
		
		for (String product : products) {
			logger.info("Current product: " + product);
			Property property = Property.loadInstance(product);
			
			// initialize bugReport repository and code repository
			BugReportRepository brRepo = new BugReportRepository();
			
			SourceCodeRepository codeRepo = new SourceCodeRepository();
			SourceCodeTfidfVectorizer codeVectorizer = new SourceCodeTfidfVectorizer(codeRepo.getSourceCodeMap());
			codeVectorizer.setUsingOkapi(false);
			codeVectorizer.train();
			codeVectorizer.calculateTokensWeight(codeRepo.getSourceCodeMap());
		
			// train tfidf model using training bug reports
			BugReportTfidfVectorizer brVectorizer = new BugReportTfidfVectorizer(codeVectorizer.getTfidf());
			// TfidfVectorizer training and test bug reports
			brVectorizer.calculateTokensWeight(brRepo.getBugReports());
			
			List<BugReport> sortedBugReports = brRepo.getSortedBugReports();
			
			
		}
		
	}
	
	public static List<IntegratedScore> calculateVSMSimilarity(BugReport bugReport,
			HashMap<String, SourceCode> sourceCodeMap) {
		List<IntegratedScore> integratedScoreList = new ArrayList<IntegratedScore>();
		Similarity sim = new Similarity();
		for (Entry<String, SourceCode> entry : sourceCodeMap.entrySet()) {
			double similarity = sim.similarity(bugReport, entry.getValue(), Similarity.VSM);
//			for (Method method : entry.getValue().getMethodList()) {
//				double methodSimilarity = Similarity.similarity(bugReport, method, Similarity.VSM);
//				if (methodSimilarity > similarity)
//					similarity = methodSimilarity;
//			}
			IntegratedScore score = new IntegratedScore(entry.getKey(), false, null);
			score.setIntegratedScore(similarity);
			integratedScoreList.add(score);
		}
		integratedScoreList.sort(new IntegratedScore.IntegratedScoreComparator());
		for (int i = 0; i < integratedScoreList.size(); i++) {
			integratedScoreList.get(i).rank = i;
		}
		return integratedScoreList;
	}
	
}
