package org.fiz.ise.gwifi.dataset;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.assignLabels.AssignLabelsBasedOnConfVecSimilarity;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.model.EmbeddingModel;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.StringUtil;

import edu.kit.aifb.gwifi.model.Article;


public class AnalyseSnippetsDataset {

	private static final String DATASET_TRAIN_WEB = Config.getString("DATASET_TRAIN_SNIPPETS","");
	private static final String DATASET_TEST_WEB = Config.getString("DATASET_TEST_SNIPPETS","");
	private static final String DATASET_SNIPPETS_TRAIN_ANNOTATIONS = Config.getString("DATASET_SNIPPETS_TRAIN_ANNOTATIONS","");
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	public static void main(String[] args) {
		System.out.println("Running Snippet analyses");
		
		Map<String, List<Article>> map = ReadDataset.read_dataset_Snippets(DATASET_TEST_WEB);
		List<String> lst_samples= ReadDataset.read_snippets(DATASET_TEST_WEB);
		writeFileAsCSV(map, lst_samples, "snippets_test.csv");
		
		//		List<String> dataset = ReadDataset.read_snippets(DATASET_TRAIN_WEB);
		//		System.out.println("Size of the dataset:"+dataset.size());
		//		//AnnonatationUtil.writeAnnotationFile(dataset);
		//		Map<String, List<String>> map_dbp_annotations_sentences = AnalyseDBPediaDataset.read_annotations_sentences(Dataset.WEB_SNIPPETS, DATASET_SNIPPETS_TRAIN_ANNOTATIONS);
		//		System.out.println("Size of the annotated sentences: "+map_dbp_annotations_sentences.size());
		//		List<Article> labels = new ArrayList<Article>(Categories.getLabels_Snippets());
		//		int count=0;
		//		for(Article a : labels) {
		//			List<String> read_WEB_BasedOnCategory = ReadDataset.read_WEB_BasedOnCategory(a.getTitle(), DATASET_TRAIN_WEB);
		//			List<String> allAnnotations = new ArrayList<String>();
		//			for(String sentence: read_WEB_BasedOnCategory) {
		//				if (map_dbp_annotations_sentences.containsKey(sentence)) {
		//					allAnnotations.addAll(map_dbp_annotations_sentences.get(sentence));
		//				}
		//				else {
		//					count++;
		//					System.out.println("Number of sentences are not in  the map so far: "+count);
		//				}
		//			}
		//			AnalyseDataset.findMostSimilarEntitesIDsForDatasetBasedOnDatasetVector(allAnnotations, "snippets_most_similar_entities"+a.getTitle()+"_datasetVec");
		//			
		//		}
		//		System.out.println("Number of sentences are not in  the map : "+count);

//		AnalyseDBPediaDataset.categorizeDataset(Dataset.WEB_SNIPPETS,EmbeddingModel.LINE_Ent_Ent,DATASET_SNIPPETS_TRAIN_ANNOTATIONS,false, null);
		
//		List<String> files = new ArrayList<String>();
//		files.add("/home/rima/playground/GeneralFiles/gwifi/Dataset_ShortTextClassification/data-web-snippets/LabelAssignment/data_generated_WEB_SNIPPETS_doc2vec_wikipedia_10_2018_dbow.model.txt");
//		files.add("/home/rima/playground/GeneralFiles/gwifi/Dataset_ShortTextClassification/data-web-snippets/LabelAssignment/data_generated_WEB_SNIPPETS_GOOGLE.txt");
//		files.add("/home/rima/playground/GeneralFiles/gwifi/Dataset_ShortTextClassification/data-web-snippets/LabelAssignment/data_generated_WEB_SNIPPETS_LINE.txt");
//		
//		for(String file: files) {
//			AnalyseDataset.compareTwoFiles(Dataset.WEB_SNIPPETS, DATASET_TRAIN_WEB, file);
//		}
//		
//		AssignLabelsBasedOnConfVecSimilarity assign = new AssignLabelsBasedOnConfVecSimilarity();
//		
//		assign.calculateConfidenceForEachModel(Dataset.WEB_SNIPPETS);
	}
	
	public static void writeFileAsCSV(Map<String, List<Article>> map, List<String> lst_samples, final String fileName) {
		try {
			final FileWriter fw = new FileWriter(fileName, false);
			for (String sample : lst_samples) {
				fw.write("\""+map.get(sample).get(0)+"\",\""+sample+"\""+"\n");
		    }
			fw.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}
