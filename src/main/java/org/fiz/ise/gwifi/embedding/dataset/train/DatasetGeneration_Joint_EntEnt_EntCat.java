package org.fiz.ise.gwifi.embedding.dataset.train;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.FilteredWikipediaPagesSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.SynchronizedCounter;
import org.fiz.ise.gwifi.util.TimeUtil;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.util.PageIterator;

public class DatasetGeneration_Joint_EntEnt_EntCat {
	private static final Logger LOG = Logger.getLogger(DatasetGeneration_Joint_EntEnt_EntCat.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger thirdLOG = Logger.getLogger("reportsLogger");

	static final long now = System.currentTimeMillis();
	private ExecutorService executor;
	private static SynchronizedCounter countArticle;
	private static SynchronizedCounter countLine;
	private final Integer NUMBER_OF_THREADS=  Config.getInt("NUMBER_OF_THREADS",-1);
	private static final Map<Integer,Set<Article>> cache = new ConcurrentHashMap<>();
	private static final Map<Integer,Set<Article>> preCache = new ConcurrentHashMap<>();
	private static List<String> globalList;
	private static Set<String> globalSet;
	private static Wikipedia wikipedia;
	
	public static void main(String[] args) {
		DatasetGeneration_Joint_EntEnt_EntCat data = new DatasetGeneration_Joint_EntEnt_EntCat();
		data.initializeVariables();
		System.out.println("Thread started...");
		final Thread t = new Thread (new Runnable() {
			@Override
			public void run() {
				while(true) {
					System.out.println("number of article processed "+ countArticle.value()+" minutes "+ TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now)+ " globalSetSize "+globalSet.size()+" globalList "+globalList.size());
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}			
				}
			}
		});
		t.setDaemon(true);
		t.start();
		data.generateDatasetEntityEntiy_EntityCategory();
	}
	private void initializeVariables() {
		long now = TimeUtil.getStart();
		wikipedia= WikipediaSingleton.getInstance().wikipedia;
		globalList = Collections.synchronizedList(new ArrayList<String>());
		globalSet = Collections.synchronizedSet(new HashSet<>());
		countArticle = new SynchronizedCounter();
		countLine= new SynchronizedCounter();
		//initializePreCache();
		System.out.println("To inititalize variables it took "+TimeUtil.getEnd(TimeUnit.SECONDS, now)/60+ " minutes");
	}
	private void generateDatasetEntityEntiy_EntityCategory() {
		PageIterator pageIterator = wikipedia.getPageIterator();
		executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
		try {
			int i = 1;
			while (pageIterator.hasNext()) {
			Page page = pageIterator.next();
			if (page.getType().equals(PageType.article)) {
				Article article = wikipedia.getArticleByTitle(page.getTitle());
				if(article==null) {
					continue;
				}
				executor.execute(handle(article, i));
				i++;
			}
		}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			System.out.println("Total time minutes " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
			System.out.println("setGlobal size "+globalSet.size());
			System.out.println("writing to a file");
			FileUtil.writeDataToFile(globalList, "EntityEntity_LINE_dataset.txt", false);	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private Runnable handle(final Article articleToProcess, int index) {
		return () -> {
			//findWeightOfEachCategory(articleToProcess);
			findWeightOfEachCategoryOnlyViaContextEntities(articleToProcess);
			//handleParallel(articleToProcess, index);
			countArticle.incrementbyValue(1);
		};
	}
	/*
	 * for a given article first entities that in the article 
	 * then find the articles that contains those entities 
	 * send those articles to calculateWeightForEntityCategory
	 */
	private void findWeightOfEachCategory(Article article) {
		try {
			Set<Article> setCArticleLinkOutLinkIn = new HashSet<Article>();
			Article[] linkOutMainArticle = article.getLinksOut(); //All the entities inside the main (Anarchism) article such as Agriculture
			for (int j = 0; j < linkOutMainArticle.length; j++) {
				Article[] linksOutLinkInAnArticle = linkOutMainArticle[j].getLinksIn(); //All the entities contains Agriculture in their article
			//	Article[] linksOutLinkInAnArticle = getFromCacheInLinks(linkOutMainArticle[j]).toArray(new Article[getFromCacheInLinks(linkOutMainArticle[j]).size()]);  //All the entities contains Agriculture in their article
				Collections.addAll(setCArticleLinkOutLinkIn, linksOutLinkInAnArticle);
			}
			calculateWeightForEntityCategory(article,setCArticleLinkOutLinkIn);
			//System.out.println("numberOfEntitiesProcessed " +numberOfEntitiesProcessed);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private void findWeightOfEachCategoryOnlyViaContextEntities(Article article) {
		try {
			Article[] linkOutMainArticle = article.getLinksOut(); //All the entities inside the main (Anarchism) article such as Agriculture
			calculateWeightForEntityCategory(article,new HashSet<>(Arrays.asList(linkOutMainArticle)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private void calculateWeightForEntityCategory(final Article article,final Set<Article> setCArticleLinkOutLinkIn) {
		Category[] categoriesCArticle = article.getParentCategories(); //(Category of Anarchism) Get all the categories at the bottom of the article
		Map<Category, Integer> mapCatVal = new HashMap<>();
		for (int i = 0; i < categoriesCArticle.length; i++) {
			Set<Article> childArticlesSet = new HashSet<Article>(Arrays.asList(categoriesCArticle[i].getChildArticles()));
			childArticlesSet.retainAll(setCArticleLinkOutLinkIn);
			mapCatVal.put(categoriesCArticle[i],childArticlesSet.size());
		}
		for(Entry<Category, Integer>entry:mapCatVal.entrySet()){
			thirdLOG.info(article.getId()+"\t"+entry.getKey().getId()+"\t"+entry.getValue());
		}
	}
	private void handleParallel(Article articleToProcess,int index) {
				Set<Article> setInLinks = getFromCacheInLinks(articleToProcess);
		try {
		//	Set<Article> setInLinks = new HashSet<>(preCache.get(articleToProcess.getId())); 
			Article[] outLinks = articleToProcess.getLinksOut();
			for (int j = 0; j < outLinks.length; j++) {
				//Set<Article> setMain = new HashSet<>(preCache.get(outLinks[j].getId()));
				Set<Article> setMain = getFromCacheInLinks(outLinks[j]);
				//Set<Article> setMain = new HashSet<>(Arrays.asList(outLinks[j].getLinksIn()));
				setMain.retainAll(setInLinks);
				if (setMain.size()>0) {
					String key = articleToProcess.getId()+"\t"+outLinks[j].getId();
					globalList.add(key+"\t"+setMain.size());
					globalSet.add(key);
				}
			}
		} catch (Exception e) {
			System.out.println("article to process "+articleToProcess+", preCache: "+preCache+  " preCacheSize: "+preCache.size());
			System.out.println(e.getMessage());
		}
	
	}
	private Set<Article> getFromCacheInLinks(Article articleToProcess) {
		try {
			Set<Article> result = cache.get(articleToProcess.getId());
			if(result==null) {
				Set<Article> hashSet = new HashSet<>(Arrays.asList(articleToProcess.getLinksIn()));
				cache.put(articleToProcess.getId(),hashSet);
				return new HashSet<>(hashSet);
			}else {
				return new HashSet<>(result);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage()+"articleToProcess "+articleToProcess);
			System.exit(1);
		}
		return null;
	}
//	private void initializePreCache() {
//		try {
//			for(Article a : articles) {
//				preCache.put(a.getId(),new HashSet<>(Arrays.asList(a.getLinksIn())));
//			}
//		}
//		catch (Exception e) {
//			// TODO: handle exception
//		}
//		System.out.println("Cache has initialized : "+preCache.size());
//	}
}
