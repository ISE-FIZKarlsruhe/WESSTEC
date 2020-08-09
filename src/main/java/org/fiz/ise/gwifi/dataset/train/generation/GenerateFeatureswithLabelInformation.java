package org.fiz.ise.gwifi.dataset.train.generation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LabelsOfTheTexts;
import org.fiz.ise.gwifi.dataset.ReadDataset;
import org.fiz.ise.gwifi.dataset.assignLabels.AssignLabelsBasedOnConfVecSimilarity;
import org.fiz.ise.gwifi.dataset.category.CategoryFeaturesForNN;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.model.EmbeddingModel;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.StringUtil;
import org.fiz.ise.gwifi.util.VectorUtil;

import edu.kit.aifb.gwifi.model.Article;

public class GenerateFeatureswithLabelInformation {

	private static ExecutorService executor;
	static final Logger secondLog = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");
	private final static Integer NUMBER_OF_THREADS= Config.getInt("NUMBER_OF_THREADS",-1);
	private static final Map<Article, String> label_Vectors = new HashMap<Article, String>();
	private static Dataset dset=null;

	static {
		List<Article> lstlabelsDataset= null;
		dset=Dataset.AG;
		LINE_modelSingleton.getInstance();

		if (dset.equals(Dataset.DBpedia)||dset.equals(Dataset.DBpedia_test)){
			lstlabelsDataset=new ArrayList<Article>(LabelsOfTheTexts.getLables_DBP_article().values());

			for (Article a : lstlabelsDataset) {
				if (a.getTitle().contains("holder")) {
					a=WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Politician");
				}
				String str_labelVec = getLabelVector(Arrays.asList(a));
				if (str_labelVec != null && !str_labelVec.isEmpty()) {
					label_Vectors.put(a,str_labelVec);
				}
				else {
					System.out.println(a+" vecor is null");
					System.exit(1);
				}
			}
		}
		else if (dset.equals(Dataset.AG)||dset.equals(Dataset.AG_test)){
			lstlabelsDataset=new ArrayList<Article>(LabelsOfTheTexts.getLables_AG_article().values());
			for (Article a : lstlabelsDataset) {
				String str_labelVec = getLabelVector(Arrays.asList(a));
				if (str_labelVec != null && !str_labelVec.isEmpty()) {
					label_Vectors.put(a,str_labelVec);
				}
				else {
					System.out.println(a+" vecor is null");
					System.exit(1);
				}
			}
		}
	}
	public static void main(String[] args) throws Exception {
		
		int nNegativeSamples=0;
		System.out.println("Number of negative samples: "+nNegativeSamples);
		if (dset.equals(Dataset.DBpedia)||dset.equals(Dataset.DBpedia_test)) {
			nNegativeSamples=13;
			generateFeatureswithLabelsWithNegativeSampling(dset,Config.getString("DATASET_DBP_TRAIN_ANNOTATIONS",""), nNegativeSamples);
		}
		else if (dset.equals(Dataset.AG)||dset.equals(Dataset.AG_test)) {
			nNegativeSamples=3;
			generateFeatureswithLabelsWithNegativeSampling(dset,Config.getString("DATASET_AG_TRAIN_ANNOTATIONS",""), nNegativeSamples);
//			generateFeaturesWithLabelInformation(dset,Config.getString("DATASET_AG_TEST_ANNOTATIONS",""));
//			generateFeaturesWithLabelInformation(dset,Config.getString("DATASET_AG_TRAIN_ANNOTATIONS",""));
		}

		//generateFeaturesForBinaryClassification_paralel(dset, Config.getString("DATASET_DBP_TEST_ANNOTATIONS",""));

		//		ArrayList<Article> labels = new ArrayList<Article>(LabelsOfTheTexts.getLables_DBP_article().values());
		//		
		//		for(Article a : labels) {
		//			System.out.println(a+" "+LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(a.getId())));
		//		}
		//		
		//		List<String> lines = FileUtils.readLines(new File(Config.getString("DATASET_DBP_TRAIN_ANNOTATIONS","")));
		//
		//		for(String line : lines) {
		//			String[] split = line.split("\t\t");
		//			if (split.length==3) {
		//				String str_label = split[1];
		//				if (str_label.contains("holder")) {
		//					str_label="Politician";
		//				}
		//				Article label = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str_label);
		//				if (!LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(label.getId()))) {
		//					System.out.println(str_label);
		//					System.out.println(label);
		//					System.out.println(LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(label.getId())));
		//					System.exit(1);
		//				}
		//			}
		//		}
	}
	
	
	private static  void generateLabelVectorFile(Dataset dName) {
		List<Article> lstlabelsDataset= null;
		if (dName.equals(Dataset.DBpedia)||dName.equals(Dataset.DBpedia_test)){
			lstlabelsDataset=new ArrayList<Article>(LabelsOfTheTexts.getLables_DBP_article().values());
		}
		else if (dName.equals(Dataset.AG) || dName.equals(Dataset.AG_test)) {
			lstlabelsDataset=new ArrayList<Article>(LabelsOfTheTexts.getLables_AG_article().values());
		}	
		for(Article a : lstlabelsDataset) {
			if (dName.equals(Dataset.DBpedia)||dName.equals(Dataset.DBpedia_test)&&a.getTitle().contains("holder")) {
				a=WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Politician");
			}
			String labelVector=getLabelVector(Arrays.asList(a));
			secondLog.info(a.getTitle()+"\t"+labelVector);
		}
		
	}
	private static  void generateFeaturesForBinaryClassification_paralel(Dataset dName,String fAnotationFile) throws IOException, InterruptedException {
		executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
		List<String> lines = FileUtils.readLines(new File(fAnotationFile));
		System.out.println("Size of the file for generating the features with labels :"+lines.size());
		for (int i = 0; i < lines.size(); i++) {
			executor.execute(logFeatures(dName, lines.get(i), i));
		}
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
	}
	private static Runnable logFeatures(Dataset dName,String line, int j ) {
		return () -> {
			List<Article> lstlabelsDataset= null;
			String[] split = line.split("\t\t");
			if (split.length==3) {
				//TODO change the label according to data set might be multiple label
				String str_label = split[1];
				Article label =null;
				String entities = split[2];
				String[] splitEntity = entities.split("\t");
				List<Article> lstentities = new ArrayList<Article>();
				StringBuilder strB = new StringBuilder();

				//if else clauses only for extraction of the entities
				if (dName.equals(Dataset.AG) || dName.equals(Dataset.AG_test)) {
					for (int i = 0; i < splitEntity.length; i++) {
						Article a = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(splitEntity[i]); 
						if (a!=null && !AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())&&!StringUtil.isNumeric(a.getTitle())) {
							lstentities.add(a);
							strB.append(a.getId()+" ");
						}
					}
				}					
				else if (dName.equals(Dataset.WEB_SNIPPETS)||dName.equals(Dataset.WEB_SNIPPETS_test)){
					for (int i = 0; i < splitEntity.length; i++) {
						Article a = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(splitEntity[i]); 
						if (a!=null && !AnnonatationUtil.getEntityBlackList_WebSnippets().contains(a.getId())&&!StringUtil.isNumeric(a.getTitle())) {
							lstentities.add(a);
							strB.append(a.getId()+" ");
						}
					}
				}
				else if (dName.equals(Dataset.DBpedia)||dName.equals(Dataset.DBpedia_test)){
					lstlabelsDataset=new ArrayList<Article>(LabelsOfTheTexts.getLables_DBP_article().values());
					lstlabelsDataset.remove(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str_label));
					if (str_label.contains("holder")) {
						str_label="Politician";
					}

					label = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str_label);
					for (int i = 0; i < splitEntity.length; i++) {
						Article a = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(splitEntity[i]);
						if (a!=null) {
							int cleanAnnotation =AnnonatationUtil.getCorrectAnnotation_DBp(a.getId());
							Article article= WikipediaSingleton.getInstance().wikipedia.getArticleById(cleanAnnotation);
							if (article!=null&&!AnnonatationUtil.getEntityBlackList_DBp().contains(article.getId())&&!StringUtil.isNumeric(article.getTitle())) {
								lstentities.add(a);
								strB.append(a.getId()+" ");
							}
						}
					}
				}
				String entityVector=GenerateJointFeatureSetForNN.getEntityVecMean(lstentities);
				String labelVector=getLabelVector(Arrays.asList(label));

