package org.fiz.ise.gwifi.dataset.shorttext.baseline;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hdfs.server.namenode.decommission_jsp;
import org.bytedeco.javacpp.presets.opencv_core.Str;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LabelsOfTheTexts;
import org.fiz.ise.gwifi.dataset.ReadDataset;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.dataset.shorttext.test.TestBasedonSortTextDatasets;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.test.longDocument.YovistoParser;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.Document;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.MergeTwoFiles;
import org.fiz.ise.gwifi.util.Print;
import org.fiz.ise.gwifi.util.WikipediaFilesUtil;

import com.hp.hpl.jena.sparql.pfunction.library.listIndex;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.model.Page.PageType;
import weka.gui.SysErrLog;

public class DatasetGeneration_TFIDF {

	private final static Integer NUMBER_OF_ARTICLES_RANDOM_PER_LABEL = Config.getInt("NUMBER_OF_ARTICLES_RANDOM_PER_LABEL", 0);
	private final static Dataset TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	private final static String WIKI_ALL_FILES= Config.getString("WIKI_ALL_FILES","");
	private final static String DATASET_TEST_AG= Config.getString("DATASET_TEST_AG","");
	private final static String DATASET_TEST_WEB= Config.getString("DATASET_TEST_WEB","");
	private final static String DATASET_TRAIN_AG= Config.getString("DATASET_TRAIN_AG","");
	private final static String DATASET_TRAIN_WEB= Config.getString("DATASET_TRAIN_WEB","");
	private final static Integer NUMBER_OF_ARTICLES_PER_LABEL=Config.getInt("NUMBER_OF_ARTICLES_PER_LABEL",0);
	private final static String DATASET_TEST_YOVISTO = Config.getString("DATASET_TEST_YOVISTO","");
	private static Wikipedia wikipedia = WikipediaSingleton.getInstance().wikipedia;
	public static void main(String[] args) {
		//generateTestSetTFIDF_AG(AG_DataType.TITLE);
		//generateTrainSetTFIDF();
		//generateTestSetTFIDF_WEB();
		//	generateTestSetTFIDF_YOVISTO();
		//generateTestSetTFIDF_AG();
		//splitDataset("TrainDataset_TFIDF_Yovisto",0.3);
		//generateDataSetTFIDF_YOVISTO(3);



		List<Integer> lst = new ArrayList<>();
//		lst.add(50);
//		lst.add(500);
		lst.add(1000000);
//		lst.add(1000);
//		lst.add(2000);
		for(int m : lst ) {
			for (int i = 1; i < 2; i++) {
				generateTrainSetPartitionTFIDF_AG(m, i, AG_DataType.TITLE);
			}
		}
	}

