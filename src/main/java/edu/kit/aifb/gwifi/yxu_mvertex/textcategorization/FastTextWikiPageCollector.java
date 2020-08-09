package edu.kit.aifb.gwifi.yxu_mvertex.textcategorization;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;

public class FastTextWikiPageCollector extends WikiPageCollector {

	private int maxThreadNum;
	private PrintWriter pw;
	public FastTextWikiPageCollector(Wikipedia wikipedia,
			String customizedCategoriesFilename, int maxThreadNum) throws FileNotFoundException {
		super(wikipedia, customizedCategoriesFilename);
		this.maxThreadNum = maxThreadNum;
		String cate2ArticlesFileName = getCate2ArticlesFileNameFromCateFileName(customizedCategoriesFilename, "txt");
		pw = new PrintWriter(createFileWithPW(cate2ArticlesFileName));
	}
	
	public class CateThread implements Runnable {
		private int articleNum;
		private List<String> cateTitles;

		public CateThread(int articleNum, List<String> cateTitles) {
			this.articleNum = articleNum;
			this.cateTitles = cateTitles;
		}

		@Override
		public void run() {
			Map<Integer, Article> id2Article = new HashMap<Integer, Article>();
			Map<Integer, Category> id2Cate = new HashMap<Integer, Category>();
			Map<Integer,Set<Integer>> cateId2ArticleIds = getCateId2FullChildArticleIdsOfCatesWithTitles(cateTitles, id2Article, id2Cate, articleNum);
			Map<String, Set<String>> cate2Articles = replaceC2AsIdMapToStringMap(cateId2ArticleIds, id2Article, id2Cate);
			String cateTitle = cateTitles.get(0);
			storeArticlesInCateTXT(cateTitle, cate2Articles.get(cateTitle));
		}

	}
	
	public void collectCate2FullChildArticlesOfCatesInCateFileToTXT(String cateFileName, int articleNum) throws IOException{
		List<String> cateTitles = extractCateTitlesFromCateFile(cateFileName);
		collectCate2FullChildArticlesOfCatesWithTitlesToTXT(cateTitles, articleNum);
	}

	public void collectCate2FullChildArticlesOfCatesWithTitlesToTXT(List<String> cateTitles, int articleNum) throws IOException{

		ExecutorService executorService = Executors.newFixedThreadPool(maxThreadNum);
		List<String> currentCate;
		for(String cateTitle: cateTitles){
			currentCate = new ArrayList<String>();
			currentCate.add(cateTitle);
			Runnable cateThread = new CateThread(articleNum, currentCate);
			executorService.execute(cateThread);
		}
		executorService.shutdown();
		while (!executorService.isTerminated()) {
			// waiting for the terminate of the thread pool service
		}
		pw.flush();
		pw.close();
	}
	
	public void storeCateId2ArticleIdsMapInTXT(Map<Integer,Set<Integer>> cateId2ArticleIds, Map<Integer, Article> id2Article, Map<Integer, Category> id2Cate) throws IOException{
		storeCate2ArticlesStringMapInTXT(replaceC2AsIdMapToStringMap(cateId2ArticleIds, id2Article, id2Cate));
	}
	
