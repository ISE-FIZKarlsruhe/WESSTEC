package org.fiz.ise.gwifi.dataset.category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LabelsOfTheTexts;
import org.fiz.ise.gwifi.dataset.shorttext.test.TestBasedOnAnnotatedDocument;
import org.fiz.ise.gwifi.longText.TestBasedonLongTextDatasets;
import org.fiz.ise.gwifi.model.Dataset;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;

public class Categories {

	public static List<String> getCategoryList(Dataset t)
	{
		switch (t) {
		case AG:  return getCategories_Ag();
		case WEB_SNIPPETS:  return getCategories_Web();
		case DBpedia:  return LabelsOfTheTexts.getLabels_DBP_category();
		case YAHOO:  return getCategories_Yahoo();
		case DBLP: return getCategories_DBLP();
		case TWENTYNEWS: return getCategories_20News();
		case YOVISTO: return getCategories_YOVISTO();
		case YOVISTO_SENTENCEBYSENTENCE_sentence: return getCategories_YOVISTOSENTENCES_sentence();
		case YOVISTO_SENTENCEBYSENTENCE_entities: return getCategories_YOVISTOSENTENCES_entities();
		default: 
			System.out.println("Invalid Dataset Type");
			return null;
		}
	}
	private static List<String> getCategories_Web() {

  // for KBSTC
  final List<String> dummySeeds = Arrays.asList("Business","Computers","Culture","Arts","Entertainment","Education",
				"Science","Engineering","Health","Politics","Society","Sports");
 
//		********************************
		
/*
 * WeaklySupervised model:		
 */
//		final List<String> dummySeeds = Arrays.asList("Business","Computers","Culture","The arts","Entertainment","Education",
//				"Science","Engineering","Health","Politics","Society","Sports");
		return Collections.unmodifiableList(dummySeeds);
	}
	public static List<Article> getLabels_Snippets() {
		final List<Article> labels = new ArrayList<Article>(); 
		labels.add(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Business"));
		labels.add(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Computers"));
		labels.add(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Culture"));
		labels.add(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("The arts"));
		labels.add(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Entertainment"));
		labels.add(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Education"));
		labels.add(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Science"));
		labels.add(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Engineering"));
		labels.add(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Health"));
		labels.add(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Politics"));
		labels.add(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Society"));
		labels.add(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Sports"));
		return Collections.unmodifiableList(labels);
	}
	
	public static Map<Integer, Category> getLables_DBpedia_category()
	{
		Map<Integer, Category> mapLabel = new HashMap<>();
		mapLabel.put(1, WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Company"));
		mapLabel.put(2, WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Educational institutions"));
		mapLabel.put(3, WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Artist"));
		mapLabel.put(4, WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Athlete"));
		mapLabel.put(5, WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Office holders"));
		mapLabel.put(6, WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Transport"));
		mapLabel.put(7, WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Building"));
		mapLabel.put(8, WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Natural environment"));
		mapLabel.put(9, WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Village"));
		mapLabel.put(10, WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Animal"));
		mapLabel.put(11, WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Plant"));
		mapLabel.put(12, WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Album"));
		mapLabel.put(13, WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Film"));
		mapLabel.put(14, WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Writing"));
		return mapLabel;
		
	}
	private static List<String> getCategories_YOVISTO() {
		List<String> categoryList = new ArrayList<>(TestBasedonLongTextDatasets.getLstCategory());
		return Collections.unmodifiableList(categoryList);
	}
	private static List<String> getCategories_YOVISTOSENTENCES_sentence() {
		List<String> categoryList = new ArrayList<>(TestBasedonLongTextDatasets.getLstCategory());
		return Collections.unmodifiableList(categoryList);
	}
	private static List<String> getCategories_YOVISTOSENTENCES_entities() {
		List<String> categoryList = new ArrayList<>(TestBasedOnAnnotatedDocument.getLstCategory());
		return Collections.unmodifiableList(categoryList);
	}
	private static List<String> getCategories_20News() {
		Map<String, List<Category>> categories = new HashMap<>(LabelsOfTheTexts.getLables_20News());
		List<String> dummySeeds = new ArrayList<>();
		categories.forEach((key, articles) -> {
          articles.forEach(a -> {
        	  dummySeeds.add(a.getTitle());
          
          });
		});
	return dummySeeds;
	}
	
	private static List<String> getCategories_Yahoo() {
		final List<String> dummySeeds = Arrays.asList("Society","Culture","Science","Mathematics","Health","Education",
				"Reference","Computers","Internet","Sports","Trade","Finance","Entertainment","Music","Family","Intimate relationships","Politics","Government");		
		return Collections.unmodifiableList(dummySeeds);
	}
	public static List<Category> getCategories_AgCats() {
		Map<Integer, Category> map = new HashMap<>(LabelsOfTheTexts.getLables_AG_category());
		final List<Category> dummySeeds = new ArrayList<>();
		for(Entry <Integer, Category> e: map.entrySet()) {
			dummySeeds.add(e.getValue());
		}
	//	final List<String> dummySeeds = Arrays.asList("World","Sports","Business","Science","Technology");
//			final List<String> dummySeeds = Arrays.asList("Sports","Science","Technology","World","Trade");//0.80
	
		
		//		final List<String> dummySeeds = Arrays.asList("Sports","Science","Technology","World","Business");
		//	final List<String> dummySeeds = Arrays.asList("World","Business");
	//	final List<String> dummySeeds = Arrays.asList("Business","World");
		return Collections.unmodifiableList(dummySeeds);
	}
	public static List<String> getCategories_Ag() {
		Map<Integer, Category> map = new HashMap<>(LabelsOfTheTexts.getLables_AG_category());
		final List<String> dummySeeds = new ArrayList<>();
		for(Entry <Integer, Category> e: map.entrySet()) {
			dummySeeds.add(e.getValue().getTitle());
		}
	//	final List<String> dummySeeds = Arrays.asList("World","Sports","Business","Science","Technology");
//			final List<String> dummySeeds = Arrays.asList("Sports","Science","Technology","World","Trade");//0.80
	
		
		//		final List<String> dummySeeds = Arrays.asList("Sports","Science","Technology","World","Business");
		//	final List<String> dummySeeds = Arrays.asList("World","Business");
	//	final List<String> dummySeeds = Arrays.asList("Business","World");
		return Collections.unmodifiableList(dummySeeds);
	}
	private static List<String> getCategories_DBLP() {
			final List<String> dummySeeds = Arrays.asList("Databases","Artificial intelligence","Computer hardware",
					"Systems Network Architecture","Programming languages","Theory of computation","Theoretical computer science");
			return Collections.unmodifiableList(dummySeeds);
		}
	
	

}
