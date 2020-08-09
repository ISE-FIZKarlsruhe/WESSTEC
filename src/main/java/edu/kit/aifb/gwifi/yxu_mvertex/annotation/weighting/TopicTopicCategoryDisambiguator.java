package edu.kit.aifb.gwifi.yxu_mvertex.annotation.weighting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import edu.kit.aifb.gwifi.annotation.detection.DisambiguationUtil;
import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.annotation.weighting.DummyTopicWeighter;
import edu.kit.aifb.gwifi.annotation.weighting.ITopicWeighter;
import edu.kit.aifb.gwifi.annotation.weighting.graph.HitsHubTopicWeighter;
import edu.kit.aifb.gwifi.annotation.weighting.graph.PageRankTopicWeighter;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Label;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.mongo.Property;
import edu.kit.aifb.gwifi.mongo.search.MongoEntityCategoryEmbeddingSearcher;
import edu.kit.aifb.gwifi.service.Service.DisambiguationModel;
import edu.kit.aifb.gwifi.util.Position;
import edu.kit.aifb.gwifi.util.RelatednessCache;
import edu.kit.aifb.gwifi.util.nlp.Language;
import edu.kit.aifb.gwifi.yxu_mvertex.annotation.detection.TopicCategory;
import edu.kit.aifb.gwifi.yxu_mvertex.annotation.weighting.graph.PageRankAndHitsHubTopicTopicCategoryWeighter;
import edu.kit.aifb.gwifi.yxu_mvertex.textcategorization.Aida2WikiCateSysConverter;
import edu.kit.aifb.gwifi.yxu_mvertex.textcategorization.WikiArticleCategoryRel;
import edu.kit.aifb.gwifi.yxu_mvertex.textcategorization.WikiPageCollector;

public class TopicTopicCategoryDisambiguator {

	private Wikipedia wikipedia;
//	private RelatednessCache rc;
	private Map<DisambiguationModel, ITopicWeighter> model2TWeighter;
	private WikiPageCollector pageCollector;
	private Aida2WikiCateSysConverter aida2wikiCateConv;
	private WikiArticleCategoryRel wikiACR;
	private MongoEntityCategoryEmbeddingSearcher searcher;
	
	private TopicCategory cateTreeRoot;
	private int rootId;
	private double rootRel;
	private int cateTreeDepth;
	private Map<Integer, TopicCategory> cateTreeModel;
//	private Collection<String> rootedCateTitles;
	private Map<String, TopicCategory> rootedCateTitle2TCMap;
	private Map<Integer, Topic> cateArticleId2TopicMap;
	
	private double minCateCateRel;
	private double minTopicCateRel;
//	private double topic4CateEXP;
//	private double topicCateRelEXP;
	private double aidaWeightThresFactor;

	public TopicTopicCategoryDisambiguator(Wikipedia wikipedia,
			DisambiguationUtil disambiguator, String customizedCategoriesFilename) throws IOException {
		this.wikipedia = wikipedia;
		RelatednessCache rc = new RelatednessCache(disambiguator.getArticleComparer());
		this.model2TWeighter = new HashMap<DisambiguationModel, ITopicWeighter>();
		this.pageCollector = new WikiPageCollector(wikipedia, customizedCategoriesFilename);
		this.aida2wikiCateConv = new Aida2WikiCateSysConverter(wikipedia);
		this.wikiACR = new WikiArticleCategoryRel(wikipedia, rc);
		Property.setProperties("configs/MongoConfig_esa.properties");
		this.searcher = new MongoEntityCategoryEmbeddingSearcher(Language.EN);
		
		this.minCateCateRel = 0.01;//0.4 0.32 0.253//0.2
		this.minTopicCateRel = 0.005;//0.2 0.14 0.09//0.1
//		this.topicCateRelEXP = 2.5;//2.5
		this.aidaWeightThresFactor = 0.1;
		this.rootId = 0;
		this.rootRel = 0.0;
		this.cateTreeDepth = 1;
		this.rootedCateTitle2TCMap = new HashMap<String, TopicCategory>();
		this.cateArticleId2TopicMap = new HashMap<Integer, Topic>();
		this.cateTreeModel = buildCateTreeModelWithCateFromCateFile(rc, customizedCategoriesFilename, cateTreeDepth);
		//buildRootCateModelWithCateFromCateFile(customizedCategoriesFilename);
		//
	}
	
	public void setAidaWeightThresFactor(double factor){
		this.aidaWeightThresFactor = factor;
	}

	public double getMinCateCateRelatedness(){
		return this.minCateCateRel;
	}

	public double getMinTopicCateRelatedness(){
		return this.minTopicCateRel;
	}
	
	private Map<Integer, TopicCategory> buildRootCateModelWithCateFromCateFile(String cateFileName){
		Map<Integer, TopicCategory> id2TC = new HashMap<Integer, TopicCategory>();
		cateTreeRoot = new TopicCategory(wikipedia.getEnvironment(), rootId, id2TC.size());
		id2TC.put(rootId, cateTreeRoot);
		TopicCategory newTopicCategory;
		Category cate;
		////aida2wikiCateConv.getMappedWikiCateCol()
		for(String cateTitle: pageCollector.extractCateTitlesFromCateFile(cateFileName)){
			cate = wikipedia.getCategoryByTitle(cateTitle);
			if(cate==null||cate.getDepth()==null) continue;
			newTopicCategory = id2TC.get(cate.getId());
			if(newTopicCategory==null){
				newTopicCategory = new TopicCategory(wikipedia.getEnvironment(), cate.getId(), id2TC.size());
				newTopicCategory.setTCArticle(wikipedia.getArticleByTitle(cate.getTitle()));
				newTopicCategory.addParentRelatedness(cateTreeRoot, rootRel);
				cateTreeRoot.addChildRelatedness(newTopicCategory, rootRel);
				id2TC.put(cate.getId(), newTopicCategory);
				rootedCateTitle2TCMap.put(cateTitle, newTopicCategory);
			}
		}
		return id2TC;
	}
	
//	private void buildStandardCateTree(
//			Map<Integer, TopicCategory> id2TC, 
//			Map<String, TopicCategory> rootedCateTitle2TCMap){
//		
//			double rel;
//			boolean isParent;
//			hasParent = false;
//			for(TopicCategory parent: parents){
//				if(!parent.equals(cateTreeRoot)){
//					rel = getCateCateRelatedness(parent,newTopicCategory,rc);
//					isParent = (rel>=minCateCateRel);
//				}else{
//					rel = rootRel;
//					isParent = true;
//				}
//				if(isParent){
//					newTopicCategory.addParentRelatedness(parent, rel);
//					parent.addChildRelatedness(newTopicCategory, rel);
//					
//					hasParent = true;
//				}
//			}
//	}
	
