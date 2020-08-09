package org.fiz.ise.gwifi.WESSTEC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LabelsOfTheTexts;
import org.fiz.ise.gwifi.dataset.ReadDataset;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.test.afterESWC.BestMatchingLabelBasedOnVectorSimilarity;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.MapUtil;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class LabeledDataGeneration {

	public static void main(String[] args) {
		String path=""; //path of the file
		List<String> lstLabels = new ArrayList<String>(); //predefined labels
		List<Article> lstLabelsToWikiEntities = mapLabelsToWikiEntities(lstLabels);
		Map<String,List<Article>> read_dataset_AG = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION, path);
		Map<String, Article> mapLabeledData = new HashMap<String, Article>(); //this stores the labeled data
		
		for(Entry<String, Article> e : mapLabeledData.entrySet()) {
			mapLabeledData.put(e.getKey(), getLabel(e.getKey(), lstLabelsToWikiEntities));
		}
		
		//here user can write the result, i.e., labeled documents, to a file 

	}
	public static Article getLabel(String shortText, List<Article> gtList) {
		List<Article> labels = new ArrayList<Article>(LabelsOfTheTexts.getLables_AG_article().values());
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		BestMatchingLabelBasedOnVectorSimilarity heuristic = new BestMatchingLabelBasedOnVectorSimilarity();
		try {
			Map<Article, Double> mapScore = new HashMap<>();
			List<Annotation> lstAnnotations = new ArrayList<>();
			service.annotate(shortText, lstAnnotations);//annotate the given text

			for (Article amainCat : labels) { //iterate over categories and calculate a score for each of them
				double score = 0.0; 
				for (Annotation a : lstAnnotations) {
					double tempScore = getSimilarity_LINE(WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId()), amainCat);
					score +=tempScore ;
				} 
				mapScore.put(amainCat, score);
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
public static double getSimilarity_LINE(Article annotation, Article cArticle) {
	return LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(annotation.getId()), String.valueOf(cArticle.getId()));
}
public static List<Article> mapLabelsToWikiEntities(List<String> lstLabels){

	List<Article> lstWikiLabels = new ArrayList<Article>();
	for (String str : lstLabels) {
		lstWikiLabels.add(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str));
	}
	return lstWikiLabels;
}

}
