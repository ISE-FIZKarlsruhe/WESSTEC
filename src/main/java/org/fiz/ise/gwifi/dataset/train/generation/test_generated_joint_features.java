package org.fiz.ise.gwifi.dataset.train.generation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.dataset.LabelsOfTheTexts;
import org.fiz.ise.gwifi.dataset.ReadDataset;
import org.fiz.ise.gwifi.dataset.assignLabels.AssignLabelsBasedOnConfVecSimilarity;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.Config;

import edu.kit.aifb.gwifi.model.Article;

public class test_generated_joint_features {
	
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");
	
	private static final String DATASET_TRAIN_WEB = Config.getString("DATASET_TRAIN_SNIPPETS","");
	private static final String DATASET_TEST_WEB = Config.getString("DATASET_TEST_SNIPPETS","");
	
	private static final String DATASET_SNIPPETS_TRAIN_CATEGORIZED_LINE = Config.getString("DATASET_SNIPPETS_TRAIN_CATEGORIZED_LINE","");
	private static final String DATASET_SNIPPETS_TRAIN_CATEGORIZED_D2Vec = Config.getString("DATASET_SNIPPETS_TRAIN_CATEGORIZED_D2Vec","");
	private static final String DATASET_SNIPPETS_TRAIN_CATEGORIZED_GOOGLE = Config.getString("DATASET_SNIPPETS_TRAIN_CATEGORIZED_GOOGLE","");
	
	static final Map<String,  List<Article>> map_DOC2VEC_SNIPPETS = new HashMap<String,List<Article>>(ReadDataset.read_dataset_Snippets(DATASET_SNIPPETS_TRAIN_CATEGORIZED_D2Vec));
	static final Map<String,  List<Article>> map_GOOGLE_SNIPPETS = new HashMap<String,List<Article>>(ReadDataset.read_dataset_Snippets(DATASET_SNIPPETS_TRAIN_CATEGORIZED_GOOGLE));
	static final Map<String,  List<Article>> map_LINE_SNIPPETS = new HashMap<String,List<Article>>(ReadDataset.read_dataset_Snippets(DATASET_SNIPPETS_TRAIN_CATEGORIZED_LINE));
	
