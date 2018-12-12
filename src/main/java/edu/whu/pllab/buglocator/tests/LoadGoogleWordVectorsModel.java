package edu.whu.pllab.buglocator.tests;

import java.io.File;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.nd4j.linalg.api.ndarray.INDArray;

public class LoadGoogleWordVectorsModel {
	
	public static void main(String[] args) {
		
		String googleWordVectorsPath = "D:\\data\\GoogleNews-vectors-negative300.bin";
		WordVectors vec = WordVectorSerializer.loadStaticModel(new File(googleWordVectorsPath));
		
		INDArray vector = vec.getWordVectorMatrixNormalized("insurance");
	
		System.out.println("vector of 'insurance': " + vector);
//		System.out.println("10 words cloest to 'insurance': " +lst);
		
	}

}
