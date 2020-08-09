package org.fiz.ise.gwifi.dataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.types.CommandlineJava.SysProperties;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.StopWordRemoval;
import org.fiz.ise.gwifi.util.StringUtil;
import org.fiz.ise.gwifi.util.SynchronizedCounter;

import riotcmd.printtokens;


public class AnalysePatentData {
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger thirdLOG = Logger.getLogger("reportsLogger");
	private static ExecutorService executor;
	private final static Integer NUMBER_OF_THREADS= Config.getInt("NUMBER_OF_THREADS",-1);
	private final static String fileNamePatent=Config.getString("DATASET_PATENT","");
	private final static String fileNamePublicationsHaveAbstract=Config.getString("DATASET_PUBLICATIONS_ABSTRACT","");
	private final static String fileNamePatentCPC=Config.getString("DATASET_PATENT_CPC","");

	private static Set<String> set_publications= Collections.synchronizedSet(new HashSet<>());
	private static Set<String> set_abstracts= Collections.synchronizedSet(new HashSet<>());

	private static Map<String, String> map_publication_abstract = new ConcurrentHashMap<>();
	private static Map<String, String> map_abstract_publication = new ConcurrentHashMap<>();
	private static Map<String, String> map_publication_application = new ConcurrentHashMap<>();
	private static Map<String, List<String>> map_application_cpc= new ConcurrentHashMap<>();

	private static SynchronizedCounter count_dublicate_publications_applications=new SynchronizedCounter();
	private static SynchronizedCounter count_application_no_CPC=new SynchronizedCounter();
	private static SynchronizedCounter count_null_app=new SynchronizedCounter();
	private static SynchronizedCounter count_null_cpc=new SynchronizedCounter();
	private static Set<String> setMostCommonCats;
	private static String file_name_patent_pub_abst="patent_publication_abstract_cpc.txt";

	public static void main(String[] args) throws Exception  {
		//		compareLabels();
		//		setMostCommonCats = findMostCommonCats("patent_publication_abstract_cpc.txt", 5);
		generateTrainandTestData(file_name_patent_pub_abst);

		//List<String> lines = FileUtils.readLines(new File("/home/rima/playground/Embeddings/PTE/workspace/patent_classification_with_label_info/patent_split_80_train.txt"), "utf-8");
		//generate_dataset_pte_baseline(lines);
		//splitTrainandTestData("patent_publication_abstract_cpc.txt",80);
		//clean_cpc_fulltitle();
	}
	