	public static void main(String[] args) {
		System.out.println("Running Snippet analyses");
		
		Map<String, List<Article>> map_train = ReadDataset.read_dataset_Snippets(DATASET_TRAIN_WEB);
		List<String> lst_train= ReadDataset.read_snippets(DATASET_TRAIN_WEB);
		
		int count_correct=0;
		int count_wrong=0;
		for(String str: lst_train ) {
			String gt=map_train.get(str).get(0).getTitle();
			String majority_vote_label=getBestLabel_all_embeddings(Dataset.WEB_SNIPPETS,str);
			secondLOG.info(gt+"**"+majority_vote_label+"**"+str);
			
//			if (gt.equalsIgnoreCase("The arts")||gt.equalsIgnoreCase("Entertainment")) {
//				gt="Culture";
//			}
//			else if (gt.equalsIgnoreCase("Science")) {
//				gt="Education";
//			}
//			else if (gt.equalsIgnoreCase("Society")) {
//				gt="Politics";
//			}
//			
//			
//			if (gt.equalsIgnoreCase(majority_vote_label)) {
//				count_correct++;
//			}
//			else {
//				System.out.println(gt+" "+majority_vote_label);
//			}
		
		}
		System.out.println(count_correct);
		System.out.println(lst_train.size());
	}
	public static String getBestLabel_all_embeddings(Dataset dname, String sentence) {
		try {
			int countTempSize_0=0;

			String str_DOC2VEC = null;
			String str_LINE = null;
			String str_GOOGLE = null;

			Map<String, Double> temp= new HashMap<String, Double>();
			List<Article> lstCats = null;
			
			 if (dname.equals(Dataset.WEB_SNIPPETS)) {

				lstCats = new ArrayList<Article>(Categories.getLabels_Snippets());
				for(Article a : lstCats) {
					if (!a.getTitle().equalsIgnoreCase("The arts")&& !a.getTitle().equalsIgnoreCase("Entertainment")&&
							!a.getTitle().equalsIgnoreCase("Science")&& !a.getTitle().equalsIgnoreCase("Society")) {
						temp.put(a.getTitle(), 0.0);
					}
				}
				
				if (map_DOC2VEC_SNIPPETS.containsKey(sentence)) {
					str_DOC2VEC = map_DOC2VEC_SNIPPETS.get(sentence).get(0).getTitle();
					if (str_DOC2VEC.equalsIgnoreCase("The arts")||str_DOC2VEC.equalsIgnoreCase("Entertainment")) {
						str_DOC2VEC="Culture";
					}
					else if (str_DOC2VEC.equalsIgnoreCase("Science")) {
						str_DOC2VEC="Education";
					}
					else if (str_DOC2VEC.equalsIgnoreCase("Society")) {
						str_DOC2VEC="Politics";
					}
				}
				if (map_LINE_SNIPPETS.containsKey(sentence)) {
					str_LINE = map_LINE_SNIPPETS.get(sentence).get(0).getTitle();

					if (str_LINE.equalsIgnoreCase("The arts")||str_LINE.equalsIgnoreCase("Entertainment")) {
						str_LINE="Culture";
					}
					else if (str_LINE.equalsIgnoreCase("Science")) {
						str_LINE="Education";
					}
					else if (str_LINE.equalsIgnoreCase("Society")) {
						str_LINE="Politics";
					}
				}
				if (map_GOOGLE_SNIPPETS.containsKey(sentence)) {
					str_GOOGLE = map_GOOGLE_SNIPPETS.get(sentence).get(0).getTitle();
					if (str_GOOGLE.equalsIgnoreCase("The arts")||str_GOOGLE.equalsIgnoreCase("Entertainment")) {
						str_GOOGLE="Culture";
					}
					else if (str_GOOGLE.equalsIgnoreCase("Science")) {
						str_GOOGLE="Education";
					}
					else if (str_GOOGLE.equalsIgnoreCase("Society")) {
						str_GOOGLE="Politics";
					}
				}
				countTempSize_0=temp.size();
			}
			if ((str_DOC2VEC!=null&&str_LINE!=null&&str_GOOGLE!=null )&& str_DOC2VEC.equals(str_GOOGLE)&&str_DOC2VEC.equals(str_LINE)) {
				if (!temp.containsKey(str_GOOGLE)) {
					System.out.println("Temp does not contain the string"+str_GOOGLE);
					System.exit(1);
				}
				temp.put(str_GOOGLE, 1.);
			}
			else if(str_DOC2VEC!=null&&str_LINE!=null&&(str_DOC2VEC.equals(str_LINE))) { //D2Vec=LINE
				if (!temp.containsKey(str_LINE)) {
					System.out.println("Temp does not contain the string: "+str_LINE);
					System.exit(1);
				}
				temp.put(str_DOC2VEC, 1.0);
			}
			else if(str_DOC2VEC!=null&&str_GOOGLE!=null&&str_DOC2VEC.equals(str_GOOGLE)) {//D2Vec=GOOGLE
				if (!temp.containsKey(str_DOC2VEC)) {
					System.out.println("Temp does not contain the string: "+str_DOC2VEC);
					System.exit(1);
				}
				temp.put(str_DOC2VEC,1.);
			}
			else if(str_LINE!=null&&str_GOOGLE!=null&&str_LINE.equals(str_GOOGLE)) {//LINE=GOOGLE

				if (!temp.containsKey(str_LINE)) {
					System.out.println("Temp does not contain the string: "+str_LINE);
					System.exit(1);
				}

				temp.put(str_LINE, 1.);
			}
			else {

				if (str_LINE!=null) {
					temp.put(str_LINE, 1.);
				}
			}
			double sum = temp.entrySet().stream().mapToDouble //to normalize the final
					(l->l.getValue()).sum();
			
			LinkedHashMap<String, Double> sortedMap = new LinkedHashMap<>();
			temp.entrySet()
			.stream()
			.sorted(Map.Entry.comparingByKey())
			.forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));

