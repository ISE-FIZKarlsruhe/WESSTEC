package org.fiz.ise.gwifi.wide_deep;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LabelsOfTheTexts;
import org.fiz.ise.gwifi.dataset.ReadDataset;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.Print;

import com.mongodb.util.Hash;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Wikipedia;

public class ErrorAnalysis {
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");

	public static void main(String[] args) throws IOException {
//		analyse_error_wide(Dataset.AG,"ErrorAnalysis/error_analysis_ag_wide_deep_words64.txt");
		String fName_classified_wide="ErrorAnalysis/error_analysis_ag_linear_entCooc64.txt";
		String fName_classified_deep="ErrorAnalysis/error_analysis_ag_deep_words64.txt";
		String fName_classified_wide_deep="ErrorAnalysis/error_analysis_ag_wide_deep_words64.txt";
//		find_overlap_wide_deep(Dataset.AG, fName_classified_wide, fName_classified_deep, fName_classified_wide_deep);
		find_overlap_of_lists();
//		compare_overlap_indivudual_wide_deep_and_wide_deep();

	}
	public static void compare_overlap_indivudual_wide_deep_and_wide_deep() throws IOException {
		List<String> wide_correct = FileUtils.readLines(new File("ErrorAnalysis/error_analysis_wide_AG_correct.txt"), "utf-8");
		List<String> wide_wrong = FileUtils.readLines(new File("ErrorAnalysis/error_analysis_wide_AG_wrong.txt"), "utf-8");
		
		List<String> deep_correct = FileUtils.readLines(new File("ErrorAnalysis/error_analysis_deep_AG_correct.txt"), "utf-8");
		List<String> deep_wrong = FileUtils.readLines(new File("ErrorAnalysis/error_analysis_deep_AG_wrong.txt"), "utf-8");
		
		List<String> wide_deep_correct = FileUtils.readLines(new File("ErrorAnalysis/error_analysis_wide_deep_AG_correct.txt"), "utf-8");
		List<String> wide_deep_wrong = FileUtils.readLines(new File("ErrorAnalysis/error_analysis_wide_deep_AG_wrong.txt"), "utf-8");
		
		find_overlap_of_lists();
		
		
	}
	public static void find_overlap_of_lists() throws IOException {
		List<String> lines_wide = FileUtils.readLines(new File("ErrorAnalysis/error_analysis_ag_linear_entCooc64.txt"), "utf-8");
		List<String> lines_deep = FileUtils.readLines(new File("ErrorAnalysis/error_analysis_ag_deep_words64.txt"), "utf-8");
		List<String> lines_wide_deep = FileUtils.readLines(new File("ErrorAnalysis/error_analysis_ag_wide_deep_words64.txt"), "utf-8");
		
		int count=0;
		List<String> overlap = new ArrayList<String>();
		
		for(String str: lines_wide) {
			if (lines_deep.contains(str)) {
				overlap.add(str);
//				overlap.add(str.split("\t\t")[1]);
				count++;
			}
		}
		System.out.println("count overlap : "+count);
		
		for(String str: overlap) {
			lines_wide.remove(str);
		}
		System.out.println("Size of wide after removing overlap: "+lines_wide.size());
		
		List<String> correct_wide = get_categorized_correct(Dataset.AG,lines_wide);
		List<String> wrong_wide = get_categorized_wrong(Dataset.AG,lines_wide);
		
		System.out.println("correct_wide: "+correct_wide.size());
		System.out.println("wrong_wide: "+wrong_wide.size());
		
		
		for(String str: overlap) {
			lines_deep.remove(str);
		}
		System.out.println("Size of wide after removing overlap: "+lines_wide.size());
		
		List<String> correct_deep = get_categorized_correct(Dataset.AG,lines_deep);
		List<String> wrong_deep = get_categorized_wrong(Dataset.AG,lines_deep);
		
		System.out.println("correct_deep: "+correct_deep.size());
		System.out.println("wrong_deep: "+wrong_deep.size());
		
		
		List<String> correct_wide_deep = get_categorized_correct(Dataset.AG,lines_wide_deep);
		List<String> wrong_wide_deep = get_categorized_wrong(Dataset.AG,lines_wide_deep);
		
		
		int count_correct=0;
		int count_wrong=0;
		
		for(String str: correct_wide) {
			if (correct_wide_deep.contains(str)) {
				count_correct++;
			}
			else if(!correct_wide_deep.contains(str)&&wrong_wide_deep.contains(str)) {
				count_wrong++;
			}
			else {
				System.out.println("Somethig is wrong");
				System.exit(1);
			}
		}
		
		System.out.println("count_correct : "+count_correct);
		System.out.println("count_wrong : "+count_wrong);
		
		System.out.println("*******************************************");
		
		count_correct=0;
		count_wrong=0;
		for(String str: correct_deep) {
			if (correct_wide_deep.contains(str)) {
				count_correct++;
			}
			else if(!correct_wide_deep.contains(str)&&wrong_wide_deep.contains(str)) {
				count_wrong++;
			}
			else {
				System.out.println("Somethig is wrong");
				System.exit(1);
			}
		}
		
		System.out.println("count_correct : "+count_correct);
		System.out.println("count_wrong : "+count_wrong);
		
		System.out.println("*******************************************");
		
		count_correct=0;
		count_wrong=0;
		for(String str: wrong_wide) {
			if (wrong_wide_deep.contains(str)) {
				count_wrong++;
			}
			else if(correct_wide_deep.contains(str)&&!wrong_wide_deep.contains(str)) {
				count_correct++;
			}
			else {
				System.out.println("Somethig is wrong");
				System.exit(1);
			}
		}
		
		System.out.println("count_correct : "+count_correct);
		System.out.println("count_wrong : "+count_wrong);
//		
//		List<String> wide_wrong_wo_intersection = new ArrayList<String>();
//		
//		for(String str: lines_wide) {
//			if (!wide_wrong.contains(str.split("\t\t")[1])) {
//				wide_wrong_wo_intersection.add(str);
//			}
//		}
//		System.out.println("Size of wide_wrong_wo_intersection : "+wide_wrong_wo_intersection.size());
//		wide_wrong.retainAll(overlap);
//		
//		List<String> deep_correct_wo_intersection = new ArrayList<String>();
//		
//		for(String str: deep_correct) {
//			if (!overlap.contains(str)) {
//				deep_correct_wo_intersection.add(str);
//			}
//		}
//		
//		System.out.println("Size of the first list: "+wide_correct_wo_intersection.size());
		
		
//		for(String str: wide_correct) {
//			if (wide_deep_correct.contains(str)) {
//				count++;
//			}
//		}
//		System.out.println("Overlap: "+count);
//		wide_correct.retainAll(wide_deep_correct);
//		System.out.println("Overlap: "+wide_correct.size());
		
	}
	public static void find_overlap_wide_deep(Dataset dName,String file_wide_classified,String file_deep_classified ,String file_wide_deep_classified) throws IOException {

		List<String> lines_1 = FileUtils.readLines(new File(file_wide_classified), "utf-8");
		List<String> lines_2 = FileUtils.readLines(new File(file_deep_classified), "utf-8");
		List<String> lines_3 = FileUtils.readLines(new File(file_wide_deep_classified), "utf-8");
		
		int count=0;
		List<String> overlap = new ArrayList<String>();
		
		for(String str: lines_1) {
			if (lines_2.contains(str)) {
				overlap.add(str);
				count++;
			}
		}
		System.out.println("count overlap : "+count);
		count=0;
		for(String str: lines_3) {
			if (overlap.contains(str)) {
				count++;
			}
		}
		
		System.out.println("count overlap : "+count);
		
		HashMap<String, Article> map_classified_wide = new HashMap<String, Article>(get_categorized_general(dName, file_wide_classified));
		HashMap<String, Article> map_classified_deep = new HashMap<String, Article>(get_categorized_general(dName, file_deep_classified));
		HashMap<String, Article> map_classified_overlap = new HashMap<String, Article>(find_overlapping_elements_map(map_classified_wide, map_classified_deep));
		
		System.out.println("Overlap result: "+map_classified_overlap.size());
		HashMap<String, Article> map_classified_wide_deep = new HashMap<String, Article>(get_categorized_general(Dataset.AG, file_wide_deep_classified));
		
		HashMap<String, Article> map_final_overlap = new HashMap<String, Article>(find_overlapping_elements_map(map_classified_overlap,map_classified_wide_deep));
		System.out.println("Overlap final result: "+map_final_overlap.size());

	}
	public static Map<String, Article> find_overlapping_elements_map(Map<String, Article> map_classified_wide, Map<String, Article> map_classified_deep) {
		Map<String, Article> map_result = new HashMap<String, Article>();

		for(Entry <String, Article> e: map_classified_wide.entrySet() ) {
			if (map_classified_deep.get(e.getKey()).equals(e.getValue())) {
				map_result.put(e.getKey(), e.getValue());
			}
		}


		return map_result;
	}
	public static void analyse_error_wide(Dataset dName,String fNameClassified) throws IOException {
		ArrayList<String> list_label = new ArrayList<>();
		Map<String,  List<Article>> dataset_test=null;
		Map<String, Article> map_classified = null;
		Map<String, Integer> map_error_analysis_cat = new HashMap<String, Integer>();
		if (dName.equals(Dataset.AG)) {
			dataset_test = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION,Config.getString("DATASET_TEST_AG",""));
			Map<Integer, Article> lables_AG_article = LabelsOfTheTexts.getLables_AG_article();
			ArrayList<Article> labels = new ArrayList<>(lables_AG_article.values());
			for(Article a : labels) {
				list_label.add(a.getTitle());
			}
			map_classified = new HashMap<String, Article>(get_categorized_general(Dataset.AG, fNameClassified));
		}

