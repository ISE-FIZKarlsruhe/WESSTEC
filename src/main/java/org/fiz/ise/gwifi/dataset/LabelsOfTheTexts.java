package org.fiz.ise.gwifi.dataset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;

public class LabelsOfTheTexts {
	private static Wikipedia wikipedia = WikipediaSingleton.getInstance().wikipedia;

	public static List<String> getLabels_Snippets() {
		final List<String> labels = new ArrayList<String>(); 
		labels.add("business");
		labels.add("computers");
		labels.add("culture-arts-entertainment");
		labels.add("education-science");
		labels.add("engineering");
		labels.add("health");
		labels.add("politics-society");
		labels.add("sports");
		return Collections.unmodifiableList(labels);
	}

	public static Map<Integer, Article> getLables_Yahoo_article()
	{
		Map<Integer, Article> mapLabel = new HashMap<>();
		mapLabel.put(1, wikipedia.getArticleByTitle("Society"));
		mapLabel.put(2, wikipedia.getArticleByTitle("Science"));
		mapLabel.put(3, wikipedia.getArticleByTitle("Health"));
		mapLabel.put(4, wikipedia.getArticleByTitle("Education"));
		mapLabel.put(5, wikipedia.getArticleByTitle("Computers"));
		mapLabel.put(6, wikipedia.getArticleByTitle("Sports"));
		mapLabel.put(7, wikipedia.getArticleByTitle("Business"));
		mapLabel.put(8, wikipedia.getArticleByTitle("Entertainment"));
		mapLabel.put(9, wikipedia.getArticleByTitle("Family"));
		mapLabel.put(10, wikipedia.getArticleByTitle("Politics"));
		return mapLabel;
	}


	//Company, EducationalInstitution, Artist, Athlete,OfficeHolder,MeanOfTransportation,Building,NaturalPlace
	//Village,Animal,Plant,	Album,Film,WrittenWork

