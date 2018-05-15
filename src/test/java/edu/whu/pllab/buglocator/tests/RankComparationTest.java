package edu.whu.pllab.buglocator.tests;

import java.io.File;
import java.util.List;

import edu.whu.pllab.buglocator.evaluation.SimpleEvaluator;
import edu.whu.pllab.buglocator.rankingmodel.IntegratedScore;

public class RankComparationTest {
	
	public static void foldTest(String foldPath) throws Exception {
		
		RankerTest ranker = new RankerTest(RankerTest.COORDINATE_ASCENT, foldPath);
		ranker.predict();
		SimpleEvaluator evaluator = new SimpleEvaluator(ranker.getTestFeaturesPath(), ranker.getPredictionsPath());
		evaluator.evaluate();
		List<List<IntegratedScore>> intergratedScores = evaluator.getIntergratedScores();
		
		SimpleEvaluator bluirEvaluator = new SimpleEvaluator(ranker.getTestFeaturesPath(),
				new File(foldPath, "BLUiR.predictions").getAbsolutePath());
		bluirEvaluator.evaluate();
		List<List<IntegratedScore>> bluirIntergratedScores = bluirEvaluator.getIntergratedScores();
		
		
	}

}
