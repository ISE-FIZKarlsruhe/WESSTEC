package org.fiz.ise.gwifi.test.afterESWC;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections15.map.HashedMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LabelsOfTheTexts;
import org.fiz.ise.gwifi.dataset.ReadDataset;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.Print;
import org.fiz.ise.gwifi.util.SynchronizedCounter;

import com.mongodb.util.Hash;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;

public class DatasetGenerationBasedOnVector {

	private final static Dataset TEST_DATASET_TYPE = Config.getEnum("TEST_DATASET_TYPE");
	private final static String TRAIN_SET_AG = Config.getString("DATASET_TRAIN_AG","");
	private final static Double THRESHOLD = Config.getDouble("THRESHOLD",1.0);
	private final static String TRAIN_SET_WEB = Config.getString("DATASET_TRAIN_WEB","");
	private final static Integer NUMBER_OF_THREADS= Config.getInt("NUMBER_OF_THREADS",-1);
	
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");
	
	static int numberOfSamples=50;
	private static int countCorrect;
	private static int countWrong;
	
	private static SynchronizedCounter countCorrectSyn;
	private static SynchronizedCounter countWrongSyn;
	private static SynchronizedCounter countNullSyn;
	
	private ExecutorService executor;

	public static void main(String[] args) {
		AnnotationSingleton.getInstance();
		System.out.println("Start loading model..");
		LINE_modelSingleton.getInstance();

		countCorrect=0;
		countWrong=0;

		countCorrectSyn=new SynchronizedCounter();
		countWrongSyn=new SynchronizedCounter();
		countNullSyn=new SynchronizedCounter();
		
		//datasetGenerateFromList(sample);
		//datasetGenerateFromTrainSet();
		DatasetGenerationBasedOnVector generate = new DatasetGenerationBasedOnVector();
		//generate.datasetGenerateFromTrainSetConsideringN(2);

		generate.datasetGenerateFromTrainSetWithThreshold();
		System.out.println("Thershold: "+THRESHOLD);

	}

	private static void datasetGenerateFromList(List<String> lst) {
		if (TEST_DATASET_TYPE.equals(Dataset.AG)) {
			for(String str: lst) {
				System.out.println(BestMatchingLabelBasedOnVectorSimilarity.getBestMatchingCategory(str)+"\t"+str);
				//secondLOG.info(HeuristicBasedOnEntitiyVectorSimilarity.getBestMatchingCategory(str)+"\t"+str);
			}
		}
	}

