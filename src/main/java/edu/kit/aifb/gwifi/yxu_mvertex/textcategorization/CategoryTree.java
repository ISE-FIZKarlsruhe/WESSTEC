package edu.kit.aifb.gwifi.yxu_mvertex.textcategorization;

import java.util.ArrayList;

public abstract class CategoryTree extends CategorySystem {

	protected int depth;
	
	protected CategoryTree(String systemName) {
		super(systemName);
		this.depth = 0;
	}
	
	public ArrayList<Integer> getParentIDsOf(int systemid){
		return getInIDsOf(systemid);
	}
	
	public ArrayList<Integer> getChildIDsOf(int systemid){
		return getOutIDsOf(systemid);
	}

}
