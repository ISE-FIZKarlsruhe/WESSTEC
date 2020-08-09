package org.fiz.ise.gwifi.Singleton;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.Config;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.model.Redirect;
import edu.kit.aifb.gwifi.util.PageIterator;

public class RedirectSingelton {
	private static RedirectSingelton single_instance = null;
	public Set<Article> categoryFilteredArticles;
	public Map<Integer, Integer> mapRedirectTarget = new HashMap<Integer, Integer>(); 

	private RedirectSingelton()
	{
		int countR1=0;
		int countPage=0;
		Wikipedia wikipedia = WikipediaSingleton.getInstance().wikipedia;
		try {
			PageIterator pageIterator = wikipedia.getPageIterator();
			while (pageIterator.hasNext()) {
				Page page = pageIterator.next();
				if (page.getType().equals(PageType.article)) {
					Article article = wikipedia.getArticleByTitle(page.getTitle());
					if (article==null) {
						continue;
					}
					Redirect[] redirects = article.getRedirects();
					for (int i = 0; i < redirects.length; i++) {
//						Page p = new Page(WikipediaSingleton.getInstance().wikipedia.getEnvironment(), redirects[i].getId());
//						if (p.getType().equals(Page.PageType.redirect)) {
//							countR1++;
//						}
						Article rArticle = wikipedia.getArticleById(redirects[i].getId());
						if(rArticle!=null) {
							mapRedirectTarget.put(rArticle.getId(), article.getId());
						}
					}
				}
			}
			System.out.println("Size of the Redirect Map " + mapRedirectTarget.size());
			//System.out.println("We have " + countR1+" countR1");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static RedirectSingelton getInstance()
	{
		if (single_instance == null)
			single_instance = new RedirectSingelton();
		return single_instance;
	}
}
