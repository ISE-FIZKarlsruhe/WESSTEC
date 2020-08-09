package org.fiz.ise.gwifi.longText;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
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
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.LINE_2modelSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LabelsOfTheTexts;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.dataset.shorttext.test.HeuristicApproach;
import org.fiz.ise.gwifi.model.NewsgroupsArticle;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.test.longDocument.NewsgroupParser;
import org.fiz.ise.gwifi.test.longDocument.YovistoParser;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.Print;
import org.fiz.ise.gwifi.util.SentenceSegmentator;
import org.fiz.ise.gwifi.util.SynchronizedCounter;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.LexedTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.WordToSentenceProcessor;

public class TestBasedonLongTextDatasets {

	private final static Integer NUMBER_OF_THREADS=  Config.getInt("NUMBER_OF_THREADS",-1);
	private final static Dataset TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE"); 
	private static Wikipedia wikipedia = WikipediaSingleton.getInstance().wikipedia;
	private static CategorySingleton singCategory;
	private static SynchronizedCounter counterTruePositive;
	private static SynchronizedCounter counterFalsePositive;
	private static SynchronizedCounter counterProcessed;
	private static Map<Category, Integer> truePositive = new ConcurrentHashMap<>();
	private static Map<Category, Integer> falsePositive = new ConcurrentHashMap<>();
	private static Map<String, Category> falsePositiveResult = new ConcurrentHashMap<>();
	private static Map<String, Integer> mapMissClassified = new ConcurrentHashMap<>();
	private ExecutorService executor;
	private static List<String> lstCategory;
	private static int NUMBER_OF_SENTENCES_YOVISTO = Config.getInt("NUMBER_OF_SENTENCES_YOVISTO",-1);
	private static Map<Category, Set<Category>> mapCategories;
	long now = System.currentTimeMillis();

