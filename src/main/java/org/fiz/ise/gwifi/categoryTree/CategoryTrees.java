package org.fiz.ise.gwifi.categoryTree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.fiz.ise.gwifi.model.CategoryFromCategoryTree;

public class CategoryTrees {
	//List of category trees
	private static final Map<CategoryFromCategoryTree,Map<String,Integer>> trees = new TreeMap<>(); 
	private Set<String> categoriesAll = new HashSet<>();
	private static String line;
	public void load(String categoryTreeFolder) {
		int commonCategories=0;
		try {
			final File[] listOfFiles = new File(categoryTreeFolder).listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				final String file = listOfFiles[i].getName();
				System.out.println("Reading "+ file);
				final BufferedReader br = new BufferedReader(new FileReader(categoryTreeFolder+File.separator+file));
				final Map<String,Integer> map = new HashMap<>();
				while ((line = br.readLine()) != null) {
					final String[] split = line.split("\t");
					String category = split[0];
					String depth = split[1];
					map.put(category,Integer.parseInt(depth));
					if (categoriesAll.contains(category)) {
						//System.out.println(split[0]);
						commonCategories++;
					}
					categoriesAll.add(category);
				}
				//trees.put(CategoryFromCategoryTree.resolve(file), map);
				br.close();
			}
		} catch (Exception e) {
			System.out.println(line);
			e.printStackTrace();
		}
		System.out.println("Total size of the common categories "+commonCategories);
		System.out.println("Total size of the categories "+categoriesAll.size());
	}
	public Set<String> getCategoriesAll() {
		return categoriesAll;
	}
	public CategoryFromCategoryTree existInAnyTree(String query) {
		for(Entry<CategoryFromCategoryTree, Map<String, Integer>> entry:trees.entrySet()) {
			if(entry.getValue().containsKey(query)) {
				return entry.getKey();
			}
		}
		return null;
	}
}
