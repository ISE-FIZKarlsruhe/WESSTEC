package org.fiz.ise.gwifi.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.EmbeddingModel;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.Request_LINEServer;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;

public class EmbeddingsService {

	private static WikipediaSingleton singleton = WikipediaSingleton.getInstance();
	private static Wikipedia wikipedia = singleton.wikipedia;
	private final static Dataset TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	private static CategorySingleton singCategory = CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE));
	
	public static List<String>  getMostSimilarConcepts(String entity1,EmbeddingModel model,int n) {
		try {
			List<String> similarity = new ArrayList<>( Request_LINEServer.getMostSimilarConcepts(entity1, EmbeddingModel.LINE_COMBINED, n));
			return similarity;
		} catch (Exception e) {
			return null;
		}
	}
	public static double getSimilarity_CONLL(String id1, String id2 ){
		try {
			Double similarity = Request_LINEServer.getSimilarity(id1,id2,EmbeddingModel.CONLL);
			return similarity;
		} catch (Exception e) {
			return 0.0;
		}
	}
	public static double getSimilarity(String id1, String id2 ){
		try {
			Double similarity = Request_LINEServer.getSimilarity(id1,id2,EmbeddingModel.PTE_modified);
			return similarity;
		} catch (Exception e) {
			return 0.0;
		}
	}
	public static Map<Category, Double> getSimilarityForEachCategory(Annotation annotation)
	{
		Set<Category> categories = new HashSet<>(singCategory.setMainCategories);
		Map<Category, Double> map = new HashMap<>();
		for(Category category:categories){
			//String key =a+" "+category.getTitle();
			map.put(category, Request_LINEServer.getSimilarity(String.valueOf(wikipedia.getArticleByTitle(annotation.getTitle()).getId()), String.valueOf(category.getId()),EmbeddingModel.LINE_COMBINED));
		}	
		Map<Category, Double> mapSorted = new LinkedHashMap<>(MapUtil.sortByValueDescending(map));
		return mapSorted;
	}
	public static Category getMostSimilarCategory(Annotation annotation,EmbeddingModel m)
	{
		Set<Category> categories = new HashSet<>(singCategory.setMainCategories);
		Map<Category, Double> map = new HashMap<>();
		for(Category category:categories){
			map.put(category, Request_LINEServer.getSimilarity(String.valueOf(wikipedia.getArticleByTitle(annotation.getTitle()).getId()), String.valueOf(category.getId()),m));
		}	
		Map<Category, Double> mapSorted = new LinkedHashMap<>(MapUtil.sortByValueDescending(map));
		return MapUtil.getFirst(mapSorted).getKey();
	}
}
