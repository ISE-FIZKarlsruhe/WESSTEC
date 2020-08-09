package org.fiz.ise.gwifi.dataset;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.GoogleModelSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.assignLabels.AssignLabelsBasedOnConfVecSimilarity;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.dataset.train.generation.GenerateDatasetForNN;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.model.EmbeddingModel;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.StringUtil;
import org.fiz.ise.gwifi.util.TimeUtil;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class AnalyseDBPediaDataset {
	private static final String DATASET_DBP_TRAIN = Config.getString("DATASET_DBP_TRAIN","");
	private static final String DATASET_DBP_TRAIN_CATEGORIZED_LINE = Config.getString("DATASET_DBP_TRAIN_CATEGORIZED_LINE","");
	private static final String DATASET_DBP_TRAIN_CATEGORIZED_D2Vec = Config.getString("DATASET_DBP_TRAIN_CATEGORIZED_D2Vec","");
	private static final String DATASET_DBP_TRAIN_CATEGORIZED_GOOGLE = Config.getString("DATASET_DBP_TRAIN_CATEGORIZED_GOOGLE","");
	private static final String DATASET_DBP_TRAIN_ANNOTATIONS = Config.getString("DATASET_DBP_TRAIN_ANNOTATIONS","");

	private static final String DATASET_DBP_TEST = Config.getString("DATASET_DBP_TEST","");
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	public static  List<Article> lstFilter;// = new ArrayList<Article>(
	//		      Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Natural environment"),
	//		    		  WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Building")));
	public static void main(String[] args) throws Exception {
		long start = TimeUtil.getStart();
	
		List<String> lst_data = new ArrayList<String>(ReadDataset.read_dataset_DBPedia_SampleLabel(DATASET_DBP_TEST).keySet());
		System.out.println("Total data size: "+lst_data.size());
		
		List<Annotation> lst_annotation = AnnonatationUtil.findAnnotationAll(lst_data);
		System.out.println("Size of all the annotations: "+lst_annotation.size());
		
		System.out.println("Avg annotation: "+lst_annotation.size()*1.0/lst_data.size()*1.0);
		
		AnalyseDataset.printAvgNumberOfWordsOfDatasets(lst_data);
		
		
		for(Article a : lstFilter) {
			//			findWordFreq(a);
			//			List<String> read_dataset_DBP_BasedOnLabel = ReadDataset.read_dataset_DBP_BasedOnLabel(DATASET_DBP_TRAIN, a);
			//			AnalyseDataset.findMostSimilarWordsForDatasetBasedOnDatasetVector(read_dataset_DBP_BasedOnLabel,"dbpedia_"+a.getTitle()+"mostsimilarwords_datasetVec.txt");
		}

		//				AnalyseDataset.compareTwoFiles(Dataset.DBpedia, DATASET_DBP_TRAIN, DATASET_DBP_TRAIN_CATEGORIZED_LINE);
		//				AnalyseDataset.compareTwoFiles(Dataset.DBpedia, DATASET_DBP_TRAIN, DATASET_DBP_TRAIN_CATEGORIZED_GOOGLE);
		//				AnalyseDataset.compareTwoFiles_d2vec(Dataset.DBpedia, DATASET_DBP_TRAIN, DATASET_DBP_TRAIN_CATEGORIZED_D2Vec);


		//		AssignLabelsBasedOnVecSimilarity assign = new AssignLabelsBasedOnVecSimilarity();
		//		assign.obtainLabelForEachSample(Dataset.DBpedia, EmbeddingModel.GOOGLE, new ArrayList<Article>(LabelsOfTheTexts.getLables_DBP_article().values()));
		//		Article a =LabelsOfTheTexts.getLables_DBP_article().get(6);
		//		findWordFreq(a); 


		//		writeAnnotationsToFile();
		//		categorizeDataset(Dataset.DBpedia, EmbeddingModel.GOOGLE, DATASET_DBP_TRAIN,false, lstFilter);
		System.out.println("Total time minutes " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - start));

	}
	private static void findWordFreq(Article a) {
		System.out.println("Analysing:"+a.getTitle()+"*************************************");
		String folder="/home/rima/playground/GeneralFiles/gwifi/Dataset_ShortTextClassification/dbpedia_csv/dbp_word_freq/";
		List<String> dataset_train = ReadDataset.read_dataset_DBP_BasedOnLabel(DATASET_DBP_TRAIN,a );
		AnalyseDataset.findFreqOfWord(dataset_train, folder+"dbp_freq_word_"+a.getTitle());
	}

	private static void findMostSimilarCategory() {


		System.out.println("Analaysing DBpedia dataset");
		//		Map<Integer, Article> lables_DBP_article2 = LabelsOfTheTexts.getLables_Yahoo_article();
		//		for(Entry<Integer, Article> e: lables_DBP_article2.entrySet()) {
		//			System.out.println(e.getKey()+" "+e.getValue());
		//		}
		Map<String, List<Article>> dataset_DBP_train = ReadDataset.read_dataset_DBPedia_SampleLabel(DATASET_DBP_TRAIN);
		Map<String, List<Article>> dataset_DBP_test = ReadDataset.read_dataset_DBPedia_SampleLabel(DATASET_DBP_TEST);
		System.out.println("Size of the train:"+dataset_DBP_train.size());
		System.out.println("Size of the test:"+dataset_DBP_test.size());


		Set<Integer> lables_dbp_article = LabelsOfTheTexts.getLables_DBP_article().keySet();

		int countTotalAnnTrain=0;
		int countTotalAnnTest=0;
		String folderName="dbp_train_most_sim_entities";
		for(int i : lables_dbp_article) {
			Long start =TimeUtil.getStart();
			Article a =LabelsOfTheTexts.getLables_DBP_article().get(i);
			System.out.println("Analysing:"+a.getTitle()+"*************************************");
			List<String> dataset_train = ReadDataset.read_dataset_DBP_BasedOnLabel(DATASET_DBP_TRAIN,a );
			//			List<Annotation> findAnnotationAll_train = new ArrayList<Annotation>(AnnonatationUtil.findAnnotationAll(dataset_train));
			//			List<Annotation> findAnnotationAll_test = new ArrayList<Annotation>(AnnonatationUtil.findAnnotationAll(ReadDataset.read_dataset_DBP_BasedOnLabel(DATASET_DBP_TEST, a)));
			//			secondLOG.info("Annotation countAllAnnotation_train: "+a.getTitle()+": "+findAnnotationAll_train.size());
			//			secondLOG.info("Annotation countAllAnnotation_test: "+a.getTitle()+": "+findAnnotationAll_test.size());
			//
			//			countTotalAnnTrain+=findAnnotationAll_train.size();
			//			countTotalAnnTest+=findAnnotationAll_test.size();

			//			AnnonatationUtil.findFreqOfAnnotation(findAnnotationAll_train, "dbp_train_annotation_freq"+LabelsOfTheTexts.getLables_DBP_article().get(i));
			//			AnnonatationUtil.findFreqOfAnnotation(findAnnotationAll_test, "dbp_test_annotation_freq"+LabelsOfTheTexts.getLables_DBP_article().get(i));

			//			AnalyseDataset.findMostSimilarEntitesForDatasetBasedOnDatasetVector(findAnnotationAll_train, "dbp_train_most_sim_ent"+a.getTitle());
			//			AnalyseDataset.findMostSimilarEntitesForDatasetBasedOnDatasetVector(findAnnotationAll_test, "dbp_test_most_sim_ent"+a.getTitle());

			AnalyseDataset.findMostSimilarEntitesForDataset(dataset_train, Dataset.DBpedia, folderName+"/dbp_train_most_sim_ent_each_ann"+LabelsOfTheTexts.getLables_DBP_article().get(i).getTitle());
			System.out.println(a.getTitle()+"-Finished-dbp-seconds time : "+TimeUtil.getEnd(TimeUnit.MILLISECONDS, start)/ 1000F);
		}
		secondLOG.info("Dbp Dataset");
		secondLOG.info("Size of the train:"+dataset_DBP_train.size());
		secondLOG.info("Size of the test:"+dataset_DBP_test.size());
		secondLOG.info("countTotalAnnTrain:"+countTotalAnnTrain);
		secondLOG.info("countTotalAnnTest:"+countTotalAnnTest);
	}

	public static void categorizeDataset(Dataset dName, EmbeddingModel model, String fName, boolean filter, List<Article> listFilter) {
		Map<String, List<String>> map_annotations_sentences=null;
		if (dName.equals(Dataset.DBpedia)) {
			if (filter) {
				System.out.println("Start Categorizing DBpedia dataset filtering");
				List<Article> lstArticles = new ArrayList<Article>(LabelsOfTheTexts.getLables_DBP_article().values());
				for(Article a : listFilter) {
					lstArticles.remove(a);
				}
				System.out.println("Size of the filtered list:"+lstArticles.size());
				System.out.println(lstArticles);
				map_annotations_sentences = read_annotations_sentences_filter(Dataset.DBpedia, DATASET_DBP_TRAIN_ANNOTATIONS,lstArticles);

			}
			else {
				map_annotations_sentences = read_annotations_sentences(Dataset.DBpedia, DATASET_DBP_TRAIN_ANNOTATIONS);
				lstFilter = new ArrayList<Article>(LabelsOfTheTexts.getLables_DBP_article().values());
			}
		}
		else if (dName.equals(Dataset.WEB_SNIPPETS)) {
			System.out.println("Start Categorizing Web Snippets dataset:"+fName);

			map_annotations_sentences = read_annotations_sentences(Dataset.WEB_SNIPPETS, fName);
			lstFilter = new ArrayList<Article>(Categories.getLabels_Snippets());
		}
		GenerateDatasetForNN generate = new GenerateDatasetForNN();
		if (model.equals(EmbeddingModel.GOOGLE)&&dName.equals(Dataset.WEB_SNIPPETS)) {
			System.out.println("The name of the dataset is "+Dataset.WEB_SNIPPETS.name()+" the model: "+EmbeddingModel.GOOGLE.name());
			GoogleModelSingleton.getInstance();
			generate.labelTrainSetParalel(EmbeddingModel.GOOGLE, Dataset.WEB_SNIPPETS,lstFilter);
		}
		if (model.equals(EmbeddingModel.LINE_Ent_Ent)&&dName.equals(Dataset.WEB_SNIPPETS)) {
			LINE_modelSingleton.getInstance();
			generate.labelAnnotatedTrainSetParalel(EmbeddingModel.LINE_Ent_Ent, Dataset.WEB_SNIPPETS, map_annotations_sentences,lstFilter);
		}
		else if(model.equals(EmbeddingModel.GOOGLE)&&dName.equals(Dataset.DBpedia)) {
			System.out.println("The name of the dataset is "+Dataset.DBpedia.name()+" the model: "+EmbeddingModel.GOOGLE.name());
			GoogleModelSingleton.getInstance();
			generate.labelTrainSetParalel(EmbeddingModel.GOOGLE, Dataset.DBpedia,lstFilter);
		}

	}

	public static Map<String, List<String>> read_annotations_sentences_filter(Dataset dName, String fName, List<Article> filter) {
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		Map<String, List<Article>> mapDataset = new HashMap<String, List<Article>>(ReadDataset.read_dataset_DBPedia_SampleLabel( DATASET_DBP_TRAIN));
		int countNullArticles=0;
		int counttotalArticles=0;
		int countSentencesNoAnnotation=0;
		int countSentencesWithAnnotation=0;
		String l = null;
		try {
			List<String> lines = FileUtils.readLines(new File(fName), "utf-8");
			System.out.println("Size of the lines "+lines.size());
			for(String line : lines) {
				l=line;

				int index = line.lastIndexOf("\t\t");
				if (index!=-1) {
					String sentence= line.substring(0, index);
					String   strAnnotations = line.substring(index+1).trim();

					if (!filter.contains(mapDataset.get(sentence).get(0))) {
						if (strAnnotations.length()>0) {
							String[] annotations = strAnnotations.split(",");
							counttotalArticles+=annotations.length;
							List<String> lstAnnotations = new ArrayList<String>();
							for (int i = 0; i < annotations.length; i++) {
								Article article = WikipediaSingleton.getInstance().wikipedia.getArticleById(Integer.valueOf(annotations[i]));
								if (article==null) {
									countNullArticles++;
								}
								else {
									boolean isNumeric = StringUtil.isNumeric(article.getTitle());
									if (dName.equals(Dataset.YAHOO)) {
										if (!isNumeric&&!AnnonatationUtil.getEntityBlackList_Yahoo().contains(article.getId())) {
											lstAnnotations.add(annotations[i]);
										}
										else {
											//System.out.println("Filtered annotation "+article+" \n");
										}

									}
									else if (dName.equals(Dataset.DBpedia)) {
										if (!isNumeric&&!AnnonatationUtil.getEntityBlackList_DBp().contains(article.getId())) {
											lstAnnotations.add(annotations[i]);
										}
										else {
											//System.out.println("Filtered annotation "+article+" \n");
										}

									}
								}
							}
							countSentencesWithAnnotation++;
							result.put(sentence, lstAnnotations);
						}
						else {
							//System.out.println("Sentence with no annotation: "+line);
							countSentencesNoAnnotation++;
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println(result.size());
			if (l.contains("\t\t")) {
				System.out.println("Yes");

			}
			if (mapDataset.containsKey(l.split("\t\t")[0])) {
				System.out.println("Yes");
			}
			System.out.println(l);
			e.printStackTrace();
			System.exit(1);
		}
		//		System.out.println("Total annotation: "+counttotalArticles);
		System.out.println("Null annotation: "+countNullArticles);
		System.out.println("countSentencesWithAnnotation: "+countSentencesWithAnnotation);
		System.out.println("countSentencesNoAnnotation: "+countSentencesNoAnnotation);
		System.out.println("Finished reading DBpedia sentences and annotation size: "+result.size());
		System.out.println("Total : "+(countSentencesNoAnnotation+countSentencesWithAnnotation));
		return result;
	}

	public static Map<String, List<String>> read_annotations_sentences(Dataset dName, String fName) {
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		int countNullArticles=0;
		int counttotalAnnotation=0;
		int countSentencesNoAnnotation=0;
		int countSentencesWithAnnotation=0;
		int countDoublicateSentence=0;
		String l = null;
		try {
			List<String> lines = FileUtils.readLines(new File(fName), "utf-8");
			System.out.println("Size of the lines "+lines.size());
			for(String line : lines) {
				l=line;
				String[] split = line.split("\t\t");
				if (split.length>1) {
					String[] annotations = split[split.length-1].split(",");
					counttotalAnnotation+=annotations.length;
					List<String> lstAnnotations = new ArrayList<String>();
					for (int i = 0; i < annotations.length; i++) {
						Article article = WikipediaSingleton.getInstance().wikipedia.getArticleById(Integer.valueOf(annotations[i]));
						if (article==null) {
							countNullArticles++;
						}
						else {
							boolean isNumeric = StringUtil.isNumeric(article.getTitle());
							if (dName.equals(Dataset.YAHOO)) {
								if (!isNumeric&&!AnnonatationUtil.getEntityBlackList_Yahoo().contains(article.getId())) {
									lstAnnotations.add(annotations[i]);
								}
								else {
									//System.out.println("Filtered annotation "+article+" \n");
								}

							}
							else if (dName.equals(Dataset.DBpedia)) {
								if (!isNumeric&&!AnnonatationUtil.getEntityBlackList_DBp().contains(article.getId())) {
									lstAnnotations.add(annotations[i]);
								}
								else {
									//System.out.println("Filtered annotation "+article+" \n");
								}

							}
							else if (dName.equals(Dataset.WEB_SNIPPETS)) {
								if (!isNumeric&&!AnnonatationUtil.getEntityBlackList_WebSnippets().contains(article.getId())) {
									lstAnnotations.add(annotations[i]);
								}
								else {
									//System.out.println("Filtered annotation "+article+" \n");
								}

							}
						}
					}
					countSentencesWithAnnotation++;
					if (result.containsKey(split[0])) {
						countDoublicateSentence++;
						//System.out.println(split[0]);
					}
					result.put(split[0], lstAnnotations);
				}
				else {
					//System.out.println("Sentence with no annotation: "+line);
					countSentencesNoAnnotation++;
				}
			}
		} catch (Exception e) {
			System.out.println(l);
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Total annotation: "+counttotalAnnotation);
		System.out.println("Doublicate sentences: "+countDoublicateSentence);
		System.out.println("Null annotation: "+countNullArticles);
		System.out.println("countSentencesWithAnnotation: "+countSentencesWithAnnotation);
		System.out.println("countSentencesNoAnnotation: "+countSentencesNoAnnotation);
		System.out.println("Finished reading DBpedia sentences and annotation size: "+result.size());
		System.out.println("Total : "+(countSentencesNoAnnotation+countSentencesWithAnnotation));
		return result;
	}

}
