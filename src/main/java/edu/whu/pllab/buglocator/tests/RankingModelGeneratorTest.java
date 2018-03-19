package edu.whu.pllab.buglocator.tests;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.common.SourceCodeRepository;
import edu.whu.pllab.buglocator.rankingmodel.RankingModelGenerator;
import edu.whu.pllab.buglocator.vectorizer.BugReportTfidfVectorizer;
import edu.whu.pllab.buglocator.vectorizer.SourceCodeTfidfVectorizer;

public class RankingModelGeneratorTest {
	
	public static void main(String[] args) {
		
		Property property = Property.loadInstance();
		
		// initialize bugReport Repository
		BugReportRepository brRepo = new BugReportRepository();
		BugReportTfidfVectorizer brVectorizer = new BugReportTfidfVectorizer(brRepo.getBugReports());
		brVectorizer.train();
		brVectorizer.calculateTokensWeight(brRepo.getBugReports());
		
		// initialize source code repository
		SourceCodeRepository codeRepo = new SourceCodeRepository();
		SourceCodeTfidfVectorizer codeVectorizer = new SourceCodeTfidfVectorizer(codeRepo.getSourceCodeMaps());
		codeVectorizer.train();
		codeVectorizer.calculateTokensWeight(codeRepo.getSourceCodeMaps());
		
		RankingModelGenerator generator = new RankingModelGenerator();
		generator.setBugReportsMap(brRepo.getBugReports());
		generator.setSourceCodeMap(codeRepo.getSourceCodeMaps());
		
		generator.generate(true);
		generator.writeRankingFeatures(property.getTrainingFeaturesPath());
	}

}
