package org.fiz.ise.gwifi.dataset;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.util.CharacterUtils;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.FilteredWikipediaPagesSingleton;
import org.fiz.ise.gwifi.Singleton.GoogleModelSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.assignLabels.AssignLabelsBasedOnConfVecSimilarity;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.model.EmbeddingModel;
import org.fiz.ise.gwifi.test.longDocument.BasedOnWordsCategorize;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.Print;
import org.fiz.ise.gwifi.util.SentenceSegmentator;
import org.fiz.ise.gwifi.util.StopWordRemoval;
import org.fiz.ise.gwifi.util.StringUtil;
import org.fiz.ise.gwifi.util.SynchronizedCounter;
import org.fiz.ise.gwifi.util.VectorUtil;
import org.openjena.atlas.lib.AlarmClock;

import com.mongodb.util.Hash;

import TEST.CharactersUtils;
import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.LexedTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;

public class AnalyseDataset {
	private final static Dataset TEST_DATASET_TYPE= Dataset.WEB_SNIPPETS;//Config.getEnum("TEST_DATASET_TYPE");
	private static final String DATASET_DBP_TRAIN = Config.getString("DATASET_DBP_TRAIN","");
	private static final String DATASET_DBP_TRAIN_CATEGORIZED_D2Vec = Config.getString("DATASET_DBP_TRAIN_CATEGORIZED_D2Vec","");
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");
	private static final String DATASET_TRAIN_SNIPPETS = Config.getString("DATASET_TRAIN_SNIPPETS","");
	private final static Integer NUMBER_OF_THREADS= Config.getInt("NUMBER_OF_THREADS",-1);
	public static final Map<String,List<String>> CACHE = new ConcurrentHashMap<>();
	static Map<String,Integer> result = new ConcurrentHashMap<>();
	private static ExecutorService executor;