	public static void main(String[] args) {
		if (Config.getBoolean("LOAD_MODEL", false)) {
			LINE_modelSingleton.getInstance();
		}
		List<Integer> lst = new ArrayList<>();
		//lst.add(2);
		//		lst.add(3);
		//		lst.add(5);
		//		lst.add(10);
		lst.add(100000);

		for(int i : lst) {
			NUMBER_OF_SENTENCES_YOVISTO=i;
			TestBasedonLongTextDatasets test = new TestBasedonLongTextDatasets();
			Map<String,List<Category>> dataset = new HashMap<>(test.initializeDataset());
			//test.findAvgEntities(dataset);
			test.setCategoryList(dataset);
			test.startProcessingData(dataset);

		}

	}
	private void findAvgEntities(Map<String,List<Category>> dataset) {
		double numberOfEntities=0;
		double numberOfDocs=dataset.size();
		try {
			NLPAnnotationService service = AnnotationSingleton.getInstance().service;
			for(Entry<String,List<Category>> e : dataset.entrySet()) {
				List<Annotation> lstAnnotations = new ArrayList<>();
				service.annotate(e.getKey(), lstAnnotations);
				numberOfEntities+=lstAnnotations.size();
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("Avg Entities "+numberOfEntities/numberOfDocs);

	}


	public Map<String,List<Category>> initializeDataset() {
		TestBasedonLongTextDatasets test = new TestBasedonLongTextDatasets();
		Map<String,List<Category>> map=null;
		counterProcessed= new SynchronizedCounter();
		counterFalsePositive= new SynchronizedCounter();
		counterTruePositive= new SynchronizedCounter();

		if (TEST_DATASET_TYPE.equals(Dataset.YOVISTO)) {
			System.out.println("The dataset type is YOVISTO");
			System.out.println("Numvber of Sentences to be considered "+NUMBER_OF_SENTENCES_YOVISTO);
			map = new HashMap<>(test.dataset_YOVISTO(NUMBER_OF_SENTENCES_YOVISTO));
			System.out.println("map size "+map.size());
		}
		else if (TEST_DATASET_TYPE.equals(Dataset.TWENTYNEWS)) {
			test.dataset_20News();
		}
		else if (TEST_DATASET_TYPE.equals(Dataset.YOVISTO_SENTENCEBYSENTENCE_sentence)) {
			YovistoParser parser = new YovistoParser();
			parser.initializeVariables();
			System.out.println("Data Type: YOVISTO_SENTENCEBYSENTENCE_sentence number of Sentences "+ NUMBER_OF_SENTENCES_YOVISTO);
			map = new HashMap<>(parser.generateDataset_gwifi(NUMBER_OF_SENTENCES_YOVISTO));
			System.out.println("map size "+map.size());
		}
		return map;
	}
	public void setCategoryList(Map<String,List<Category>> map) {
		lstCategory= new ArrayList<>();
		for(Entry<String, List<Category>> e: map.entrySet()) {
			List<Category> temp = new ArrayList<>(e.getValue());
			for(Category c:temp) {
				if (!lstCategory.contains(c.getTitle())) {
					lstCategory.add(c.getTitle());
				}
			}
		}
		System.out.println("Size of Category list "+lstCategory.size());
		singCategory= CategorySingleton.getInstance(lstCategory);
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
	}

	public  Map<String,List<Category>> dataset_YOVISTO(int numberOfSentences) {
		Map<String,List<Category>> dataset=null;
		Map<Category, Integer> mapCount = new HashMap<>();
		int numberOfSentencesTotal=0;
		int countMultiWordCats=0;
		try {
			dataset = new HashMap<>();
			List<String> lines = FileUtils.readLines(new File(Config.getString("DATASET_TEST_YOVISTO","")), "utf-8");
			for(String line : lines) {
				String[] split = line.split("\t");
				String title = split[0];
				String[] categories = split[1].split(",");
				String content = split[2];
				String sentence=segment2Sentence(content,numberOfSentences);
				//			if (categories.length==1 && !categories[0].contains(" ")) { //58 is number of multi word categories I ignore them. 1452 is the total number of the dataset
				if (categories.length==1) { //58 is number of multi word categories I ignore them. 1452 is the total number of the dataset
					Category c = wikipedia.getCategoryByTitle(StringUtils.capitalize(categories[0]));
					if (c!=null) {
						numberOfSentencesTotal+=SentenceSegmentator.findNumberOfSentences(content);
						dataset.put(title+" "+sentence, Arrays.asList(c));
					}
					else {
						System.out.println(categories[0]);
					}
				}
			}
			System.out.println("Number of articles: "+dataset.size());
			System.out.println("Number of multi word cats: "+countMultiWordCats);
			System.out.println("Start processing");

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		int count =0;
		for (Map.Entry<Category, Integer> entry : mapCount.entrySet()) {
			System.out.println(entry.getKey().getTitle()+"\t"+entry.getValue());
			count+=entry.getValue();
		}
		System.out.println("total Number Of Sentence : "+numberOfSentencesTotal);
		System.out.println("Average number of Sentences : "+numberOfSentencesTotal/dataset.size());
		System.out.println(count);
		System.out.println("returned dataset size : " + dataset.size());
		return dataset;
	}
	//	public  Map<String,List<Category>> dataset_YOVISTO(int numberOfSentences) {
	//		Map<String,List<Category>> dataset=null;
	//		Map<Category, Integer> mapCount = new HashMap<>();
	//		int numberOfSentencesTotal=0;
	//		try {
	//			dataset = new HashMap<>();
	//			List<String> lines = FileUtils.readLines(new File(DATASET_TEST_YOVISTO), "utf-8");
	//			for(String line : lines) {
	//				String[] split = line.split("\t");
	//				String title = split[0];
	//				String[] categories = split[1].split(",");
	//				String content = split[2];
	//				String sentence=segment2Sentence(content,numberOfSentences);
	//				if (categories.length==1) {
	//					Category c = wikipedia.getCategoryByTitle(StringUtils.capitalize(categories[0]));
	//					if (c!=null) {
	//						if (!lstCategory.contains(c)) {
	//							lstCategory.add(c.getTitle());
	//						}
	//						mapCount.put(c, mapCount.containsKey(c) ? mapCount.get(c) + 1 : 1);
	//						numberOfSentencesTotal+=SentenceSegmentator.findNumberOfSentences(content);
	//						dataset.put(title+" "+sentence, Arrays.asList(c));
	//					}
	//				}
	//			}
	//			System.out.println("Number of articles: "+dataset.size());
	//			System.out.println("Start processing");
	//
	//		} catch (Exception e) {
	//			System.out.println(e.getMessage());
	//		}
	//		int count =0;
	//		for (Map.Entry<Category, Integer> entry : mapCount.entrySet()) {
	//			System.out.println(entry.getKey().getTitle()+"\t"+entry.getValue());
	//			count+=entry.getValue();
	//		}
	//		System.out.println("total Number Of Sentence : "+numberOfSentencesTotal);
	//		System.out.println("Average number of Sentences : "+numberOfSentencesTotal/count);
	//		System.out.println(count);
	//		System.out.println("returned dataset size" + dataset.size());
	//		return dataset;
	//	}
	public  void dataset_20News() {
		Map<String,List<Category>> dataset = new HashMap<>();
		Map<String, List<Category>> mapLabel = new HashMap<>(LabelsOfTheTexts.getLables_20News());
		try {
			NewsgroupParser parser = new NewsgroupParser(Config.getString("DATASET_TEST_20NEWS_RANDOM100",""));
			parser.parse();
			Map<String, List<NewsgroupsArticle>> mapArticles = new HashMap<String, List<NewsgroupsArticle>>(parser.getArticles());
			for(Entry <String, List<NewsgroupsArticle>> e: mapArticles.entrySet() ) {
				System.out.println(e.getKey()+": "+e.getValue().size());
				e.getValue().forEach(a -> {
					List<Category> gtList = new ArrayList<>(mapLabel.get(e.getKey()));
					//String content =a.getRawText();
					String content = segment2Sentence(a.getRawText(), 1);
					dataset.put(content, gtList);
				});
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		startProcessingData(dataset);
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
			Category bestMatchingCategory= HeuristicApproach.getBestMatchingCategory(description,gtList);
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
	public static String segment2Sentence(String text,int numOfSentence) {
		final List<CoreLabel> tokens = new ArrayList<CoreLabel>();
		final LexedTokenFactory<CoreLabel> tokenFactory = new CoreLabelTokenFactory();
		final PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<CoreLabel>(new StringReader(text), tokenFactory, "untokenizable=noneDelete");
		while (tokenizer.hasNext()) {
			tokens.add(tokenizer.next());
		}
		final List<List<CoreLabel>> sentences = new WordToSentenceProcessor<CoreLabel>().process(tokens);
		int end;
		int start = 0;
		StringBuffer resultSentences = new StringBuffer();
		final ArrayList<String> sentenceList = new ArrayList<String>();
		for (List<CoreLabel> sentence: sentences) {
			end = sentence.get(sentence.size()-1).endPosition();
			sentenceList.add(text.substring(start, end).trim());
			resultSentences.append(text.substring(start, end).trim()+" ");
			if (numOfSentence==sentenceList.size()) {
				return resultSentences.toString();
			}
			start = end;
		}
		return resultSentences.toString();
	}

	public static List<String> getLstCategory() {
		return lstCategory;
	}
}


