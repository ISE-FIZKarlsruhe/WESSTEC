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
import org.fiz.ise.gwifi.Singleton.LINE_2modelSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.EmbeddingModel;
import org.fiz.ise.gwifi.model.Dataset;
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

public class HeuristicApproach2DifferentEmbeddings {

	private final static Dataset TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	private static Set<Category> setMainCategories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);
	private static boolean LOAD_2MODEL = Config.getBoolean("LOAD_2MODEL", false);
	private final static Integer DEPTH_OF_CAT_TREE = Config.getInt("DEPTH_OF_CAT_TREE", 0);
	private static Map<Category, Set<Category>> mapCategories;
	private static final Logger LOG = Logger.getLogger(HeuristicApproach2DifferentEmbeddings.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	private final double threshold = 0.9;

	/*
	 * The main purpose of this class is the calculate the similarity and decide 
	 * which category a text belongs to based on the probability
	 * 
	 */
	public static Category getBestMatchingCategory(String shortText,List<Category> gtList, Map<Category, Set<Category>> map) {
		mapCategories = new HashMap<>(map);
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		HeuristicApproach2DifferentEmbeddings heuristic = new HeuristicApproach2DifferentEmbeddings();
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
			boolean first =true;
			//Map<Integer, Double>  coherency = new HashMap<>(heuristic.calculateCoherency(lstAnnotations));
			Map<Integer, Map<Integer, Double>> contextSimilarity = new HashMap<>(calculateContextEntitySimilarities(lstAnnotations));
			for (Category mainCat : setMainCategories) {
				double score = 0.0; 
				for(Annotation a:lstAnnotations) {
					score+=heuristic.calculateScoreBasedInitialFormula(a, mainCat, contextSimilarity);
					//score+=heuristic.calculateScoreBasedWeightMaxCatSimilarity(a, mainCat);
				}
				first=false;
				mapScore.put(mainCat, score);
			}
			mainBuilder.append("\n");
			Map<Category, Double>  sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			Category firstElement = MapUtil.getFirst(sortedMap).getKey();

			for(Entry<Category, Double> e: sortedMap.entrySet()){			
				mainBuilder.append(e.getKey()+" "+e.getValue()+"\n");
			}
			mainBuilder.append(""+"\n");
			mainBuilder.append(""+"\n");
			if (!gtList.contains(firstElement)) {
				secondLOG.info(mainBuilder.toString());
			}
//			System.out.println(mainBuilder.toString());
			return firstElement;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public static void findCandidates(String shortText) {
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
		double P_Ce_e=get_P_Ce_e_efficient(a.getId(),contextSimilarity);
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
			for(Annotation a2: annotations){
				double similarity=.0;
				if (LOAD_2MODEL) {
					similarity=LINE_2modelSingleton.getInstance().lineModel_2nd.similarity(String.valueOf(a.getId()), String.valueOf(a2.getId()));
				}
				else {
					similarity =(EmbeddingsService.getSimilarity(String.valueOf(a.getId()), String.valueOf(a2.getId())));
				}
				temp.put(a2.getId(), similarity);
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
		if (DEPTH_OF_CAT_TREE==0) {
			double P_Cr_c=0.0;
			double P_e_Cr =0.0;
			if (LOAD_2MODEL) {
				P_e_Cr =LINE_2modelSingleton.getInstance().lineModel_1st.similarity(String.valueOf(articleID), String.valueOf(mainCat.getId()));
			}
			else {
				//P_e_Cr =EmbeddingsService.getSimilarity(String.valueOf(articleID), String.valueOf(mainCat.getId()));
			}
			if (!Double.isNaN(P_e_Cr)) {
				result+=P_e_Cr;
				countNonZero++;
			}
		}
		else {
			childCategories = new HashSet<>(mapCategories.get(mainCat));
			for(Category c:childCategories) {
				double P_Cr_c=0.0;
				double P_e_Cr =0.0;
				if (LOAD_2MODEL) {
					P_Cr_c =1;
					//P_Cr_c = LINE_modelSingleton.getInstance().line_Combined.similarity(String.valueOf(mainCat.getId()), String.valueOf(c.getId()));
					P_e_Cr =LINE_2modelSingleton.getInstance().lineModel_1st.similarity(String.valueOf(articleID), String.valueOf(c.getId()));
				}
				else {
					//P_Cr_c = EmbeddingsService.getSimilarity(String.valueOf(mainCat.getId()), String.valueOf(c.getId()));
					P_Cr_c =1; // here we ignore the relation between root cat and the child cat as we onl care about the relation between an entity and the category
					P_e_Cr =EmbeddingsService.getSimilarity(String.valueOf(articleID), String.valueOf(c.getId()));
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
		}
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
			if (LOAD_2MODEL) {
				temp=LINE_2modelSingleton.getInstance().lineModel_2nd.similarity(String.valueOf(mainId), String.valueOf(a.getId()));
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
				if (LOAD_2MODEL) {
					if (LINE_2modelSingleton.getInstance().lineModel_1st.hasWord(String.valueOf(category.getId()))&&LINE_2modelSingleton.getInstance().lineModel_1st.hasWord(String.valueOf(annotation.getId()))) {
						double similarity = 0.0;
						try {
							similarity=LINE_2modelSingleton.getInstance().lineModel_1st.similarity(String.valueOf(annotation.getId()), String.valueOf(category.getId()));
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
					if (LOAD_2MODEL) {
						if (LINE_2modelSingleton.getInstance().lineModel_1st.hasWord(String.valueOf(category.getId()))&&LINE_2modelSingleton.getInstance().lineModel_1st.hasWord(String.valueOf(annotation.getId()))) {
							double similarity = 0.0;
							try {
								similarity=LINE_2modelSingleton.getInstance().lineModel_1st.similarity(String.valueOf(annotation.getId()), String.valueOf(category.getId()));
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
