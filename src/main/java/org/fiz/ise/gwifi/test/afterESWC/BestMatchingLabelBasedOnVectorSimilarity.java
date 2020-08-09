package org.fiz.ise.gwifi.test.afterESWC;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.ml.distance.ManhattanDistance;
import org.apache.log4j.Logger;
import org.apache.xpath.operations.Gt;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.Doc2VecModelSingleton;
import org.fiz.ise.gwifi.Singleton.GoogleModelSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_2modelSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LabelsOfTheTexts;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.dataset.train.generation.GenerateDatasetForNN;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.Print;
import org.fiz.ise.gwifi.util.StopWordRemoval;
import org.fiz.ise.gwifi.util.StringUtil;
import org.fiz.ise.gwifi.util.VectorUtil;
import org.nd4j.linalg.api.ops.impl.accum.AMin;
import org.openjena.atlas.test.Gen;

import com.hp.hpl.jena.sparql.pfunction.library.listIndex;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.LexedTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;

public class BestMatchingLabelBasedOnVectorSimilarity {
	private final static Dataset TEST_DATASET_TYPE = Config.getEnum("TEST_DATASET_TYPE");
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");
	private final static Integer NUMBER_OF_MOST_SIMILAR_ARTICLES= Config.getInt("NUMBER_OF_MOST_SIMILAR_ARTICLES",-1);
	private final static Map<Article, double[]> CACHE_mostSimilarNArticles = new HashMap<>();