	private Map<Integer, TopicCategory> buildCateTreeModelWithCateFromCateFile(
			RelatednessCache rc, String cateFileName, int depth){
		Map<Integer, TopicCategory> id2TC = new HashMap<Integer, TopicCategory>();
		Map<Integer, TopicCategory> id2UncheckedTC = new LinkedHashMap<Integer, TopicCategory>();
		cateTreeRoot = new TopicCategory(wikipedia.getEnvironment(), rootId, id2TC.size());
		id2TC.put(rootId, cateTreeRoot);
		TopicCategory curTC;
		HashSet<String> invalidCateTitle = new HashSet<String>();
		for(String cateTitle: pageCollector.extractCateTitlesFromCateFile(cateFileName)){//aida2wikiCateConv.getMappedWikiCateCol()//
			curTC = createTCWithParentAndAddInMaps(rc, cateTitle, cateTreeRoot, id2TC, id2UncheckedTC);
			if(curTC == null) {
				invalidCateTitle.add(cateTitle);
				continue;
			}
			rootedCateTitle2TCMap.put(cateTitle, curTC);
		}
		//TODO build cate article to cateTopic, used by aida2wiki category converting
		cateArticleId2TopicMap = buildCateArticlesWithCateTitle(invalidCateTitle);
		Iterator<TopicCategory> uncheckedTCIter;
		while(id2UncheckedTC.size()>0){
			uncheckedTCIter = id2UncheckedTC.values().iterator();
			if(!uncheckedTCIter.hasNext()) continue;
			curTC = uncheckedTCIter.next();
			uncheckedTCIter.remove();
			if(curTC.getTCDepth()>=1) continue;//getMaxTCDepth4curDepth(curTC.getDepth())
			//TODO also get child articles
			for(Category child: curTC.getChildCategories()){
				//subtract root categories
				if(rootedCateTitle2TCMap.containsKey(child.getTitle())) continue;
				createTCWithParentAndAddInMaps(rc, child.getTitle(), curTC, id2TC, id2UncheckedTC);
			}
		}
		System.out.println("total categories number: " + (id2TC.size()-1));
//		String filename = "results/gwifi_allcate.txt";
//		PrintWriter pw0;
//		try {
//			pw0 = new PrintWriter(createFileWithPW(filename));
//			for(TopicCategory tc: id2TC.values()){
//				pw0.println(tc.toString());
//			}
//			pw0.flush();
//			pw0.close();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
		return id2TC;
	}
	
	private Map<Integer, Topic> buildCateArticlesWithCateTitle(Set<String> cateTitles){
		HashMap<Integer, Topic> cateArticleId2Topic = new HashMap<Integer, Topic>();
		Article cateArticle;
		int cateArticleId;
		for(String cateTitle: cateTitles){
			cateArticle = wikipedia.getArticleByTitle(cateTitle);
			if(cateArticle == null) continue;
			cateArticleId = cateArticle.getId();
			Topic cateArticleTopic = new Topic(wikipedia, cateArticleId, 0.0, 0.0, 1.0);
			cateArticleId2Topic.put(cateArticleId, cateArticleTopic);
		}
		return cateArticleId2Topic;
	}
	
//	private static File createFileWithPW(String filename)
//			throws FileNotFoundException {
//		File file = new File(filename);
//		if (!file.exists()) {
//			file.getParentFile().mkdirs();
//		}
//		return file;
//	}
	
	private TopicCategory createTCWithParentAndAddInMaps(
			RelatednessCache rc, String tcTitle, TopicCategory parent, 
			Map<Integer, TopicCategory> id2TC, 
			Map<Integer, TopicCategory> id2UncheckedTC){
		if(!pageCollector.existCateTitle(tcTitle)) return null;
		if(!checkValidTCTitle(tcTitle)) return null;
		TopicCategory curTC = createTCWithParentByTitle(rc, tcTitle, id2TC, parent);
		if(curTC!=null){
			id2UncheckedTC.put(curTC.getId(), curTC);
		}
		return curTC;
	}
	
	private boolean checkValidTCTitle(String title){
//		if(title.startsWith("Categories by")) return false;
		if(title.endsWith("stubs")) return false;
		//TODO valid cate has main article
		if(pageCollector.findArticleOfCate(title)==null) return false;
		return true;
	}
	
	private Map<Integer, TopicCategory> genCateTree(
			Collection<TopicCategory> rootedTCList, 
			Map<String, TopicCategory> title2CateMap){
		Map<Integer, TopicCategory> id2TC = new HashMap<Integer, TopicCategory>();
		TopicCategory newTC;
		TopicCategory newChild;
		for(TopicCategory tc: cateTreeModel.values()){
			newTC = addNewTCInTree(tc, id2TC, rootedTCList, title2CateMap);
			for(TopicCategory child: tc.getChildCates()){
				newChild = addNewTCInTree(child, id2TC, rootedTCList, title2CateMap);
				double parentRel = child.getRelatednessOfParent(tc);
				newChild.addParentRelatedness(newTC, parentRel);
				double childRel = tc.getRelatednessOfChild(child);
				newTC.addChildRelatedness(newChild, childRel);
			}
		}
		for(TopicCategory tc:id2TC.values()){
			normParentRelOfTC(tc);
		}
		for(TopicCategory tc:id2TC.values()){
			normChildRelOfTC(tc);
		}
		id2TC.remove(rootId);
		System.out.println("standart categories number: "+id2TC.size());
		return id2TC;
	}
	
	private Map<Integer, TopicCategory> genCateTreeWithInitWeight(
			Collection<TopicCategory> rootedTCList, 
			Map<String, TopicCategory> title2CateMap,
			Map<String, Double> title2CateWeightMap){
		if(title2CateWeightMap == null || title2CateWeightMap.size()==0)
			return genCateTree(rootedTCList, title2CateMap);
		Map<Integer, TopicCategory> id2TC = new HashMap<Integer, TopicCategory>();
		Map<Integer, TopicCategory> id2UncheckedTC = new LinkedHashMap<Integer, TopicCategory>();
		TopicCategory originRootedTC;
		TopicCategory newRootedTC;
		double initTCWeight;
		int rootedTCDepth;
		for(String title: title2CateWeightMap.keySet()){
			initTCWeight = title2CateWeightMap.get(title);
			originRootedTC = rootedCateTitle2TCMap.get(title);
			newRootedTC = addNewTCInTreeWithInitWeight(originRootedTC, id2TC, rootedTCList, title2CateMap, id2UncheckedTC, true);
			newRootedTC.setWeight(initTCWeight);
			rootedTCDepth = 1;//getDepth4InitTCWeight(newRootedTC.getDepth(), initTCWeight);
			addChildTCsInTreeWithDepth(id2TC, title2CateMap, id2UncheckedTC, rootedTCDepth);
		}
		for(TopicCategory tc:id2TC.values()){
			normParentRelOfTC(tc);
		}
		for(TopicCategory tc:id2TC.values()){
			normChildRelOfTC(tc);
		}
		id2TC.remove(rootId);
		System.out.println("focused categories number: "+id2TC.size());
		return id2TC;
	}
	
	private int getMaxTCDepth4curDepth(int cateDepth){
		int depth = 0;
		if(cateDepth <= 4) {
			depth = 3;
		}else if(cateDepth <= 6) {
			depth = 2;
		}else if(cateDepth <= 8) {
			depth = 1;
		}else if(cateDepth <= 10){
			depth = 0;
		}else{
			depth = 0;
		}
		return 1;
	}
	
		//0-4,1-5		4
		//2-5,3-6,4-7	3
		//5-7,6-8,7-9	2
		//8-9,9-10		1

		//0-3,1-4		3
		//2-4,3-5,4-6	2
		//5-6,6-7,7-8	1
		//8-8,9-9		0
	private int getDepth4InitTCWeight(int cateDepth, double initTCWeight){
		int depth = 0;
		if(cateDepth <= 1) {
			depth = 3;
		}else if(cateDepth <= 4) {
			depth = 2;
		}else if(cateDepth <= 7) {
			depth = 1;
		}else if(cateDepth <= 9){
			depth = 0;
		}else{
			depth = 0;
		}
		//TODO
//		if(initTCWeight==0.0){
//			depth -= cateTreeDepth;
//		}
		if(depth<0){
			depth = 0;
		}
		return 1;
	}
	
