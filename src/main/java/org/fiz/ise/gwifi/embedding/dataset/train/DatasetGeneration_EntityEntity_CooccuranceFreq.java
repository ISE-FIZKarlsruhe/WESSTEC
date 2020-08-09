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

public class DatasetGeneration_EntityEntity_CooccuranceFreq {
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
/*
 * This class is responsible for generating a entity-entity cooccurance dataset for LINE,
 * iterates over Wikipedia articles 
 * For each article collects the context entities
 * writes them into a log file without any processing (such as "e1 e2") 
 */
	public static void main(String[] args) {
		DatasetGeneration_EntityEntity_CooccuranceFreq data = new DatasetGeneration_EntityEntity_CooccuranceFreq();
		data.initializeVariables();
		System.out.println("Thread started...");
		final Thread t = new Thread (new Runnable() {
			@Override
			public void run() {
				while(true) {
					System.out.println("number of lines "+ countLine.value()+" minutes "+ TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
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
		//data.generateDatasetEntityEntiy();

//		String file = "/home/rima/playground/JavaProjectsRun/gwifi/bin/log_entityCategory/secondLog_onlySort";
//		countSortedCooccuranceFile(file);
	}
	/*
	 * After generating the dataset for cooccurance file
	 * First the file partitined to 5 and each file sorted seperately
	 * sorted files are merged into one file (it was something like sort -m xa* > merge/allMerged) 
	 * then the countSortedCooccuranceFile function called with the sortedMerged file
	 */
	public static void countSortedCooccuranceFile(String file) {
		try {
			int thereshold=1;
			BufferedReader bf = new BufferedReader(new FileReader(file));
			//BufferedReader bf = new BufferedReader(new FileReader("sample"));
			String line = null;
			int i=0;
			int count=1;
			String previousLine=bf.readLine();
			while ((line = bf.readLine()) != null) {
				if (line.equals(previousLine)) {
					count++;
				}
				else {
					if (count>thereshold) {
						thirdLOG.info(previousLine+"\t"+count);
					}
					secondLOG.info(previousLine+"\t"+count);
					count=1;
					previousLine=line;
				}
				countLine.increment();
			}
			thirdLOG.info(previousLine+"\t"+count);
			bf.close();
			System.out.println("Line number "+countLine.value());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
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
	private void generateDatasetEntityEntiy() {
		List<Article> mainList = new ArrayList<>();
		PageIterator pageIterator = wikipedia.getPageIterator();
		while (pageIterator.hasNext()) {
			Page page = pageIterator.next();
			if (page.getType().equals(PageType.article)) {
				Article article = wikipedia.getArticleByTitle(page.getTitle());
				if(article==null) {
					continue;
				}
				mainList.add(article);
			}
		}
		Collections.sort(mainList);

		executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
		System.err.println("started.."+ countArticle.value()+" main List Size "+mainList.size());
		long now = TimeUtil.getStart();
		try {
			int size=mainList.size();
			int ofset=1000000;
			for(int i=0;i<(size/ofset)+1;i++) {
				int begin=(i*ofset)+i;
				int end=(i*ofset+i+ofset);
				if (end>size) {
					end=size;
				}
				System.out.println(begin+" "+end+" "+mainList.subList(begin, end).size());
				executor.execute(handle(null,new ArrayList<>(mainList.subList(begin, end))));
			}
			//			for (int j = 0; j < mainList.size()/10; j++) {
			//				
			//				//				List<Article> childList = new ArrayList<>(mainList.subList(j+1, mainList.size()));
			//				//				System.out.println(mainList.get(j+1)+" Size of the sub List "+childList.size()+" "+childList.get(j));
			////				executor.execute(handle(mainList.get(j),new ArrayList<>(mainList.subList(j+1, mainList.size()))));
			//				
			//			}
			//			executor.execute(handle(null,new ArrayList<>(mainList.subList(0, mainList.size()))));
			//System.out.println("Total time spend "+ TimeUtil.getEnd(TimeUnit.SECONDS, now)+" countArticle: "+countArticle.value() );
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

	private Runnable handle(Article articleToProcess, ArrayList<Article> arrayList) {
		return () -> {
			handleParallelWriteToFile(arrayList);
			//			handleParallel(articleToProcess, arrayList);

		};
	}
	private void handleParallelWriteToFile(ArrayList<Article> articleList) {
		try {
			System.out.println(articleList.size()+" number of aricles need to be processed");
			Map<String, MutableInt> freq = new HashMap<String, MutableInt>();
			final List<String> pairs = new ArrayList<>();
			for(final Article article: articleList) {
				final Article[] linksOut = article.getLinksOut();//context entities
				for(int i=0;i<linksOut.length;i++) {
					for(int j=i+1;j<linksOut.length;j++) {					
						String key = linksOut[i].getId()+"\t"+linksOut[j].getId();
						//globalList.add(key);
						secondLOG.info(key);
					}
				}
				countArticle.increment();
			}

			//			pairs.stream().collect(Collectors.groupingBy(p -> p, ()-> localMap,Collectors.counting()));
			////			System.err.println(i2 +"== "+TimeUtil.getEnd(TimeUnit.SECONDS, start));
			//			FileUtil.writeDataToFile(localMap, OUTPUT_FOLDER + File.separator + (((i2 - 1) * NUMBER_OF_PAGES) + "_" + i2* NUMBER_OF_PAGES) + ".txt", false);		
			//			
			//			
			//			Set<Article> mainArticleAsContext = new HashSet<>(getFromCacheInLinks(articleToProcess));//contains this article as a context 
			//			for(Article article : lstArticles) {
			//				Set<Article> setMain = getFromCacheInLinks(article);
			//				setMain.retainAll(mainArticleAsContext);
			//				if (setMain.size()>0) {
			//					String key = articleToProcess.getId()+"\t"+article.getId();
			//					globalList.add(key+"\t"+setMain.size());
			//					globalSet.add(key);
			//				}
			//			}
		} catch (Exception e) {
			//System.out.println("article to process "+articleToProcess+", preCache: "+preCache+  " preCacheSize: "+preCache.size());
			System.out.println(e.getMessage());
		}
	}
	private void handleParallel(Article articleToProcess,ArrayList<Article> lstArticles) {
		try {
			Set<Article> mainArticleAsContext = new HashSet<>(getFromCacheInLinks(articleToProcess));//contains this article as a context 
			for(Article article : lstArticles) {
				Set<Article> setMain = getFromCacheInLinks(article);
				setMain.retainAll(mainArticleAsContext);
				if (setMain.size()>0) {
					String key = articleToProcess.getId()+"\t"+article.getId();
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