	public static void initializeMostSimilarCache() {
		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories); //get predefined cats
		System.out.println(setMainCategories);
		for (Category mainCat : setMainCategories) { //iterate over categories and calculate a score for each of them
			Article amainCat = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(mainCat.getTitle());
			Collection<String> wordsNearest = new ArrayList<String>(LINE_modelSingleton.getInstance().lineModel.wordsNearest(String.valueOf(amainCat.getId()), NUMBER_OF_MOST_SIMILAR_ARTICLES));
			wordsNearest.add(String.valueOf(amainCat.getId()));
			List<String> list = new ArrayList<String>(wordsNearest);
			double[] documentVec = VectorUtil.getSentenceVector(list,LINE_modelSingleton.getInstance().lineModel);
			CACHE_mostSimilarNArticles.put(amainCat, documentVec);
		}
		System.out.println("Finished initializing the cache");
		Print.printMap(CACHE_mostSimilarNArticles);
	}	
	public static Article getBestMatchingArticleFromWordVectorModel(Dataset dname, List<Article> labels, String shortText, List<Article> gtList) {
		try {
			Map<Article, Double> mapScore = new HashMap<>();
			shortText=StopWordRemoval.removeStopWords(StringUtil.removePunctuation(shortText));
			List<String> tokensStr = new ArrayList<String>(StringUtil.tokinizeString(shortText));
			StringBuilder strB = new StringBuilder(shortText+"\n");
			for (Article amainCat : labels) { //iterate over categories and calculate a score for each of them
				double score = 0.0; 
				for (String t : tokensStr) {
					double tempScore=0;
					tempScore=get_word_similarity(dname,t ,amainCat.getTitle());
					if (!Double.isNaN(tempScore)) {
						score +=tempScore ;
					}
				}
				strB.append(amainCat+": "+score+"\n");
				mapScore.put(amainCat, score);
			}
			Map<Article, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			Article firstElement = MapUtil.getFirst(sortedMap).getKey();
			secondLOG.info(strB.toString());
			return firstElement;
		}
		catch (Exception e) {
			System.out.println();
			e.printStackTrace();
		}
		return null;
	}

	public static Article getBestMatchingArticleFromWordVectorModel_(Dataset dname, List<Article> labels, String shortText, List<Article> gtList) {
		try {
			Map<Article, Double> mapScore = new HashMap<>();
			shortText=StopWordRemoval.removeStopWords(StringUtil.removePunctuation(shortText));
			List<String> tokensStr = new ArrayList<String>(StringUtil.tokinizeString(shortText));
			double[] sentenceVector = VectorUtil.getSentenceVector(tokensStr, GoogleModelSingleton.getInstance().google_model);
			StringBuilder strB = new StringBuilder(shortText+"\n");
			for (Article amainCat : labels) { //iterate over categories and calculate a score for each of them
				String amainCatAbstract = null; 
				if (dname.equals(Dataset.DBpedia)) {
					if (amainCat.getTitle().equals("Office-holder")) {
						//amainCatAbstract = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Politician").getFirstParagraphMarkup();
						amainCatAbstract ="Politician";
					}
					//					else if (amainCat.getTitle().equals("Educational institution")) {
					//						amainCatAbstract="School College Library University";
					//					}
					else {
						//					amainCatAbstract = amainCat.getFirstParagraphMarkup();
						amainCatAbstract = amainCat.getTitle();
					}

				}
				tokensStr = new ArrayList<String>(StringUtil.tokinizeString(StringUtil.removePunctuation(amainCatAbstract)));
				double[] labelVector = VectorUtil.getSentenceVector(tokensStr, GoogleModelSingleton.getInstance().google_model);
				double score = VectorUtil.cosineSimilarity(labelVector, sentenceVector);
				strB.append(amainCat.getTitle()+": "+score+"\n");
				mapScore.put(amainCat, score);
			}

			Map<Article, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			Article firstElement = MapUtil.getFirst(sortedMap).getKey();
			secondLOG.info(strB.toString());
			return firstElement;
		}
		catch (Exception e) {
			System.out.println();
			e.printStackTrace();
		}
		return null;
	}
	public static Article getBestMatchingArticlewith_3_DifferentApproachAgreement(String shortText,List<Article> gtList) {
		Article bestMatching1 = getBestMatchingArticle(shortText, gtList);
		Article bestMatching2 = null;// getBestMatchingArticleFromWordVectorModel(TEST_DATASET_TYPE,shortText, gtList);
		Article bestMatching3 = GenerateDatasetForNN.map_results_doc2vec.get(shortText);
		if (bestMatching1==null || bestMatching2==null || bestMatching3==null) {
			return null;
		}
		else if (bestMatching1.equals(bestMatching2) && bestMatching2.equals(bestMatching3)) {
			return bestMatching1;
		}
		return null;
	}
	public static Article getBestMatchingArticlewith_3_DifferentApproachAgreement_categorize_all_dataset_write(Dataset dname,String shortText,List<Article> gtList) {
		Article best_LINE = getBestMatchingArticle_resolve_redirect(dname, shortText, gtList);
		Article best_google = null;//getBestMatchingArticleFromWordVectorModel(TEST_DATASET_TYPE,shortText, gtList);
		Article best_doc2vec = GenerateDatasetForNN.map_results_doc2vec.get(shortText);
		Map<Article, Integer> map = new HashMap<Article, Integer>();

		if (best_LINE!=null ) {
			Integer integer = map.get(best_LINE);
			if (integer==null) {
				map.put(best_LINE, 1);
			}
			else {
				map.put(best_LINE, integer+1);
			}
		}
		if (best_google!=null ) {
			Integer integer = map.get(best_google);
			if (integer==null) {
				map.put(best_google, 1);
			}
			else {
				map.put(best_google, integer+1);
			}
		}
		if (best_doc2vec!=null ) {
			Integer integer = map.get(best_doc2vec);
			if (integer==null) {
				map.put(best_doc2vec, 1);
			}
			else {
				map.put(best_doc2vec, integer+1);
			}
		}
		if(map.isEmpty()) {
			return null;
		}
		Map<Article, Integer> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(map));
		Entry<Article, Integer> firstElement = MapUtil.getFirst(sortedMap);

		if (firstElement.getValue()==3) {
			resultLog.info("\""+LabelsOfTheTexts.getArticleValue_AG().get(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(firstElement.getKey().getTitle()))+
					"\",\""+shortText+"\"");
			return firstElement.getKey();
		}else if(firstElement.getValue()==2) {
			for (Entry <Article, Integer> e: map.entrySet()) {
				resultLog.info("\""+LabelsOfTheTexts.getArticleValue_AG().get(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(e.getKey().getTitle()))+
						"\",\""+shortText+"\"");
			}
			//			resultLog.info("\""+LabelsOfTheTexts.getArticleValue_AG().get(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(firstElement.getKey().getTitle()))+
			//					"\",\""+shortText+"\"");
			return firstElement.getKey();
		}
		else {
			for (Entry <Article, Integer> e: map.entrySet()) {
				resultLog.info("\""+LabelsOfTheTexts.getArticleValue_AG().get(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(e.getKey().getTitle()))+
						"\",\""+shortText+"\"");
			}
			return best_LINE;
		}
	}
	public static Article getBestMatchingArticlewith_3_DifferentApproachAgreement_categorize_all_dataset(Dataset dname,String shortText,List<Article> gtList) {
		Article best_LINE = getBestMatchingArticle_resolve_redirect(dname, shortText, gtList);
		Article best_google = null;//getBestMatchingArticleFromWordVectorModel(TEST_DATASET_TYPE,shortText, gtList);
		Article best_doc2vec = GenerateDatasetForNN.map_results_doc2vec.get(shortText);
		Map<Article, Integer> map = new HashMap<Article, Integer>();

		if (best_LINE!=null ) {
			Integer integer = map.get(best_LINE);
			if (integer==null) {
				map.put(best_LINE, 1);
			}
			else {
				map.put(best_LINE, integer+1);
			}
		}
		if (best_google!=null ) {
			Integer integer = map.get(best_google);
			if (integer==null) {
				map.put(best_google, 1);
			}
			else {
				map.put(best_google, integer+1);
			}
		}
		if (best_doc2vec!=null ) {
			Integer integer = map.get(best_doc2vec);
			if (integer==null) {
				map.put(best_doc2vec, 1);
			}
			else {
				map.put(best_doc2vec, integer+1);
			}
		}

		if(map.isEmpty()) {
			return null;
		}

		Map<Article, Integer> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(map));
		Entry<Article, Integer> firstElement = MapUtil.getFirst(sortedMap);
		if (firstElement.getValue()==3) {
			return firstElement.getKey();
		}else if(firstElement.getValue()==2) {
			return firstElement.getKey();
		}

		else
			return best_LINE;
	}

	public static Article getBestMatchingArticlewithTwoDifferentApproachAgreement(String shortText, List<Article> gtList) {
		Article bestMatching1 = getBestMatchingArticle(shortText, gtList);
		Article bestMatching2 = null;//getBestMatchingArticleFromWordVectorModel(TEST_DATASET_TYPE,shortText, gtList);
		if (bestMatching1==null || bestMatching2==null) {
			return null;
		}
		else if (bestMatching1.equals(bestMatching2)) {
			return bestMatching1;
		}
		return null;
	}

	public static Article getBestMatchingArticlewithTwoDifferentSimilarityMetricAgreement(String shortText, List<Article> gtList) {
		Article bestMatching1 = getBestMatchingArticle(shortText, gtList);
		Article bestMatching2 = getBestMatchingArticlewithEuclineDistance(shortText, gtList);

		if (bestMatching1==null || bestMatching2==null) {
			return null;
		}
		else if (bestMatching1.equals(bestMatching2)) {
			return bestMatching1;
		}
		return null;
	}



	/*
	 * One time experiment: instead of assigning one single category for each training sample this helps to assign N most similar category/article
	 */
	public static List<Article> getBestMatchingNArticles(String shortText, int n) {
		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories); //get predefined cats
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		BestMatchingLabelBasedOnVectorSimilarity heuristic = new BestMatchingLabelBasedOnVectorSimilarity();
		StringBuilder mainBuilder = new StringBuilder();
		try {
			Map<Article, Double> mapScore = new HashMap<>();
			mainBuilder.append(shortText + "\n");
			List<Annotation> lstAnnotations = new ArrayList<>();
			service.annotate(shortText, lstAnnotations);//annotate the given text
			List<Annotation> filteredAnnotations = new ArrayList<>(filterEntitiesNotInVectorSpace(lstAnnotations));

			for (Category mainCat : setMainCategories) { //iterate over categories and calculate a score for each of them
				Article amainCat = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(mainCat.getTitle());
				double score = 0.0; 
				for (Annotation a : filteredAnnotations) {
					if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())&&WikipediaSingleton.getInstance().getArticle(a.getTitle())!=null) { //we had so many noisy entities therefore filtering required
						double tempScore=0;
						if (TEST_DATASET_TYPE.equals(Dataset.AG)) {
							tempScore+= heuristic.calculateScore_AG(a, amainCat);
						}
						else if (TEST_DATASET_TYPE.equals(Dataset.WEB_SNIPPETS)) {
							tempScore+= heuristic.calculateScore_WEB(a, amainCat);
						}
						score +=tempScore ;
					} 
				}
				mapScore.put(amainCat, score);
			}
			Map<Article, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));

			List<Article> lst = new ArrayList<Article>();
			int count=0;
			for(Entry <Article, Double> e :sortedMap.entrySet()) {
				if (count<n) {
					lst.add(e.getKey());
					count++;
				}
				else
					break;
			}
			return lst;
		}
		catch (Exception e) {
			System.out.println();
			e.printStackTrace();
		}
		return null;
	}
	public static Article getBestMatchingArticlewithThreshold(String shortText, double threshold) {

		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories); //get predefined cats
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		BestMatchingLabelBasedOnVectorSimilarity heuristic = new BestMatchingLabelBasedOnVectorSimilarity();
		StringBuilder mainBuilder = new StringBuilder();
		try {
			Map<Article, Double> mapScore = new HashMap<>();
			mainBuilder.append(shortText + "\n");
			StringBuilder strBuild = new StringBuilder();

			List<Annotation> lstAnnotations = new ArrayList<>();
			service.annotate(shortText, lstAnnotations);//annotate the given text
			List<Annotation> filteredAnnotations = new ArrayList<>(filterEntitiesNotInVectorSpace(lstAnnotations));

			mainBuilder.append(strBuild.toString() + "\n" + "\n");

			for (Category mainCat : setMainCategories) { //iterate over categories and calculate a score for each of them
				Article amainCat = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(mainCat.getTitle());
				double score = 0.0; 
				int count =0;
				for (Annotation a : filteredAnnotations) {
					if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())&&WikipediaSingleton.getInstance().getArticle(a.getTitle())!=null) { //we had so many noisy entities therefore filtering required
						double tempScore=0;
						tempScore+= heuristic.calculateScore_AG(a, amainCat);
						score +=tempScore ;
						count++;
					} 
				}
				mapScore.put(amainCat, score/count);

			}
			Map<Article, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			Article firstElement = MapUtil.getFirst(sortedMap).getKey();
			if (sortedMap.get(firstElement)>=threshold) {
				return firstElement;
			}
		}
		catch (Exception e) {
			System.out.println();
			e.printStackTrace();
		}
		return null;
	}

	public static Article getBestMatchingArticleByExtendingCategory(String shortText, List<Article> gtList) {
		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories); //get predefined cats
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		BestMatchingLabelBasedOnVectorSimilarity heuristic = new BestMatchingLabelBasedOnVectorSimilarity();
		StringBuilder mainBuilder = new StringBuilder();
		try {
			Map<Article, Double> mapScore = new HashMap<>();
			mainBuilder.append(shortText + "\n");
			StringBuilder strBuild = new StringBuilder();
			for (Article c : gtList) {
				strBuild.append("Ground Truth"+c + " ");
			}
			List<Annotation> lstAnnotations = new ArrayList<>();
			service.annotate(shortText, lstAnnotations);//annotate the given text
			List<Annotation> filteredAnnotations = new ArrayList<>(filterEntitiesNotInVectorSpace(lstAnnotations));

			mainBuilder.append(strBuild.toString() + "\n" + "\n");

			for (Category mainCat : setMainCategories) { //iterate over categories and calculate a score for each of them
				Article amainCat = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(mainCat.getTitle());
				double score = 0.0; 
				for (Annotation a : filteredAnnotations) {
					if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())&&WikipediaSingleton.getInstance().getArticle(a.getTitle())!=null) { //we had so many noisy entities therefore filtering required
						double tempScore=0;
						if (TEST_DATASET_TYPE.equals(Dataset.AG)) {
							double ttempScore = heuristic.calculateScore_AG_withExtention(a, amainCat, NUMBER_OF_MOST_SIMILAR_ARTICLES);
							tempScore+=ttempScore;
							mainBuilder.append(a.getMention()+" "+a.getTitle()+":"+mainCat.getTitle()+":"+ttempScore+"\n" );
						}
						else if (TEST_DATASET_TYPE.equals(Dataset.WEB_SNIPPETS)) {
							tempScore+= heuristic.calculateScore_WEB(a, amainCat);
						}
						score +=tempScore ;
					} 
				}
				mapScore.put(amainCat, score);
				mainBuilder.append(amainCat+": "+score+"\n\n");
			}
			Map<Article, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			Article firstElement = MapUtil.getFirst(sortedMap).getKey();
			mainBuilder.append("predicted:"+firstElement.getTitle());
			if ((firstElement.getTitle().equals("Business")&&gtList.get(0).getTitle().equals("Software"))||
					firstElement.getTitle().equals("Software")&&gtList.get(0).getTitle().equals("Business")) {
				secondLOG.info(mainBuilder.toString()+"\n--------------------------------------------");
			}

			return firstElement;
		}
		catch (Exception e) {
			System.out.println();
			e.printStackTrace();
		}
		return null;
	}
	public static Article getBestMatchingArticlewithManhattenDistance(String shortText, List<Article> gtList) {
		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories); //get predefined cats
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		StringBuilder mainBuilder = new StringBuilder();
		try {
			Map<Article, Double> mapScore = new HashMap<>();
			mainBuilder.append(shortText + "\n");
			StringBuilder strBuild = new StringBuilder();
			for (Article c : gtList) {
				strBuild.append("Ground Truth"+c + " ");
			}
			List<Annotation> lstAnnotations = new ArrayList<>();
			service.annotate(shortText, lstAnnotations);//annotate the given text
			List<Annotation> filteredAnnotations = new ArrayList<>(filterEntitiesNotInVectorSpace(lstAnnotations));

			mainBuilder.append(strBuild.toString() + "\n" + "\n");

			List<String> lstCleanedAnnotation = new ArrayList<String>();

			for (Annotation a : filteredAnnotations) {
				if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())&&WikipediaSingleton.getInstance().getArticle(a.getTitle())!=null) { //we had so many noisy entities therefore filtering required
					lstCleanedAnnotation.add(String.valueOf(a.getId()));
				}
			}

			double[] sentenceVector = VectorUtil.getSentenceVector(lstCleanedAnnotation, LINE_modelSingleton.getInstance().lineModel);

			for (Category mainCat : setMainCategories) { //iterate over categories and calculate a score for each of them
				Article amainCat = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(mainCat.getTitle());
				double score = VectorUtil.distanceManhatten(sentenceVector, LINE_modelSingleton.getInstance().lineModel.getWordVector(String.valueOf(amainCat.getId())));
				mapScore.put(amainCat, score);
			}
			Map<Article, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			Article lastElement = MapUtil.getLast(sortedMap).getKey();
			return lastElement;
		}
		catch (Exception e) {
			System.out.println();
			e.printStackTrace();
		}
		return null;
	}



	public static Article getBestMatchingArticlewithEuclineDistance(String shortText, List<Article> gtList) {
		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories); //get predefined cats
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		StringBuilder mainBuilder = new StringBuilder();
		try {
			Map<Article, Double> mapScore = new HashMap<>();
			mainBuilder.append(shortText + "\n");
			StringBuilder strBuild = new StringBuilder();
			for (Article c : gtList) {
				strBuild.append("Ground Truth"+c + " ");
			}
			List<Annotation> lstAnnotations = new ArrayList<>();
			service.annotate(shortText, lstAnnotations);//annotate the given text
			List<Annotation> filteredAnnotations = new ArrayList<>(filterEntitiesNotInVectorSpace(lstAnnotations));

			mainBuilder.append(strBuild.toString() + "\n" + "\n");

			List<String> lstCleanedAnnotation = new ArrayList<String>();

			for (Annotation a : filteredAnnotations) {
				if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())&&WikipediaSingleton.getInstance().getArticle(a.getTitle())!=null) { //we had so many noisy entities therefore filtering required
					lstCleanedAnnotation.add(String.valueOf(a.getId()));
				}
			}
			if (lstCleanedAnnotation==null || lstCleanedAnnotation.isEmpty() ) {
				return null;
			}
			double[] sentenceVector = VectorUtil.getSentenceVector(lstCleanedAnnotation, LINE_modelSingleton.getInstance().lineModel);

			for (Category mainCat : setMainCategories) { //iterate over categories and calculate a score for each of them
				Article amainCat = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(mainCat.getTitle());
				double score = VectorUtil.distanceEucline(sentenceVector, LINE_modelSingleton.getInstance().lineModel.getWordVector(String.valueOf(amainCat.getId())));
				mapScore.put(amainCat, score);
			}
			Map<Article, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			Article lastElement = MapUtil.getLast(sortedMap).getKey();
			return lastElement;
		}
		catch (Exception e) {
			System.out.println();
			e.printStackTrace();
		}
		return null;
	}
	public static Article getBestMatchingArticlewithAnnotationList(Dataset dname, List<String> lstAnnotationIDs, List<Article>  labels) {
		try {
			StringBuilder strBuild = new StringBuilder();
			strBuild.append(lstAnnotationIDs + "\n");
			Map<Article, Double> mapScore = new HashMap<>();
			for (Article amainCat : labels) { //iterate over categories and calculate a score for each of them
				double score = 0.0; 
				for (String a : lstAnnotationIDs) {
					if (dname.equals(Dataset.WEB_SNIPPETS)) {
						if (!AnnonatationUtil.getEntityBlackList_WebSnippets().contains(Integer.valueOf(a))) {
							double tempScore=0;
							tempScore=get_article_similarity_clean(dname,Integer.valueOf(a) ,amainCat);
							if (!Double.isNaN(tempScore)) {
								score +=tempScore ;
							}
						}
					}
					else {
						double tempScore=0;
						tempScore=get_article_similarity_clean(dname,Integer.valueOf(a) ,amainCat);
						if (!Double.isNaN(tempScore)) {
							score +=tempScore ;
						}
					}
				}
				strBuild.append(amainCat+": "+score+"\n");
				mapScore.put(amainCat, score);
			}
			Map<Article, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			Article firstElement = MapUtil.getFirst(sortedMap).getKey();
			if (sortedMap.get(firstElement)==0.0) {
				return null;
			}
			strBuild.append("\n\n");

			secondLOG.info(strBuild.toString());
			return firstElement;
		}
		catch (Exception e) {
			System.out.println();
			e.printStackTrace();
		}
		return null;
	}

	public static double get_word_similarity(Dataset dSet,String word, String label) {
		List<String> enrich = new ArrayList<String>();
		if (dSet.equals(Dataset.DBpedia)) {
			try {
				if(label.equals("Company")){
					enrich = new ArrayList<String>();
					enrich.add("company");
				}
				else if(label.equals("Educational institution")){
					enrich = new ArrayList<String>();
					enrich.add("School");
					enrich.add("College");
					enrich.add("Library");
					enrich.add("University");

				}
				else if (label.equals("Artist")) {
					enrich = new ArrayList<String>();
					enrich.add("singer");
					enrich.add("music");
					enrich.add("writer");
					enrich.add("artist");
				}
				else if(label.equals("Athlete")){
					enrich = new ArrayList<String>();
					enrich.add("player");
					enrich.add("footballer");
				}
				else if(label.equals("Office-holder")){
					enrich = new ArrayList<String>();
					enrich.add("Politician");
				}
				else if (label.equals("Transport")) {
					enrich = new ArrayList<String>();
					enrich.add("Transport");
				}
				else if(label.equals("Building")){
					enrich = new ArrayList<String>();
					enrich.add("building");				}
				else if (label.equals("Natural environment")) {
					enrich = new ArrayList<String>();
					enrich.add("river");
					enrich.add("lake");
					enrich.add("mountain");
				}
				else if(label.equals("Village")){
					enrich = new ArrayList<String>();
					enrich.add("village");
				}
				else if(label.equals("Animal")){
					enrich = new ArrayList<String>();
					enrich.add("animal");
				}
				else if(label.equals("Plant")){
					enrich = new ArrayList<String>();
					enrich.add("plant");
				}
				else if(label.equals("Album")){

					enrich = new ArrayList<String>();
					enrich.add("album");
				}
				else if(label.equals("Film")){

					enrich = new ArrayList<String>();
					enrich.add("film");
				}
				else if(label.equals("Writing")){
					enrich = new ArrayList<String>();
					enrich.add("written");
				}
				else {
					System.out.println("Entity title is not matching");
					System.exit(1);
				}
				double score =0.0;
				double size =0.0;
				for (String a : enrich) {
					double tempScore=0;
					tempScore=GoogleModelSingleton.getInstance().google_model.similarity(a, word);
					if (!Double.isNaN(tempScore)) {
						score +=tempScore ;
						size+=1.0;
					}
				}
				return score/size;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(0);
			}

		}
		if (dSet.equals(Dataset.WEB_SNIPPETS)) {
			if (label.equalsIgnoreCase("Computer")) {
				enrich.add("Software");
				enrich.add("Computer");
			}
			else if (label.equalsIgnoreCase("Culture")||label.equalsIgnoreCase("The arts") || label.equalsIgnoreCase("Entertainment")) {
				enrich.add("music");
				//				enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Lyrics").getId()));
				//				enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("The arts").getId()));
				//				enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Book").getId()));
			}
			else if (label.equalsIgnoreCase("Education")||label.equalsIgnoreCase("Science")) {
				enrich.add("science");
				enrich.add("research");
				enrich.add("theory");
			}
			else if (label.equalsIgnoreCase("Engineering")) {
				enrich.add("car");
			}
			else if (label.equalsIgnoreCase("Health")) {
				enrich.add("disease");
				enrich.add("drug");
			}
			else {
				enrich.add(label.toLowerCase());
			}
			double score =0.0;
			double size =0.0;
			for (String a : enrich) {
				double tempScore=0;
				tempScore=GoogleModelSingleton.getInstance().google_model.similarity(a, word);
				if (!Double.isNaN(tempScore)) {
					score +=tempScore ;
					size+=1.0;
				}
			}
			return score/size;		
		}
		return 0.0;
	}
	public static double get_article_similarity_clean(Dataset dSet,int articleID, Article cArticle) {
		if (dSet.equals(Dataset.DBpedia)) {
			String fName="/home/rima/playground/GeneralFiles/gwifi/Dataset_ShortTextClassification/dbpedia_csv/dbp_sub_classes/";
			//			String fName="/home/rtue/Desktop/";
			try {
				List<String> enrich = new ArrayList<String>();
				int cleanAnnotation =AnnonatationUtil.getCorrectAnnotation_DBp(articleID);
				if(cArticle.getTitle().equals("Company")){
					enrich = new ArrayList<String>();
					enrich.add(String.valueOf(cArticle.getId()));

					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Winery").getId()));
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Record Label").getId()));
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Law Firm").getId()));
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Caterer").getId()));
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Brewery").getId()));
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Bank").getId()));
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Airline").getId()));
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Publisher").getId()));
				}
				else if(cArticle.getTitle().equals("Educational institution")){
					enrich = new ArrayList<String>();
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Education").getId()));


					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("School").getId()));
					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("College").getId()));
					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Library").getId()));
					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("University").getId()));

				}

				else if (cArticle.getTitle().equals("Artist")) {
					enrich = new ArrayList<String>();
					enrich.add(String.valueOf(cArticle.getId()));


					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Sculptor").getId()));
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Photographer").getId()));
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Painter").getId()));
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Musical artist").getId()));
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Humorist").getId()));
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Fashion Designer").getId()));
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Dancer").getId()));
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Comedian").getId()));
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Actor").getId()));


					//				enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Writer").getId()));
					//				enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Screenwriter").getId()));
					//				enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Musician").getId()));
					//				enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Singer").getId()));


				}
				else if(cArticle.getTitle().equals("Athlete")){
					enrich.add(String.valueOf(cArticle.getId()));

					//					List<String> lines = FileUtils.readLines(new File(fName+"Athlete_sub_classes.txt"), "utf-8");
					//					for(String str:lines) {
					//						if (WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str)!=null) {
					//							enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str).getId()));
					//						}
					//					}

				}
				else if(cArticle.getTitle().equals("Office-holder")){
					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Politician").getId()));
				}
				else if (cArticle.getTitle().equals("Transport")) {
					enrich.add(String.valueOf(cArticle.getId()));
					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Aircraft").getId()));
					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Automobile").getId()));
					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Locomotive").getId()));
					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Motorcycle").getId()));
					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Rocket").getId()));
					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Ship").getId()));
					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Space Shuttle").getId()));
					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Spacecraft").getId()));
					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Train").getId()));
					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Tram").getId()));

				}
				else if(cArticle.getTitle().equals("Building")){
					List<String> lines = FileUtils.readLines(new File(fName+"Building_sub_classes.txt"), "utf-8");
					enrich.add(String.valueOf(cArticle.getId()));

					for(String str:lines) {
						if (WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str)!=null) {
							enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str).getId()));
						}
					}
				}
				else if (cArticle.getTitle().equals("Natural environment")) {
					List<String> lines = FileUtils.readLines(new File(fName+"Natural environment_sub_classes.txt"), "utf-8");
					enrich.add(String.valueOf(cArticle.getId()));

					for(String str:lines) {
						if (WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str)!=null) {
							enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str).getId()));
						}
					}
					//					enrich = new ArrayList<String>();
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Natural environment").getId()));
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("River").getId()));
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Lake").getId()));
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Construction").getId()));
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Mountain").getId()));
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Valley").getId()));
					//					enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Church").getId()));

				}
				else if(cArticle.getTitle().equals("Village")){
					enrich.add(String.valueOf(cArticle.getId()));
				}
				else if(cArticle.getTitle().equals("Animal")){
					List<String> lines = FileUtils.readLines(new File(fName+"Animal_sub_classes.txt"), "utf-8");
					enrich.add(String.valueOf(cArticle.getId()));

					//					for(String str:lines) {
					//						if (WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str)!=null) {
					//							enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str).getId()));
					//						}
					//					}
				}
				else if(cArticle.getTitle().equals("Plant")){
					List<String> lines = FileUtils.readLines(new File(fName+"Plant_sub_classes.txt"), "utf-8");
					enrich.add(String.valueOf(cArticle.getId()));

					//					for(String str:lines) {
					//						if (WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str)!=null) {
					//							enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str).getId()));
					//						}
					//					}
				}
				else if(cArticle.getTitle().equals("Album")){

					enrich.add(String.valueOf(cArticle.getId()));
				}
				else if(cArticle.getTitle().equals("Film")){

					enrich.add(String.valueOf(cArticle.getId()));
				}
				else if(cArticle.getTitle().equals("Writing")){
					//					List<String> lines = FileUtils.readLines(new File(fName+"Writing_sub_classes.txt"), "utf-8");
					enrich.add(String.valueOf(cArticle.getId()));

					//					for(String str:lines) {
					//						if (WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str)!=null) {
					//							enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str).getId()));
					//						}
					//					}
				}
				else {
					System.out.println("Entity title is not matching");
					System.exit(1);
				}
				double score =0.0;
				double size =0.0;
				for (String a : enrich) {
					double tempScore=0;
					tempScore=LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(cleanAnnotation),String.valueOf(a));
					if (!Double.isNaN(tempScore)) {
						score +=tempScore ;
						size+=1.0;
					}
				}
				return score/size;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(0);
			}

		}
		else if (dSet.equals(Dataset.WEB_SNIPPETS)) {
			List<String> enrich = new ArrayList<String>();

			if (cArticle.getTitle().equalsIgnoreCase("Computer")) {
				enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Software").getId()));
				enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Computer").getId()));
			}
			else if (cArticle.getTitle().equalsIgnoreCase("Culture")||cArticle.getTitle().equalsIgnoreCase("The arts") || cArticle.getTitle().equalsIgnoreCase("Entertainment")) {
				enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Music").getId()));
				//				enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Lyrics").getId()));
				//				enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("The arts").getId()));
				//				enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Book").getId()));
			}
			else if (cArticle.getTitle().equalsIgnoreCase("Education")||cArticle.getTitle().equalsIgnoreCase("Science")) {
				enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Science").getId()));
				enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Research").getId()));
				enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Theory").getId()));
			}
			else if (cArticle.getTitle().equalsIgnoreCase("Engineering")) {
				enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Automotive industry").getId()));
			}
			else if (cArticle.getTitle().equalsIgnoreCase("Health")) {
				enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Disease").getId()));
				enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Drug").getId()));
			}

			else {
				if (enrich.size()==0) {
					return LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(articleID),String.valueOf(cArticle.getId()));
				}
			}

			double score =0.0;
			double size =0.0;
			for (String a : enrich) {
				double tempScore=0;
				tempScore=LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(articleID),String.valueOf(a));
				if (!Double.isNaN(tempScore)) {
					score +=tempScore ;
					size+=1.0;
				}
			}
			return score/size;

		}
		return 0.0;
	}

	public static Article getBestMatchingArticle_resolve_redirect(Dataset dname, String shortText, List<Article> gtList) {
		try {
			List<Article> labels = null;
			NLPAnnotationService service = AnnotationSingleton.getInstance().service;
			List<Annotation> lstAnnotations = new ArrayList<>();
			service.annotate(shortText, lstAnnotations);//annotate the given text

			List<Annotation> filteredAnnotations = new ArrayList<>(filterEntitiesNotInVectorSpace(lstAnnotations));
			List<Annotation> ffilteredAnnotations= null;


			if (dname.equals(Dataset.AG)) {
				labels = new ArrayList<Article>(LabelsOfTheTexts.getLables_AG_article().values());
			}
			else if (dname.equals(Dataset.TREC)) {
				labels = new ArrayList<Article>(LabelsOfTheTexts.getLables_TREC_article().values());
			}
			else if (dname.equals(Dataset.DBpedia)) {
				labels = new ArrayList<Article>(LabelsOfTheTexts.getLables_DBP_article().values());
				ffilteredAnnotations = new ArrayList<>();
				for(Annotation a : filteredAnnotations) {
					if (!AnnonatationUtil.getEntityBlackList_DBp().contains(a.getId())&& !StringUtil.isNumeric(a.getTitle())) {
						ffilteredAnnotations.add(a);
					}
				}

			}
			//BestMatchingLabelBasedOnVectorSimilarity heuristic = new BestMatchingLabelBasedOnVectorSimilarity();
			StringBuilder mainBuilder = new StringBuilder();

			Map<Article, Double> mapScore = new HashMap<>();
			mainBuilder.append(shortText + "\n");
			StringBuilder strBuild = new StringBuilder();
			for (Article c : gtList) {
				strBuild.append("Ground Truth"+c + " ");
			}
			mainBuilder.append(strBuild.toString() + "\n" + "\n");
			for (Article amainCat : labels) { //iterate over categories and calculate a score for each of them
				double score = 0.0; 
				for (Annotation a : ffilteredAnnotations) {
					double tempScore=0;
					if (WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())==null) {
						Page p = new Page(WikipediaSingleton.getInstance().wikipedia.getEnvironment(), a.getId());
						if (p.getType().equals(Page.PageType.redirect)) {

							String key = a.getURL().replace("http://en.wikipedia.org/wiki/", "");
							if(GenerateDatasetForNN.mapRedirectPages.containsKey(key)) {
								String tName = GenerateDatasetForNN.mapRedirectPages.get(key).replace("_", " ");
								Article article = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(tName);
								if (article!=null) {
									tempScore=get_article_similarity(article,amainCat);
								}
							}
						}
					}
					else {
						tempScore=get_article_similarity(WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId()),amainCat);
					}

					score +=tempScore ;

				}
				mapScore.put(amainCat, score);
				mainBuilder.append(amainCat+": "+score+"\n\n");
			}
			Map<Article, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			Article firstElement = MapUtil.getFirst(sortedMap).getKey();
			if (sortedMap.get(firstElement)==0.0) {
				return null;
			}
			return firstElement;
		}
		catch (Exception e) {
			System.out.println();
			e.printStackTrace();
		}
		return null;
	}
	public static double get_article_similarity(Article article, Article cArticle) {
		return LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(article.getId()), String.valueOf(cArticle.getId()));
	}
	//	public static double get_article_similarity(Article article, Article cArticle) {
	//		double P_e_c=0; 
	//		if(article.getId()==25614) {
	//			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("Racing"), cArticle); 
	//		}
	//		else if(article.getId()==12240) {
	//			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("Gold medal"), cArticle); 
	//		}
	//		else if(article.getId()==870936) 
	//		{
	//			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("Coach (sport)"), cArticle); 
	//		}
	//		else if(article.getId()==60930) 
	//		{
	//			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("New product development"), cArticle); 
	//		} 
	//		else if(article.getId()==2532101) 
	//		{
	//			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("Profit (accounting)"), cArticle); 
	//		}
	//		else if(article.getId()==16888425) 
	//		{
	//			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("Job"), cArticle); 
	//		}
	//		else if(article.getId()==770846) 
	//		{
	//			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("Personal trainer"), cArticle); 
	//		}
	//		else {
	//			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle(article.getTitle()), cArticle);
	//		}
	//		return P_e_c;
	//	}

	public static Article getBestMatchingArticle(String shortText, List<Article> gtList) {
		List<Article> labels = new ArrayList<Article>(LabelsOfTheTexts.getLables_AG_article().values());
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		BestMatchingLabelBasedOnVectorSimilarity heuristic = new BestMatchingLabelBasedOnVectorSimilarity();
		StringBuilder mainBuilder = new StringBuilder();
		try {
			Map<Article, Double> mapScore = new HashMap<>();
			mainBuilder.append(shortText + "\n");
			StringBuilder strBuild = new StringBuilder();
			for (Article c : gtList) {
				strBuild.append("Ground Truth"+c + " ");
			}
			List<Annotation> lstAnnotations = new ArrayList<>();
			service.annotate(shortText, lstAnnotations);//annotate the given text
			List<Annotation> filteredAnnotations = new ArrayList<>(filterEntitiesNotInVectorSpace(lstAnnotations));

			mainBuilder.append(strBuild.toString() + "\n" + "\n");

			for (Article amainCat : labels) { //iterate over categories and calculate a score for each of them
				double score = 0.0; 
				for (Annotation a : filteredAnnotations) {
					if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())&&WikipediaSingleton.getInstance().getArticle(a.getTitle())!=null) { //we had so many noisy entities therefore filtering required
						double tempScore=0;
						if (TEST_DATASET_TYPE.equals(Dataset.AG)) {
							double ttempScore = heuristic.calculateScore_AG(a, amainCat);
							tempScore+=ttempScore;
							mainBuilder.append(a.getMention()+" "+a.getTitle()+":"+amainCat.getTitle()+":"+ttempScore+"\n" );
						}
						else if (TEST_DATASET_TYPE.equals(Dataset.WEB_SNIPPETS)) {
							tempScore+= heuristic.calculateScore_WEB(a, amainCat);
						}
						score +=tempScore ;
					} 
				}
				mapScore.put(amainCat, score);
				mainBuilder.append(amainCat+": "+score+"\n\n");
			}
			Map<Article, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			Article firstElement = MapUtil.getFirst(sortedMap).getKey();
			//			mainBuilder.append("predicted:"+firstElement.getTitle());
			//			if ((firstElement.getTitle().equals("Business")&&gtList.get(0).getTitle().equals("Software"))||
			//					firstElement.getTitle().equals("Software")&&gtList.get(0).getTitle().equals("Business")) {
			//				secondLOG.info(mainBuilder.toString()+"\n--------------------------------------------");
			//			}
			if (sortedMap.get(firstElement)==0.0) {
				return null;
			}
			return firstElement;
		}
		catch (Exception e) {
			System.out.println();
			e.printStackTrace();
		}
		return null;
	}

	public static Category getBestMatchingCategory(String shortText) {

		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories); //get predefined cats
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		BestMatchingLabelBasedOnVectorSimilarity heuristic = new BestMatchingLabelBasedOnVectorSimilarity();
		try {
			Map<Category, Double> mapScore = new HashMap<>();
			StringBuilder strBuild = new StringBuilder();

			List<Annotation> lstAnnotations = new ArrayList<>();
			service.annotate(shortText, lstAnnotations);//annotate the given text
			List<Annotation> filteredAnnotations = new ArrayList<>(filterEntitiesNotInVectorSpace(lstAnnotations));


			for (Category mainCat : setMainCategories) { //iterate over categories and calculate a score for each of them
				double score = 0.0; 
				for (Annotation a : filteredAnnotations) {
					if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())&&WikipediaSingleton.getInstance().getArticle(a.getTitle())!=null) { //we had so many noisy entities therefore filtering required
						double tempScore=0;
						if (TEST_DATASET_TYPE.equals(Dataset.AG)) {
							tempScore+= heuristic.calculateScore_AG(a, WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(mainCat.getTitle()));
						}
						score +=tempScore ;
					} 
				}
				mapScore.put(mainCat, score);
			}
			Map<Category, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			System.out.println(shortText);
			Print.printMap(sortedMap);	
			System.out.println();

			Category firstElement = MapUtil.getFirst(sortedMap).getKey();
			//resultLog.info(shortText+"\t\t"+firstElement.getTitle());
			return firstElement;
		}
		catch (Exception e) {
			System.out.println();
			e.printStackTrace();
		}
		return null;
	}


	public static Category getBestMatchingCategory(String shortText, List<Category> gtList) {

		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories); //get predefined cats
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		BestMatchingLabelBasedOnVectorSimilarity heuristic = new BestMatchingLabelBasedOnVectorSimilarity();
		StringBuilder mainBuilder = new StringBuilder();
		try {
			Map<Category, Double> mapScore = new HashMap<>();
			mainBuilder.append(shortText + "\n");
			StringBuilder strBuild = new StringBuilder();
			for (Category c : gtList) {
				strBuild.append(c + " ");
			}
			List<Annotation> lstAnnotations = new ArrayList<>();
			service.annotate(shortText, lstAnnotations);//annotate the given text
			List<Annotation> filteredAnnotations = new ArrayList<>(filterEntitiesNotInVectorSpace(lstAnnotations));

			mainBuilder.append(strBuild.toString() + "\n" + "\n");

			for (Category mainCat : setMainCategories) { //iterate over categories and calculate a score for each of them
				double score = 0.0; 
				for (Annotation a : filteredAnnotations) {
					if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())&&WikipediaSingleton.getInstance().getArticle(a.getTitle())!=null) { //we had so many noisy entities therefore filtering required
						double tempScore=0;
						if (TEST_DATASET_TYPE.equals(Dataset.AG)) {
							tempScore+= heuristic.calculateScore_AG(a, WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(mainCat.getTitle()));
						}
						score +=tempScore ;
					} 
				}
				mapScore.put(mainCat, score);
			}
			Map<Category, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			Category firstElement = MapUtil.getFirst(sortedMap).getKey();
			//resultLog.info(shortText+"\t\t"+firstElement.getTitle());
			return firstElement;
		}
		catch (Exception e) {
			System.out.println();
			e.printStackTrace();
		}
		return null;
	}

	public static List<Annotation> filterEntitiesNotInVectorSpace(List<Annotation> lstAnnotations) {
		List<Annotation> result = new ArrayList<>();
		for(Annotation a : lstAnnotations) {
			if (LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(a.getId()))) {
				result.add(a);
			}
		}
		return result;
	}
	private double calculateScore_WEB(Annotation a, Article cArticle) {
		double P_e_c=0; 
		//		if(a.getId()==30653) {
		if(a.getMention().getTerm().toLowerCase().trim().equals("consumption")||a.getMention().getTerm().toLowerCase().trim().equals("consumption")) {
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Consumption (economics)"), cArticle); 
		}
		if(a.getId()==13930) {//House band --> white house
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().wikipedia.getArticleById(33057), cArticle); 
		}

		else {
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle(a.getTitle()), cArticle);
		}

		return P_e_c;
	}
	//	/*
	//	 * This function calculates a score for a given annotation and a main category
	//	 */

	private Article checkAnnotationCorrectness(Article a) {
		if(a.getId()==25614) {
			return WikipediaSingleton.getInstance().getArticle("Racing"); 
		}
		else if(a.getId()==12240) {
			return WikipediaSingleton.getInstance().getArticle("Gold medal"); 
		}
		//		else if(a.getMention().getTerm().toLowerCase().trim().equals("enterprise")||a.getMention().getTerm().toLowerCase().trim().equals("enterprises")) 
		//		{
		//			return WikipediaSingleton.getInstance().getArticle("Enterprise (computer)"); 
		//		}
		else if(a.getId()==870936) 
		{
			return WikipediaSingleton.getInstance().getArticle("Coach (sport)"); 
		}
		else if(a.getId()==60930) 
		{
			return WikipediaSingleton.getInstance().getArticle("New product development"); 
		} 
		else if(a.getId()==2532101) 
		{
			return WikipediaSingleton.getInstance().getArticle("Profit (accounting)"); 
		}
		else if(a.getId()==16888425) 
		{
			return WikipediaSingleton.getInstance().getArticle("Job"); 
		}
		else if(a.getId()==770846) 
		{
			return WikipediaSingleton.getInstance().getArticle("Personal trainer"); 
		}
		else {
			return a;
		}

	}
	private double calculateScore_AG_withExtention(Annotation a, Article cArticle, int n) {
		//return get_P_e_c_withMostSimilarNCats(WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId()), cArticle, n);
		return get_P_e_c_withMostSimilarNCats(checkAnnotationCorrectness(WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())),cArticle, n);
	}
	private double calculateScore_AG(Annotation a, Article cArticle) {
		double P_e_c=0; 
		if(a.getId()==25614) {
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("Racing"), cArticle); 
		}
		else if(a.getId()==12240) {
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("Gold medal"), cArticle); 
		}
		else if(a.getMention().getTerm().toLowerCase().trim().equals("enterprise")||a.getMention().getTerm().toLowerCase().trim().equals("enterprises")) 
		{
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("Enterprise (computer)"), cArticle); 
		}
		else if(a.getId()==870936) 
		{
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("Coach (sport)"), cArticle); 
		}
		else if(a.getId()==60930) 
		{
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("New product development"), cArticle); 
		} 
		else if(a.getId()==2532101) 
		{
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("Profit (accounting)"), cArticle); 
		}
		else if(a.getId()==16888425) 
		{
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("Job"), cArticle); 
		}
		else if(a.getId()==770846) 
		{
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("Personal trainer"), cArticle); 
		}
		else {
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle(a.getTitle()), cArticle);
		}
		return P_e_c;
	}
	public static double get_P_e_c_withMostSimilarNCats(Article annotationArticle, Article categoryArticle,int n) {

		return VectorUtil.cosineSimilarity(CACHE_mostSimilarNArticles.get(categoryArticle), LINE_modelSingleton.getInstance().lineModel.getWordVector(String.valueOf(annotationArticle.getId())));
	}
	public static double get_P_e_c(Article article, Article cArticle) {
		return LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(article.getId()), String.valueOf(cArticle.getId()));
	}
}
