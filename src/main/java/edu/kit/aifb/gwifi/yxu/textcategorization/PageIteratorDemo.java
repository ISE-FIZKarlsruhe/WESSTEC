package edu.kit.aifb.gwifi.yxu.textcategorization;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.util.PageIterator;

public class PageIteratorDemo {

	public static void main(String[] args) throws Exception {
		File databaseDirectory = new File("configs/wikipedia-template-en.xml");
		Wikipedia wikipedia = new Wikipedia(databaseDirectory, false);
		System.out.println("The Wikipedia environment has been initialized.");

		PageIterator pageIterator = wikipedia.getPageIterator();
		int i = 0;
		int numArticle = 0;
		int numCategory = 0;
		while (pageIterator.hasNext()) {
			if(++i%100000 == 0) {
				System.out.println(i + " pages have been processed!");
			}
			Page page = pageIterator.next();
//			if (!page.getType().equals(PageType.article)) {
//				System.out.println("title: " + page.getTitle());
//				System.out.println("type: " + page.getType());
//			}	
			if (page.getType().equals(PageType.article)) {
				numArticle++;
			}	
			if (page.getType().equals(PageType.category)) {
				numCategory++;
			}	
		}	
		
		System.out.println("the number of articles: " + numArticle);
		System.out.println("the number of categories: " + numCategory);
		System.out.println("the number of pages: " + i);
		
//		PageIterator pageIterator = wikipedia.getPageIterator(PageType.article);
//		int i = 0;
//		int j = 0;
//		while (pageIterator.hasNext()) {
//			if (++i % 100000 == 0) {
//				System.out.println(i + " pages have been processed!");
//			}
//			Article article = (Article) pageIterator.next();
//			// if(page.getTitle().equals("CBD")) {
//			if (article.getDepth() == null) {
//				String markup = article.getMarkup();
//				if (markup == null)
//					continue;
//				Matcher dis_m = Pattern.compile("((?<=\\u007B\\u007B)disambig\\|.*?(?=\\u007D\\u007D))").matcher(markup);
//				if(dis_m.find()) {
//					System.out.println("title: " + article.getTitle());
//					j++;
//				}	
//			}
//		}
//		System.out.println(j);
		
	}

}