	private void addChildTCsInTreeWithDepth(
			Map<Integer, TopicCategory> id2TC, 
			Map<String, TopicCategory> title2CateMap, 
			Map<Integer, TopicCategory> id2UncheckedTC, 
			int depth){
		Iterator<TopicCategory> uncheckedTCIter;
		TopicCategory newTC;
		TopicCategory originTC;
		TopicCategory newChild;
		while(id2UncheckedTC.size()>0){
			uncheckedTCIter = id2UncheckedTC.values().iterator();
			if(!uncheckedTCIter.hasNext()) continue;
			newTC = uncheckedTCIter.next();
			uncheckedTCIter.remove();
			if(newTC.getTCDepth()>=depth) continue;
			originTC = cateTreeModel.get(newTC.getId());
			for(TopicCategory originChild: originTC.getChildCates()){
				newChild = addNewTCInTreeWithInitWeight(originChild, id2TC, null, title2CateMap, id2UncheckedTC, false);
				double parentRel = originChild.getRelatednessOfParent(originTC);
				newChild.addParentRelatedness(newTC, parentRel);
				double childRel = originTC.getRelatednessOfChild(originChild);
				newTC.addChildRelatedness(newChild, childRel);
			}
		}
	}
	
	private TopicCategory addNewTCInTree(TopicCategory originTC, 
			Map<Integer, TopicCategory> id2TC, 
			Collection<TopicCategory> rootedTCList,
			Map<String, TopicCategory> title2TCMap){
		TopicCategory newTC = id2TC.get(originTC.getId());
		if(newTC==null){
			newTC = originTC.copyTC(false, true);
			if(rootedTCList!=null&&newTC.getTCDepth()==0){
				rootedTCList.add(newTC);
			}
			id2TC.put(newTC.getId(), newTC);
			title2TCMap.put(newTC.getTitle(), newTC);
		}
		return newTC;
	}
	
	private TopicCategory addNewTCInTreeWithInitWeight(
			TopicCategory originTC, 
			Map<Integer, TopicCategory> id2TC, 
			Collection<TopicCategory> rootedTCList,
			Map<String, TopicCategory> title2TCMap,
			Map<Integer, TopicCategory> id2UncheckedTC,
			boolean isRooted){
		TopicCategory newTC = id2TC.get(originTC.getId());
		if(newTC==null){
			newTC = originTC.copyTC(false, isRooted);
			if(rootedTCList!=null&&newTC.getTCDepth()==0){
				rootedTCList.add(newTC);
			}
			id2TC.put(newTC.getId(), newTC);
			title2TCMap.put(newTC.getTitle(), newTC);
			if(id2UncheckedTC!=null){
				id2UncheckedTC.put(newTC.getId(), newTC);
			}
		}else{
			if(isRooted && rootedTCList!=null){
				newTC.setTCDepth(0);
				rootedTCList.add(newTC);
			}
		}
		return newTC;
	}
	
	public void getWeightedTopicTopicCategory(Collection<Topic> topics,
			RelatednessCache rc, DisambiguationModel model,
			Collection<TopicCategory> categories,
			Collection<String> initCategoryTitles, float talpha, float calpha, float beta) {
		long start = System.currentTimeMillis();
		ITopicWeighter tWeighter = getTopicTopicCategoryWeighter(model);
		if(tWeighter instanceof PageRankAndHitsHubTopicTopicCategoryWeighter){
			if(beta != 0.0){
				HashMap<Integer, Topic> index2AllTopics = new HashMap<Integer, Topic>();				//labeled and non-labeled topics
				HashMap<Integer, Set<Topic>> id2CateArticleTopics = new HashMap<Integer, Set<Topic>>();	//keep the score of cate in Article represented by topics
				HashMap<Integer, Topic> index2NonLabelledTopic = new HashMap<Integer, Topic>();//topics without label, don't needed to be disambiguated
				Map<String, Set<Topic>> label2Topics = 
						genLabelledAndNonLabelledTopicsWithTopicsAndCateArticles(topics, index2NonLabelledTopic, index2AllTopics, id2CateArticleTopics);
				long labelTopicEnd = System.currentTimeMillis();
				System.out.println("Time for labeling topics: " + (labelTopicEnd - start) + " ms");

				((PageRankAndHitsHubTopicTopicCategoryWeighter) tWeighter).setAlpha(talpha, calpha, 0.0f);
				((PageRankAndHitsHubTopicTopicCategoryWeighter) tWeighter).getWeightedTopicTopicCategory(index2AllTopics.values(), label2Topics, index2NonLabelledTopic, new HashSet<TopicCategory>(), rc);
				long pagerankEnd = System.currentTimeMillis();
				System.out.println("Time for pagerank: " + (pagerankEnd - labelTopicEnd) + " ms");
				//filter topics
				filterTopics(index2AllTopics, index2NonLabelledTopic, label2Topics);
				
				Map<String, TopicCategory> title2CateMap = new HashMap<String, TopicCategory>(); // for topic cate rel
				//TODO train and initialize category weight
//				Map<String, Double> title2CateInitWeightMap = genInitWeightOfCatesForSource(new HashSet<String>(), null);//initCategoryTitles
				Map<Integer, TopicCategory> allCates = genCateTreeWithInitWeight(categories, title2CateMap, null);//title2CateInitWeightMap
				long genTreeEnd = System.currentTimeMillis();
				System.out.println("Time for generating category tree: " + (genTreeEnd - labelTopicEnd) + " ms");
				
				Map<Topic, Map<TopicCategory, Double>> topic2ParentCateRelsMap = new HashMap<Topic, Map<TopicCategory, Double>>();
				relateTopicCategoriesWithTopics(rc, index2AllTopics.values(), allCates, topic2ParentCateRelsMap, title2CateMap);
//				relateT2TC(index2AllTopics.values(), allCates, topic2ParentCateRelsMap, title2CateMap);
				
				long relateEnd = System.currentTimeMillis();
				System.out.println("Time for relating topic and category: " + (relateEnd - genTreeEnd) + " ms");
				
				((PageRankAndHitsHubTopicTopicCategoryWeighter) tWeighter).setAlpha(talpha, calpha, beta);
				((PageRankAndHitsHubTopicTopicCategoryWeighter) tWeighter).getWeightedTopicTopicCategory(index2AllTopics.values(), label2Topics, index2NonLabelledTopic, allCates.values(), rc);
				
				//TODO without cate tree in graph
//				updateParentTCWeights(allCates.values());
				
				//TODO choose output categories
//				revertWiki2AidaTCs(categories); //index2CateArticle
				
				normalizeTCWeights(categories);
	//			normalizeTWeights(topics);
				
			}else{
				Map<Integer, TopicCategory> allCates = new HashMap<Integer, TopicCategory>();
				HashMap<Integer, Topic> index2NonLabelledTopic = new HashMap<Integer, Topic>();
				HashMap<String, Set<Topic>> label2Topics = new HashMap<String, Set<Topic>>();
				genLabelledTopicsWithTopics(topics, label2Topics);
				((PageRankAndHitsHubTopicTopicCategoryWeighter) tWeighter).setAlpha(talpha, calpha, beta);
				((PageRankAndHitsHubTopicTopicCategoryWeighter) tWeighter).getWeightedTopicTopicCategory(topics, label2Topics, index2NonLabelledTopic, allCates.values(), rc);
			}
			
		}else{
			tWeighter.getWeightedTopics(topics, rc);
		}
		long end = System.currentTimeMillis();
		//System.out.println("Time for topic disambiguation: " + (end - start) + " ms");
	}
	
