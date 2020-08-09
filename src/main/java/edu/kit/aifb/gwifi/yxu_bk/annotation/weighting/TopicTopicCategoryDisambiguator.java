package edu.kit.aifb.gwifi.yxu_bk.annotation.weighting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.kit.aifb.gwifi.annotation.detection.DisambiguationUtil;
import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.annotation.weighting.DummyTopicWeighter;
import edu.kit.aifb.gwifi.annotation.weighting.ITopicWeighter;
import edu.kit.aifb.gwifi.annotation.weighting.graph.HitsHubTopicWeighter;
import edu.kit.aifb.gwifi.annotation.weighting.graph.PageRankTopicWeighter;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.Service.DisambiguationModel;
import edu.kit.aifb.gwifi.util.RelatednessCache;
import edu.kit.aifb.gwifi.yxu_bk.annotation.detection.TopicCategory;
import edu.kit.aifb.gwifi.yxu_bk.annotation.weighting.graph.PageRankAndHitsHubTopicTopicCategoryWeighter;
import edu.kit.aifb.gwifi.yxu_bk.textcategorization.WikiPageCollector;

public class TopicTopicCategoryDisambiguator {

	private Map<DisambiguationModel, ITopicWeighter> model2TWeighter;

	private Wikipedia wikipedia;
	
	private String customizedCategoriesFilename;
	
	private TopicCategory cateTreeRoot;
	
	private List<TopicCategory> cateTreeModel;
	
	private WikiPageCollector pageCollector;
	
	private double minCateCateRel;
	private double minTopicCateRel;
	private double topic2CateEXP;

	public TopicTopicCategoryDisambiguator(Wikipedia wikipedia,
			DisambiguationUtil disambiguator, String customizedCategoriesFilename) throws IOException {
		this.wikipedia = wikipedia;
		RelatednessCache rc = new RelatednessCache(disambiguator.getArticleComparer());
		this.model2TWeighter = new HashMap<DisambiguationModel, ITopicWeighter>();
		this.customizedCategoriesFilename = customizedCategoriesFilename;
		this.pageCollector = new WikiPageCollector(wikipedia, customizedCategoriesFilename);
		this.minCateCateRel = 0.4;
		this.minTopicCateRel = 0.2;
		this.topic2CateEXP = 2.0;
		this.cateTreeModel = buildCateTreeModelWithCateFromCateFile(rc, customizedCategoriesFilename, 2);
	}

	public double getMinCateCateRelatedness(){
		return this.minCateCateRel;
	}

	public double getMinTopicCateRelatedness(){
		return this.minTopicCateRel;
	}

	public double getTopic2CateEXP(){
		return this.topic2CateEXP;
	}
	
	private List<TopicCategory> buildCateTreeModelWithCateFromCateFile(RelatednessCache rc, String cateFileName, int depth){
		List<String> cateTitleList = pageCollector.extractCateTitlesFromCateFile(customizedCategoriesFilename);
		List<TopicCategory> tcList = new ArrayList<TopicCategory>();
		cateTreeRoot = new TopicCategory(wikipedia.getEnvironment(), 0, tcList.size());
		tcList.add(cateTreeRoot);
		List<TopicCategory> uncheckedTCList = new ArrayList<TopicCategory>();
		for(String cateTitle: cateTitleList){
			createAndAddTCInList(rc, cateTitle, cateTreeRoot, tcList, uncheckedTCList);
		}
		boolean isChanged = true;
		List<TopicCategory> tempCheckedTCList;
		List<TopicCategory> newTCList;
		while(isChanged){
			isChanged = false;
			tempCheckedTCList = new ArrayList<TopicCategory>();
			newTCList = new ArrayList<TopicCategory>();
			for(TopicCategory tc: uncheckedTCList){
				if(tc.getTCDepth()>=depth)
					continue;
				isChanged = true;
				tempCheckedTCList.add(tc);
				//TODO get child article
				for(Category child: tc.getChildCategories()){
					createAndAddTCInList(rc, child.getTitle(), tc, tcList, newTCList);
				}
			}
			uncheckedTCList.removeAll(tempCheckedTCList);
			uncheckedTCList.addAll(newTCList);
		}
		for(TopicCategory tc:tcList){
			normalizeParentRelatednessOfTC(tc);
		}
		return tcList;
	}
	
