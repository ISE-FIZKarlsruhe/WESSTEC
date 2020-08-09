package edu.kit.aifb.gwifi.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import edu.kit.aifb.gwifi.comparison.ArticleComparer;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.model.Wikipedia;

public class ExtractArticleCategoryDemo {

	/**
	 * Provides a demo of functionality available to Articles
	 * 
	 */
	public static void main(String[] args) throws Exception {
		File databaseDirectory = new File("configs/wikipedia-template-en.xml");
		Wikipedia wikipedia = new Wikipedia(databaseDirectory, false);
		System.out.println("The Wikipedia environment has been initialized.");

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

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

				PageType type = article.getType();
				System.out.println("\n - page type");
				System.out.println("    - " + type);

				System.out.println("\n - parent categories (hypernyms):");
				Map<Integer, Set<Category>> dep2categories = new TreeMap<Integer, Set<Category>>(); 
				for (Category parent : article.getParentCategories()) {
					Integer dep = parent.getDepth() == null ? 0 : parent.getDepth();
					Set<Category> parents = dep2categories.get(dep);
					if(parents == null) {
						parents = new HashSet<Category>();
						dep2categories.put(dep, parents);
					}
					parents.add(parent);
					Set<Category> allAncestors = getAncestors(parent, new HashSet<Category>());
					for(Category ancestor : allAncestors) {
						dep = ancestor.getDepth() == null ? 0 : ancestor.getDepth();
						Set<Category> ancestors = dep2categories.get(dep);
						if(ancestors == null) {
							ancestors = new HashSet<Category>();
							dep2categories.put(dep, ancestors);
						}
						ancestors.add(ancestor);
					}
				}	
				
				for(int dep : dep2categories.keySet()) {
					System.out.println("Depth: " + dep);
					for(Category cate : dep2categories.get(dep)) {
						System.out.println("\t" + cate);
					}
				} 
				
			}
		}

	}

	public static Set<Category> getAncestors(Category cate, Set<Category> dealed) {
		Set<Category> ancestors = new HashSet<Category>();
		Category[] parents =  cate.getParentCategories();
		for(Category parent : parents) {
			ancestors.add(parent);
			if(!dealed.contains(parent)) {
				dealed.add(parent);
				Set<Category> cates = getAncestors(parent, dealed);
				ancestors.addAll(cates);
			}
		}
		
		return ancestors;
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

}