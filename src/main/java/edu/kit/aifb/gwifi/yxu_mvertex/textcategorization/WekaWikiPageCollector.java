package edu.kit.aifb.gwifi.yxu_mvertex.textcategorization;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;

public class WekaWikiPageCollector extends WikiPageCollector {

	public WekaWikiPageCollector(Wikipedia wikipedia,
			String customizedCategoriesFilename) {
		super(wikipedia, customizedCategoriesFilename);
	}

	public Instances storeFullChildArticle2CatesOfCatesInCateFileToARFF(String cateFileName, int articleNum) throws IOException{
		List<String> cateTitles = extractCateTitlesFromCateFile(cateFileName);
		return storeFullChildArticle2CatesOfCatesWithTitlesToARFF(cateTitles, articleNum);
	}

	public Instances storeFullChildArticle2CatesOfCatesWithTitlesToARFF(List<String> cateTitles, int articleNum) throws IOException{
		Map<Integer, Article> id2Article = new HashMap<Integer, Article>();
		Map<Integer, Category> id2Cate = new HashMap<Integer, Category>();
		Map<Integer,List<Integer>> articleId2CateIds = getFullChildArticleId2CateIdsOfCatesWithTitles(cateTitles, id2Article, id2Cate, articleNum);
		return storeArticleId2CateIdsMapInARFF(articleId2CateIds, id2Article, id2Cate);
	}
	
	public Instances storeArticleId2CateIdsMapInARFF(Map<Integer,List<Integer>> articleId2CateIds, Map<Integer, Article> id2Article, Map<Integer, Category> id2Cate) throws IOException{
		return storeArticle2CatesStringMapInARFF(replaceA2CsIdMapToStringMap(articleId2CateIds, id2Article, id2Cate), id2Cate);
	}

	public Instances storeArticle2CatesStringMapInARFF(Map<String,List<String>> article2CatesStringMap, Map<Integer, Category> id2Cate) throws IOException{
		String fileType = "arff";
		String POS = "pos";
		String NEG = "neg";
		String article2CatesFileName = getArticle2CatesFileNameFromCateFileName(customizedCategoriesFilename, fileType);
		
		FastVector attrs = new FastVector();
		FastVector attrVals = new FastVector();
		attrs.addElement(new Attribute("article_text", (FastVector)null));
		for(Category cate:id2Cate.values()){
			attrVals = new FastVector();
			attrVals.addElement(POS);
			attrVals.addElement(NEG);
			attrs.addElement(new Attribute(cate.getTitle().replace(" ", "_"), attrVals));
		}
		Instances data = new Instances("article_categorization", attrs, 0);
		double[] vals;
		List<String> validCateList;
		int cateIndex;
		for(Entry<String,List<String>> a2csEntry:article2CatesStringMap.entrySet()){
			vals = new double[data.numAttributes()];
			vals[0] = data.attribute(0).addStringValue(a2csEntry.getKey());
			validCateList = a2csEntry.getValue();
			cateIndex=1;
			for(Category cate: id2Cate.values()){
				if(validCateList.contains(cate.getTitle())){
					vals[cateIndex] = attrVals.indexOf(POS);
				}else{
					vals[cateIndex] = attrVals.indexOf(POS);
				}
			}
			data.add(new Instance(1.0, vals));
		}
		ArffSaver saver = new ArffSaver();
		saver.setInstances(data);
		saver.setFile(new File(article2CatesFileName));
		saver.writeBatch();
		return data;
	}
	
	public static void main(String[] args) throws Exception {
//		HubConfiguration config = new HubConfiguration(new File("configs/hub-template.xml"));
		String customizedCategoriesFilename = "res/categories.txt";//config.getCategoriesPath();/slow/users/lzh/xu/
		File databaseDirectory = new File("configs/wikipedia-template-en.xml");
		Wikipedia wikipedia = new Wikipedia(databaseDirectory, false);
		WekaWikiPageCollector wekaPageCollector = new WekaWikiPageCollector(wikipedia, customizedCategoriesFilename);
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
				if(cate.trim().length()>0)
					cateList.add(cate.trim());
			}
			if(cateList.size()>0){
				wekaPageCollector.storeFullChildArticle2CatesOfCatesWithTitlesToARFF(cateList, articleNum);
			} else {
				wekaPageCollector.storeFullChildArticle2CatesOfCatesInCateFileToARFF(customizedCategoriesFilename, articleNum);
			}
		}
		scanner.close();
	}

}
