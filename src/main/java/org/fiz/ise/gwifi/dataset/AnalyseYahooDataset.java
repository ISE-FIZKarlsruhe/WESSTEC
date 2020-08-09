package org.fiz.ise.gwifi.dataset;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.dataset.train.generation.GenerateDatasetForNN;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.model.EmbeddingModel;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.TimeUtil;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;


public class AnalyseYahooDataset {
	private static final String DATASET_YAHOO_TRAIN = Config.getString("DATASET_YAHOO_TRAIN","");
	private static final String DATASET_YAHOO_TEST = Config.getString("DATASET_YAHOO_TEST","");
	private static final String DATASET_YAHOO_TRAIN_ANNOTATIONS = Config.getString("DATASET_YAHOO_TRAIN_ANNOTATIONS","");
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");
	public static void main(String[] args) {
		System.out.println("Running yahoo analyses");
		
		//writeAnnotationsToFile();
		categorizeDataset();
		//findMostSimilarEntities();
		
	}
	private static void writeAnnotationsToFile() {
		AnnonatationUtil.writeAnnotationFile(new ArrayList<String>(ReadDataset.read_dataset_Yahoo_LabelArticle(DATASET_YAHOO_TRAIN).keySet()));
	}
	private static void categorizeDataset() {
		Map<String, List<String>> map_yahoo_annotations_sentences = AnalyseDBPediaDataset.read_annotations_sentences(Dataset.YAHOO,DATASET_YAHOO_TRAIN_ANNOTATIONS);
		LINE_modelSingleton.getInstance();
		GenerateDatasetForNN generate = new GenerateDatasetForNN();
		generate.labelAnnotatedTrainSetParalel(EmbeddingModel.LINE_Ent_Ent, Dataset.YAHOO, map_yahoo_annotations_sentences, null);

	}
	private static void findMostSimilarEntities() {
		System.out.println("Analaysing yahoo dataset");
		Map<String, List<Article>> dataset_YAHOO_train = ReadDataset.read_dataset_Yahoo_LabelArticle(DATASET_YAHOO_TRAIN);
		Map<String, List<Article>> dataset_YAHOO_test = ReadDataset.read_dataset_Yahoo_LabelArticle(DATASET_YAHOO_TEST);
		System.out.println("Size of the train:"+dataset_YAHOO_train.size());
		System.out.println("Size of the test:"+dataset_YAHOO_test.size());
//		AnnonatationUtil.getAvgAnnotationOfDatasets(new ArrayList<String>(dataset_YAHOO_train.keySet()));
//		System.out.println();
//		AnnonatationUtil.getAvgAnnotationOfDatasets(new ArrayList<String>(dataset_YAHOO_test.keySet()));
//		
//		System.out.println();
		String folderName="yahoo_dataset_qt_qc_ba";
		Set<Integer> lables_Yahoo_article = LabelsOfTheTexts.getLables_Yahoo_article().keySet();
		int countTotalAnnTrain=0;
		int countTotalAnnTest=0;
		
		for(int i : lables_Yahoo_article) {
			Long start =TimeUtil.getStart();
			Article a =LabelsOfTheTexts.getLables_Yahoo_article().get(i);
			System.out.println("Analysing:"+a.getTitle()+"*************************************");
			List<String> lst_dataset_train = new ArrayList<String>(ReadDataset.read_dataset_Yahoo_BasedOnLabel(DATASET_YAHOO_TRAIN,i ));
			List<Annotation> findAnnotationAll_train = new ArrayList<Annotation>(AnnonatationUtil.findAnnotationAll(lst_dataset_train));
			List<Annotation> findAnnotationAll_test = new ArrayList<Annotation>(AnnonatationUtil.findAnnotationAll(ReadDataset.read_dataset_Yahoo_BasedOnLabel(DATASET_YAHOO_TEST, i)));
			System.out.println("Annotation countAllAnnotation_train: "+findAnnotationAll_train.size());
			System.out.println("Annotation countAllAnnotation_test: "+findAnnotationAll_test.size());

			countTotalAnnTrain+=findAnnotationAll_train.size();
			countTotalAnnTest+=findAnnotationAll_test.size();
			
			AnnonatationUtil.findFreqOfAnnotation(findAnnotationAll_train, folderName+"/"+"yahoo_train_annotation_freq"+LabelsOfTheTexts.getLables_Yahoo_article().get(i).getTitle());
//			AnnonatationUtil.findFreqOfAnnotation(findAnnotationAll_test, "yahoo_test_annotation_freq"+LabelsOfTheTexts.getLables_Yahoo_article().get(i));
			
//			AnalyseDataset.findMostSimilarEntitesForDatasetBasedOnDatasetVector(findAnnotationAll_train, folderName+"/"+"yahoo_train_most_sim_ent"+LabelsOfTheTexts.getLables_Yahoo_article().get(i).getTitle());
//			AnalyseDataset.findMostSimilarEntitesForDatasetBasedOnDatasetVector(findAnnotationAll_test, "yahoo_test_most_sim_ent"+a.getTitle());
			
//			AnalyseDataset.findMostSimilarEntitesForDataset(lst_dataset_train, Dataset.YAHOO, "yahoo_train_most_sim_ent_each_ann"+LabelsOfTheTexts.getLables_Yahoo_article().get(i).getTitle());
			
			System.out.println("Annotation countTotalAnnTrain: "+countTotalAnnTrain);
			System.out.println("Annotation countTotalAnnTest: "+countTotalAnnTest);
			System.out.println(LabelsOfTheTexts.getLables_Yahoo_article().get(i).getTitle()+"-Finished-yahoo-seconds time : "+TimeUtil.getEnd(TimeUnit.MILLISECONDS, start)/ 1000F);
			System.out.println("********************************************************************");
		
		}
		secondLOG.info("Yahoo Dataset");
		secondLOG.info("Size of the train:"+dataset_YAHOO_train.size());
		secondLOG.info("Size of the test:"+dataset_YAHOO_test.size());
		secondLOG.info("countTotalAnnTrain:"+countTotalAnnTrain);
		secondLOG.info("countTotalAnnTest:"+countTotalAnnTest);
	}
}
