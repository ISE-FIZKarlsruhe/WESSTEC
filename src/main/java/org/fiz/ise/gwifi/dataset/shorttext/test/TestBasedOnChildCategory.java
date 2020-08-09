package org.fiz.ise.gwifi.dataset.shorttext.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.LINE_2modelSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LabelsOfTheTexts;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.Print;
import org.fiz.ise.gwifi.util.SynchronizedCounter;

import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;

public class TestBasedOnChildCategory {

	private final String DATASET_TEST_AG = Config.getString("DATASET_TEST_AG","");
	private final String DATASET_TEST_WEB = Config.getString("DATASET_TEST_WEB","");
	private final static Integer NUMBER_OF_THREADS=  Config.getInt("NUMBER_OF_THREADS",-1);
	private static boolean LOAD_MODEL = Config.getBoolean("LOAD_MODEL", false);
	private final static Dataset TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE"); 
	private static Wikipedia wikipedia = WikipediaSingleton.getInstance().wikipedia;
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
	private static Map<Category, Set<Category>> mapCategories;
	long now = System.currentTimeMillis();

	public static void main(String[] args) {
		TestBasedOnChildCategory test = new TestBasedOnChildCategory();
		test.initializeVariables();
	}
	private void initializeVariables() {
		System.out.println("NUMBER_OF_THREADS "+NUMBER_OF_THREADS);
		singCategory= CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE));
		counterProcessed= new SynchronizedCounter();
		counterFalsePositive= new SynchronizedCounter();
		counterTruePositive= new SynchronizedCounter();
		TestBasedOnChildCategory test = new TestBasedOnChildCategory();
	
		if (TEST_DATASET_TYPE.equals(Dataset.AG)) {
			startProcessingData(test.read_dataset_AG());
		}
		else if (TEST_DATASET_TYPE.equals(Dataset.WEB_SNIPPETS)) {
			test.dataset_WEB();
		}
	}
	public Map<String,List<Category>> read_dataset_AG() {
		Map<String,List<Category>> dataset = new HashMap<>();
		Map<Integer, Category> mapLabel = new HashMap<>(LabelsOfTheTexts.getLables_AG_category());
		try {
			List<String> lines = FileUtils.readLines(new File(DATASET_TEST_AG), "utf-8");
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			int i=0;
			for (i = 0; i < arrLines.length; i++) {
				List<Category> gtList = new ArrayList<>(); 
				String[] split = arrLines[i].split("\",\"");
				String label = split[0].replace("\"", "");
				numberOfSamplesPerCategory.put(mapLabel.get(Integer.valueOf(label)), numberOfSamplesPerCategory.getOrDefault(mapLabel.get(Integer.valueOf(label)), 0) + 1);
				gtList.add(mapLabel.get(Integer.valueOf(label)));
				String title = split[1].replace("\"", "");
				String description = split[2].replace("\"", "");
				dataset.put(title+" "+description, gtList);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return dataset;
	}
	private void startProcessingData(Map<String,List<Category>> dataset) {
		int count=0;
		try {
			executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
			for(Entry<String, List<Category>> e : dataset.entrySet()) {
				executor.execute(handle(e.getKey(),e.getValue(),++count));
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			System.out.println("Total time minutes " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
			System.out.println("Number of true positive: "+counterTruePositive.value()+" number of processed: "+counterProcessed.value());
			Double d = (counterTruePositive.value()*0.1)/(counterProcessed.value()*0.1);
			System.out.println("Accuracy: "+d);
			System.out.println("True Positive");
			Print.printMap(truePositive);
			System.out.println("\nmiss Clasified Positive");
			Print.printMap(mapMissClassified);
			System.out.println("\nFalse Positive");
			Print.printMap(falsePositive);
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
	private Runnable handle(String description, List<Category> gtList,int i) {
		return () -> {
			Category bestMatchingCategory=null;
			bestMatchingCategory = ClassifyBasedOnParentCategory.getBestMatchingCategory(description,gtList);
			if (bestMatchingCategory!=null) {
				
			}
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
					//falseNegative = totalNumberOfSamples_B -(predicted_B(true or false does not matter)) 
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
	public List<String> generateRandomDataset_AG(){
		List<String> result = new ArrayList<>();
		try {
			//	Map<String, Integer> map = new HashMap<>();
			List<String> lines = FileUtils.readLines(new File(DATASET_TEST_AG), "utf-8");
			for(String l : lines) {
				String[] split = l.split("\",\"");
				String label = split[0].replace("\"", "");
				result.add(l);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Number of lines "+result.size());
		return result;
	}
	
	public void dataset_WEB() {
		try {
			Map<String,List<Category>> dataset = new HashMap<>();
			List<String> lines = FileUtils.readLines(new File(DATASET_TEST_WEB), "utf-8");
			System.out.println("size of the file "+lines.size());
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			for (int i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split(" ");
				String label = split[split.length-1];
				String snippet = arrLines[i].substring(0, arrLines[i].length()-(label).length()).trim();
				List<Category> gtList = new ArrayList<>(); 
				if (label.contains("-")) {
					String[] splitLabel = label.split("-");
					for (int j = 0; j < splitLabel.length; j++) {
						if (splitLabel[j].equals("education")) {
							gtList.add(wikipedia.getCategoryByTitle("Hypotheses"));
							numberOfSamplesPerCategory.put(wikipedia.getCategoryByTitle("Hypotheses"), numberOfSamplesPerCategory.getOrDefault(wikipedia.getCategoryByTitle("Hypotheses"), 0) + 1);
						}else {
							gtList.add(wikipedia.getCategoryByTitle(StringUtils.capitalize(splitLabel[j])));
							numberOfSamplesPerCategory.put(wikipedia.getCategoryByTitle(StringUtils.capitalize(splitLabel[j])), numberOfSamplesPerCategory.getOrDefault(wikipedia.getCategoryByTitle(StringUtils.capitalize(splitLabel[j])), 0) + 1);
						}
					}
				}
				else{
					if (label.equalsIgnoreCase("computers")) {
						gtList.add(wikipedia.getCategoryByTitle(("Computer hardware")));
						numberOfSamplesPerCategory.put(wikipedia.getCategoryByTitle(("Computer hardware")), numberOfSamplesPerCategory.getOrDefault(wikipedia.getCategoryByTitle(("Computer hardware")), 0) + 1);
						//gtList.add(wikipedia.getCategoryByTitle(("Computer networking")));
					}
					
					else {gtList.add(wikipedia.getCategoryByTitle(StringUtils.capitalize(label)));
					numberOfSamplesPerCategory.put(wikipedia.getCategoryByTitle(StringUtils.capitalize(label)), numberOfSamplesPerCategory.getOrDefault(wikipedia.getCategoryByTitle(StringUtils.capitalize(label)), 0) + 1);
					}
				}
				Print.printMap(numberOfSamplesPerCategory);
				dataset.put(snippet, gtList);
			}
			startProcessingData(dataset);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
