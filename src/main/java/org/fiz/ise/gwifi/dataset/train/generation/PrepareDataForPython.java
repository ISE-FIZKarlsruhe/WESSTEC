package org.fiz.ise.gwifi.dataset.train.generation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.GoogleModelSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.ReadDataset;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.test.afterESWC.GenerateWideFeatureSet;
import org.fiz.ise.gwifi.test.afterESWC.TestBasedonSortTextDatasets;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.StringUtil;
import org.fiz.ise.gwifi.util.SynchronizedCounter;
import org.fiz.ise.gwifi.util.TimeUtil;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Page.PageType;

public class PrepareDataForPython {
	private static final String DATASET_AG_TRAIN = Config.getString("DATASET_TRAIN_AG","");
	private static final String DATASET_AG_TEST = Config.getString("DATASET_AG_TEST","");
	private final static Integer NUMBER_OF_THREADS= Config.getInt("NUMBER_OF_THREADS",-1);
	private static final String DATASET_DBP_TRAIN = Config.getString("DATASET_DBP_TRAIN","");
	private static final String DATASET_DBP_TEST = Config.getString("DATASET_DBP_TEST","");
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");
	private static Map<String, String> mapRedirectPages; //= new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);//new HashMap<>();
	private static SynchronizedCounter totalRedirect;
	private static SynchronizedCounter resolvedRedirect;
	private static SynchronizedCounter couldNotResolved;

	private static ExecutorService executor;
	public static void main(String[] args) throws Exception {
		System.out.println("running PrepareDataForPython");
		long now = TimeUtil.getStart();
		totalRedirect=new SynchronizedCounter();
		resolvedRedirect=new SynchronizedCounter();
		couldNotResolved=new SynchronizedCounter();

		mapRedirectPages= new HashMap<>(AnalysisEmbeddingandRedirectDataset.loadRedirectPages());
		AnnotationSingleton.getInstance();
		Map<String, List<Article>> dataset = ReadDataset.read_dataset_Snippets(Config.getString("DATASET_TEST_SNIPPETS",""));
//		Map<String, List<Article>> dataset = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION, DATASET_AG_TEST);
		
//		Map<String, List<Article>> dataset = ReadDataset.read_dataset_DBPedia_SampleLabel(DATASET_DBP_TEST);

		PrepareDataForPython generate = new PrepareDataForPython();
		generate.extractEntities(dataset, Dataset.WEB_SNIPPETS);
		
		System.out.println("Total time minutes :"+ TimeUnit.SECONDS.toMinutes(TimeUtil.getEnd(TimeUnit.SECONDS, now)));
	}
	
	private void extractEntities(Map<String, List<Article>> dataset, Dataset dName ) throws Exception {
		int count =0;
		executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);		
		
		if (dName.equals(Dataset.WEB_SNIPPETS)) {
			System.out.println("Extracting WEB Snippets dataset");
			List<String> lst_snippets= ReadDataset.read_dataset_Snippets_list(Config.getString("DATASET_TEST_SNIPPETS",""));
			System.out.println("Size of the file to extract entities: "+lst_snippets.size());
			for(String s : lst_snippets) {
				executor.execute(handleExtractEntities(dName ,s, dataset.get(s),count++));
			}
		}
		else {
			for(Entry<String, List<Article>> e : dataset.entrySet()) {
				executor.execute(handleExtractEntities(dName ,e.getKey(), e.getValue(),count++));
			}
			
		}
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
	}

	private Runnable handleExtractEntities(Dataset dName, String str, List<Article> gt,int count)  {
		return () -> {
			List<Annotation> lstAnnotations = new ArrayList<>();
			try {
				AnnotationSingleton.getInstance().service.annotate(str, lstAnnotations);
				StringBuilder strBuild = new StringBuilder(str+"\t\t"+gt.get(0).getTitle()+"\t\t");
				for(Annotation a : lstAnnotations) {
					if (dName.equals(Dataset.DBpedia)) {
						int cleanAnnotation =AnnonatationUtil.getCorrectAnnotation_DBp(a.getId());
						
						if (!AnnonatationUtil.getEntityBlackList_DBp().contains(cleanAnnotation)) {
							Article article= WikipediaSingleton.getInstance().wikipedia.getArticleById(cleanAnnotation);
							if (article==null) {
								Article rArticle = resolveRedirect(a);
								if (rArticle!=null && !StringUtil.isNumeric(rArticle.getTitle()))  {
									strBuild.append(rArticle.getTitle()+"\t");
								}
							}
							else {
								if (!StringUtil.isNumeric(article.getTitle())) {
									strBuild.append(article.getTitle()+"\t");
								}
							}
						}
					}
					else if (dName.equals(Dataset.AG)) {
						if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())){
							Article article= WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId());
							if (article==null) {
								article = resolveRedirect(a);
								if (article!=null) {
									strBuild.append(a.getTitle()+"\t");
								}
							}
							else {
								strBuild.append(a.getTitle()+"\t");
							}
							
						}
					}
					else if (dName.equals(Dataset.WEB_SNIPPETS)) {
						if (!AnnonatationUtil.getEntityBlackList_WebSnippets().contains(a.getId())){
							Article article= WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId());
							if (article==null) {
								article = resolveRedirect(a);
								if (article!=null&& !StringUtil.isNumeric(article.getTitle())) {
									strBuild.append(a.getTitle()+"\t");
								}
							}
							else {
								strBuild.append(a.getTitle()+"\t");
							}
							
						}
					}
					
				}
				resultLog.info(strBuild.toString().subSequence(0, strBuild.toString().length()-1));
				System.out.println(count+" files are processed. totalRedirect: "+totalRedirect.value()+" resolvedRedirect: "+resolvedRedirect.value()
				+" couldNotResolved: "+couldNotResolved.value());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(1);
			}//annotate the given text


		};
	}
	public static Article resolveRedirect(Annotation a) {
		Page p = new Page(WikipediaSingleton.getInstance().wikipedia.getEnvironment(), a.getId());
		if (p.getType().equals(Page.PageType.redirect)) {
			totalRedirect.increment();
			String key = a.getURL().replace("http://en.wikipedia.org/wiki/", "");
			if(mapRedirectPages.containsKey(key)) {
				String tName = mapRedirectPages.get(key).replace("_", " ");
				Article article = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(tName);
				if (article!=null) {
					resolvedRedirect.increment();
					return article;
					//					strBuild.append(article.getTitle()+"\t");
				}
				else {
					couldNotResolved.increment();
					secondLOG.info(key);
					return null;
				}
			}
		}
		return null;
	}
}
