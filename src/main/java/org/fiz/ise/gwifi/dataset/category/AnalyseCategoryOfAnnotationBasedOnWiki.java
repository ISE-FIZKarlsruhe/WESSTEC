package org.fiz.ise.gwifi.dataset.category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.ReadDataset;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.CategoryUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.Print;


import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;

public class AnalyseCategoryOfAnnotationBasedOnWiki {

	private final static Dataset TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	private final static Category catFindMatching = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Technology"); 
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	private final static Integer CATEGORY_DEPTH_FOR_FILTERING = Config.getInt("CATEGORY_DEPTH_FOR_FILTERING", 0);
	public static void main(String[] args) {
		Article at =WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("White House");
		System.out.println(at.getId()+" "+at.getTitle()+" "+Arrays.asList(at.getParentCategories()));
		
		AnalyseCategoryOfAnnotationBasedOnWiki test = new AnalyseCategoryOfAnnotationBasedOnWiki();
		List<String> lstCat = new ArrayList<>(Categories.getCategoryList(TEST_DATASET_TYPE));
		List<Category> lstDatasetCatList = new ArrayList<>();
		for(String c : lstCat) {
			lstDatasetCatList.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle(c));
		}
		System.out.println("Depth of the category "+CATEGORY_DEPTH_FOR_FILTERING);