	private TopicCategory createAndAddTCInList(RelatednessCache rc, 
			String tcTitle, TopicCategory parent, 
			List<TopicCategory> tcList, List<TopicCategory> newTCList){
		Category child = wikipedia.getCategoryByTitle(tcTitle);
		if(child == null){
			return null;
		}
		double rel = 0.0;
		if(!parent.equals(cateTreeRoot)){
			rel = getCateCateRelatedness(parent,child, rc);
			if(rel<minCateCateRel)
				return null;
		}
		int oldTCListLen = tcList.size();
		TopicCategory curTC = createTCToParentByTitle(tcTitle, tcList, parent);
		if(curTC==null)
			return null;
		curTC.addParentRelatedness(parent, rel);
		if(tcList.size() == oldTCListLen+1)
			newTCList.add(curTC);
		return curTC;
	}
	
	private List<TopicCategory> genCateTree(List<TopicCategory> rootedTCList, 
			Map<String, TopicCategory> title2CateMap, 
			Map<Integer, TopicCategory> id2CateMap, 
			Map<TopicCategory, Article> cate2ArticleMap){
		List<TopicCategory> tcList = new ArrayList<TopicCategory>();
		TopicCategory newTC;
		TopicCategory newChild;
		for(TopicCategory tc: cateTreeModel){
			newTC = addNewTCInTree(tc, tcList, rootedTCList, title2CateMap, id2CateMap, cate2ArticleMap);
			for(TopicCategory child: tc.getChildCates()){
				newChild = addNewTCInTree(child, tcList, rootedTCList, title2CateMap, id2CateMap, cate2ArticleMap);
				newTC.addChildCate(newChild);
				newChild.addParentCate(newTC);
				double rel = child.getRelatednessOfParent(tc);
				newChild.addParentRelatedness(newTC, rel);
			}
		}
		tcList.remove(cateTreeRoot);
		return tcList;
	}
	
	private TopicCategory addNewTCInTree(TopicCategory originTC, 
			List<TopicCategory> tcList, List<TopicCategory> rootedTCList,
			Map<String, TopicCategory> title2CateMap, 
			Map<Integer, TopicCategory> id2CateMap, 
			Map<TopicCategory, Article> cate2ArticleMap){
		TopicCategory newTC;
		if(tcList.contains(originTC)){
			newTC = tcList.get(tcList.indexOf(originTC));
		} else {
			newTC = originTC.copyTC(false);
			tcList.add(newTC);
			if(newTC.getTCDepth()==0){
				rootedTCList.add(newTC);
			}
			title2CateMap.put(newTC.getTitle(), newTC);
			id2CateMap.put(newTC.getId(), newTC);
			Article cateArticle = wikipedia.getArticleById(newTC.getArticleId());
			cate2ArticleMap.put(newTC, cateArticle);
		}
		return newTC;
	}
	
	public void getWeightedTopicTopicCategory(Collection<Topic> topics,
			RelatednessCache rc, DisambiguationModel model,
			Collection<TopicCategory> categories,
			Collection<String> initCategories, float talpha, float calpha, float beta) {
		long start = System.currentTimeMillis();
		ITopicWeighter tWeighter = getTopicTopicCategoryWeighter(model);
		if(tWeighter instanceof PageRankAndHitsHubTopicTopicCategoryWeighter){
			((PageRankAndHitsHubTopicTopicCategoryWeighter) tWeighter).setAlpha(talpha, calpha, beta);

			Map<String, TopicCategory> title2CateMap = new HashMap<String, TopicCategory>();
			Map<Integer, TopicCategory> id2CateMap = new HashMap<Integer, TopicCategory>();
			Map<TopicCategory, Article> cate2ArticleMap = new HashMap<TopicCategory, Article>();
			Map<Topic, Map<TopicCategory, Double>> topic2ParentCateRelsMap = new HashMap<Topic, Map<TopicCategory, Double>>();
			//TODO remove category and child category list
			List<TopicCategory> categoryList = new ArrayList<TopicCategory>();
//			initTCList(rc, categoryList, childCategoryList, initCategories);
			List<TopicCategory> allCates = genCateTree(categoryList, title2CateMap, id2CateMap, cate2ArticleMap);
			long genTreeEnd = System.currentTimeMillis();
			System.out.println("Time for generate category tree: " + (genTreeEnd - start) + " ms");
			//TODO train and initialize category weight
			//TODO choose category manually
//			categoryList = new ArrayList<TopicCategory>();
//			chooseCateFromTree(categoryList, allCates);
			categories.addAll(categoryList);
			relateTopicCategoriesWithTopics(rc, topics, allCates, topic2ParentCateRelsMap, title2CateMap, id2CateMap, cate2ArticleMap);
			long relateEnd = System.currentTimeMillis();
			System.out.println("Time for relate topic and category: " + (relateEnd - genTreeEnd) + " ms");
			((PageRankAndHitsHubTopicTopicCategoryWeighter) tWeighter).getWeightedTopicTopicCategory(topics, allCates, rc);
//			postCalcTCWeights(categoryList);
			updateParentTCWeights(allCates);
			normalizeTCWeights(categories);
		}else{
			tWeighter.getWeightedTopics(topics, rc);
		}
		long end = System.currentTimeMillis();
		//System.out.println("Time for topic disambiguation: " + (end - start) + " ms");
	}
	
