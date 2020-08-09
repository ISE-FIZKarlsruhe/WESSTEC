package org.fiz.ise.gwifi.dataset.category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.PTEModelSingleton;
import org.fiz.ise.gwifi.util.VectorUtil;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;

public class CategoryFeaturesForNN {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	public static String getCategoryNames(List<Article> lstArticle) {
		//		List<String> resultAll = new ArrayList<String>();
		StringBuilder strB = new StringBuilder();
		for(Article a : lstArticle) {
			Category[] parentCategories = a.getParentCategories();
			for (int i = 0; i < parentCategories.length; i++) {
				if (!parentCategories[i].getTitle().matches(".*\\d.*")) {
					//resultAll.add(parentCategories[i].getTitle());
					strB.append(parentCategories[i].getTitle()+" ");
				}
			}
		}
		return strB.toString().trim();
	}
	public static String getCategoryVec(List<Article> lstArticle) {
		List<String> catIds = new ArrayList<String>();
		for(Article a : lstArticle) {
			Category[] parentCategories = a.getParentCategories();
			for (int i = 0; i < parentCategories.length; i++) {
				catIds.add(String.valueOf(parentCategories[i].getId()));
			}
		}
		double[] vector = VectorUtil.getSentenceVector(catIds,PTEModelSingleton.getInstance().pte_model);
		if (vector==null || vector.length==0) {
			return null;
		}
		else {
			String strVector="";
			for (int j = 0; j < vector.length; j++) {
				strVector=strVector+(String.valueOf(vector[j]) + ",");
			}
			strVector = strVector.substring(0, strVector.length() - 1);
			return strVector;
		}
	}
	public static String getCategoryIDs(List<Article> lstArticle) {
		//		List<String> resultAll = new ArrayList<String>();
		StringBuilder strB = new StringBuilder();
		for(Article a : lstArticle) {
			Category[] parentCategories = a.getParentCategories();
			for (int i = 0; i < parentCategories.length; i++) {
				//if (!parentCategories[i].getTitle().matches(".*\\d.*")) {
				//resultAll.add(parentCategories[i].getTitle());
				strB.append(parentCategories[i].getId()+" ");
				//}
				//else {
				//System.out.println("Ignored cat: "+parentCategories[i]);
				//}
			}
		}
		return strB.toString().trim();
	}

}
