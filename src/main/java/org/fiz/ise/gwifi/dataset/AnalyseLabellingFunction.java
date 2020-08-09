package org.fiz.ise.gwifi.dataset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.assignLabels.AssignLabelsBasedOnConfVecSimilarity;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.Print;

import edu.kit.aifb.gwifi.model.Article;

public class AnalyseLabellingFunction {
		
		private final static Dataset TEST_DATASET_TYPE= Dataset.WEB_SNIPPETS;//Config.getEnum("TEST_DATASET_TYPE");
		private static final String DATASET_DBP_TRAIN = Config.getString("DATASET_DBP_TRAIN","");
		private static final String DATASET_DBP_TRAIN_CATEGORIZED_D2Vec = Config.getString("DATASET_DBP_TRAIN_CATEGORIZED_D2Vec","");
		static final Logger secondLOG = Logger.getLogger("debugLogger");
		static final Logger resultLog = Logger.getLogger("reportsLogger");
		private static final String DATASET_TRAIN_SNIPPETS = Config.getString("DATASET_TRAIN_SNIPPETS","");
		private final static Integer NUMBER_OF_THREADS= Config.getInt("NUMBER_OF_THREADS",-1);
		public static final Map<String,List<String>> CACHE = new ConcurrentHashMap<>();
		static Map<String,Integer> result = new ConcurrentHashMap<>();
		private static ExecutorService executor;

		public static void main(String[] args) throws Exception {
			Dataset d = Dataset.AG;
			System.out.println("In main AnalyseLabellingFunction");

			Map<String,List<Article>> map_categorized = new HashMap<>();
			Map<String, List<Article>>  map_gt = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION,Config.getString("DATASET_TRAIN_AG",""));
			System.out.println("Finished reading the dataset: "+map_gt.size());
			
			//Map<String, List<Article>>  map_gt = ReadDataset.read_dataset_Snippets(DATASET_TRAIN_SNIPPETS);
			//Map<String, List<Article>>  map_gt = ReadDataset.read_dataset_DBPedia_SampleLabel(DATASET_DBP_TRAIN);
			
			
			for(Entry <String, List<Article>> e: map_gt.entrySet()) {
				List<Article> temp= new ArrayList<Article>();
				Article articleByTitle = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(AssignLabelsBasedOnConfVecSimilarity.getBestLabelBasedOnConfidence( d, e.getKey()));
				temp.add(articleByTitle);
				map_categorized.put(e.getKey(), temp);
			}
			//compareTwoDataset( d, DATASET_DBP_TRAIN, map);
			//compareTwoDataset(d, DATASET_TRAIN_SNIPPETS, map);
			
			System.out.println("Start comparing map_categorized.size: "+map_categorized.size());
			compareTwoDataset(d, Config.getString("DATASET_TRAIN_AG",""), map_categorized);

