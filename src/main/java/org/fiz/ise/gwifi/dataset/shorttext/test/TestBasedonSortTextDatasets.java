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
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.LINE_2modelSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.PageCategorySingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LabelsOfTheTexts;
import org.fiz.ise.gwifi.dataset.ReadDataset;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.Print;
import org.fiz.ise.gwifi.util.SynchronizedCounter;

import com.mongodb.util.Hash;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class TestBasedonSortTextDatasets {

	private final String DATASET_TEST_AG = Config.getString("DATASET_TEST_AG","");
	private final String DATASET_TEST_WEB = Config.getString("DATASET_TEST_WEB","");
	private final String DATASET_TEST_DBLP = Config.getString("DATASET_TEST_DBLP","");
	private final String DATASET_TEST_YAHOO = Config.getString("DATASET_TEST_YAHOO","");
	private final static Integer NUMBER_OF_THREADS=  Config.getInt("NUMBER_OF_THREADS",-1);
	private static boolean LOAD_MODEL = Config.getBoolean("LOAD_MODEL", false);
	private final static Dataset TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE"); 
	private static Wikipedia wikipedia = WikipediaSingleton.getInstance().wikipedia;
	private static SynchronizedCounter counterTruePositive;
	private static SynchronizedCounter counterFalsePositive;
	private static SynchronizedCounter counterProcessed;
	private static Map<Category, Integer> numberOfSamplesPerCategory = new ConcurrentHashMap<>();
	private static Map<String, Integer> truePositive = new ConcurrentHashMap<>();
	private static Map<String, Integer> falsePositive = new ConcurrentHashMap<>();
	private static Map<String, String> falsePositiveResult = new ConcurrentHashMap<>();
	private static Map<String, Integer> mapMissClassified = new ConcurrentHashMap<>();
	private ExecutorService executor;
	long now = System.currentTimeMillis();

	public static final Map<Article,List<Article>> CACHE = new HashMap<>();
	static {
		if (LOAD_MODEL) {
			LINE_modelSingleton.getInstance();
		}
		PageCategorySingleton.getInstance();
	}
	public static void main(String[] args) {
		TestBasedonSortTextDatasets test = new TestBasedonSortTextDatasets();
		test.initializeVariables();
	}
	private void initializeVariables() {
		System.out.println("running: "+this.getClass().getSimpleName());
		System.out.println("NUMBER_OF_THREADS: "+NUMBER_OF_THREADS);
		System.out.println("TEST_DATASET_TYPE: "+TEST_DATASET_TYPE);
		//singCategory= CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE));
		counterProcessed= new SynchronizedCounter();
		counterFalsePositive= new SynchronizedCounter();
		counterTruePositive= new SynchronizedCounter();
		//counterWorldFalsePositive= new SynchronizedCounter();

		AnnotationSingleton.getInstance();
		TestBasedonSortTextDatasets test = new TestBasedonSortTextDatasets();

		if (TEST_DATASET_TYPE.equals(Dataset.AG)) {
			System.out.println("Start reading AG News data");
			startProcessingData(ReadDataset.read_dataset_AG(AG_DataType.TITLEANDDESCRIPTION));
		}
		else if (TEST_DATASET_TYPE.equals(Dataset.WEB_SNIPPETS)) {
			AnnotationSingleton.getInstance();
			System.out.println("Start reading WEB data");
			startProcessingData(test.read_dataset_WEB());
		}
		else if (TEST_DATASET_TYPE.equals(Dataset.DBpedia)) {
			System.out.println("Start reading DBpedia data");
			startProcessingData(ReadDataset.read_dataset_DBPedia_SampleLabel_with_Categories(Config.getString("DATASET_DBP_TEST","")));
		}
		else if (TEST_DATASET_TYPE.equals(Dataset.DBLP)) {
			test.dataset_DBLP();
		}
		else if (TEST_DATASET_TYPE.equals(Dataset.YAHOO)) {
			test.dataset_Yahoo();
		}
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
			//			System.out.println("counterWorldFalsePositive: "+counterWorldFalsePositive.value());
			System.out.println("True Positive");
			Print.printMap(truePositive);
			System.out.println("\nmiss Clasified Positive");
			Print.printMap(mapMissClassified);
			System.out.println((Categories.getCategoryList(TEST_DATASET_TYPE)));
			System.out.println("\nFalse Positive");
			Print.printMap(falsePositive);
			System.out.println("Calculating F measures");
			//			CalculateClassificationMetrics calculate = new CalculateClassificationMetrics();
			//			calculate.evaluateResults(truePositive, falsePositive, numberOfSamplesPerCategory);
			FileUtil.writeDataToFile(truePositive,"TRUE_POSITIVE_RESULTS");
			FileUtil.writeDataToFile(falsePositiveResult,"FALSE_POSITIVE_RESULTS");
			FileUtil.writeDataToFile(mapMissClassified,"MISS_CLASSIFIED_RESULTS");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	private Runnable handle(String description, List<Category> gtList,int i ) {
		return () -> {
			Category bestMatchingCategory=null;
			if (TEST_DATASET_TYPE==Dataset.AG ) {
				bestMatchingCategory = HeuristicApproachCIKMPaperAGNews.getBestMatchingCategory(description,gtList);
			}
			else if(TEST_DATASET_TYPE==Dataset.WEB_SNIPPETS ) {
				bestMatchingCategory = HeuristicApproachCIKMPaperWebSnippets.getBestMatchingCategory(description,gtList);
			}
			else {
				bestMatchingCategory = HeuristicApproachCIKMPaper.getBestMatchingCategory(description,gtList);
			}
			counterProcessed.increment();
			StringBuilder builderGt = new StringBuilder();
			for(Category g : gtList) {
				builderGt.append(StringUtils.capitalize(g.getTitle()));
			}
			if (gtList.contains(bestMatchingCategory)) {
				counterTruePositive.increment();
				truePositive.put(builderGt.toString(), truePositive.getOrDefault(builderGt.toString(), 0) + 1);
				System.out.println(" total processed: "+i+" True positive "+counterTruePositive.value());
			}
			else{
				try {
					//					String key=builderGt.toString()+"\t"+"predicted: "+bestMatchingCategory;
					String key=builderGt.toString()+"\t"+"predicted: ";

					if(bestMatchingCategory.getTitle().contains("Culture")||bestMatchingCategory.getTitle().contains("Arts")
							||bestMatchingCategory.getTitle().contains("Entertainment")) {

						falsePositiveResult.put(description+"\n gt:"+builderGt.toString(), "CultureArtsEntertainment");
						key=key.concat("CultureArtsEntertainment");
					}
					else if(bestMatchingCategory.getTitle().contains("Education")||bestMatchingCategory.getTitle().contains("Science")){
						falsePositiveResult.put(description+"\n gt:"+builderGt.toString(),"EducationScience");
						key=key.concat("EducationScience");
					}
					else {
						falsePositiveResult.put(description+"\n gt:"+builderGt.toString(), bestMatchingCategory.getTitle());
						key=key.concat(bestMatchingCategory.getTitle());
					}
					falsePositive.put(builderGt.toString(), falsePositive.getOrDefault(builderGt.toString(), 0) + 1);
					counterFalsePositive.increment();
					mapMissClassified.put(key, mapMissClassified.getOrDefault(key, 0) + 1);

				} catch (Exception e) {
					System.out.println("Exception msg "+e.getMessage());
					System.out.println("description "+description+" "+gtList+" "+bestMatchingCategory );
					System.exit(1);
				}

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
	public void dataset_Yahoo() {
		Map<String,List<Category>> dataset = new HashMap<>();
		Map<Integer, String> mapLabel = new HashMap<>(LabelsOfTheTexts.getLables_Yahoo());
		List<String> lines;
		try {
			lines = new ArrayList<>(FileUtils.readLines(new File(DATASET_TEST_YAHOO), "utf-8"));
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			for (int i = 0; i < arrLines.length; i++) {
				List<Category> gtList = new ArrayList<>(); 
				String[] split = arrLines[i].split("\",\"");
				String label = split[0].replace("\"", "");
				String originalLabel =String.valueOf(mapLabel.get(Integer.valueOf(label)));
				String text = arrLines[i].substring(0, arrLines[i].length()-(label).length()).trim();
				if (originalLabel.contains("-")) {
					String[] splitLabel = originalLabel.split("-");
					for (int j = 0; j < splitLabel.length; j++) {
						gtList.add(wikipedia.getCategoryByTitle(StringUtils.capitalize(splitLabel[j])));
					}
					numberOfSamplesPerCategory.put(wikipedia.getCategoryByTitle(StringUtils.capitalize(splitLabel[0])), numberOfSamplesPerCategory.getOrDefault(wikipedia.getCategoryByTitle(StringUtils.capitalize(splitLabel[0])), 0) + 1);
				}
				else{
					gtList.add(wikipedia.getCategoryByTitle(StringUtils.capitalize(originalLabel)));
					numberOfSamplesPerCategory.put(wikipedia.getCategoryByTitle(StringUtils.capitalize(originalLabel)), numberOfSamplesPerCategory.getOrDefault(wikipedia.getCategoryByTitle(StringUtils.capitalize(originalLabel)), 0) + 1);
				}
				dataset.put(text, gtList);
			}
			Print.printMap(numberOfSamplesPerCategory);
			startProcessingData(dataset);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	} 
	public void dataset_DBLP() {
		try {
			System.out.println("Start readingn "+TEST_DATASET_TYPE);
			Map<String,List<Category>> dataset = new HashMap<>();
			List<String> lines = FileUtils.readLines(new File(DATASET_TEST_DBLP), "utf-8");
			Map<Integer, Category> mapLabel = new HashMap<>(LabelsOfTheTexts.getLables_DBLP());
			System.out.println("size of the file "+lines.size());
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			for (int i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split(" ");
				String label = split[0];
				String snippet = arrLines[i].substring(split[0].length(), arrLines[i].length()).trim();
				List<Category> gtList = new ArrayList<>(); 
				if (label.equals("6")||label.equals("7")) {
					gtList.add(mapLabel.get(6));
					gtList.add(mapLabel.get(7));
				}
				else {
					gtList.add(mapLabel.get(Integer.valueOf(label)));
				}
				dataset.put(snippet, gtList);
			}
			startProcessingData(dataset);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	public Map<String,List<Category>> read_dataset_WEB() {
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
						gtList.add(wikipedia.getCategoryByTitle(StringUtils.capitalize(splitLabel[j])));
						numberOfSamplesPerCategory.put(wikipedia.getCategoryByTitle(StringUtils.capitalize(splitLabel[j])), numberOfSamplesPerCategory.getOrDefault(wikipedia.getCategoryByTitle(StringUtils.capitalize(splitLabel[j])), 0) + 1);

						//						if (splitLabel[j].equals("education")) {
						////							gtList.add(wikipedia.getCategoryByTitle("Hypotheses"));
						//							gtList.add(wikipedia.getCategoryByTitle("Hypotheses"));
						//							numberOfSamplesPerCategory.put(wikipedia.getCategoryByTitle("Hypotheses"), numberOfSamplesPerCategory.getOrDefault(wikipedia.getCategoryByTitle("Hypotheses"), 0) + 1);
						//						}else {
						//							gtList.add(wikipedia.getCategoryByTitle(StringUtils.capitalize(splitLabel[j])));
						//							numberOfSamplesPerCategory.put(wikipedia.getCategoryByTitle(StringUtils.capitalize(splitLabel[j])), numberOfSamplesPerCategory.getOrDefault(wikipedia.getCategoryByTitle(StringUtils.capitalize(splitLabel[j])), 0) + 1);
						//						}
					}
				}
				else{
					gtList.add(wikipedia.getCategoryByTitle(StringUtils.capitalize(label)));
					numberOfSamplesPerCategory.put(wikipedia.getCategoryByTitle(StringUtils.capitalize(label)), numberOfSamplesPerCategory.getOrDefault(wikipedia.getCategoryByTitle(StringUtils.capitalize(label)), 0) + 1);
					//					if (label.equalsIgnoreCase("computers")) {
					//						//						gtList.add(wikipedia.getCategoryByTitle(("Computer hardware")));
					//						//						numberOfSamplesPerCategory.put(wikipedia.getCategoryByTitle(("Computer hardware")), numberOfSamplesPerCategory.getOrDefault(wikipedia.getCategoryByTitle(("Computer hardware")), 0) + 1);
					//						gtList.add(wikipedia.getCategoryByTitle(("Computer hardware")));
					//						numberOfSamplesPerCategory.put(wikipedia.getCategoryByTitle(("Technology")), numberOfSamplesPerCategory.getOrDefault(wikipedia.getCategoryByTitle(("Technology")), 0) + 1);
					//					}
					//
					//					else {
					//						gtList.add(wikipedia.getCategoryByTitle(StringUtils.capitalize(label)));
					//						numberOfSamplesPerCategory.put(wikipedia.getCategoryByTitle(StringUtils.capitalize(label)), numberOfSamplesPerCategory.getOrDefault(wikipedia.getCategoryByTitle(StringUtils.capitalize(label)), 0) + 1);
					//					}
				}

				dataset.put(snippet, gtList);
			}
			Print.printMap(numberOfSamplesPerCategory);
			return dataset;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
}
