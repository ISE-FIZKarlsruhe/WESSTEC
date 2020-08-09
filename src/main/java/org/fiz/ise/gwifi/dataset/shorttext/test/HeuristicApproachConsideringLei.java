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
import org.fiz.ise.gwifi.model.EmbeddingModel;
import org.fiz.ise.gwifi.model.Dataset;
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

public class HeuristicApproachConsideringLei {

	private final static Dataset TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	private static Set<Category> setMainCategories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);
	private final static Integer DEPTH_OF_CAT_TREE = Config.getInt("DEPTH_OF_CAT_TREE", 0);
	private static Map<Category, Set<Category>> mapCategories=new HashMap<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).mapMainCatAndSubCats);
	private static final Logger LOG = Logger.getLogger(HeuristicApproachConsideringLei.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	/*
	 * The main purpose of this class is the calculate the similarity and decide 
	 * which category a text belongs to based on the probability
	 * 
	 */
	public static Category getBestMatchingCategory(String shortText,List<Category> gtList) {
		//shortText="Earthquake at sea gives Taiwan a jolt TAIPEI A strong earthquake in the Pacific off Taiwan rocked the island #39;s northeast on Friday, damaging buildings and injuring several people, officials said.";

		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		HeuristicApproachConsideringLei heuristic = new HeuristicApproachConsideringLei();
		StringBuilder mainBuilder = new StringBuilder();
		try {
			Map<Category, Double> mapScore = new HashMap<>(); 
			List<Annotation> lstAnnotations = new ArrayList<>();
			service.annotate(shortText, lstAnnotations);
			Map<Integer, Map<Integer, Double>> contextSimilarity = new HashMap<>(calculateContextEntitySimilarities(lstAnnotations));
			for (Category mainCat : setMainCategories) {
				secondLOG.info(shortText+" "+mainCat.getTitle()+"\n");
				//System.out.println((shortText+" \n"+mainCat.getTitle()+"\n"));
				double score = 0.0; 
				for(Annotation a:lstAnnotations) {
					score+=heuristic.calculateScoreBasedInitialFormula(a, mainCat, contextSimilarity);
					//System.out.println(a.getTitle()+" "+mainCat.getTitle()+": " +heuristic.calculateScoreBasedInitialFormula(a, mainCat, contextSimilarity));
					//	secondLOG.info(a.getTitle()+", "+mainCat.getTitle()+" "+": " +heuristic.calculateScoreBasedInitialFormula(a, mainCat, contextSimilarity));
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
		//		System.out.println("similarity: "+a.getTitle()+", "+mainCat.getTitle()+": "+P_e_c);
		//		List<Category> ancestorCat = new ArrayList<>(Arrays.asList(WikipediaSingleton.getInstance().getArticle(a.getTitle()).getParentCategories()));
		//		for(Category c: ancestorCat) {
		//			List<Category> mainCatChils = new ArrayList<>(Arrays.asList(mainCat.getChildCategories()));
		//			for(Category d: mainCatChils) {
		//				System.out.println("similarity  between: "+ c.getTitle()+" "+d.getTitle()+", "+LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(c.getId()),String.valueOf(d.getId())));
		//			}
		//		}
		//		System.out.println("\n");
		//		System.out.println("similarity: "+a.getTitle()+", "+mainCat.getTitle()+": "+P_e_c);

		double P_Se_c=get_P_Se_c(a);
		double P_Ce_e=1;
		if (contextSimilarity.size()>1) {
			P_Ce_e=get_P_Ce_e_efficient(a.getId(),contextSimilarity);
		}
		return (P_e_c*P_Se_c*P_Ce_e);
	}
	private List<Annotation> findMaxWeightedAnnotation(List<Annotation> contextAnnotations) {
		double max = 0.0;
		Annotation result = null;
		for(Annotation a: contextAnnotations) {
			if(a.getWeight() > max){
				max = a.getWeight();
				result = a;
			}
		}
		return Arrays.asList(result);
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
	//		childCategories = new HashSet<>(mapCategories.get(mainCat));
	//		if (WikipediaSingleton.getInstance().wikipedia.getArticleById(articleID)==null) {
	//			return 0;
	//		}
	//		for(Category c:childCategories) {
	//			double P_e_Cr =0.0;
	//			Set<Category> childCatOfArticle = new HashSet<>(Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getArticleById(articleID).getParentCategories()));
	//			for(Category cA:childCatOfArticle) {
	//				P_e_Cr =LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(cA.getId()), String.valueOf(c.getId()));
	//				//secondLOG.info(c.getTitle()+" "+cA.getTitle()+" "+P_e_Cr);
	//				System.out.println(c.getTitle()+" "+cA.getTitle()+" "+P_e_Cr);
	//				if (!Double.isNaN(P_e_Cr)&&P_e_Cr>0.0) {
	//					result+=P_e_Cr;
	//					countNonZero++;
	//				}
	//				else{
	//					LOG.info("similarity could not be calculated category: "+c.getTitle()+" "+c.getChildArticles().length);
	//				}
	//			}
	//		}
	//
	//		if (countNonZero==0) {
	//			return 0.0;
	//		}
	//		return result/countNonZero;
	//	}

	private static double get_P_e_c(int articleID,Category mainCat) {
		Set<Category> childCategories;
		double result =0.0;
		double countNonZero=0.0;
		childCategories = new HashSet<>(mapCategories.get(mainCat));
		if (WikipediaSingleton.getInstance().wikipedia.getArticleById(articleID)==null) {
			return 0;
		}
		for(Category c:childCategories) {
			double P_e_Cr =0.0;
			Set<Category> childCatOfArticle = new HashSet<>(Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getArticleById(articleID).getParentCategories()));
			for(Category cA:childCatOfArticle) {
				P_e_Cr =LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(cA.getId()), String.valueOf(c.getId()));
				if (!Double.isNaN(P_e_Cr)&&P_e_Cr>0.0) {
					result+=P_e_Cr;
					countNonZero++;
				}
			}
			if (countNonZero==0) {
				return 0.0;
			}
		}
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