	public static Map<Integer, Article> getLables_DBP_article()
	{
		Map<Integer, Article> mapLabel = new HashMap<>();
		mapLabel.put(1, wikipedia.getArticleByTitle("Company"));
		mapLabel.put(2, wikipedia.getArticleByTitle("Educational institution"));
		mapLabel.put(3, wikipedia.getArticleByTitle("Artist"));
		mapLabel.put(4, wikipedia.getArticleByTitle("Athlete"));
		mapLabel.put(5, wikipedia.getArticleByTitle("Office Holder"));
		mapLabel.put(6, wikipedia.getArticleByTitle("Transport"));
		mapLabel.put(7, wikipedia.getArticleByTitle("Building"));
		mapLabel.put(8, wikipedia.getArticleByTitle("Natural environment"));
		mapLabel.put(9, wikipedia.getArticleByTitle("Village"));
		mapLabel.put(10, wikipedia.getArticleByTitle("Animal"));
		mapLabel.put(11, wikipedia.getArticleByTitle("Plant"));
		mapLabel.put(12, wikipedia.getArticleByTitle("Album"));
		mapLabel.put(13, wikipedia.getArticleByTitle("Film"));
		mapLabel.put(14, wikipedia.getArticleByTitle("Writing"));
		return mapLabel;

	}
	public static List<String> getLabels_DBP_category() {
		Map<Integer, Article> map = new HashMap<>(getLables_DBP_article());
		final List<String> dummySeeds = new ArrayList<>();
		for(Entry <Integer, Article> e: map.entrySet()) {
			if (e.getValue().getTitle().equals("Company")) {
				dummySeeds.add("Companies");	
			}
			else if (e.getValue().getTitle().equals("Educational institution")) {
				dummySeeds.add("Educational institutions");	
			}
			else if (e.getValue().getTitle().equals("Athlete")) {
				dummySeeds.add("Athletic sports");	
			}
			else if (e.getValue().getTitle().equals("Office-holder")) {
				dummySeeds.add("Office-holders");	
			}
			else if (e.getValue().getTitle().equals("Plant")) {
				dummySeeds.add("Plants");	
			}
			else if (e.getValue().getTitle().equals("Animal")) {
				dummySeeds.add("Animals");	
			}
			else if (e.getValue().getTitle().equals("Artist")) {
				dummySeeds.add("Artists");	
			}
			else if (e.getValue().getTitle().equals("Album")) {
				dummySeeds.add("Albums");	
			}
			else if (e.getValue().getTitle().equals("Village")) {
				dummySeeds.add("Villages");	

			}
			else
				dummySeeds.add(e.getValue().getTitle());
		}
		return Collections.unmodifiableList(dummySeeds);
	}
	public static List<String> getLabels_DBP() {
		Map<Integer, Article> map = new HashMap<>(getLables_DBP_article());
		final List<String> dummySeeds = new ArrayList<>();
		for(Entry <Integer, Article> e: map.entrySet()) {
			dummySeeds.add(e.getValue().getTitle());
		}
		return Collections.unmodifiableList(dummySeeds);
	}
	public static Map<String, Article> getLables_TREC_article()
	{
		Map<String, Article> mapLabel = new HashMap<>();
		mapLabel.put("ABBR", wikipedia.getArticleByTitle("Abbreviation"));
		mapLabel.put("DESC", wikipedia.getArticleByTitle("Reason"));
		mapLabel.put("ENTY", wikipedia.getArticleByTitle("Fear"));
		mapLabel.put("HUM", wikipedia.getArticleByTitle("Actor"));
		mapLabel.put("LOC", wikipedia.getArticleByTitle("Country"));
		mapLabel.put("NUM", wikipedia.getArticleByTitle("Average"));
		return mapLabel;
	}
/*
 * Wide and 
 */
	public static Map<Integer, Article> getLables_AG_article()
	{
		Map<Integer, Article> mapLabel = new HashMap<>();
		mapLabel.put(1, wikipedia.getArticleByTitle("Politics"));
		mapLabel.put(2, wikipedia.getArticleByTitle("Sport"));
		mapLabel.put(3, wikipedia.getArticleByTitle("Business"));
		mapLabel.put(4, wikipedia.getArticleByTitle("Software"));
		return mapLabel;
	}
	public static Map<Integer, Category> getLables_AG_category()
	{
		Map<Integer, Category> mapLabel = new HashMap<>();
		mapLabel.put(1, wikipedia.getCategoryByTitle("World"));
		//		mapLabel.put(1, wikipedia.getCategoryByTitle("Politics"));
		mapLabel.put(2, wikipedia.getCategoryByTitle("Sports"));
		mapLabel.put(3, wikipedia.getCategoryByTitle("Business"));
		//		mapLabel.put(4, wikipedia.getCategoryByTitle("Software"));
		mapLabel.put(4, wikipedia.getCategoryByTitle("Technology"));

		return mapLabel;
	}

	public static Map<Article,Integer> getArticleValue_AG()
	{
		Map<Integer, Article>  mapLabel = new HashMap<>(getLables_AG_article());
		Map<Article,Integer> resultLabel = new HashMap<>();
		for(Entry <Integer, Article> e : mapLabel.entrySet()) {
			resultLabel.put(e.getValue(),e.getKey());
		}
		return resultLabel;
	}
	public static Map<Category,Integer> getCatValue_AG()
	{
		Map<Integer, Category>  mapLabel = new HashMap<>(getLables_AG_category());
		Map<Category,Integer> resultLabel = new HashMap<>();
		for(Entry <Integer, Category> e : mapLabel.entrySet()) {
			resultLabel.put(e.getValue(),e.getKey());
		}
		return resultLabel;
	}