	private static void generate_dataset_pte_baseline(List<String> lst) throws Exception {
		System.out.println("Size of the patent training set is :" +lst.size());
		
		Set<String> set_dublicates = new HashSet<String>();
		for(String str : lst) {
			String[] split = str.split("\t");
			String s_abstract=StringUtil.removePunctuation(split[1].replace(".\"@en .", "")).toLowerCase().trim().replaceAll(" +", " ");
			String label= split[2];
			String key = s_abstract+" "+label;

			if (!set_dublicates.contains(key)) {
				String[] split_label = label.split(",");
				Set<String> set_label = new HashSet<String>();
				for (int i = 0; i < split_label.length; i++) {
					String first_Chars=split_label[i].substring(0,4);
					set_label.add(first_Chars);
				}
				if (set_label.size()>0) {
					for (String str_label: set_label) {
						secondLOG.info(s_abstract);
						thirdLOG.info(str_label);
					}
				}
				set_dublicates.add(key);
			}
		}
		System.out.println("After converting to set pte dataset: "+set_dublicates.size());
	}
	
	
	private static void generateTrainandTestData(String fName) throws Exception {
//		List<String> lines = FileUtils.readLines(new File(fName), "utf-8");
//		Set<String> set_dublicates = new HashSet<String>();
//		List<String> lines_no_dublicates = new ArrayList<String>();
//
//		for(String str : lines) {
//			String[] split = str.split("\t");
//			String s_abstract=StringUtil.removePunctuation(split[1].replace(".\"@en .", "")).toLowerCase().trim().replaceAll(" +", " ");
//			String label= split[2];
//			String key = s_abstract+" "+label;
//			if (!set_dublicates.contains(key)) {
//				lines_no_dublicates.add(str);
//				set_dublicates.add(key);
//			}
//		}
//		System.out.println("Original list size :"+ lines.size());
//		System.out.println("After removing the dublicates list size :"+ lines_no_dublicates.size());
//		System.out.println("set_dublicates size :"+ set_dublicates.size());
//
//		int lastIndex= (lines_no_dublicates.size()*80)/100;
//
//		List<String> list_train = new ArrayList<String>(lines_no_dublicates.subList(0, lastIndex));
//		List<String> list_test = new ArrayList<String>(lines_no_dublicates.subList(lastIndex, lines_no_dublicates.size()));
		
		List<String> list_train = FileUtils.readLines(new File("/home/rima/playground/Embeddings/PTE-master/workspace/patent_classification_with_label_info/patent_split_80_train.txt"), "utf-8");
		List<String> list_test = FileUtils.readLines(new File("/home/rima/playground/Embeddings/PTE-master/workspace/patent_classification_with_label_info/patent_split_80_test.txt"), "utf-8");

		//System.out.println("Size the lines_no_dublicates size: " + list_train.size()+list_test.size());
		System.out.println("Size the  list_train: " + list_train.size());
		System.out.println("Size the  list_test: " + list_test.size());
		System.out.println("Size the total: " + (list_test.size()+list_train.size()));

		//generateDatasetPatentForBinaryClassification_train(list_train,5);
		//generateDatasetPatentForBinaryClassification_test(list_test);
		writeDataToFile(list_train);
	}
	/*
	 * Generated a dataset set for test a bun classification model no negative samples
	 * s1 l1 1
	 * s1 l2 1 and so on
	 */
	
