package org.fiz.ise.gwifi.dataset.shorttext.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.dataset.category.KBCorrespondingCategory;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.MapUtil;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class ClassifyBasedOnParentCategory {
	private final static Dataset TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	private static Set<Category> setMainCategories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);
	static final Logger resultLog = Logger.getLogger("reportsLogger");
//	public static void main(String[] args) {
//		String str = "\"E-mail scam targets police chief\",\"Wiltshire Police warns about \"\"phishing\"\" after its fraud squad chief was targeted.";
//		getBestMatchingCategory(str);
//	}
	public static Category getBestMatchingCategory(String shortText, List<Category> gtList) {
		//shortText="UPDATE 4-New York #39;s Spitzer charges Universal Life with fraud New York Attorney General Eliot Spitzer on Friday filed suit against Universal Life Resources (ULR), charging the life and disability insurance broker with taking fraudulent kick-backs for steering business to certain insurers ";
		Map<Category, Integer> mapScore = new HashMap<>(); 
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		try {
			List<Annotation> lstAnnotations = new ArrayList<>();
			service.annotate(shortText, lstAnnotations);
			Integer score = 0; 
			for (Category mainCat : setMainCategories) {
				//if (!mainCat.getTitle().equals("Business")) {
					for(Annotation a:lstAnnotations) {
						Category findParentCat = findParentCat(a, mainCat);
						if (findParentCat!=null) {
							score+=1;
						}
					//}
					mapScore.put(mainCat, score);
				}
			}
				Map<Category, Integer>  sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
				Category firstElement = MapUtil.getFirst(sortedMap).getKey();

				if (!gtList.contains(firstElement)) {
				//	resultLog.info(shortText+"\npredicted:"+firstElement+" gt:"+gtList.get(0));
				}
				return firstElement;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
	}
	public static Category findParentCat(Annotation a, Category c) {
		Set<Category> setMainCatAndSubCats = new HashMap<>( CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).mapMainCatAndSubCats).get(c);

//		if (c.getTitle().equals(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Trade").getTitle())) {
//			Category cBusiness= WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Business");
//			Set<Category> setBusiness = new HashMap<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).mapMainCatAndSubCats).get(cBusiness);
////			System.out.println("count "+count+" BusinessSize:" + setBusiness.size()+" MainSize: "+setMainCatAndSubCats.size());
//			setMainCatAndSubCats.addAll(setBusiness);
//			//System.out.println("count "+count+" BusinessSize:" + setBusiness.size()+" MainSize: "+setMainCatAndSubCats.size());
//		}
		if (WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())!=null) {
			List<Category> catOfEntity = new ArrayList<>(Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId()).getParentCategories()));
			for(Category cat: catOfEntity ) {
				if (setMainCatAndSubCats.contains(cat)) {
					//System.out.println(cat+"-->"+c);
					return cat;
				}
			}
		}
		return null;
	}
}