	private ITopicWeighter getTopicTopicCategoryWeighter(
			DisambiguationModel model) {
		ITopicWeighter ttcWeighter = model2TWeighter.get(model);
		if (ttcWeighter == null) {
			if(model.equals(DisambiguationModel.PRIOR))
			ttcWeighter = new DummyTopicWeighter();
			else if (model.equals(DisambiguationModel.PAGERANK))
			ttcWeighter = new PageRankTopicWeighter();
			else if (model.equals(DisambiguationModel.HITSHUB))
			ttcWeighter = new HitsHubTopicWeighter();
			else if (model.equals(DisambiguationModel.PAGERANK_HITSHUB))
			ttcWeighter = new PageRankAndHitsHubTopicTopicCategoryWeighter();
			model2TWeighter.put(model, ttcWeighter);
		}
		return ttcWeighter;
	}
	
	private void revertWiki2AidaTCs(Collection<TopicCategory> tcCol){
		HashMap<String, Double> wikiCateWithScore = new HashMap<String, Double>();
		for(TopicCategory wikiTC: tcCol){
			wikiCateWithScore.put(wikiTC.getTitle(), wikiTC.getWeight());
		}
		HashMap<String,Double> aidaCateWithScore = aida2wikiCateConv.revertWikiToAidaTitleWithScore(wikiCateWithScore);
		tcCol.clear();
		Double maxWeight = 0.0;
		Double aidaWeight;
		for(String aidaCate: aidaCateWithScore.keySet()){
			aidaWeight = aidaCateWithScore.get(aidaCate);
			if(maxWeight < aidaWeight) maxWeight = aidaWeight;
		}
		HashMap<String,Double> filteredAidaCateWithScore = new HashMap<String, Double>();
		for(String aidaCate: aidaCateWithScore.keySet()){
			aidaWeight = aidaCateWithScore.get(aidaCate)/maxWeight;
			if(aidaWeight < aidaWeightThresFactor) continue;
			filteredAidaCateWithScore.put(aidaCate, aidaWeight);
		}
		Collection<String> hierAidaCateCol = aida2wikiCateConv.getHierAidaTitleCol(filteredAidaCateWithScore.keySet());
		for(String aidaCate: hierAidaCateCol){
			int aidaID = aida2wikiCateConv.getAidaSysIDByTitle(aidaCate);
			TopicCategory aidaTC = new TopicCategory(wikipedia.getEnvironment(), aidaID, aidaCate, 1.0);
			tcCol.add(aidaTC);
		}
	}
	
	private Map<String, Set<Topic>> genLabelledAndNonLabelledTopicsWithTopicsAndCateArticles(
			Collection<Topic> topics, HashMap<Integer,Topic> index2NonLabelledTopic, 
			HashMap<Integer, Topic> index2AllTopics, HashMap<Integer, Set<Topic>> id2CateArticleTopics){
		genNonLabelledTopicsWithCateArticles(topics, index2NonLabelledTopic, index2AllTopics, id2CateArticleTopics);
		HashMap<String, Set<Topic>> label2Topics = new HashMap<String, Set<Topic>>();
		genLabelledTopicsWithTopics(topics, label2Topics);
		return label2Topics;
	}
	
	private void genLabelledTopicsWithTopics(Collection<Topic> topics, HashMap<String, Set<Topic>> label2Topics){
		String label;
		Set<Topic> labelledTopics;
		for(Topic topic: topics){
			label = topic.getReferences().get(0).getLabel().getText();
			labelledTopics = label2Topics.get(label);
			if(labelledTopics == null){
				labelledTopics = new HashSet<Topic>();
				label2Topics.put(label, labelledTopics);
			}
			labelledTopics.add(topic);
		}
	}
	
	private void genNonLabelledTopicsWithCateArticles(Collection<Topic> topics, HashMap<Integer,Topic> index2NonLabelledTopic, 
			HashMap<Integer, Topic> index2AllTopics, HashMap<Integer, Set<Topic>> id2CateArticleTopics){
		Map<Integer, Set<Topic>> id2Topics = new HashMap<Integer, Set<Topic>>();
		for(Topic topic: topics){
			index2AllTopics.put(topic.getIndex(), topic);
			Set<Topic> topicSet = id2Topics.get(topic.getId());
			if(topicSet == null){
				topicSet = new HashSet<Topic>();
				id2Topics.put(topic.getId(), topicSet);
			}
			topicSet.add(topic);
		}
		int cateArticleIndex = -1;
		for(Integer cateArticleId: cateArticleId2TopicMap.keySet()){
			Set<Topic> topicSet = id2Topics.get(cateArticleId);
			if(topicSet == null) {
				Topic cateArticle = copyCateArticle(cateArticleId2TopicMap.get(cateArticleId));
				cateArticle.setIndex(cateArticleIndex);
				index2NonLabelledTopic.put(cateArticleIndex, cateArticle);
				index2AllTopics.put(cateArticleIndex, cateArticle);
				cateArticleIndex--;
				topicSet = new HashSet<Topic>();
				topicSet.add(cateArticle);
			}
			id2CateArticleTopics.put(cateArticleId, topicSet);
		}
	}
	
	private Topic copyCateArticle(Topic cateArticle){
		int cateArticleId = cateArticle.getId();
		double relToContext = cateArticle.getRelatednessToContext();
		double commonness = cateArticle.getCommenness();
		double docLen = 1.0;
		Topic newCateArticle = new Topic(wikipedia, cateArticleId, relToContext, commonness, docLen);
		return newCateArticle;
	}
	
	private void filterTopics(Map<Integer, Topic> index2AllTopics, 
			Map<Integer, Topic> index2NonLabelledTopic,
			Map<String, Set<Topic>> label2Topics){
		double topicWeightThres = 0.3;
		//filter labelled topics
		double minWeight = 1.0;
		double labelMinWeight = 0.0;
		Set<String> removedLabel = new HashSet<String>();
		Set<Topic> labelledTopics;
		for(String label: label2Topics.keySet()){
			labelledTopics = label2Topics.get(label);
			labelMinWeight = filterLabelledTopics(labelledTopics, index2AllTopics, topicWeightThres);
			if(labelMinWeight == 0.0) removedLabel.add(label);
			else if(labelMinWeight<minWeight) minWeight = labelMinWeight;
		}
		for(String rmLabel: removedLabel){
			label2Topics.remove(rmLabel);
		}
		//print label->topics
//		for(String label: label2Topics.keySet()){
//			System.out.println(label+" :");
//			for(Topic topic: label2Topics.get(label)){
//				System.out.println(topic.toString());
//			}
//		}
		//filter non-labelled topics with min. weight from above
		Map<Integer, Topic> nonLabelledTopicMap = new HashMap<Integer, Topic>();
		Topic nonLabelledTopic;
		for(Integer index: index2NonLabelledTopic.keySet()){
			nonLabelledTopic = index2NonLabelledTopic.get(index);
			if(nonLabelledTopic.getWeight()>=minWeight){
				nonLabelledTopicMap.put(index, nonLabelledTopic);
			}else{
				nonLabelledTopic.setWeight(0.0);
				index2AllTopics.remove(index);
			}
		}
		index2NonLabelledTopic.clear();
		index2NonLabelledTopic.putAll(nonLabelledTopicMap);
	}
	