			//		compareTwoFiles(d, DATASET_DBP_TRAIN,"data_generated_dbpedia_GOOGLE.txt");
		}
		
		public static void compareTwoDataset(Dataset dset, String gtFile, Map<String, List<Article>> map_categorized) {
			int countCorrect=0;
			int countWrong=0;
			int countNotInDataset=0;
			Map<String, List<Article>> map_gt =  null;
			Map<String, Integer> map_error_analysis_cat = new HashMap<String, Integer>();
			List<String> lst_snippets = null;
			if (dset.equals(Dataset.DBpedia)) {
				map_gt = ReadDataset.read_dataset_DBPedia_SampleLabel(gtFile);
			}
			else if (dset.equals(Dataset.AG)) {
				map_gt = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION,Config.getString("DATASET_TRAIN_AG",""));
			}
			else if (dset.equals(Dataset.WEB_SNIPPETS)) {
				map_gt = ReadDataset.read_dataset_Snippets(gtFile);
				lst_snippets= ReadDataset.read_dataset_Snippets_list(gtFile);
				for (String s : lst_snippets) {
					List<Article> list = map_categorized.get(s);
					if (list==null) {
						countNotInDataset++;
					}
					else if (map_gt.get(s).contains(list.get(0))) {
						countCorrect++;
					}
					else {
						secondLOG.info(list+ "  "+map_gt.get(s));
						countWrong++;
					}
				}
			}
			if (!dset.equals(Dataset.WEB_SNIPPETS)) {

				for(Entry <String, List<Article>> e: map_gt.entrySet()) {
					List<Article> list = map_categorized.get(e.getKey());
					if (list.isEmpty()) {
						countNotInDataset++;
						continue;
					}
					else if (list.contains(e.getValue().get(0))) {
						countCorrect++;
					}
					else {
						countWrong++;
						//					String key = dataset_test.get(e.getKey()).get(0)+"->"+e.getValue();
						if (list.get(0)!=null) {
							//System.out.println(list.size()+" "+list.get(0));
							String key = list.get(0).getTitle();
							map_error_analysis_cat.merge(key, 1, Integer::sum);
						}
					}
				}
			}
			System.out.println("Size of the original dataset: "+map_gt.size());
			System.out.println("Size of the categorized dataset: "+map_categorized.size());

			System.out.println("countCorrect: "+countCorrect);
			System.out.println("countWrong: "+countWrong);
			System.out.println("countNotInDataset: "+countNotInDataset);
			System.out.println("accuracy: "+countCorrect*1.0/(countWrong+countCorrect)*1.0);
			System.out.println("error rate: "+countWrong*1.0/(countWrong+countCorrect)*1.0);
			System.out.println("***********************");

			Map<String, Integer> map_sample_size = new HashMap<String, Integer>();
			for ( Entry <String, List<Article>> e: map_categorized.entrySet() ) {
				if (e.getValue().get(0)!=null) {
					map_sample_size.merge(e.getValue().get(0).getTitle(), 1, Integer::sum);
				}
			}
			for ( Entry <String, Integer> e: map_sample_size.entrySet() ) {
				System.out.println(e.getKey()+" "+(map_error_analysis_cat.get(e.getKey())*1.0)/(e.getValue()*1.0));
			}
			System.out.println("***************************************************");
			System.out.println("***************************************************");
			for ( Entry <String, Integer> e: map_sample_size.entrySet() ) {
				System.out.println("error rate: " +e.getKey()+" "+(map_error_analysis_cat.get(e.getKey())*1.0)/(countWrong)*1.0);
			}
			System.out.println("***************************************************");
			Print.printMap(map_sample_size);
			System.out.println("***************************************************");
			Print.printMap(map_error_analysis_cat);

		}
		public static void compareTwoFiles(Dataset dset, String gtFile, String cFile) {
			int countCorrect=0;
			int countWrong=0;
			int countNotInDataset=0;
			Map<String, List<Article>> map_gt =  null;
			Map<String, List<Article>> map_categorized = null;
			List<String> lst_snippets = null;
			if (dset.equals(Dataset.DBpedia)) {
				map_gt = ReadDataset.read_dataset_DBPedia_SampleLabel(gtFile);
				map_categorized = ReadDataset.read_dataset_DBPedia_SampleLabel(cFile);
			}
			else if (dset.equals(Dataset.AG)) {
				map_gt = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION,Config.getString("DATASET_TRAIN_AG",""));
				map_categorized =  AssignLabelsBasedOnConfVecSimilarity.readLabelAssignment_AG_article("LabelAssignment_AG_GOOGLE");
			}
			else if (dset.equals(Dataset.WEB_SNIPPETS)) {
				map_gt = ReadDataset.read_dataset_Snippets(gtFile);
				map_categorized = ReadDataset.read_dataset_Snippets(cFile);
				lst_snippets= ReadDataset.read_dataset_Snippets_list(gtFile);
				for (String s : lst_snippets) {
					List<Article> list = map_categorized.get(s);
					if (list==null) {
						countNotInDataset++;
					}
					else if (list.contains(map_gt.get(s).get(0))) {
						countCorrect++;
					}
					else {
						countWrong++;
					}
				}
			}
			if (!dset.equals(Dataset.WEB_SNIPPETS)) {

				for(Entry <String, List<Article>> e: map_gt.entrySet()) {
					List<Article> list = map_categorized.get(e.getKey());
					if (list==null) {
						countNotInDataset++;
					}
					else if (list.contains(e.getValue().get(0))) {
						countCorrect++;
					}
					else {
						countWrong++;
					}
				}
			}
			System.out.println("File name: "+cFile);
			System.out.println("Size of the original dataset: "+map_gt.size());
			System.out.println("Size of the categorized dataset: "+map_categorized.size());

			System.out.println("countCorrect: "+countCorrect);
			System.out.println("countWrong: "+countWrong);
			System.out.println("countNotInDataset: "+countNotInDataset);
			System.out.println("accuracy: "+countCorrect*1.0/(countWrong+countCorrect)*1.0);
			System.out.println("***********************");

		}
}
