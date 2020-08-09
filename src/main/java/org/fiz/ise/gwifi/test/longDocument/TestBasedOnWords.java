package org.fiz.ise.gwifi.test.longDocument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.deeplearning4j.models.word2vec.Word2Vec;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.longText.TestBasedonLongTextDatasets;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.test.afterESWC.TestBasedonSortTextDatasets;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.SynchronizedCounter;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;

public class TestBasedOnWords {
	/*
	 * This class is a baseline of our approach which considers words to present a document vector by using
	 *  a pre trained word embeddings. HeuristicApproachAGNewsEntEnt
	 *  More specifically it can be considered as a pure dataless classification since we do only a similarity based classification
	 *  cosine similarity between a document vector and all the other category/word vectors  
	 */
	private final static Integer NUMBER_OF_THREADS=  Config.getInt("NUMBER_OF_THREADS",-1);
	private final static Dataset TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	private static SynchronizedCounter counterTruePositive;
	private static SynchronizedCounter counterFalsePositive;
	private static SynchronizedCounter counterProcessed;
	private static Map<Category, Integer> truePositive = new ConcurrentHashMap<>();
	private static Map<Category, Integer> falsePositive = new ConcurrentHashMap<>();
	private static Map<String, Category> falsePositiveResult = new ConcurrentHashMap<>();
	private static Map<String, Integer> mapMissClassified = new ConcurrentHashMap<>();
	private ExecutorService executor;
	private static Set<Category> setMainCategories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);
	private static Map<Category, Set<Category>> mapCategories;
	private static int numberOfSimilarWordsToCat=50;

	long now = System.currentTimeMillis();
	public static final Map<String,List<String>> CACHE_nearestWords = new HashMap<>();

	public static void main(String[] args) {
		counterProcessed= new SynchronizedCounter();
		counterFalsePositive= new SynchronizedCounter();
		counterTruePositive= new SynchronizedCounter();

		Word2Vec model= LINE_modelSingleton.getInstance().lineModel;

		for (Category mainCat : setMainCategories) {
			if (!CACHE_nearestWords.containsKey(mainCat.getTitle())) {
				Collection<String> wordsNearest = model.wordsNearest(mainCat.getTitle(), numberOfSimilarWordsToCat);
				List<String> lst = new ArrayList<>();
				for (String s : wordsNearest) {
					lst.add(s);
				}
				CACHE_nearestWords.put(mainCat.getTitle(), lst);
			}
			else
				break;
		}
		
		for(Entry<String,List<String>> e :CACHE_nearestWords.entrySet() ) {
			System.out.println(e.getKey()+" "+e.getValue());
		}
		
	TestBasedonSortTextDatasets test = new TestBasedonSortTextDatasets();
	Map<String,List<Category>> dataset = null;
	if (TEST_DATASET_TYPE.equals(Dataset.AG)) {
		System.out.println("Start reading AG News data");
		dataset = new HashMap<>(test.read_dataset_AG(AG_DataType.TITLEANDDESCRIPTION,Config.getString("DATASET_TEST_AG","")));
	}
	else if (TEST_DATASET_TYPE.equals(Dataset.WEB_SNIPPETS)) {
		System.out.println("Start reading WEB data");
		dataset = new HashMap<>(test.read_dataset_WEB(null));
	}

	TestBasedonLongTextDatasets test2 = new TestBasedonLongTextDatasets();
	//Map<String,List<Category>> dataset = new HashMap<>(test.initializeDataset()); //here you initialize the dataset also based on a number of sentences
	//test2.setCategoryList(dataset);

	//			Set<Category> setMainCategories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);
	//			Word2Vec model = LINE_modelSingleton.getInstance().lineModel;
	//			for(Category c : setMainCategories) {
	//				if (!model.hasWord(c.getTitle())) {
	//					System.out.println(c.getTitle());
	//				}
	//			}
	LINE_modelSingleton.getInstance();
	TestBasedOnWords base = new TestBasedOnWords();
	base.startProcessingData(dataset);
}
public void startProcessingData(Map<String,List<Category>> dataset) {
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
		System.out.println("Calculating F measures");
		FileUtil.writeDataToFile(truePositive,"TRUE_POSITIVE_RESULTS");
		FileUtil.writeDataToFile(falsePositiveResult,"FALSE_POSITIVE_RESULTS");
		FileUtil.writeDataToFile(mapMissClassified,"MISS_CLASSIFIED_RESULTS");
	} catch (Exception e) {
		System.out.println(e.getMessage());
	}
}
private Runnable handle(String description, List<Category> gtList,int i) {
	return () -> {
		Category bestMatchingCategory= BasedOnWordsCategorize.getBestMatchingCategory(description,gtList,mapCategories,CACHE_nearestWords);
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

}