	private double filterLabelledTopics(Set<Topic> labelledTopics, Map<Integer, Topic> index2AllTopics, double topicWeightThres){
		double maxTopicWeight = 0.0;
		double topicWeight = 0.0;
		for(Topic topic: labelledTopics){
			topicWeight = topic.getWeight();
			if(topicWeight>maxTopicWeight) maxTopicWeight = topicWeight;
		}
		if(maxTopicWeight<=0.005){
			//TODO ignore this label or take all topics in this label
			for(Topic topic: labelledTopics){
				topic.setWeight(0.0);
				index2AllTopics.remove(topic.getIndex());
			}
			labelledTopics.clear();
			return 0.0;
		}
		Set<Topic> weightedTopics = new HashSet<Topic>();
		double minWeight = 1.0;
		double relativeTopicWeight = 0.0;
		for(Topic topic: labelledTopics){
			topicWeight = topic.getWeight();
			relativeTopicWeight = topicWeight/maxTopicWeight;
			if(relativeTopicWeight<topicWeightThres){
				topic.setWeight(0.0);
				index2AllTopics.remove(topic.getIndex());
			}else{
				weightedTopics.add(topic);
				if(topicWeight<minWeight) minWeight = topicWeight;
			}
		}
		labelledTopics.clear();
		labelledTopics.addAll(weightedTopics);
		return minWeight;
	}
	
	private void relateT2TC(Collection<Topic> topics, Map<Integer, TopicCategory> id2CateMap, 
			Map<Topic, Map<TopicCategory, Double>> topic2ParentCateRelsMap, 
			Map<String, TopicCategory> title2CateMap){
		Set<Category> tarCates = new HashSet<Category>();
		for(TopicCategory tc: id2CateMap.values()){
			tarCates.add(tc);
		}
		Map<Category, Double> tarCateRelatedness;
		Map<TopicCategory, Double> parentCate2Rel;
		TopicCategory tarTC;
		for (Topic topic : topics) {
			if(topic==null||!topic.exists()){
				System.out.println("null topic!");
				continue;
			}
			tarCateRelatedness = wikiACR.getTarCateRelatedness(topic, tarCates);
			parentCate2Rel = new HashMap<TopicCategory, Double>();
			for(Category tarCate: tarCateRelatedness.keySet()){
				tarTC = title2CateMap.get(tarCate.getTitle());
				parentCate2Rel.put(tarTC, tarCateRelatedness.get(tarCate));
			}
			//the orientation of the topic in category space
			orientTopicCateRel(parentCate2Rel, topic);
			topic2ParentCateRelsMap.put(topic, parentCate2Rel);
		}
	}

	private void relateTopicCategoriesWithTopics(
			RelatednessCache rc, Collection<Topic> topics, Map<Integer, TopicCategory> id2CateMap, 
			Map<Topic, Map<TopicCategory, Double>> topic2ParentCateRelsMap, 
			Map<String, TopicCategory> title2CateMap) {
		boolean isCateFound = false;
		Category[] parentCates;
		for (Topic topic : topics) {
			// topic and category are the same
			TopicCategory sameTitleCate = title2CateMap.get(topic.getTitle());
			if (sameTitleCate!=null) {
				if(sameTitleCate.addChildTopic(topic)){
					sameTitleCate.addTopicRelatedness(topic, 1.0);
				}
				continue;
			}
			Article topicArticle = wikipedia.getArticleByTitle(topic.getTitle());
			if(topicArticle == null)
				continue;
			isCateFound = false;
			Map<TopicCategory, Double> parentCate2Rel = new HashMap<TopicCategory, Double>();
			topic2ParentCateRelsMap.put(topic, parentCate2Rel);
			// category is parent of topic
			parentCates = topic.getParentCategories();
			for (Category parent: parentCates){
				TopicCategory parentTC = id2CateMap.get(parent.getId());
				if(parentTC!=null){
					Article cateArticle = parentTC.getTCArticle();
					double rel = getTopicCateRelatedness(topic, parentCates, cateArticle, rc);
					//TODO embedding
//					double rel = searcher.getRelatedness(topic.getTitle(), false, parent.getTitle(), true);
//					if(rel<0.0) rel = getTopicCateRelatedness(topic, parentCates, parentTC.getTCArticle(), rc);
					if(rel < minTopicCateRel)
						continue;
//						rel= Math.pow(rel, topic2CateEXP);
//						if(!topicCate.getChildTopics().contains(topic)){
//							topicCate.getChildTopics().add(topic);
//							topicCate.addTopicRelatedness(topic, rel);
					parentCate2Rel.put(parentTC, rel);
//						}
					isCateFound = true;
				}
			}
			if (!isCateFound){
				// topic is under all the categories
				for (TopicCategory topicCate : id2CateMap.values()) {
					if(topicCate.getChildCates().size()>0)
						continue;
					//TODO get relatedness to child category and also to child article
//					Map<Category, Double> tempLeafChildRelMap = new HashMap<Category, Double>();
//					for(Category leafChild :topicCate.getChildCategories()){
//						double leafChildRel = getCateCateRelatedness(topicCate, leafChild, rc);
//						if(leafChildRel>=minCateCateRel)
//							tempLeafChildRelMap.put(leafChild, leafChildRel);
//					}
//					normalizeParentRelatednessInMap(tempLeafChildRelMap);
//					double totalTopicLeafRel = 0.0;
//					for(Category leafChild :tempLeafChildRelMap.keySet()){
//						double topicLeafChildRel = getTopicCateRelatedness(topic, leafChild, rc);
//						if(topicLeafChildRel>=minTopicCateRel){
//							totalTopicLeafRel += topicLeafChildRel*tempLeafChildRelMap.get(leafChild);
//						}
//					}
					double topicLeafRel = getTopicCateRelatedness(topic, null, topicCate.getTCArticle(), rc);
					//TODO embedding
//					double topicLeafRel = searcher.getRelatedness(topic.getTitle(), false, topicCate.getTitle(), true);
//					if(topicLeafRel<0.0) 
//						topicLeafRel = getTopicCateRelatedness(topic, null, topicCate.getTCArticle(), rc);
//					if(topicLeafRel < totalTopicLeafRel)
//						topicLeafRel = totalTopicLeafRel;
					if (topicLeafRel >= minTopicCateRel) {
						parentCate2Rel.put(topicCate, topicLeafRel);
					}
				}
			}
			//the orientation of the topic in category space
			orientTopicCateRel(parentCate2Rel, topic);
		}
	}
	
	private void orientTopicCateRel(Map<TopicCategory, Double> parentCate2Rel, Topic topic){
		double relFactor = 1 - entropy(parentCate2Rel.values());
//		relFactor = relFactor>0.1 ? Math.pow(relFactor, 0.5) : 0.0;
//		relFactor = relFactor>0.1? relFactor : 0.0;
//		relFactor = relFactor>0.1? 1.0 : Math.pow(relFactor, 2);
//		relFactor = relFactor>0.1? 1.0 : relFactor;
//		relFactor = relFactor>0.1? Math.pow(relFactor, 0.5) : Math.pow(relFactor, 2);
//		relFactor = relFactor>0.1? 1.0 : 0.0;
		
		double parentCateNum = parentCate2Rel.size();
		double relExp = Math.pow(parentCateNum, 0.5);
		double totalTopicCateRel = 0.0;
		double maxTopicCateRel = 0.0;
		String maxTC = "";
		double rel = 0.0;
		for(TopicCategory tc: parentCate2Rel.keySet()){
			rel = parentCate2Rel.get(tc);
			rel = Math.pow(rel, relExp);
			totalTopicCateRel += rel;
//			if(rel > maxTopicCateRel)
//				maxTopicCateRel = rel;
			parentCate2Rel.put(tc, rel);
		}

		if(totalTopicCateRel<=0.0)
			totalTopicCateRel = 1.0;
//		if(maxTopicCateRel<=0.0)
//			maxTopicCateRel = 1.0;
		
		for(TopicCategory tc: parentCate2Rel.keySet()){
			rel = relFactor * parentCate2Rel.get(tc)/totalTopicCateRel;//maxTopicCateRel;
//			if(rel > maxTopicCateRel){
//				maxTopicCateRel = rel;
//				maxTC = tc.getTitle();
//			}
			tc.addTopicRelatedness(topic, rel);
			parentCate2Rel.put(tc, rel);
		}
//		maxTopicCateRel = maxTopicCateRel/relFactor;
//		if(maxTopicCateRel<0.5){
//			relFactor = 0.0;
//		}
	}
	
