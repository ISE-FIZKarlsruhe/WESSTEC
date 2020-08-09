package edu.kit.aifb.gwifi.yxu.textcategorization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public abstract class CategorySystem {
	
	protected final String systemName;
	
	protected Map<Integer, HashSet<String>> id2categoryTitleGroupMap;
	protected Map<Integer, ArrayList<Integer>> id2inIDsMap;
	protected Map<Integer, ArrayList<Integer>> id2outIDsMap;
	
	private int cateNum;
	
	protected CategorySystem(String systemName){
		this.systemName = systemName;
		this.id2categoryTitleGroupMap = new HashMap<Integer, HashSet<String>>();
		this.cateNum = 0;
		this.id2inIDsMap = new HashMap<Integer, ArrayList<Integer>>();
		this.id2outIDsMap = new HashMap<Integer, ArrayList<Integer>>();
	}
	
	public int getCateNum(){
		return cateNum;
	}
	
	public abstract void initCategories();
	
	public int addCate(String cateTitle){
		HashSet<String> cateTitleGroup = new HashSet<String>();
		cateTitleGroup.add(cateTitle);
		return addCate(cateTitleGroup);
	}
	
	public int addCate(HashSet<String> cateTitleGroup){
		if(!isCateValid(cateTitleGroup)) return -1;
		HashSet<String> filteredCateTitleGroup = filterCateTitleGroup(cateTitleGroup);
		int cateID = findCate(filteredCateTitleGroup);
		if(cateID >= 0) return cateID;
		id2categoryTitleGroupMap.put(cateNum++, filteredCateTitleGroup);
		return cateNum - 1;
	}
	
	private HashSet<String> filterCateTitleGroup(HashSet<String> cateTitleGroup){
		HashSet<String> filtered = new HashSet<String>();
		boolean isIn = false;
		for(String originTitle: cateTitleGroup){
			isIn = false;
			for(String filteredTitle: filtered){
				if(originTitle.equals(filteredTitle)){
					isIn = true;
					break;
				}
			}
			if(!isIn) filtered.add(originTitle);
		}
		return filtered;
	}
	
	private boolean isCateValid(HashSet<String> cateTitleGroup){
		//is null ?
		if(cateTitleGroup == null) return false;
		//is empty ?
		if(cateTitleGroup.size()==0) return false;
		return true;
	}
	
	private int findCate(HashSet<String> cateTitleGroup){
		HashSet<String> exCateTitleGroup;
		for(Integer id: id2categoryTitleGroupMap.keySet()){
			exCateTitleGroup = id2categoryTitleGroupMap.get(id);
			if(compareCates(cateTitleGroup, exCateTitleGroup)==0) return id;
		}
		return -1;
	}
	
	private int compareCates(HashSet<String> cateTitleGroup1, HashSet<String> cateTitleGroup2){
		//1 contains 2	,1
		//1 equals 2	,0
		//1 sits in 2	,-1
		//1 cross 2		,2
		//1 out of 2	,-2
		//unexcepted	,-3
		int cate1Num = cateTitleGroup1.size();
		int cate2Num = cateTitleGroup2.size();
		int commCateNum = 0;
		for(String cate1: cateTitleGroup1){
			for(String cate2: cateTitleGroup2){
				if(cate1.equals(cate2)){
					commCateNum++;
					break;
				}
			}
		}
		if(cate1Num < cate2Num){
			if(commCateNum == cate1Num) return -1;
			else if(commCateNum < cate1Num) return 2;
			else if(commCateNum == 0) return -2;
			else return -3;
		} else if(cate1Num > cate2Num){
			if(commCateNum == cate2Num) return 1;
			else if(commCateNum < cate2Num) return 2;
			else if(commCateNum == 0) return -2;
			else return -3;
		} else {
			if(commCateNum == cate1Num) return 0;
			else if(commCateNum < cate1Num) return 2;
			else if(commCateNum == 0) return -2;
			else return -3;
		}
	}

	public String getSystemName() {
		return systemName;
	}
	
	public HashSet<String> getCateTitleGroupByID(int systemid){
		return id2categoryTitleGroupMap.get(systemid);
	}
	
	public Integer getIDByCateTitleGroup(HashSet<String> cateTitleGroup){
		for(Integer id: id2categoryTitleGroupMap.keySet()){
			if(compareCates(cateTitleGroup, id2categoryTitleGroupMap.get(id))==0){
				return id;
			}
		}
		return -1;
	}

	public String getCateTitleByID(Integer systemid){
		return id2categoryTitleGroupMap.get(systemid).iterator().next();
	}
	
	public Integer getIDByCateTitle(String cateTitle){
		HashSet<String> cateTitleGroup = new HashSet<String>();
		cateTitleGroup.add(cateTitle);
		return getIDByCateTitleGroup(cateTitleGroup);
	}
	
	protected ArrayList<Integer> getInIDsOf(int systemid){
		return id2inIDsMap.get(systemid);
	}
	
	protected void addInIDOf(int systemid, int inID){
		ArrayList<Integer> inIDs = id2inIDsMap.get(systemid);
		if(inIDs == null){
			inIDs = new ArrayList<Integer>();
			id2inIDsMap.put(systemid, inIDs);
		}
		inIDs.add(inID);
	}
	
	protected ArrayList<Integer> getOutIDsOf(int systemid){
		return id2outIDsMap.get(systemid);
	}

	protected void addOutIDOf(int systemid, int outID){
		ArrayList<Integer> outIDs = id2outIDsMap.get(systemid);
		if(outIDs == null){
			outIDs = new ArrayList<Integer>();
			id2outIDsMap.put(systemid, outIDs);
		}
		outIDs.add(outID);
	}
	
	protected void addRel(int scrID, int tarID){
		addInIDOf(tarID, scrID);
		addOutIDOf(scrID, tarID);
	}
	
	public String printCateSys(){
		//TODO
		return "";
	}
	
 	protected class CategoryNode {
		private HashSet<CategoryNode> parents;
		private HashSet<CategoryNode> children;
		protected CategoryNode(){
			
		}
		protected HashSet<CategoryNode> getParents() {
			return parents;
		}
		protected void setParents(HashSet<CategoryNode> parents) {
			this.parents = parents;
		}
		protected HashSet<CategoryNode> getChildren() {
			return children;
		}
		protected void setChildren(HashSet<CategoryNode> children) {
			this.children = children;
		}
	}

}
