package edu.kit.aifb.gwifi.yxu_bk.annotation.detection;

import java.util.ArrayList;
import java.util.HashMap;

import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.db.WEnvironment;
import edu.kit.aifb.gwifi.db.struct.DbPage;
import edu.kit.aifb.gwifi.model.Category;

public class TopicCategory extends Category {

	public static String TC_CULTURE = "Culture";
	public static String TC_ARTS = "Arts";
	public static String TC_SPORTS = "Sports";
	public static String TC_GEOGRAPHY = "Geography";
	public static String TC_HEALTH = "Health";
	public static String TC_HISTORY = "History";
	public static String TC_MATHEMATICS = "Mathematics";
	public static String TC_NATURE = "Nature";
	public static String TC_PEOPLE = "People";
	public static String TC_PHILOSOPHY = "Philosophy";
	public static String TC_PSYCHOLOGY = "Psychology";
	public static String TC_RELIGION = "Religion";
	public static String TC_SOCIETY = "Society";
	public static String TC_BUSINESS = "Business";
	public static String TC_FINANCE = "Finance";
	public static String TC_LAW = "Law";
	public static String TC_TECHNOLOGY = "Technology";
	public static String TC_POLITICS = "Politics";
	public static String TC_PLACES = "Places";
	public static String TC_MEDICINE = "Medicine";
	public static String TC_EVENTS = "Events";
	public static String TC_LOGIC = "Logic";
	public static String TC_STATISTICS = "Statistics";
	public static String TC_PHYSICAL_SCIENCES = "Physical sciences";
	public static String TC_BIOLOGY = "Biology";
	public static String TC_BIOGRAPHY = "Biography_(genre)";
	public static String TC_WAR = "War";
	public static String TC_EDUCATION = "Education";
	public static String TC_ORGANIZATIONS = "Organizations";
	public static String TC_COMPUTING = "Computing";
	public static String TC_ENGINEERING = "Engineering";
	public static String TC_TRANSPORT = "Transport";
	public static String TC_INDUSTRY = "Industry";
	public static String TC_ARCHITECTURE = "Architecture";
	public static String TC_FOOD_AND_DRINK = "Food and drink";
	
	/**
	 * The identify in the including article.
	 */
	private int index;
	private int articleId;
	private int tcDepth;

	private ArrayList<TopicCategory> parentCates;
	private ArrayList<TopicCategory> childCates;
	private HashMap<TopicCategory, Double> parentRelatednessMap;

	private ArrayList<Topic> childTopics;
	private HashMap<Topic, Double> topicRelatednessMap;

	public TopicCategory(WEnvironment env, int id) {
		super(env, id);
		parentCates = new ArrayList<TopicCategory>();
		childCates = new ArrayList<TopicCategory>();
		childTopics = new ArrayList<Topic>();
		topicRelatednessMap = new HashMap<Topic, Double>();
		parentRelatednessMap = new HashMap<TopicCategory, Double>();
		index = 0;
		articleId=-1;
		tcDepth = -1;
		weight = 0.0;
	}

	public TopicCategory(WEnvironment env, int id, int index) {
		super(env, id);
		parentCates = new ArrayList<TopicCategory>();
		childCates = new ArrayList<TopicCategory>();
		childTopics = new ArrayList<Topic>();
		topicRelatednessMap = new HashMap<Topic, Double>();
		parentRelatednessMap = new HashMap<TopicCategory, Double>();
		this.index = index;
		articleId=-1;
		tcDepth = -1;
		weight = 0.0;
	}

	protected TopicCategory(WEnvironment env, int id, DbPage pd) {
		super(env, id, pd);
		parentCates = new ArrayList<TopicCategory>();
		childCates = new ArrayList<TopicCategory>();
		childTopics = new ArrayList<Topic>();
		topicRelatednessMap = new HashMap<Topic, Double>();
		parentRelatednessMap = new HashMap<TopicCategory, Double>();
		articleId=-1;
		tcDepth = -1;
		weight = 0.0;
	}
	
	public void initTopicRelatedness(){
		childTopics = new ArrayList<Topic>();
		topicRelatednessMap = new HashMap<Topic, Double>();
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}
	
	public void setArticleId(int articleId){
		this.articleId = articleId;
	}
	
	public int getArticleId(){
		return articleId;
	}
	
	public void setTCDepth(int depth){
		this.tcDepth = depth;
	}
	
	public int getTCDepth(){
		return tcDepth;
	}
	
	public void updateTCDepth(int newDepth){
		if(tcDepth == -1 || tcDepth>newDepth){
			tcDepth = newDepth;
			for(TopicCategory child: childCates){
				child.updateTCDepth(newDepth+1);
			}
		}
	}

	public ArrayList<TopicCategory> getParentCates() {
		return parentCates;
	}

	public boolean addParentCate(TopicCategory parentCate) {
		if (!this.parentCates.contains(parentCate)) {
			this.parentCates.add(parentCate);
			updateTCDepth(parentCate.getTCDepth()+1);
			return true;
		}
		return false;
	}

	public boolean isChildOf(TopicCategory cate) {
		return this.parentCates.contains(cate);
	}

	public ArrayList<TopicCategory> getChildCates() {
		return childCates;
	}

	public boolean addChildCate(TopicCategory childCate) {
		if (!this.childCates.contains(childCate)) {
			this.childCates.add(childCate);
			return true;
		}
		return false;
	}
	
	public void removeChildCate(TopicCategory childCate){
		while(childCates.remove(childCate)){
			// do nothing
		}
	}

	public boolean isParentOf(TopicCategory cate) {
		return this.childCates.contains(cate);
	}

	public void addParentRelatedness(TopicCategory parent, double relatedness) {
		parentRelatednessMap.put(parent, relatedness);
	}

	public HashMap<TopicCategory, Double> getParentRelatednessMap() {
		return parentRelatednessMap;
	}

	public double getRelatednessOfParent(TopicCategory parent) {
		return parentRelatednessMap.get(parent);
	}

	public boolean containsParentInMap(TopicCategory parent) {
		return parentRelatednessMap.containsKey(parent);
	}

	public void setChildTopics(ArrayList<Topic> childTopics) {
		this.childTopics = childTopics;
	}

	public ArrayList<Topic> getChildTopics() {
		return childTopics;
	}
	
	public boolean addChildTopic(Topic childTopic){
		if(!childTopics.contains(childTopic)){
			return childTopics.add(childTopic);
		}else{
			return false;
		}
	}
	
	public void removeChildTopic(Topic childTopic){
		while(childTopics.remove(childTopic)){
			// do nothing
		}
	}

	public boolean containsTopics(Topic topic) {
		return childTopics.contains(topic);
	}

	public void addTopicRelatedness(Topic childTopic, double relatedness) {
		topicRelatednessMap.put(childTopic, relatedness);
	}

	public HashMap<Topic, Double> getTopicRelatednessMap() {
		return topicRelatednessMap;
	}

	public double getRelatednessOfTopic(Topic topic) {
		return topicRelatednessMap.get(topic);
	}

	public boolean containsTopicInMap(Topic topic) {
		return topicRelatednessMap.containsKey(topic);
	}
	
	public TopicCategory copyTC(boolean isWeighted){
		TopicCategory copy = new TopicCategory(env, id, index);
		copy.setTCDepth(tcDepth);
		copy.setArticleId(articleId);
		if(isWeighted)
			copy.setWeight(weight);
		return copy;
	}

}
