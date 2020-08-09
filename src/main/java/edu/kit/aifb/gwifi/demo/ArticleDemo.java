package edu.kit.aifb.gwifi.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import edu.kit.aifb.gwifi.comparison.ArticleComparer;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.model.Redirect;
import edu.kit.aifb.gwifi.model.Wikipedia;

public class ArticleDemo {

	/**
	 * Provides a demo of functionality available to Articles
	 * 
	 */
	public static void main(String[] args) throws Exception {
		File databaseDirectory = new File("configs/wikipedia-template-en.xml");
		Wikipedia wikipedia = new Wikipedia(databaseDirectory, false);
		ArticleComparer comparer = new ArticleComparer(wikipedia);
		System.out.println("The Wikipedia environment has been initialized.");

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		DecimalFormat df = new DecimalFormat("0");

		while (true) {
			System.out.println("\nEnter article title or id (or enter to quit): ");
			String[] input = in.readLine().split(":");
			Article article = null;

			if (input.length == 2 && input[0].equals("id")) {
				int id = Integer.valueOf(input[1]);
				article = wikipedia.getArticleById(id);
			} else {
				String title = null;
				if (input.length == 1) {
					title = input[0];
				}
				if (input.length == 2 && input[0].equals("title")) {
					title = input[1];
				}
				if (title == null || title.equals(""))
					break;
				article = wikipedia.getArticleByTitle(title);

				if (article == null) {
					System.out.println("Could not find exact match. Searching through anchors instead");
					article = wikipedia.getMostLikelyArticle(title, null);
				}
			}

			if (article == null) {
				System.out.println("Could not find exact article. Try again");
			} else {
				System.out.println("\n" + article.getId() + " : " + article.getTitle());

				System.out.println(" - first sentence:");
				System.out.println("    - " + article.getSentenceMarkup(0));

				System.out.println(" - first paragraph:");
				System.out.println("    - " + article.getFirstParagraphMarkup());

				PageType type = article.getType();
				System.out.println("\n - page type");
				System.out.println("    - " + type);

//				String markup = article.getMarkup();
//				System.out.println("\n - markup");
//				System.out.println("    - " + markup);

//				String plainText = article.getPlainText();
//				System.out.println("\n - plainText");
//				System.out.println(" - " + plainText);

//				System.out.println("\n - redirects:");
//				for(Redirect redirect : article.getRedirects())
//					System.out.println("    - " + redirect.getTitle());
				
//				System.out.println("\n - parent categories (hypernyms):");
//				for (Category c : article.getParentCategories())
//					System.out.println("    - " + c);

//				System.out.println("\n - language links (translations):");
//				TreeMap<String, String> translations = article.getTranslations();
//				for (String lang : translations.keySet())
//					System.out.println(" - \"" + translations.get(lang) + "\" (" + lang + ")");

//				System.out.println(
//						"\n - redirects (synonyms or very small related topics that didn't deserve a seperate article):");
//				for (Redirect r : article.getRedirects())
//					System.out.println(" - " + r);
				
//				System.out.println("\n - anchors (synonyms and hypernyms):");
//				for (Article.Label at : article.getLabels())
//					System.out.println(" - \"" + at.getText() + "\" (used " + at.getLinkOccCount() + " times)");

				HashMap<String, Double> map = new HashMap<String, Double>();
				
				System.out.println("\n - pages that this links to (related concepts):");
				for (Article a : article.getLinksOut()) {
					double relatedness = comparer.getRelatedness(article, a);
					System.out.println(" - " + a + " (" + df.format(relatedness * 100) + "% related)");
					map.put(a.getTitle(), relatedness);
//					for (int index : article.getSentenceIndexesMentioning(a)) {
//						System.out.println(" - " + article.getSentenceMarkup(index));
//					}
				}

//				for (Article a : article.getLinksIn()) {
//					double relatedness = comparer.getRelatedness(article, a);
//					System.out.println(" - " + a + " (" + df.format(relatedness * 100) + "% related)");
//					map.put(a.getTitle(), relatedness);
////					for (int index : article.getSentenceIndexesMentioning(a)) {
////						System.out.println(" - " + article.getSentenceMarkup(index));
////					}
//				}
				
				Map<String, Double> sortedMap = sortByValue(map);
				int i = 0;
				for (String title : sortedMap.keySet()) {
					// if(i++>100)
					// break;
					double relatedness = sortedMap.get(title);
					System.out.println(" - " + title + " (" + df.format(relatedness * 100) + "% related)");
				}
			}
		}

	}

