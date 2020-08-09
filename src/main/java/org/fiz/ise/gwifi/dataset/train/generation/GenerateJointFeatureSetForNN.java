package org.fiz.ise.gwifi.dataset.train.generation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.mapred.IFile;
import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.ReadDataset;
import org.fiz.ise.gwifi.dataset.assignLabels.AssignLabelsBasedOnConfVecSimilarity;
import org.fiz.ise.gwifi.dataset.category.CategoryFeaturesForNN;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.StringUtil;
import org.fiz.ise.gwifi.util.SynchronizedCounter;
import org.fiz.ise.gwifi.util.TimeUtil;
import org.fiz.ise.gwifi.util.VectorUtil;

import com.hp.hpl.jena.sparql.pfunction.library.str;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class GenerateJointFeatureSetForNN {

	private final static NLPAnnotationService service = AnnotationSingleton.getInstance().service;
	private static final String DATASET_AG_TRAIN_ANNOTATIONS = Config.getString("DATASET_AG_TRAIN_ANNOTATIONS","");//"/home/rtue/eclipse-workspace/Resources/Datasets/ag_news_csv/ag_sentence_cat_entities_redirect_resolved";
	private static final String DATASET_AG_TEST_ANNOTATIONS = Config.getString("DATASET_AG_TEST_ANNOTATIONS","");//"/home/rtue/eclipse-workspace/Resources/Datasets/ag_news_csv/ag_sentence_cat_entities_redirect_resolved";

	private static final String DATASET_DBP_TRAIN_ANNOTATIONS = Config.getString("DATASET_DBP_TRAIN_ANNOTATIONS","");//"/home/rtue/eclipse-workspace/Resources/Datasets/ag_news_csv/ag_sentence_cat_entities_redirect_resolved";
	private static final String DATASET_DBP_TEST_ANNOTATIONS = Config.getString("DATASET_DBP_TEST_ANNOTATIONS","");//"/home/rtue/eclipse-workspace/Resources/Datasets/ag_news_csv/ag_sentence_cat_entities_redirect_resolved";

	private static final String DATASET_TEST_AG=Config.getString("DATASET_TEST_AG","");
	private static final String DATASET_TEST_DBP=Config.getString("DATASET_DBP_TEST","");

	private static final String DATASET_TRAIN_SNIPPETS = Config.getString("DATASET_TRAIN_SNIPPETS","");
	private static final String DATASET_SNIPPETS_TRAIN_ANNOTATIONS = Config.getString("DATASET_SNIPPETS_TRAIN_ANNOTATIONS_2018","");
	private static final String DATASET_SNIPPETS_TEST_ANNOTATIONS = Config.getString("DATASET_SNIPPETS_TEST_ANNOTATIONS_2018","");

	private static SynchronizedCounter synCountNumberOfEntityPairs;
	private static ExecutorService executor;
	private final static Integer NUMBER_OF_THREADS= Config.getInt("NUMBER_OF_THREADS",-1);
	static Map<String, Integer> mapAllEnitityPairs = new ConcurrentHashMap<String, Integer>();
	//static Map<Integer, Integer> mapOverlappingPairs = new ConcurrentHashMap<Integer, Integer>();
	static Map<String, Integer> mapOverlappingPairsIndex = new ConcurrentHashMap<String, Integer>();
	private static Map<String, Integer> mapDataFromFileForIndexingMap_test=null;
	private static Map<String, Integer> mapDataFromFileForIndexingMap=null;
	static Set<String> set = new HashSet<String>();
	static Set<Integer> setCountElementEntCooc = new HashSet<Integer>();
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	//	static Set<String,> overlappingPairs = new HashSet<String>();

	public static void main(String[] args) throws Exception {

		System.out.println("Running GenerateJointFeatureSetForNN");

		GenerateJointFeatureSetForNN generete = new GenerateJointFeatureSetForNN();
		//generete.findAllPossibleEntityPairs(DATASET_SNIPPETS_TEST_ANNOTATIONS);
		//System.out.println("DATASET_SNIPPETS_TRAIN_ANNOTATIONS Train set all possible entity pairs:"+mapAllEnitityPairs.size());

		//FileUtil.writeDataToFile(mapAllEnitityPairs, "mapAllEnitityOccurance_snippets_test_filtered_black_and_numeric_2018.txt");

		//Map<String, Integer> sortByValueDescending = MapUtil.sortByValueAscendingGeneric(mapAllEnitityPairs);
		//System.out.println("mapAllEnitityOccurance train :"+mapAllEnitityPairs.size());
		//		System.out.println("mapOverlappingPairs:"+mapOverlappingPairs.size());
		//		FileUtil.writeDataToFile(sortByValueDescending, "mapAllEnitityOverlaping_ag_train_test_filtered_black_and_numeric");


		//		mapDataFromFileForIndexingMap_test = readDataFromFileForIndexingMap("mapAllEnitityOccurance_dbpedia_test_filtered_black_and_numeric_2018.txt", "\t");
		//		mapDataFromFileForIndexingMap = readDataFromFileForIndexingMap("mapAllEnitityOccurance_dbpedia_train_filtered_black_and_numeric_2018.txt", "\t");

		//		mapDataFromFileForIndexingMap_test = readDataFromFileForIndexingMap("mapAllEnitityOccurance_dbpedia_test_filtered_black_and_numeric_2018.txt", "\t");
		//		mapDataFromFileForIndexingMap = readDataFromFileForIndexingMap("mapAllEnitityOccurance_dbpedia_train_filtered_black_and_numeric_2018.txt", "\t");


		LINE_modelSingleton.getInstance();

/*		
		mapDataFromFileForIndexingMap_test = readDataFromFileForIndexingMap("mapAllEnitityOccurance_snippets_test_filtered_black_and_numeric_2018.txt", "\t");
		mapDataFromFileForIndexingMap = readDataFromFileForIndexingMap("mapAllEnitityOccurance_snippets_train_filtered_black_and_numeric_2018.txt", "\t");

		compare2EntityPairs(mapDataFromFileForIndexingMap,mapDataFromFileForIndexingMap_test );
		generateJointFeaturesCid_C_Eid_S_Lc_train(Dataset.WEB_SNIPPETS, DATASET_SNIPPETS_TRAIN_ANNOTATIONS);


				
		mapDataFromFileForIndexingMap_test = readDataFromFileForIndexingMap("mapAllEnitityOccurance_dbpedia_test_filtered_black_and_numeric_2018.txt", "\t");
		mapDataFromFileForIndexingMap = readDataFromFileForIndexingMap("mapAllEnitityOccurance_dbpedia_train_filtered_black_and_numeric_2018.txt", "\t");

		compare2EntityPairs(mapDataFromFileForIndexingMap,mapDataFromFileForIndexingMap_test );
		generateJointFeaturesCid_C_Eid_S_Lc_train(Dataset.DBpedia, DATASET_DBP_TRAIN_ANNOTATIONS);
			
*/
 
		mapDataFromFileForIndexingMap_test = readDataFromFileForIndexingMap("mapAllEnitityOccurance_ag_test_filtered_black_and_numeric_2018.txt", "\t");
		mapDataFromFileForIndexingMap = readDataFromFileForIndexingMap("mapAllEnitityOccurance_ag_train_filtered_black_and_numeric_2018.txt", "\t");

		compare2EntityPairs(mapDataFromFileForIndexingMap,mapDataFromFileForIndexingMap_test );
		generateJointFeaturesCid_C_Eid_S_Lc_train(Dataset.AG, DATASET_AG_TRAIN_ANNOTATIONS);

		System.out.println("Total overlapping pairs: "+mapOverlappingPairsIndex.size());
		System.out.println("Total overlapping pairs: "+mapOverlappingPairsIndex.size());

		//		FileUtil.writeDataToFile(mapOverlappingPairsIndex, "mapAllEnitityOverlaping_snippets_train_test_filtered_black_and_numeric_2018.txt");


		//		analyseCoocu();

		//generateJointFeaturesCid_C_Eid_S_Lc_test(Dataset.WEB_SNIPPETS_test, DATASET_SNIPPETS_TEST_ANNOTATIONS);
		//		generateJointFeaturesCid_C_Eid_S_Lc(Dataset.AG_test, DATASET_AG_TEST_ANNOTATIONS);
		////		System.out.println("*****countOnes*******: "+countOnes);
		////		FileUtil.writeDataToFile(setCountElementEntCooc, "setCountElementEntCooc.txt");

	}
	public static void analyseCooc() {
		int count_one_train=0;
		int count_one_test=0;
		int count_overlapping=0;
		int count_overlapping_one_train=0;
		int count_overlapping_one_test=0;
		int count_overlapping_one_train_test=0;

		for(Entry <String, Integer> e:mapDataFromFileForIndexingMap_test.entrySet() ) {
			if (e.getValue()==1) {
				count_one_test++;
			}
		}
		for(Entry <String, Integer> e:mapDataFromFileForIndexingMap.entrySet() ) {
			if (e.getValue()==1) {
				count_one_train++;
			}
		}
		System.out.println("count_one_train: "+count_one_train);
		System.out.println("count_one_test: "+count_one_test);

		for(Entry <String, Integer> e:mapDataFromFileForIndexingMap_test.entrySet() ) {
			if (mapDataFromFileForIndexingMap.containsKey(e.getKey())) {
				count_overlapping++;
			}
		}

		for(Entry <String, Integer> e:mapDataFromFileForIndexingMap_test.entrySet() ) {
			if (mapDataFromFileForIndexingMap.containsKey(e.getKey())&&mapDataFromFileForIndexingMap.get(e.getKey())==1) {
				count_overlapping_one_train++;
			}
		}
		for(Entry <String, Integer> e:mapDataFromFileForIndexingMap_test.entrySet() ) {
			if (mapDataFromFileForIndexingMap.containsKey(e.getKey())&&mapDataFromFileForIndexingMap_test.get(e.getKey())==1) {
				count_overlapping_one_test++;
			}
		}
		for(Entry <String, Integer> e:mapDataFromFileForIndexingMap_test.entrySet() ) {
			if (mapDataFromFileForIndexingMap.containsKey(e.getKey())&&mapDataFromFileForIndexingMap_test.get(e.getKey())==1&&mapDataFromFileForIndexingMap.get(e.getKey())==1) {
				count_overlapping_one_train_test++;
			}
		}
		System.out.println("count_overlapping: "+count_overlapping);
		System.out.println("count_overlapping_one_train: "+count_overlapping_one_train);
		System.out.println("count_overlapping_one_test: "+count_overlapping_one_test);
		System.out.println("count_overlapping_one_train_test: "+count_overlapping_one_train_test);
		System.out.println("mapOverlappingPairsIndex: "+mapOverlappingPairsIndex.size());

	}
	public static String getEntityVecMean(List<Article> lstentities) {
		List<String> lstEntityID = new ArrayList<String>();
		for(Article a : lstentities) {
			lstEntityID.add(String.valueOf(a.getId()));
		}
		double[] vector = VectorUtil.getSentenceVector(lstEntityID,LINE_modelSingleton.getInstance().lineModel);
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
	public static Map<String, Integer> readDataFromFileForIndexingMap(final String fileName, String separator) {
		Map<String, Integer> map = new LinkedHashMap<>();
		try (BufferedReader br = Files.newBufferedReader(Paths.get(fileName))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] split = line.split(separator);
				map.put(split[0]+"\t"+split[1],Integer.valueOf(split[2]));
			}
		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
		}
		System.out.println("Size of the map after reading "+map.size());
		return map;
	}
	public static void compare2EntityPairs(Map<String, Integer> map1, Map<String, Integer> map2) {
		System.out.println("Will iterate over: "+map2.size());
		int countOverlap=0;
		int index=0;
		for (Entry<String, Integer> e: map2.entrySet()) {
			if (map1.containsKey(e.getKey())) {
				//Integer key = e.getValue();
				//mapOverlappingPairs.merge(key, 1, Integer::sum);
				mapOverlappingPairsIndex.put(e.getKey(),index++);
				countOverlap++;
			}
		}
		System.out.println("Total overlapping pairs: "+countOverlap);
		System.out.println("Total overlapping pairs: "+mapOverlappingPairsIndex.size());
	}

	public static String getEntityCoocuranceFeature(List<Article> lstentities, Dataset dName) {
		StringBuilder strBuild = new StringBuilder();
		List<Integer> lstId = new ArrayList<Integer>();
		for(Article a : lstentities) {
			lstId .add(a.getId());
		}
		Collections.sort(lstId); 
		int count=0;
		for (int i = 0; i < lstId.size(); i++) {
			for (int j = i+1; j < lstId.size(); j++) {
				String key = lstId.get(i)+"\t"+lstId.get(j);
				if (dName.equals(Dataset.AG) || dName.equals(Dataset.DBpedia) 
						|| dName.equals(Dataset.AG_test) || dName.equals(Dataset.DBpedia_test)) {
					if (mapOverlappingPairsIndex.containsKey(key)&&mapDataFromFileForIndexingMap_test.get(key)==1
							&&mapDataFromFileForIndexingMap.get(key)==1) {
						String s=String.valueOf(lstId.get(i));
						String s2=String.valueOf(lstId.get(j));
						strBuild.append(s+"_"+s2+" ");
						count++;
					}
				}
				else if (dName.equals(Dataset.WEB_SNIPPETS)||dName.equals(Dataset.WEB_SNIPPETS_test)) {				
					if (mapOverlappingPairsIndex.containsKey(key)) {
						String s=String.valueOf(lstId.get(i));
						String s2=String.valueOf(lstId.get(j));
						strBuild.append(s+"_"+s2+" ");
						count++;
					}
				}
			}	
		}
		setCountElementEntCooc.add(count);
		return strBuild.toString().trim();
	}

	public static void generateJointFeaturesCid_C_Eid_S_Lc_train(Dataset dName,String fAnotationFile) throws Exception {

		int count=0;
		int countIgnoredLines=0;
		int countNoEntity=0;
		List<String> samples = new LinkedList<String>();
		List<String> catIds = new LinkedList<String>();
		List<String> catNames = new LinkedList<String>();
		List<String> entIDs = new LinkedList<String>();
		List<String> entVecMean = new LinkedList<String>();
		List<String> catVecMean = new LinkedList<String>();
		List<String> entCoOc = new LinkedList<String>();
		List<String> labels = new LinkedList<String>();
		StringBuilder strB = null;
		List<Article> lstentities = null;
		int countNullEnt=0;
		List<String> lines = FileUtils.readLines(new File(fAnotationFile));
		System.out.println("Size of the file for generating the joint features :"+lines.size());

		for(String line : lines) {
			String[] split = line.split("\t\t");
			if (split.length==3) {
				String sample = split[0];
				String entities = split[2];
				String[] splitEntity = entities.split("\t");
				lstentities = new ArrayList<Article>();
				strB = new StringBuilder();

				//Here you extract entities
				if (dName.equals(Dataset.AG)) {
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
				else if (dName.equals(Dataset.WEB_SNIPPETS)){
					for (int i = 0; i < splitEntity.length; i++) {
						Article a = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(splitEntity[i]); 
						if (a!=null && !AnnonatationUtil.getEntityBlackList_WebSnippets().contains(a.getId())&&!StringUtil.isNumeric(a.getTitle())) {
							lstentities.add(a);
							strB.append(a.getId()+" ");
						}
						else {
							countNullEnt++;
						}
					}
				}
				else if (dName.equals(Dataset.DBpedia)){ //Cooc entity pairs only once train and test 1,256,881
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
				String labelStr = AssignLabelsBasedOnConfVecSimilarity.getLabelBasedOnConfidence(dName, sample);
				//String labelStr = AssignLabelsBasedOnConfVecSimilarity.getBestLabelBasedOnConfidence_all_embeddings(dName, sample);
				String entityIDs = strB.toString().trim();

				String entityCooc = getEntityCoocuranceFeature(lstentities, dName); 

				String categoryIDs = CategoryFeaturesForNN.getCategoryIDs(lstentities);

				String categoryNames = CategoryFeaturesForNN.getCategoryNames(lstentities);
				String entityVector=getEntityVecMean(lstentities);

				String categoryVector=CategoryFeaturesForNN.getCategoryVec(lstentities);

				if (labelStr!=null&&!labelStr.contains("NaN")&&entityIDs.length()>1&&entityVector!=null&&categoryVector!=null) {
					entIDs.add(entityIDs);
					samples.add(sample);
					catIds.add(categoryIDs);
					catNames.add(categoryNames);
					labels.add(labelStr);
					entCoOc.add(entityCooc);
					entVecMean.add(entityVector);
					catVecMean.add(categoryVector);
				}
				else {
					//						System.exit(1);
					//						System.out.println();
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
		System.out.println("count Ignored Lines: "+countIgnoredLines);

		FileUtil.writeDataToFile(catIds, dName.name().toLowerCase()+"_train_joint_features_catIds",false);
		FileUtil.writeDataToFile(catNames, dName.name().toLowerCase()+"_train_joint_features_catNames",false);
		FileUtil.writeDataToFile(entIDs, dName.name().toLowerCase()+"_train_joint_features_entIDs",false);
		FileUtil.writeDataToFile(samples, dName.name().toLowerCase()+"_train_joint_features_samples",false);
		FileUtil.writeDataToFile(labels, dName.name().toLowerCase()+"_train_joint_features_labels",false);
		FileUtil.writeDataToFile(entCoOc, dName.name().toLowerCase()+"_train_joint_features_entCooc",false);
		FileUtil.writeDataToFile(entVecMean, dName.name().toLowerCase()+"_train_joint_features_entVecMean",false);
		FileUtil.writeDataToFile(catVecMean, dName.name().toLowerCase()+"_train_joint_features_catVecMean",false);
	}
	public static void generateJointFeaturesCid_C_Eid_S_Lc_test(Dataset dName,String fAnotationFile) throws Exception {
		int count=0;
		int countIgnoredLines=0;
		int countNoEntity=0;
		List<String> samples = new LinkedList<String>();
		List<String> catIds = new LinkedList<String>();
		List<String> catNames = new LinkedList<String>();
		List<String> entIDs = new LinkedList<String>();
		List<String> entVecMean = new LinkedList<String>();
		List<String> catVecMean = new LinkedList<String>();
		List<String> entCoOc = new LinkedList<String>();
		List<String> labels = new LinkedList<String>();
		StringBuilder strB = null;
		List<Article> lstentities = null;
		int countNullEnt=0;
		List<String> lines = FileUtils.readLines(new File(fAnotationFile));
		System.out.println("Size of the file for generating the joint features: "+lines.size());

		for(String line : lines) {
			String[] split = line.split("\t\t");
			if (split.length==3) {
				String sample = split[0];
				String entities = split[2];
				String[] splitEntity = entities.split("\t");
				lstentities = new ArrayList<Article>();
				strB = new StringBuilder();

				if (dName.equals(Dataset.WEB_SNIPPETS_test) || dName.equals(Dataset.WEB_SNIPPETS)){
					for (int i = 0; i < splitEntity.length; i++) {
						Article a = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(splitEntity[i]); 
						if (a!=null && !AnnonatationUtil.getEntityBlackList_WebSnippets().contains(a.getId())&&!StringUtil.isNumeric(a.getTitle())) {
							lstentities.add(a);
							strB.append(a.getId()+" ");
						}
						else {
							countNullEnt++;
						}
					}
				}

				if (dName.equals(Dataset.DBpedia_test) || dName.equals(Dataset.DBpedia)) {
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
				else if (dName.equals(Dataset.AG_test) || dName.equals(Dataset.AG)) {
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
				String entityIDs = strB.toString().trim();

				String entityCooc = getEntityCoocuranceFeature(lstentities,dName); 

				String categoryIDs = CategoryFeaturesForNN.getCategoryIDs(lstentities);

				String categoryNames = CategoryFeaturesForNN.getCategoryNames(lstentities);

				String labelStr = AssignLabelsBasedOnConfVecSimilarity.getOneHotEncodingLabel(dName, sample);

				String entityVector=getEntityVecMean(lstentities);

				String categoryVector=CategoryFeaturesForNN.getCategoryVec(lstentities);

				if (labelStr!=null&&entityIDs.length()>1&&entityVector!=null&&categoryVector!=null&&categoryIDs.length()>1) {
					entIDs.add(entityIDs);
					samples.add(sample);
					catIds.add(categoryIDs);
					catNames.add(categoryNames);
					labels.add(labelStr);
					entCoOc.add(entityCooc);
					entVecMean.add(entityVector);
					catVecMean.add(categoryVector);
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
		System.out.println("count Ignored Lines: "+countIgnoredLines);

		FileUtil.writeDataToFile(catIds, dName.name().toLowerCase()+"_train_original_joint_features_catIds",false);
		FileUtil.writeDataToFile(catNames, dName.name().toLowerCase()+"_train_original_joint_features_catNames",false);
		FileUtil.writeDataToFile(entIDs, dName.name().toLowerCase()+"_train_original_joint_features_entIDs",false);
		FileUtil.writeDataToFile(samples, dName.name().toLowerCase()+"_train_original_joint_features_samples",false);
		FileUtil.writeDataToFile(labels, dName.name().toLowerCase()+"_train_original_joint_features_labels",false);
		FileUtil.writeDataToFile(entCoOc, dName.name().toLowerCase()+"_train_original_joint_features_entCooc",false);

		FileUtil.writeDataToFile(entVecMean, dName.name().toLowerCase()+"_train_original_joint_features_entVecMean",false);
		FileUtil.writeDataToFile(catVecMean, dName.name().toLowerCase()+"_train_original_joint_features_catVecMean",false);

		//		FileUtil.writeDataToFile(catIds, dName.name().toLowerCase()+"_joint_features_catIds",false);
		//		FileUtil.writeDataToFile(catNames, dName.name().toLowerCase()+"_joint_features_catNames",false);
		//		FileUtil.writeDataToFile(entIDs, dName.name().toLowerCase()+"_joint_features_entIDs",false);
		//		FileUtil.writeDataToFile(samples, dName.name().toLowerCase()+"_joint_features_samples",false);
		//		FileUtil.writeDataToFile(labels, dName.name().toLowerCase()+"_joint_features_labels",false);
		//		FileUtil.writeDataToFile(entCoOc, dName.name().toLowerCase()+"_joint_features_entCooc",false);
		//
		//		FileUtil.writeDataToFile(entVecMean, dName.name().toLowerCase()+"_joint_features_entVecMean",false);
		//		FileUtil.writeDataToFile(catVecMean, dName.name().toLowerCase()+"_joint_features_catVecMean",false);

	}
	private  void findAllPossibleEntityPairs(String fAnotationFile) throws Exception {
		synCountNumberOfEntityPairs= new SynchronizedCounter();
		executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
		List<String> lines = FileUtils.readLines(new File(fAnotationFile));
		System.out.println("Size of the file :"+lines.size());
		List<Article> lstentities = null;
		int countNullEnt=0;
		for(String line : lines) {
			String[] split = line.split("\t\t");
			if (split.length==3) {
				String entities = split[2];
				String[] splitEntity = entities.split("\t");
				lstentities = new ArrayList<Article>();
				for (int i = 0; i < splitEntity.length; i++) {
					Article a = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(splitEntity[i]); 
					if (a!=null) {
						lstentities.add(a);
					}
					else {
						countNullEnt++;
					}
				}
				executor.execute(handleEntitiyPairs(lstentities));
			}
		}
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		System.out.println("Null entity: "+countNullEnt);

	}
	private Runnable handleEntitiyPairs(List<Article> lstentities)  {
		return () -> {
			try {
				List<Integer> lstId = new ArrayList<Integer>();

				for(Article a : lstentities) {
					if (!StringUtil.isNumeric(a.getTitle())) {
						lstId.add(a.getId());
					}
				}
				Collections.sort(lstId); 
				for (int i = 0; i < lstId.size(); i++) {
					for (int j = i+1; j < lstId.size(); j++) {

						//if (lstId.get(i)!=lstId.get(j)) {
						String key = lstId.get(i)+"\t"+lstId.get(j);
						//mapAllThePairs.put(lstId.get(i)+"\t"+lstId.get(j), 0);

						synCountNumberOfEntityPairs.increment();
						mapAllEnitityPairs.merge(key, 1, Integer::sum);

						//}

					}	
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		};
	}
}
