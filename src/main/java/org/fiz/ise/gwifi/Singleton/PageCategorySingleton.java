package org.fiz.ise.gwifi.Singleton;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.Config;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;

public class PageCategorySingleton {
	private static PageCategorySingleton single_instance = null;
	private final static Dataset TEST_DATASET_TYPE = Config.getEnum("TEST_DATASET_TYPE");
	public Map<Category, Set<Article>> mapMainCatAndArticles;

	private PageCategorySingleton() {
		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);
		mapMainCatAndArticles = new HashMap<>();
		for (Category c : setMainCategories) {
			Set<Category> childCategories = new HashSet<>(
					CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).mapMainCatAndSubCats
							.get(c));
		//	System.out.println("Page sing: "+c.getTitle()+" "+childCategories.size());
			Set<Article> sArticle = new HashSet<>();
			sArticle.addAll(Arrays.asList(c.getChildArticles()));
			for (Category child : childCategories) {
				sArticle.addAll(Arrays.asList(child.getChildArticles()));
			}
			mapMainCatAndArticles.put(c, sArticle);
		}
	}

	public static PageCategorySingleton getInstance() {
		if (single_instance == null)
			single_instance = new PageCategorySingleton();
		return single_instance;
	}
}