	public static Map<String, Double> sortByValue(Map<String, Double> map) {
		List<Map.Entry<String, Double>> list = new LinkedList<>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
			public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});

		Map<String, Double> result = new LinkedHashMap<>();
		for (Map.Entry<String, Double> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByGeneralValue(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	// public static void main(String args[]) throws Exception {
	// File file = new File("configs/wikipedia-template.xml");
	// Wikipedia wikipedia = new Wikipedia(file, false);
	// System.out.println("The Wikipedia environment has been initialized.");
	//
	// Label label = wikipedia.getLabel("Dog", null);
	// System.out.println("Senses for Dog:");
	// for (Label.Sense sense : label.getSenses())
	// System.out.println(" - " + sense.getTitle());
	//
	// Article artDog = label.getSenses()[0];
	// System.out.println(artDog.getSentenceMarkup(0));
	//
	// System.out.println("Synonyms: ");
	// for (Article.Label synDog : artDog.getLabels())
	// System.out.println(" - " + synDog.getText());
	//
	// System.out.println("Translations: ");
	// TreeMap<String, String> trans = artDog.getTranslations();
	// for (String e : trans.keySet())
	// System.out.println(" - " + trans.get(e) + " (" + e + ")");
	//
	// Article[] relatedTopics = artDog.getLinksOut();
	// ArticleComparer comparer = new ArticleComparer(wikipedia);
	// for (Article rt : relatedTopics)
	// rt.setWeight(comparer.getRelatedness(artDog, rt));
	// Arrays.sort(relatedTopics);
	//
	// System.out.println("Related Topics: ");
	// for (Article rt : relatedTopics)
	// System.out.println(" - " + rt.getTitle());
	//
	// }

	// public static void main(String[] args) throws Exception {
	//
	// File databaseDirectory = new File("configs/wikipedia-template.xml");
	// Wikipedia wikipedia = new Wikipedia(databaseDirectory, false);
	//
	// Article nzBirds = wikipedia.getMostLikelyArticle("Birds of New Zealand",
	// null);
	// Article kiwi = wikipedia.getMostLikelyArticle("Kiwi", null);
	// System.out.println(kiwi);
	//
	// DbLinkLocationList ll =
	// wikipedia.getEnvironment().getDbPageLinkOut().retrieve(kiwi.getId());
	// for (DbLinkLocation l : ll.getLinkLocations()) {
	// System.out.print(" - " + l.getLinkId() + ":");
	// for (Integer s : l.getSentenceIndexes())
	// System.out.println(" " + s);
	// }
	//
	// System.out.println();
	//
	// Article nz = wikipedia.getMostLikelyArticle("New Zealand", null);
	// for (Article art : kiwi.getLinksOut()) {
	// if (art.equals(nz))
	// System.out.println(" - link: " + art);
	// }
	//
	// for (Article art : nz.getLinksIn()) {
	// if (art.equals(kiwi))
	// System.out.println(" - link in: " + art);
	// }
	//
	// System.out.println(nzBirds.getMarkup());
	// System.out.println();
	//
	// ArrayList<Article> arts = new ArrayList<Article>();
	// arts.add(wikipedia.getMostLikelyArticle("Kiwi", null));
	// arts.add(wikipedia.getMostLikelyArticle("Takahe", null));
	//
	// for (Article art : arts) {
	// System.out.println("retrieving sentences mentioning " + art);
	// for (int si : nzBirds.getSentenceIndexesMentioning(art)) {
	// System.out.println(nzBirds.getSentenceMarkup(si));
	// System.out.println();
	// }
	// }
	//
	// System.out.println("retrieving sentences mentioning all");
	//
	// for (int si : nzBirds.getSentenceIndexesMentioning(arts)) {
	// System.out.println(nzBirds.getSentenceMarkup(si));
	// System.out.println();
	// }
	//
	// }

}