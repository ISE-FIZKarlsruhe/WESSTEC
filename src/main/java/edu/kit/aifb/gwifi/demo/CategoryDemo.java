package edu.kit.aifb.gwifi.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.model.Page.PageType;

public class CategoryDemo {

	/**
	 * Provides a demo of functionality available to Categories
	 * 
	 */
	public static void main(String[] args) throws Exception {
		File databaseDirectory = new File("configs/wikipedia-template-en.xml");
		Wikipedia wikipedia = new Wikipedia(databaseDirectory, false);
		System.out.println("The Wikipedia environment has been initialized.");

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			System.out.println("\nEnter article title (or enter to quit): ");
			String title = in.readLine();

			if (title == null || title.equals(""))
				break;

			Category category = wikipedia.getCategoryByTitle(title);

			if (category == null) {
				System.out.println("Could not find exact category. Try again");
			} else {
				System.out.println("\n" + category);

				System.out.println(" - first sentence:");
				System.out.println("    - " + category.getSentenceMarkup(0));

				System.out.println(" - first paragraph:");
				System.out.println("    - " + category.getFirstParagraphMarkup());

				PageType type = category.getType();
				System.out.println("\n - page type");
				System.out.println("    - " + type);

				System.out.println("\n - parent categories:");
				for (Category c : category.getParentCategories())
					System.out.println("    - " + c);
				
				System.out.println("\n - child categories:");
				for (Category c : category.getChildCategories())
					System.out.println("    - " + c);

				System.out.println("\n -  articles that belong to this category:");
				for (Article a : category.getChildArticles()) 
					System.out.println("    - " + a);
				
			}
			System.out.println("");
		}

	}

}