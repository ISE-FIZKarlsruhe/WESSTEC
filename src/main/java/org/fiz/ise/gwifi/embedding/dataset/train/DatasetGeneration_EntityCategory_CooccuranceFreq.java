package org.fiz.ise.gwifi.embedding.dataset.train;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.apache.lucene.search.Sort;
import org.fiz.ise.gwifi.Singleton.FilteredWikipediaPagesSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.MutableInt;
import org.fiz.ise.gwifi.util.SynchronizedCounter;
import org.fiz.ise.gwifi.util.TimeUtil;

import com.google.common.collect.Lists;
import com.twelvemonkeys.util.Time;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.util.PageIterator;
import edu.stanford.nlp.time.JollyDayHolidays.MyXMLManager;

public class DatasetGeneration_EntityCategory_CooccuranceFreq {
	private static final Logger LOG = Logger.getLogger(DatasetGeneration_EntityCategory_CooccuranceFreq.class);
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
		DatasetGeneration_EntityCategory_CooccuranceFreq data = new DatasetGeneration_EntityCategory_CooccuranceFreq();
		data.initializeVariables();
		System.out.println("Thread started...");
		final Thread t = new Thread (new Runnable() {
			@Override
			public void run() {
				while(true) {
					//					System.out.println("number of article processed "+ countArticle.value()+" minutes "+ TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now)+ " globalSetSize "+globalSet.size()+" globalList "+globalList.size());
					//System.out.println("number of lines "+ countLine.value()+" minutes "+ TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
					System.out.println("number of article processed "+ countArticle.value()+" minutes "+ TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
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
		data.generateDatasetEntityCategory();
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
	private void generateDatasetEntityCategory() {
		try {
			long now = TimeUtil.getStart();
			executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
			PageIterator pageIterator = wikipedia.getPageIterator();
			while (pageIterator.hasNext()) {
				Page page = pageIterator.next();
				if (page.getType().equals(PageType.article)) {
					Article article = wikipedia.getArticleByTitle(page.getTitle());
					if(article==null) {
						continue;
					}
					executor.execute(handle(article,article.getParentCategories()));
				}
			}
			System.out.println("Total time spend "+ TimeUtil.getEnd(TimeUnit.SECONDS, now)+" countArticle: "+countArticle.value() );
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			System.out.println("Total time minutes " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
			System.out.println("setGlobal size "+globalSet.size());
			System.out.println("writing to a file");
			FileUtil.writeDataToFile(globalList, "EntityEntity_LINE_CooccuranceFreq_dataset.txt", false);	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private Runnable handle(Article articleToProcess, Category[] categories) {
		return () -> {
			handleWriteToFile(articleToProcess,categories);
		};
	}
	private void handleWriteToFile(Article articleToProcess, Category[] categories) {
		try {
			Article[] linkOutMainArticle = articleToProcess.getLinksOut(); //All the entities inside the main (Anarchism) article such as Agriculture
			for (int j = 0; j < linkOutMainArticle.length; j++) {
				for (int i = 0; i < categories.length; i++) {
					secondLOG.info(linkOutMainArticle[j].getId()+"\t"+categories[i].getId());
				}
			}
			countArticle.increment();
		} catch (Exception e) {
			System.out.println("article to process "+articleToProcess+", preCache: "+preCache+  " preCacheSize: "+preCache.size());
			System.out.println(e.getMessage());
		}
	}
	private void handleProcessWriteToFile(Article articleToProcess, Category[] categories) {
		try {
			Article[] linkOutMainArticle = articleToProcess.getLinksOut(); //All the entities inside the main (Anarchism) article such as Agriculture
			for (int j = 0; j < linkOutMainArticle.length; j++) {
				Set<Article> contextArticleAsContext = new HashSet<>(getFromCacheInLinks(linkOutMainArticle[j]));
				for (int i = 0; i < categories.length; i++) {
					Set<Article> intersection = new HashSet<>(Arrays.asList(categories[i].getChildArticles()));
					intersection.retainAll(contextArticleAsContext);
					if (intersection.size()>0) {
						secondLOG.info(linkOutMainArticle[j].getId()+"\t"+categories[i].getId()+"\t"+intersection.size());
					}
				}
			}
			countArticle.increment();
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
}