		for(Category c : lstDatasetCatList ) {
//						Category cDataset=WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Technology");
			System.out.println(c.getTitle());
			Category cDataset=c;
			List<String> dataset = new ArrayList<>(ReadDataset.read_WEB_BasedOnCategory(cDataset.getTitle(),Config.getString("DATASET_TEST_WEB","")));
			test.analyseAnchorText(dataset);
			
			//			List<Annotation> lstAllAnnotation = new ArrayList<>(AnnonatationUtil.findAnnotationAll(dataset));
//			AnnonatationUtil.findFreqOfEntity(lstAllAnnotation,"AnnotationFrequency_"+TEST_DATASET_TYPE+"_"+c.getTitle());
//			System.out.println();
			
//			Map<Category, Integer> findBestMMatchingCategoryCIKM = new LinkedHashMap<>(test.findBestMMatchingCategoryCIKM(dataset,c));
//			//			for(Entry<Category,Integer> e : findBestMMatchingCategoryCIKM.entrySet()) {
//			//				findParentCat(e.getKey(),CATEGORY_DEPTH_FOR_FILTERING);
//			//			}
//
//			System.out.print("\n"+cDataset.getTitle()+"Text"+" ");
//			//test.countNumberOfLinkedCat(dataset, c);
//			//			test.canBeGeneralizedDataset(dataset, c, CATEGORY_DEPTH_FOR_FILTERING);
//			//test.canBeGeneralizedDataset(dataset, WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Business"), CATEGORY_DEPTH_FOR_FILTERING);
//			System.out.println();
//
//			//			test.canBeGeneralizedDataset(dataset,WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("World government"),3);
//			//test.canBeGeneralizedDataset(dataset,c,3);
//
//			//			List<Annotation> lstAllAnnotation = new ArrayList<>(AnnonatationUtil.findAnnotationAll(dataset)); 
//			//
//			//			Map<String, Integer> mapfindMostCommonCats = new LinkedHashMap<>(test.findMostCommonCatsBasedOnDepthwitMainCat(AnnonatationUtil.filterAnnotation(lstAllAnnotation)));
//			//			FileUtil.writeDataToFile(mapfindMostCommonCats, c.getTitle()+"_CommonSubCatsBasedOnDepthwithMainCat_"+Config.getInt("DEPTH_OF_CAT_TREE", 0));
//
		}
	}
	//	private static void findParentCat(Category key, Integer categoryDepthForFiltering) {
	//		List<Category> parents = 
	//		for (int i = 0; i < categoryDepthForFiltering; i++) {
	//			
	//		}
	//		
	//	}

	public void analyseAnchorText(List<String> dataset) {
		List<Annotation> lstAnnotations = new ArrayList<>(AnnonatationUtil.findAnnotationAll(dataset));
		int count=0;
		for(Annotation a : lstAnnotations) {
			 
//			if(a.getMention().getTerm().toLowerCase().trim().equals("enterprise")||a.getMention().getTerm().toLowerCase().trim().equals("enterprises")) {
//				
//				 System.out.println(a.getMention()+"\t"+a.getTitle()+"\t"+ ++count);
//			}
			
			if (a.getId()==13930) {//5043734
				System.out.println(a.getMention()+"\t"+a.getTitle()+"\t"+ ++count);
			}
		}
		System.out.println();
	}
	public void analyseSubCategories() {
		Map<Category, Set<Category>> mapMainCatAndSubCats = CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).mapMainCatAndSubCats;
		for(Entry<Category, Set<Category>> e: mapMainCatAndSubCats.entrySet()) {
			if (e.getKey().getTitle().equals("World")) {
				for(Entry<Category, Set<Category>> c: mapMainCatAndSubCats.entrySet()) {
					//					if (!c.getKey().getTitle().equals(e.getKey().getTitle())) {
					if (c.getKey().getTitle().equals("Business")) {
						Set<Category> set = new HashSet<>(e.getValue());
						set.retainAll(c.getValue());
						//				System.out.println("e: "+e.getKey()+" c: "+c.getKey()+" intersection "+set.size()+set);
						secondLOG.info("e: "+e.getKey()+" c: "+c.getKey()+" intersection "+set.size()+set);

					}
				}

			}
			secondLOG.info("\n");
			System.out.println("Writing");
		}
	}
	public Map<Category, Integer> findBestMMatchingCategoryCIKM(List<String> dataset,Category mainCat) {
		List<Annotation> lstAnnotations = new ArrayList<>(AnnonatationUtil.findAnnotationAll(dataset));
		List<Article> containsEntitiy = new ArrayList<>();
		for(Annotation a : lstAnnotations) {
			if (WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())!=null) {
				containsEntitiy.addAll(Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId()).getLinksIn()));
			}
		}
		System.out.println("Size of "+ lstAnnotations.size()+ "pages contains entities" + containsEntitiy.size());
		Map<Category, Integer> result = new HashMap<>();
		for(Article a : containsEntitiy) {
			List<Category> catsOfAnArticle = new ArrayList<>(Arrays.asList(a.getParentCategories()));
			for(Category c : catsOfAnArticle) {
				if (result.containsKey(c)) {
					result.put(c, result.get(c)+1);
				}
				else {
					result.put(c, 1);
				}

			}
		}
		Map<Category, Integer>  sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(result));
		System.out.println("Writing to scond log");
		FileUtil.writeDataToFile(sortedMap, mainCat.getTitle()+"_MostCommonCategoriesOfTheEntitiesAppear");
		//		for(Entry<Category, Integer> e : sortedMap.entrySet()) {
		//			secondLOG.info(e.getKey()+" "+e.getValue());
		//		}
		return sortedMap;
	}
	public void countNumberOfLinkedCat(List<String> dataset,Category c) {
		List<Annotation> lstAnnotations = new ArrayList<>(AnnonatationUtil.findAnnotationAll(dataset));
		int count=0;
		for(Annotation a : lstAnnotations) {
			if (AnnonatationUtil.hasALink(a, c)) {
				count++;
			}
		}

		System.out.println("Total annotation "+ lstAnnotations.size()+ "from "+c.getTitle()+" dataset "+ "linked: "+count);
	}
	public  Map<Category, Integer> countNumberOfSubCatstoMainCat(List<String> dataset,int depth) {
		Map<Category, Integer> result = new HashMap<>();
		Set<Category> mainCategory = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);
		try {
			for(Category mC : mainCategory) {
				for(String line : dataset)
					if (result.containsKey(mC)) {
						result.put(mC, result.get(mC)+AnnonatationUtil.countALinkToMainCat(line, mC, depth));
					}
					else {
						result.put(mC, AnnonatationUtil.countALinkToMainCat(line, mC, depth));
					}
			}
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		Print.printMap(result);
		return null;
	}
	public void canBeGeneralizedDataset(List<String> lst,Category c,int depth) {
		int count=0;
		for(String str: lst) {
			if(AnnonatationUtil.hasALink(str, c, depth)) {
				count++;
			}
		}
		System.out.println("Category: "+c.getTitle()+" "+count+"/"+lst.size());
		System.out.println();
	}

	public  Map<String, Integer> findMostCommonCatsBasedOnDepthwitMainCat(List<Annotation> lstText) {
		Map<String, Integer> result = new HashMap<>();
		Map<Category, Set<Category>> mapCategory = new HashMap<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).mapMainCatAndSubCats);
		Set<Category> allCats = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setAllCategories);
		try {
			for(Annotation a:lstText) {
				if (WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())!=null) {
					List<Category> cats = new ArrayList<>(Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId()).getParentCategories()));
					if (cats!=null) {
						for(Category c:cats) {
							if (allCats.contains(c)) {
								for(Entry <Category, Set<Category>> e: mapCategory.entrySet()) {
									if (e.getValue().contains(c)) {
										if (result.containsKey(e.getKey()+"->"+c)) {
											result.put(e.getKey()+"->"+c, (result.get(e.getKey()+"->"+c)+1));
										}
										else{
											result.put(e.getKey()+"->"+c, 1);
										}
									}

								}

							}
						}
					}
				}
			}
			Map<String, Integer>  sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(result));
			return sortedMap;
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
	public  Map<Category, Integer> findMostCommonCatsBasedOnDepth(List<Annotation> lstText) {
		Map<Category, Integer> result = new HashMap<>();
		Set<Category> allCats = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setAllCategories);
		try {
			for(Annotation a:lstText) {
				if (WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())!=null) {
					List<Category> cats = new ArrayList<>(Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId()).getParentCategories()));
					if (cats!=null) {
						for(Category c:cats) {
							if (allCats.contains(c)) {
								if (result.containsKey(c)) {
									result.put(c, (result.get(c)+1));
								}
								else{
									result.put(c, 1);
								}
							}
						}
					}
				}
			}
			Map<Category, Integer>  sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(result));
			return sortedMap;
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
}
