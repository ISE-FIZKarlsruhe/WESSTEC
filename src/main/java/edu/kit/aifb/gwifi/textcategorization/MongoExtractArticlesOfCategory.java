package edu.kit.aifb.gwifi.textcategorization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.mongo.Property;
import edu.kit.aifb.gwifi.mongo.index.MongoArticleIndexer;
import edu.kit.aifb.gwifi.util.nlp.Language;

/**
 * choose the wiki categories in a layer
 * 
 * @author aifb-ls3
 *
 */
public class MongoExtractArticlesOfCategory {

	private Wikipedia wikipedia;
	private DBCollection dbCollection;
	
	private Set<String> allChildCategories = new HashSet<String>();

	String lastMaxDepthCat = "";
	Set<String> childrenSet = new HashSet<String>();
	Set<String> dealedSet = new HashSet<String>();
	
	int MAX_DEPTH;
	int MIN_DEPTH;
	
	public MongoExtractArticlesOfCategory(String wikiDir, String langLabel) throws Exception {
		Language lang = Language.getLanguage(langLabel);
		dbCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.CATEGORY_ENTITY_COLLECTION + lang.getLabel());
		wikipedia = new Wikipedia(new File(wikiDir), false);
	}

	public Category getCategoryByTitle(String catName) {
		return wikipedia.getCategoryByTitle(catName);
	}

	public int getMAX_DEPTH() {
		return MAX_DEPTH;
	}

	public int getMIN_DEPTH() {
		return MIN_DEPTH;
	}

	public void setMAX_DEPTH(int mAX_DEPTH) {
		MAX_DEPTH = mAX_DEPTH;
	}

	public void setMIN_DEPTH(int mIN_DEPTH) {
		MIN_DEPTH = mIN_DEPTH;
	}
	
	public static class DepthArticle {
		private int depth = 0;
		private String article;

		public DepthArticle(String article, int depth) {
			this.article = article;
			this.depth = depth;
		}

		public int getDepth() {
			return depth;
		}

		public void setDepth(int depth) {
			this.depth = depth;
		}

		public String getArticle() {
			return article;
		}

		public void setArticle(String article) {
			this.article = article;
		}
	}

	public void insertData(Category category) throws IOException {
		List<DepthArticle> articles = treeTraversal(category, "", 0);
		for(DepthArticle article : articles) {
			BasicDBObject dbObject = new BasicDBObject(DBConstants.CATEGORY_ENTITY_ENTITY_NAME, article.getArticle())
					.append(DBConstants.CATEGORY_ENTITY_CATEGORY_NAME, category.getTitle())
					.append(DBConstants.CATEGORY_ENTITY_DISTANCE, article.getDepth());
			dbCollection.insert(dbObject);
		}
		
		dbCollection.createIndex(new BasicDBObject(DBConstants.ARTICLES_ID, 1));
		dbCollection.createIndex(new BasicDBObject(DBConstants.ARTICLES_TITLE, 1));
	}
	
	public void close() {
		MongoResource.INSTANCE.finalizing();
		wikipedia.close();
	}
	
	public List<DepthArticle> treeTraversal(Category cat, String path, int depth) {
		String curPath = path + "/" + cat.getTitle();
		// if(cat.getTitle().trim().equals("總類")){
		// return null;
		// }
		if (cat.getTitle().equals("Fundamental categories")) {
			return null;
		}
		if (isColored(cat) || childrenSet.contains(cat.getTitle())) {
			return null;
		}
		if (cat.getDepth() != null && (depth - cat.getDepth()) > 2) {
			return null;
		}

		if (depth == this.getMAX_DEPTH() && !cat.getTitle().equals(lastMaxDepthCat)) {
			childrenSet.clear();
			childrenSet = new HashSet<String>();
			lastMaxDepthCat = cat.getTitle();
		}
		childrenSet.add(cat.getTitle());

		List<DepthArticle> curArtTitles = new ArrayList<DepthArticle>();
		// titles of current category
		Article[] arts = cat.getChildArticles();
		for (Article t : arts) {
			curArtTitles.add(new DepthArticle(t.getTitle(), depth));
		}

		this.giveColor(cat);

		// collect articles from all children categories
		Category[] children = cat.getChildCategories();
		for (Category childCat : children) {
			List<DepthArticle> childrenArts = treeTraversal(childCat, curPath, depth + 1);

			if (childrenArts != null) {
				curArtTitles.addAll(childrenArts);
				childrenArts.clear();
				childrenArts = null;
			}
		}

		this.removeColor(cat);

		if (depth >= this.getMIN_DEPTH() && depth <= this.getMAX_DEPTH() && !dealedSet.contains(cat.getTitle())) {
			dealedSet.add(cat.getTitle());
			System.out.println(curPath + " " + curArtTitles.size());
		}
		if (depth == this.getMIN_DEPTH()) {
			curArtTitles.clear();
			curArtTitles = null;
		}
		return curArtTitles;
	}

	void giveColor(Category category) {
		allChildCategories.add(category.getTitle());
	}

	void removeColor(Category category) {
		allChildCategories.remove(category.getTitle());
	}

	boolean isColored(Category category) {
		if (allChildCategories.contains(category.getTitle())) {
			return true;
		} else {
			return false;
		}
	}
	
	public static Set<String> loadAllCategories(String inputFile) throws IOException {
		Set<String> categories = new HashSet<String>();
		File input = new File(inputFile);
		BufferedReader br = new BufferedReader(new FileReader(input));
		String line = null;
		while (((line = br.readLine()) != null)) {
			categories.add(line.trim());
		}
		br.close();
		
		return categories;
	}

	// "configs/configuration_esa.properties" "configs/wikipedia-template-en.xml" "en" "categories.txt"
//	public static void main(String[] args) {
//		try {
//			String mongoConfigPath = args[0];
//			Property.setProperties(mongoConfigPath);
//
//			MongoExtractArticlesOfCategory m = new MongoExtractArticlesOfCategory(args[1], args[2]);
//			Set<String> categoryLabels = loadAllCategories(args[3]);			
//			
//			for(String categoryLabel : categoryLabels) {
//				Category category = m.getCategoryByTitle(categoryLabel);
//				m.insertData(category);
//			}
//				
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//	}
	
	public static void main(String[] args) throws Exception {
		Property.setProperties("configs/configuration_esa.properties");
		MongoExtractArticlesOfCategory m = new MongoExtractArticlesOfCategory("configs/wikipedia-template-en.xml",
				"en");

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			System.out.println("\nEnter article title (or enter to quit): ");
			String title = in.readLine();

			if (title == null || title.equals(""))
				break;
			Category category = m.getCategoryByTitle(title);
			List<DepthArticle> articles = m.treeTraversal(category, "", 0);
			int i = 0;
			for(DepthArticle article : articles) {
				System.out.println(article.getArticle() + ": " + article.getDepth());
				if(i++ > 100)
					break;
			}

		}

	}
	
}