	private static  void generateDatasetPatentForBinaryClassification_test(List<String> lst){
		
		System.out.println("I am in generateDatasetPatentForBinaryClassification_test"+lst.size());
		int count=0;
		Set<String> set_dublicates = new HashSet<String>();
		for(String str : lst) {
			String[] split = str.split("\t");
			String s_abstract=StringUtil.removePunctuation(split[1].replace(".\"@en .", "")).toLowerCase().trim().replaceAll(" +", " ");
			String label= split[2];
			String key = s_abstract+" "+label;
			if (!set_dublicates.contains(key)) {
				String[] split_label = label.split(",");
				Set<String> set_label = new HashSet<String>();
				for (int i = 0; i < split_label.length; i++) {
					String first_Chars=split_label[i].substring(0,4);
					set_label.add(first_Chars);
				}
				for (String str_label: set_label) {
					thirdLOG.info(s_abstract+"\t\t"+str_label+"\t\t"+"1");
				}
				set_dublicates.add(key);
			}
		}
		System.out.println("Size of the dataset after dublicates removed test: "+set_dublicates.size());
		System.out.println("Size of the dataset original test: "+lst.size());
	}
	/*
	 * Generated a dataset set for training a bin classification model with negative samples
	 * s1 l1 1
	 * s1 l2 1 
	 * s1 l3 0 
	 * s1 l4 0 
	 * s1 l5 0 and so on (3 negatieve samples only)
	 * 
	 */
	private static  void generateDatasetPatentForBinaryClassification_train(List<String> lst, int negativeSamples) throws IOException {
		Set<String> set_dublicates = new HashSet<String>();
		Set<String> set_allLabels = new HashSet<String>(getAllLabelsOfPatents());
		int count_lines=0;
		
		for(String str : lst) {
			String[] split = str.split("\t");
			String s_abstract=StringUtil.removePunctuation(split[1].replace(".\"@en .", "")).toLowerCase().trim().replaceAll(" +", " ");
			String label= split[2];
			String key = s_abstract+" "+label;
			if (!set_dublicates.contains(key)) {
				String[] split_label = label.split(",");
				Set<String> set_label = new HashSet<String>();
				for (int i = 0; i < split_label.length; i++) {
					String first_Chars=split_label[i].substring(0,4);
					set_label.add(first_Chars);
				}
				count_lines+=set_label.size();
				for (String str_label: set_label) {
					secondLOG.info(s_abstract+"\t\t"+str_label+"\t\t"+"1");
					//thirdLOG.info("1");
				}
				Set<String> set_temp_remove_labels = new HashSet<String>(set_allLabels);
				set_temp_remove_labels.removeAll(set_label);
				List<String> lst_temp = new ArrayList<String>(set_temp_remove_labels);
				
				for (int i = 0; i < negativeSamples; i++) {
					Random rand = new Random();
					int int_random = rand.nextInt(lst_temp.size()); 
					secondLOG.info(s_abstract+"\t\t"+lst_temp.get(int_random)+"\t\t"+"0");
					//thirdLOG.info("0");
				}
				count_lines+=5;
				set_dublicates.add(key);
			}
		}
		System.out.println("Size of the dataset after dublicates removed: "+set_dublicates.size());
		System.out.println("Size of the dataset original: "+lst.size());
		System.out.println("Total number of the lines: "+count_lines);
	}
	private static void writeDataToFile(List<String> lst) {
		int count_num_samples_one_cat=0;
		Set<String> set_dublicates = new HashSet<String>();
		for(String str : lst) {
			String[] split = str.split("\t");
			String s_abstract=StringUtil.removePunctuation(split[1].replace(".\"@en .", "")).toLowerCase().trim().replaceAll(" +", " ");
			String label= split[2];
			String key = s_abstract+" "+label;
			if (!set_dublicates.contains(key)) {
				String[] split_label = label.split(",");
				Set<String> set_label = new HashSet<String>();
				for (int i = 0; i < split_label.length; i++) {
					String first_Chars=split_label[i].substring(0,4);
					set_label.add(first_Chars);
				}
				StringBuilder strBuild = new StringBuilder();
				count_num_samples_one_cat+=set_label.size();
				for (String str_label: set_label) {
					strBuild.append(str_label+" ");
				}
				if (set_abstracts.contains(s_abstract)) {
					System.out.println(str);
				}
				secondLOG.info(s_abstract);
				thirdLOG.info(strBuild.substring(0, strBuild.length()-1));

				set_dublicates.add(key);
			}
		}
		System.out.println("*********After converting to set********: "+set_dublicates.size());
		System.out.println("count_num_samples_one_cat : "+count_num_samples_one_cat);
	}
	/*
	 * This function have been used to generate train and test slit general no preprocessing whatsoever included
	 * Also to train PTE I used the 80% of the dataset - train set 
	 */
	private static void splitTrainandTestData(String fName, int percentage) throws Exception {
		List<String> lines = FileUtils.readLines(new File(fName), "utf-8");
		Set<String> set_dublicates = new HashSet<String>();
		List<String> lines_no_dublicates = new ArrayList<String>();

		for(String str : lines) {
			String[] split = str.split("\t");
			String s_abstract=StringUtil.removePunctuation(split[1].replace(".\"@en .", "")).toLowerCase().trim().replaceAll(" +", " ");
			String label= split[2];
			String key = s_abstract+" "+label;
			if (!set_dublicates.contains(key)) {
				lines_no_dublicates.add(str);
				set_dublicates.add(key);
			}
		}
		System.out.println("Original list size :"+ lines.size());
		System.out.println("After removing the dublicates list size :"+ lines_no_dublicates.size());
		System.out.println("set_dublicates size :"+ set_dublicates.size());

		int lastIndex= (lines_no_dublicates.size()*percentage)/100;

		List<String> list_train = new ArrayList<String>(lines_no_dublicates.subList(0, lastIndex));
		List<String> list_test = new ArrayList<String>(lines_no_dublicates.subList(lastIndex, lines_no_dublicates.size()));

		System.out.println("Size the lines_no_dublicates size: " + lines_no_dublicates.size());
		System.out.println("Size the  list_train: " + list_train.size());
		System.out.println("Size the  list_test: " + list_test.size());
		System.out.println("Size the total: " + (list_test.size()+list_train.size()));

		FileUtil.writeDataToFile(list_train, "patent_split_80_train.txt");
		FileUtil.writeDataToFile(list_test, "patent_split_80_test.txt");
	}
	private static void compareLabels() throws IOException {
		List<String> labels_train  = FileUtils.readLines(new File("/home/rima/playground/Datasets/Features/LabelInformation/patent/patent_train_labels_no_dublicate.txt"), "utf-8");
		List<String> labels_test  = FileUtils.readLines(new File("/home/rima/playground/Datasets/Features/LabelInformation/patent/patent_test_labels_no_dublicate.txt"), "utf-8");

		Set<String> s_train = new HashSet<String>();
		Set<String> s_test = new HashSet<String>();

		for(String str: labels_train) {
			String[] split = str.split(" ");
			for (int i = 0; i < split.length; i++) {
				s_train.add(split[i]);
			}
		}
		for(String str: labels_test) {
			String[] split = str.split(" ");
			for (int i = 0; i < split.length; i++) {
				if (!s_train.contains(split[i])) {
					System.out.println(split[i]);
				}
				s_test.add(split[i]);
			}
		}

		System.out.println("Size of the train : "+s_train.size());
		System.out.println("Size of the test : "+s_test.size());
	}
	private static void writeDataToFile_filter(List<String> lst) {
		Set<String> set_dublicates = new HashSet<String>();
		for(String str : lst) {
			String[] split = str.split("\t");
			String s_abstract=split[1];
			String label= split[2];

			String key = s_abstract+" "+label;
			if (!set_dublicates.contains(key)) {
				String[] split_label = label.split(",");
				Set<String> set_label = new HashSet<String>();
				for (int i = 0; i < split_label.length; i++) {
					String first_Chars=split_label[i].substring(0,4);
					if (setMostCommonCats.contains(first_Chars)) {
						set_label.add(first_Chars);
					}
				}
				if (set_label.size()>0) {
					StringBuilder strBuild = new StringBuilder();
					for (String str_label: set_label) {
						strBuild.append(str_label+" ");
					}
					if (set_abstracts.contains(s_abstract)) {
						System.out.println(str);
					}
					secondLOG.info(s_abstract);
					thirdLOG.info(strBuild.substring(0, strBuild.length()-1));
				}
				set_dublicates.add(key);
			}
		}
		System.out.println("After converting to set: "+set_dublicates.size());
	}
	private static Set<String> getAllLabelsOfPatents() throws IOException {
		List<String> lst = FileUtils.readLines(new File(file_name_patent_pub_abst), "utf-8");
		Set<String> set_label = new HashSet<String>();
		for(String str : lst) {
			String[] split = str.split("\t");
			String label= split[2];
			String[] split_label = label.split(",");
			for (int i = 0; i < split_label.length; i++) {
				String first_Chars=split_label[i].substring(0,4);
				set_label.add(first_Chars);
			}
		}	
		System.out.println("Total size of the labels: "+set_label.size());
		return set_label;
	}

	
	private static void analyseExtractedResults(String fName) throws Exception {
		Set<String> set_first_chars= new HashSet<String>();
		Map<String, Set<String>> map_dataset = new HashMap<>();
		List<String> lines = FileUtils.readLines(new File(fName), "utf-8");
		int count_single_label=0;
		int count_multiple_label=0;

		for(String str : lines) {
			String[] split = str.split("\t");
			String label= split[2];

			String[] split_label = label.split(",");

			Set<String> set_label = new HashSet<String>();
			for (int i = 0; i < split_label.length; i++) {
				String first_Chars=split_label[i].substring(0,4);
				set_label.add(first_Chars);
			}
			if (set_label.size()>1) {
				count_multiple_label++;
			}
			else if (set_label.size()==1) {
				count_single_label++;
			}
		}
		System.out.println("Total lines: "+lines.size());

		System.out.println("count_single_label: "+count_single_label);
		System.out.println("count_multiple_label: "+count_multiple_label);
		System.out.println("The size of the set set_first_char: "+set_first_chars.size());
	}
	private static void get_patent_id_abstract_labels() throws IOException, Exception {
		map_publication_abstract=new HashMap<String, String>(readExtractedData("patent_publicationID_abstract_clean.txt"));
		set_publications= new HashSet<String>(map_publication_abstract.keySet());
		System.out.println("Size of the publications: "+map_publication_abstract.size());
		//extractPublications();
		map_publication_application= new HashMap<String, String>(readExtractedData("patent_publicationID_application_clean.txt"));
		System.out.println("Size of the publications and applications: "+		map_publication_application.size());

		//extractApplication();

		extractCPCCodeApplications();
		System.out.println("Size of the applications and cpc: "+		map_application_cpc.size());



		//		System.out.println("Count of the applications null: "+		count_null_app.value());
		//		System.out.println("Count of the  cpc null: "+		count_null_cpc.value());

		//		System.out.println("**********************************");
		//		System.out.println(map_publication_application.get("1892927/A1"));
		//		System.out.println("**********************************");
		//		System.out.println(map_application_cpc.get("07021657").size());
		//		System.out.println("**********************************");
		//		System.out.println(map_application_cpc.get("07021657"));

		mergeResults();
		System.out.println("count_application_no_CPC: "+count_application_no_CPC.value() );
	}

