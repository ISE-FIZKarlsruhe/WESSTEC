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
import org.fiz.ise.gwifi.util.Config;

import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;
/*
 * The purpose of this class is to create a Category tree and a categorySet comes from all the merge of the cat
 * trees 
 *we will use the set and tree for filtering the articles sake of reducing the complexity
 *we will deal only with the pages under the categories we consider
 */
public class CategorySingletonAnnotationFiltering {
	private static CategorySingletonAnnotationFiltering single_instance = null;

	private final Integer CATEGORY_DEPTH_FOR_FILTERING = Config.getInt("CATEGORY_DEPTH_FOR_FILTERING", 0);
	public Map<String, Set<Category>> mapCategoryDept;
	public Map<Category, Set<Category>> mapMainCatAndSubCats;
	public Set<Category> setAllCategories;
	public Set<Category> setMainCategories;

	private CategorySingletonAnnotationFiltering(List<String> categories) {
		mapMainCatAndSubCats = new HashMap<>();
		mapCategoryDept = new HashMap<>();
		setAllCategories = new HashSet<>();
		setMainCategories = new HashSet<>();

		Wikipedia wikipedia = WikipediaSingleton.getInstance().wikipedia;
		System.out.println("Depth of the category Tree for annotation Filtering is "+CATEGORY_DEPTH_FOR_FILTERING );
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
		for (int j = 0; j < CATEGORY_DEPTH_FOR_FILTERING; j++) {
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

	}

	private Set<Category> getChildCategoriesSet(Set<Category> setParent) {
		Set<Category> child = new HashSet<>();
		for (Category category : setParent) {
			Set<Category> temp = new HashSet<>(Arrays.asList(category.getChildCategories()));
			child.addAll(temp);
		}
		return child;
	}
	public static CategorySingletonAnnotationFiltering getInstance(List<String> categories) {
		if (single_instance == null)
			single_instance = new CategorySingletonAnnotationFiltering(new ArrayList<>(categories));
		return single_instance;
	}
}
