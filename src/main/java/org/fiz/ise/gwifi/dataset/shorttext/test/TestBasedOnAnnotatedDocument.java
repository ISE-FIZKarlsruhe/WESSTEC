package org.fiz.ise.gwifi.dataset.shorttext.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.dataset.shorttext.test.CalculateClassificationMetrics;
import org.fiz.ise.gwifi.dataset.shorttext.test.HeuristicApproach;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.EmbeddingsService;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.SynchronizedCounter;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class TestBasedOnAnnotatedDocument {
	private static CategorySingleton singCategory;
	private static SynchronizedCounter counterTruePositive;
	private static SynchronizedCounter counterFalsePositive;
	private static SynchronizedCounter counterProcessed;
	private static Map<Category, Integer> numberOfSamplesPerCategory = new ConcurrentHashMap<>();
	private static Map<Category, Integer> truePositive = new ConcurrentHashMap<>();
	private static Map<Category, Integer> falsePositive = new ConcurrentHashMap<>();
	private static Map<String, Category> falsePositiveResult = new ConcurrentHashMap<>();
	private static Map<String, Integer> mapMissClassified = new ConcurrentHashMap<>();
	private ExecutorService executor;
	private static List<String> lstCategory;
	private final static Integer NUMBER_OF_THREADS=  Config.getInt("NUMBER_OF_THREADS",-1);
	private static Map<Category, Set<Category>> mapCategories;
	long now = System.currentTimeMillis();
	private static boolean LOAD_MODEL = Config.getBoolean("LOAD_MODEL", false);
	private final static Dataset TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE"); 
	private final static Integer DEPTH_OF_CAT_TREE = Config.getInt("DEPTH_OF_CAT_TREE", 0);
	private static final Logger LOG = Logger.getLogger(HeuristicApproach.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	private static Wikipedia wikipedia = WikipediaSingleton.getInstance().wikipedia;
	
	private static Set<Category> setMainCategories ;
	
	public void initializeCategoryMap(Map<List<String>, List<Category>> dataset)
	{	
		counterProcessed= new SynchronizedCounter();
		counterFalsePositive= new SynchronizedCounter();
		counterTruePositive= new SynchronizedCounter();
		lstCategory = new ArrayList<>();
		for(Entry<List<String>, List<Category>> e: dataset.entrySet()) {
			List<Category> temp = new ArrayList<>(e.getValue());
			for(Category c:temp) {
				if (!lstCategory.contains(c.getTitle())) {
					lstCategory.add(c.getTitle());
				}
			}
		}
		System.out.println("Size of Category list "+lstCategory.size());
		singCategory= CategorySingleton.getInstance(lstCategory);
		setMainCategories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);
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
		mapCategories= new HashMap<>(mapTemp);
		System.out.println("category map initialized "+mapCategories.size());
		if (LOAD_MODEL) {
			LINE_modelSingleton.getInstance();
		}
	}
	public void startProcessingData(Map<List<String>, List<Category>> dataset) {
		int count=0;
		try {
			System.out.println("inside start processing " +dataset.size());
			initializeCategoryMap(dataset);
			System.out.println("initialized mapCategories "+mapCategories.size() );
			executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
			for(Entry<List<String>, List<Category>> e : dataset.entrySet()) {
				executor.execute(handle(e.getKey(),e.getValue(),++count));
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			System.out.println("Total time minutes " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
			System.out.println("Number of true positive: "+counterTruePositive.value()+" number of processed: "+counterProcessed.value());
			Double d = (counterTruePositive.value()*0.1)/(counterProcessed.value()*0.1);
			System.out.println("Accuracy: "+d);
			System.out.println("Calculating F measures");
			CalculateClassificationMetrics calculate = new CalculateClassificationMetrics();
			calculate.evaluateResults(truePositive, falsePositive, numberOfSamplesPerCategory);
			FileUtil.writeDataToFile(truePositive,"TRUE_POSITIVE_RESULTS");
			FileUtil.writeDataToFile(falsePositiveResult,"FALSE_POSITIVE_RESULTS");
			FileUtil.writeDataToFile(mapMissClassified,"MISS_CLASSIFIED_RESULTS");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	private Runnable handle(List<String> description, List<Category> gtList,int i) {
		return () -> {
			Category bestMatchingCategory= getBestMatchingCategory(description,gtList);
			counterProcessed.increment();
			if (gtList.contains(bestMatchingCategory)) {
				counterTruePositive.increment();
				truePositive.put(gtList.get(0), truePositive.getOrDefault(gtList.get(0), 0) + 1);
				System.out.println(" total processed: "+i+" True positive "+counterTruePositive.value());
			}
			else{
				try {
					falsePositiveResult.put(description+"\n gt:"+gtList.get(0).getTitle(), bestMatchingCategory);
					falsePositive.put(gtList.get(0), falsePositive.getOrDefault(gtList.get(0), 0) + 1);
				} catch (Exception e) {
					System.out.println("Exception msg "+e.getMessage());
					System.out.println("description "+description+" "+gtList+" "+bestMatchingCategory );
					System.exit(1);
				}
				counterFalsePositive.increment();
				String key=gtList.get(0)+"\t"+"predicted: "+bestMatchingCategory;
				mapMissClassified.put(key, mapMissClassified.getOrDefault(key, 0) + 1);
				System.out.println(" total processed: "+i+" True positive "+counterTruePositive.value());
			}
		};
	}
	private Category getBestMatchingCategory(List<String> entities,List<Category> gtList) {
		StringBuilder mainBuilder = new StringBuilder();
		try {
			Map<Category, Double> mapScore = new HashMap<>(); 
			entities.forEach(entity -> mainBuilder.append(entity+" "));
			gtList.forEach(category -> mainBuilder.append(category+" "));
			for (Category mainCat : setMainCategories) {
				double score = 0.0; 
				for(String a:entities) {
					score+=calculateScoreBasedInitialFormula(wikipedia.getArticleByTitle(a).getId(), mainCat);
				}
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
	private double calculateScoreBasedInitialFormula(int ID, Category mainCat) {
		double P_e_c=get_P_e_c(ID, mainCat);
		return P_e_c;
	}
	public static List<String> getLstCategory() {
		return lstCategory;
	}
	
	private static double get_P_e_c(int articleID,Category mainCat) {
		Set<Category> childCategories;
		double result =0.0;
		double countNonZero=0.0;
		if (DEPTH_OF_CAT_TREE==0) {
			double P_Cr_c=0.0;
			double P_e_Cr =0.0;
			if (LOAD_MODEL) {
				P_e_Cr =LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(articleID), String.valueOf(mainCat.getId()));
			}
			else {
				P_e_Cr =EmbeddingsService.getSimilarity(String.valueOf(articleID), String.valueOf(mainCat.getId()));
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
				if (LOAD_MODEL) {
					P_Cr_c =1;
					P_e_Cr =LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(articleID), String.valueOf(c.getId()));
				}
				else {
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
}