				if (label!=null&&entityVector!=null) {
					secondLog.info(entityVector+","+labelVector);
					resultLog.info("1");
					for(Article a : lstlabelsDataset) {
						//					 Random rand = new Random(); 
						//					 Article a= lstlabelsDataset.get(rand.nextInt(lstlabelsDataset.size())); 	
						if (a.getTitle().contains("holder")) {
							a=WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Politician");
						}
						entityVector=GenerateJointFeatureSetForNN.getEntityVecMean(lstentities);
						labelVector=getLabelVector(Arrays.asList(a));
						secondLog.info(entityVector+","+labelVector);
						resultLog.info("0");
					}
				}
				System.out.println("Lines are processed: "+j);
			}
		};
	}
	
	private static void generateFeaturesWithLabelInformation(Dataset dName,String fAnotationFile) throws IOException {
		int count=0;
		int countIgnoredLines=0;
		int countNoEntity=0;
		String fName ="train_";
		if (dName.toString().contains("test")) {
			fName="";
		}
		List<String> samples = new LinkedList<String>();
		List<String> entVecMean = new LinkedList<String>();
		List<String> labels = new LinkedList<String>();
		List<String> labelVecs = new LinkedList<String>();

		StringBuilder strB = null;
		List<Article> lstentities = null;

		int countNullEnt=0;
		List<String> lines = FileUtils.readLines(new File(fAnotationFile));
		System.out.println("Size of the file for generating the features with labels :"+lines.size());

		for(String line : lines) {
			String[] split = line.split("\t\t");
			if (split.length==3) {
				String sample = split[0];
				//TODO change the label according to data set might be multiple label
				String str_label = split[1];
				Article label =null;
				String entities = split[2];
				String[] splitEntity = entities.split("\t");
				lstentities = new ArrayList<Article>();
				strB = new StringBuilder();

				//if else clauses only for extraction of the entities
				if (dName.equals(Dataset.AG) || dName.equals(Dataset.AG_test)) {
					label = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str_label);
					for (int i = 0; i < splitEntity.length; i++) {
						Article a = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(splitEntity[i]); 
						if (a!=null && !AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())&&!StringUtil.isNumeric(a.getTitle())) {
							lstentities.add(a);
							strB.append(a.getId()+" ");
						}
						else {
							countNullEnt++;
						}
					}
				}					
				else if (dName.equals(Dataset.DBpedia)||dName.equals(Dataset.DBpedia_test)){
					if (str_label.contains("holder")) {
						str_label="Politician";
					}

					label = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str_label);
					for (int i = 0; i < splitEntity.length; i++) {
						Article a = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(splitEntity[i]);
						if (a!=null) {
							int cleanAnnotation =AnnonatationUtil.getCorrectAnnotation_DBp(a.getId());
							Article article= WikipediaSingleton.getInstance().wikipedia.getArticleById(cleanAnnotation);
							if (article!=null&&!AnnonatationUtil.getEntityBlackList_DBp().contains(article.getId())&&!StringUtil.isNumeric(article.getTitle())) {
								lstentities.add(a);
								strB.append(a.getId()+" ");
							}
							else {
								countNullEnt++;
							}
						}
					}
				}
				String entityVector=GenerateJointFeatureSetForNN.getEntityVecMean(lstentities);
				String labelVector=label_Vectors.get(label);//getLabelVector(Arrays.asList(label));

				if (labelVector!=null&&entityVector!=null) {
					samples.add(sample);
					entVecMean.add(entityVector);
					labelVecs.add(label_Vectors.get((label)));
					labels.add(label.getTitle());
				}
				else {
					countIgnoredLines++;
				}
				System.out.println("Lines are processed: "+count++);
				System.out.println("Lines are ignored: "+countIgnoredLines);
			}
			else {
				countNoEntity++;
			}
		}
		System.out.println("No entity:"+countNoEntity);
		System.out.println("Count null entites: "+countNullEnt);
		System.out.println("count ignored Lines: "+countIgnoredLines);

		FileUtil.writeDataToFile(samples, dName.name().toLowerCase()+"_"+fName+"label_features_samples",false);
		FileUtil.writeDataToFile(labels, dName.name().toLowerCase()+"_"+fName+"label_features_labels",false);
		FileUtil.writeDataToFile(labelVecs, dName.name().toLowerCase()+"_"+fName+"label_features_labelVec",false);
		FileUtil.writeDataToFile(entVecMean, dName.name().toLowerCase()+"_"+fName+"label_features_entVecMean",false);
	}

	private static void generateFeatureswithLabelsWithNegativeSampling(Dataset dName,String fAnotationFile, int nNegativeSamples) throws IOException {
		int count=0;
		int countIgnoredLines=0;
		int countNoEntity=0;
		String fName ="train_";
		if (dName.toString().contains("test")) {
			fName="";
		}
		List<String> samples = new LinkedList<String>();
		List<String> entVecMean = new LinkedList<String>();
		List<String> labels = new LinkedList<String>();
		List<String> labelVecs = new LinkedList<String>();

		StringBuilder strB = null;
		List<Article> lstentities = null;
		List<Article> lstlabelsDataset= null;

		int countNullEnt=0;
		List<String> lines = FileUtils.readLines(new File(fAnotationFile));
		System.out.println("Size of the file for generating the features with labels :"+lines.size());

		for(String line : lines) {
			String[] split = line.split("\t\t");
			if (split.length==3) {
				String sample = split[0];
				//TODO change the label according to dataset
				String str_label = split[1];
				Article a_label =null;
				String entities = split[2];
				String[] splitEntity = entities.split("\t");
				lstentities = new ArrayList<Article>();
				strB = new StringBuilder();

				//if else clauses only for extraction of the entities
				if (dName.equals(Dataset.AG) || dName.equals(Dataset.AG_test)) {
					lstlabelsDataset=new ArrayList<Article>(LabelsOfTheTexts.getLables_AG_article().values());
					lstlabelsDataset.remove(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str_label));

					if (lstlabelsDataset.size()+1!=LabelsOfTheTexts.getLables_AG_article().values().size()) {
						System.out.println("The element could not be removed "+ str_label);
						System.exit(1);
					}
					for (int i = 0; i < splitEntity.length; i++) {
						Article a = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(splitEntity[i]); 
						if (a!=null && !AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())&&!StringUtil.isNumeric(a.getTitle())) {
							lstentities.add(a);
							strB.append(a.getId()+" ");
						}
						else {
							countNullEnt++;
						}
					}
				}					
				else if (dName.equals(Dataset.DBpedia)||dName.equals(Dataset.DBpedia_test)){
					lstlabelsDataset=new ArrayList<Article>(LabelsOfTheTexts.getLables_DBP_article().values());
					lstlabelsDataset.remove(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str_label));

					if (lstlabelsDataset.size()+1!=LabelsOfTheTexts.getLables_DBP_article().values().size()) {
						System.out.println("The element could not be removed "+ str_label);
						System.exit(1);
					}
					if (str_label.contains("holder")) {
						str_label="Politician";
					}
					for (int i = 0; i < splitEntity.length; i++) {
						Article a = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(splitEntity[i]);
						if (a!=null) {
							int cleanAnnotation =AnnonatationUtil.getCorrectAnnotation_DBp(a.getId());
							Article article= WikipediaSingleton.getInstance().wikipedia.getArticleById(cleanAnnotation);
							if (article!=null&&!AnnonatationUtil.getEntityBlackList_DBp().contains(article.getId())&&!StringUtil.isNumeric(article.getTitle())) {
								lstentities.add(a);
								strB.append(a.getId()+" ");
							}
							else {
								countNullEnt++;
							}
						}

					}
				}
				a_label = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str_label);
				String entityVector=GenerateJointFeatureSetForNN.getEntityVecMean(lstentities);
				String labelVector=label_Vectors.get(a_label);//getLabelVector(Arrays.asList(a_label));

				if (a_label!=null&&entityVector!=null) {
					samples.add(sample);
					labels.add(a_label.getTitle());
					entVecMean.add(entityVector);
					labelVecs.add(labelVector);
					resultLog.info("1");

					ArrayList<Article> temp_list = new ArrayList<>(lstlabelsDataset);
					Collections.shuffle(temp_list);
					List<Article> random_list = temp_list.subList(0, nNegativeSamples);

					for (int i = 0; i < random_list.size(); i++) {
						Article rand_a= random_list.get(i);

						if (rand_a.getId()==a_label.getId()) {
							System.out.println("The random and the correct labels are the same");
							System.exit(1);
						}	
						if (dName.equals(Dataset.DBpedia)&&rand_a.getTitle().contains("holder")) {
							rand_a=WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Politician");
						}

						samples.add(sample);
						labels.add(rand_a.getTitle());
						entVecMean.add(entityVector);
						labelVecs.add(label_Vectors.get(rand_a));
						resultLog.info("0");
					}
				}
				else {
					countIgnoredLines++;
				}
				System.out.println("Lines are processed: "+count++);
				System.out.println("Lines are ignored: "+countIgnoredLines);
			}
			else {
				countNoEntity++;
			}
		}
		System.out.println("No entity:"+countNoEntity);
		System.out.println("Count null entites: "+countNullEnt);
		System.out.println("Count ignored lines: "+countIgnoredLines);

		FileUtil.writeDataToFile(samples, dName.name().toLowerCase()+"_"+fName+"label_features_samples",false);
		FileUtil.writeDataToFile(labels, dName.name().toLowerCase()+"_"+fName+"label_features_labels",false);
		FileUtil.writeDataToFile(labelVecs, dName.name().toLowerCase()+"_"+fName+"label_features_labelVec",false);
		FileUtil.writeDataToFile(entVecMean, dName.name().toLowerCase()+"_"+fName+"label_features_entVecMean",false);
	}

	public static String getLabelVector(List<Article> lstCats) {
		List<String> lstLabelID = new ArrayList<String>();
		for(Article a : lstCats) {
			lstLabelID.add(String.valueOf(a.getId()));
		}
		double[] vector = VectorUtil.getSentenceVector(lstLabelID,LINE_modelSingleton.getInstance().lineModel);
		if (vector==null || vector.length==0) {
			return null;
		}
		else {
			String strVector="";
			for (int j = 0; j < vector.length; j++) {
				strVector=strVector+(String.valueOf(vector[j]) + ",");
			}
			strVector = strVector.substring(0, strVector.length() - 1);
			return strVector;
		}
	}
}