	private static double entropy(Collection<Double> eles) {
		double totalEle = 0.0;
		int eleNum = 0;
		for(Double ele:eles){
			if(ele<0.0) continue;
			eleNum ++;
			totalEle += ele;
		}
		if(totalEle == 0.0) return 1.0;
		if(eleNum == 1.0) return 0.0;
		double entropy = 0.0;
		double relEle;
		for(Double ele:eles){
			if(ele<=0.0) continue;
			relEle = ele/totalEle;
			entropy -= relEle * Math.log(relEle)/Math.log(eleNum);
		}
		return entropy;
	}

	private double getTopicCateRelatedness(Topic topic, Category[] topicParentCate, Article cateArticle, RelatednessCache rc) { 
//		Article topicArticle = wikipedia.getArticleByTitle(topic.getTitle());
//		if(topicArticle==null){
//			return 0.0;
//		}
//		String cateTitle = wikipedia.getCategoryById(cate.getId()).getTitle();
//		Article cateArticle = wikipedia.getArticleByTitle(cateTitle);
		if (cateArticle == null) {
			if(topicParentCate==null){
				return 0.0;
			}else{
				return 1/(double)(topicParentCate.length);
			}
//				topicParentCate = topicArticle.getParentCategories();
//			for(Category parentCate:topicParentCate){
//				if(parentCate.equals(cate)){
//					return 1/(double)(topicParentCate.length);
//				}
//			}
//			return 0.0;
		} else {
			return rc.getRelatedness(topic, cateArticle);
		}
	}

	private double getCateCateRelatedness(Category parent, Category child, RelatednessCache rc) {
		//TODO embedding
//		double rel = searcher.getRelatedness(parent.getTitle(), true, child.getTitle(), true);
//		if(rel>=0.0) return rel;
		
		String parentTitle = wikipedia.getCategoryById(parent.getId()).getTitle();
		Article parentArticle = wikipedia.getArticleByTitle(parentTitle);
		String childTitle = wikipedia.getCategoryById(child.getId()).getTitle();
		Article childArticle = wikipedia.getArticleByTitle(childTitle);
		if (parentArticle == null || childArticle == null) {
			Category[] parentCates = child.getParentCategories();
			for(Category parentCate:parentCates){
				if(parentCate.equals(parent)){
					return 1/(double)(parentCates.length);
				}
			}
			return 0.0;
		} else {
			return rc.getRelatedness(parentArticle, childArticle);
		}
	}

	public Collection<TopicCategory> genTopicCategories(RelatednessCache rc, List<String> cateTitleList) {
		if(cateTitleList==null || cateTitleList.size()==0){
			return genDefaultTopicCategories(rc);
		} else {
			return genTopicCategoriesWithTitle(rc, cateTitleList);
		}
	}
	
	private Collection<TopicCategory> genTopicCategoriesWithTitle(RelatednessCache rc, List<String> cateTitleList){
		Map<Integer, TopicCategory> topicCatgoryList = new HashMap<Integer, TopicCategory>();
		for(String cateName: cateTitleList){
			createTCWithParentsByTitle(rc, cateName, topicCatgoryList, null);
		}
		return topicCatgoryList.values();
	}

	private Collection<TopicCategory> genDefaultTopicCategories(RelatednessCache rc) {
		Map<Integer, TopicCategory> topicCatgoryList = new HashMap<Integer, TopicCategory>();
		createTCWithParentsByTitle(rc, TopicCategory.TC_ARTS, topicCatgoryList, null);
		createTCWithParentsByTitle(rc, TopicCategory.TC_CULTURE, topicCatgoryList, null);
		createTCWithParentsByTitle(rc, TopicCategory.TC_GEOGRAPHY, topicCatgoryList, null);
		createTCWithParentsByTitle(rc, TopicCategory.TC_HEALTH, topicCatgoryList, null);
		createTCWithParentsByTitle(rc, TopicCategory.TC_HISTORY, topicCatgoryList, null);
		createTCWithParentsByTitle(rc, TopicCategory.TC_MATHEMATICS, topicCatgoryList, null);
		createTCWithParentsByTitle(rc, TopicCategory.TC_NATURE, topicCatgoryList, null);
		createTCWithParentsByTitle(rc, TopicCategory.TC_PEOPLE, topicCatgoryList, null);
		createTCWithParentsByTitle(rc, TopicCategory.TC_PHILOSOPHY, topicCatgoryList, null);
		createTCWithParentsByTitle(rc, TopicCategory.TC_RELIGION, topicCatgoryList, null);
		createTCWithParentsByTitle(rc, TopicCategory.TC_SOCIETY, topicCatgoryList, null);
		createTCWithParentsByTitle(rc, TopicCategory.TC_SPORTS, topicCatgoryList, null);
		createTCWithParentsByTitle(rc, TopicCategory.TC_TECHNOLOGY, topicCatgoryList, null);
		createTCWithParentsByTitle(rc, TopicCategory.TC_PSYCHOLOGY, topicCatgoryList, null);

		createTCWithParentByTitle(rc, TopicCategory.TC_PLACES, topicCatgoryList, topicCatgoryList.get(2));
		createTCWithParentByTitle(rc, TopicCategory.TC_MEDICINE, topicCatgoryList, topicCatgoryList.get(3));
		createTCWithParentByTitle(rc, TopicCategory.TC_FOOD_AND_DRINK, topicCatgoryList, topicCatgoryList.get(3));
		createTCWithParentByTitle(rc, TopicCategory.TC_EVENTS, topicCatgoryList, topicCatgoryList.get(4));
		createTCWithParentByTitle(rc, TopicCategory.TC_STATISTICS, topicCatgoryList, topicCatgoryList.get(5));
		createTCWithParentByTitle(rc, TopicCategory.TC_PHYSICAL_SCIENCES, topicCatgoryList, topicCatgoryList.get(6));
		createTCWithParentByTitle(rc, TopicCategory.TC_BIOLOGY, topicCatgoryList, topicCatgoryList.get(6));
//		createTCToParentByTitle(rc, TopicCategory.TC_BIOGRAPHY, topicCatgoryList, topicCatgoryList.get(7));
		createTCWithParentByTitle(rc, TopicCategory.TC_LOGIC, topicCatgoryList, topicCatgoryList.get(8));
		createTCWithParentByTitle(rc, TopicCategory.TC_BUSINESS, topicCatgoryList, topicCatgoryList.get(10));
		createTCWithParentByTitle(rc, TopicCategory.TC_FINANCE, topicCatgoryList, topicCatgoryList.get(10));
		createTCWithParentByTitle(rc, TopicCategory.TC_LAW, topicCatgoryList, topicCatgoryList.get(10));
		createTCWithParentByTitle(rc, TopicCategory.TC_POLITICS, topicCatgoryList, topicCatgoryList.get(10));
		createTCWithParentByTitle(rc, TopicCategory.TC_WAR, topicCatgoryList, topicCatgoryList.get(10));
		createTCWithParentByTitle(rc, TopicCategory.TC_EDUCATION, topicCatgoryList, topicCatgoryList.get(10));
		createTCWithParentByTitle(rc, TopicCategory.TC_ORGANIZATIONS, topicCatgoryList, topicCatgoryList.get(10));
		createTCWithParentByTitle(rc, TopicCategory.TC_COMPUTING, topicCatgoryList, topicCatgoryList.get(12));
		createTCWithParentByTitle(rc, TopicCategory.TC_ENGINEERING, topicCatgoryList, topicCatgoryList.get(12));
		createTCWithParentByTitle(rc, TopicCategory.TC_TRANSPORT, topicCatgoryList, topicCatgoryList.get(12));
		createTCWithParentByTitle(rc, TopicCategory.TC_INDUSTRY, topicCatgoryList, topicCatgoryList.get(12));
		createTCWithParentByTitle(rc, TopicCategory.TC_ARCHITECTURE, topicCatgoryList, topicCatgoryList.get(12));
		
		return topicCatgoryList.values();
	}
	