	public void storeCate2ArticlesStringMapInTXT(Map<String,Set<String>> cate2ArticlesStringMap){
		String fileType = "txt";
		String label = "__label__";
		String cate2ArticlesFileName = getCate2ArticlesFileNameFromCateFileName(customizedCategoriesFilename, fileType);
		PrintWriter pw0;
		try {
			pw0 = new PrintWriter(createFileWithPW(cate2ArticlesFileName));
			for(String cate: cate2ArticlesStringMap.keySet()){
				for(String article: cate2ArticlesStringMap.get(cate)){
					//TODO validate the article string, asyn. in n threads.
					String cate2Article = label + cate + ": " + article;
					pw0.println(cate2Article);
				}
			}
			pw0.flush();
			pw0.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void storeArticlesInCateTXT(String cateTitle, Set<String> articles){
		String fileType = "txt";
		String label = "__label__"+cateTitle+": ";
//		String cate2ArticlesFileName = getCate2ArticlesFileNameFromCateFileName(cateTitle, customizedCategoriesFilename, fileType);
//		PrintWriter pw0;
//		try {
//			pw0 = new PrintWriter(createFileWithPW(cate2ArticlesFileName));
			int articleIndex = 0;
			System.out.println(cateTitle);
			for(String article: articles){
				if(article == null||article.trim().length()==0) continue;
				articleIndex++;
//				if (articleIndex < 0) {
//					continue;
//				}
//				if (articleIndex > 10000) {
//					break;
//				}
//				System.out.println(articleIndex);
				//TODO validate the article string, asyn. in n threads.
				String cate2Article = label + filterArticleText(article);
				synchronized (pw) {
					pw.println(cate2Article);
				}
			}
//			pw0.flush();
//			pw0.close();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
	}
	
	private String getCate2ArticlesFileNameFromCateFileName(String cateTitle, String cateFileName, String fileType){
		int pathEndAt = cateFileName.lastIndexOf("/")+1;
		String path = cateFileName.substring(0, pathEndAt);
		String cateFileShortName = cateFileName.substring(pathEndAt);
		String[] sp = cateFileShortName.split("\\.");
		cateFileShortName =  cateTitle + "." + fileType;
		return path + sp[0] + "/" + cateFileShortName;
	}
	
	public void collectFullChildArticle2CatesOfCatesInCateFileToTXT(String cateFileName, int articleNum) throws IOException{
		List<String> cateTitles = extractCateTitlesFromCateFile(cateFileName);
		collectFullChildArticle2CatesOfCatesWithTitlesToTXT(cateTitles, articleNum);
	}

	public void collectFullChildArticle2CatesOfCatesWithTitlesToTXT(List<String> cateTitles, int articleNum) throws IOException{
		Map<Integer, Article> id2Article = new HashMap<Integer, Article>();
		Map<Integer, Category> id2Cate = new HashMap<Integer, Category>();
		Map<Integer,List<Integer>> articleId2CateIds = getFullChildArticleId2CateIdsOfCatesWithTitles(cateTitles, id2Article, id2Cate, articleNum);
		storeArticleId2CateIdsMapInTXT(articleId2CateIds, id2Article, id2Cate);
	}
	
	public void storeArticleId2CateIdsMapInTXT(Map<Integer,List<Integer>> articleId2CateIds, Map<Integer, Article> id2Article, Map<Integer, Category> id2Cate) throws IOException{
		storeArticle2CatesStringMapInTXT(replaceA2CsIdMapToStringMap(articleId2CateIds, id2Article, id2Cate));
	}
	
	public void storeArticle2CatesStringMapInTXT(Map<String,List<String>> article2CatesStringMap){
		String fileType = "txt";
		String article2CatesFileName = getArticle2CatesFileNameFromCateFileName(customizedCategoriesFilename, fileType);
		PrintWriter pw0;
		try {
			pw0 = new PrintWriter(createFileWithPW(article2CatesFileName));
			for(String article: article2CatesStringMap.keySet()){
				String article2Cates = article + ": ";
				for(String cate: article2CatesStringMap.get(article)){
					article2Cates += " " + cate + ",";
				}
				article2Cates = article2Cates.substring(0, article2Cates.length()-1);
				pw0.println(article2Cates);
			}
			pw0.flush();
			pw0.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private File createFileWithPW(String filename)
			throws FileNotFoundException {
		File file = new File(filename);
		if (!file.exists()) {
			file.getParentFile().mkdirs();
		}
		return file;
	}
	
	private String filterArticleText(String articleText){
		// <([{\^-=$!|]})?*+.>
		// \s = [ \t\n\x0B\f\r]
		Pattern indexMark = Pattern.compile("(^\\s*\\*+)|(\\s+\\*+)");
		Pattern comment = Pattern.compile("[<]\\s*\\-+.*\\-+\\s*[>]");
		Pattern pic = Pattern.compile("([{]{2}[^{&&[^}]]*[}]{2})|([{][^{&&[^}]]*[}])");
//		Pattern pic_potential = Pattern.compile("([{]{1,}[^}]*$)|(^[^{]*[}]{1,})");
		Pattern pic_file = Pattern.compile("[_\\w\\-]*\\s*\\.(bmp|BMP|gif|GIF|jpg|JPG|jpeg|JPEG|png|PNG|tif|TIF|2em|2EM)");
		Pattern pic_px = Pattern.compile("[x\\d]+px([ ]|$)");
		Pattern attr = Pattern.compile("([a-zA-Z\\-]{2,}[=:][^;]*[;])|([a-zA-Z\\-]{2,}[=:]\\s*[#]\\w+)");
		Pattern url = Pattern.compile("((ftp|FTP|http|HTTP|https|HTTPS)://\\S*)|(\\S*\\.(htm|HTM|html|HTML|pdf|PDF))");
		Pattern ref = Pattern.compile("(^[&]\\S*)|(\\S*[&]$)|(\\S*[\\W&&\\S]\\S*[&]\\S*)|(\\S*[&]\\S*[\\W&&\\S]\\S*)|(\\S*%\\w+\\S*)");
		Pattern and_sharp_perc = Pattern.compile("(^[&#%]+\\S*)|(\\S*[&#]+$)|([ ][&#%]+\\S*)|(\\S*[&#]+[ ])");
		Pattern equal_colon = Pattern.compile("[=:]");
		Pattern parentheses = Pattern.compile("[\\[(|)\\]]");
		Pattern quotMark = Pattern.compile("\"");
		Pattern singleQuotMark = Pattern.compile("(^\')|(\'$)|(\'\\W)|(\\W\')");
		Pattern hyphen = Pattern.compile("(^\\-)|(\\-$)|(\\-\\W)|(\\W\\-)");
		Pattern whiteSpace = Pattern.compile("\\s+");
		Pattern table = Pattern.compile("\\t");
		Pattern newline = Pattern.compile("\\n");
		Pattern comma_dot = Pattern.compile("(\\s+[,.?]+)|([,.?]+\\s+)|(^[,.?]+)|([,.?]+$)|[;]");
		
		String filtered = "";
		String lines = articleText;
		
		for(String line: newline.split(lines)){
			for(String unit: table.split(line)){
				unit = indexMark.matcher(unit).replaceAll("");
				unit = comment.matcher(unit).replaceAll(" ");
				unit = pic.matcher(unit).replaceAll(" ");
				unit = pic_file.matcher(unit).replaceAll(" ");
				unit = attr.matcher(unit).replaceAll(" ");
				unit = url.matcher(unit).replaceAll(" ");
				unit = ref.matcher(unit).replaceAll(" ");
				unit = whiteSpace.matcher(unit).replaceAll("  ");
				unit = and_sharp_perc.matcher(unit).replaceAll(" ");
				unit = equal_colon.matcher(unit).replaceAll(" ");
				unit = parentheses.matcher(unit).replaceAll(" ");
				unit = quotMark.matcher(unit).replaceAll(" ");
				unit = singleQuotMark.matcher(unit).replaceAll(" ");
				unit = hyphen.matcher(unit).replaceAll(" ");
				
				unit = unit.trim();
				//ignore blank unit
				if (unit.length() == 0) continue;
				filtered+=unit+" ";
			}
		}
		filtered = comment.matcher(filtered).replaceAll(" ");
		while(pic.matcher(filtered).find()){
			filtered = pic.matcher(filtered).replaceAll(" ");
		}
		filtered = pic_px.matcher(filtered).replaceAll(" ");
//		filtered = pic_potential.matcher(filtered).replaceAll(" ");
		filtered = comma_dot.matcher(filtered).replaceAll(" ");
		filtered = whiteSpace.matcher(filtered).replaceAll(" ");
		return filtered;
	}

	// "10000" "10"
	public static void main(String[] args) throws Exception {
//		HubConfiguration config = new HubConfiguration(new File("configs/hub-template.xml"));
		String customizedCategoriesFilename = "res/categories.txt";//config.getCategoriesPath();/slow/users/lzh/xu/
		File databaseDirectory = new File("configs/wikipedia-template-en.xml");
		Wikipedia wikipedia = new Wikipedia(databaseDirectory, false);
		int articleNum = Integer.valueOf(args[0]);
		int maxThreadNum = Integer.valueOf(args[1]);
		FastTextWikiPageCollector fastTextPageCollector = new FastTextWikiPageCollector(wikipedia, customizedCategoriesFilename, maxThreadNum);
		
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
			if(cates.length()>0 && cateList.size()>0){
				fastTextPageCollector.collectCate2FullChildArticlesOfCatesWithTitlesToTXT(cateList, articleNum);
			} else {
				fastTextPageCollector.collectCate2FullChildArticlesOfCatesInCateFileToTXT(customizedCategoriesFilename, articleNum);
			}
//			String filtered = fastTextPageCollector.filterArticleText(cates);
//			System.out.println(filtered);
		}
		scanner.close();
	}

}