	private static void mergeResults() {
		for(Entry<String, String> e : map_publication_abstract.entrySet()) {
			String publicationID = e.getKey();
			String publicationAbstract = e.getValue();
			String application = map_publication_application.get(publicationID);



			if (application!=null &&map_application_cpc.containsKey(application)){
				count_application_no_CPC.increment();
				List<String> lst = map_application_cpc.get(application);
				StringBuilder str_cpc=new StringBuilder();
				for(String s : lst) {
					str_cpc.append(s+",");
				}
				secondLOG.info(publicationID+"\t"+publicationAbstract+"\t"+str_cpc.toString().substring(0, str_cpc.length()-1));
			}
			//			if (publicationID.equals("1045302/A1")) {
			//				System.out.println("pub ID: "+publicationID);
			//				
			//				if (map_application_cpc.containsKey(application)){
			//					System.out.println("map_application_cpc contains the application: "+application+" "+publicationID);
			//					System.out.println("*******: "+map_application_cpc.get(application));
			//				}
			//				else {
			//					System.out.println("***********map_application_cpc does not contain the application: "+application+" "+publicationID);
			//					count_null_cpc.increment();
			//					//if (count_null_cpc.value()==2) {
			//						//System.exit(0);
			////					}
			//				}
			//				System.exit(0);
			//			}


			//			if (application!=null && map_application_cpc.containsKey(application)) {
			//				List<String> lst = map_application_cpc.get(application);
			//				StringBuilder str_cpc=new StringBuilder();
			//				for(String s : lst) {
			//					str_cpc.append(s+",");
			//				}
			//				secondLOG.info(publicationID+"\t"+publicationAbstract+"\t"+str_cpc.toString().substring(0, str_cpc.length()-1));
			//			}
			//			else {
			//				count_application_no_CPC.increment();
			//				if (count_application_no_CPC.value()==3) {
			//					System.out.println("application does not have a cpc: "+application+" "+publicationID);
			//					System.out.println("e key and a value: "+e.getKey()+" "+e.getValue());
			//					System.exit(1);
			//				}
			//			}
		}
	}

