package org.fiz.ise.gwifi.test.longDocument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.GoogleModelSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.model.EmbeddingModel;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.VectorUtil;
import org.nd4j.linalg.api.ndarray.INDArray;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Category;
import edu.stanford.nlp.neural.Embedding;

public class BasedOnWordsCategorize {
	private final static Dataset TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE"); //here you get the name of the dataset
	private static Set<Category> setMainCategories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);
	static final Logger secondLOG = Logger.getLogger("debugLogger");

	public static Category getBestMatchingCategory(String text,List<Category> gtList, Map<Category, Set<Category>> map, Map<String, List<String>> cacheNearestwords) {
		try {
			Map<Category, Double> mapScore = new HashMap<>(); 
			secondLOG.info(text+" "+gtList+"\n");
			
			for (Category mainCat : setMainCategories) {
				List<String> wordsNearest = cacheNearestwords.get(mainCat.getTitle());
				double similarity=0;
				for (String s : wordsNearest) {
					similarity+=getSimilarity(text,s);
				}
				double score=similarity+getSimilarity(text,mainCat.getTitle());
				mapScore.put(mainCat, score);
			}
			
			Map<Category, Double>  sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			for(Entry<Category, Double> e : sortedMap.entrySet()) {
				secondLOG.info(e.getKey()+": "+e.getValue());
			}
			Category firstElement = MapUtil.getFirst(sortedMap).getKey();
			return firstElement;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Category getBestMatchingCategory(String text,List<Category> gtList, Map<Category, Set<Category>> map) {
		try {
			Map<Category, Double> mapScore = new HashMap<>(); 
			secondLOG.info(text+" "+gtList+"\n");
			for (Category mainCat : setMainCategories) {
				double score=getSimilarity(text,mainCat.getTitle());
				mapScore.put(mainCat, score);
			}
			Map<Category, Double>  sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			for(Entry<Category, Double> e : sortedMap.entrySet()) {
				secondLOG.info(e.getKey()+": "+e.getValue());
			}
			Category firstElement = MapUtil.getFirst(sortedMap).getKey();
			return firstElement;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static double getSimilarity(EmbeddingModel eName,double[] docVec,String category) {
		Word2Vec model=null;
		if (eName.equals(EmbeddingModel.LINE_Ent_Ent)) {
			model= LINE_modelSingleton.getInstance().lineModel;
		}
		else if (eName.equals(EmbeddingModel.GOOGLE)) {
			model = GoogleModelSingleton.getInstance().google_model;
		}
		double[] catVec = VectorUtil.getSentenceVector(Arrays.asList(category), model);
		if (docVec!=null && catVec!=null) {
			return VectorUtil.cosineSimilarity(docVec, catVec);
		}
		return 0;
	}
	public static double getSimilarity(List<String> words,List<String> words2) {
		Word2Vec model= LINE_modelSingleton.getInstance().lineModel;
		double[] docVec= VectorUtil.getSentenceVector(words, model);
		double[] catVec= VectorUtil.getSentenceVector(words2, model);
		if (docVec!=null && catVec!=null) {
			return VectorUtil.cosineSimilarity(docVec, catVec);
		}
		return 0;
	}
	public static double getSimilarityForAnnotations(List<Annotation> annotations,String category) {
		Word2Vec model= LINE_modelSingleton.getInstance().lineModel;
		List<String> words = new ArrayList<>();
		for(Annotation a: annotations) {
			words.add(String.valueOf(a.getId()));
		}
		double[] docVec= VectorUtil.getSentenceVector(words, model);
		double[] catVec = VectorUtil.getSentenceVector(Arrays.asList(category), model);
		if (docVec!=null && catVec!=null) {
			return VectorUtil.cosineSimilarity(docVec, catVec);
		}
		return 0;
	}
	public static double getSimilarity(List<String> words,String category) {
		Word2Vec model= LINE_modelSingleton.getInstance().lineModel;
		double[] docVec= VectorUtil.getSentenceVector(words, model);
		double[] catVec = VectorUtil.getSentenceVector(Arrays.asList(category), model);
		if (docVec!=null && catVec!=null) {
			return VectorUtil.cosineSimilarity(docVec, catVec);
		}
		return 0;
	}
	
	public static double getSimilarity(String text,String category) {
		Word2Vec model= LINE_modelSingleton.getInstance().lineModel;
		final String[] words = text.split(" ");
		final String[] cats = category.split(" ");
		double[] catVec2;

		double[] docVec= VectorUtil.getSentenceVector(Arrays.asList(words), model);
		double[] catVec = VectorUtil.getSentenceVector(Arrays.asList(category), model);
		//		double[] catVec = model.getWordVector(category);
		//		if(cats.length==1) {
		//			if (cosineSimilarity(docVec, catVec)!=cosineSimilarity(docVec, catVec2)) {
		//				System.out.println("ERROR :" + category);
		//			}
		//		}
		if (docVec!=null && catVec!=null) {
			return VectorUtil.cosineSimilarity(docVec, catVec);
		}
		return 0;
	}
	
}
