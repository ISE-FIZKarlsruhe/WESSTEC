package org.fiz.ise.gwifi.test.afterESWC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.VectorUtil;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Category;
import java_cup.action_part;

public class GenerateFeatureSet {
	private static final String DATASET_TEST_AG = Config.getString("DATASET_TEST_AG","");
	private static final String DATASET_TRAIN_AG = Config.getString("DATASET_TRAIN_AG","");
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	
	public static void main(String[] args) throws Exception {
		TestBasedonSortTextDatasets test = new TestBasedonSortTextDatasets();
		Map<String,List<Category>> dataset = new HashMap<>(test.read_dataset_AG(AG_DataType.TITLEANDDESCRIPTION, DATASET_TEST_AG));
//		Map<String, double[]> result = new HashMap<String, double[]>();
		String str =null;
		
		try {
			for(String line : dataset.keySet()) {
				double[] vector = featureSet_EntitiyVectorMean(line);
				if (vector==null || vector.length==0) {
					System.out.println(line);
				}
				else {
					String strVector="";
	 				for (int j = 0; j < vector.length; j++) {
	 					strVector=strVector+(String.valueOf(vector[j]) + ",");
	 				}
	 				strVector = strVector.substring(0, strVector.length() - 1);
	 				secondLOG.info(line+"\t"+strVector);
				}
				//result.put(line, featureSet_EntitiyVectorMean(line));
			}	
		} catch (Exception e) {
			System.out.println(str);
		}
	}
	public static String featureSet_EntitiyVectorMeanAsString(String str) throws Exception {
		List<String> lstEntityID = new ArrayList<String>();
		List<Annotation> lstAnnotations = new ArrayList<>();
		AnnotationSingleton.getInstance().service.annotate(str, lstAnnotations);//annotate the given text
		for(Annotation a:lstAnnotations) {
			if (LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(a.getId()))){
					//WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())!=null) {
				lstEntityID.add(String.valueOf(a.getId()));
			}
		}
		if (lstEntityID.size()==0) {
			System.out.println(str);
		}
		
		double[] vector=VectorUtil.getSentenceVector(lstEntityID,LINE_modelSingleton.getInstance().lineModel,str);
		if (vector==null || vector.length==0) {
			System.out.println(str);
			return null;
		}
		else {
			String strVector="";
				for (int j = 0; j < vector.length; j++) {
					strVector=strVector+(String.valueOf(vector[j]) + ",");
				}
				strVector = strVector.substring(0, strVector.length() - 1);
				return strVector;
		}
	}
	public static double[] featureSet_EntitiyVectorMean(String str) throws Exception {
		List<String> lstEntityID = new ArrayList<String>();
		List<Annotation> lstAnnotations = new ArrayList<>();
		AnnotationSingleton.getInstance().service.annotate(str, lstAnnotations);//annotate the given text
		for(Annotation a:lstAnnotations) {
			if (LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(a.getId()))){
					//WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())!=null) {
				lstEntityID.add(String.valueOf(a.getId()));
			}
		}
//		double[] documentVec = VectorUtil.getSentenceVector(lstEntityID,LINE_modelSingleton.getInstance().lineModel);
		if (lstEntityID.size()==0) {
			System.out.println(str);
		}
		return VectorUtil.getSentenceVector(lstEntityID,LINE_modelSingleton.getInstance().lineModel,str);
	}
}