	private static void generateDataSetTFIDF_YOVISTO(Integer numberOfSentences) {
		int i=0;
		File directory = null;
		String content = null;
		try {
			List<String> lines = FileUtils.readLines(new File("/home/rtue/eclipse-workspace/Dataset_ShortTextClassification/Re__SciHi_blod_data/SciHi_articles_parsed"), "utf-8");
			for(String line : lines) {
				String[] split = line.split("\t");
				String[] categories = split[1].split(",");
				if (categories.length==1) {
					StringBuilder build = new StringBuilder();
					Category c = wikipedia.getCategoryByTitle(StringUtils.capitalize(categories[0]));
					if (c!=null) {
						List<String> sentences = new LinkedList<>(YovistoParser.generateSentences(split[2]));
						for(int j=0 ;j<numberOfSentences;j++) {
							String sentence = sentences.get(j);
							build.append(sentence);
						}
						content=build.toString();
						String folderName = c.getTitle();
						directory = new File("TrainDataset_TFIDF_"+numberOfSentences +"_sentences"+File.separator+folderName);
						if (! directory.exists()){
							System.out.println(directory);
							directory.mkdir();
						}
						System.out.println(directory+File.separator+i);
						FileUtil.writeDataToFile(Arrays.asList(content), directory+File.separator+i,false);
						i++;
						System.out.println(i);
					}
				}

			}
			System.out.println("number of lines processed " + i++);
		} catch (Exception e) {
			System.out.println(directory+File.separator+i+content);
			System.out.println(e.getMessage());
		}		
	}
	private static void generateTrainSetHandCraftedPartitionTFIDF_AG(int numberOfSamples,int folderNumber) {
		try {
			String nameOfDirectory="TrainTFID_AGOnlyTitles_fromOriginalDataset_";
			File directory = new File(nameOfDirectory+numberOfSamples+"_"+folderNumber);
			if (! directory.exists()){
				directory.mkdir();
			}
			Map<Integer, Category> mapLabel = new HashMap<>(LabelsOfTheTexts.getLables_AG_category());
			List<Integer> lstKeys = new ArrayList<>(mapLabel.keySet());
			Collections.shuffle(lstKeys);
			Map<Integer, Integer> mapRandomSamplePerLabel=new HashMap<>();
			int totalRandom =0;
			for (int i = 0; i < lstKeys.size(); i++) {
				Random randomGenerator = new Random();
				int index = randomGenerator.nextInt(numberOfSamples);
				for(Entry<Integer,Integer> e : mapRandomSamplePerLabel.entrySet()) {
					totalRandom+=e.getValue();
				}
				if ((totalRandom+index)<numberOfSamples) {
					mapRandomSamplePerLabel.put(lstKeys.get(i), index);
				}
				else if(numberOfSamples-totalRandom>0){
					mapRandomSamplePerLabel.put(lstKeys.get(i), numberOfSamples-totalRandom);
				}
			}
			if (checkSizeOfTheSamples(mapRandomSamplePerLabel, numberOfSamples)) {
				System.out.println("YES");
			}
			else {
				System.out.println("NO");
			}

			//			List<String> lines = FileUtils.readLines(new File(DATASET_TRAIN_AG), "utf-8");
			//			Collections.shuffle(lines);
			//			//			List<String> subList = new ArrayList<>(lines.subList(0, numberOfSamples));
			//			List<String> subList = new ArrayList<>(randomListGenerator(lines,numberOfSamples));
			//			Map<String, List<String>> mapResult = new HashMap<>();
			//
			//			String[] arrLines = new String[subList.size()];
			//			arrLines = subList.toArray(arrLines);
			//			int i=0;
			//			for (i = 0; i < arrLines.length; i++) {
			//				String[] split = arrLines[i].split("\",\"");
			//				String label = split[0].replace("\"", "");
			//				String title = split[1].replace("\"", "");
			//				String description = split[2].replace("\"", "");
			//
			//				List<String> temp ;
			//				if (mapResult.containsKey(label) ) {
			//					temp = new ArrayList<>(mapResult.get(label));
			//					temp.add(title);
			//					//					temp.add(title+" "+description);
			//				}
			//				else {
			//					temp = new ArrayList<>();
			//					temp.add(title);
			//					//					temp.add(title+" "+description);
			//				}
			//				mapResult.put(label, temp);
			//			}
			//			for(Entry<String, List<String>> e: mapResult.entrySet() ) {
			//				i=0;
			//				String folderName = mapLabel.get(Integer.parseInt(e.getKey())).getTitle();
			//				directory = new File(nameOfDirectory+numberOfSamples+"_"+folderNumber+File.separator+folderName);
			//				if (! directory.exists()){
			//					directory.mkdir();
			//				}
			//				List<String> lstData = new ArrayList<>(e.getValue());
			//				for(String s : lstData) {
			//					FileUtil.writeDataToFile(Arrays.asList(s), directory+File.separator+ ++i,false);
			//				}
			//			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	private static boolean checkSizeOfTheSamples(Map<Integer, Integer> mapRandomSamplePerLabel,int numberOfSample) {
		int totalMap =0;
		for(Entry<Integer, Integer> e: mapRandomSamplePerLabel.entrySet()) {
			totalMap+=e.getValue();
		}
		if (totalMap==numberOfSample) {
			return true;
		}
		return false;
	}


	private static void splitDataset(String path,double percent) {
		try {
			File[] listOfFoleders = new File(path).listFiles();
			for (int i = 0; i < listOfFoleders.length; i++) {
				File[] listOfFiles = new File(listOfFoleders[i].getPath()).listFiles();
				if (listOfFiles.length>10) {
					int numberOfFilesTest=(int) Math.round(listOfFiles.length*percent);
					int numberOfFilesTrain=listOfFiles.length-numberOfFilesTest;
					int count=0;
					for (int j = 0; j < numberOfFilesTrain; j++) {
						String folderName =listOfFoleders[i].getName();
						File directory = new File("TrainDataset_TFIDF_Yovisto_split_train_more10"+File.separator+folderName);
						if (! directory.exists()){
							System.out.println(directory+" test:"+numberOfFilesTest+" train:"+numberOfFilesTrain+" total "+listOfFiles.length);
							directory.mkdir();
						}
						List<String> lines = FileUtils.readLines(new File(listOfFiles[j].getPath()), "utf-8");
						String content = lines.get(0);
						FileUtil.writeDataToFile(Arrays.asList(content), directory+File.separator+listOfFiles[j].getName(),false);
						count++;
					}
					for (int j = numberOfFilesTrain; j < listOfFiles.length; j++) {
						String folderName =listOfFoleders[i].getName();
						File directory = new File("TrainDataset_TFIDF_Yovisto_split_test_more10"+File.separator+folderName);
						if (! directory.exists()){
							System.out.println(directory);
							directory.mkdir();
						}
						List<String> lines = FileUtils.readLines(new File (listOfFiles[j].getPath()), "utf-8");
						String content = lines.get(0);
						FileUtil.writeDataToFile(Arrays.asList(content), directory+File.separator+listOfFiles[j].getName(),false);
						count++;
					}
					if (count!=listOfFiles.length) {
						System.out.println("HATA");
						break;
					}
				}
			}

		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	private static void generateTestSetTFIDF_YOVISTO() {
		int i=0;
		File directory = null;
		String content = null;
		try {
			//			List<String> lines = FileUtils.readLines(new File(DATASET_TEST_YOVISTO), "utf-8");
			List<String> lines = FileUtils.readLines(new File("/home/rtue/eclipse-workspace/Dataset_ShortTextClassification/Re__SciHi_blod_data/SciHi_articles_parsed"), "utf-8");
			for(String line : lines) {
				String[] split = line.split("\t");
				String[] categories = split[1].split(",");
				content = split[2];
				if (categories.length==1) {
					Category c = wikipedia.getCategoryByTitle(StringUtils.capitalize(categories[0]));
					if (c!=null) {
						String folderName = c.getTitle();
						directory = new File("TrainDataset_TFIDF_Yovisto"+File.separator+folderName);
						if (! directory.exists()){
							System.out.println(directory);
							directory.mkdir();
						}
						System.out.println(directory+File.separator+i);
						FileUtil.writeDataToFile(Arrays.asList(content), directory+File.separator+i,false);
						i++;
						System.out.println(i);
					}
				}

			}
			System.out.println("number of lines processed " + i++);
		} catch (Exception e) {
			System.out.println(directory+File.separator+i+content);
			System.out.println(e.getMessage());
		}		
	}
	private static void generateTrainSetTFIDF() {
		DatasetGeneration_TFIDF test = new DatasetGeneration_TFIDF();
		Map<Category, Set<Integer>> randomArticles = new HashMap<>(test.generateRandomArticleIDs());
		List<Integer> IDs = new ArrayList<>();
		for (Entry<Category, Set<Integer>> e: randomArticles.entrySet()) {
			IDs.addAll(e.getValue());
		}
		System.out.println("IDs size "+IDs.size());

		//		for(Entry<Integer,Document> e: wikipediaDocuments.entrySet()) {
		//			System.out.println(e.getKey()+" "+e.getValue().getId()+" "+e.getValue().getContent());
		//		}
		Map<Integer,Document> wikipediaDocuments = new HashMap<>(test.readWikipediaDocumentsBasedOnIds(IDs));
		test.compareWriteToFile(wikipediaDocuments,randomArticles);
	}
	private static void generateTestSetTFIDF_WEB() {
		try {
			//			List<String> lines = FileUtils.readLines(new File(DATASET_TEST_WEB), "utf-8");
			List<String> lines = FileUtils.readLines(new File("/home/rima/playground/GeneralFiles/gwifi/Dataset_ShortTextClassification/data-web-snippets/train.txt"), "utf-8");
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			for (int i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split(" ");
				String label = split[split.length-1];
				String snippet = arrLines[i].substring(0, arrLines[i].length()-(label).length()).trim();
				String folderName = label;
				//				File directory = new File("TestDataset_TFIDF_WEB"+File.separator+folderName);
				File directory = new File("TrainDataset_TFIDF_WEB_original"+File.separator+folderName);
				if (! directory.exists()){
					directory.mkdir();
				}
				FileUtil.writeDataToFile(Arrays.asList(snippet), directory+File.separator+i,false);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	private static void generateTrainSetPartitionTFIDF_WEB(int numberOfSamples,int folderNumber) {
		try {
			File directory = new File("TrainTFID_WEB_fromOriginalDataset_"+numberOfSamples+"_"+folderNumber);
			if (! directory.exists()){
				directory.mkdir();
			}
			List<String> lines = FileUtils.readLines(new File(DATASET_TRAIN_WEB), "utf-8");
			Collections.shuffle(lines);
			List<String> subList = new ArrayList<>(lines.subList(0, numberOfSamples));
			Map<String, List<String>> mapResult = new HashMap<>();
			String[] arrLines = new String[subList.size()];
			arrLines = subList.toArray(arrLines);
			int i=0;
			for (i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split(" ");
				String label = split[split.length-1];
				String snippet = arrLines[i].substring(0, arrLines[i].length()-(label).length()).trim();
				List<String> temp ;
				if (mapResult.containsKey(label) ) {
					temp = new ArrayList<>(mapResult.get(label));
					temp.add(snippet);
				}
				else {
					temp = new ArrayList<>();
					temp.add(snippet);
				}
				mapResult.put(label, temp);
			}
			for(Entry<String, List<String>> e: mapResult.entrySet() ) {
				i=0;
				String folderName = e.getKey();
				directory = new File("TrainTFID_WEB_fromOriginalDataset_"+numberOfSamples+"_"+folderNumber+File.separator+folderName);
				if (! directory.exists()){
					directory.mkdir();
				}
				List<String> lstData = new ArrayList<>(e.getValue());
				for(String s : lstData) {
					FileUtil.writeDataToFile(Arrays.asList(s), directory+File.separator+ ++i,false);
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}
	private static void generateTrainSetPartitionTFIDF_AG(int numberOfSamples,int folderNumber, AG_DataType type) {
		try {
			String nameOfDirectory="TrainTFID_AGOnlyTitles_fromOriginalDataset_";

			//			File directory = new File("TrainTFID_AG_fromOriginalDataset_"+numberOfSamples+"_"+folderNumber);
			File directory = new File(nameOfDirectory+numberOfSamples+"_"+folderNumber);
			if (! directory.exists()){
				directory.mkdir();
			}
			
			List<String> lines = FileUtils.readLines(new File(DATASET_TRAIN_AG), "utf-8");
			Collections.shuffle(lines);
			//			List<String> subList = new ArrayList<>(lines.subList(0, numberOfSamples));
			List<String> subList = new ArrayList<>(randomListGenerator(lines,numberOfSamples));
			Map<String, List<String>> mapResult = new HashMap<>();
			Map<Integer, Category> mapLabel = new HashMap<>(LabelsOfTheTexts.getLables_AG_category());
			String[] arrLines = new String[subList.size()];
			arrLines = subList.toArray(arrLines);
			int i=0;
			for (i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split("\",\"");
				String label = split[0].replace("\"", "");
				String title = split[1].replace("\"", "");
				String description = split[2].replace("\"", "");
				String strFinal ="";
				
				if (type.equals(AG_DataType.TITLE)) {
					strFinal=title;
				}
				else if (type.equals(AG_DataType.DESCRIPTION)) {
					strFinal=description;
				}
				else if (type.equals(AG_DataType.TITLEANDDESCRIPTION)) {
					strFinal=title+" "+description;
				}
				List<String> temp ;
				if (mapResult.containsKey(label) ) {
					temp = new ArrayList<>(mapResult.get(label));
					temp.add(strFinal);
				}
				else {
					temp = new ArrayList<>();
					temp.add(strFinal);
				}
				mapResult.put(label, temp);
			}
			for(Entry<String, List<String>> e: mapResult.entrySet() ) {
				i=0;
				String folderName = mapLabel.get(Integer.parseInt(e.getKey())).getTitle();
				directory = new File(nameOfDirectory+numberOfSamples+"_"+folderNumber+File.separator+folderName);
				if (! directory.exists()){
					directory.mkdir();
				}
				List<String> lstData = new ArrayList<>(e.getValue());
				for(String s : lstData) {
					FileUtil.writeDataToFile(Arrays.asList(s), directory+File.separator+ ++i,false);
				}
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	private static List<String> randomListGenerator(List<String> lst, int size) {
		List<String> result = new ArrayList<>();
		if (size>= lst.size()) {
			return lst;
		}
		
		while (result.size()<size) {
			Random randomGenerator = new Random();
			int index = randomGenerator.nextInt(lst.size());
			String item = lst.get(index);
			if (!result.contains(item)) {
				result.add(item);
			}
		}
		System.out.println(result.size());
		return result;
	}
	private static void generateTestSetTFIDF_AG(AG_DataType type) {
		try {
			List<String> lines = FileUtils.readLines(new File(DATASET_TEST_AG), "utf-8");
			Map<Integer, Category> mapLabel = new HashMap<>(LabelsOfTheTexts.getLables_AG_category());
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			int i=0;
			String strmainDirectory="TestTFID_AG_fromOriginalDataset_"+type.toString();
			File mainDirectory = new File(strmainDirectory);
			mainDirectory.mkdir();
			for (i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split("\",\"");
				String label = split[0].replace("\"", "");
				String title = split[1].replace("\"", "");
				String description = split[2].replace("\"", "");
				String folderName = mapLabel.get(Integer.parseInt(label)).getTitle();
				File directory = new File(strmainDirectory+File.separator+folderName);
				if (! directory.exists()){
					directory.mkdir();
				}
				if (type.equals(AG_DataType.TITLE)) {
					FileUtil.writeDataToFile(Arrays.asList(title), directory+File.separator+i,false);
				}
				else if (type.equals(AG_DataType.DESCRIPTION)) {
					FileUtil.writeDataToFile(Arrays.asList(description), directory+File.separator+i,false);
				}
				else if (type.equals(AG_DataType.TITLEANDDESCRIPTION)) {
					FileUtil.writeDataToFile(Arrays.asList(title+" "+description), directory+File.separator+i,false);
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	private static void generateTrainSetTFIDF_AG_BasedOnCertainNumber(int numberOfFilesPerCategory) {
		try {
			List<String> lines = FileUtils.readLines(new File(DATASET_TRAIN_AG), "utf-8");
			Map<String, List<String>> mapResult = new HashMap<>();
			Map<Integer, Category> mapLabel = new HashMap<>(LabelsOfTheTexts.getLables_AG_category());
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			int i=0;
			for (i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split("\",\"");
				String label = split[0].replace("\"", "");
				String title = split[1].replace("\"", "");
				String description = split[2].replace("\"", "");

				List<String> temp ;
				if (mapResult.containsKey(label) && mapResult.get(label).size()<numberOfFilesPerCategory) {
					temp = new ArrayList<>(mapResult.get(label));
					temp.add(title+" "+description);
				}
				else {
					temp = new ArrayList<>();
					temp.add(title+" "+description);
				}
				mapResult.put(label, temp);
			}
			for(Entry<String, List<String>> e: mapResult.entrySet() ) {
				i=0;
				String folderName = mapLabel.get(Integer.parseInt(e.getKey())).getTitle();
				File directory = new File("TrainTFID_AG_fromOriginalDataset_50"+File.separator+folderName);
				if (! directory.exists()){
					directory.mkdir();
				}
				List<String> lstData = new ArrayList<>(e.getValue());
				for(String s : lstData) {
					FileUtil.writeDataToFile(Arrays.asList(s), directory+File.separator+ ++i,false);
				}
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	private void compareWriteToFile(Map<Integer,Document> wikipediaDocuments,Map<Category, Set<Integer>> randomArticles) {
		int countNotContain =0;
		int countContains=0;
		String folder ="TrainTFIDF";
		Map<Category, Integer> map = new HashMap<>();
		try {
			File directory = new File(folder);
			FileUtils.deleteDirectory(directory);
			directory.mkdir();
			System.out.println("Wikipedia size "+wikipediaDocuments.size());
			for(Entry<Category, Set<Integer>> e : randomArticles.entrySet() ) {
				directory = new File(folder+File.separator+e.getKey().getTitle());
				directory.delete();
				directory.mkdir();
				for(Integer i : e.getValue()) {
					if (wikipediaDocuments.containsKey(i)) {
						if (map.containsKey(e.getKey())) {
							int count = map.get(e.getKey());
							if (count<(NUMBER_OF_ARTICLES_PER_LABEL+1)) {
								FileUtil.writeDataToFile(Arrays.asList(wikipediaDocuments.get(i).toString()), directory+File.separator+i+".txt",false);
								map.put(e.getKey(), ++count);
							}
						}
						else {
							map.put(e.getKey(), 1);
							FileUtil.writeDataToFile(Arrays.asList(wikipediaDocuments.get(i).toString()), directory+File.separator+i+".txt",false);
						}
						countContains++;
					}
					else {
						countNotContain++;
					}
				}
				Print.printMap(map);
				System.out.println("Does not contain size "+countNotContain);
				System.out.println("contains "+ countContains);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	private Map<Integer,Document> readWikipediaDocumentsBasedOnIds(List<Integer> IDs) {
		Map<Integer,Document> result= new HashMap<>();
		final List<Document> resultDocuments = new ArrayList<>(WikipediaFilesUtil.getDocuments(WIKI_ALL_FILES))	;//all wikipedia article is in the list
		System.out.println("Total wikipedia article size "+ resultDocuments.size());
		for(Document d : resultDocuments) { //Based on random article IDs we get their corresponding wikipedia article 
			if (IDs.contains(d.getId())) {
				result.put(d.getId(),d);
			}
		}
		System.out.println("Total wikipedia article size after reading the all wikipedia "+ result.size());
		return result;
	}
	private Map<Category, Set<Integer>> generateRandomArticleIDs(){
		Map<Category, Set<Integer>> result = new HashMap<>();
		Map<Category, Set<Category>> mapCategories = new HashMap<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).mapMainCatAndSubCats);
		for(Entry<Category, Set<Category>> e : mapCategories.entrySet()) {//iterate over all the main cates and get their child articles
			List<Article> dirtyChildArticles = new ArrayList<>();
			dirtyChildArticles.addAll(Arrays.asList(e .getKey().getChildArticles()));
			for(Category cCat : e.getValue()) {
				dirtyChildArticles.addAll(Arrays.asList(cCat.getChildArticles()));//per category all child articles are stored in childArticles
			}
			List<Article> cleanChildArticles = new ArrayList<>();
			for(Article a: dirtyChildArticles) {
				if (a.getType().equals((PageType.article))) {
					cleanChildArticles.add(a);
				}
			}
			System.out.println("For category "+e.getKey().getTitle()+" number of dirtyList(contains disambiguation) articles: "+dirtyChildArticles.size()+" "
					+ "\nafter filtering the disambiguation pages the size is "+cleanChildArticles.size());
			List<Integer> random = new ArrayList<>(MergeTwoFiles.random(0, cleanChildArticles.size()-1, NUMBER_OF_ARTICLES_RANDOM_PER_LABEL));//Based on number of child articles and and the maximum sizes of the random articles
			Set<Integer> temp = new HashSet<>();//fist generate random unique numbers and then get the corresponding articles 
			for(Integer i : random) {
				temp.add(cleanChildArticles.get(i).getId());
			}
			result.put(e.getKey(), temp);//add to map each category and its random articles 
		}
		System.out.println("Random article size "+ result.size()); 
		if (result.containsKey(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science"))&&result.containsKey(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Education"))) {
			List<Integer> sci = new ArrayList<>(result.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science")));
			int size=sci.size();
			List<Integer> tech = new ArrayList<>(result.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Education")));
			Set<Integer> merge = new HashSet<>(sci.subList(0, size/2));
			merge.addAll(tech.subList(0, size/2));
			System.out.println("Size of the merger after merging the science and education random IDs: "+merge.size());
			result.remove(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science"));
			result.put(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Education"), merge);
		}
		else {
			System.err.println("Random Map does not contain all the categories");
			System.exit(1);
		}
		if (result.containsKey(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Politics"))&&result.containsKey(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Society"))) {
			List<Integer> sci = new ArrayList<>(result.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Politics")));
			int size=sci.size();
			List<Integer> tech = new ArrayList<>(result.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Society")));
			Set<Integer> merge = new HashSet<>(sci.subList(0, size/2));
			merge.addAll(tech.subList(0, size/2));
			System.out.println("Size of the merger after merging the Society and Politics random IDs: "+merge.size());
			result.remove(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Politics"));
			result.put(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Society"), merge);
		}
		else {
			System.err.println("Random Map does not contain all the categories");
			System.exit(1);
		}
		if (result.containsKey(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Culture"))&&result.containsKey(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Arts"))&&result.containsKey(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Entertainment"))) {
			List<Integer> sci = new ArrayList<>(result.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Culture")));
			int size=sci.size();
			List<Integer> ent = new ArrayList<>(result.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Entertainment")));
			List<Integer> arts = new ArrayList<>(result.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Arts")));
			Set<Integer> merge = new HashSet<>(sci.subList(0, size/3));
			merge.addAll(ent.subList(0, size/3));
			merge.addAll(arts.subList(0, size/3));
			System.out.println("Size of the merger after merging the Entertainment, Culture and Arts random IDs: "+merge.size());
			result.remove(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Culture"));
			result.remove(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Entertainment"));
			result.put(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Arts"), merge);
		}
		else {
			System.err.println("Random Map does not contain all the categories");
			System.exit(1);
		}




		//		/*
		//		 * Since science and technology is considered as a one class then we can devide each class elemnets of list into half and then merge them
		//		 */
		//		if (result.containsKey(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science"))&&result.containsKey(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Technology"))) {
		//			List<Integer> sci = new ArrayList<>(result.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science")));
		//			int size=sci.size();
		//			List<Integer> tech = new ArrayList<>(result.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Technology")));
		//			Set<Integer> merge = new HashSet<>(sci.subList(0, size/2));
		//			merge.addAll(tech.subList(0, size/2));
		//			System.out.println("Size of the merger after merging the science and technology random IDs: "+merge.size());
		//			result.remove(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science"));
		//			result.put(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Technology"), merge);
		//		}
		//		else {
		////			System.err.println("Random Map does not contain all the categories");
		////			System.exit(1);
		//		}
		return result;
	}
}
