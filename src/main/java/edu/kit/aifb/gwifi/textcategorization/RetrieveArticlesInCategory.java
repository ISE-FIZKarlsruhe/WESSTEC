package edu.kit.aifb.gwifi.textcategorization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;

import edu.kit.aifb.gwifi.util.text.PorterStemmer;
import edu.stanford.nlp.process.Morphology;
/**
 * retrieve the articles according to the categories in following format:
 * folder name: category name
 * file name(in the folder): the articles which belong to this category
 * @author aifb-ls3
 *
 */
public class RetrieveArticlesInCategory implements Runnable{
	//public static String baseURL =  "zh.wikipedia.org";
	public static String baseURL =  "en.wikipedia.org";
	//public static NLPPreprocessor prepro = new StanfordNLPPreprocessor("/Users/aifb-ls3/JavaWorkspace/gwifi/configs/NLPConfig.properties", Language.ZH);
	//public static NLPPreprocessor prepro = new StanfordNLPPreprocessor("/Users/aifb-ls3/JavaWorkspace/gwifi/configs/NLPConfig.properties", Language.EN);
	private static PorterStemmer stemmer = new PorterStemmer();
	private String category;
	private List<String> articles;
	private String outputDir;
	 
	
	

	public RetrieveArticlesInCategory(String category, List<String> articles, String outputDir){
		this.category = category;
		this.articles = articles;
		this.outputDir = outputDir;
	}
	
	@Override
	public void run() {
		
		Wiki wiki = new Wiki(baseURL);
		for(String article : articles){
			
			String text;
			PrintWriter pw = null;
			File dir = new File(outputDir+category+"/");
			if(!dir.exists()){
				dir.mkdirs();
			}
			try {
				System.out.println(dir.getAbsolutePath()+File.separator+article+".txt");
				text = wiki.getPageText(article);
				text = wiki.parse(text);
				text = Jsoup.parse(text).text();
				text = RetrieveArticlesInCategory.wordPreprocess(text);
				//text = RetrieveArticlesInCategory.splitWords(text, prepro);
				pw = new PrintWriter(new FileWriter(dir.getAbsolutePath()+File.separator+article+".txt"));
				pw.println(text);
			} catch (Exception e) {
				continue;
			}finally{
				if (pw!=null)
					pw.close();
			}
			
		}
	}
	// english text preprocessing//
	public static synchronized  String wordPreprocess(String content){
		String test = stemmer.processText(content); // words stemming
	    test = test.replaceAll("\\P{InBasic_Latin}|\\d", "");// delete non-english character
	    test = Stopword.removeStopwords(test); 
	    return test;
	}
	
	//chinese text preprocessing//
//	public static synchronized  String splitWords(String content, NLPPreprocessor prepro){
//		String s = prepro.segmentation(content);// for Chinese words segment
//		s = Stopword.removeStopwords(s);
//		return s;
//	}

	public static Map<String, List<String>> loadCatArticleMap(String[] categories, int[] catIDs, String[] mappingFiles,int minDep) throws IOException{
		Map<String, List<String>> rstMap = new HashMap<String, List<String>>();
		Set<String> catSet= new HashSet<String>();
		for(String s : categories){
			catSet.add(s);
		}
		Map<Integer, String> idCatMap = new HashMap<Integer, String>();
		for(int i = 0; i < catIDs.length; i++){
			idCatMap.put(catIDs[i], categories[i]);
		}
		
		for(String file : mappingFiles){
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = null;
			while((line = br.readLine()) != null){
				String[] data = line.split(",");
				int id = Integer.parseInt(data[0]);
				int depth = Integer.parseInt(data[(data.length)-1]);
				if(idCatMap.containsKey(id) && depth<minDep){ // choose the article with depth smaller than 4
					String article = data[1];
					String cat = idCatMap.get(id);
					List<String> list = rstMap.get(cat);
					if(list == null){
						list = new ArrayList<String>();
						rstMap.put(cat, list);
					}
					list.add(article);
				}
			}
			br.close();
		}
		return rstMap;
	}
	
	//public static int NUM_ARTICLE = 1000;
	public static int Thread_per_Cat = 4;
	/**
	 * caution: please rewrite the file dir of stopwords list in class Sopword.java 
	 * and the category dic file in class ReadCategoryFile.java
	 * @param args0: mapping csv file directory
	 * 		  args1: training text directory
	 * 	      args2: number of articles in every category to choose
	 *        args3: min depth
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException{
		ReadCategoryFile r = new ReadCategoryFile();
		int[] catIDs = r.getCateid2int();
		String[] categories = r.getCatename();
		//String[] mappingFilses ={"/home/ls3data/users/lzh/congliu/minDep_mapping_en.csv"};
		String[] mappingFilses ={args[0]};
		//String outputDir = "/home/ls3data/users/lzh/congliu/TrainingText_en/";
		String outputDir = args[1];
		int num_art = Integer.parseInt(args[2]);
		int dep = Integer.parseInt(args[3]);
		Map<String, List<String>> catArtMap = RetrieveArticlesInCategory.loadCatArticleMap(categories, catIDs, mappingFilses,dep);
		ExecutorService exec = Executors.newFixedThreadPool(200);
		for(String cat : catArtMap.keySet()){
			List<String> allArticles = catArtMap.get(cat);
			List<String> articles = new ArrayList<String>();
			int length = allArticles.size();
//			if(length < 200){
//				continue;
//			}
			Random ran = new Random();
			Set<Integer> selectedSet = new HashSet<Integer>();
			//for(int i = 0; i < RetrieveArticlesInCategory.NUM_ARTICLE; i++){
			  for(int i=0;i< num_art;i++ ){
				if(selectedSet.size() == length){
					break;
				}
				int next = ran.nextInt(length);
				while(selectedSet.contains(next)){
					next = ran.nextInt(length);
					}
				selectedSet.add(next);
				articles.add(allArticles.get(next));
				//System.out.print("rrrrrrrrr");
			}
			int size = articles.size() / RetrieveArticlesInCategory.Thread_per_Cat;
			
			for(int i = 0; i < RetrieveArticlesInCategory.Thread_per_Cat; i++){
				RetrieveArticlesInCategory runable = new RetrieveArticlesInCategory(cat, articles.subList(size*i, size*i+size), outputDir);
				exec.execute(runable);
			}
			
				
		}
		exec.shutdown();
	}
}