package edu.kit.aifb.gwifi.textcategorization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;
/**
 * choose the wiki categories in a layer
 * @author aifb-ls3
 *
 */
public class ExtractArticlesOfCategory {

	private Wikipedia wiki;
	private Set<String> allChildCategories = new HashSet<String>();

	private PrintWriter pw;
	private PrintWriter pwDic;
	private File outputDir;
	private File outputFile;

	int MAX_DEPTH;
	int MIN_DEPTH;
	int beginID = 0;
	String lastMaxDepthCat = "";
	Set<String> childrenSet = new HashSet<String>();
	Set<String> dealedSet = new HashSet<String>();
	int catID = 0;

	private Map<String, Set<String>> cache = new HashMap<String, Set<String>>();

	

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

	public ExtractArticlesOfCategory(String dir, String file, int catIDBegin)
			throws Exception {
		outputDir = new File(dir);
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}
		outputFile = new File(outputDir.getAbsolutePath() + File.separator
				+ file);
		pw = new PrintWriter(new FileWriter(outputFile, true));
		String dictFile = outputDir.getAbsolutePath() + File.separator + file
				+ ".dic";
		pwDic = new PrintWriter(new FileWriter(dictFile, true));
		loadDealedSet(dictFile);

		File f = new File("./configs/wikipedia-template-en.xml");
		wiki = new Wikipedia(f, false);
		
		this.catID = catIDBegin;
	}

	private void loadDealedSet(String file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] data = line.split(",");
			dealedSet.add(data[1]);
//			catID = Integer.parseInt(data[0]);
		}
//		catID++;
		br.close();
	}

	public Category getCategoryByTitle(String catName) {
		return wiki.getCategoryByTitle(catName);
	}
	
	public static class DepthArticle{
		private int depth = 0;
		private String article;
		
		public DepthArticle(String article, int depth){
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
		// titles of current categor
		Article[] arts = cat.getChildArticles();
		for (Article t : arts) {
			curArtTitles.add(new DepthArticle(t.getTitle(),depth));
		}

		this.giveColor(cat);

		// collect articles from all children categories
		Category[] children = cat.getChildCategories();
		for (Category childCat : children) {
			List<DepthArticle> childrenArts = treeTraversal(childCat, curPath,
					depth + 1);

			if (childrenArts != null) {
				curArtTitles.addAll(childrenArts);
				childrenArts.clear();
				childrenArts = null;
			}
		}

		this.removeColor(cat);

		if (depth >= this.getMIN_DEPTH() && depth <= this.getMAX_DEPTH()
				&& !dealedSet.contains(cat.getTitle())) {
			catID++;
			pwDic.println(catID + "," + cat.getTitle());
			printMapping(catID, curArtTitles);
			dealedSet.add(cat.getTitle());
			System.out.println(curPath + " " + curArtTitles.size());
		}
		if (depth == this.getMIN_DEPTH()) {
			curArtTitles.clear();
			curArtTitles = null;
		}
		return curArtTitles;
	}

	List<String> content = new ArrayList<String>();

	private void printMapping(int cat, List<DepthArticle> arts) {
		for (DepthArticle art : arts) {
			content.add(cat + "," + art.getArticle()+","+art.getDepth());
		}
		if (content.size() > 100000) {
			for (String s : content) {
				pw.println(s);
			}
			pw.flush();
			content.clear();
		}
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

	public void finish() {
		for (String s : content) {
			pw.println(s);
		}
		this.pw.close();
		this.pwDic.close();
	}
	public void testPrint(){
		if(wiki.getCategoryByTitle("Contents") == null){
			System.out.print("errrrrrrr");
		}else{
			System.out.print(wiki.getCategoryByTitle("Contents"));
		}
	}
	/**
	 * 
	 * 
	 * @param args[0],args[1] are the number of layer we choose which are the same
	 * @throws Exception
	 */
	public static void main(String[]args) throws Exception {
		int catIDBegin = 1;
		 String categoryNode = "Main topic classifications";
		//String categoryNode ="Contents";

		//ExtractArticlesOfCategory m = new ExtractArticlesOfCategory(
				//"/Users/aifb-ls3/MasterThesis/English_operation", "mapping_en.csv", catIDBegin);
		//ExtractArticlesOfCategory m = new ExtractArticlesOfCategory(
			//"/home/ls3data/users/lzh/congliu", "mapping_en.csv", catIDBegin);
		ExtractArticlesOfCategory m = new ExtractArticlesOfCategory(
				"/home/ls3data/users/lzh/congliu", "mapping_en_dep3.csv", catIDBegin);
		
		m.setMAX_DEPTH(Integer.parseInt(args[0]));
		m.setMIN_DEPTH(Integer.parseInt(args[1]));
		Category cat = m.getCategoryByTitle(categoryNode);
		 m.treeTraversal(cat, "", 0);
		 m.finish();
		 System.out
		 .print("CSV is created, please check the directory:/home/ls3data/users/lzh/congliu");

	}


}