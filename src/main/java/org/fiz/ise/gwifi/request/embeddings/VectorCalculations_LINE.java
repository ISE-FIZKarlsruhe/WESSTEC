package org.fiz.ise.gwifi.request.embeddings;

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
import org.fiz.ise.gwifi.util.Print;
import org.fiz.ise.gwifi.util.Request_LINEServer;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;

public class VectorCalculations_LINE {
	private final static Dataset TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	public static void main(String[] args) {
		WikipediaSingleton singleton = WikipediaSingleton.getInstance();
		Wikipedia wikipedia = singleton.wikipedia;
		CategorySingleton singCategory = CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE));
		Set<Category> categories = new HashSet<>(singCategory.setMainCategories);
		List<String> list = new ArrayList<>();

		list.add("Albert Einstein"); 
		list.add("Physics");
		list.add("Latent semantic analysis");
		list.add("Computer");

		for (EmbeddingModel m : EmbeddingModel.values()) {
			System.out.println("---------------------Model----------------------:"+m);
			for(String a : list)
			{
				Map<String, Double> map = new HashMap<>();
				for(Category category:categories){
					String key =a+" "+category.getTitle();
					map.put(key, Request_LINEServer.getSimilarity(String.valueOf(wikipedia.getArticleByTitle(a).getId()), String.valueOf(category.getId()),m));
				}	
				Map<String, Double> mapSorted = new LinkedHashMap<>(MapUtil.sortByValueDescending(map));
				Print.printMap(mapSorted);
				System.out.println("---------------------------------------");
			}
		}		
		//Category cat = wikipedia.getCategoryByTitle(category);



	}
}
