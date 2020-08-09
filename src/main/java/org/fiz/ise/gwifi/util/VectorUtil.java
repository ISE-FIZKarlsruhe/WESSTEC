package org.fiz.ise.gwifi.util;

import java.util.List;

import org.apache.commons.math3.ml.distance.ManhattanDistance;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.nd4j.linalg.api.ndarray.INDArray;

public class VectorUtil {
	
	
	public static double distanceManhatten(double x[], double y[]) 
    { 
		ManhattanDistance a = new ManhattanDistance();
		return a.compute(x, y);
 
    } 
	public static double distanceEucline(double[] a, double[] b) {
		double diff_square_sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            diff_square_sum += (a[i] - b[i]) * (a[i] - b[i]);
        }
        return Math.sqrt(diff_square_sum);
    }
	
	public static double cosineSimilarity(double[] vectorA, double[] vectorB) {
	    double dotProduct = 0.0;
	    double normA = 0.0;
	    double normB = 0.0;
	    for (int i = 0; i < vectorA.length; i++) {
	        dotProduct += vectorA[i] * vectorB[i];
	        normA += Math.pow(vectorA[i], 2);
	        normB += Math.pow(vectorB[i], 2);
	    }   
	    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
	}
	public static double[] getSentenceVector(List<String> words, Word2Vec model,String sentence) {        
        INDArray a = null;
        try{
            a = model.getWordVectorsMean(words);
        }catch(Exception e) {
        	System.out.println("I am in getSentenceVector "+ words.size()+" "+a+"\n"+sentence);
        	e.printStackTrace();
            return null;
        }
        int cols = a.columns();
        double[] result = new double[cols];
        for(int i=0;i<cols;i++) {
            result[i] = a.getDouble(i);
        }
        return result;
    }
	public static double[] getSentenceVector(List<String> words, Word2Vec model) {        
		if (words.size()==1) {
			return model.getWordVector(words.get(0));
		}
		INDArray a = null;
		try{
			System.out.println("I am getting the mean");
			
			System.out.println(words);
			a = model.getWordVectorsMean(words);
		}catch(Exception e) {
			System.out.println(words);
			System.out.println("words size :" +words.size());
			System.out.println("Could not obtain the sentence vector");
			e.printStackTrace();
			return null;
		}
		int cols = a.columns();
		double[] result = new double[cols];
		for(int i=0;i<cols;i++) {
			result[i] = a.getDouble(i);
		}
		return result;
	}
	
	public static double[] getSentenceVector(String sentence, Word2Vec model) {        
		List<String> words = SentenceSegmentator.tokenizeSentence(sentence);
		if (words.size()==1) {
			return model.getWordVector(words.get(0));
		}
		INDArray a = null;
		try{
			a = model.getWordVectorsMean(words);
		}catch(Exception e) {
			System.out.println(words);
			e.printStackTrace();
			return null;
		}
		int cols = a.columns();
		double[] result = new double[cols];
		for(int i=0;i<cols;i++) {
			result[i] = a.getDouble(i);
		}
		return result;
	}
	public static double getSimilarity2Vecs(double[] docVec,double[] wordVec) {
		if (docVec!=null && wordVec!=null) {
			return VectorUtil.cosineSimilarity(docVec, wordVec);
		}
		return 0;
	}
//	public static double[] getSentenceVector(List<String> words, Word2Vec model) {        
//        INDArray a = null;
//        try{
//            a = model.getWordVectorsMean(words);
//        }catch(Exception e) {
//        	System.out.println("I am in getSentenceVector exception "+words +" "+words.size()+" "+a);
//        	e.printStackTrace();
//        	System.exit(1);
//            return null;
//        }
//        int cols = a.columns();
//        double[] result = new double[cols];
//        for(int i=0;i<cols;i++) {
//            result[i] = a.getDouble(i);
//        }
//        return result;
//    }

}