	private void chooseCateFromTree(List<TopicCategory> chosenCates, List<TopicCategory> allCates){
		List<String> choseCateTitles = new ArrayList<String>();
		choseCateTitles.add("Economy of the European Union");
		choseCateTitles.add("European Union law");
		choseCateTitles.add("Immigration to Europe");
		for(TopicCategory cate: allCates){
			for(String cateTitle:choseCateTitles){
				if(cate.getTitle().equalsIgnoreCase(cateTitle)){
					chosenCates.add(cate);
					break;
				}
			}
		}
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

	private void initTCWeights(Collection<TopicCategory> categoryList, Collection<String> initCategoryList){
		if(initCategoryList==null||initCategoryList.size()==0)
			return;
		int initCateNum = initCategoryList.size();
		for(TopicCategory cate: categoryList){
			if(initCateNum<=0)
				break;
			for(String initCate: initCategoryList){
				if(cate.getTitle().equals(initCate)){
					cate.setWeight(1.0);
					initCateNum--;
					break;
				}
			}
		}
	}

	private List<TopicCategory> relateTopicCategoriesWithTopics(
			RelatednessCache rc, Collection<Topic> topics, List<TopicCategory> categoryList, 
			Map<Topic, Map<TopicCategory, Double>> topic2ParentCateRelsMap, 
			Map<String, TopicCategory> title2CateMap, 
			Map<Integer, TopicCategory> id2CateMap, 
			Map<TopicCategory, Article> cate2ArticleMap) {
		ArrayList<TopicCategory> allCates = new ArrayList<TopicCategory>();
		allCates.addAll(categoryList);
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
			double maxTopicCateRel = 0.0;
			// category is parent of topic
			parentCates = topic.getParentCategories();
			for (Category parent: parentCates){
				TopicCategory parentTC = id2CateMap.get(parent.getId());
				if(parentTC!=null){
					Article cateArticle = cate2ArticleMap.get(parentTC);
					double rel = getTopicCateRelatedness(topic, parentCates, cateArticle, rc);
					if(rel < minTopicCateRel)
						continue;
					if(rel > maxTopicCateRel)
						maxTopicCateRel = rel;
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
				for (TopicCategory topicCate : allCates) {
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
					double topicLeafRel = getTopicCateRelatedness(topic, null, cate2ArticleMap.get(topicCate), rc);
//					if(topicLeafRel < totalTopicLeafRel)
//						topicLeafRel = totalTopicLeafRel;
					if (topicLeafRel >= minTopicCateRel) {
						if(topicLeafRel > maxTopicCateRel)
							maxTopicCateRel = topicLeafRel;
//						topicLeafRel = Math.pow(topicLeafRel, topic2CateEXP);
//						if(!topicCate.getChildTopics().contains(topic)){
//							topicCate.getChildTopics().add(topic);
//							topicCate.addTopicRelatedness(topic, topicLeafRel);
							parentCate2Rel.put(topicCate, topicLeafRel);
//						}
					}
				}
			}
			//the orientation of the topic in category space
			orientTopicCateRel(parentCate2Rel, maxTopicCateRel, topic);
		}
		return allCates;
	}
	
	private void orientTopicCateRel(Map<TopicCategory, Double> parentCate2Rel, 
			double maxTopicCateRel, Topic topic){
		if(maxTopicCateRel<=0.0)
			return;
		for(TopicCategory tc: parentCate2Rel.keySet()){
			double rel = parentCate2Rel.get(tc)/maxTopicCateRel;
			rel = Math.pow(rel, topic2CateEXP);
			rel = rel*maxTopicCateRel;
			parentCate2Rel.put(tc, rel);
			tc.addChildTopic(topic);
			tc.addTopicRelatedness(topic, rel);
		}
	}

	private double getTopicCateRelatedness(Topic topic, Category[] topicParentCate, Article cateArticle,
			RelatednessCache rc) {
		// TODO find relatedness between topic and category
		// weaken the article relatedness used as category relatedness 
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
			return Math.pow(rc.getRelatedness(topic, cateArticle), topic2CateEXP);
		}
	}