	private static void extractCPCCodeApplications() throws Exception{
		executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
		int count=0;
		BufferedReader br = new BufferedReader(new FileReader(fileNamePatentCPC)); 
		String st; 
		while ((st = br.readLine()) != null) {
			executor.execute(runextractCPCCodeApplications(st, ++count));
		} 
		br.close();
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		System.out.println("Total number of lines: "+count);
		System.out.println("map_application_cpc: "+map_application_cpc.size());
		//		System.out.println("Total number of lines A1: "+count_A1.value());

	}
	private static Runnable runextractCPCCodeApplications(String line, int i) {
		return () -> {
			if (line.contains("http://data.epo.org/linked-data/def/patent/classificationCPCInventive")) {
				String[] split = line.split("<http://data.epo.org/linked-data/def/patent/classificationCPCInventive>");
				if (split[0].contains("id/application/EP/")) {
					String application=getApplicationID(split[0]);
					try {
						String cpc=getCPC(split[1]);
						synchronized (map_application_cpc) {
							List<String> tmp;
							if(map_application_cpc.containsKey(application)) {
								tmp=new ArrayList<String>(map_application_cpc.get(application));
								//								List<String> tmp =Collections.synchronizedList(new ArrayList<>(map_application_cpc.get(application)));
								//Collections.synchronizedList(tmp);
							}
							else {
								tmp = new ArrayList<String>();
							}
							tmp.add(cpc);
							map_application_cpc.put(application, tmp);
						}
					} catch (Exception e) {
						System.out.println(line);
						System.out.println(split[0]);
						System.exit(1);
					}
				}

			}
		};
	}
	private static void extractApplication() throws IOException, InterruptedException {
		executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
		int count=0;
		BufferedReader br = new BufferedReader(new FileReader(fileNamePatent)); 
		String st; 
		while ((st = br.readLine()) != null) {
			executor.execute(runExtractFeatures(st, ++count));
		} 
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		br.close();
		System.out.println("Total number of lines application: "+count);
		System.out.println("map_publication_application: "+map_publication_application.size());
	}
	private static Runnable runExtractFeatures(String line, int i) {
		return () -> {
			if (line.contains("http://data.epo.org/linked-data/def/patent/application")) {
				String[] split = line.split("<http://data.epo.org/linked-data/def/patent/application>");
				String application=null;
				if (split[0].contains("/publication/EP/")) {
					String publicationID_type=null;
					try {
						publicationID_type=getPublicationID(split[0]);
						if (set_publications.contains(publicationID_type)) {
							application=getApplicationID(split[1]);
							synchronized (count_dublicate_publications_applications) {
								if (map_publication_application.containsKey(publicationID_type) ) {
									count_dublicate_publications_applications.increment();
								}
								map_publication_application.put(publicationID_type, application);
							}
							//							resultLog.info(publicationID_type+"\t"+application);
						}

					} catch (Exception e) {
						System.out.println("publicationID_type: "+publicationID_type);
						System.out.println(line);
						System.out.println(split[0]);
						System.exit(1);
					}
				}
			}
		};
	}
	private static Set<String> findMostCommonCats(String fName, int num) throws Exception {
		Set<String> set_common_cats= new HashSet<String>();
		Map<String, Integer> map_result = new HashMap<String, Integer>();

		List<String> lines = FileUtils.readLines(new File(fName), "utf-8");
		for(String line: lines) {
			String[] split = line.split("\t");
			String label= split[2];
			String[] split_label = label.split(",");
			Set<String> set_label = new HashSet<String>();
			for (int i = 0; i < split_label.length; i++) {
				String first_Chars=split_label[i].substring(0,4);
				set_label.add(first_Chars);
			}
			for(String c : set_label) {
				map_result.merge(c, 1, Integer::sum);
			}
		}
		Map<String, Integer> sortByValueDescending = MapUtil.sortByValueDescending(map_result);
		int count=0;
		for(Entry<String, Integer> e : sortByValueDescending.entrySet()) {
			set_common_cats.add(e.getKey());
			count++;
			if (count==num) {
				break;
			}
		}
		System.out.println("Size of the most common cats: "+set_common_cats.size());
		return set_common_cats;
	}
	private static void clean_cpc_fulltitle_doc2vec_dataset() throws Exception { 
		Set<String> set_cpc_4_char = new HashSet<String>();

		List<String> lines = FileUtils.readLines(new File("cpc_4_char_title.txt"), "utf-8");
		for(String line: lines) {
			String[] split = line.split(" <http://data.epo.org/linked-data/def/cpc/fullTitle> ");
			String cpc_4_char = split[0].substring(split[0].length()-5,split[0].length()).replace(">", "");
			set_cpc_4_char.add(cpc_4_char);

			if (cpc_4_char.length()!=4) {
				System.out.println("The length is not equal to 4"+line);
				System.exit(0);
			}
			String full_title = StringUtil.removePunctuation(split[1].toLowerCase().trim().replaceAll(" +", " "));
			//secondLOG.info(cpc_4_char+"\t"+full_title);
		}
		System.out.println("set_cpc_4_char: "+set_cpc_4_char.size());

	}
	private static void extractPublications() throws IOException, InterruptedException {
		executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
		int count=0;
		BufferedReader br = new BufferedReader(new FileReader(fileNamePublicationsHaveAbstract)); 
		String st; 
		while ((st = br.readLine()) != null) {
			executor.execute(extractPublicationIDs(st, ++count));
		} 
		br.close();
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		System.out.println("Total number of publications: "+set_publications.size());
		System.out.println("Total number of publications map: "+map_publication_abstract.size());
		System.out.println("Size of map_abstract_publication: "+map_abstract_publication.size());
	}