		java.util.Collections.sort(list_label);

		for(Entry <String, Article> e: map_classified.entrySet() ) {
			if (!dataset_test.get(e.getKey()).contains(e.getValue())) {
				//				String key = dataset_test.get(e.getKey()).get(0)+"->"+e.getValue();
				String key = e.getValue().getTitle();
				if (key.equals("Business")) {
					System.out.println(e.getKey());
				}
				map_error_analysis_cat.merge(key, 1, Integer::sum);
			}
		}
		int totalWrong=0;

		for ( Entry <String, Integer> e: map_error_analysis_cat.entrySet() ) {
			totalWrong+=e.getValue();

		}
		System.out.println("Total wrong: "+totalWrong);
		Map<String, Integer> map_classified_size = new HashMap<String, Integer>();

		for ( Entry <String, Article> e: map_classified.entrySet() ) {
			if (e.getValue()!=null) {
				map_classified_size.merge(e.getValue().getTitle(), 1, Integer::sum);
			}
		}
		System.out.println("****************************");

		for ( Entry <String, Integer> e: map_classified_size.entrySet() ) {
			System.out.println("error rate: " +e.getKey()+" "+(map_error_analysis_cat.get(e.getKey())*1.0)/(totalWrong*1.0));
		}
		System.out.println("****************************");