	public static Map<String, List<Category>> getLables_20News()
	{
		Map<String, List<Category>> mapLabel = new HashMap<>();
		List<Category> arrList = new ArrayList<Category>();
		//		arrList.add(wikipedia.getCategoryByTitle("Federal Bureau of Investigation"));
		arrList.add(wikipedia.getCategoryByTitle("Weapons"));
		mapLabel.put("talk.politics.guns", arrList);

		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Middle East"));
		mapLabel.put("talk.politics.mideast", arrList);

		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Politics"));
		mapLabel.put("talk.politics.misc", arrList);

		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Atheism"));
		mapLabel.put("alt.atheism", arrList);

		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Christianity"));
		arrList.add(wikipedia.getCategoryByTitle("Christians"));
		mapLabel.put("soc.religion.christian", arrList);

		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Religion"));
		mapLabel.put("talk.religion.misc", arrList);

		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("IBM"));
		arrList.add(wikipedia.getCategoryByTitle("Computer hardware"));
		mapLabel.put("comp.sys.ibm.pc.hardware", arrList);

		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Macintosh computers"));
		arrList.add(wikipedia.getCategoryByTitle("Apple Inc."));
		arrList.add(wikipedia.getCategoryByTitle("Apple Inc. hardware"));
		mapLabel.put("comp.sys.mac.hardware", arrList);

		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Computer graphics"));
		mapLabel.put("comp.graphics", arrList);

		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Windows software"));
		arrList.add(wikipedia.getCategoryByTitle("X Window System"));
		mapLabel.put("comp.windows.x", arrList);

		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Operating systems"));
		mapLabel.put("comp.os.ms-windows.misc", arrList);

		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Cars"));
		mapLabel.put("rec.autos", arrList);

		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Motorcycles"));
		mapLabel.put("rec.motorcycles", arrList);

		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Baseball"));
		mapLabel.put("rec.sport.baseball", arrList);

		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Hockey"));
		mapLabel.put("rec.sport.hockey", arrList);

		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Electronics"));
		mapLabel.put("sci.electronics", arrList);

		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Cryptography"));
		mapLabel.put("sci.crypt", arrList);

		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Medicine"));
		mapLabel.put("sci.med", arrList);

		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Space"));
		mapLabel.put("sci.space", arrList);

		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Sales"));
		mapLabel.put("misc.forsale", arrList);
		return mapLabel;
		//		Israel, Arab, Jews, Muslims
		//		Human_sexuality, LGBT, Male homosexuality
		//		Atheist, Category:Christianity,Atheism, God ,Islam 
	}	
	public static Map<Integer, String> getLables_Yahoo()
	{
		Map<Integer, String> mapLabel = new HashMap<>();
		mapLabel.put(1, "Society-Culture");
		mapLabel.put(2, "Science-Mathematics");
		mapLabel.put(3,"Health");
		mapLabel.put(4,"Education-Reference");
		mapLabel.put(5,"Computers-Internet");
		mapLabel.put(6,"Sports");
		mapLabel.put(7,"Business-Finance");
		mapLabel.put(8,"Entertainment-Music");
		mapLabel.put(9,"Family-Intimate relationships");
		mapLabel.put(10,"Politics-Government");
		return mapLabel;
	}
	public static Map<Integer, Category> getLables_DBLP()
	{
		Map<Integer, Category> mapLabel = new HashMap<>();
		//mapLabel.put(1, wikipedia.getCategoryByTitle);
		//mapLabel.put(2, wikipedia.getCategoryByTitle("Artificial intelligence"));
		mapLabel.put(3, wikipedia.getCategoryByTitle("Computer hardware"));
		mapLabel.put(4, wikipedia.getCategoryByTitle("Systems Network Architecture"));
		mapLabel.put(5, wikipedia.getCategoryByTitle("Programming languages"));
		mapLabel.put(6, wikipedia.getCategoryByTitle("Theory of computation"));
		mapLabel.put(7, wikipedia.getCategoryByTitle("Theoretical computer science"));
		return mapLabel;
	}

}
