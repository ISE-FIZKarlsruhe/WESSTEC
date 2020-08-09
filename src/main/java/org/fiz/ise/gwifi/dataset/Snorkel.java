package org.fiz.ise.gwifi.dataset;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.assignLabels.AssignLabelsBasedOnConfVecSimilarity;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.model.EmbeddingModel;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.MapUtil;

import edu.kit.aifb.gwifi.model.Article;

public class Snorkel {

	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");
	
	private static final String DATASET_DBP_TRAIN = Config.getString("DATASET_DBP_TRAIN","");
	private static final String DATASET_TRAIN_SNIPPETS = Config.getString("DATASET_TRAIN_SNIPPETS","");
	private static final String DATASET_TEST_SNIPPETS = Config.getString("DATASET_TEST_SNIPPETS","");
	
	private static final String DATASET_DBP_TRAIN_CATEGORIZED_LINE = Config.getString("DATASET_DBP_TRAIN_CATEGORIZED_LINE","");
	private static final String DATASET_DBP_TRAIN_CATEGORIZED_D2Vec = Config.getString("DATASET_DBP_TRAIN_CATEGORIZED_D2Vec","");
	private static final String DATASET_DBP_TRAIN_CATEGORIZED_GOOGLE = Config.getString("DATASET_DBP_TRAIN_CATEGORIZED_GOOGLE","");
	
	private static final String DATASET_SNIPPETS_TRAIN_CATEGORIZED_LINE = Config.getString("DATASET_SNIPPETS_TRAIN_CATEGORIZED_LINE","");
	private static final String DATASET_SNIPPETS_TRAIN_CATEGORIZED_D2Vec = Config.getString("DATASET_SNIPPETS_TRAIN_CATEGORIZED_D2Vec","");
	private static final String DATASET_SNIPPETS_TRAIN_CATEGORIZED_GOOGLE = Config.getString("DATASET_SNIPPETS_TRAIN_CATEGORIZED_GOOGLE","");

	static final Map<String, String> map_DOC2VEC_AG= new HashMap<String, String>(AssignLabelsBasedOnConfVecSimilarity.readLabelAssignment_AG(Dataset.AG, EmbeddingModel.Doc2Vec));
	static final Map<String, String> map_GOOGLE_AG = new HashMap<String, String>(AssignLabelsBasedOnConfVecSimilarity.readLabelAssignment_AG(Dataset.AG, EmbeddingModel.GOOGLE));
	static final Map<String, String> map_LINE_AG = new HashMap<String, String>(AssignLabelsBasedOnConfVecSimilarity.readLabelAssignment_AG(Dataset.AG, EmbeddingModel.LINE_Ent_Ent));

	static final Map<String,  List<Article>> map_DOC2VEC_DBp = new HashMap<String,List<Article>>(ReadDataset.read_dataset_Doc2Vec_categorized(Dataset.DBpedia, DATASET_DBP_TRAIN_CATEGORIZED_D2Vec));
	static final Map<String,  List<Article>> map_GOOGLE_DBp = new HashMap<String,List<Article>>( ReadDataset.read_dataset_DBPedia_SampleLabel(DATASET_DBP_TRAIN_CATEGORIZED_GOOGLE));
	static final Map<String,  List<Article>> map_LINE_DBp = new HashMap<String,List<Article>>(ReadDataset.read_dataset_DBPedia_SampleLabel(DATASET_DBP_TRAIN_CATEGORIZED_LINE));

