package org.fiz.ise.gwifi.dataset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.dataset.assignLabels.AssignLabelsBasedOnConfVecSimilarity;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.model.EmbeddingModel;
import org.fiz.ise.gwifi.util.Config;

import edu.kit.aifb.gwifi.model.Article;

public class ErrorAnalysis {
	private static final String DATASET_DBP_TRAIN = Config.getString("DATASET_DBP_TRAIN","");
	private static final String DATASET_DBP_TRAIN_CATEGORIZED_LINE = Config.getString("DATASET_DBP_TRAIN_CATEGORIZED_LINE","");
	private static final String DATASET_DBP_TRAIN_CATEGORIZED_D2Vec = Config.getString("DATASET_DBP_TRAIN_CATEGORIZED_D2Vec","");
	private static final String DATASET_DBP_TRAIN_CATEGORIZED_GOOGLE = Config.getString("DATASET_DBP_TRAIN_CATEGORIZED_GOOGLE","");
	private final static String DATASET_TRAIN_AG = Config.getString("DATASET_TRAIN_AG","");
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");
	Map<String, List<Article>> map_AG_test_gt = null;//ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION,Config.getString("DATASET_TEST_AG",""));

	static final Map<String, String> map_DOC2VEC_AG= new HashMap<String, String>(AssignLabelsBasedOnConfVecSimilarity.readLabelAssignment_AG(Dataset.AG, EmbeddingModel.Doc2Vec));
	static final Map<String, String> map_GOOGLE_AG = new HashMap<String, String>(AssignLabelsBasedOnConfVecSimilarity.readLabelAssignment_AG(Dataset.AG, EmbeddingModel.GOOGLE));
	static final Map<String, String> map_LINE_AG = new HashMap<String, String>(AssignLabelsBasedOnConfVecSimilarity.readLabelAssignment_AG(Dataset.AG, EmbeddingModel.LINE_Ent_Ent));
	
	static final Map<String,  List<Article>> map_DOC2VEC_DBp = new HashMap<String,List<Article>>(ReadDataset.read_dataset_Doc2Vec_categorized(Dataset.DBpedia, DATASET_DBP_TRAIN_CATEGORIZED_D2Vec));
	static final Map<String,  List<Article>> map_GOOGLE_DBp = new HashMap<String,List<Article>>( ReadDataset.read_dataset_DBPedia_SampleLabel(DATASET_DBP_TRAIN_CATEGORIZED_GOOGLE));
	static final Map<String,  List<Article>> map_LINE_DBp = new HashMap<String,List<Article>>(ReadDataset.read_dataset_DBPedia_SampleLabel(DATASET_DBP_TRAIN_CATEGORIZED_LINE));


	public static void main(String[] args) {
		Map<String, List<Article>> dataset = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION,DATASET_TRAIN_AG);
		int countCorrectOverlapThree=0;
		int countCorrectOverlapTwo=0;
		
		int countWrongOverlapThree=0;
		int countWrongOverlapTwo=0;
		
		int countLINECorrect=0;
		int countGoogleCorrect=0;
		int countD2VCorrect=0;
		
		for(Entry<String, List<Article>> e : dataset.entrySet()) {
			
			String str_DOC2VEC = null;
			String str_LINE = null;
			String str_GOOGLE = null;
			
			if (map_DOC2VEC_AG.containsKey(e.getKey())) {
				str_DOC2VEC = map_DOC2VEC_AG.get(e.getKey());
			}
			if (map_LINE_AG.containsKey(e.getKey())) {
				str_LINE = map_LINE_AG.get(e.getKey());
			}
			if (map_GOOGLE_AG.containsKey(e.getKey())) {
				str_GOOGLE = map_GOOGLE_AG.get(e.getKey());
			}
			if ((str_DOC2VEC!=null&&str_LINE!=null&&str_GOOGLE!=null )&& str_DOC2VEC.equals(str_GOOGLE)&&str_DOC2VEC.equals(str_LINE)) {
				if (str_DOC2VEC.equals(e.getValue().get(0).getTitle())) {
					countCorrectOverlapThree++;
				}
				else {
					countWrongOverlapThree++;
				}
			}
			else if(str_DOC2VEC!=null&&str_LINE!=null&&(str_DOC2VEC.equals(str_LINE))) { //D2Vec=LINE
				
				if (str_DOC2VEC.equals(e.getValue().get(0).getTitle())) {
					countCorrectOverlapTwo++;
				}
				else {
					countWrongOverlapTwo++;
				}
			}
			else if(str_DOC2VEC!=null&&str_GOOGLE!=null&&str_DOC2VEC.equals(str_GOOGLE)) {//D2Vec=GOOGLE
				if (str_DOC2VEC.equals(e.getValue().get(0).getTitle())) {
					countCorrectOverlapTwo++;
				}
				else {
					countWrongOverlapTwo++;
				}

			}
			else if(str_LINE!=null&&str_GOOGLE!=null&&str_LINE.equals(str_GOOGLE)) {//LINE=GOOGLE
				if (str_LINE.equals(e.getValue().get(0).getTitle())) {
					countCorrectOverlapTwo++;
				}
				else {
					countWrongOverlapTwo++;
				}
			}
			else {

				if (str_LINE!=null&&str_LINE.equals(e.getValue().get(0).getTitle())) {
					countLINECorrect++;
				}
				if (str_DOC2VEC!=null&&str_DOC2VEC.equals(e.getValue().get(0).getTitle())) {
					countD2VCorrect++;
				}
				if (str_GOOGLE.equals(e.getValue().get(0).getTitle())) {
					countGoogleCorrect++;
				}
			}

			
		}
		System.out.println("countCorrectOverlapThree: "+countCorrectOverlapThree);
		System.out.println("countWrongOverlapThree: "+countWrongOverlapThree);

		System.out.println("countCorrectOverlapTwo: "+countCorrectOverlapTwo);
		System.out.println("countWrongOverlapTwo: "+countWrongOverlapTwo);
		
		System.out.println("countLINECorrect: "+countLINECorrect);
		System.out.println("countGoogleCorrect: "+countGoogleCorrect);
		System.out.println("countD2VCorrect: "+countD2VCorrect);

	}

}