	private HashSet<String> buildCateTitleList(){
		HashSet<String> cateTitleList = new HashSet<String>();
		cateTitleList.add(TopicCategory.TC_SPORTS);
//		cateTitleList.add(TopicCategory.TC_SPORTS);
		return cateTitleList;
	}
	
	private void chooseOutputCates(Map<Integer, TopicCategory> allCates, Collection<TopicCategory> outputCates){
		outputCates.clear();
		String cateTitle;
		for(Integer cateIndex: allCates.keySet()){
			
		}
	}
	
	private TopicCategory createTCWithParentsByTitle(
			RelatednessCache rc, String title, 
			Map<Integer, TopicCategory> id2TC, 
			Collection<TopicCategory> parents){
		Category cate = wikipedia.getCategoryByTitle(title);
		if(cate==null){
			return null;
		}
		boolean isNewTC = false;
		TopicCategory newTopicCategory = id2TC.get(cate.getId());
		if(newTopicCategory==null){
			newTopicCategory = new TopicCategory(wikipedia.getEnvironment(), cate.getId(), id2TC.size());
			id2TC.put(cate.getId(), newTopicCategory);
			isNewTC = true;
		}
		boolean hasParent = true;
		if(parents!=null){
			double rel;
			boolean isParent;
			hasParent = false;
			for(TopicCategory parent: parents){
				if(!parent.equals(cateTreeRoot)){
					rel = getCateCateRelatedness(parent,newTopicCategory,rc);
					isParent = (rel>=minCateCateRel);
				}else{
					rel = rootRel;
					isParent = true;
				}
				if(isParent){
					newTopicCategory.addParentRelatedness(parent, rel);
					parent.addChildRelatedness(newTopicCategory, rel);
					
					hasParent = true;
				}
			}
		}
		if(isNewTC && hasParent){
			Article cateArticle = wikipedia.getArticleByTitle(cate.getTitle());
			newTopicCategory.setTCArticle(cateArticle);
			return newTopicCategory;
		}else{
			return null;
		}
	}
	
	private TopicCategory createTCWithParentByTitle(
			RelatednessCache rc, String title, 
			Map<Integer, TopicCategory> id2TC, TopicCategory parent){
		if(parent == null){
			return createTCWithParentsByTitle(rc, title, id2TC, null);
		}
		List<TopicCategory> parentList = new ArrayList<TopicCategory>();
		parentList.add(parent);
		return createTCWithParentsByTitle(rc, title, id2TC, parentList);
	}

	private int normalizeTCWeights(Collection<TopicCategory> categories) {
		double sumWeight = 0.0;
		double maxWeight = 0.0;
		for (TopicCategory cate : categories) {
			sumWeight += cate.getWeight();
			if(cate.getWeight()>maxWeight)
				maxWeight = cate.getWeight();
		}
		if(sumWeight==0.0)
			sumWeight = 1.0;
		if(maxWeight == 0.0)
			maxWeight=1.0;
		double normalizedWeight = 0.0;
		for (TopicCategory cate : categories) {
			//TODO may remove extreme low weighted cate before normalizing
//			if(cate.getWeight()<0.001) cate.setWeight(0.0);
			normalizedWeight = cate.getWeight() / maxWeight;//sumWeight;
//			normalizedWeight = Math.sqrt(normalizedWeight);
			cate.setWeight(normalizedWeight);
		}
		return 0;
	}
	
	private int normalizeTWeights(Collection<Topic> topics) {
		double sumWeight = 0.0;
		for (Topic topic : topics) {
				sumWeight += topic.getWeight();
		}
		if(sumWeight==0.0)
			sumWeight = 1.0;
		double normalizedWeight = 0.0;
		for (Topic topic : topics) {
			normalizedWeight = topic.getWeight() / sumWeight;
//			normalizedWeight = Math.sqrt(normalizedWeight);
			topic.setWeight(normalizedWeight);
		}
		return 0;
	}
	
	public int updateParentTCWeights(Collection<TopicCategory> categories) {
		List<TopicCategory> allCates = new ArrayList<TopicCategory>();
		allCates.addAll(categories);
		while (allCates.size() > 0) {
			allCates = mergeChildrenToParents(allCates);
		}
		return 0;
	}

	private List<TopicCategory> mergeChildrenToParents(
			Collection<TopicCategory> categories) {
		int mergedNum = 0;
		List<TopicCategory> unmergedCates = new ArrayList<TopicCategory>();
		for (TopicCategory cate : categories) {
			if (cate.getChildCates().size() == 0) {
				mergedNum += mergeChildToParents(cate, categories);
			} else {
				unmergedCates.add(cate);
			}
		}
		if (mergedNum == 0) {
			unmergedCates = new ArrayList<TopicCategory>();
		}
		return unmergedCates;
	}

	private int mergeChildToParents(TopicCategory child,
			Collection<TopicCategory> cates) {
		int parentNum = 0;
		double childWeight = child.getWeight();
		Map<TopicCategory, Double> parentRelatedness = child
				.getParentRelatednessMap();
		for (TopicCategory cate : cates) {
			if (cate.isParentOf(child)) {
				double rel = parentRelatedness.get(cate);
				cate.setWeight(cate.getWeight() + childWeight * rel);
				// TODO recovery the child cate list
				cate.removeChildCate(child);
				parentNum++;
			}
		}
		return parentNum;
	}
	
	private void normParentRelOfTC(TopicCategory child) {
		double totalWeight = 0.0;
		double tempWeight = 0.0;
		Map<TopicCategory, Double> parentRelatedness = child
				.getParentRelatednessMap();
		for (TopicCategory parent : child.getParentCates()) {
			tempWeight = parentRelatedness.get(parent);
			totalWeight += tempWeight;
		}
		if(totalWeight==0.0){
			totalWeight = 1.0;
		}
		double normalizedWeight = 0.0;
		for (TopicCategory parent : child.getParentCates()) {
			normalizedWeight = parentRelatedness.get(parent) / totalWeight;
			parentRelatedness.put(parent, normalizedWeight);
		}
	}
	