	static final Map<String,  List<Article>> dataset_dbp_test = ReadDataset.read_dataset_DBPedia_SampleLabel(Config.getString("DATASET_DBP_TEST",""));
	static final Map<String,  List<Article>> dataset_dbp_train = ReadDataset.read_dataset_DBPedia_SampleLabel(Config.getString("DATASET_DBP_TRAIN",""));
	static final Map<String,  List<Article>> dataset_ag_train = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION,Config.getString("DATASET_TRAIN_AG",""));

	static final Map<String,  List<Article>> dataset_snippest_test = ReadDataset.read_dataset_Snippets(DATASET_TEST_SNIPPETS);
	static final Map<String,  List<Article>> dataset_snippest_train = ReadDataset.read_dataset_Snippets(DATASET_TRAIN_SNIPPETS);

	static final Map<String,  List<Article>> map_DOC2VEC_SNIPPETS = new HashMap<String,List<Article>>(ReadDataset.read_dataset_Snippets(DATASET_SNIPPETS_TRAIN_CATEGORIZED_D2Vec));
	static final Map<String,  List<Article>> map_GOOGLE_SNIPPETS = new HashMap<String,List<Article>>(ReadDataset.read_dataset_Snippets(DATASET_SNIPPETS_TRAIN_CATEGORIZED_GOOGLE));
	static final Map<String,  List<Article>> map_LINE_SNIPPETS = new HashMap<String,List<Article>>(ReadDataset.read_dataset_Snippets(DATASET_SNIPPETS_TRAIN_CATEGORIZED_LINE));

	static{
		System.out.println("Size of the maps GOOGLE:"+ map_GOOGLE_AG.size());
		System.out.println("Size of the maps LINE:"+ map_LINE_AG.size());
		System.out.println("Size of the maps DOC2VEC:"+ map_DOC2VEC_AG.size());
	}

	public static void main(String[] args) {
		Map<String, List<Article>>  dataset_ag_train = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION,Config.getString("DATASET_TRAIN_AG",""));
		Dataset dname=Dataset.AG;
		for(Entry<String, List<Article>> e : dataset_ag_train.entrySet()) {
			secondLOG.info(getLabels(dname, e.getKey()));
			resultLog.info(e.getKey()+"\t"+e.getValue().get(0).toString().split(" ")[1]);
		}
	}
	public static String getLabels(Dataset dname, String sentence) {
		try {
			String str_DOC2VEC = null;
			String str_LINE = null;
			String str_GOOGLE = null;
			
			Integer int_DOC2VEC = null;
			Integer int_LINE = null;
			Integer int_GOOGLE = null;

			Map<String, Double> temp= new HashMap<String, Double>();
			List<Article> lstCats = null;
			if (dname.equals(Dataset.AG)) {
				if (map_DOC2VEC_AG.size()==0||map_LINE_AG.size()==0||map_GOOGLE_AG.size()==0) {
					System.out.println("Could not read the files DBpedia files");
					System.exit(1);
				}
				str_DOC2VEC = map_DOC2VEC_AG.get(sentence);
				str_LINE = map_LINE_AG.get(sentence);
				str_GOOGLE = map_GOOGLE_AG.get(sentence);

				if (str_DOC2VEC==null) {
					int_DOC2VEC = 0;
				}
				else {
					int_DOC2VEC = MapUtil.getKeyByValue(LabelsOfTheTexts.getLables_AG_article(), WikipediaSingleton.getInstance().getArticle(str_DOC2VEC));
				}
				
				if (str_LINE==null) {
					int_LINE = 0;
				}
				else {
					int_LINE = MapUtil.getKeyByValue(LabelsOfTheTexts.getLables_AG_article(), WikipediaSingleton.getInstance().getArticle(str_LINE));
				}
				
				if (str_GOOGLE==null) {
					int_GOOGLE = 0;
				}
				else {
					int_GOOGLE = MapUtil.getKeyByValue(LabelsOfTheTexts.getLables_AG_article(), WikipediaSingleton.getInstance().getArticle(str_GOOGLE));
				}
			}
			else if (dname.equals(Dataset.DBpedia)) {

				if (map_DOC2VEC_DBp.size()==0||map_LINE_DBp.size()==0||map_GOOGLE_DBp.size()==0) {
					System.out.println("Could not read the files DBpedia files");
					System.exit(1);
				}
				
				lstCats = new ArrayList<Article>(LabelsOfTheTexts.getLables_DBP_article().values());
				for(Article a : lstCats) {
					temp.put(a.getTitle(), 0.0);
				}
				if (map_DOC2VEC_DBp.containsKey(sentence)) {
					str_DOC2VEC = map_DOC2VEC_DBp.get(sentence).get(0).getTitle();
				}
				if (map_LINE_DBp.containsKey(sentence)) {
					str_LINE = map_LINE_DBp.get(sentence).get(0).getTitle();
				}
				if (map_GOOGLE_DBp.containsKey(sentence)) {
					str_GOOGLE = map_GOOGLE_DBp.get(sentence).get(0).getTitle();
				}
			}
			else if (dname.equals(Dataset.WEB_SNIPPETS)) {
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
			}
			String str = int_DOC2VEC+","+int_GOOGLE+","+int_LINE;
			return str;
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		return null;
	}
	public static Map<String, String> readLabelAssignment_AG(Dataset dname, EmbeddingModel model) {
		Map<String, String> result = new HashMap<String, String>();
		try {	
			String pathFolderLabelResult="/home/rtue/eclipse-workspace/gwifi/AG_LabelAssignement/";
			List<String> lines = FileUtils.readLines(new File(pathFolderLabelResult+"/"+"LabelAssignment_"+dname+"_"+model), "utf-8");
			for(String line : lines) {
				String[] split = line.split("\t");
				Article articleByTitle = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(split[1]);
				if (articleByTitle==null) {
					articleByTitle = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(split[1].split(": ")[1]);
				}
				result.put(split[0],articleByTitle.getTitle());
			}
			System.out.println("Size of label Ass "+model.name()+" "+result.size());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

}
