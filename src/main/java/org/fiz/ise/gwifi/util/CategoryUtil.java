package org.fiz.ise.gwifi.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import edu.kit.aifb.gwifi.model.Category;

public class CategoryUtil {
	public static  Set<Category> generateCategoryTree(Category c, int depth) {
		Set<Category> result = new HashSet<>();
		Map<Category, Set<Category>> latest = new HashMap<>();
		for (int j = 0; j < depth; j++) {
			if (j == 0) {
				
					Set<Category> child = new HashSet<>(
							getChildCategoriesSet(new HashSet<>(Arrays.asList(c))));
					result.addAll(child);
					latest.put(c, child);
				
			} else {
				for (Entry<Category, Set<Category>> entry : latest.entrySet()) {
					Set<Category> parent = new HashSet<>(entry.getValue());
					Set<Category> child = new HashSet<>(getChildCategoriesSet(parent));
					latest.put(entry.getKey(), child);
					List<Category> lstoriginalSet = new ArrayList<>(parent);
					List<Category> lstchildSet = new ArrayList<>(child);
					lstoriginalSet.addAll(lstchildSet);
					parent.addAll(child);// with the previous elements
					result.addAll(parent);
				}
			}
		}
		return result;
	}
	
	private static Set<Category> getChildCategoriesSet(Set<Category> setParent) {
		Set<Category> child = new HashSet<>();
		for (Category category : setParent) {
			Set<Category> temp = new HashSet<>(Arrays.asList(category.getChildCategories()));
			child.addAll(temp);
		}
		return child;
	}
}