	private void normChildRelOfTC(TopicCategory parent) {
		double totalWeight = 0.0;
		Map<TopicCategory, Double> childRelatedness = parent
				.getChildRelatednessMap();
		for (TopicCategory child : parent.getChildCates()) {
			totalWeight += childRelatedness.get(child);
		}
		if(totalWeight==0.0){
			totalWeight = 1.0;
		}
		double normalizedWeight = 0.0;
		double childRel = 0.0;
		for (TopicCategory child : parent.getChildCates()) {
			childRel = childRelatedness.get(child);
			normalizedWeight = childRel / totalWeight;
			childRelatedness.put(child, normalizedWeight);
		}
	}

	private void normParentRelInMap(Map<Category, Double> parentRelatednessMap){
		double totalWeight = 0.0;
		double tempWeight = 0.0;
		for (Category parent : parentRelatednessMap.keySet()) {
			tempWeight = parentRelatednessMap.get(parent);
			totalWeight += tempWeight;
		}
		if(totalWeight==0.0){
			totalWeight = 1.0;
		}
		double normalizedWeight = 0.0;
		for (Category parent : parentRelatednessMap.keySet()) {
			normalizedWeight = parentRelatednessMap.get(parent) / totalWeight;
			parentRelatednessMap.put(parent, normalizedWeight);
		}
	}
	
	public List<Category> getFullParentCatesOfTopic(Topic topic) {
		List<Category> topCategories = new ArrayList<Category>();
		for (Category cate : topic.getParentCategories()) {
			topCategories.add(cate);
		}
		pageCollector.getFullParentCatesOfCates(topCategories);
		return topCategories;
	}
	
	public ArrayList<TopicCategory> getFullChildCatesOfTCs(
			RelatednessCache rc, Collection<TopicCategory> parentCates, int startIndex) {
		int childCateIndex = startIndex;
		ArrayList<TopicCategory> fullChildCateList = new ArrayList<TopicCategory>();
		TopicCategory tempChild;
		for (TopicCategory parent : parentCates) {
			for (Category category : parent.getChildCategories()) {
				tempChild = findTCInTCListById(fullChildCateList,
						category.getId());
				if (tempChild == null) {
					tempChild = new TopicCategory(wikipedia.getEnvironment(),
							category.getId(), childCateIndex);
					childCateIndex++;
					double rel = getCateCateRelatedness(parent, tempChild, rc);
					if (rel > 0.0) {
						parent.addChildRelatedness(tempChild, rel);
						tempChild.addParentRelatedness(parent, rel);
						fullChildCateList.add(tempChild);
					}
				} else {
					double rel = getCateCateRelatedness(parent, tempChild, rc);
					if (rel > 0.0) {
						parent.addChildRelatedness(tempChild, rel);
						tempChild.addParentRelatedness(parent, rel);
					}
				}
			}
		}
		for (TopicCategory child : fullChildCateList) {
			normParentRelOfTC(child);
		}
		return fullChildCateList;
	}
	
	private TopicCategory findTCInTCListById(
			List<TopicCategory> cateList, int cateId) {
		for (TopicCategory cate : cateList) {
			if (cate.getId() == cateId) {
				return cate;
			}
		}
		return null;
	}
	
	public Map<Integer,String> getFullChildArticlesOfTC(TopicCategory rootCate){
		return pageCollector.getFullChildArticlesOfCateWithTitle(rootCate.getTitle(), -1);
	}
	
	public Map<String, Double> genInitWeightOfCatesForSource(Collection<String> cateTitleList, String source){
		Map<String, Double> title2CateWeight = new LinkedHashMap<String, Double>();
//		cateTitleList = new HashSet<String>();
//		cateTitleList.add(TopicCategory.TC_SOCIETY);
		if(cateTitleList.size() > 0){
			//TODO initialize the weight of categories
			// in descend order
			int cateNum = cateTitleList.size();
			double initCateWeight = 1/((double)cateNum);
			for(String cateTitle: cateTitleList){
				title2CateWeight.put(cateTitle, initCateWeight);
			}
		}
			for(String cateTitle: rootedCateTitle2TCMap.keySet()){
				if(!title2CateWeight.containsKey(cateTitle))
					title2CateWeight.put(cateTitle, 0.0);
			}
		return title2CateWeight;
	}

	public static void main(String[] args) throws Exception {
		File databaseDirectory = new File("configs/wikipedia-template-en.xml");
		Wikipedia wikipedia = new Wikipedia(databaseDirectory, false);
		DisambiguationUtil disambiguator = new DisambiguationUtil(wikipedia);
		RelatednessCache rc = new RelatednessCache(disambiguator.getArticleComparer());
		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.println("Please input the articles:");
			String articles = scanner.nextLine();

			if (articles.startsWith("exit")) {
				break;
			}else if(articles.trim().isEmpty()){
				continue;
			}
			String[] articleArray = articles.split(",");
			for(int i=0;i<articleArray.length;i++){
				articleArray[i] = articleArray[i].trim().replace('_', ' ');
			}
			if(articleArray[0].equalsIgnoreCase("l-s")){//sences of label
				if(articleArray.length<2) continue;
				Label label = new Label(wikipedia.getEnvironment(), articleArray[1], disambiguator.getTextProcessor());
				for(Label.Sense s: label.getSenses()){
					System.out.println(s.getTitle() + ":" + s.getPriorProbability());
				}
			}else if(articleArray[0].equalsIgnoreCase("aa-r")){//relatedness between articles
				if(articleArray.length<3) continue;
				Article article1 = wikipedia.getArticleByTitle(articleArray[1]);
				Article article2 = wikipedia.getArticleByTitle(articleArray[2]);
				if (article1 == null || article2 == null) {
					System.out.println("either article doesn't exist!");
				} else {
					double rel = rc.getRelatedness(article1, article2);
					System.out.println(rel);
				}
			}else if(articleArray[0].equalsIgnoreCase("a-cr")){//article to root categories
				if(articleArray.length<2) continue;
				Article article = wikipedia.getArticleByTitle(articleArray[1]);
				if(article==null) continue;
				List<String> cateList = new ArrayList<String>();
				cateList.add("Economics");
				cateList.add("Trade");
				cateList.add("Money");
				cateList.add("Business");
				cateList.add("Industry");
				cateList.add("Politics");
				cateList.add("Society");
				cateList.add("Culture");
				cateList.add("Nature");
				cateList.add("Health");
				cateList.add("People");
				cateList.add("Religion");
				cateList.add("Science");
				cateList.add("technology");
				cateList.add("Sports");
				cateList.add("Geography");
				cateList.add("History");
				cateList.add("Mathematics");
				cateList.add("Philosophy");
				Article cate;
				String catetitle;
				for(int i=0;i<cateList.size();i++){
					catetitle = cateList.get(i);
					cate = wikipedia.getArticleByTitle(catetitle);
					if (cate == null) {
						System.out.println(catetitle + " doesn't exist!");
						continue;
					} else {
						double rel = rc.getRelatedness(article, cate);
						System.out.println(catetitle + ": " + rel);
					}
				}
			}else if(articleArray[0].equalsIgnoreCase("c-pc")){//category to parents
				if(articleArray.length>2) continue;
				Category cate = wikipedia.getCategoryByTitle(articleArray[1]);
				if(cate == null) {
					System.out.println("no category exit!");
				} else {
					for(Category pc:cate.getParentCategories()){
						System.out.println(pc.getTitle());
					}
				}
			}else{
				continue;
			}
		}
		scanner.close();
//		Set<Double> eles = new HashSet<Double>();
//		eles.add(0.7);
//		eles.add(0.11);
//		eles.add(0.12);//0.665
//		eles.add(0.13);
//		eles.add(0.14);
//		eles.add(0.15);
//		eles.add(0.16);//0.846
//		
//		double ent = entropy(eles);
//		System.out.println(ent);
	}

}
