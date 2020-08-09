package edu.kit.aifb.gwifi.yxu_mvertex.annotation.detection;

import java.util.Collection;
import java.util.HashMap;

import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.db.WEnvironment;
import edu.kit.aifb.gwifi.db.struct.DbPage;
import edu.kit.aifb.gwifi.model.Article;
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
	public static String TC_TECHNOLOGY = "Technology";
	

	public static String TC_BUSINESS = "Business";
	public static String TC_FINANCE = "Finance";
	public static String TC_LAW = "Law";
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
	private Article tcArticle;
	private int tcDepth;
	private String aidaTitle;

	private HashMap<TopicCategory, Double> parentRelatednessMap;
	private HashMap<TopicCategory, Double> childRelatednessMap;
	private HashMap<Topic, Double> topicRelatednessMap;

	public TopicCategory(WEnvironment env, int id) {
		super(env, id);
		topicRelatednessMap = new HashMap<Topic, Double>();
		parentRelatednessMap = new HashMap<TopicCategory, Double>();
		childRelatednessMap = new HashMap<TopicCategory, Double>();
		index = 0;
		tcDepth = -1;
		weight = 0.0;
	}

	public TopicCategory(WEnvironment env, int id, int index) {
		super(env, id);
		topicRelatednessMap = new HashMap<Topic, Double>();
		parentRelatednessMap = new HashMap<TopicCategory, Double>();
		childRelatednessMap = new HashMap<TopicCategory, Double>();
		this.index = index;
		tcDepth = -1;
		weight = 0.0;
	}

	protected TopicCategory(WEnvironment env, int id, DbPage pd) {
		super(env, id, pd);
		topicRelatednessMap = new HashMap<Topic, Double>();
		parentRelatednessMap = new HashMap<TopicCategory, Double>();
		childRelatednessMap = new HashMap<TopicCategory, Double>();
		tcDepth = -1;
		weight = 0.0;
	}
	
	public TopicCategory(WEnvironment env, int aidaID, String aidaTitle, Double weight){
		super(env, aidaID);
		topicRelatednessMap = new HashMap<Topic, Double>();
		parentRelatednessMap = new HashMap<TopicCategory, Double>();
		childRelatednessMap = new HashMap<TopicCategory, Double>();
		index = 0;
		tcDepth = -1;
		this.aidaTitle = aidaTitle;
		this.weight = weight;
	}
	
	public void resetTopicRelatedness(){
		topicRelatednessMap = new HashMap<Topic, Double>();
	}
	
	public String getAidaTitle(){
		return this.aidaTitle;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}
	
	public void setTCArticle(Article tcArticle){
		this.tcArticle = tcArticle;
	}
	
	public Article getTCArticle(){
		return tcArticle;
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
			for(TopicCategory child: childRelatednessMap.keySet()){
				child.updateTCDepth(newDepth+1);
			}
		}
	}

	public Collection<TopicCategory> getChildCates() {
		return childRelatednessMap.keySet();
	}

	public boolean addChildCate(TopicCategory childCate) {
		if (!childRelatednessMap.containsKey(childCate)) {
			childRelatednessMap.put(childCate, null);
			return true;
		}
		return false;
	}
	
	public void removeChildCate(TopicCategory childCate){
		childRelatednessMap.remove(childCate);
	}

	public boolean isParentOf(TopicCategory childCate) {
		return childRelatednessMap.containsKey(childCate);
	}

	public int getChildNum(){
		return childRelatednessMap.size();
	}
	
	public HashMap<TopicCategory, Double> getChildRelatednessMap() {
		return childRelatednessMap;
	}
	
	public void addChildRelatedness(TopicCategory child, double relatedness) {
		childRelatednessMap.put(child, relatedness);
	}
	
	public double getRelatednessOfChild(TopicCategory child) {
		return childRelatednessMap.get(child);
	}

	public Collection<TopicCategory> getParentCates() {
		return parentRelatednessMap.keySet();
	}
	
	public boolean addParentCate(TopicCategory parentCate) {
		if (!parentRelatednessMap.containsKey(parentCate)) {
			parentRelatednessMap.put(parentCate, null);
			updateTCDepth(parentCate.getTCDepth()+1);
			return true;
		}
		return false;
	}
	
	public void removeParentCate(TopicCategory parentCate){
		parentRelatednessMap.remove(parentCate);
		//TODO update tcDepth
	}
	
	public boolean isChildOf(TopicCategory parent) {
		return parentRelatednessMap.containsKey(parent);
	}

	public int getParentNum(){
		return parentRelatednessMap.size();
	}
	
	public HashMap<TopicCategory, Double> getParentRelatednessMap() {
		return parentRelatednessMap;
	}

	public void addParentRelatedness(TopicCategory parent, double relatedness) {
		parentRelatednessMap.put(parent, relatedness);
		updateTCDepth(parent.getTCDepth()+1);
	}
	
	public double getRelatednessOfParent(TopicCategory parent) {
		return parentRelatednessMap.get(parent);
	}

	public Collection<Topic> getChildTopics() {
		return topicRelatednessMap.keySet();
	}
	
	public boolean addChildTopic(Topic childTopic){
		if(!topicRelatednessMap.containsKey(childTopic)){
			topicRelatednessMap.put(childTopic, null);
			return true;
		}else{
			return false;
		}
	}
	
	public void removeChildTopic(Topic childTopic){
		topicRelatednessMap.remove(childTopic);
	}

	public boolean containsTopic(Topic topic) {
		return topicRelatednessMap.containsKey(topic);
	}
	
	public int getTopicNum(){
		return topicRelatednessMap.size();
	}

	public void addTopicRelatedness(Topic childTopic, double relatedness) {
		topicRelatednessMap.put(childTopic, relatedness);
	}

	public double getRelatednessOfTopic(Topic topic) {
		return topicRelatednessMap.get(topic);
	}
	
	public TopicCategory copyTC(boolean isWeighted, boolean initDepth){
		TopicCategory copy = new TopicCategory(env, id, index);
		copy.setTCArticle(tcArticle);
		if(initDepth){
			copy.setTCDepth(tcDepth);
		}
		if(isWeighted)
			copy.setWeight(weight);
		return copy;
	}

}