	private static Runnable extractPublicationIDs(String line, int i) {
		return () -> {
			if (line.contains("<http://purl.org/dc/terms/abstract>")) {
				String[] split = line.split("<http://purl.org/dc/terms/abstract>");

				if (split[0].contains("/publication/EP/") && split[1].contains("@en")) {
					try {
						String publicationID_type=getPublicationID(split[0]);
						if (publicationID_type!=null) {
							set_publications.add(publicationID_type);
							map_publication_abstract.put(publicationID_type, split[1].trim());
							map_abstract_publication.put(split[1].trim(),publicationID_type);
						}
					} catch (Exception e) {
						System.out.println(line);
						System.out.println(split[0]);
						System.exit(1);
					}
				}
			}
		};
	}
	private static String getPublicationID(String str) {
		int index_begin=str.indexOf("/publication/EP/");
		int index_end=str.indexOf("/-");
		if (index_end>0 && str.contains("/publication/EP/")) {
			return str.substring(index_begin,index_end).replace("/publication/EP/", "");
		}
		else {
			//			System.out.println("********************************");
			//			System.out.println(str);
			return null;
		}
	}
	private static String getApplicationID(String str) {
		try {
			int index_end_app=str.indexOf(">");
			return str.substring(str.indexOf("id/application/EP/"),index_end_app).replace("id/application/EP/","");
		} catch (Exception e) {

			System.out.println("******************");
			System.out.println(str);
			System.exit(1);
			return null;
		}

	}
	private static Map<String, String> readExtractedData(String fName) throws IOException{
		Map<String, String> map = new HashMap<String, String>();
		BufferedReader br = new BufferedReader(new FileReader(fName)); 
		String st; 
		while ((st = br.readLine()) != null) {
			map.put(st.split("\t")[0], st.split("\t")[1]);
		} 
		br.close();
		return map;
	}
	private static String getCPC(String str) {
		int index_end_app=str.indexOf("> .");
		return str.substring(str.indexOf("/cpc/"),index_end_app).replace("/cpc/","");
	}
}
