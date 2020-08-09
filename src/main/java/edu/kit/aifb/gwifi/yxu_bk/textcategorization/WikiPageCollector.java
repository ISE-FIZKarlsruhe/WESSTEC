package edu.kit.aifb.gwifi.yxu_bk.textcategorization;

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
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.HubConfiguration;

public class WikiPageCollector {
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
	
	public Map<Article,List<Category>> getFullChildArticle2CatesOfCatesInFileWithName(String cateFileName) throws IOException{
		List<String> cateTitles = extractCateTitlesFromCateFile(cateFileName);
		return getFullChildArticle2CatesOfCatesWithTitles(cateTitles);
	}
	
	public Map<Integer,List<Integer>> getFullChildArticleId2CateIdsOfCatesInFileWithName(String cateFileName){
		List<String> cateTitles = extractCateTitlesFromCateFile(cateFileName);
		Map<Integer, Article> id2Article = new HashMap<Integer, Article>();
		Map<Integer, Category> id2Cate = new HashMap<Integer, Category>();
		return getFullChildArticleId2CateIdsOfCatesWithTitles(cateTitles, id2Article, id2Cate);
	}

	public Map<Article,List<Category>> getFullChildArticle2CatesOfCatesWithTitles(List<String> cateTitles) throws IOException{
		Map<Integer, Article> id2Article = new HashMap<Integer, Article>();
		Map<Integer, Category> id2Cate = new HashMap<Integer, Category>();
		Map<Integer, List<Integer>> articleId2CateIds = getFullChildArticleId2CateIdsOfCatesWithTitles(cateTitles, id2Article, id2Cate);
		return replaceIdMapToPageMap(articleId2CateIds, id2Article, id2Cate);
	}
	
	public Map<Integer, List<Integer>> getFullChildArticleId2CateIdsOfCatesWithTitles(List<String> cateTitles, 
			Map<Integer, Article> id2Article, Map<Integer, Category> id2Cate){
		Map<Integer, List<Integer>> articleId2CateIds = new HashMap<Integer, List<Integer>>();
		for(String cateTitle:cateTitles){
			Category cate = wikipedia.getCategoryByTitle(cateTitle);
			Integer cateId = cate.getId();
			if(id2Cate.containsKey(cateId))
				continue; // category already searched
			List<Article> childArticles = new ArrayList<Article>();
			getFullChildPagesOfCateWithTitle(cateTitle, childArticles, null);
			if(childArticles.size()==0)
				continue; // no child article detected under the category
			id2Cate.put(cateId, cate);
			for(Article article:childArticles){
				Integer articleId = article.getId();
				List<Integer> cateIds = articleId2CateIds.get(articleId);
				if(cateIds==null){
					id2Article.put(articleId, article);
					cateIds = new ArrayList<Integer>();
					articleId2CateIds.put(articleId, cateIds);
				}
				cateIds.add(cateId);
			}
		}
		return articleId2CateIds;
	}
	
	protected Map<Article,List<Category>> replaceIdMapToPageMap(
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
	
	protected Map<String,List<String>> replaceIdMapToStringMap(
			Map<Integer, List<Integer>> articleId2CateIds, 
			Map<Integer, Article> id2Article, Map<Integer, Category> id2Cate){
		Map<String,List<String>> articleText2CateTitles = new HashMap<String, List<String>>();
		List<Integer> tempCateIds;
		List<String> tempCateTitles;
		for(Integer articleId: articleId2CateIds.keySet()){
			tempCateIds = articleId2CateIds.get(articleId);
			tempCateTitles = new ArrayList<String>();
			articleText2CateTitles.put(id2Article.get(articleId).getPlainText(), tempCateTitles);
			for(Integer cateId: tempCateIds){
				tempCateTitles.add(id2Cate.get(cateId).getTitle());
			}
		}
		return articleText2CateTitles;
	}
	
	public Map<Integer,String> getFullChildArticlesOfCateWithTitle(String rootCateTitle){
		if(rootCateTitle==null)
			return null; //no root category
		Map<Integer, String> articleId2Text = new HashMap<Integer, String>();
		List<Article> childArticles =  new ArrayList<Article>();
		getFullChildPagesOfCateWithTitle(rootCateTitle, childArticles, null);
		for(Article article:childArticles){
			articleId2Text.put(article.getId(), article.getPlainText());
		}
		return articleId2Text;
	}

	public void getFullChildPagesOfCateWithTitle(String rootCateTitle, List<Article> childArticles, List<Category> childCates){
		if(rootCateTitle==null)
			return; //no root category
		if(childArticles==null)
			return; //no valid container for child articles
		List<Category> uncheckedCates = new ArrayList<Category>();
		List<Category> checkedCates = new ArrayList<Category>();
		Category currentCate;
		Article[] tempChildArticles;
		Category[] tempChildCates;
		Category rootCate = wikipedia.getCategoryByTitle(rootCateTitle);
		if(rootCate==null)
			return; //no valid root category
		uncheckedCates.add(rootCate);
		do{
			currentCate = uncheckedCates.remove(0);
			tempChildArticles = currentCate.getChildArticles();
			tempChildCates = currentCate.getChildCategories();
			addArticleArrayToCollection(tempChildArticles, childArticles);
			checkedCates.add(currentCate);
			addCateArrayToUncheckedCollection(tempChildCates, uncheckedCates, checkedCates);
		}while(uncheckedCates.size()>0);
		if(childCates!=null)
			childCates.addAll(checkedCates);
	}
	
	private boolean addArticleArrayToCollection(Article[] array, Collection<Article> collection){
		if(collection==null || array == null || array.length == 0){
			return false;
		}
		boolean isChanged = false;
		for(Article article:array){
			if(collection.add(article)){
				isChanged = true;
			}
		}
		return isChanged;
	}

	private boolean addCateArrayToUncheckedCollection(Category[] array, 
			Collection<Category> uncheckedCollection, Collection<Category> checkedCollection){
		if(uncheckedCollection==null || checkedCollection==null || array == null || array.length == 0){
			return false;
		}
		boolean isChanged = false;
		for(Category cate:array){
			if(isCateInCates(cate, checkedCollection))
				continue;
			if(uncheckedCollection.add(cate)){
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
		String article2CatesFileName = path + cateFileShortName;
		return article2CatesFileName;
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
		String customizedCategoriesFilename = "/slow/users/lzh/xu/res/categories.txt";//config.getCategoriesPath();
		File databaseDirectory = new File("configs/wikipedia-template-en.xml");
		Wikipedia wikipedia = new Wikipedia(databaseDirectory, false);
		WikiPageCollector pageCollector = new WikiPageCollector(wikipedia, customizedCategoriesFilename);
		
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
			if(cateList.size()>0){
				pageCollector.getFullChildArticle2CatesOfCatesWithTitles(cateList);
			} else {
				pageCollector.getFullChildArticle2CatesOfCatesInFileWithName(customizedCategoriesFilename);
			}
		}
		scanner.close();
	}

}