	public static void main(String[] args) throws Exception {
	
	}

	
	public static Map<String, List<Article>> readLabelAssignment_AG_article(String fName) {
		Map<String,  List<Article>> result = new HashMap<String, List<Article>>();
		try {	
			List<String> lines = FileUtils.readLines(new File(fName), "utf-8");
			for(String line : lines) {
				String[] split = line.split("\t");
				Article articleByTitle = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(split[1]);
				if (articleByTitle==null) {
					articleByTitle = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(split[1].split(": ")[1]);
				}
				List<Article> temp = new ArrayList<Article>();
				temp.add(articleByTitle);
				result.put(split[0],temp);
			}
			System.out.println("Size of label Ass "+result.size());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	public static void compareTwoFiles_d2vec(Dataset dset, String gtFile, String cFile) {
		Map<String, List<Article>> map_gt =  null;
		Map<String, List<Article>> map_categorized = null;
		if (dset.equals(Dataset.DBpedia)) {
			map_gt = ReadDataset.read_dataset_DBPedia_SampleLabel(gtFile);
			map_categorized = ReadDataset.read_dataset_Doc2Vec_categorized(Dataset.DBpedia,cFile);
		}
		int countCorrect=0;
		int countWrong=0;
		int countNotInDataset=0;
		for(Entry <String, List<Article>> e: map_gt.entrySet()) {
			List<Article> list = map_categorized.get(e.getKey());
			if (list==null) {
				countNotInDataset++;
			}
			else if (list.contains(e.getValue().get(0))) {
				countCorrect++;
			}
			else {
				countWrong++;
			}
		}
		System.out.println("File name: "+cFile);
		System.out.println("Size of the original dataset: "+map_gt.size());
		System.out.println("Size of the categorized dataset: "+map_categorized.size());

		System.out.println("countCorrect: "+countCorrect);
		System.out.println("countWrong: "+countWrong);
		System.out.println("countNotInDataset: "+countNotInDataset);
		System.out.println("accuracy: "+countCorrect*1.0/(countWrong+countCorrect)*1.0);
		System.out.println("***********************");

	}
	public static void findMostSimilarEntitesIDsForDatasetBasedOnDatasetVector(List<String> lstAllAnnotation, String fName) {
		System.out.println("Start running: "+"findMostSimilarEntitesForDatasetBasedOnSentenceVector");
		Set<Article> articles = FilteredWikipediaPagesSingleton.getInstance().articles;   
		Map<String,Double> result = new HashMap<>();

		List<String> listWithoutNulls = lstAllAnnotation.parallelStream()
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		double[] docVec= VectorUtil.getSentenceVector(listWithoutNulls, LINE_modelSingleton.getInstance().lineModel);

		for(Article a : articles) {
			if (!StringUtil.isNumeric(a.getTitle())&&LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(a.getId()))) {
				result.put(a.getTitle(), BasedOnWordsCategorize.getSimilarity(EmbeddingModel.LINE_Ent_Ent,docVec, String.valueOf(a.getId())));
			}
		}
		System.out.println("Start writing..");
		Map<String, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(result));
		FileUtil.writeDataToFile(sortedMap,fName);
	}
	public static void findMostSimilarEntitesForDatasetBasedOnDatasetVector(List<Annotation> lstAllAnnotation, String fName) {
		System.out.println("Start running: "+"findMostSimilarEntitesForDatasetBasedOnSentenceVector");
		Set<Article> articles = FilteredWikipediaPagesSingleton.getInstance().articles;   
		int i =0;
		Map<String,Double> result = new HashMap<>();

		List<String> annoID = new ArrayList<>();
		for(Annotation a: lstAllAnnotation) {
			if (!StringUtil.isNumeric(a.getTitle())) {
				annoID.add(String.valueOf(a.getId()));
			}
		}
		List<String> listWithoutNulls = annoID.parallelStream()
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		double[] docVec= VectorUtil.getSentenceVector(listWithoutNulls, LINE_modelSingleton.getInstance().lineModel);

		for(Article a : articles) {
			result.put(a.getTitle(), BasedOnWordsCategorize.getSimilarity(EmbeddingModel.LINE_Ent_Ent,docVec, String.valueOf(a.getId())));
			//System.out.println(++i);
		}
		System.out.println("Start writing..");
		Map<String, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(result));
		FileUtil.writeDataToFile(sortedMap,fName);
	}
	public static void findMostSimilarWordsForDatasetBasedOnDatasetVector(List<String> dSet, String fName) {
		System.out.println("Start running: "+"findMostSimilarWordsForDatasetBasedOnSentenceVector");
		int i =0;
		Map<String,Double> result = new HashMap<>();
		List<String> words = new ArrayList<String>();
		for (String str : dSet) {
			List<String> tokinizeString = StringUtil.tokinizeString(StopWordRemoval.removeStopWords(str));
			words.addAll(tokinizeString);
		}

		double[] docVec= VectorUtil.getSentenceVector(words, GoogleModelSingleton.getInstance().google_model);
		VocabCache<VocabWord> vocab = GoogleModelSingleton.getInstance().google_model.vocab();
		List<String> stopWords = Arrays.asList(StopWordRemoval.stopwords);

		for (String w : vocab.words()) {
			if (!stopWords.contains(w)&& !w.contains("_")&& !w.contains("#")) {
				result.put(w, BasedOnWordsCategorize.getSimilarity(EmbeddingModel.GOOGLE, docVec, w));
			}
		}
		System.out.println("Size of the dataset for word-doc: "+result.size());

		System.out.println("Start writing..");
		Map<String, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(result));
		FileUtil.writeDataToFile(sortedMap,fName);
	}
	public static void findMostSimilarEntitesForDataset(List<String> dataset, Dataset dName, String fName ) {
		List<Annotation> lstAllAnnotation = new ArrayList<>(AnnonatationUtil.findAnnotationAll(dataset));
		System.out.println("Size of the annotations: "+ lstAllAnnotation.size());
		int cCount=0;
		if (dName.equals(Dataset.MR)){
			for(Annotation a : lstAllAnnotation) {
				if (!AnnonatationUtil.getEntityBlackList_MR().contains(a.getId())) {
					if (!CACHE.containsKey(a.getTitle())) {
						List<String> lstArt = new ArrayList<> (LINE_modelSingleton.getInstance().lineModel.wordsNearest(String.valueOf(a.getId()), 10));
						if (lstArt.size()==0) {
							System.out.println("Size is zero: "+a.getTitle()+" "+a.getId());
						}
						CACHE.put(a.getTitle(), lstArt);
					}
					//System.out.println(fName+" CACHE size "+CACHE.size()+" count: "+cCount);
					List<String> lstArt = new ArrayList<String>(CACHE.get(a.getTitle())); 
					for(String s : lstArt) {
						if (WikipediaSingleton.getInstance().wikipedia.getArticleById(Integer.valueOf(s))!=null) {
							String entity = WikipediaSingleton.getInstance().wikipedia.getArticleById(Integer.valueOf(s)).getTitle();
							int count = result.containsKey(entity) ? result.get(entity) : 0;
							result.put(entity, count + 1);
						}
					}
					cCount++;
				}
			}
		}
		else if (dName.equals(Dataset.YAHOO)) {
			for(Annotation a : lstAllAnnotation) {
				if(!AnnonatationUtil.getEntityBlackList_Yahoo().contains(a.getId())
						&&!StringUtil.isNumeric(a.getTitle())) {
					if (!CACHE.containsKey(a.getTitle())) {
						List<String> lstArt = new ArrayList<> (LINE_modelSingleton.getInstance().lineModel.wordsNearest(String.valueOf(a.getId()), 10));
						if (lstArt.size()==0) {
							System.out.println("Size is zero: "+a.getTitle()+" "+a.getId());
						}
						CACHE.put(a.getTitle(), lstArt);
					}
					//System.out.println(fName+" CACHE size "+CACHE.size()+" count: "+cCount);
					List<String> lstArt = new ArrayList<String>(CACHE.get(a.getTitle())); 
					for(String s : lstArt) {
						if (WikipediaSingleton.getInstance().wikipedia.getArticleById(Integer.valueOf(s))!=null) {
							String entity = WikipediaSingleton.getInstance().wikipedia.getArticleById(Integer.valueOf(s)).getTitle();
							int count = result.containsKey(entity) ? result.get(entity) : 0;
							result.put(entity, count + 1);
						}
					}
					cCount++;
				}
			}
		}
		else if (dName.equals(Dataset.DBpedia)) {
			System.out.println("Analysing DBpedia Dataset annotations with filtering");
			try {
				int count=0;
				LINE_modelSingleton.getInstance();
				executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);		
				for(Annotation a : lstAllAnnotation) {
					executor.execute(handleAnnotation(a, count++));
				}
				executor.shutdown();
				executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Map<String,Integer> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(result));
		System.out.println("Start writing..");
		FileUtil.writeDataToFile(sortedMap, fName+"_most similar10EntitiesForEachAn");
		System.out.println("Finished writing: " +fName+"_most similar10EntitiesForEachAn");
	}

	private static  Runnable handleAnnotation(Annotation a, int aCount)  {
		return () -> {
			try {
				if(!AnnonatationUtil.getEntityBlackList_DBp().contains(a.getId())
						&&!StringUtil.isNumeric(a.getTitle())) {
					if (!CACHE.containsKey(a.getTitle())) {
						List<String> lstArt = new ArrayList<> (LINE_modelSingleton.getInstance().lineModel.wordsNearest(String.valueOf(a.getId()), 10));
						if (lstArt.size()==0) {
							System.out.println("Size is zero: "+a.getTitle()+" "+a.getId());
						}
						CACHE.put(a.getTitle(), lstArt);
					}
					System.out.println(" CACHE size "+CACHE.size()+" count: "+aCount);
					List<String> lstArt = new ArrayList<String>(CACHE.get(a.getTitle())); 
					for(String s : lstArt) {
						if (WikipediaSingleton.getInstance().wikipedia.getArticleById(Integer.valueOf(s))!=null) {
							String entity = WikipediaSingleton.getInstance().wikipedia.getArticleById(Integer.valueOf(s)).getTitle();
							int count = result.containsKey(entity) ? result.get(entity) : 0;
							result.put(entity, count + 1);
						}
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		};
	}
	//	public static void findMostSimilarEntitesForDataset(List<String> dataset, Dataset dName, String fName ) {
	//		Map<String,Integer> result = new HashMap<>();
	//		List<Annotation> lstAllAnnotation = new ArrayList<>(AnnonatationUtil.findAnnotationAll(dataset));
	//		System.out.println("Size of the annotations: "+ lstAllAnnotation.size());
	//		int cCount=0;
	//		for(Annotation a : lstAllAnnotation) {
	//			if (dName.equals(Dataset.MR)&&!AnnonatationUtil.getEntityBlackList_MR().contains(a.getId())) {
	//				if (!CACHE.containsKey(a.getTitle())) {
	//					List<String> lstArt = new ArrayList<> (LINE_modelSingleton.getInstance().lineModel.wordsNearest(String.valueOf(a.getId()), 10));
	//					if (lstArt.size()==0) {
	//						System.out.println("Size is zero: "+a.getTitle()+" "+a.getId());
	//					}
	//					CACHE.put(a.getTitle(), lstArt);
	//				}
	//				System.out.println(fName+" CACHE size "+CACHE.size()+" count: "+cCount);
	//				List<String> lstArt = new ArrayList<String>(CACHE.get(a.getTitle())); 
	//				for(String s : lstArt) {
	//					if (WikipediaSingleton.getInstance().wikipedia.getArticleById(Integer.valueOf(s))!=null) {
	//						String entity = WikipediaSingleton.getInstance().wikipedia.getArticleById(Integer.valueOf(s)).getTitle();
	//						int count = result.containsKey(entity) ? result.get(entity) : 0;
	//						result.put(entity, count + 1);
	//					}
	//				}
	//				cCount++;
	//			}
	//
	//			if (dName.equals(Dataset.YAHOO)&&!AnnonatationUtil.getEntityBlackList_Yahoo().contains(a.getId())
	//					&&!StringUtil.isNumeric(a.getTitle())) {
	//				if (!CACHE.containsKey(a.getTitle())) {
	//					List<String> lstArt = new ArrayList<> (LINE_modelSingleton.getInstance().lineModel.wordsNearest(String.valueOf(a.getId()), 10));
	//					if (lstArt.size()==0) {
	//						System.out.println("Size is zero: "+a.getTitle()+" "+a.getId());
	//					}
	//					CACHE.put(a.getTitle(), lstArt);
	//				}
	//				System.out.println(fName+" CACHE size "+CACHE.size()+" count: "+cCount);
	//				List<String> lstArt = new ArrayList<String>(CACHE.get(a.getTitle())); 
	//				for(String s : lstArt) {
	//					if (WikipediaSingleton.getInstance().wikipedia.getArticleById(Integer.valueOf(s))!=null) {
	//						String entity = WikipediaSingleton.getInstance().wikipedia.getArticleById(Integer.valueOf(s)).getTitle();
	//						int count = result.containsKey(entity) ? result.get(entity) : 0;
	//						result.put(entity, count + 1);
	//					}
	//				}
	//				cCount++;
	//			}
	//
	//		}
	//		Map<String,Integer> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(result));
	//		System.out.println("Start writing..");
	//		FileUtil.writeDataToFile(sortedMap, fName+"_most similar10EntitiesForEachAn");
	//		System.out.println("Finished writing: " +fName+"_most similar1EntitiesForEachAn");
	//	}
	public static void analyseAnchorText(int id) {
		List<Category> lstDatasetCatList = new ArrayList<>(LabelsOfTheTexts.getCatValue_AG().keySet());
		for(Category c : lstDatasetCatList ) {
			System.out.println(c.getTitle());
			Category cDataset=c;
			List<String> dataset = new ArrayList<>(ReadDataset.read_AG_BasedOnCategory(cDataset,AG_DataType.TITLE));
			List<Annotation> lstAnnotations = new ArrayList<>(AnnonatationUtil.findAnnotationAll(dataset));
			int count=0;
			for(Annotation a : lstAnnotations) {
				if (a.getId()==id) {
					System.out.println(a.getMention()+"\t"+a.getTitle()+"\t"+ ++count);
				}
			}
			System.out.println();
		}
	}
	private static void findFreqOfEntitiesOfDatasetwithSimilarities(AG_DataType type) {
		List<Category> lstDatasetCatList = new ArrayList<>(LabelsOfTheTexts.getCatValue_AG().keySet());
		for(Category c : lstDatasetCatList ) {
			String fileName = "AnnotationFrequency_"+type+"_"+TEST_DATASET_TYPE+"_"+c.getTitle()+"_withSimilarity_filteredEntities";
			List<String> dataset = new ArrayList<>(ReadDataset.read_AG_BasedOnCategory(c,type));
			List<Annotation> lstAllAnnotation = new ArrayList<>(AnnonatationUtil.findAnnotationAll_FilterAG(dataset));
			Map<String, Integer> findFreqOfEntitySortedMap = AnnonatationUtil.findFreqOfEntitySortedMap(lstAllAnnotation);
			Map<String, Double> result = new LinkedHashMap<String, Double>();

			for(Entry<String,Integer> e : findFreqOfEntitySortedMap.entrySet()) {
				for(Category cC : lstDatasetCatList ) {
					String aId = e.getKey().split("\t")[1];
					String cId = String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(cC.getTitle()).getId());
					result.put(e.getKey().split("\t")[0]+"\t"+aId+"\t"+e.getValue()+"\t"+cC.getTitle(), BasedOnWordsCategorize.getSimilarity(Arrays.asList(aId), Arrays.asList(cId)));
				}
			}
			FileUtil.writeDataToFile(result, fileName);
		}
	}




	public static void findMostSimilarWordForVectorOfDataset(List<String> dataset ,String fileName) {
		final List<String> tokens = new ArrayList<String>();
		final LexedTokenFactory<CoreLabel> tokenFactory = new CoreLabelTokenFactory();
		for(String line: dataset) {
			final PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<CoreLabel>(new StringReader(line), tokenFactory,
					"untokenizable=noneDelete");
			while (tokenizer.hasNext()) {
				tokens.add(tokenizer.next().toString());
			}
		}
		double[] docVec= VectorUtil.getSentenceVector(tokens, GoogleModelSingleton.getInstance().google_model);
		System.out.println("Document Vector Generated...");
		VocabCache<VocabWord> vocab = GoogleModelSingleton.getInstance().google_model.getVocab();
		Map<String, Double> result = new HashMap<>();

		for (int i = 0; i < vocab.numWords(); i++) {
			String word=vocab.elementAtIndex(i).getWord();
			if (!word.contains("_")&&!word.contains("*")&&!word.contains(".")&&!word.contains("#")) {
				Double sim = VectorUtil.getSimilarity2Vecs(docVec, VectorUtil.getSentenceVector(Arrays.asList(word), GoogleModelSingleton.getInstance().google_model));
				result.put(word,sim);
			}
		}

		System.out.println("Size of the word similarity map..."+result.size());
		Map<String, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(result));
		FileUtil.writeDataToFile(sortedMap,fileName);
		System.out.println("Finished one dataset writing: " + fileName);
	}

	public static void findFreqOfWord(List<String> dataset ,String fileName) {
		final List<String> tokens = new ArrayList<String>();
		final LexedTokenFactory<CoreLabel> tokenFactory = new CoreLabelTokenFactory();
		for(String line: dataset) {
			line=StopWordRemoval.removeStopWords(line);
			line=StringUtil.removePunctuation(line);
			final PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<CoreLabel>(new StringReader(line), tokenFactory,
					"untokenizable=noneDelete");
			while (tokenizer.hasNext()) {
				tokens.add(tokenizer.next().toString());
			}
		}
		Map<String, Integer> resultFreq = new HashMap<>();

		for(String str :tokens) {
			if (resultFreq.containsKey(str)) {
				resultFreq.put(str, (resultFreq.get(str)+1));
			}
			else{
				resultFreq.put(str, 1);
			}
		}
		Map<String, Integer> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(resultFreq));
		FileUtil.writeDataToFile(sortedMap,fileName);
		System.out.println("Finished one dataset writing: " + fileName);
	}

	public static void printAvgNumberOfWordsOfDatasets(List<String> dataset) {
		int totalWord=0;
		System.out.println("Size of dataset: "+ dataset.size());
		for(String str: dataset) {
			str=StringUtil.removePunctuation(str);
			totalWord+=SentenceSegmentator.wordCount(str);
		}
		System.out.println("totalWordCount : "+totalWord);
		System.out.println("WordAvg: "+totalWord*1.0/dataset.size()*1.0);
	}

	/*
	 * private static void findFreqOfWords(List<String> dataset,String fileName) {
		final List<String> tokensStr = new ArrayList<String>();

		final LexedTokenFactory<CoreLabel> tokenFactory = new CoreLabelTokenFactory();
		for(String line: dataset) {
			final PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<CoreLabel>(new StringReader(line), tokenFactory,
					"untokenizable=noneDelete");
			while (tokenizer.hasNext()) {
				tokensStr.add(tokenizer.next().toString());
			}
		}
		AnnonatationUtil.findFreqOfWord(tokensStr, fileName);

	}




	 */

}
