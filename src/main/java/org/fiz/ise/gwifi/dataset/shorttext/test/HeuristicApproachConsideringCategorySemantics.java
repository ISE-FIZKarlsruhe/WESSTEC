package org.fiz.ise.gwifi.dataset.shorttext.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.ejml.equation.IntegerSequence.For;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.dataset.category.KBCorrespondingCategory;
import org.fiz.ise.gwifi.model.EmbeddingModel;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.Request_LINEServer;

import com.hp.hpl.jena.reasoner.rulesys.builtins.Print;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.annotation.detection.NLPTopicDetector;
import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class HeuristicApproachConsideringCategorySemantics {

	private final static Dataset TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	private static Set<Category> setMainCategories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);
	private final static Integer DEPTH_OF_CAT_TREE = Config.getInt("DEPTH_OF_CAT_TREE", 0);
	private static Map<Category, Set<Category>> mapCategories=new HashMap<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).mapMainCatAndSubCats);
	private static final Logger LOG = Logger.getLogger(HeuristicApproachConsideringCategorySemantics.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	/*
	 * The main purpose of this class is the calculate the similarity and decide 
	 * which category a text falls to to based on the probability
	 * 
	 */
	public static Category getBestMatchingCategory(String shortText,List<Category> gtList) {
		List<Integer> blackList = new ArrayList<>(AnnonatationUtil.getEntityBlackList_AGNews());
		
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		HeuristicApproachConsideringCategorySemantics heuristic = new HeuristicApproachConsideringCategorySemantics();
		StringBuilder mainBuilder = new StringBuilder();
		try {
			Map<Category, Double> mapScore = new HashMap<>(); 
			List<Annotation> lstAnnotations = new ArrayList<>();
			service.annotate(shortText, lstAnnotations);
			Map<Integer, Map<Integer, Double>> contextSimilarity = new HashMap<>(calculateContextEntitySimilarities(lstAnnotations));
			for (Category mainCat : setMainCategories) {
				secondLOG.info(shortText+" "+mainCat.getTitle()+"\n");
				double score = 0.0; 
				for(Annotation a:lstAnnotations) {
					if (!blackList.contains(a.getId())) {
						score+=heuristic.calculateScoreBasedInitialFormula(a, mainCat, contextSimilarity);
					}
				}
				secondLOG.info("\nTotal, "+mainCat.getTitle()+" "+score+"\n");
				mapScore.put(mainCat, score);
			}
			mainBuilder.append("\n");
			Map<Category, Double>  sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			Category firstElement = MapUtil.getFirst(sortedMap).getKey();

			for(Entry<Category, Double> e: sortedMap.entrySet()){			
				mainBuilder.append(e.getKey()+" "+e.getValue()+"\n");
			}
			if (!gtList.contains(firstElement)) {
				secondLOG.info(mainBuilder.toString());
			}
			return firstElement;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	private static void findCandidates(String shortText) {
		System.out.println(shortText);
		Map<String, Page> m = new HashMap<>();
		for(Topic t: AnnotationSingleton.getInstance().getCandidates(shortText) ) {
			System.out.println(t.getReferences().get(0).getLabel()+" "+t.getDisplayName()+" "+t.getWeight());
		}
	}

	private double calculateScoreBasedInitialFormula(Annotation a, Category mainCat,Map<Integer, Map<Integer, Double>> contextSimilarity) {
		double P_e_c=get_P_e_c(a.getId(), mainCat);
		double P_Se_c=get_P_Se_c(a);
		double P_Ce_e=1;
		if (contextSimilarity.size()>1) {
			P_Ce_e=get_P_Ce_e_efficient(a.getId(),contextSimilarity);
		}
		return (P_e_c*P_Se_c*P_Ce_e);
	}
	private static Map<Integer, Map<Integer, Double>>  calculateContextEntitySimilarities(List<Annotation> annotations) {
		Map<Integer, Map<Integer, Double>> mapContextSimilarity = new HashMap<>();
		for(Annotation a: annotations){
			Map<Integer, Double> temp = new HashMap<>();
			for(Annotation c: annotations){
				double similarity=.0;
				similarity=(LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(a.getId()), String.valueOf(c.getId())));
				temp.put(c.getId(), similarity);
			}
			mapContextSimilarity.put(a.getId(), temp);
		}
		return mapContextSimilarity;
	}
//	private static double get_P_e_c(int articleID,Category mainCat) {
//		Set<Category> childCategories;
//		double result =0.0;
//		double countNonZero=0.0;
//		int countNan=0;
//		childCategories = new HashSet<>(mapCategories.get(mainCat));
//		Set<Category> finalSet = new HashSet<>();
//		if (mainCat.getTitle().equals("World")) {	
//			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Internal affairs ministries"));
//			//finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Ethically disputed working conditions"));
//			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Legal concepts"));
//			//finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Chambers of commerce"));
//			childCategories = new HashSet<>(finalSet);
//
//		}	
//		else if (mainCat.getTitle().equals("Business")) {	
//			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Finance lists"));
//			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Food industry trade groups"));
//			//finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Commodity exchanges"));
//			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Intangible assets"));
//			//finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Grants (money)"));
//			
//			childCategories = new HashSet<>(finalSet);
//		}	
//		else if (mainCat.getTitle().equals("Sports")) {	
//			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Olympic tennis players of Indonesia"));
//			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("2011 in African basketball"));
//			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Orthopedic braces"));
//			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Northwestern Wildcats men's basketball seasons"));
//			childCategories = new HashSet<>(finalSet);
//		}	
//		else if (mainCat.getTitle().equals("Science and technology")) {	
//			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Data centers"));
//			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Security engineering"));
//			//finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Advertising techniques"));
//			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Email spammers"));
//			childCategories = new HashSet<>(finalSet);
//		}
//
//		if (WikipediaSingleton.getInstance().wikipedia.getArticleById(articleID)==null) {
//			return 0;
//		}
//
//		//			Set<Category> childCatOfArticle = new HashSet<>(Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getArticleById(articleID).getParentCategories()));
//		//			for(Category cA:childCatOfArticle) {
//		//				P_e_Cr =LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(cA.getId()), String.valueOf(c.getId()));
//		//				//secondLOG.info(c.getTitle()+" "+cA.getTitle()+" "+P_e_Cr);
//		//				System.out.println(c.getTitle()+" "+cA.getTitle()+" "+P_e_Cr);
//		//				if (!Double.isNaN(P_e_Cr)&&P_e_Cr>0.0) {
//		//					result+=P_e_Cr;
//		//					countNonZero++;
//		//				}
//		//				else{
//		//					LOG.info("similarity could not be calculated category: "+c.getTitle()+" "+c.getChildArticles().length);
//		//				}
//		//			}
//		//		}
//
//		Map<String,Double> map = new HashMap<>();
//		for(Category c:childCategories) {
//			double P_e_Cr =0.0;
//			Set<Category> childCatOfArticle = new HashSet<>(Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getArticleById(articleID).getParentCategories()));
//			for(Category cA:childCatOfArticle) {
//				P_e_Cr =LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(cA.getId()), String.valueOf(c.getId()));
//				if (!Double.isNaN(P_e_Cr)) {
//					map.put(WikipediaSingleton.getInstance().wikipedia.getArticleById(articleID).getTitle()+", "+c.getTitle(), P_e_Cr);
//					result+=P_e_Cr;
//					countNonZero++;
//				}
//				else{
//					countNan++;
//					LOG.info("similarity could not be calculated category: "+c.getTitle()+" "+c.getChildArticles().length);
//				}
//			}
//		}
//		if (countNonZero==0) {
//			return 0.0;
//		}
//		return result/countNonZero;
//	}
		private static double get_P_e_c(int articleID,Category mainCat) {
			Set<Category> childCategories;
			double result =0.0;
			double countNonZero=0.0;
			int countNan=0;
			childCategories = new HashSet<>(mapCategories.get(mainCat));
	
			Set<Category> finalSet = new HashSet<>();
			
			if (mainCat.getTitle().equals("World")) {	
				finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("International law"));
				finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Social movements"));
				finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Racism"));
				finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Social movements"));
				childCategories = new HashSet<>(finalSet);
				
			}	
			else if (mainCat.getTitle().equals("Business")) {	
				finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Urban planning"));
				finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Retailing"));
				finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Employment classifications"));
				finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Finance fraud"));
				childCategories = new HashSet<>(finalSet);
//				childCategories = new HashSet<>(Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Trade").getChildCategories()));
			}	
			else if (mainCat.getTitle().equals("Sports")) {	
				finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Light-heavyweight boxers"));
				finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("American male long-distance runners"));
//				finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("FINA World Junior Synchronised Swimming Championships"));
				childCategories = new HashSet<>(finalSet);
			}	
			else if (mainCat.getTitle().equals("Science and technology")) {	
				finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Free software programmed in Pascal"));
				finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("E-commerce"));
				//Technology in society
//				finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Anti-competitive behaviour"));
//				finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Auditing"));
				childCategories = new HashSet<>(finalSet);
			}
			
			if (WikipediaSingleton.getInstance().wikipedia.getArticleById(articleID)==null) {
				return 0;
			}
			Map<String,Double> map = new HashMap<>();
			for(Category c:childCategories) {
				double P_Cr_c=0.0;
				double P_e_Cr =0.0;
				P_Cr_c =1;
				P_e_Cr =LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(articleID), String.valueOf(c.getId()));
				double temp =P_e_Cr*P_Cr_c;
				if (!Double.isNaN(temp)) {
					map.put(WikipediaSingleton.getInstance().wikipedia.getArticleById(articleID).getTitle()+", "+c.getTitle(), P_e_Cr);
					result+=temp;
					countNonZero++;
				}
				else{
					countNan++;
					LOG.info("similarity could not be calculated category: "+c.getTitle()+" "+c.getChildArticles().length);
				}
			}
	
			if (countNonZero==0) {
				return 0.0;
			}
			Map<String, Double>  sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(map));
			//sortedMap.forEach((k,v) -> System.out.println("key: "+k+" value:"+v));
			//		if (mainCat.getTitle().equals("Trade") || mainCat.getTitle().equals("World")) {
			//System.out.println(MapUtil.getFirst(sortedMap).getKey()+" "+MapUtil.getFirst(sortedMap).getValue());
			//secondLOG.info(mainCat.getTitle() +"\n"+WikipediaSingleton.getInstance().wikipedia.getArticleById(articleID).getTitle() +", key: "+MapUtil.getFirst(sortedMap).getKey()+" value:"+MapUtil.getFirst(sortedMap).getValue());
			//		}
			//return MapUtil.getFirst(sortedMap).getValue();
			return result/countNonZero;
		}
	private static double get_P_Se_c(Annotation a) {//comes from EL system weight value because we calculate the confidence based on the prior prob
		return a.getWeight();
	}

	/**
	 * This method takes the all the context entities and tries to calculate the probabilities of the given an 
	 * entitiy and all the other context entities and sums them up and takes the avarage
	 * @return the avarage result
	 */
	private static double get_P_Ce_e_efficient(Integer mainId,Map<Integer, Map<Integer, Double>> mapContextSimilarity){ //Context entities an the entity(already disambiguated) 
		double result =0.0;
		//		double result =1.0;
		double countNonZero=0;
		Map<Integer, Double> temp = new HashMap<>(mapContextSimilarity.get(mainId));
		for (Entry<Integer, Double> e: temp.entrySet()) {
			double similarity = e.getValue();
			if (!Double.isNaN(similarity)&&similarity>0.0&&similarity!=1.0) {
				countNonZero++;
				result+=similarity;
			}
		}
		if (countNonZero==0) {
			return 0.0;
		}
		return result/countNonZero;
	}

	/**
	 * This method takes the all the context entities and tries to calculate the probabilities of the given an 
	 * entitiy and all the other context entities and sums them up
	 * @return
	 */
	private static double get_P_Ce_e(Integer mainId,List<Annotation> contextEntities){ //Context entities an the entity(already disambiguated) 
		double result =0.0;
		double countNonZero=0;
		for(Annotation a: contextEntities){
			double temp=.0;
			temp=(LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(mainId), String.valueOf(a.getId())));
			if (!Double.isNaN(temp)&&temp>0.0) {
				countNonZero++;
				result+=temp;
			}
			else{
				LOG.info("similarity could not be calculated entity-entity: "+mainId+" "+a.getURL());
			}
		}
		return result/countNonZero;
	}

	//	public static Map<Annotation,Map<Category, Double>> initilazeAnnotationCategoryRelation(List<Annotation> annotations){
	//		Map<Annotation,Map<Category, Double>> result = new HashMap<>();
	//		Set<Category> allCategories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setAllCategories);
	//		for(Annotation annotation: annotations) {
	//			if (annotation!=null) {
	//				Map<Category, Double> mapTempCat = new HashMap<>();
	//				for(Category category:allCategories){
	//						double similarity=Request_LINEServer.getSimilarity(String.valueOf(annotation.getId()), String.valueOf(category.getId()), Model_LINE.LINE_COMBINED_2nd);
	//						if (similarity>0) {
	//							mapTempCat.put(category,similarity);
	//						}
	//					}
	//					Map<Category, Double> mapSorted = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapTempCat));
	//					Entry<Category, Double> firstElement = MapUtil.getFirst(mapSorted);
	//					mapTempCat.put(firstElement.getKey(), firstElement.getValue());
	//				}	
	//				result.put(annotation, mapTempCat);
	//			}
	//		}
	//		return result;
	//	}
	public static Entry<Category, Double> getMostSimilarCategory(Annotation annotation)
	{
		if (annotation!=null) {
			Set<Category> categories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setAllCategories);
			Map<Category, Double> map = new HashMap<>();
			for(Category category:categories){

				if (LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(category.getId()))&&LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(annotation.getId()))) {
					double similarity = 0.0;
					try {
						similarity=LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(annotation.getId()), String.valueOf(category.getId()));
						map.put(category, similarity);
					} catch (Exception e) {
						System.out.println("exception finding the similarity: "+similarity);
					}
				}
				else {
					LOG.info("LINE model does not contain the category: "+category+" or "+annotation.getURL());
				}


			}	
			Map<Category, Double> mapSorted = new LinkedHashMap<>(MapUtil.sortByValueDescending(map));

			return MapUtil.getFirst(mapSorted);
		}
		return null;
	}
}
