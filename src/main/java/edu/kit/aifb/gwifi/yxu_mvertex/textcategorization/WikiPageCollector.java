package edu.kit.aifb.gwifi.yxu_mvertex.textcategorization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.HubConfiguration;

public class WikiPageCollector {
	private final static int MAX_ARTICLE_NUM = 1000000;
	private final static int MAX_COLLECT_DEPTH = 5;
	private final static int MAX_CATE_NUM_OF_ARTICLE = 5;
	private final static float SAMPLE_RATE = 0.01f;
	private Wikipedia wikipedia;
	protected String customizedCategoriesFilename;
	
	public WikiPageCollector(Wikipedia wikipedia, String customizedCategoriesFilename){
		this.wikipedia = wikipedia;
		this.customizedCategoriesFilename = customizedCategoriesFilename;
	}
	
	public List<Category> getFullParentCatesOfCatesWithTitles(List<String> cateTitleList) {
		List<Category> topCategories = new ArrayList<Category>();
		for (String cateTitle : cateTitleList) {
			topCategories.add(wikipedia.getCategoryByTitle(cateTitle));
		}
		getFullParentCatesOfCates(topCategories);
		return topCategories;
	}

	public int getFullParentCatesOfCates(List<Category> cateList) {
		Category[] tempParentList;
		List<Category> newList = cateList;
		List<Category> tempNewList;
		do {
			tempNewList = new ArrayList<Category>();
			for (Category cate1 : newList) {
				tempParentList = cate1.getParentCategories();
				if (tempParentList.length == 0)
					continue;
				for (Category cate2 : tempParentList) {
					if (cate2 == null || !isCateValid(cate2))
						continue;
					if (!isCateInCates(cate2, cateList)) {
						if (!isCateInCates(cate2, tempNewList)) {
							tempNewList.add(cate2);
						}
					}
				}
			}
			if (!cateList.addAll(tempNewList))
				break;
			newList = tempNewList;
		} while (true);
		return 0;
	}

	private boolean isCateInCates(Category cate, Collection<Category> cateList) {
		for (Category cate1 : cateList) {
			if (cate1.equals(cate))
				return true;
		}
		return false;
	}