	private void datasetGenerateFromTrainSetConsideringN(int n) {
		try {
			Map<String, List<Article>> dataset = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION,Config.getString("DATASET_TRAIN_AG",""));
			int count =0;
			executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
			for(Entry<String, List<Article>> e: dataset.entrySet()) {
				executor.execute(handle(e.getKey(),e.getValue(),++count, n));
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			System.out.println("Accuracy "+(countCorrectSyn.value()/(dataset.size()*1.0)));

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private Runnable handle(String description, List<Article> gtList,int i, int n ) {
		return () -> {
			List<Article> bestMatchingCategory = new ArrayList<Article>(BestMatchingLabelBasedOnVectorSimilarity.getBestMatchingNArticles(description,n));
			bestMatchingCategory.retainAll(gtList);
			if (bestMatchingCategory.size()>0) {
				countCorrectSyn.increment();
			}
			else {
				countWrongSyn.increment();
				resultLog.info(description+"\t"+gtList+"\t"+bestMatchingCategory);
			}
			System.out.println("Number of precessed: "+i +" correctly classified: "+countCorrectSyn.value());
			System.out.println("countCorrect "+countCorrectSyn.value()+"\nWrongly assigned labels: "+countWrongSyn.value());
			System.out.println("Total classified "+(countCorrectSyn.value()+countWrongSyn.value()));

		};
	}

	public void datasetGenerateFromTrainSetWithThreshold() {
		try {

//			Collection<Article> values = LabelsOfTheTexts.getLablesAsArticle_AG().values();
//			Iterator<Article> iterator = values.iterator();
//			while (iterator.hasNext()) {
//				File directory = new File(iterator.next().getTitle());
//				if (! directory.exists()){
//					directory.mkdir();
//				}
//			}
			Map<String, List<Article>> dataset = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION,Config.getString("DATASET_TRAIN_AG",""));
			int count =0;
			executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
			for(Entry<String, List<Article>> e: dataset.entrySet()) {
				executor.execute(handle_findBestMachingArticleWithThreshold(e.getKey(),e.getValue(),++count));
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

			System.out.println("countCorrect "+countCorrectSyn.value()+"\nWrongly assigned labels: "+countWrongSyn.value()+"\nNull assigned labels: "+countNullSyn.value());
			System.out.println("Total classified "+(countCorrectSyn.value()+countWrongSyn.value()));
			System.out.println("Accuracy "+(countCorrectSyn.value()/(dataset.size()*1.0)));
			System.out.println("Threshold "+THRESHOLD);
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private Runnable handle_findBestMachingArticleWithThreshold(String description, List<Article> gtList, int i ) {
		return () -> {
			Article bestMatchingCategory = BestMatchingLabelBasedOnVectorSimilarity.getBestMatchingArticlewithThreshold(description,THRESHOLD);
			if (gtList.contains(bestMatchingCategory)) {
				countCorrectSyn.increment();
				//FileUtil.writeDataToFile(Arrays.asList(description), bestMatchingCategory.getTitle()+File.separator+ i,false);
			}
			else if(bestMatchingCategory==null) {
				countNullSyn.increment();
			}
			else 
			{
				//System.out.println("Wrong: predicted "+ bestMatchingCategory +" gt:"+ gtList  );
				countWrongSyn.increment();
			}
			System.out.println(i+" files are processed. Correctly: "+countCorrectSyn.value()+" Wrongly: "+countWrongSyn.value()+" Null: "+countNullSyn.value());

		};
	}

	

	public static void writeGeneratedDataToFile(String folderName, String data, int fileName ){
		String mainFolderName="TrainTFID_AG_"+numberOfSamples;
		File directory = new File(mainFolderName);
		if (! directory.exists()){
			directory.mkdir();
		}
		directory = new File(mainFolderName+File.separator+folderName);
		if (! directory.exists()){
			directory.mkdir();
		}
		FileUtil.writeDataToFile(Arrays.asList(data), directory+File.separator+ fileName,false);
	}
	private static void datasetGenerateFromTestSet() {
		String fileName="TrainTFID_AG_"+numberOfSamples;
		File directory = new File(fileName);
		if (! directory.exists()){
			directory.mkdir();
		}

		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);
		Map<String, List<String>> result = new HashedMap<String, List<String>>();
		//		List<String> dataset = new ArrayList<>(ReadTestDataset.read_AG_BasedOnType(AG_DataType.TITLEANDDESCRIPTION));
		Map<String, Double> mapCatValues = new HashedMap<String, Double>();
		//		for(String text:dataset) {
		//			String bestMatchingCategory = HeuristicBasedOnEntityVector.getBestMatchingCategory(text);
		//			String[] split = bestMatchingCategory.split("\t\t");
		//			mapCatValues.put(text+"\t\t"+split[0], Double.valueOf(split[1]));
		//			resultLog.info(text+"\t\t"+split[0]+"\t\t"+split[1]);
		//		}
		try {
			List<String>  lines = new ArrayList<>(FileUtils.readLines(new File("/home/rtue/eclipse-workspace/gwifi/log/classificationResults_AG",""), "utf-8"));
			for(String str:lines) {
				String[] split = str.split("\t\t");
				mapCatValues.put(split[0]+"\t\t"+split[1], Double.valueOf(split[2]));
			}

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		for (Category mainCat : setMainCategories) {
			String entityCat = WikipediaSingleton.getInstance().getArticle(mainCat.getTitle()).getTitle();
			List<String> lst = new ArrayList<String>();
			System.out.println(entityCat+" "+lst);
			result.put(entityCat, lst);
		}
		Map<String, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapCatValues));

		for(Entry <String, Double> e: sortedMap.entrySet() ) {
			String[] split = e.getKey().split("\t\t");
			String text = split[0];
			Double val = e.getValue();
			String entityCat = WikipediaSingleton.getInstance().getArticle(split[1]).getTitle();
			System.out.println(entityCat);
			List<String> lst = new ArrayList<String>(result.get(entityCat));
			if (lst.size()<numberOfSamples) {
				lst.add(text);
				result.put(entityCat, lst);
				secondLOG.info(entityCat+"\t"+text+"\t"+val);
				System.out.println((entityCat+"\t"+text+"\t"+val));
				//if(entityCat.equals(anObject))
			}
		}
		Print.printMap(result);
		calculateAccuracyBasedOnVectorSimilarity(fileName, result);
	}
	private static void calculateAccuracyBasedOnVectorSimilarity(String fileName, Map<String, List<String>> result) {
		File directory;
		TestBasedonSortTextDatasets test = new TestBasedonSortTextDatasets();
		Map<String, List<Category>> read_dataset_AG = test.read_dataset_AG(AG_DataType.TITLEANDDESCRIPTION,TRAIN_SET_AG);
		Map<String, Integer> mapResultWrongAssignedLabel = new HashMap<String, Integer>();;
		int i =0;
		for(Entry<String, List<String>> e: result.entrySet() ) {
			Category strObtainedCat=WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle(e.getKey());
			System.out.println(strObtainedCat);
			String folderName = e.getKey();
			directory = new File(fileName+File.separator+folderName);
			if (! directory.exists()){
				directory.mkdir();
			}
			List<String> lstData = new ArrayList<>(e.getValue());
			for(String s : lstData) {
				List<Category> gtlist = read_dataset_AG.get(s);

				if (gtlist.contains(strObtainedCat)) {
					countCorrect++;
					FileUtil.writeDataToFile(Arrays.asList(s), directory+File.separator+ ++i,false);
				}
				else if(gtlist.get(0).getTitle().equals("Sports")&&strObtainedCat.getTitle().equals("Sport")) {
					countCorrect++;
					FileUtil.writeDataToFile(Arrays.asList(s), directory+File.separator+ ++i,false);
				}
				else
				{
					int count = mapResultWrongAssignedLabel.containsKey(strObtainedCat.getTitle()) ? mapResultWrongAssignedLabel.get(strObtainedCat.getTitle()) : 0;
					mapResultWrongAssignedLabel.put(strObtainedCat.getTitle(), count + 1);
				}
			}
		}
		System.out.println("countCorrect "+countCorrect+"\nWrongly assigned labels");
		Print.printMap(mapResultWrongAssignedLabel);
	}
}
