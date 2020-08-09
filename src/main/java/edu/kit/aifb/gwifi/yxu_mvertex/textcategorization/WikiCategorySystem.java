package edu.kit.aifb.gwifi.yxu_mvertex.textcategorization;

import java.util.ArrayList;
import java.util.HashMap;

import edu.kit.aifb.gwifi.model.Wikipedia;

public class WikiCategorySystem extends CategorySystem {

	private Wikipedia wikipedia;
	private HashMap<Integer, String> id2wikiCate;
	private HashMap<Integer, String> id2wikiArticle;
	
	public WikiCategorySystem(String systemName, Wikipedia wikipedia) {
		super(systemName);
		this.wikipedia = wikipedia;
		initCategories();
	}

	@Override
	public void initCategories() {
		this.id2wikiCate = new HashMap<Integer, String>();
		this.id2wikiArticle = new HashMap<Integer, String>();
	}
	
	public int addWikiCateWithTitle(String cateTitle){
		if(existCateTitle(cateTitle)==0) return -1;
		return addCate(cateTitle);
	}
	
	private int existCateTitle(String cateTitle){
		if(wikipedia.getCategoryByTitle(cateTitle)!=null) return 1;
		if(wikipedia.getArticleByTitle(cateTitle)!=null) return 2;
		return 0;
	}
	
	public void buildWikiCateArticleSet(){
		int cateOrArticle;
		String cateTitle;
		for(Integer id: id2categoryTitleGroupMap.keySet()){
			cateTitle = getCateTitleByID(id);
			cateOrArticle = existCateTitle(cateTitle);
			if(cateOrArticle==1){
				id2wikiCate.put(id, cateTitle);
			}else if(cateOrArticle==2){
				id2wikiArticle.put(id, cateTitle);
			}
		}
	}
	
	public ArrayList<Integer> getCateIDsOf(int systemid){
		return getInIDsOf(systemid);
	}
	
	public ArrayList<Integer> getSubcateIDsOf(int systemid){
		return getOutIDsOf(systemid);
	}
	
	public HashMap<Integer, String> getWikiCates(){
		return id2wikiCate;
	}
	
	public HashMap<Integer, String> getWikiArticles(){
		return id2wikiArticle;
	}


}
