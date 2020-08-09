package org.fiz.ise.gwifi.Singleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fiz.ise.gwifi.categoryTree.CategorySeedLoaderFromMemory;
import org.fiz.ise.gwifi.categoryTree.CategorySeedloader;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.Print;

import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;
/*
 * The purpose of this class is to create a Category tree and a categorySet comes from all the merge of the cat
 * trees 
 *we will use the set and tree for filtering the articles sake of reducing the complexity
 *we will deal only with the pages under the categories we consider
 */
public class CategorySingleton {
	private static CategorySingleton single_instance = null;

	private final Integer DEPTH_OF_CAT_TREE = Config.getInt("DEPTH_OF_CAT_TREE", 0);
	public Map<String, Set<Category>> mapCategoryDept;
	public Map<Category, Set<Category>> mapMainCatAndSubCats;
	public Set<Category> setAllCategories;
	public Set<Category> setMainCategories;

	private CategorySingleton(List<String> categories) {
		mapMainCatAndSubCats = new HashMap<>();
		mapCategoryDept = new HashMap<>();
		setAllCategories = new HashSet<>();
		setMainCategories = new HashSet<>();

		Wikipedia wikipedia = WikipediaSingleton.getInstance().wikipedia;
		System.out.println("Depth of the category Tree is "+DEPTH_OF_CAT_TREE );
		Category[] mainCats = new Category[categories.size()];
		List<Category> mainCategories = new ArrayList<>();
		int id = 0;
		for (String category : categories) {
			Category cat = wikipedia.getCategoryByTitle(category);
			mainCats[id] = cat;
			if (cat==null) {
				System.out.println(category);
			}
			mainCategories.add(cat);
			setMainCategories.add(cat);
			setAllCategories.add(cat);
			id++;
		}
		//System.out.println(setAll);
		Map<Category, Set<Category>> latest = new HashMap<>();
		for (int j = 0; j < DEPTH_OF_CAT_TREE; j++) {
			if (j == 0) {
				for (int i = 0; i < mainCats.length; i++) {
					Set<Category> child = new HashSet<>(
							getChildCategoriesSet(new HashSet<>(Arrays.asList(mainCats[i]))));
					mapCategoryDept.put(mainCats[i].getTitle()+"\t"+j,child);
					mapMainCatAndSubCats.put(mainCats[i], child);
					setAllCategories.addAll(child);
					latest.put(mainCats[i], child);
				}
			} else {
				for (Entry<Category, Set<Category>> entry : latest.entrySet()) {
					Set<Category> parent = new HashSet<>(entry.getValue());
					Set<Category> child = new HashSet<>(getChildCategoriesSet(parent));
					latest.put(entry.getKey(), child);
					List<Category> lstoriginalSet = new ArrayList<>(parent);
					List<Category> lstchildSet = new ArrayList<>(child);
					lstoriginalSet.addAll(lstchildSet);

					mapCategoryDept.put(entry.getKey().getTitle()+"\t"+j,child);

					parent.addAll(child);// with the previous elements
					mapMainCatAndSubCats.put(entry.getKey(), parent);
					setAllCategories.addAll(parent);
				}
			}
		}
		filterCategories();

	}
	private void filterCategories() {
		List<Category> mainCats = new ArrayList<>(setMainCategories);
		Set<Category> intersectionAllRemove = new HashSet<>();
		
		for (int i = 0; i < mainCats.size(); i++) {
			Set<Category> setTemp = new HashSet<>(mapMainCatAndSubCats.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle(mainCats.get(i).getTitle())));
//			System.out.println(mainCats.get(i).getTitle()+" "+mapMainCatAndSubCats.get(mainCats.get(i)).size());
			for (int j = i+1; j < mainCats.size(); j++) {
				Set<Category> intersectionFiltered = new HashSet<>();
				//System.out.println(mainCats.get(j).getTitle()+" "+mapMainCatAndSubCats.get(mainCats.get(j)).size());
				for(Category cT : mapMainCatAndSubCats.get(mainCats.get(j))) {
					if (!setTemp.contains(cT)) {
						intersectionFiltered.add(cT);
					}
					else {
						intersectionAllRemove.add(cT);
					}
				}
				//System.out.println(mainCats.get(j).getTitle()+" "+intersectionFiltered.size());
				mapMainCatAndSubCats.put(mainCats.get(j), intersectionFiltered);
			}
			Set<Category> oTemp = new HashSet<>(mapMainCatAndSubCats.get(mainCats.get(i)));
			Set<Category> intersectionFilterO = new HashSet<>();
			for(Category c : oTemp) {
				if (!intersectionAllRemove.contains(c)) {
					intersectionFilterO.add(c);
				}
			}
		//	System.out.println(mainCats.get(i).getTitle()+" "+intersectionFilterO.size());
			mapMainCatAndSubCats.put(mainCats.get(i), intersectionFilterO);
		}
		///////////////////////////////////////////////////////////////////////DELETE/////////////////////////////////
//		Set<Category> tech = new HashSet<>(mapMainCatAndSubCats.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Technology")));
//		Set<Category> sci = new HashSet<>(mapMainCatAndSubCats.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science")));
//		
//		tech.addAll(sci);
//		mapMainCatAndSubCats.put(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Technology"), tech);
//		System.out.println("New tech size "+mapMainCatAndSubCats.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Technology")).size());
//		
//		for(Entry<String,Set<Category>> e : mapCategoryDept.entrySet()) {
//			
//			if (e.getKey().contains("Technology")) {
//				String depth= e.getKey().split("\t")[1];
//				Set<Category> setSci = new HashSet<>(mapCategoryDept.get("Science\t"+depth));
//				Set<Category> setTech = new HashSet<>(e.getValue());
//				setTech.addAll(setSci);
//				mapCategoryDept.put(e.getKey(), setTech);
//			}
//		}
	}
	private Set<Category> getChildCategoriesSet(Set<Category> setParent) {
		Set<Category> child = new HashSet<>();
		for (Category category : setParent) {
			Set<Category> temp = new HashSet<>(Arrays.asList(category.getChildCategories()));
			child.addAll(temp);
		}
		return child;
	}
	public static CategorySingleton getInstance(List<String> categories) {
		if (single_instance == null)
			single_instance = new CategorySingleton(new ArrayList<>(categories));
		return single_instance;
	}
}
