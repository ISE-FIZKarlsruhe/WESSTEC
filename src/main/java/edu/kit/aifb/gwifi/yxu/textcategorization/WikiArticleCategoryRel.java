package edu.kit.aifb.gwifi.yxu.textcategorization;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import edu.kit.aifb.gwifi.annotation.detection.DisambiguationUtil;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.mongo.Property;
import edu.kit.aifb.gwifi.mongo.search.MongoEntityCategoryEmbeddingSearcher;
import edu.kit.aifb.gwifi.mongo.search.MongoMemoryBasedWordEmbeddingSearcher;
import edu.kit.aifb.gwifi.util.RelatednessCache;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class WikiArticleCategoryRel {

	private Wikipedia wikipedia;
	private RelatednessCache rc;
	private MongoEntityCategoryEmbeddingSearcher searcher;
//	private MongoMemoryBasedWordEmbeddingSearcher searcher;
	
	//the smallest depth, which might be checked
	private int upperDepth;		
	//the max. distance from child to parent with higher depth
//	private int backwardsDist;
	//the max. length of path from scrA to tarC, which should be considered
	private int maxPathLen;		
	//the max. length of path based on the distance between scrA to tarC
	private int maxPlusDist;
	//the number of selected parent cates, which are significant to child cate
	private int topPCateNum;	
	private double minRelatedness;
	
	public WikiArticleCategoryRel(Wikipedia wikipedia, RelatednessCache rc, 
			MongoEntityCategoryEmbeddingSearcher searcher){
		this.wikipedia = wikipedia;
		this.rc=rc;
		this.searcher = searcher;
		this.upperDepth = wikipedia.getRootCategory().getDepth()+1;
		this.maxPathLen = 6;
		this.maxPlusDist = 5;
		this.topPCateNum = 2;
		this.minRelatedness = 0.001;
	}
	
	public void setUpperDepth(int upperDepth){
		this.upperDepth = upperDepth;
	}
	
	public void setMaxPathLen(int maxPathLen){
		this.maxPathLen = maxPathLen;
	}
	
	public void setMaxPlusDist(int maxPlusDist){
		this.maxPlusDist = maxPlusDist;
	}
	
	public void setTopPCateNum(int topPCateNum){
		this.topPCateNum = topPCateNum;
	}
	
	public void buildAndPrintA2CRels(Collection<String> scrATitles, Collection<String> tarCTitles){
		Set<Article> scrArticles = new HashSet<Article>();
		Set<Category> tarCates = new HashSet<Category>();
		transStr2Page(scrATitles, tarCTitles, scrArticles, tarCates);
		Map<Article, Collection<PageRel>> allArticle2Rels = new HashMap<Article, Collection<PageRel>>();
		Map<Category, Collection<PageRel>> allCate2Rels = new HashMap<Category, Collection<PageRel>>();
		buildA2CRels(scrArticles, tarCates, allArticle2Rels, allCate2Rels);
		printA2CRels(allArticle2Rels);
	}
	
	private void transStr2Page(Collection<String> scrATitles, Collection<String> tarCTitles, 
			Set<Article> scrArticles, Set<Category> tarCates){
		Article scrArticle;
		for(String scrATitle: scrATitles){
			scrArticle = wikipedia.getArticleByTitle(scrATitle);
			if(scrArticle == null|| !scrArticle.exists()){
				System.out.println(scrATitle + " is no article!");
				continue;
			}
			scrArticles.add(scrArticle);
		}
		Category tarCate;
		for(String tarCTitle: tarCTitles){
			tarCate = wikipedia.getCategoryByTitle(tarCTitle);
			if(tarCate == null|| !tarCate.exists()){
				System.out.println(tarCTitle + " is no category!");
				continue;
			}
			tarCates.add(tarCate);
		}
	}
	
	private void printA2CRels(Map<Article, Collection<PageRel>> allArticle2Rels){
		for(Article article: allArticle2Rels.keySet()){
			for(PageRel a2cRel: allArticle2Rels.get(article)){
				System.out.println(a2cRel.toString());
			}
		}
	}
	
	public void buildA2CRels(Collection<Article> scrArticles, Collection<Category> tarCates, 
			Map<Article, Collection<PageRel>> allArticle2Rels,
			Map<Category, Collection<PageRel>> allCate2Rels){
		Map<Category, Collection<PageRel>> checkedCate2Rels;
		for(Article scrArticle: scrArticles){
			checkedCate2Rels = new HashMap<Category, Collection<PageRel>>();
			Map<Category, PageRel> a2cRels = buildA2CsRel(scrArticle, tarCates, checkedCate2Rels);
			allArticle2Rels.put(scrArticle, a2cRels.values());
			//TODO merge cateRels into checked cateRels
//			allCate2Rels.putAll(checkedCate2Rels);
		}
	}
	
	public Map<Category, Double> getTarCateRelatedness(Article scrArticle, Collection<Category> tarCates){
		Map<Category, Collection<PageRel>> checkedCate2Rels = new HashMap<Category, Collection<PageRel>>();
		Map<Category, PageRel> a2csRel = buildA2CsRel(scrArticle, tarCates, checkedCate2Rels);
		Map<Category, Double> tarCate2Relatedness = new HashMap<Category, Double>();
		for(Category cate:a2csRel.keySet()){
			tarCate2Relatedness.put(cate, a2csRel.get(cate).getRel());
		}
		return tarCate2Relatedness;
	}
	
	public Map<Category, PageRel> buildA2CsRel(Article scrArticle, Collection<Category> tarCates, 
			Map<Category, Collection<PageRel>> checkedCate2Rels){
		Map<Category, PageRel> tarC2Rel = new HashMap<Category, PageRel>();
		ArrayList<Category> pathCates = new ArrayList<Category>();
		Category[] pCateArray = scrArticle.getParentCategories();
		if(pCateArray == null || pCateArray.length==0){	//article has no parent cate
			return tarC2Rel;
		}
		if(scrArticle==null||!scrArticle.exists()){
			System.out.println("null article!");
			return tarC2Rel;
		}
		Map<Category, Double> pCates = filterPCates(scrArticle.getTitle(), false, pCateArray);
		pCateArray = null;
		double rel = 0.0;
		PageRel a2c;
		Map<Category, PageRel> pCateRels;
		for(Category pCate: pCates.keySet()){
			rel = pCates.get(pCate);
			if(tarCates.contains(pCate)){	//reach target
				a2c = tarC2Rel.get(pCate);
				if(a2c==null){
					a2c = new PageRel(scrArticle, pCate, rel);
					a2c.setRelPath(new ArrayList<Category>());
					tarC2Rel.put(pCate, a2c);
				}else if(a2c.getRel()<rel){
					a2c.setRel(rel);
					a2c.setRelPath(new ArrayList<Category>());
				}
				a2c.addPath(new ArrayList<Category>());
			}else if(checkedCate2Rels.containsKey(pCate)){	//reach checked path
				//add the checked path to the current path
				updateCRelWithPRel(scrArticle, rel, checkedCate2Rels.get(pCate), tarC2Rel);
			}else if(pathCates.contains(pCate)){	//path is still empty, unexpected
			}else if(pCate.getDepth()<=upperDepth){	//reach upper depth
			}else{
				pathCates.add(pCate);
				pCateRels = buildC2CsRel(pCate, pathCates, checkedCate2Rels, tarCates);
				if(!pathCates.remove(pathCates.size()-1).equals(pCate)){	//throw new RuntimeException();
					System.out.println("Error! "+ pCate.getTitle());
				}
				if(pathCates.size()!=0) {
					System.out.println("Error! base-path:"+ pathCates.toString());
					pathCates.clear();
				}
				//add the parent path to the current path
				updateCRelWithPRel(scrArticle, rel, pCateRels.values(), tarC2Rel);
			}
		}
		normalizeTarCate2Rel(tarC2Rel);
		return tarC2Rel;
	}
	
	private Map<Category, PageRel> buildC2CsRel(Category cCate, ArrayList<Category> pathCates, 
			Map<Category, Collection<PageRel>> checkedCate2Rels, Collection<Category> tarCates){
		Map<Category, PageRel> tarC2Rel = new HashMap<Category, PageRel>();
		if(pathCates.size()>maxPathLen) return tarC2Rel; //reach the max. path length
		Category[] pCateArray = cCate.getParentCategories();
		if(pCateArray == null || pCateArray.length==0){	//reach dead alley, cCate has no path out
			checkedCate2Rels.put(cCate, new HashSet<PageRel>());
			return tarC2Rel;
		}
		Map<Category, Double> pCate2Relatedness = filterPCates(cCate.getTitle(), true, pCateArray);
		pCateArray = null;
		double rel = 0.0;
		PageRel cRel;	//cRel: cCate --> pCate
		Map<Category, PageRel> pCateRels;
		for(Category pCate: pCate2Relatedness.keySet()){
			rel = pCate2Relatedness.get(pCate);
			if(tarCates.contains(pCate)){	//reach target
				cRel = tarC2Rel.get(pCate);
				if(cRel==null){
					cRel = new PageRel(cCate, pCate, rel);
					cRel.setRelPath(new ArrayList<Category>());
					tarC2Rel.put(pCate, cRel);
				}else if(cRel.getRel()<rel){
					cRel.setRel(rel);
					cRel.setRelPath(new ArrayList<Category>());
				}
				cRel.addPath(new ArrayList<Category>());
			}else if(checkedCate2Rels.containsKey(pCate)){	//reach checked path
				// add the checked path to the current path 
				updateCRelWithPRel(cCate, rel, checkedCate2Rels.get(pCate), tarC2Rel);
			}else if(pathCates.contains(pCate)){	//TODO reach current path, in loop
				//avoid loop with help of cate-depth
				//diff. article share checkedRel : unckeck the cCate?
				//diff. article has own checkedRel : ignore
			}else if(pCate.getDepth()<=upperDepth){	//reach upper depth
			}else{
				pathCates.add(pCate);
				pCateRels = buildC2CsRel(pCate, pathCates, checkedCate2Rels, tarCates);
				if(!pathCates.remove(pathCates.size()-1).equals(pCate)){	//throw new RuntimeException();
					System.out.println("Error! "+ pCate.getTitle());
				}
				//add the parent path to the current path
				updateCRelWithPRel(cCate, rel, pCateRels.values(), tarC2Rel);
			}
		}
		normalizeTarCate2Rel(tarC2Rel);
		checkedCate2Rels.put(cCate, tarC2Rel.values());
		return tarC2Rel;
	}
	
	private void updateCRelWithPRel(Page cPage, double c2pRelatedness, Collection<PageRel> pRels, Map<Category, PageRel> tarC2Rel){
		Category p1;	//pRel: p1 --> p2
		Category p2;
		PageRel cRel;	//cRel: cPage --> p2
		ArrayList<Category> cPath;
		double c2tRelatedness = 0.0;
		for(PageRel pRel: pRels){
			p1 = (Category) pRel.getP1();
			p2 = (Category) pRel.getP2();
			c2tRelatedness = c2pRelatedness*pRel.getRel();
			cRel = tarC2Rel.get(p2);
			if(cRel==null){
				cRel = new PageRel(cPage, p2, c2tRelatedness);
				cPath = new ArrayList<Category>();
				cPath.add(p1);
				cPath.addAll(pRel.getRelPath());
				cRel.setRelPath(cPath);
				tarC2Rel.put(p2, cRel);
			}else if(cRel.getRel()<c2tRelatedness){
				cRel.setRel(c2tRelatedness);
				cPath = new ArrayList<Category>();
				cPath.add(p1);
				cPath.addAll(pRel.getRelPath());
				cRel.setRelPath(cPath);
			}
			for(ArrayList<Category> pPath: pRel.getPathes()){
				cPath = new ArrayList<Category>();
				cPath.add(p1);
				cPath.addAll(pPath);
				cRel.addPath(cPath);
			}
		}
	}
	
	private Map<Category, Double> filterPCates(String child, boolean isChildCate, Category[] pCateArray){
		Set<Category> pCates = new HashSet<Category>();
		for(Category pCate: pCateArray){
			if(!pCate.exists()) continue;		// no category
			if(pCate.getDepth()==null)continue; // invalid category
			pCates.add(pCate);
		}
		return filterPCates(child, isChildCate, pCates);
	}
	
	synchronized private Map<Category, Double> filterPCates(String child, boolean isChildCate, Set<Category> pCates){
		Set<Category> highPCates = pCates;//filterPCatesWithDepth(pCates, cDepth);
		Map<Category, Double> pCate2Rel = new HashMap<Category, Double>();
		Map<Category, Double> standardPCate2Rel = new HashMap<Category, Double>();
		boolean isFilter = true;
		Article cArticle = wikipedia.getArticleByTitle(child);
		Article pArticle;
		TreeMap<Double, Set<Category>> rel2pCate = new TreeMap<Double, Set<Category>>();
		double rel = 0.0;
		Set<Category> cates4rel;
		for (Category pCate : highPCates) {
			// TODO embedding
//			rel = searcher.getRelatedness(child, pCate.getTitle());
			rel = searcher.getRelatedness(child, isChildCate, pCate.getTitle(),true);//-1
			if (rel >= 0.0) {
				if (rel < minRelatedness)
					continue;
				cates4rel = rel2pCate.get(rel);
				if (cates4rel == null) {
					cates4rel = new HashSet<Category>();
					rel2pCate.put(rel, cates4rel);
				}
				cates4rel.add(pCate);
			} else {
				pArticle = wikipedia.getArticleByTitle(pCate.getTitle());
				// TODO decide relatedness to parent category
				if (pArticle == null || cArticle == null) {
					// isFilter = false;
					// break; //take all pCates,
					standardPCate2Rel.put(pCate, 1.0); // or take this pCate ignoring sorting
				} else {
					rel = rc.getRelatedness(cArticle, pArticle);
					if (rel < minRelatedness)
						continue;
					cates4rel = rel2pCate.get(rel);
					if (cates4rel == null) {
						cates4rel = new HashSet<Category>();
						rel2pCate.put(rel, cates4rel);
					}
					cates4rel.add(pCate);
				}
			}
		}
		// select the top 2 pCates
		int topNum = (topPCateNum < rel2pCate.size()) ? topPCateNum : rel2pCate.size();
		Entry<Double, Set<Category>> top;
		for (int i = 0; i < topNum; i++) {
			top = rel2pCate.pollLastEntry();
			rel = top.getKey();
			for (Category pCate : top.getValue()) {
				pCate2Rel.put(pCate, rel);
			}
		}
		if(!isFilter){
			noFilterPCates(highPCates, pCate2Rel);
		}else{
			normalizePCate2Rel(pCate2Rel,standardPCate2Rel);
		}
		return pCate2Rel;
	}
	
//	private Set<Category> filterPCatesWithDepth(Set<Category> pCates, int cDepth){
//		Set<Category> highPCates = new HashSet<Category>();
//		for(Category pCate: pCates){
//			int pDepth = pCate.getDepth();
//			if(pDepth>cDepth+backwardsDist) continue;
//			highPCates.add(pCate);
//		}
//		return highPCates;
//	}
	
	private void noFilterPCates(Set<Category> pCates, Map<Category, Double> pCate2Rel){
		int pCateNum = pCates.size();
		double rel = 1/((double)pCateNum);
		for(Category pCate: pCates){
			pCate2Rel.put(pCate, rel);
		}
	}

	private void normalizeTarCate2Rel(Map<Category, PageRel> tarCate2Rel){
		double totalRel = 0.0;
		Set<Category> unrelatedCates = new HashSet<Category>();
		for(Category tarCate: tarCate2Rel.keySet()){
			if(!tarCate2Rel.get(tarCate).checkRelated()){
				unrelatedCates.add(tarCate);
			}
		}
		for(Category unrelatedCate: unrelatedCates){
			tarCate2Rel.remove(unrelatedCate);
		}
		for(PageRel tarCateRel: tarCate2Rel.values()){
			totalRel += tarCateRel.getRel();
		}
		if(totalRel==0.0){
			totalRel = 1.0;
		}
		double relativeTarCateRel = 0.0;
		for(PageRel tarCateRel: tarCate2Rel.values()){
			relativeTarCateRel = tarCateRel.getRel()/totalRel;
			tarCateRel.setRel(relativeTarCateRel);
		}
	}
	
	private void normalizePCate2Rel(
			Map<Category, Double> pCate2Rel, 
			Map<Category, Double> standardPCate2Rel){
		double pCateNum = pCate2Rel.size();
		double standardPCateNum = standardPCate2Rel.size();
		double pCateRatio = pCateNum/(pCateNum+standardPCateNum);
		double standardPCateRatio = 1.0/(pCateNum+standardPCateNum);
		double totalRel = 0.0;
		for(Double pCateRel: pCate2Rel.values()){
			totalRel += pCateRel;
		}
		if(totalRel==0.0){
			totalRel = 1.0;
		}
		double relativePCateRel = 0.0;
		for(Category pCate: pCate2Rel.keySet()){
			relativePCateRel = pCateRatio*pCate2Rel.get(pCate)/totalRel;
			pCate2Rel.put(pCate, relativePCateRel);
		}
		for(Category standardPCate: standardPCate2Rel.keySet()){
			relativePCateRel = standardPCateRatio*standardPCate2Rel.get(standardPCate);
			pCate2Rel.put(standardPCate, relativePCateRel);
		}
	}
	
	public class PageRel{
		private Page p1;
		private Page p2;
		private int relId;
		private double rel;
		private ArrayList<Category> relPath;
		private Set<ArrayList<Category>> p1p2Pathes;
		
		public PageRel(Page p1, Page p2, double rel){
			this.p1 = p1;
			this.p2 = p2;
			this.relId = Objects.hash(p1.getId(),p2.getId());
			this.rel = rel;
			this.relPath = null;
			this.p1p2Pathes = new HashSet<ArrayList<Category>>();
		}
		
		public Page getP1(){
			return p1;
		}

		public Page getP2(){
			return p2;
		}
		
		public int getRelId(){
			return relId;
		}
		
		public double getRel(){
			return rel;
		}
		
		public void setRel(double rel){
			this.rel = rel;
		}
		
		public ArrayList<Category> getRelPath(){
			return relPath;
		}
		
		public void setRelPath(ArrayList<Category> relPath){
			this.relPath = relPath;
		}
		
		public Set<ArrayList<Category>> getPathes(){
			return p1p2Pathes;
		}
		
		public void addPath(ArrayList<Category> path){
			this.p1p2Pathes.add(path);
		}
		
		public boolean checkRelated(){
			if(rel>0.0 && relPath!=null) return true;
			return false;
		}
		
		public String toString(){
			String to = " --> ";
			String sp = "|";
			StringBuilder pageRelStr = new StringBuilder();
			int p1Depth = p1.getDepth();
			String p1TitleDepth = p1.getTitle()+sp+p1Depth;
			int p2Depth = p2.getDepth();
			String p2TitleDepth = p2.getTitle()+sp+p2Depth;
			pageRelStr.append(p1TitleDepth+to+p2TitleDepth+" :\n");
			if(relPath == null) return "";
			if(relPath.size()>(p1Depth-p2Depth+maxPlusDist)) return "";
			pageRelStr.append("Best fit path : "+rel+"\n");
			String cTitlePath = to;
			for(Category cate: relPath){
				cTitlePath += cate.getTitle()+sp+cate.getDepth() + to;
			}
			pageRelStr.append(cTitlePath+"\n");
			pageRelStr.append("All pathes :\n");
			int pathNum = 0;
			for(ArrayList<Category> path: p1p2Pathes){
				if(path.size()>(p1Depth-p2Depth+maxPlusDist)) continue;
				cTitlePath = to;
				for(Category cate: path){
					cTitlePath += cate.getTitle()+sp+cate.getDepth() + to;
				}
				pathNum++;
				pageRelStr.append(cTitlePath+"\n");
			}
			if(pathNum>0) return pageRelStr.toString();
			return "";
		}
		
		@Override
		public int hashCode(){
			return relId;
		}
		
		@Override
		public boolean equals(Object obj){
			if(obj instanceof PageRel){
				if(((PageRel) obj).getRelId() == this.relId){
					return true;
				}else{
					return false;
				}
			}else{
				return false;
			}
		}
	}
	
	public void setupOutputFile(String dir, String file, int catIDBegin){
		// 
	}
	
	/**
	 * 
	 * 
	 * @param 
	 * maxPathLen, 	upperDepth, 	topPCateNum
	 * 6,			1,				1
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		File databaseDirectory = new File("configs/wikipedia-template-en.xml");
		Wikipedia wikipedia = new Wikipedia(databaseDirectory, false);
		DisambiguationUtil disambiguator = new DisambiguationUtil(wikipedia);
		RelatednessCache rc = new RelatednessCache(disambiguator.getArticleComparer());
		Property.setProperties("configs/MongoConfig_esa.properties");
		MongoEntityCategoryEmbeddingSearcher mongoSearcher = new MongoEntityCategoryEmbeddingSearcher(Language.EN);
//		MongoMemoryBasedWordEmbeddingSearcher mongoSearcher = new MongoMemoryBasedWordEmbeddingSearcher(Language.EN);
		WikiArticleCategoryRel wikiACR = new WikiArticleCategoryRel(wikipedia, rc, mongoSearcher);
		wikiACR.setMaxPathLen(Integer.valueOf(args[0]));//6
		wikiACR.setUpperDepth(Integer.valueOf(args[1]));//1
		wikiACR.setTopPCateNum(Integer.valueOf(args[2]));//1
		Scanner scanner = new Scanner(System.in);
		Set<String> cateTitles = new HashSet<String>();
//		cateTitles.add("Economy");
		cateTitles.add("Economics");
		cateTitles.add("Trade");
		cateTitles.add("Money");
		cateTitles.add("Business");
		cateTitles.add("Industry");
		cateTitles.add("Politics");
		cateTitles.add("Society");
		cateTitles.add("Culture");
		cateTitles.add("Nature");
		cateTitles.add("Health");
		cateTitles.add("People");
		cateTitles.add("Religion");
		cateTitles.add("Science");
		cateTitles.add("Technology");
		cateTitles.add("Sports");
		cateTitles.add("Geography");
		cateTitles.add("History");
		cateTitles.add("Mathematics");
		cateTitles.add("Philosophy");
		String[] articleArray;
		Set<String> articleTitles;
		while (true) {
			System.out.println("Please input the articles:");
			String articles = scanner.nextLine();
			if (articles.startsWith("exit")) {
				break;
			}else if(articles.trim().isEmpty()){
				continue;
			}
			articleArray = articles.split(",");
			articleTitles = new HashSet<String>();
			for(int i=0;i<articleArray.length;i++){
				articleArray[i] = articleArray[i].trim().replace('_', ' ');
				articleTitles.add(articleArray[i]);
			}
			wikiACR.buildAndPrintA2CRels(articleTitles, cateTitles);
		}
		scanner.close();

	}

}