		Print.printMap(map_classified_size);
		System.out.println("****************************");
		Print.printMap(map_error_analysis_cat);
		System.out.println("****************************");
	}
	public static Map<String, Article> get_categorized_general(Dataset dName,String fName) throws IOException {
		ArrayList<String> list_label = new ArrayList<>();
		if (dName.equals(Dataset.AG)) {
			Map<Integer, Article> lables_AG_article = LabelsOfTheTexts.getLables_AG_article();
			ArrayList<Article> labels = new ArrayList<>(lables_AG_article.values());
			for(Article a : labels) {
				list_label.add(a.getTitle());
			}
		}
		else if (dName.equals(Dataset.WEB_SNIPPETS)) {
			ArrayList<Article> temp_lstCats = new ArrayList<Article>(Categories.getLabels_Snippets());
			for(Article a : temp_lstCats) {
				if (!a.getTitle().equalsIgnoreCase("The arts")&& !a.getTitle().equalsIgnoreCase("Entertainment")&&
						!a.getTitle().equalsIgnoreCase("Science")&& !a.getTitle().equalsIgnoreCase("Society")) {
					list_label.add(a.getTitle());
				}
			}

		}
		java.util.Collections.sort(list_label);

		List<String> lines = FileUtils.readLines(new File(fName), "utf-8");
		Map<String, Article> map_classified = new HashMap<String, Article>();
		for(String str : lines) {
			String[] split = str.split("\t\t");
			map_classified.put(split[1], WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(list_label.get(Integer.valueOf(split[0]))));
		}
		return map_classified;
	}
	public static List<String> get_categorized_correct(Dataset dName,List<String> lst_data) throws IOException {
		Map<String, Article> map_error_analysis = new HashMap<String, Article>();
		ArrayList<String> list_label = new ArrayList<>();
		Map<String,  List<Article>> dataset_test=null;
		if (dName.equals(Dataset.AG)) {
			dataset_test = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION,Config.getString("DATASET_TEST_AG",""));
			Map<Integer, Article> lables_AG_article = LabelsOfTheTexts.getLables_AG_article();
			ArrayList<Article> labels = new ArrayList<>(lables_AG_article.values());
			for(Article a : labels) {
				list_label.add(a.getTitle());
			}
		}
		else if (dName.equals(Dataset.WEB_SNIPPETS)) {
			dataset_test = ReadDataset.read_dataset_Snippets(Config.getString("DATASET_TEST_SNIPPETS",""));
			ArrayList<Article> temp_lstCats = new ArrayList<Article>(Categories.getLabels_Snippets());
			for(Article a : temp_lstCats) {
				if (!a.getTitle().equalsIgnoreCase("The arts")&& !a.getTitle().equalsIgnoreCase("Entertainment")&&
						!a.getTitle().equalsIgnoreCase("Science")&& !a.getTitle().equalsIgnoreCase("Society")) {
					list_label.add(a.getTitle());
				}
			}

		}
		java.util.Collections.sort(list_label);

		Map<String, Article> map_classified = new HashMap<String, Article>();
		for(String str : lst_data) {
			String[] split = str.split("\t\t");
			map_classified.put(split[1], WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(list_label.get(Integer.valueOf(split[0]))));
		}
		int countCorrect=0;
		int countWrong=0;
		List<String> correct= new ArrayList<String>();
		for(Entry <String, Article> e: map_classified.entrySet() ) {
			if (dataset_test.get(e.getKey()).contains(e.getValue())) {
				correct.add(e.getKey());
				countCorrect++;
			}
			else {
				countWrong++;
				map_error_analysis.put(e.getKey(), e.getValue());
			}

		}
		System.out.println("countCorrect: "+countCorrect);
		System.out.println("countWrong: "+countWrong);
		return correct;
	}
	public static List<String> get_categorized_wrong(Dataset dName,List<String> lst_data) throws IOException {
		Map<String, Article> map_error_analysis = new HashMap<String, Article>();
		ArrayList<String> list_label = new ArrayList<>();
		Map<String,  List<Article>> dataset_test=null;
		if (dName.equals(Dataset.AG)) {
			dataset_test = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION,Config.getString("DATASET_TEST_AG",""));
			Map<Integer, Article> lables_AG_article = LabelsOfTheTexts.getLables_AG_article();
			ArrayList<Article> labels = new ArrayList<>(lables_AG_article.values());
			for(Article a : labels) {
				list_label.add(a.getTitle());
			}
		}
		else if (dName.equals(Dataset.WEB_SNIPPETS)) {
			dataset_test = ReadDataset.read_dataset_Snippets(Config.getString("DATASET_TEST_SNIPPETS",""));
			ArrayList<Article> temp_lstCats = new ArrayList<Article>(Categories.getLabels_Snippets());
			for(Article a : temp_lstCats) {
				if (!a.getTitle().equalsIgnoreCase("The arts")&& !a.getTitle().equalsIgnoreCase("Entertainment")&&
						!a.getTitle().equalsIgnoreCase("Science")&& !a.getTitle().equalsIgnoreCase("Society")) {
					list_label.add(a.getTitle());
				}
			}

		}
		java.util.Collections.sort(list_label);

		Map<String, Article> map_classified = new HashMap<String, Article>();
		for(String str : lst_data) {
			String[] split = str.split("\t\t");
			map_classified.put(split[1], WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(list_label.get(Integer.valueOf(split[0]))));
		}
		int countCorrect=0;
		int countWrong=0;
		List<String> wrong= new ArrayList<String>();
		for(Entry <String, Article> e: map_classified.entrySet() ) {
			if (dataset_test.get(e.getKey()).contains(e.getValue())) {
				countCorrect++;
			}
			else {
				countWrong++;
				wrong.add(e.getKey());
			}

		}
		System.out.println("countCorrect: "+countCorrect);
		System.out.println("countWrong: "+countWrong);
		return wrong;
	}
	public static Map<String, Article> get_categorized_wrong(Dataset dName,String fName) throws IOException {
		Map<String, Article> map_error_analysis = new HashMap<String, Article>();
		ArrayList<String> list_label = new ArrayList<>();
		Map<String,  List<Article>> dataset_test=null;
		if (dName.equals(Dataset.AG)) {
			dataset_test = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION,Config.getString("DATASET_TEST_AG",""));
			Map<Integer, Article> lables_AG_article = LabelsOfTheTexts.getLables_AG_article();
			ArrayList<Article> labels = new ArrayList<>(lables_AG_article.values());
			for(Article a : labels) {
				list_label.add(a.getTitle());
			}
		}
		else if (dName.equals(Dataset.WEB_SNIPPETS)) {
			dataset_test = ReadDataset.read_dataset_Snippets(Config.getString("DATASET_TEST_SNIPPETS",""));
			ArrayList<Article> temp_lstCats = new ArrayList<Article>(Categories.getLabels_Snippets());
			for(Article a : temp_lstCats) {
				if (!a.getTitle().equalsIgnoreCase("The arts")&& !a.getTitle().equalsIgnoreCase("Entertainment")&&
						!a.getTitle().equalsIgnoreCase("Science")&& !a.getTitle().equalsIgnoreCase("Society")) {
					list_label.add(a.getTitle());
				}
			}

		}
		java.util.Collections.sort(list_label);

		List<String> lines = FileUtils.readLines(new File(fName), "utf-8");
		Map<String, Article> map_classified = new HashMap<String, Article>();
		for(String str : lines) {
			String[] split = str.split("\t\t");
			map_classified.put(split[1], WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(list_label.get(Integer.valueOf(split[0]))));
		}
		int countCorrect=0;
		int countWrong=0;
		for(Entry <String, Article> e: map_classified.entrySet() ) {
			if (dataset_test.get(e.getKey()).contains(e.getValue())) {
				countCorrect++;
			}
			else {
				countWrong++;
				map_error_analysis.put(e.getKey(), e.getValue());
			}

		}
		System.out.println("countCorrect: "+countCorrect);
		System.out.println("countWrong: "+countWrong);
		System.out.println("Accucracy: "+ (countCorrect*1.0)/(countCorrect+countWrong));
		System.out.println("Map size: "+map_error_analysis.size());
		return map_error_analysis;
	}

	public static void analyse_error_general(Dataset dName,String fName) throws IOException {

		ArrayList<String> list_label = new ArrayList<>();
		Map<String,  List<Article>> dataset_test=null;
		if (dName.equals(Dataset.AG)) {
			dataset_test = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION,Config.getString("DATASET_TEST_AG",""));
			Map<Integer, Article> lables_AG_article = LabelsOfTheTexts.getLables_AG_article();
			ArrayList<Article> labels = new ArrayList<>(lables_AG_article.values());
			for(Article a : labels) {
				list_label.add(a.getTitle());
			}
		}
		else if (dName.equals(Dataset.WEB_SNIPPETS)) {
			dataset_test = ReadDataset.read_dataset_Snippets(Config.getString("DATASET_TEST_SNIPPETS",""));
			ArrayList<Article> temp_lstCats = new ArrayList<Article>(Categories.getLabels_Snippets());
			for(Article a : temp_lstCats) {
				if (!a.getTitle().equalsIgnoreCase("The arts")&& !a.getTitle().equalsIgnoreCase("Entertainment")&&
						!a.getTitle().equalsIgnoreCase("Science")&& !a.getTitle().equalsIgnoreCase("Society")) {
					list_label.add(a.getTitle());
				}
			}

		}
		java.util.Collections.sort(list_label);

		List<String> lines = FileUtils.readLines(new File(fName), "utf-8");
		Map<String, Article> map_classified = new HashMap<String, Article>();
		for(String str : lines) {
			String[] split = str.split("\t\t");
			map_classified.put(split[1], WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(list_label.get(Integer.valueOf(split[0]))));
		}
		int countCorrect=0;
		int countWrong=0;
		for(Entry <String, Article> e: map_classified.entrySet() ) {
			if (dataset_test.get(e.getKey()).contains(e.getValue())) {
				secondLOG.info(e.getKey());
				countCorrect++;
			}
			else {
				resultLog.info(e.getKey());
				countWrong++;
			}

		}
		System.out.println("countCorrect: "+countCorrect);
		System.out.println("countWrong: "+countWrong);
		System.out.println("Accucracy: "+ (countCorrect*1.0)/(countCorrect+countWrong));
	}


}
