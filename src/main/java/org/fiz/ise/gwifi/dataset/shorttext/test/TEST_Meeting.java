package org.fiz.ise.gwifi.dataset.shorttext.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.Doc2VecModelSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.PageCategorySingleton;
import org.fiz.ise.gwifi.Singleton.RedirectSingelton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.ReadDataset;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.dataset.train.generation.AnalysisEmbeddingandRedirectDataset;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.model.EmbeddingModel;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.test.afterESWC.BestMatchingLabelBasedOnVectorSimilarity;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.Print;
import org.fiz.ise.gwifi.util.Request_LINEServer;
import org.fiz.ise.gwifi.util.TimeUtil;
import org.fiz.ise.gwifi.util.VectorUtil;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.mingyuzuo.Wiki;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Redirect;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;
import edu.kit.aifb.gwifi.util.PageIterator;

public class TEST_Meeting {
	private static Wikipedia wikipedia = WikipediaSingleton.getInstance().wikipedia;
	private final static Dataset TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	static NLPAnnotationService service = AnnotationSingleton.getInstance().service;
	static final Logger secondLOG = Logger.getLogger("debugLogger");

	private static final String REDIRECT_FILE = Config.getString("REDIRECT_PAGE_ADDRESS", "");
	private static Map<String, String> mapRedirectPages; 