			StringBuilder labels=new StringBuilder();
			for(Double s : sortedMap.values()) {
				labels.append((s*1.)/(sum*1.)+",");
			}
			if (temp.size()!=countTempSize_0) {
				System.out.println("Size of the temp is not equal to lstCats");
				System.out.println("Temp: "+temp+" \n size"+temp.size());
				System.out.println("lstCats: "+lstCats+" \n size"+lstCats.size());
				System.exit(1);
			}
			if (str_LINE!=null||str_GOOGLE!=null||str_DOC2VEC!=null) {
				return labels.substring(0,labels.length()-1);
			}
			else {
				return null;
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
	public static String get_majority_vote_label(Dataset dname, String sentence) {
		try {
			int countTempSize_0=0;

			String str_DOC2VEC = null;
			String str_LINE = null;
			String str_GOOGLE = null;

			Map<String, Double> temp= new HashMap<String, Double>();
			List<Article> lstCats = null;
			
			
			if (dname.equals(Dataset.WEB_SNIPPETS)) {

				lstCats = new ArrayList<Article>(Categories.getLabels_Snippets());
				for(Article a : lstCats) {
					if (!a.getTitle().equalsIgnoreCase("The arts")&& !a.getTitle().equalsIgnoreCase("Entertainment")&&
							!a.getTitle().equalsIgnoreCase("Science")&& !a.getTitle().equalsIgnoreCase("Society")) {
						temp.put(a.getTitle(), 0.0);
					}
				}
				
				if (map_DOC2VEC_SNIPPETS.containsKey(sentence)) {
					str_DOC2VEC = map_DOC2VEC_SNIPPETS.get(sentence).get(0).getTitle();
					if (str_DOC2VEC.equalsIgnoreCase("The arts")||str_DOC2VEC.equalsIgnoreCase("Entertainment")) {
						str_DOC2VEC="Culture";
					}
					else if (str_DOC2VEC.equalsIgnoreCase("Science")) {
						str_DOC2VEC="Education";
					}
					else if (str_DOC2VEC.equalsIgnoreCase("Society")) {
						str_DOC2VEC="Politics";
					}
				}
				if (map_LINE_SNIPPETS.containsKey(sentence)) {
					str_LINE = map_LINE_SNIPPETS.get(sentence).get(0).getTitle();

					if (str_LINE.equalsIgnoreCase("The arts")||str_LINE.equalsIgnoreCase("Entertainment")) {
						str_LINE="Culture";
					}
					else if (str_LINE.equalsIgnoreCase("Science")) {
						str_LINE="Education";
					}
					else if (str_LINE.equalsIgnoreCase("Society")) {
						str_LINE="Politics";
					}
				}
				if (map_GOOGLE_SNIPPETS.containsKey(sentence)) {
					str_GOOGLE = map_GOOGLE_SNIPPETS.get(sentence).get(0).getTitle();
					if (str_GOOGLE.equalsIgnoreCase("The arts")||str_GOOGLE.equalsIgnoreCase("Entertainment")) {
						str_GOOGLE="Culture";
					}
					else if (str_GOOGLE.equalsIgnoreCase("Science")) {
						str_GOOGLE="Education";
					}
					else if (str_GOOGLE.equalsIgnoreCase("Society")) {
						str_GOOGLE="Politics";
					}
				}
				countTempSize_0=temp.size();
			}
			if ((str_DOC2VEC!=null&&str_LINE!=null&&str_GOOGLE!=null )&& str_DOC2VEC.equals(str_GOOGLE)&&str_DOC2VEC.equals(str_LINE)) {
				if (!temp.containsKey(str_GOOGLE)) {
					System.out.println("Temp does not contain the string"+str_GOOGLE);
					System.exit(1);
				}
				return str_GOOGLE;
			}
			else if(str_DOC2VEC!=null&&str_LINE!=null&&(str_DOC2VEC.equals(str_LINE))) { //D2Vec=LINE
				if (!temp.containsKey(str_LINE)) {
					System.out.println("Temp does not contain the string: "+str_LINE);
					System.exit(1);
				}
				return str_DOC2VEC;
			}
			else if(str_DOC2VEC!=null&&str_GOOGLE!=null&&str_DOC2VEC.equals(str_GOOGLE)) {//D2Vec=GOOGLE
				if (!temp.containsKey(str_DOC2VEC)) {
					System.out.println("Temp does not contain the string: "+str_DOC2VEC);
					System.exit(1);
				}
				return str_DOC2VEC;
			}
			else if(str_LINE!=null&&str_GOOGLE!=null&&str_LINE.equals(str_GOOGLE)) {//LINE=GOOGLE

				if (!temp.containsKey(str_LINE)) {
					System.out.println("Temp does not contain the string: "+str_LINE);
					System.exit(1);
				}

				return str_LINE;
			}
			else { //none of the embedding models agreed the default then LINE

				if (str_LINE!=null) {
					return str_LINE;
				}
			}
			
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
	
}
