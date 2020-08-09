package org.fiz.ise.gwifi.embedding.dataset.train;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.w3c.dom.ls.LSException;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.util.PageIterator;

public class DatasetGeneration_Joint_EntityEntity {
	private static final Logger LOG = Logger.getLogger(DatasetGeneration_Joint_EntityEntity.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final long now = System.currentTimeMillis();
	private ExecutorService executor;
	private static SynchronizedCounter countArticle;
	private static SynchronizedCounter countLine;
	private final Integer NUMBER_OF_THREADS=  Config.getInt("NUMBER_OF_THREADS",-1);
	private static final Map<Integer,Set<Article>> cache = new ConcurrentHashMap<>();
	private static final Map<Integer,Set<Article>> preCache = new ConcurrentHashMap<>();
	private static Wikipedia wikipedia;
	private static List<String> globalList;
	private static Set<String> globalSet;
	public static void main(String[] args) {
		DatasetGeneration_Joint_EntityEntity data = new DatasetGeneration_Joint_EntityEntity();
		data.initializeVariables();
		System.out.println("Thread started...");
		final Thread t = new Thread (new Runnable() {
			@Override
			public void run() {
				while(true) {
					System.out.println("preCache size "+preCache.size()+", countLine "+countLine.value()+"number of article processed "+ countArticle.value()+" minutes "+ TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now)+ " globalSetSize "+globalSet.size()+" globalList "+globalList.size());
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}			
				}
			}
		});
		t.setDaemon(true);
		//t.start();
		data.initializePreCache();
		data.generateDatasetEntityEntiy();
		//data.generateDatasetEntityEntiy_fromMap();
	}
	private void initializeVariables() {
		globalList = Collections.synchronizedList(new ArrayList<String>());
		globalSet = Collections.synchronizedSet(new HashSet<>());
		wikipedia = WikipediaSingleton.getInstance().wikipedia;
		countArticle = new SynchronizedCounter();
		countLine= new SynchronizedCounter();
	}
	private void generateDatasetEntityEntiy_fromMap() {
		int count =0;
		List<String> tep = new ArrayList<>();
		Set<String> set1 = new HashSet<>();
		String sCurrentLine;
		String file1 = "/home/rima/playground/JavaProjectsRun/gwifi_logging/bin/log/secondLog";
		//String file1 = "sample_Log";
		try (BufferedReader br = new BufferedReader(new FileReader(file1))) {
			while ((sCurrentLine = br.readLine()) != null) {
				int i = sCurrentLine.indexOf("\t\t");
				String firstPart = sCurrentLine.substring(0, i);
				//				String secondPart = sCurrentLine.substring(i+2);
				if (set1.contains(firstPart)) {
					//System.out.println("Set contains the second part "+firstPart);
					//LOG.info("Set contains the second part "+firstPart);
					if (!tep.contains(sCurrentLine)) {
						System.out.println(sCurrentLine);
						System.exit(1);
					}
				}
				tep.add(sCurrentLine);
				set1.add(firstPart);
				count++;
			}
			System.out.println("size of the map "+set1.size());
			System.out.println("size of the count "+count);
			System.out.println("Started processing...");

			executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
			int i = 1;
			for(String s: set1) {
				int index = s.indexOf("\t");
				String firstPart = s.substring(0, index).trim();
				String secondPart = s.substring(index+1).trim();
				Article a1 = wikipedia.getArticleById(Integer.parseInt(firstPart));
				Article a2 = wikipedia.getArticleById(Integer.parseInt(secondPart));
				if (a1!=null&&a2!=null) {
					//					System.out.println(s);
					//					System.exit(1);

					i++;
				}
				countLine.increment();
				//executor.execute(handle_map(a1,a2, i));

			}
			System.out.println("count i "+i);
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			System.out.println("Total time minutes " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
			System.out.println("setGlobal size "+globalSet.size());
			System.out.println("writing to a file");
			FileUtil.writeDataToFile(globalList, "EntityEntity_LINE_dataset.txt", false);	
		} 
		catch (Exception e) {
			e.printStackTrace();
		} 
	}
	private void generateDatasetEntityEntiy() {
		//PageIterator pageIterator = wikipedia.getPageIterator();
		executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
		try {
			int i = 1;
			Set<Article> articles = FilteredWikipediaPagesSingleton.getInstance().articles;
			for(Article a : articles) {
				executor.execute(handle(a, i));
				i++;
			}
//			while (pageIterator.hasNext()) {
//				Page page = pageIterator.next();
//				if (page.getType().equals(PageType.article)) {
//					Article article = wikipedia.getArticleByTitle(page.getTitle());
//					if(article==null) {
//						continue;
//					}
//					executor.execute(handle(article, i));
//					i++;
//				}
//			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			System.out.println("Total time minutes " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
			System.out.println("setGlobal size "+globalSet);
			System.out.println("writing to a file");
			FileUtil.writeDataToFile(globalList, "EntityEntity_LINE_dataset.txt", false);	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Runnable handle_map(Article article1, Article article2, int index2) {
		return () -> {
			//System.out.println(article1+" "+ article2);
			handleParallel_map(article1, article2);
			countArticle.incrementbyValue(1);
		};
	}
	private Runnable handle(final Article articleToProcess, int index) {
		return () -> {
			handleParallel(articleToProcess, index);
			countArticle.incrementbyValue(1);
		};
	}
	private void handleParallel_map(Article article1,Article article2) {
		Set<Article> setTemp = getFromCacheInLinks(article1);
		//Set<Article> setTemp = new HashSet<>(Arrays.asList(article1.getLinksIn()));
		Set<Article> setMain = getFromCacheInLinks(article2);
		//	Set<Article> setMain = new HashSet<>(Arrays.asList(article2.getLinksIn()));
		setMain.retainAll(setTemp);
		if (setMain.size()>0) {
			String key = article1.getId()+"\t"+article2.getId();
			//			if (globalSet.contains(key)) {
			//				System.out.println("Set contains the key "+key);
			//				System.exit(1);
			//			}
			globalSet.add(key);
			globalList.add(key+"\t"+setMain.size());
		}
	}
	private void handleParallel(Article articleToProcess,int index) {
		//		Set<Article> setTemp = getFromCacheInLinks(articleToProcess);
		//Set<Article> setTemp = new HashSet<>(Arrays.asList(articleToProcess.getLinksIn()));
		Set<Article> setInLinks = new HashSet<>(preCache.get(articleToProcess.getId())); 
		Article[] outLinks = articleToProcess.getLinksOut();
		Arrays.sort(outLinks);
		for (int j = 0; j < outLinks.length; j++) {
			if (outLinks[j].getType().equals(PageType.article)) {
				Set<Article> setMain = new HashSet<>(preCache.get(outLinks[j].getId()));
				//Set<Article> setMain = getFromCacheInLinks(outLinks[j]);
				//Set<Article> setMain = new HashSet<>(Arrays.asList(outLinks[j].getLinksIn()));
				setMain.retainAll(setInLinks);
				if (setMain.size()>0) {
					String key = articleToProcess.getId()+"\t"+outLinks[j].getId();
					//secondLOG.info(key+"\t\t"+setTemp.size());
					//					if (globalSet.contains(key)) {
					//						System.out.println("Set contains the key "+key);
					//						//System.exit(1);
					//					}
					//globalSet.add(key+"\t"+setMain.size());
					globalList.add(key+"\t"+setMain.size());
				}
			}
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
	private void initializePreCache() {
		try {
			Set<Article> articles = FilteredWikipediaPagesSingleton.getInstance().articles;
			for(Article a : articles) {
				preCache.put(a.getId(),new HashSet<>(Arrays.asList(a.getLinksIn())));
			}
		}
		catch (Exception e) {
			// TODO: handle exception
		}
		System.out.println("Cache has initialized : "+preCache.size());
	}
}
