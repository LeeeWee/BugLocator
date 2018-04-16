package edu.whu.pllab.buglocator.tests;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReport;
import edu.whu.pllab.buglocator.common.BugReportRepository;
import edu.whu.pllab.buglocator.common.SourceCodeRepository;
import edu.whu.pllab.buglocator.evaluation.Evaluator;
import edu.whu.pllab.buglocator.rankingmodel.IntegratedScore;
import edu.whu.pllab.buglocator.rankingmodel.RankingModelGenerator;
import edu.whu.pllab.buglocator.rankingmodel.SVMRank;
import edu.whu.pllab.buglocator.utils.BugReportsSplitter;
import edu.whu.pllab.buglocator.vectorizer.BugReportTfidfVectorizer;
import edu.whu.pllab.buglocator.vectorizer.SourceCodeTfidfVectorizer;

public class RankingModelGeneratorTest {
	
	public static void main(String[] args) {
		
		Property property = Property.loadInstance();
		
		// initialize bugReport Repository
		BugReportRepository brRepo = new BugReportRepository();
		
		BugReportsSplitter validation = new BugReportsSplitter(brRepo.getBugReports(), 10);
		List<HashMap<Integer, BugReport>> bugReportsMapList = validation.getBugReportsMapList();
		HashMap<Integer, BugReport> trainingBugReports = bugReportsMapList.get(0);
		HashMap<Integer, BugReport> testBugReports = bugReportsMapList.get(1);
		List<String> preCommitIDList = validation.getPreCommitIDList();
		
		// training tfidf model for training BugReorts
		BugReportTfidfVectorizer brVectorizer = new BugReportTfidfVectorizer(trainingBugReports);
		brVectorizer.train();
		brVectorizer.calculateTokensWeight(brRepo.getBugReports());
		
		// initialize source code repository
		SourceCodeRepository codeRepo = new SourceCodeRepository(preCommitIDList.get(0));
		SourceCodeTfidfVectorizer codeVectorizer = new SourceCodeTfidfVectorizer(codeRepo.getSourceCodeMap());
		codeVectorizer.train();
		codeVectorizer.calculateTokensWeight(codeRepo.getSourceCodeMap());
		
		// initialize rankModelGenerator
		RankingModelGenerator generator = new RankingModelGenerator();
		generator.setSourceCodeMap(codeRepo.getSourceCodeMap());
		// generate training data
		generator.setTrainingBugReportsMap(trainingBugReports);
		generator.generate(true);
		generator.writeRankingFeatures(property.getTrainingFeaturesPath());
		generator.saveParameters(new File(property.getFeaturesExtremumPath()));
		
		// reset source code repository to the i+1-th version, retrain tfidf model
		codeRepo = new SourceCodeRepository(preCommitIDList.get(1));
		codeVectorizer = new SourceCodeTfidfVectorizer(codeRepo.getSourceCodeMap());
		codeVectorizer.train();
		codeVectorizer.calculateTokensWeight(codeRepo.getSourceCodeMap());
		// generate test data
		generator.setTestBugReportsMap(testBugReports);
		generator.generate(false);
		generator.writeRankingFeatures(property.getTestFeaturesPath());
		
		// svm rank training and predicting
		SVMRank.train(200);
		SVMRank.predict();
		
		// get test integratedScores
		HashMap<BugReport, List<IntegratedScore>> testIntegratedScores = generator.getFinals();
		
		//evaluate
		Evaluator evaluator = new Evaluator(testIntegratedScores);
		evaluator.evaluate();
		
	}

}
