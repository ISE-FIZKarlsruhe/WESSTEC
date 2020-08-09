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
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingletonAnnotationFiltering;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.EmbeddingModel;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.CategoryUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.EmbeddingsService;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.Request_LINEServer;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.annotation.detection.NLPTopicDetector;
import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;
@Deprecated
public class HeuristicApproachConsiderAlsoWikiLinks {

	private final static Dataset TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	private static boolean LOAD_MODEL = Config.getBoolean("LOAD_MODEL", false);
	private final static Integer DEPTH_OF_CAT_TREE = Config.getInt("DEPTH_OF_CAT_TREE", 0);
	private static final Logger LOG = Logger.getLogger(HeuristicApproachConsiderAlsoWikiLinks.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger trhirLOG = Logger.getLogger("resultLogger");
	private final double threshold = 0.9;
	private static final int CATEGORY_DEPTH_FOR_FILTERING = Config.getInt("CATEGORY_DEPTH_FOR_FILTERING", 0);

	/*
	 * The main purpose of this class is the calculate the similarity and decide 
	 * which category a text belongs to based on the probability
	 * 
	 */
	public static Category getBestMatchingCategory(String shortText,List<Category> gtList) {
		//shortText="Retail sales improve amid caution The High Street perked up in September,   but consumer confidence is falling as a result of higher interest rates and concerns over the housing market,  figures reveal.";
		Set<Category> setMainCategories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);
		Set<Category> setfilterAnnotationCategories = new HashSet<>(CategorySingletonAnnotationFiltering.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		HeuristicApproachConsiderAlsoWikiLinks heuristic = new HeuristicApproachConsiderAlsoWikiLinks();
		StringBuilder mainBuilder = new StringBuilder();
		try {
			Map<Category, Double> mapScore = new HashMap<>(); 
			mainBuilder.append(shortText+"\n");
			StringBuilder strBuild = new StringBuilder();
			for(Category c: gtList)	{
				strBuild.append(c+" ");
			}
			List<Annotation> lstAnnotations = new ArrayList<>();
			service.annotate(shortText, lstAnnotations);
			mainBuilder.append(strBuild.toString()+"\n"+"\n");
			Map<Integer, Map<Integer, Double>> contextSimilarity = new HashMap<>(calculateContextEntitySimilarities(lstAnnotations));
			for (Category mainCat : setMainCategories) {
				double score = 0.0; 
				if (AnnonatationUtil.hasALink(shortText, mainCat, CATEGORY_DEPTH_FOR_FILTERING)) {
					for(Annotation a:lstAnnotations) {
						//if (AnnonatationUtil.hasALink(a, mainCat)) {
						score+=heuristic.calculateScoreBasedInitialFormula(a, mainCat, contextSimilarity);
					}
				}
				mapScore.put(mainCat, score);
				//	}
			}
			boolean write=false;
			mainBuilder.append("\n");
			//			if (mapScore.size()==0) {
			//				write=true;
			//				for (Category mainCat : setMainCategories) {
			//					double score = 0.0; 
			//					for(Annotation a:lstAnnotations) {
			//						score+=heuristic.calculateScoreBasedInitialFormula(a, mainCat, contextSimilarity);
			//					}
			//					mapScore.put(mainCat, score);
			//				}
			//			}
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
	private double calculateScoreBasedWeightMaxCatSimilarity(Annotation a, Category mainCat) {
		Entry<Category, Double> entry = getMostSimilarCategory(a,mainCat);
		double P_e_c = 0.0;
		if (entry!=null) {
			P_e_c= entry.getValue();
		}
		double P_Se_c=get_P_Se_c(a);
		return (P_e_c*P_Se_c);
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
	private  Map<Integer, Double>  calculateCoherency(List<Annotation> lstAnnotations) {
		Map<Integer, Double> mapContextSimilarity = new HashMap<>();
		for(Annotation a: lstAnnotations){
			//System.out.println("main A: "+a.getTitle()+" "+a.getWeight());
			List<Annotation> contextAnnotations = lstAnnotations.stream()
					.filter(p -> p.getId()!=a.getId()).collect(Collectors.toList());
			List<Annotation> listTemp = new ArrayList<>();
			for(Annotation c: contextAnnotations){
				//System.out.println("context A: "+c.getTitle()+" "+c.getWeight());
				if (c.getWeight()>=threshold) {
					listTemp.add(c);
				}
			}
			if (listTemp.size()==0) {
				listTemp=new ArrayList<>(findMaxWeightedAnnotation(contextAnnotations));
			}
			//System.out.println("listTemp:  "+listTemp.size());
			double similarity=.0;
			for(Annotation aFilteredContext:listTemp) {
				double simTemp =0.0;
				if (LOAD_MODEL) {
					simTemp=(LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(a.getId()), String.valueOf(aFilteredContext.getId())));
				}
				else {
					simTemp =(EmbeddingsService.getSimilarity(String.valueOf(a.getId()), String.valueOf(aFilteredContext.getId())));
				}
				similarity+=simTemp;
			}
			mapContextSimilarity.put(a.getId(), similarity/listTemp.size());
		}
		return mapContextSimilarity;
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
				if (LOAD_MODEL) {
					similarity=(LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(a.getId()), String.valueOf(c.getId())));
				}
				else {
					similarity =(EmbeddingsService.getSimilarity(String.valueOf(a.getId()), String.valueOf(c.getId())));
				}
				temp.put(c.getId(), similarity);
			}
			mapContextSimilarity.put(a.getId(), temp);
		}
		return mapContextSimilarity;
	}
	/*
	 * Popularity of the category
	 */
	private static int get_P_c_(Category c){
		return c.getChildArticles().length;
	}

