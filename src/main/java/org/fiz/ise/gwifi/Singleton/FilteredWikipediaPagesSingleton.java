package org.fiz.ise.gwifi.Singleton;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.Config;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.util.PageIterator;
import edu.kit.aifb.gwifi.model.Wikipedia;

public class FilteredWikipediaPagesSingleton {
	private final static Dataset TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	private static FilteredWikipediaPagesSingleton single_instance = null;
	public Set<Article> categoryFilteredArticles;
	public Set<Article> articles;

	private FilteredWikipediaPagesSingleton()
	{
		Wikipedia wikipedia = WikipediaSingleton.getInstance().wikipedia;
		categoryFilteredArticles = new HashSet<>();
		articles = new HashSet<>();

		CategorySingleton categories = CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE));
		Set<Category> setMainCat = new HashSet<>(categories.setAllCategories);
		try {
			PageIterator pageIterator = wikipedia.getPageIterator();
			while (pageIterator.hasNext()) {
				Page page = pageIterator.next();
				if (page.getType().equals(PageType.article)) {
					Article article = wikipedia.getArticleByTitle(page.getTitle());
					if(article!=null) {
						articles.add(article);
					}
				}
			}
			for (Category category : setMainCat) {
				Article[] temp = category.getChildArticles();
				for (int i = 0; i < temp.length; i++) {
					if (temp[i].getType().equals(PageType.article)) {
						categoryFilteredArticles.add(temp[i]);
					}
				}
			}
			System.out.println("We have " + categoryFilteredArticles.size()+" articles after category based filtering");
			System.out.println("We have " + articles.size()+" articles after article page based filtering");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static FilteredWikipediaPagesSingleton getInstance()
	{
		if (single_instance == null)
			single_instance = new FilteredWikipediaPagesSingleton();
		return single_instance;
	}
}
