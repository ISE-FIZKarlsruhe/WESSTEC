
package org.fiz.ise.gwifi.dataset.shorttext.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.SynchronizedCounter;

import edu.kit.aifb.gwifi.db.WDatabase.DatabaseType;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;

public class Demo {
	private static boolean LOAD_MODEL = Config.getBoolean("LOAD_MODEL", false);
	public static void main(String[] args) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		if (LOAD_MODEL) {
			LINE_modelSingleton.getInstance();
		}
		CategorySingleton singCategory= CategorySingleton.getInstance(Categories.getCategoryList(Dataset.AG));
		Map<Category, Set<Category>> mapTemp = new HashMap<>(singCategory.mapMainCatAndSubCats);
		for(Entry<Category, Set<Category>> e: mapTemp.entrySet())
		{
			Category main = e.getKey();
			Set<Category> temp = new HashSet<>();
			for(Category c: e.getValue() ) {
				if (c.getChildArticles().length>0) {
					temp.add(c);
				}
			}
			mapTemp.put(main, temp);
		}
		Map<Category, Set<Category>> mapCategories= new HashMap<>(mapTemp);
		while (true) {
			try {
				System.out.println("Enter your text");
				String input = in.readLine();
				if (input.equals(null)) {
					continue;
				}
				Category bestMatchingCategory = HeuristicApproach.getBestMatchingCategory(input,null);
				System.out.println("predicted: "+bestMatchingCategory.getTitle());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