	private static double get_P_e_c(int articleID,Category mainCat) {
		Set<Category> childCategories;
		double result =0.0;
		double countNonZero=0.0;
		int countNan=0;
		childCategories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).mapMainCatAndSubCats.get(mainCat));

		Set<Category> finalSet = new HashSet<>();

		if (mainCat.getTitle().equals("World")) {	
			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Human migration"));
			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Missing people"));
			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("International relations"));
			childCategories = new HashSet<>(finalSet);

		}	
		else if (mainCat.getTitle().equals("Business")) {	
			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Life insurance companies"));
			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Employment classifications"));
			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Finance fraud"));
			childCategories = new HashSet<>(finalSet);
		}	
		else if (mainCat.getTitle().equals("Sports")) {	
			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Museums established in 1910"));
			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("American male triathletes"));
			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("FINA World Junior Synchronised Swimming Championships"));
			childCategories = new HashSet<>(finalSet);
		}	
		else if (mainCat.getTitle().equals("Science and technology")) {	
			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Crowdsourcing"));
			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Anti-competitive behaviour"));
			finalSet.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Auditing"));
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
	private static double get_P_e_c_old(int articleID,Category mainCat) {
		Set<Category> childCategories;
		double result =0.0;
		double countNonZero=0.0;

		childCategories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).mapMainCatAndSubCats.get(mainCat));
		for(Category c:childCategories) {
			double P_Cr_c=0.0;
			double P_e_Cr =0.0;
			if (LOAD_MODEL) {
				P_Cr_c =1;
				//P_Cr_c = LINE_modelSingleton.getInstance().line_Combined.similarity(String.valueOf(mainCat.getId()), String.valueOf(c.getId()));
				P_e_Cr =LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(articleID), String.valueOf(c.getId()));
				//System.out.println(c+" "+P_e_Cr);
			}
			else {
				//P_Cr_c = EmbeddingsService.getSimilarity(String.valueOf(mainCat.getId()), String.valueOf(c.getId()));
				P_Cr_c =1; // here we ignore the relation between root cat and the child cat as we onl care about the relation between an entity and the category
				P_e_Cr =EmbeddingsService.getSimilarity(String.valueOf(articleID), String.valueOf(c.getId()));
				System.out.println(c+" "+P_e_Cr);
			}
			double temp =P_e_Cr*P_Cr_c;
			if (!Double.isNaN(temp)&&temp>0.0) {
				result+=temp;
				countNonZero++;
			}
			else{
				LOG.info("similarity could not be calculated category: "+c.getTitle()+" "+c.getChildArticles().length);
			}
		}
		//		}
		if (countNonZero==0) {
			return 0.0;
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
				//				result*=similarity;
			}
		}
		if (countNonZero==0) {
			return 0.0;
		}
		return result/countNonZero;
		//		return result;
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
			if (LOAD_MODEL) {
				temp=(LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(mainId), String.valueOf(a.getId())));
			}
			else {
				temp =(EmbeddingsService.getSimilarity(String.valueOf(mainId), String.valueOf(a.getId())));
			}
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
	public static Entry<Category, Double> getMostSimilarCategory(Annotation annotation,Category mainCategory)
	{
		if (annotation!=null) {
			Set<Category> categories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).mapMainCatAndSubCats.get(mainCategory));
			categories.add(mainCategory);
			Map<Category, Double> map = new HashMap<>();
			for(Category category:categories){
				if (LOAD_MODEL) {
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
				else {
					double similarity = 0.0;
					try {
						similarity=Request_LINEServer.getSimilarity(String.valueOf(annotation.getId()), String.valueOf(category.getId()), EmbeddingModel.LINE_1st_Complex);
						if (similarity>0) {
							map.put(category, similarity);
						}
					} catch (Exception e) {
						System.out.println("exception finding the similarity: "+similarity);
					}
				}
			}	

			Map<Category, Double> mapSorted = new LinkedHashMap<>(MapUtil.sortByValueDescending(map));
			return MapUtil.getFirst(mapSorted);
		}
		return null;
	}
	public static Entry<Category, Double> getMostSimilarCategory(Annotation annotation)
	{
		if (annotation!=null) {
			Set<Category> categories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setAllCategories);
			Map<Category, Double> map = new HashMap<>();
			for(Category category:categories){
				if (LOAD_MODEL) {
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
				else {
					double similarity = 0.0;
					try {
						similarity=Request_LINEServer.getSimilarity(String.valueOf(annotation.getId()), String.valueOf(category.getId()), EmbeddingModel.LINE_COMBINED_2nd);
						if (similarity>0) {
							map.put(category, similarity);
						}
					} catch (Exception e) {
						System.out.println("exception finding the similarity: "+similarity);
					}
				}
			}	
			Map<Category, Double> mapSorted = new LinkedHashMap<>(MapUtil.sortByValueDescending(map));
			return MapUtil.getFirst(mapSorted);
		}
		return null;
	}
}