	public static void main(String[] args) throws Exception {

		System.out.println(WikipediaSingleton.getInstance().wikipedia.getCategoryById(6794513));
		
		String str = "wikipedia wiki private sector sector wikipedia encyclopedia sector fundamental economy profit controlled enterprises";
		
		List<Annotation> tmp = new ArrayList<Annotation>();
		AnnotationSingleton.getInstance().service.annotate(str, tmp);
		System.out.println(tmp);
		
		List<Article> lstCats = new ArrayList<Article>(Categories.getLabels_Snippets());
		Map<String, Double> temp= new HashMap<String, Double>();
		
		for(Article a : lstCats) {
			if (!a.getTitle().equalsIgnoreCase("The arts")&& !a.getTitle().equalsIgnoreCase("Entertainment")&&
					!a.getTitle().equalsIgnoreCase("Science")&& !a.getTitle().equalsIgnoreCase("Society")) {
				temp.put(a.getTitle(), 0.0);
			}
		}
		double sum = temp.entrySet().stream().mapToDouble //to normalize the final
				(l->l.getValue()).sum();

		LinkedHashMap<String, Double> sortedMap = new LinkedHashMap<>();
		temp.entrySet()
		.stream()
		.sorted(Map.Entry.comparingByKey())
		.forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));
		
		Print.printMap(sortedMap);
		StringBuilder labels=new StringBuilder();
		for(Double s : sortedMap.values()) {
			labels.append((s*1.)/(sum*1.)+",");
		}
		
		
		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);
		
		for (Category mainCat : setMainCategories) { 
			Article[] cArticle =null;
			if (mainCat.getTitle().equals("Animal")) {
				mainCat=WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Animals");
			}
			else if (mainCat.getTitle().equals("Artist")) {
				mainCat=WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Artists");
			}
			else if (mainCat.getTitle().equals("Album")) {
				mainCat=WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Albums");

			}
			else if (mainCat.getTitle().equals("Village")) {
				mainCat=WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Villages");

			}
					
			cArticle=mainCat.getChildArticles();
			System.out.println(mainCat+": "+cArticle.length);
		}
		Map<Category, Set<Article>> mapMainCatAndArticles = PageCategorySingleton.getInstance().mapMainCatAndArticles;
		for(Entry <Category, Set<Article>> e : mapMainCatAndArticles.entrySet()) {
			System.out.println(e.getKey()+" "+e.getValue().size());
		}
		//		ArrayList<Article> arrayList = new ArrayList<Article>(Categories.getLabels_Snippets());
		//		for(Article a : arrayList) {
		//			System.out.println(a.getTitle());
		//		}



		//mapRedirectPages= new HashMap<>(AnalysisEmbeddingandRedirectDataset.loadRedirectPages());
		//Redirect = beta Realease id:3095107

		//System.out.println(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Glossary of professional wrestling terms"));
		//		Page articleById = WikipediaSingleton.getInstance().wikipedia.getPageById(3095107);


		//		Article articleById = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Software release life cycle");
		//		Redirect[] redirects2 = articleById.getRedirects();
		//		
		//
		//        // Convert String Array to List
		//        List<Redirect> list = Arrays.asList(redirects2);
		Redirect re = new Redirect(wikipedia.getEnvironment(), 3095107);
		//        if(list.contains(re)){
		//            System.out.println("Found");
		//        }
		//(3095107);


		//		Article articleByTitle = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Software release life cycle");
		//		Redirect[] redirects = articleByTitle.getRedirects();
		//		for (int i = 0; i < redirects.length; i++) {
		//			if (redirects[i].getId()==3095107) {
		//				System.out.println(redirects[i]);
		//			}
		//			
		//		}

		//		int totalRedirect=0;
		//		int resolvedRedirect=0;
		//		int mapDoesnotContain=0;
		//		int countAnnotation=0;
		//		int nullButNotRed=0;
		//		
		//		RedirectSingelton.getInstance();
		//		Map<String, List<Article>> dataset = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION);
		//		int count =0;
		//		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		//		for(Entry<String, List<Article>> e: dataset.entrySet()) {
		//			List<Annotation> lstAnnotations = new ArrayList<>();
		//			service.annotate(e.getKey(), lstAnnotations);
		//			for (Annotation a : lstAnnotations) {
		//				if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())){
		//					if(WikipediaSingleton.getInstance().getArticle(a.getTitle())==null) { //we had so many noisy entities therefore filtering required
		//						Page p = new Page(WikipediaSingleton.getInstance().wikipedia.getEnvironment(), a.getId());
		//						if (p.getType().equals(Page.PageType.redirect)) {
		//							totalRedirect++;
		//							if (RedirectSingelton.getInstance().mapRedirectTarget.containsKey(a.getId())) {
		//								resolvedRedirect++;
		//								secondLOG.info("Redirect="+a.getTitle());
		//								secondLOG.info("Resolved="+RedirectSingelton.getInstance().mapRedirectTarget.get(a.getId()));
		//								secondLOG.info("----------------");
		//								System.out.println("Redirect="+a.getTitle());
		//								System.out.println("Resolved="+RedirectSingelton.getInstance().mapRedirectTarget.get(a.getId()));
		//							}
		//							else {
		//								mapDoesnotContain++;
		//								System.out.println("mapDoesnotContain: "+mapDoesnotContain);
		//							}
		//						}
		//						else {
		//							System.out.println("nullButNotRed: "+nullButNotRed);
		//						}
		//					}
		//
		//				}
		//			}
		//			countAnnotation++;
		//			System.out.println("countAnnotation: "+countAnnotation);
		//		}
		//		
		//		System.out.println("countAnnotation: "+countAnnotation);
		//		System.out.println("totalRedirect: "+totalRedirect);
		//		System.out.println("resolvedRedirect: "+resolvedRedirect);
		//		System.out.println("mapDoesnotContain: "+mapDoesnotContain);


		//		int count=0;
		//		try {
		//			PageIterator pageIterator = wikipedia.getPageIterator();
		//			while (pageIterator.hasNext()) {
		//				Page page = pageIterator.next();
		//				if (page.getType().equals(PageType.redirect)) {
		//					count++;
		//				}
		//			}
		//
		//			System.out.println("We have " + count+" redirectPages");
		//			System .exit(1);
		//		}
		//		catch (Exception e) {
		//			e.printStackTrace();
		//		}

		//		Article b = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Business");
		//		Map<Article, Integer> map = new HashMap<Article, Integer>();
		//		int i = 5;
		//		map.put(b, i);
		//		System.out.println(map.get(b));
		//		map.put(b, i+1);
		//		System.out.println(map.get(b));
		//		int countNotExist=0;
		//		int countTotal=0;
		//		int countline=0;
		//		List<String> lines = FileUtils.readLines(new File("/home/rtue/eclipse-workspace/gwifi/log/ag_sentence_cat_entities_2018_10"), "utf-8");
		//		for (String line : lines) {
		//			String[] split = line.split("\t\t");
		//            if (split.length==3) {
		//            	String[] split2 = split[2].split("\t");
		//            	for (int i = 0; i < split2.length; i++) {
		//            		countTotal++;
		//					Article temp =WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(split2[i]);
		//            		if (temp!=null) {
		//            			if(WikipediaSingleton.getInstance().wikipedia.getArticleById(temp.getId())==null) {
		//    						countNotExist++;
		//    					}
		//					}
		//            		
		//            		
		//				}
		//            }
		//            countline++;
		//            System.out.println("countTotal:"+countline);
		//    		System.out.println("countNotExist:"+countNotExist);
		//                
		//		}
		//		
		//		System.out.println("countTotal:"+countTotal);
		//		System.out.println("countNotExist:"+countNotExist);
		//		String str = "Doctors work to save Haiti storm survivors With no electricity or running water and short of basics like antibiotics, doctors in makeshift clinics are fighting to save survivors of Tropical Storm Jeanne -ven";
		//		String str = "Workplaces Slowly Adopt IM Instant messaging (IM) is gaining slow prominence in the workplace, but the application is used for a variety of reasons. An early 2004 study of more than 2,200 US adults from Pew Internet  amp; American Life";

		//		PageIterator pageIterator = wikipedia.getPageIterator();
		//		int i = 0;
		//		while (pageIterator.hasNext()) {
		//			if(++i%100000 == 0) {
		//				System.out.println(i + " pages have been processed!");
		//			}
		//			Page page = pageIterator.next();
		//			if (!page.getType().equals(PageType.article)) {
		//				System.out.println("title: " + page.getTitle());
		//				System.out.println("type: " + page.getType());
		//			}	
		//		}	


		str = "Is Google Page Rank Still Important? Is Google Page Rank Still Important?\\\\Since 1998 when Sergey Brin and Larry Page developed the Google search engine, it has relied (and continues to rely) on the Page Rank Algorithm. Googles reasoning behind this is, the higher the number of inbould links pointing to a website, the more valuable that ...\n";
		List<Annotation> lstAnnotations = new ArrayList<>();
		try {
			AnnotationSingleton.getInstance().service.annotate(str, lstAnnotations);
			for(Annotation a : lstAnnotations) {
				if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())){
					if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())){
						if (WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())==null&& 
								WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId()).getType().equals(PageType.redirect)) {
							String key = a.getURL().replace("http://en.wikipedia.org/wiki/", "");
							if(mapRedirectPages.containsKey(key)) {
								String tName = mapRedirectPages.get(key).replace("_", " ");
								Article article = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(tName);
								if (article!=null) {
									System.out.println(article.getTitle()+"\t");
								}

							}
						}
						else {
							System.out.println(a.getTitle()+"\t");
						}
					}



					//				System.out.println();
					//				System.out.println(WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId()));
				}
			}} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}//annotate the given text

		//		System.out.println(WikipediaSingleton.getInstance().wikipedia.getArticleById(294096));
		//		Doc2VecModelSingleton.getInstance();
		//		
		//		Set<Category> setMainCategories = CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories;
		//		for (Category mainCat : setMainCategories) { //iterate over categories and calculate a score for each of them
		//			Article amainCat = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(mainCat.getTitle());
		//			System.out.println(Doc2VecModelSingleton.getInstance().doc2vec_model.hasWord(amainCat.getTitle())); 
		//		}

		//		System.out.println(wikipedia.getArticleById(18935732));
		//		Category c = wikipedia.getCategoryByTitle("Technology");
		//		Category c1 = wikipedia.getCategoryByTitle("World");
		//		Set<Category> setC = new HashSet<>(Arrays.asList(c.getChildCategories()));
		//		
		//		System.out.println(Arrays.asList(setC));
		//		Set<Category> setC1 = new HashSet<>(Arrays.asList(c1.getChildCategories()));
		//		System.out.println(Arrays.asList(setC1));
		//		
		//		setC.retainAll(setC1);
		////		for(Article a: setA)
		////			System.out.println(a.getTitle());
		//		
		//		for(Category i: setC1) {
		//			
		////			Set<Article> setA = new HashSet<>(Arrays.asList(i.getChildArticles()));
		////			for(Category i1: setC1) {
		////				
		////				Set<Article> setB = new HashSet<>(Arrays.asList(i1.getChildArticles()));
		////				
		////				setA.retainAll(setB);
		////				for(Article a: setA)
		////					System.out.println(a.getTitle());
		////			}
		//			System.out.println(i.getTitle());
		//		}
		//		
		//		
		////		System.out.println(i.getTitle());
		//
		//		setC.retainAll(setC1);
		//		for(Category i: setC)
		//			System.out.println(i.getTitle());

		//		dataset_WEB();
		//		String DATASET_TEST_WEB ="/home/rtue/eclipse-workspace/Dataset_ShortTextClassification/ag_news_csv/test.csv";
		//		List<String> lines = FileUtils.readLines(new File(DATASET_TEST_WEB), "utf-8");
		//		System.out.println("number of the lines "+lines.size()+" dataset: "+DATASET_TEST_WEB);
		//		double count =0;
		//		for (String line:lines) {
		//			String[] split = line.split("\",\"");
		//			String title = split[1].replace("\"", "");
		//			String description = split[2].replace("\"", "");
		//			String text =title+" "+description;
		//			List<Annotation> lstAnnotations = new ArrayList<>();
		//			service.annotate(text, lstAnnotations);
		//			count+=lstAnnotations.size();
		//		}
		//		System.out.println("total number of the entities "+count);
		//		System.out.println("Avg entities "+count/lines.size());
		//		
		//		List<String> catList = new ArrayList<>();
		//		catList.add("Business");
		//		catList.add("Sports");
		//		catList.add("World");
		//		catList.add("Science");
		//		catList.add("Technology");
		//		for(String entity : catList) {
		//			System.out.println(entity+" "+wikipedia.getCategoryByTitle(entity).getId());
		//			for (Model_LINE m : Model_LINE.values()) {
		//			for(String category : catList) {
		//					System.out.println(m+" "+wikipedia.getCategoryByTitle(entity).getTitle()+" "+wikipedia.getCategoryByTitle(category).getTitle()+" "+Request_LINEServer.getSimilarity(String.valueOf(wikipedia.getCategoryByTitle(entity).getId()),String.valueOf(wikipedia.getCategoryByTitle(category).getId()), m));
		//				}
		//			}
		//			
		//		}

		//		
		//		List<String> lines = new ArrayList<>(TestBasedonDatasets.generateRandomDataset_AG());
		//		System.out.println("Rima");
		//		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		//		String str = "Mozart was a prolific and influential composer";//"Intel Delays Launch of Projection TV Chip";//"China's Red Flag Linux to focus on enterprise";//"North Korea Talks Still On, China Tells Downer";//"Loosing the War on Terrorism";
		//		List<Annotation> lstAnnotations = new ArrayList<>();
		//		service.annotate(str, lstAnnotations);
		//		//
		//		for(Annotation a : lstAnnotations) {
		//			System.out.println(a);
		//		}

		//		String[] arrLines = new String[lines.size()];
		//		arrLines = lines.toArray(arrLines);
		//		int count=0;
		//		
		//		for (int i = 0; i < arrLines.length; i++) {
		//			String[] split = arrLines[i].split("\",\"");
		//			String title = split[1].replace("\"", "");
		//			String description = split[2].replace("\"", "");
		//			
		//			List<Annotation> lstAnnotations = new ArrayList<>();
		//			service.annotate(title, lstAnnotations);
		//			if (lstAnnotations.size()==0) {
		//				System.out.println("Size is :" +title);
		//			}
		//			count+=lstAnnotations.size();
		//			
		//		}
		//		double per = count/Double.valueOf(arrLines.length);
		//		System.out.println("per: "+per);


		//		List<String> lst = new ArrayList<>(Categories.getCategoryList(TestDatasetType_Enum.AG));
		//		for(String str: lst) {
		//			for(String str2: lst) {
		//				if (!str.equals(str2)) {
		//					System.out.println(str+" "+str2+" "+EmbeddingsService.getSimilarity(String.valueOf(wikipedia.getCategoryByTitle(str).getId()),String.valueOf(wikipedia.getCategoryByTitle(str2).getId()))); 
		//				}
		//			}
		//		}

		//	
		//		List<String> entityList = new ArrayList<>();
		////		entityList.add("Sports");
		////		entityList.add("Physics");
		//		entityList.add("Trade");
		////		entityList.add("World");
		//
		//		for(String entity : entityList) {
		//			System.out.println(entity+"  "+wikipedia.getCategoryByTitle(entity)+ " "+String.valueOf(wikipedia.getCategoryByTitle(entity).getId()));
		//			List<String> result = new ArrayList<>(LINE_modelSingleton.getInstance().lineModel.wordsNearest(String.valueOf(wikipedia.getCategoryByTitle(entity).getId()), 20));
		//			for(String str: result) {
		//				System.out.println(wikipedia.getPageById(Integer.parseInt(str)).getTitle());
		//			}
		//			System.out.println();
		//		}
		//		//		entityList.add("Company");
		//		//		entityList.add("London");
		//		//		for(String entity : entityList) {
		//			System.out.println(entity+"  "+wikipedia.getArticleByTitle(entity)+ " "+String.valueOf(wikipedia.getArticleByTitle(entity).getId()));
		//			
		////			System.out.println(entity);
		//			List<String> result = new ArrayList<>(model.wordsNearest(String.valueOf(wikipedia.getArticleByTitle(entity).getId()), 10));
		//			
		//			
		//			for(String str: result) {
		//								System.out.println(wikipedia.getPageById(Integer.parseInt(str)).getTitle());
		//							}
		////			//for (Model_LINE m : Model_LINE.values()) {
		////			//System.out.println(m+": "+getMostSimilarCategory(wikipedia.getArticleByTitle(entity).getId(),m));
		////			//System.out.println(getMostSimilarCategory(wikipedia.getArticleByTitle(entity).getId(), Model_LINE.LINE_COMBINED_2nd));
		////			//}
		//////			List<String> result = new ArrayList<>(EmbeddingsService.getMostSimilarConcepts(String.valueOf(wikipedia.getArticleByTitle(entity).getId()), null, 12));
		////			//			for(String str: result) {
		////			//				System.out.println(wikipedia.getPageById(Integer.parseInt(str)).getTitle());
		////			//			}
		//			System.out.println();
		//		}


		//		System.out.println(" "+"  "+wikipedia.getCategoryByTitle("Sports")+ " "+String.valueOf(wikipedia.getCategoryByTitle("Sports").getId()));
		//		System.out.println(" "+"  "+wikipedia.getCategoryByTitle("Physics")+ " "+String.valueOf(wikipedia.getCategoryByTitle("Physics").getId()));
		//		System.out.println(" "+"  "+wikipedia.getCategoryByTitle("Trade")+ " "+String.valueOf(wikipedia.getCategoryByTitle("Trade").getId()));
		//
		//		entityList = new ArrayList<>();
		//
		//		entityList.add("Sports");
		//		entityList.add("Physics");
		//		entityList.add("Trade");
		//
		//		for(String entity : entityList) {
		//			System.out.println(entity+"  "+wikipedia.getCategoryByTitle(entity)+ " "+String.valueOf(wikipedia.getCategoryByTitle(entity).getId()));
		//			List<String> result = new ArrayList<>(model.wordsNearest(String.valueOf(wikipedia.getCategoryByTitle(entity).getId()), 10));
		//			for(String str: result) {
		//				System.out.println(wikipedia.getPageById(Integer.parseInt(str)).getTitle());
		//			}
		//			System.out.println();
		//		}


		//		List<String> catList = new ArrayList<>();
		//		catList.add("Business");
		//		catList.add("Sports terminology");
		//		catList.add("Mathematics");
		//		catList.add("Continents");
		//		catList.add("Sports rules and regulations");
		//		for(String entity : catList) {
		//			System.out.println(entity+" "+wikipedia.getCategoryByTitle(entity).getId());
		//			for (Model_LINE m : Model_LINE.values()) {
		//				Map<Category, Double > map = new LinkedHashMap<>(getMostSimilarCategory(wikipedia.getCategoryByTitle(entity),m));
		//				int i=0;
		//				for (Entry<Category, Double > e: map.entrySet()) {
		//					System.out.println(m+" "+entity+" "+e.getKey());
		//					if (++i>11) {
		//						break;	
		//					}
		//				}
		//
		//	

		//	for(String entity : entityList) {
		//		System.out.println(entity);
		//		List<String> result = new ArrayList<>(EmbeddingsService.getMostSimilarConcepts(String.valueOf(wikipedia.getArticleByTitle(entity).getId()), null, 12));
		//		for(String str: result) {
		//			System.out.println(wikipedia.getPageById(Integer.parseInt(str)).getTitle());
		//		}
		//					System.out.println();
		//		//		}
		//	}


		//		String category = "Sports";
		//		List<String> result = new ArrayList<>(EmbeddingsService.getMostSimilarConcepts(String.valueOf(wikipedia.getCategoryByTitle(category).getId()), null, 10));
		//		for(String str: result) {
		//
		//			System.out.println(wikipedia.getPageById(Integer.parseInt(str)));
		//			//				
		//			//			
		//			//			
		//		}

	}


	public static void findSimilarityBetweenCats() {
		Set<Category> categories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);
		for(Category c : categories) {
			int aId = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(c.getTitle()).getId();

			for(Category cC : categories) {
				int cId = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(cC.getTitle()).getId();
				System.out.println(c.getTitle()+" "+cC.getTitle()+": "+LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(aId), String.valueOf(cId)));
			}
		}
	}
	public static void dataset_WEB() {
		String DATASET_TEST_WEB ="/home/rtue/eclipse-workspace/Dataset_ShortTextClassification/data-web-snippets/test.txt";
		try {
			List<String> lines = FileUtils.readLines(new File(DATASET_TEST_WEB), "utf-8");
			System.out.println("size of the file "+lines.size());
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			double count=0;
			for (int i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split(" ");
				String label = split[split.length-1];
				String snippet = arrLines[i].substring(0, arrLines[i].length()-(label).length()).trim();
				//				String snippet ="IBM adds midrange server to eServer lineup";
				List<Annotation> lstAnnotations = new ArrayList<>();
				service.annotate(snippet, lstAnnotations);
				System.out.println(lstAnnotations);
				count+=lstAnnotations.size();
			}
			System.out.println("total number of the entities "+count);
			System.out.println("Avg entities "+count/lines.size());

		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public static  Map<Category, Double> getMostSimilarCategory(Category c,EmbeddingModel m)
	{
		Set<Category> categories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setAllCategories);
		Map<Category, Double> map = new HashMap<>();
		for(Category category:categories){
			double similarity = 0.0;
			try {
				similarity=Request_LINEServer.getSimilarity(String.valueOf(c.getId()), String.valueOf(category.getId()), m);
				if (similarity>0) {
					map.put(category, similarity);
				}
			} catch (Exception e) {
				System.out.println("exception finding the similarity: "+similarity);
			}
		}	

		Map<Category, Double> mapSorted = new LinkedHashMap<>(MapUtil.sortByValueDescending(map));
		return mapSorted;
		//		return MapUtil.getFirst(mapSorted);

	}
	public static  Entry<Category, Double> getMostSimilarCategory(Integer id,EmbeddingModel m)
	{
		Set<Category> categories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setAllCategories);
		Map<Category, Double> map = new HashMap<>();
		for(Category category:categories){
			double similarity = 0.0;
			try {
				similarity=Request_LINEServer.getSimilarity(String.valueOf(id), String.valueOf(category.getId()), m);
				if (similarity>0) {
					map.put(category, similarity);
				}
			} catch (Exception e) {
				System.out.println("exception finding the similarity: "+similarity);
			}
		}	

		Map<Category, Double> mapSorted = new LinkedHashMap<>(MapUtil.sortByValueDescending(map));
		return MapUtil.getFirst(mapSorted);

	}

}
