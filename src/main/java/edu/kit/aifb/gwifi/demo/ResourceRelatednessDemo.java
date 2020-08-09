package edu.kit.aifb.gwifi.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import edu.kit.aifb.gwifi.comparison.ArticleComparer;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Wikipedia;

public class ResourceRelatednessDemo {

	public static String SOURCE_PAGE_ID_FIELD = "s_id";
	public static String TARGET_PAGE_ID_FIELD = "t_id";
	public static String SCORE_FIELD = "score";

	// "configs/wikipedia-template-en.xml"
	public static void main(String[] args) throws Exception {
		File databaseDirectory = new File("configs/wikipedia-template-en.xml");
		Wikipedia wikipedia = new Wikipedia(databaseDirectory, false);
		ArticleComparer comparer = new ArticleComparer(wikipedia);
		System.out.println("The Wikipedia environment has been initialized.");

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			System.out.println("\nEnter article titles (or enter to quit): ");
			String line = in.readLine();
			
			if(line.equals("exit"))
				break;
			
			int startIdx1 = line.indexOf("\"");
			int endIdx1 = line.indexOf("\"", startIdx1 + 1);
			int startIdx2 = line.indexOf("\"", endIdx1 + 1);
			int endIdx2 = line.indexOf("\"", startIdx2 + 1);
			String title1 = line.substring(startIdx1+1, endIdx1);
			String title2 = line.substring(startIdx2+1, endIdx2);
			System.out.println(title1 +" "+ title2);
//			String title1 = "United States"; 
//			String title2 = "Ultranet";
					
			if (title1 == null || title1.equals("") || title2 == null || title2.equals(""))
				continue;

			Article article1 = wikipedia.getArticleByTitle(title1);
			Article article2 = wikipedia.getArticleByTitle(title2);

			if (article1 == null) {
				System.out.println("Could not find exact match for \"" + title1
						+ "\". Searching through anchors instead");
				article1 = wikipedia.getMostLikelyArticle(title1, null);
			}

			if (article2 == null) {
				System.out.println("Could not find exact match for \"" + title2
						+ "\". Searching through anchors instead");
				article2 = wikipedia.getMostLikelyArticle(title2, null);
			}

			if (article1 == null || article2 == null) {
				System.out.println("Could not find exact article. Try again");
			} else {
				double relatedness = comparer.getRelatedness(article1, article2);
				System.out.println("\nThe relatedness between \"" + article1 + "\" and \"" + article2 + "\" is: "
						+ relatedness);
			}
		}

	}

}