	private double getCateCateRelatedness(Category parent,
			Category child, RelatednessCache rc) {
		// TODO find relatedness between category and category
		String parentTitle = wikipedia.getCategoryById(parent.getId())
				.getTitle();
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
			return Math.pow(rc.getRelatedness(parentArticle, childArticle), topic2CateEXP);
		}
	}

	public List<TopicCategory> genTopicCategories(List<String> cateTitleList) {
		if(cateTitleList==null || cateTitleList.size()==0){
			return genDefaultTopicCategories();
		} else {
			return genTopicCategoriesWithTitle(cateTitleList);
		}
	}
	
	private List<TopicCategory> genTopicCategoriesWithTitle(List<String> cateTitleList){
		ArrayList<TopicCategory> topicCatgoryList = new ArrayList<TopicCategory>();
		for(String cateName: cateTitleList){
			createTCToParentsByTitle(cateName, topicCatgoryList, null);
		}
		return topicCatgoryList;
	}

	private List<TopicCategory> genDefaultTopicCategories() {
		ArrayList<TopicCategory> topicCatgoryList = new ArrayList<TopicCategory>();
		createTCToParentsByTitle(TopicCategory.TC_ARTS, topicCatgoryList, null);
		createTCToParentsByTitle(TopicCategory.TC_CULTURE, topicCatgoryList, null);
		createTCToParentsByTitle(TopicCategory.TC_GEOGRAPHY, topicCatgoryList, null);
		createTCToParentsByTitle(TopicCategory.TC_HEALTH, topicCatgoryList, null);
		createTCToParentsByTitle(TopicCategory.TC_HISTORY, topicCatgoryList, null);
		createTCToParentsByTitle(TopicCategory.TC_MATHEMATICS, topicCatgoryList, null);
		createTCToParentsByTitle(TopicCategory.TC_NATURE, topicCatgoryList, null);
		createTCToParentsByTitle(TopicCategory.TC_PEOPLE, topicCatgoryList, null);
		createTCToParentsByTitle(TopicCategory.TC_PHILOSOPHY, topicCatgoryList, null);
		createTCToParentsByTitle(TopicCategory.TC_RELIGION, topicCatgoryList, null);
		createTCToParentsByTitle(TopicCategory.TC_SOCIETY, topicCatgoryList, null);
		createTCToParentsByTitle(TopicCategory.TC_SPORTS, topicCatgoryList, null);
		createTCToParentsByTitle(TopicCategory.TC_TECHNOLOGY, topicCatgoryList, null);
		createTCToParentsByTitle(TopicCategory.TC_PSYCHOLOGY, topicCatgoryList, null);

		createTCToParentByTitle(TopicCategory.TC_PLACES, topicCatgoryList, topicCatgoryList.get(2));
		createTCToParentByTitle(TopicCategory.TC_MEDICINE, topicCatgoryList, topicCatgoryList.get(3));
		createTCToParentByTitle(TopicCategory.TC_FOOD_AND_DRINK, topicCatgoryList, topicCatgoryList.get(3));
		createTCToParentByTitle(TopicCategory.TC_EVENTS, topicCatgoryList, topicCatgoryList.get(4));
		createTCToParentByTitle(TopicCategory.TC_STATISTICS, topicCatgoryList, topicCatgoryList.get(5));
		createTCToParentByTitle(TopicCategory.TC_PHYSICAL_SCIENCES, topicCatgoryList, topicCatgoryList.get(6));
		createTCToParentByTitle(TopicCategory.TC_BIOLOGY, topicCatgoryList, topicCatgoryList.get(6));
//		createTCToParentByTitle(TopicCategory.TC_BIOGRAPHY, topicCatgoryList, topicCatgoryList.get(7));
		createTCToParentByTitle(TopicCategory.TC_LOGIC, topicCatgoryList, topicCatgoryList.get(8));
		createTCToParentByTitle(TopicCategory.TC_BUSINESS, topicCatgoryList, topicCatgoryList.get(10));
		createTCToParentByTitle(TopicCategory.TC_FINANCE, topicCatgoryList, topicCatgoryList.get(10));
		createTCToParentByTitle(TopicCategory.TC_LAW, topicCatgoryList, topicCatgoryList.get(10));
		createTCToParentByTitle(TopicCategory.TC_POLITICS, topicCatgoryList, topicCatgoryList.get(10));
		createTCToParentByTitle(TopicCategory.TC_WAR, topicCatgoryList, topicCatgoryList.get(10));
		createTCToParentByTitle(TopicCategory.TC_EDUCATION, topicCatgoryList, topicCatgoryList.get(10));
		createTCToParentByTitle(TopicCategory.TC_ORGANIZATIONS, topicCatgoryList, topicCatgoryList.get(10));
		createTCToParentByTitle(TopicCategory.TC_COMPUTING, topicCatgoryList, topicCatgoryList.get(12));
		createTCToParentByTitle(TopicCategory.TC_ENGINEERING, topicCatgoryList, topicCatgoryList.get(12));
		createTCToParentByTitle(TopicCategory.TC_TRANSPORT, topicCatgoryList, topicCatgoryList.get(12));
		createTCToParentByTitle(TopicCategory.TC_INDUSTRY, topicCatgoryList, topicCatgoryList.get(12));
		createTCToParentByTitle(TopicCategory.TC_ARCHITECTURE, topicCatgoryList, topicCatgoryList.get(12));
		
		return topicCatgoryList;
	}
	
	private TopicCategory createTCToParentsByTitle(String title, List<TopicCategory> tcList, List<TopicCategory> parents){
		Category cate = wikipedia.getCategoryByTitle(title);
		if(cate==null){
			return null;
		}
		TopicCategory newTopicCategory = new TopicCategory(
				wikipedia.getEnvironment(), cate.getId(), tcList.size());
		if(!tcList.contains(newTopicCategory)){
			if(!tcList.add(newTopicCategory))
				return null;
		}else{
			newTopicCategory = tcList.get(tcList.indexOf(newTopicCategory));
		}
		if(parents!=null){
			for(TopicCategory parent: parents){
				newTopicCategory.addParentCate(parent);
				parent.addChildCate(newTopicCategory);
			}
		}
		Article cateArticle = wikipedia.getArticleByTitle(title);
		if(cateArticle!=null){
			newTopicCategory.setArticleId(cateArticle.getId());
		}
		return newTopicCategory;
	}
	
	private TopicCategory createTCToParentByTitle(String title, List<TopicCategory> tcList, TopicCategory parent){
		List<TopicCategory> parentList = new ArrayList<TopicCategory>();
		parentList.add(parent);
		return createTCToParentsByTitle(title, tcList, parentList);
	}
	
	private int postCalcTCWeights(List<TopicCategory> categoryList){
		for(TopicCategory parentCate: categoryList){
			double parentWeight = parentCate.getWeight();
			for(TopicCategory childCate:parentCate.getChildCates()){
				double childRel = childCate.getRelatednessOfParent(parentCate);
				parentWeight += childRel * childCate.getWeight();
			}
			parentCate.setWeight(parentWeight);
		}
		return 0;
	}

	private int normalizeTCWeights(Collection<TopicCategory> categories) {
		double maxWeight = 0.0;
		for (TopicCategory cate : categories) {
			if (cate.getWeight() > maxWeight) {
				maxWeight = cate.getWeight();
			}
		}
		if(maxWeight==0.0)
			maxWeight = 1.0;
		double normalizedWeight = 0.0;
		for (TopicCategory cate : categories) {
			normalizedWeight = cate.getWeight() / maxWeight;
			normalizedWeight = Math.sqrt(normalizedWeight);
			cate.setWeight(normalizedWeight);
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
	
	public ArrayList<TopicCategory> getFullChildCatesOfTCs(
			Collection<TopicCategory> parentCates, int startIndex,
			RelatednessCache rc) {
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
						parent.addChildCate(tempChild);
						tempChild.addParentCate(parent);
						tempChild.addParentRelatedness(parent, rel);
						fullChildCateList.add(tempChild);
					}
				} else {
					double rel = getCateCateRelatedness(parent, tempChild, rc);
					if (rel > 0.0) {
						parent.addChildCate(tempChild);
						tempChild.addParentCate(parent);
						tempChild.addParentRelatedness(parent, rel);
					}
				}
			}
		}
		for (TopicCategory child : fullChildCateList) {
			normalizeParentRelatednessOfTC(child);
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
	
	private void normalizeParentRelatednessOfTC(TopicCategory child) {
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
	
	private void normalizeParentRelatednessInMap(Map<Category, Double> parentRelatednessMap){
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
	
	//TODO initialize the weight of categories
	public Map<Integer,String> getFullChildArticlesOfTC(TopicCategory rootCate){
		return pageCollector.getFullChildArticlesOfCateWithTitle(rootCate.getTitle());
	}
	
	public Map<String, Double> genInitWeightOfCatesForText(List<String> cateTitleList, String source){
		Map<String, Double> cateTitle2Weight = new HashMap<String, Double>();
		// implement..
		return cateTitle2Weight;
	}

	public static void main(String[] args) throws Exception {
		// do nothing
	}

}
