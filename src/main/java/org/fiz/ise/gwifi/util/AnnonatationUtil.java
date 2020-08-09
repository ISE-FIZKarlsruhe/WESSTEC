package org.fiz.ise.gwifi.util;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingletonAnnotationFiltering;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.Dataset;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.LexedTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;

public class AnnonatationUtil {
	private final static Dataset TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	private final static Integer NUMBER_OF_THREADS= Config.getInt("NUMBER_OF_THREADS",-1);
	static CopyOnWriteArrayList<Annotation> listAnnotations = new CopyOnWriteArrayList<>();	
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");
	private final static NLPAnnotationService service = AnnotationSingleton.getInstance().service;
	private static SynchronizedCounter synCountNumberOfEntityPairs;

	private static ExecutorService executor;
	public static String getAnnotationsXML(String shortText) {
		try {
			NLPAnnotationService service = AnnotationSingleton.getInstance().service;
			List<Annotation> lstAnnotations = new ArrayList<>();
			String xmlResponce= service.annotate(shortText, lstAnnotations);
			return xmlResponce;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}
	public static List<Annotation> findAnnotationAll_FilterWEB(List<String> lst) {
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		List<Annotation> result = new ArrayList<>();
		try {
			for(String text:lst) {
				List<Annotation> lstAnnotations = new ArrayList<>();
				service.annotate(text, lstAnnotations);
				for(Annotation a : lstAnnotations) {
					if (!AnnonatationUtil.getEntityBlackList_WebSnippets().contains(a.getId())) { //we had so many noisy entities therefore filtering required
						result.add(a);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	public static List<Annotation> findAnnotationAll_FilterAG(List<String> lst) {
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		List<Annotation> result = new ArrayList<>();
		try {
			for(String text:lst) {
				List<Annotation> lstAnnotations = new ArrayList<>();
				service.annotate(text, lstAnnotations);
				for(Annotation a : lstAnnotations) {
					if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())) { //we had so many noisy entities therefore filtering required
						result.add(a);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static List<Annotation> findAnnotationAll_filter(List<String> lst, List<Integer> lstBlack) {
		try {
			synCountNumberOfEntityPairs=new SynchronizedCounter();

			executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);		
			for (int i = 0; i < lst.size(); i++) {
				executor.execute(handleAnnotate_filter(lst.get(i),i,lstBlack));
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Sentences with no entitiy "+synCountNumberOfEntityPairs.value());
		return listAnnotations;
	}
	private static  Runnable handleAnnotate_filter(String str,int count, List<Integer> lstBlack)  {
		return () -> {
			try {
				List<Annotation> lstAnnotations = new ArrayList<>();
				service.annotate(str, lstAnnotations);
				if (lstAnnotations.isEmpty()) {
					synCountNumberOfEntityPairs.increment();
				}
				for(Annotation an: lstAnnotations) {
					if (!lstBlack.contains(an.getId())&&!StringUtil.isNumeric(an.getTitle())) {
						listAnnotations.add(an);
					}
				}
				//System.out.println("Number of processed samples:"+count);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		};
	}
	public static void writeAnnotationFile(List<String> dataset) {
		try {
			System.out.println("Size of the dataset"+dataset.size());
			executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);		
			for(String str : dataset) {
				executor.execute(handleWriteAnnotation(str));
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static  Runnable handleWriteAnnotation(String str)  {
		return () -> {
			try {
				List<Annotation> lstAnnotations = new ArrayList<>();
				service.annotate(str, lstAnnotations);
				StringBuilder sbuilder = new StringBuilder(str+"\t\t");
				for (Annotation a : lstAnnotations) {
					sbuilder.append(a.getId()+",");
				}
				secondLOG.info(sbuilder.toString().subSequence(0, sbuilder.toString().length()-1));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		};
	}
	public static List<Annotation> findAnnotationAll(List<String> lst) {
		try {
			synCountNumberOfEntityPairs=new SynchronizedCounter();

			executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);		
			for (int i = 0; i < lst.size(); i++) {
				executor.execute(handleAnnotate(lst.get(i),i));
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		//		List<Annotation> result = new ArrayList<>();
		//		try {
		//			for(String text:lst) {
		//				List<Annotation> lstAnnotations = new ArrayList<>();
		//				service.annotate(text, lstAnnotations);
		//				result.addAll(lstAnnotations);
		//			}
		//		} catch (Exception e) {
		//			e.printStackTrace();
		//		}

		System.out.println("Sentences with no entitiy "+synCountNumberOfEntityPairs.value());
		return listAnnotations;
	}

	private static  Runnable handleAnnotate(String str,int count)  {
		return () -> {
			try {
				List<Annotation> lstAnnotations = new ArrayList<>();
				service.annotate(str, lstAnnotations);
				if (lstAnnotations.isEmpty()) {
					synCountNumberOfEntityPairs.increment();
				}
				listAnnotations.addAll(lstAnnotations);
				System.out.println("Number of processed samples:"+count);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		};
	}
	public static int countALinkToMainCat(String str, Category c, int depth) {
		Set<Category> categories = new HashSet<>(CategoryUtil.generateCategoryTree(c, depth));
		categories.add(c);
		List<Annotation> allAnnotation = new ArrayList<>(findAnnotationAll(Arrays.asList(str)));
		int count=0;
		for(Annotation a : allAnnotation) {
			if (WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())!=null) {
				Set<Category> entityCats = new HashSet<>(Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId()).getParentCategories()));
				entityCats.retainAll(categories);
				if (entityCats.size()>0) {
					count++;
				}
			}
		}
		return count;
	}
	public static boolean hasALink(Annotation a, Category c) {
		Set<Category> categories = new HashSet<>(CategorySingletonAnnotationFiltering.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).mapMainCatAndSubCats.get(c));
		//		if (c.getTitle().equals("Science")||c.getTitle().equals("Technology")) {
		//			categories.addAll(CategoryUtil.generateCategoryTree(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science and technology"), depth));
		//		}
		if (WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())!=null) {
			Set<Category> entityCats = new HashSet<>(Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId()).getParentCategories()));
			for(Category t: entityCats) {
				if (categories.contains(t)) {
					return true;
				}
			}
		}
		return false;
	}
	public static boolean hasALink(String str, Category c, int depth) {
		Set<Category> categories = new HashSet<>(CategorySingletonAnnotationFiltering.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).mapMainCatAndSubCats.get(c));
		List<Annotation> allAnnotation = new ArrayList<>(findAnnotationAll(Arrays.asList(str)));
		for(Annotation a : allAnnotation) {


			if (c.getTitle().equals("Science")||c.getTitle().equals("Technology")||c.getTitle().equals("Science and technology")) {
				//categories = new HashSet<>(CategoryUtil.generateCategoryTree(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Business"), depth));
				//categories.addAll(CategoryUtil.generateCategoryTree(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Companies listed on NASDAQ"), depth));
				categories.addAll(CategoryUtil.generateCategoryTree(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science and technology"), depth));
				//categories.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Companies listed on NASDAQ"));
			}

			else if (c.getTitle().equals("World")) {
				categories = new HashSet<>(CategoryUtil.generateCategoryTree(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Countries by continent"), depth));
				Set<Category> sTemp = new HashSet<>(CategoryUtil.generateCategoryTree(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("World government"), depth));
				categories.addAll(sTemp);
			}
			if (WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())!=null) {
				Set<Category> entityCats = new HashSet<>(Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId()).getParentCategories()));
				for(Category t: entityCats) {
					if (categories.contains(t)) {
						return true;
					}
				}
			}
		}
		return false;
	}


	public static Map<String, Integer> findFreqOfEntitySortedMap(List<Annotation> lst ) {
		Map<String, Integer> resultFreq = new HashMap<>();
		for(Annotation a :lst  ) {
			if (resultFreq.containsKey(a.getTitle()+"\t"+a.getId())) {
				resultFreq.put(a.getTitle()+"\t"+a.getId(), (resultFreq.get(a.getTitle()+"\t"+a.getId())+1));
			}
			else{
				resultFreq.put(a.getTitle()+"\t"+a.getId(), 1);
			}
		}
		Map<String, Integer> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(resultFreq));
		return sortedMap;
	}
	public static void getAvgAnnotationOfDatasets(List<String> dataset) {
		List<Annotation> findAnnotationAll = AnnonatationUtil.findAnnotationAll(dataset);
		//		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		//		int counttotalEntity=0;
		//		int countSentencesNoEntity=0;
		try {
			//			for(String str: dataset) {
			//				List<Annotation> lstAnnotations = new ArrayList<>();
			//				service.annotate(str, lstAnnotations);
			//				counttotalEntity+=lstAnnotations.size();
			//
			//				if (lstAnnotations.size()==0) {
			//					countSentencesNoEntity++;
			//				}
			//			}
			System.out.println("counttotalAnnotation: "+findAnnotationAll.size());
			System.out.println("countSentencesNoAnnotation: "+synCountNumberOfEntityPairs);
			System.out.println("AvgAnnCount: "+(double)findAnnotationAll.size()*1.0/dataset.size()*1.0);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void findFreqOfAnnotation(List<Annotation> lstAllAnnotation ,String fileName) {
		Map<String, Integer> resultFreq = new HashMap<>();
		for(Annotation a :lstAllAnnotation  ) {
			if (!StringUtil.isNumeric(a.getTitle())) {
				String key=a.getTitle()+"-"+a.getId();
				resultFreq.merge(key, 1, Integer::sum);
			}
		}
		Map<String, Integer> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(resultFreq));
		FileUtil.writeDataToFile(sortedMap,fileName);
		System.out.println("Finished one dataset writing: " + fileName);
	}
	public static int getFreqOfAnnotation(List<String> dataset ,String fileName) {
		List<Annotation> lstAllAnnotation = new ArrayList<>(AnnonatationUtil.findAnnotationAll(dataset));

		Map<String, Integer> resultFreq = new HashMap<>();
		for(Annotation a :lstAllAnnotation  ) {
			String key=a.getTitle()+"-"+a.getId();
			resultFreq.merge(key, 1, Integer::sum);
		}
		Map<String, Integer> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(resultFreq));
		FileUtil.writeDataToFile(sortedMap,fileName);
		System.out.println("Finished one dataset writing: " + fileName);
		return sortedMap.size();
	}
	public Map<Annotation, Double> analizeWeightOfAnnotations(List<Annotation> lst) {
		Map<Annotation, Double> annotations = new HashMap<>();
		for(Annotation a : lst) {
			annotations.put(a, a.getWeight());
		}
		Map<Annotation, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(annotations));
		return sortedMap;
	}


	public static List<Integer> getEntityBlackList_MR(){
		List<Integer> lstidBlack = new ArrayList<>();
		lstidBlack.add(21555729); //Film
		lstidBlack.add(76749); //Character (arts)
		lstidBlack.add(21554680); //Film director
		lstidBlack.add(83597);//Screenplay
		lstidBlack.add(20914042);//Comedy
		return lstidBlack;

	}

	public static List<Integer> getEntityBlackList_AGNews(){
		List<Integer> lstidBlack = new ArrayList<>();
		lstidBlack.add(18935732);
		lstidBlack.add(60534); //Shilling
		lstidBlack.add(18998750); //Reuters
		lstidBlack.add(3434750);//Inited States
		lstidBlack.add(54635);//Tuesday
		lstidBlack.add(54407);

		//New black list
		lstidBlack.add(54634);//Wednesday
		lstidBlack.add(266139);//ThursdayBand
		lstidBlack.add(169788);//FridayFilm
		lstidBlack.add(145418);//CompanyMilitary Unit
		lstidBlack.add(35524);//2004
		lstidBlack.add(4249942);//Dollar (band)-4249942
		lstidBlack.add(557667);//Face (professional wrestling)
		lstidBlack.add(1220573);//UPDATE news
		lstidBlack.add(13623073);//CUT film
		lstidBlack.add(2605250);//CUTS
		lstidBlack.add(1691433);//NEW (TV station)
		lstidBlack.add(471981);//Agence France-Presse

		return lstidBlack;

	}
	public static List<Integer> getEntityBlackList_DBp(){
		List<Integer> lst = new ArrayList<>();
		lst.add(3194908);// \n
		lst.add(600744);// !!!
		lst.add(3434750);// US
		lst.add(31717);// United Kingdom
		lst.add(5042916);// Canada
		lst.add(5405);// China
		lst.add(5407);// California
		//lst.add(4918223);//Company
		//lst.add(28022);//School
		lst.add(10568);//Association football
		lst.add(554992);//Secondary school
		lst.add(14653);//Iran-
		lst.add(11600);//Persian language-
		lst.add(60534);//Shilling-
		lst.add(8569916);//English language-
		lst.add(14533);//India-
		lst.add(645042);//New York City-	1968
		lst.add(4689264);//Australia-	
		
		return lst;
	}
	public static int getCorrectAnnotation_DBp(int id){
		if (id==540448) {
			return 285436;
		}
		else if (id==145418) {
			return 4918223;
		}
		return id;
	}
	public static List<Integer> getEntityBlackList_Yahoo(){
		List<Integer> lst = new ArrayList<>();
		lst.add(3194908);// \n
		lst.add(600744);// !!!
		lst.add(3434750);// US
		lst.add(43975);// Yes (band)
		lst.add(2190991);// Why (Annie Lennox song)
		lst.add(251573);//Can (band)
		lst.add(42445);//Unified atomic mass unit-
		lst.add(251573);//Can (band)-
		lst.add(5042765);//God
		lst.add(1095706);//Jesus
		lst.add(25414);//Religion
		lst.add(33306);//Water
		lst.add(19192);//Mean
		lst.add(42445);//Unified atomic mass unit
		lst.add(523137);//Try-
		lst.add(18337522);//Christian
		lst.add(3390);//Bible  
		lst.add(11064);//Faith-
		return lst;
	}
	public static int check_trec_annotation(Article a) {
		System.out.println("a.getId()"+a.getId());
		if (a.getId()==2190991) {//Why (Annie Lennox song)
			int id = 42446; //Reason
			return id;
			//			return WikipediaSingleton.getInstance().wikipedia.getArticleById(id);
		}
		return a.getId();
	}

	public static List<Integer> getEntityBlackList_WebSnippets(){
		List<Integer> lstidBlack = new ArrayList<>();
		lstidBlack.add(5043734); //Wikipedia
		return lstidBlack;

	}
	public static List<Annotation> filterAnnotation(List<Annotation> lst) {
		List<Integer> lstidBlack = new ArrayList<>(getEntityBlackList_AGNews());
		List<Annotation> filteredList = new ArrayList<>();
		int countFiltered=0;

		for(Annotation a : lst) {
			if (!lstidBlack.contains(a.getId())) {
				filteredList.add(a);
			}
			else {
				countFiltered++;
			}
		}
		System.out.println("Filtered number of elements "+countFiltered);
		return filteredList;
	}
	private List<Annotation> findMaxWeightedAnnotation(List<Annotation> contextAnnotations) {
		double max = 0.0;
		Annotation result = null;
		for (Annotation a : contextAnnotations) {
			if (a.getWeight() > max) {
				max = a.getWeight();
				result = a;
			}
		}
		return Arrays.asList(result);
	}

}