	private boolean isCateValid(Category cate) {
		int lowDepth = 5;
		int highDepth = 1;
		int depth = cate.getDepth() == null ? -1 : cate.getDepth();
		if (depth <= lowDepth && depth >= highDepth) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean existCateTitle(String cateTitle){
		Category cate = wikipedia.getCategoryByTitle(cateTitle);
		if(cate == null){
			return false;
		}else{
			return true;
		}
	}
	
	public Article findArticleOfCate(String cateTitle){
		String articleTitle = cateTitle;
		Article article = wikipedia.getArticleByTitle(articleTitle);
		return article;
	}
	
	public Map<Article,List<Category>> getFullChildArticle2CatesOfCatesInFileWithName(String cateFileName, int articleNum) throws IOException{
		List<String> cateTitles = extractCateTitlesFromCateFile(cateFileName);
		return getFullChildArticle2CatesOfCatesWithTitles(cateTitles, articleNum);
	}
	
	public Map<Integer,List<Integer>> getFullChildArticleId2CateIdsOfCatesInFileWithName(String cateFileName, int articleNum){
		List<String> cateTitles = extractCateTitlesFromCateFile(cateFileName);
		Map<Integer, Article> id2Article = new HashMap<Integer, Article>();
		Map<Integer, Category> id2Cate = new HashMap<Integer, Category>();
		return getFullChildArticleId2CateIdsOfCatesWithTitles(cateTitles, id2Article, id2Cate, articleNum);
	}

	public Map<Article,List<Category>> getFullChildArticle2CatesOfCatesWithTitles(List<String> cateTitles, int articleNum) throws IOException{
		Map<Integer, Article> id2Article = new HashMap<Integer, Article>();
		Map<Integer, Category> id2Cate = new HashMap<Integer, Category>();
		Map<Integer, List<Integer>> articleId2CateIds = getFullChildArticleId2CateIdsOfCatesWithTitles(cateTitles, id2Article, id2Cate, articleNum);
		return null;//replaceA2CsIdMapToPageMap(articleId2CateIds, id2Article, id2Cate);
	}
	
	public Map<Integer, List<Integer>> getFullChildArticleId2CateIdsOfCatesWithTitles(
			List<String> cateTitles, 
			Map<Integer, Article> id2Article, 
			Map<Integer, Category> id2Cate,
			int articleNum){
		Map<Integer, List<Integer>> articleId2CateIds = new HashMap<Integer, List<Integer>>();
		if(cateTitles==null)
			return articleId2CateIds; //no root categories
		if(id2Article == null || id2Cate == null)
			return articleId2CateIds; //no container of child articles and categories
		for(String cateTitle:cateTitles){
			Category cate = wikipedia.getCategoryByTitle(cateTitle);
			if(cate==null)
				continue; // invalid root category
			Integer cateId = cate.getId();
			if(id2Cate.containsKey(cateId))
				continue; // category already searched
			id2Cate.put(cateId, cate);
			Map<Integer, Article> id2ChildArticle =  new HashMap<Integer, Article>();
			Map<Integer, Category> id2ChildCate =  new HashMap<Integer, Category>();
			collectFullChildPagesOfCate(cate, id2ChildArticle, id2ChildCate, articleNum);
			if(id2ChildArticle.isEmpty())
				continue; // no child article detected under the category
//			id2ChildArticle = sampleArticles(id2ChildArticle);
			for(Article article:id2ChildArticle.values()){
				Integer articleId = article.getId();
				List<Integer> cateIds = articleId2CateIds.get(articleId);
				if(cateIds==null){
					id2Article.put(articleId, article);
					cateIds = new ArrayList<Integer>();
					articleId2CateIds.put(articleId, cateIds);
				}
				cateIds.add(cateId);
			}
			System.out.println("total article num: " + articleId2CateIds.size());
		}
		//TODO drop articles with lots categories, and then sample 1% articles at last
		for(Integer articleId: articleId2CateIds.keySet()){
			if(articleId2CateIds.get(articleId).size()>MAX_CATE_NUM_OF_ARTICLE){
				articleId2CateIds.remove(articleId);
			}
		}
		articleId2CateIds = sampleArticle2Cates(articleId2CateIds);
		return articleId2CateIds;
	}
	
	private Map<Integer, List<Integer>> sampleArticle2Cates(Map<Integer, List<Integer>> articleId2CateIds){
		Map<Integer, List<Integer>> sampleArticle2Cates =  new HashMap<Integer, List<Integer>>();
		int articleNum = articleId2CateIds.size();
		int sampleNum = Math.round(articleNum*SAMPLE_RATE);
		//TODO
		System.out.println("sample: " + articleNum + "\t: " + sampleNum);
		List<Integer> ids = new ArrayList<Integer>(articleId2CateIds.keySet());
		int curIndex = ids.get(0);
		int curId = 0;
		while(sampleNum>0){
			curIndex = (int)((curIndex + getRandom(articleNum)) % articleNum);
			curId = ids.get(curIndex);
			ids.remove(curIndex);
			sampleNum--;
			articleNum--;
			if(sampleArticle2Cates.put(curId, articleId2CateIds.get(curId))!=null){
				System.out.println("random article duplicated!");
			}
		}
		return sampleArticle2Cates;
	}
	
	private Map<Integer, Article> sampleArticles(Map<Integer, Article> id2ChildArticle){
		Map<Integer, Article> sampleArticles =  new HashMap<Integer, Article>();
		int articleNum = id2ChildArticle.size();
		int sampleNum = Math.round(articleNum*SAMPLE_RATE);
		//TODO
		System.out.println("sample: " + articleNum + "\t: " + sampleNum);
		List<Integer> ids = new ArrayList<Integer>(id2ChildArticle.keySet());
		int curIndex = ids.get(0);
		int curId = 0;
		while(sampleNum>0){
			curIndex = (int)((curIndex + getRandom(articleNum)) % articleNum);
			curId = ids.get(curIndex);
			ids.remove(curIndex);
			sampleNum--;
			articleNum--;
			if(sampleArticles.put(curId, id2ChildArticle.get(curId))!=null){
				System.out.println("random article duplicated!");
			}
		}
		return sampleArticles;
	}
	
	private long getRandom(long max){
		return Math.round(Math.random()*max);
	}
	
	public Map<Integer, Set<Integer>> getCateId2FullChildArticleIdsOfCatesWithTitles(
			List<String> cateTitles, 
			Map<Integer, Article> id2Article, 
			Map<Integer, Category> id2Cate,
			int articleNum){
		Map<Integer, Set<Integer>> cateId2ArticleIds = new HashMap<Integer, Set<Integer>>();
		if(cateTitles==null)
			return cateId2ArticleIds; //no root categories
		if(id2Article == null || id2Cate == null)
			return cateId2ArticleIds; //no container of child articles and categories
		for(String cateTitle:cateTitles){
			Category cate = wikipedia.getCategoryByTitle(cateTitle);
			if(cate==null)
				continue; // invalid root category
			Integer cateId = cate.getId();
			if(id2Cate.containsKey(cateId))
				continue; // category already searched
			id2Cate.put(cateId, cate);
			Map<Integer, Article> id2ChildArticle =  new HashMap<Integer, Article>();
			Map<Integer, Category> id2ChildCate =  new HashMap<Integer, Category>();
			collectFullChildPagesOfCate(cate, id2ChildArticle, id2ChildCate, articleNum);
			if(id2ChildArticle.isEmpty())
				continue; // no child article detected under the category
			id2ChildArticle = sampleArticles(id2ChildArticle);
			Set<Integer> articleIds = new HashSet<Integer>();
			cateId2ArticleIds.put(cateId, articleIds);
			for(Article article:id2ChildArticle.values()){
				Integer articleId = article.getId();
				articleIds.add(articleId);
				id2Article.put(articleId, article);
			}
		}
		return cateId2ArticleIds;
	}
	
	//TODO build the category tree structure with leaf articles
	// 1. keep indirect child articles under each category
	// 2. offer method to collect child articles under any category
	public void collectFullChildPagesOfCatesWithTitles(List<String> cateTitles, 
			Map<Integer, Article> id2ChildArticle, 
			Map<Integer, Category> id2ChildCate,
			int articleNum){
		if(cateTitles==null)
			return; //no root categories
		if(id2ChildArticle==null || id2ChildCate == null)
			return; //no valid container for child articles and categories
//		Map<Category, Set<Article>> cate2Articles = new HashMap<Category, Set<Article>>();
		for(String cateTitle:cateTitles){
			Category cate = wikipedia.getCategoryByTitle(cateTitle);
			if(cate==null)
				continue; //invalid root category
//			Set<Article> articles = new HashSet<Article>();
//			cate2Articles.put(cate, articles);
			collectFullChildPagesOfCate(cate, id2ChildArticle, id2ChildCate, articleNum);
		}
	}
	
	protected Map<Article,List<Category>> replaceA2CsIdMapToPageMap(
			Map<Integer, List<Integer>> articleId2CateIds, 
			Map<Integer, Article> id2Article, Map<Integer, Category> id2Cate){
		Map<Article,List<Category>> article2Cates = new HashMap<Article, List<Category>>();
		List<Integer> tempCateIds;
		List<Category> tempCates;
		for(Integer articleId: articleId2CateIds.keySet()){
			tempCateIds = articleId2CateIds.get(articleId);
			tempCates = new ArrayList<Category>();
			article2Cates.put(id2Article.get(articleId), tempCates);
			for(Integer cateId: tempCateIds){
				tempCates.add(id2Cate.get(cateId));
			}
		}
		return article2Cates;
	}
	
	protected Map<String,List<String>> replaceA2CsIdMapToStringMap(
			Map<Integer, List<Integer>> articleId2CateIds, 
			Map<Integer, Article> id2Article, Map<Integer, Category> id2Cate){
		Map<String,List<String>> articleText2CateTitles = new HashMap<String, List<String>>();
		List<Integer> tempCateIds;
		List<String> tempCateTitles;
		for(Integer articleId: articleId2CateIds.keySet()){
			tempCateIds = articleId2CateIds.get(articleId);
			tempCateTitles = new ArrayList<String>();
			articleText2CateTitles.put(id2Article.get(articleId).getFirstParagraphMarkup(), tempCateTitles);//.getPlainText()
			for(Integer cateId: tempCateIds){
				tempCateTitles.add(id2Cate.get(cateId).getTitle());
			}
		}
		return articleText2CateTitles;
	}
	
	protected Map<String,Set<String>> replaceC2AsIdMapToStringMap(
			Map<Integer, Set<Integer>> cateId2ArticleIds, 
			Map<Integer, Article> id2Article, Map<Integer, Category> id2Cate){
		Map<String,Set<String>> cateTitle2ArticleTexts = new HashMap<String, Set<String>>();
		Set<Integer> tempArticleIds;
		Set<String> tempArticleTexts;
		for(Integer cateId: cateId2ArticleIds.keySet()){
			tempArticleIds = cateId2ArticleIds.get(cateId);
			tempArticleTexts = new HashSet<String>();
			cateTitle2ArticleTexts.put(id2Cate.get(cateId).getTitle(), tempArticleTexts);
			for(Integer articleId: tempArticleIds){
				tempArticleTexts.add(id2Article.get(articleId).getFirstParagraphMarkup());//getPlainText()
			}
		}
		return cateTitle2ArticleTexts;
	}
	
	public Map<Integer,String> getFullChildArticlesOfCateWithTitle(String rootCateTitle, int articleNum){
		if(rootCateTitle==null)
			return null; //no root category
		Map<Integer, String> articleId2Text = new HashMap<Integer, String>();
		Map<Integer, Article> id2ChildArticle =  new HashMap<Integer, Article>();
		collectFullChildPagesOfCateWithTitle(rootCateTitle, id2ChildArticle, null, articleNum);
		for(Article article:id2ChildArticle.values()){
			articleId2Text.put(article.getId(), article.getPlainText());
		}
		return articleId2Text;
	}

	public void collectFullChildPagesOfCateWithTitle(String rootCateTitle, 
			Map<Integer, Article> id2ChildArticle, 
			Map<Integer, Category> id2ChildCate,
			int articleNum){
		if(rootCateTitle==null)
			return; //no root category
		Category rootCate = wikipedia.getCategoryByTitle(rootCateTitle);
		collectFullChildPagesOfCate(rootCate, id2ChildArticle, id2ChildCate, articleNum);
	}
	
	//TODO 
	// now save all the pages under the root cate
	// count the number of articles under the root cate
	// later save each page under his parents cate
	// and save all the cates of each page
	public void collectFullChildPagesOfCate(Category rootCate, 
//			Map<Article, Set<Category>> article2Cates, 
//			Map<Category, Set<Article>> cate2Articles,
			Map<Integer, Article> id2ChildArticle, 
			Map<Integer, Category> id2ChildCate,
			int articleNum){
		if(rootCate==null)
			return; //no valid root category
		if(id2ChildArticle==null || id2ChildCate == null)
			return; //no valid container for child articles and categories
		int num = articleNum<0? MAX_ARTICLE_NUM:articleNum;
		if(rootCate.getDepth() == null){
			Article[] childArticles = rootCate.getChildArticles();
			addArticleArrayToMap(childArticles, id2ChildArticle);
			System.out.println("cate num: " + id2ChildCate.size());
			System.out.println("article num: " + id2ChildArticle.size());
			return;
		}
		int maxDepth = rootCate.getDepth() + MAX_COLLECT_DEPTH;
		Map<Integer, Category> id2uncheckedCate = new HashMap<Integer, Category>();
		Map<Integer, Category> id2checkedCate = new HashMap<Integer, Category>();
		id2checkedCate.putAll(id2ChildCate);
		Iterator<Category> uncheckedCateIterator;
		Category currentCate;
		Article[] tempChildArticles;
		Category[] tempChildCates = new Category[1];
		tempChildCates[0]=rootCate;
		addCateArrayToUncheckedCollection(tempChildCates, id2uncheckedCate, id2checkedCate);
		while(id2uncheckedCate.size()>0){
			uncheckedCateIterator = id2uncheckedCate.values().iterator();
			if(!uncheckedCateIterator.hasNext())
				continue;
			currentCate = uncheckedCateIterator.next();
			uncheckedCateIterator.remove();
			id2checkedCate.put(currentCate.getId(), currentCate);
			tempChildArticles = currentCate.getChildArticles();
			addArticleArrayToMap(tempChildArticles, id2ChildArticle);
			//TODO check for stop condition
			if(currentCate.getDepth()>maxDepth) continue;
//			if(id2ChildArticle.size()>num) break;
			tempChildCates = currentCate.getChildCategories();
			addCateArrayToUncheckedCollection(tempChildCates, id2uncheckedCate, id2checkedCate);
		};
		id2ChildCate.clear();
		id2ChildCate.putAll(id2checkedCate);
		System.out.println("cate num: " + id2ChildCate.size());
		System.out.println("article num: " + id2ChildArticle.size());
	}
	
	private boolean addArticleArrayToMap(Article[] array, Map<Integer, Article> id2ChildArticle){
		if(id2ChildArticle==null || array == null || array.length == 0){
			return false;
		}
		boolean isChanged = false;
		for(Article article:array){
			if(!id2ChildArticle.containsKey(article.getId())){
				id2ChildArticle.put(article.getId(), article);
				isChanged = true;
			}
		}
		return isChanged;
	}

	private boolean addCateArrayToUncheckedCollection(Category[] array, 
			Map<Integer, Category> id2uncheckedCate, Map<Integer, Category> id2checkedCate){
		if(id2uncheckedCate==null || id2checkedCate==null || array == null || array.length == 0){
			return false;
		}
		boolean isChanged = false;
		for(Category cate:array){
			if(id2checkedCate.containsKey(cate.getId()))
				continue;
			if(!id2uncheckedCate.containsKey(cate.getId())){
				id2uncheckedCate.put(cate.getId(), cate);
				isChanged = true;
			}
		}
		return isChanged;
	}
	
	protected String getArticle2CatesFileNameFromCateFileName(String cateFileName, String fileType){
		int pathEndAt = cateFileName.lastIndexOf("/")+1;
		String path = cateFileName.substring(0, pathEndAt);
		String cateFileShortName = cateFileName.substring(pathEndAt);
		String[] sp = cateFileShortName.split("\\.");
		cateFileShortName = sp[0] + "_article_oriented." + fileType;
		return path + cateFileShortName;
	}
	
	protected String getCate2ArticlesFileNameFromCateFileName(String cateFileName, String fileType){
		int pathEndAt = cateFileName.lastIndexOf("/")+1;
		String path = cateFileName.substring(0, pathEndAt);
		String cateFileShortName = cateFileName.substring(pathEndAt);
		String[] sp = cateFileShortName.split("\\.");
		cateFileShortName = sp[0] + "_cate_oriented." + fileType;
		return path + sp[0] + "/" + cateFileShortName;
	}

	public List<String> extractCateTitlesFromCateFile(String filename) {
		ArrayList<String> cateTitleList = new ArrayList<String>();
		if(filename == null || filename.trim().length()==0)
			return cateTitleList;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(filename), Charset.forName("UTF-8")));
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if(line.length()==0 || line.startsWith("#")){
					continue;
				}
				cateTitleList.add(line);
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		return cateTitleList;
	}

	public static void main(String[] args) throws Exception {
//		HubConfiguration config = new HubConfiguration(new File("configs/hub-template.xml"));
		String customizedCategoriesFilename = "res/categories.txt";//config.getCategoriesPath();/slow/users/lzh/xu/
		File databaseDirectory = new File("configs/wikipedia-template-en.xml");
		Wikipedia wikipedia = new Wikipedia(databaseDirectory, false);
		WikiPageCollector pageCollector = new WikiPageCollector(wikipedia, customizedCategoriesFilename);
		int articleNum = 10000;
		
		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.println("Please input the categories:");
			String cates = scanner.nextLine();
			if (cates.startsWith("exit")) {
				break;
			}
			String[] cateArray = cates.split(",");
			List<String> cateList = new ArrayList<String>();
			for(String cate:cateArray){
				cateList.add(cate.trim());
			}
			if(cates.length()>0 && cateList.size()>0){
				pageCollector.getFullChildArticle2CatesOfCatesWithTitles(cateList, articleNum);
			} else {
				pageCollector.getFullChildArticle2CatesOfCatesInFileWithName(customizedCategoriesFilename, articleNum);
			}
//			Article article= pageCollector.findArticleOfCate(cates);
//			System.out.println(article);
		}
		scanner.close();
		

//		int articleNum = 310128;
//		int sampleNum = Math.round(articleNum*SAMPLE_RATE);
//		int curIndex = 1;
//		for(int i=0; i<10; i++){
//			curIndex = (int)((curIndex + Math.round(Math.random()*articleNum)) % sampleNum);
//			System.out.println(curIndex);
//			sampleNum--;
//		}
//		System.out.println();
//		curIndex = 1;
//		for(int i=0; i<10; i++){
//			curIndex = (int)((curIndex + Math.round(Math.random()*articleNum)) % sampleNum);
//			System.out.println(curIndex);
//			sampleNum--;
//		}
	}

}
