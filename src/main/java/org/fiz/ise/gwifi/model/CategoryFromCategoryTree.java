package org.fiz.ise.gwifi.model;

public enum CategoryFromCategoryTree {
	HEAD_OF_STATE_TAG("president"),
	POPE_TAG("pope"), 
	MONARCH_TAG("king"), 
	CHAIR_PERSON_TAG("ceo"),
	/**
	 * I add this because I need it in the class MyStandfordCoreNLPRegex
	 * when I run "run" function 
	 */
	ROLE("ROLE");

	private String text;

	CategoryFromCategoryTree(String text) {
		this.text = text;
	}

	public String text() {
		return text;
	}
	
	public static CategoryFromCategoryTree resolve(String text){
		for(CategoryFromCategoryTree cat: CategoryFromCategoryTree.values()){
			if(cat.text().equals(text.toLowerCase())){
				return cat;
			}
		}
		System.err.println("*******************************************************");
		System.exit(1);
		return null;
	}

	public static CategoryFromCategoryTree resolveWithCategoryName(String text) {
		for(CategoryFromCategoryTree cat: CategoryFromCategoryTree.values()){
			if(cat.name().equals(text)){
				return cat;
			}
		}
		return null;
	}
}