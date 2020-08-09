package edu.kit.aifb.gwifi.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.kit.aifb.gwifi.comparison.ArticleComparer;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.model.Redirect;
import edu.kit.aifb.gwifi.model.Wikipedia;

public class ArticleReferenceDemo {

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

				Map<String, Integer> labelMap = new HashMap<String, Integer>();
				List<String> sentences = new ArrayList<String>();
				for (Article a : article.getLinksIn()) {
					for (int j : a.getSentenceIndexesMentioning(article)) {
						String sentence = a.getSentenceMarkup(j);
						sentences.add(sentence);
						
						Matcher m = Pattern.compile("((?<=\\u005B\\u005B).*?(?=\\u005D\\u005D))").matcher(sentence);
						while (m.find()) {
							String label = m.group();
							if (label.contains("|")) {
								label = label.substring(label.indexOf("|") + 1, label.length());
							}
							if (labelMap.containsKey(label)) {
								int frequency = labelMap.get(label);
								frequency++;
								labelMap.put(label, frequency);
							} else {
								labelMap.put(label, 1);
							}
						}
					}
				}
				
				for(String sentence : sentences) {
					System.out.println(sentence);
				}
				
				System.out.println();
				
				Map<String, Integer> sortedMap = sortByGeneralValue(labelMap);
				for(Entry<String, Integer> entry : sortedMap.entrySet()) {
					System.out.println(entry.getKey() + ": " + entry.getValue());
				}
			}
		}

	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByGeneralValue(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
	
}