package edu.kit.aifb.gwifi.yxu.textcategorization;

import java.util.HashMap;
import java.util.HashSet;

public class CategorySystemConverter {

	protected CategorySystem scrCateSys;
	protected CategorySystem tarCateSys;
	
	protected HashMap<Integer, HashSet<Integer>> scrID2tarIDsMap;
	protected HashMap<Integer, HashSet<Integer>> tarID2scrIDsMap;
	
	protected CategorySystemConverter(){
		this.scrID2tarIDsMap = new HashMap<Integer, HashSet<Integer>>();
		this.tarID2scrIDsMap = new HashMap<Integer, HashSet<Integer>>();
	}
	
	public CategorySystemConverter(CategorySystem scrCateSys, CategorySystem tarCateSys){
		this.scrCateSys = scrCateSys;
		this.tarCateSys = tarCateSys;
		this.scrID2tarIDsMap = new HashMap<Integer, HashSet<Integer>>();
		this.tarID2scrIDsMap = new HashMap<Integer, HashSet<Integer>>();
	}
	
	public CategorySystem getScrCateSys(){
		return scrCateSys;
	}
	
	public CategorySystem getTarCateSys(){
		return tarCateSys;
	}
	
	public void initScr(){
		scrCateSys.initCategories();
	}
	
	public void convert(){
		//convert scr title to tar titles
	}
	
	public void buildTar(){
		//build tar sys according to the result of convert
	}
	
	protected void mapScrToTarCate(Integer scrCate, Integer tarCate){
		//map cate in scr sys to a cate in tar sys
	}

	protected void addIDMapTo(Integer id1, Integer id2, HashMap<Integer, HashSet<Integer>> map){
		HashSet<Integer> id2s = map.get(id1);
		if(id2s == null){
			id2s = new HashSet<Integer>();
			map.put(id1, id2s);
		}
		id2s.add(id2);
	}

}
