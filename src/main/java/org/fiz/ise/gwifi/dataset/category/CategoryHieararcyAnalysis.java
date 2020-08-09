package org.fiz.ise.gwifi.dataset.category;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;

public class CategoryHieararcyAnalysis {
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	public static void main(String[] args) {
		//		iterateCategoryHierarcy();
		enrichEntCatcooccuranceDataset();
	}
	private static void enrichEntCatcooccuranceDataset() {
		String fileName = "/home/rima/playground/LINE/linux/Data/entity-category-complex/dataset_LINE_EntCat_cooccFreq";
		int count = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			while ((br.readLine()) != null) {
				count++;	
			}
			System.out.println(count);

		} catch (IOException e) {
			e.printStackTrace();
		}
		int total= count;
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			count = 0;
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				System.out.println(total+" "+ ++count);
				String[] split = sCurrentLine.split("\t");
				String entitiy = split[0];
				Category c =WikipediaSingleton.getInstance().wikipedia.getCategoryById(Integer.valueOf(split[1]));
				ArrayList<Category> sC = new ArrayList<>(Arrays.asList(c.getParentCategories()));

				secondLOG.info(sCurrentLine);
				//				System.out.println(line);
				for(Category sparentCat : sC) {
					secondLOG.info(entitiy+"\t"+sparentCat.getId()+"\t"+split[2]);
					//						ArrayList<Category> ssC = new ArrayList<>(Arrays.asList(sparentCat.getParentCategories()));
					//						for(Category ssparentCat : ssC) {
					//							//System.out.println(entitiy+"\t"+ssparentCat.getId()+"\t"+split[2]);
					//							secondLOG.info(entitiy+"\t"+ssparentCat.getId()+"\t"+split[2]);
					//						}
				}
			}


		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private static void iterateCategoryHierarcy() {
		//		Category pC =WikipediaSingleton.getInstance().wikipedia.getCategoryById(3488351);
		//		System.out.println(WikipediaSingleton.getInstance().wikipedia.getArticleById(3488351).getSentenceMarkup(1));
		//		System.out.println(WikipediaSingleton.getInstance().wikipedia.getPageById(3488351).getSentenceMarkup(1));
		//		Category ppC =WikipediaSingleton.getInstance().wikipedia.getCategoryById(691008);
		//		System.out.println(ppC.getPlainText());

		Category mC =WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Main topic classifications");
		System.out.println(Arrays.asList(mC.getChildCategories()));
		for (int i = 0; i < mC.getChildCategories().length; i++) { //iterate over root categories
			ArrayList<Category> cC = new ArrayList<>(Arrays.asList(mC.getChildCategories()[i].getChildCategories()));
			for (int j = 0; j < cC.size(); j++) {
				secondLOG.info(mC.getChildCategories()[i].getId()+"\t"+cC.get(j).getId()+"\t"+"1");
				//System.out.println(mC.getChildCategories()[i]+"\t"+cC.get(j)+"\t"+"1");
				iterateOverChildCats(cC.get(j)); //child of child
			}
		}

		//		System.out.println(Arrays.asList(mC.getChildCategories()));


	}
	private static void iterateOverChildCats(Category mainCat) {
		ArrayList<Category> ccC = new ArrayList<>(Arrays.asList(mainCat.getChildCategories()));
		for (int j = 0; j < ccC.size(); j++) {
			secondLOG.info(mainCat.getId()+"\t"+ccC.get(j).getId()+"\t"+"1");
			//			System.out.println(mainCat+"\t"+ccC.get(j)+"\t"+"1");
		}
	}
}
