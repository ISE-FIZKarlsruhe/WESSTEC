package org.fiz.ise.gwifi.dataset;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.GoogleModelSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.train.generation.GenerateDatasetForNN;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.model.EmbeddingModel;
import org.fiz.ise.gwifi.test.longDocument.BasedOnWordsCategorize;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.VectorUtil;
import org.nd4j.linalg.api.ndarray.INDArray;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;

public class AnalyseMRDataset {

	private static final String DATASET_TRAIN_MR_POS = Config.getString("DATASET_TRAIN_MR_POS","");
	private static final String DATASET_TRAIN_MR_NEG = Config.getString("DATASET_TRAIN_MR_NEG","");
	public static void main(String[] args) throws IOException {
		
//		LINE_modelSingleton.getInstance();
//		AnnotationSingleton.getInstance();
//		
//		GenerateDatasetForNN generate = new GenerateDatasetForNN();
//		generate.labelTrainSetParalel(EmbeddingModel.LINE_Ent_Ent, Dataset.AG);
		
		List<String> lines_pos = FileUtils.readLines(new File(DATASET_TRAIN_MR_POS), "utf-8");
		List<String> lines_neg = FileUtils.readLines(new File(DATASET_TRAIN_MR_NEG), "utf-8");
		analyse(lines_pos,lines_neg);
//		AnalyseDataset.findMostSimilarWordForVectorOfDataset(lines_pos, "mr_mostSimWordsForSentenceVec_pos");
//		AnalyseDataset.findMostSimilarWordForVectorOfDataset(lines_neg, "mr_mostSimWordsForSentenceVec_neg");
		
		
//		AnnonatationUtil.findFreqOfWord(lines_pos, "mr_freqOfWords_pos");
//		AnnonatationUtil.findFreqOfWord(lines_neg, "mr_freqOfWords_neg");
		
	
//		AnalyseDataset.findMostSimilarEntitesForDataset(lines_pos, Dataset.MR, "pos");
//		AnalyseDataset.findMostSimilarEntitesForDataset(lines_neg, Dataset.MR, "neg");
		
		//AnnonatationUtil.findFreqOfEntity(AnnonatationUtil.findAnnotationAll(lines_pos),"AnnotationFrequency_MR_train_pos");
		//AnnonatationUtil.findFreqOfEntity(AnnonatationUtil.findAnnotationAll(lines_neg),"AnnotationFrequency_MR_train_neg");
		
		//AnalyseDataset.countEntitiesOfDatasets(new ArrayList<String>(read_trec_dataset().keySet()));

		
		
	}
	public static void analyse(List<String> datasetPos,List<String> datasetNeg) {
//		String[] adjPos = {"terrific", "great", "awesome", "enjoyable"};
		String[] adjPos = {"first-rate","insightful","clever","charming","comical","charismatic",
				"enjoyable","uproarious","original","tender","hilarious","absorbing","sensitive",
				"riveting","intriguing","powerful","fascinating","pleasant","surprising","dazzling",
				"thought provoking","imaginative","legendary","unpretentious"};
		System.out.println(adjPos.length);
		
		String[] adjNeg = {"second-rate","violent","moronic","third-rate","flawed","juvenile",
				"boring","distasteful","ordinary","disgusting","senseless","static","brutal",
				"confused","disappointing","bloody","silly","tired","predictable","stupid",
				"uninteresting","weak","incredibly tiresome","trite","uneven","cliché ridden",
				"outdated","dreadful","bland"};

		System.out.println(adjNeg.length);
		
		
		//		String[] adjNeg = {"horrible", "disappointing", "boring", "weak"};
		int countWrongPos=0;
		int countWrongNeg=0;
		List<double[]> lstVectorsPos = new ArrayList<double[]>();
		List<double[]> lstVectorsNeg = new ArrayList<double[]>();
		
		for (int i = 0; i < adjPos.length; i++) {
			lstVectorsPos.add(VectorUtil.getSentenceVector(Arrays.asList(adjPos[i]), GoogleModelSingleton.getInstance().google_model));
		}
		for (int i = 0; i < adjNeg.length; i++) {
			lstVectorsNeg.add(VectorUtil.getSentenceVector(Arrays.asList(adjNeg[i]), GoogleModelSingleton.getInstance().google_model));
		}
		
		for (String sP : datasetPos) {
			double simP=0;
			double simN=0;
			double[] vecSentence=VectorUtil.getSentenceVector(sP,GoogleModelSingleton.getInstance().google_model);
			for(double[] vP : lstVectorsPos ) {
				simP+=VectorUtil.getSimilarity2Vecs(vP,vecSentence);
			}
			for(double[] vP : lstVectorsNeg ) {
				simN+=VectorUtil.getSimilarity2Vecs(vP,vecSentence);
			}
			simN=simN/29.0;
			simP=simP/24.0;
			if (simP<simN) {
				countWrongPos++;
			}
		}
		
		for (String sP : datasetNeg) {
			double simP=0;
			double simN=0;
			double[] vecSentence=VectorUtil.getSentenceVector(sP,GoogleModelSingleton.getInstance().google_model);
			for(double[] vP : lstVectorsPos ) {
				simP+=VectorUtil.getSimilarity2Vecs(vP,vecSentence);
			}
			for(double[] vP : lstVectorsNeg ) {
				simN+=VectorUtil.getSimilarity2Vecs(vP,vecSentence);
			}
			simN=simN/29.0;
			simP=simP/24.0;
			if (simN<simP) {
				countWrongNeg++;
			}
		}
		
		System.out.println("Total POs: "+datasetPos.size()+" Wrong Pos:"+countWrongPos);
		System.out.println("Total Neg: "+datasetNeg.size()+" Wrong Neg:"+countWrongNeg);
	
	}
	
}
