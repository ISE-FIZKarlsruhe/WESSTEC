package org.fiz.ise.gwifi.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public class MergeTwoFiles {

	private final static String file1 = Config.getString("MERGE_FILE1",""); 
	private final static String file2 = Config.getString("MERGE_FILE2",""); 
	private final static String FILE_ENTITY_ENTITY = Config.getString("FILE_ENTITY_ENTITY",""); 
	private final static String FILE_MODEL1_DENSE = Config.getString("FILE_MODEL1_DENSE",""); 
	private final static Set<Integer> set1 = new HashSet<>();
	private final static Set<Double> set2 = new HashSet<>();
	private final List<String> list= new ArrayList<>();
	private final List<String> list2= new ArrayList<>();
	private final Set<String> setFile2= new HashSet<>();
	private static SynchronizedCounter counterLines;
	private static SynchronizedCounter counterNotEqual;
	private static SynchronizedCounter counterEqual;
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	
	public static void main(String[] args) {
		counterLines= new SynchronizedCounter();
		counterEqual= new SynchronizedCounter();
		counterNotEqual= new SynchronizedCounter();
		MergeTwoFiles merge = new MergeTwoFiles();

		//		String file1 = "/home/rtue/eclipse-workspace/Dataset_ShortTextClassification/dblp/label_test.txt";
		//		String file2 = "/home/rtue/eclipse-workspace/Dataset_ShortTextClassification/dblp/text_test.txt";
		//		String resultFile = "dblp_dataset";
		//		List<Integer> range = new ArrayList<>(random(0,19999,19999));
		//		mergeFiles(file1, file2, resultFile,null);
		merge.startThread();
		//merge.compareFiles_2(FILE_ENTITY_ENTITY, FILE_MODEL1_DENSE);
		//checkGraphFiles();
		normlizeFiles("","");
		
	}
	public  void startThread() {
		System.out.println("Thread started...");
		final Thread t = new Thread (new Runnable() {
			@Override
			public void run() {
				while(true) {
//					System.out.println("set1 size "+set1.size()+" counterLines: "+counterLines.value() +" equal lines "+counterEqual.value()+" not equal lines "+counterNotEqual.value());
//					System.out.println("list size "+list.size()+" list2 size "+list2.size()+", counterLines: "+counterLines.value()+" equal lines "+counterEqual.value()+" not equal lines "+counterNotEqual.value());
					System.out.println("counterLines: "+counterLines.value()+" equal lines "+counterEqual.value()+" not equal lines "+counterNotEqual.value());
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}			
				}
			}
		});
		t.setDaemon(true);
		t.start();
	}
	public static void checkGraphFiles() {
		Map<String, String> map1 = new HashMap<>();
		String file1 ="/home/rima/playground/LINE/linux/Data/entity-category/backup_dataset_LINE_EntityEntitiyID.txt";
		String sCurrentLine;
//		try {
//			BufferedReader b = new BufferedReader(new FileReader(file2));
//			counterLines.setValue(0);
//			while ((sCurrentLine = b.readLine()) != null) {
//				int i = sCurrentLine.indexOf('\t', 1 + sCurrentLine.indexOf('\t', 1));
//				String secondPart = sCurrentLine.substring(i+1);
//				String firstPart = sCurrentLine.substring(0, i);
//				set2.add(Double.valueOf(secondPart));
//				if (Double.compare(Double.valueOf(map1.get(firstPart)), Double.valueOf(secondPart))!=0) {
//					counterNotEqual.increment();
//					secondLOG.info(sCurrentLine);
//					//System.out.println(map1.get(secondPart)+" not equal to "+Double.valueOf(secondPart));
//					//System.exit(1);
//					
//				}
//				else{
//					counterEqual.increment();
//				}
//			}
//			
//		} catch (Exception e) {
//			// TODO: handle exception
//		}
		int count =0;
		Set<String> set1 = new HashSet<>();
		List<String> lst = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(file1))) {
			while ((sCurrentLine = br.readLine()) != null) {
				int i = sCurrentLine.indexOf('\t', 1 + sCurrentLine.indexOf('\t', 1));
				String firstPart = sCurrentLine.substring(0, i);
				String secondPart = sCurrentLine.substring(i+1);
				if (set1.contains(firstPart)) {
					System.out.println("Set contains the second part "+secondPart);
					//System.exit(1);
				}
				lst.add(firstPart);
				set1.add(firstPart);
				counterLines.increment();
				count++;
			}
			System.out.println("number of lines file1 "+counterLines.value());
			System.out.println("size of the map "+set1.size());
			System.out.println("size of the lst "+lst.size());
			System.out.println("size of the count "+count);
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void normlizeFiles(String file_Ent_Ent, String file_Original) {
		try {
			file_Ent_Ent= "/home/rima/playground/LINE/linux/Data/entity-category/dataset_LINE_model1_dense.txt";
			file_Original="/home/rima/playground/LINE/linux/Data/entity-entity-bugFixed/dataset_LINE_model1";
			List<String> result = new LinkedList<>();

			Map<String, String> map_Ent_Ent = new HashMap<>();

			BufferedReader br = new BufferedReader(new FileReader(file_Ent_Ent));
			String sCurrentLine;
			int count =0;
			while ((sCurrentLine = br.readLine()) != null) {
				int i = sCurrentLine.indexOf('\t', 1 + sCurrentLine.indexOf('\t', 1));
				String secondPart = sCurrentLine.substring(i+1);
				String firstPart = sCurrentLine.substring(0, i);
//				i=sCurrentLine.indexOf('\t', 1);
//				String fFirstPart=firstPart.substring(0,i);
//				String sFirstPart=firstPart.substring(0,i);
				map_Ent_Ent.put(firstPart, secondPart);
				count++;
				counterLines.increment();
			}
			counterLines.incrementbyValue(0);
			System.out.println("Finished reading the first file "+file_Ent_Ent+ " int wil start reading the "+file_Original);
			br.close();
			count =0;
			br = new BufferedReader(new FileReader(file_Original));
			while ((sCurrentLine = br.readLine()) != null) {
				int i = sCurrentLine.indexOf('\t', 1 + sCurrentLine.indexOf('\t', 1));
				String firstPart = sCurrentLine.substring(0, i);
				String correctValue = map_Ent_Ent.get(firstPart);
				if (correctValue!=null) {
					correctValue=correctValue+".000000";
					result.add(firstPart+"\t"+correctValue);
					counterEqual.increment();
				}
				else{
					counterNotEqual.increment();
				}
			}
			br.close();
			counterLines.increment();
			System.out.println("writing to a file");
			FileUtil.writeDataToFile(result, "java_denseFile",false);
//			for(Entry<String, String> e: map_Ent_Cat.entrySet()) {
//				String firstPart = e.getKey();
//				String seconPart = String.valueOf(Integer.parseInt(e.getValue())/max_Ent_Ent);
//			}

//			br = new BufferedReader(new FileReader(file_dense));
//			count =0;
//			while ((sCurrentLine = br.readLine()) != null) {
//				int i = sCurrentLine.indexOf('\t', 1 + sCurrentLine.indexOf('\t', 1));
//				String secondPart = sCurrentLine.substring(i+1);
//				String firstPart = sCurrentLine.substring(0, i);
//				if (map_Ent_Ent.get(firstPart)!=null) {
//					result.add(firstPart+"\t"+Double.valueOf(secondPart)/max_Ent_Ent);
//				}
//				else if(map_Ent_Cat.get(firstPart)!=null) {
//					result.add(firstPart+"\t"+Double.valueOf(secondPart)/max_Ent_Cat);
//
//				}
//				else {
//					System.out.println("none of the maps contains the specific element in dense network "+sCurrentLine);
//					System.exit(1);
//				}
//				br.close();
//				System.out.println("Linked list size "+result.size());		
//				for (int j = result.size()-1; j >=0; j--) {
//
//				}
//
//
//			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	public void compareFiles_2(String file1, String file2) {
//		file1="sample";
//		file2="sample_dense";

		Map<String, String> map1 = new HashMap<>();
		Map<String, String> map2 = new HashMap<>();
		//List<String> list = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(file1))) {
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
//				int i = sCurrentLine.indexOf('\t', 1 + sCurrentLine.indexOf('\t', 1));
//				String secondPart = sCurrentLine.substring(i+1);
//				String firstPart = sCurrentLine.substring(0, i);
//				map1.put(firstPart, secondPart);
//				set1.add(Integer.parseInt(secondPart));
//				counterLines.increment();
				list.add(sCurrentLine);
			}
			System.out.println("number of lines file1 "+counterLines.value());
			System.out.println("line1 are in the set");
			System.out.println("size of the map "+map1.size());
			
			BufferedReader b = new BufferedReader(new FileReader(file2));
			counterLines.setValue(0);
			
			while ((sCurrentLine = b.readLine()) != null) {
				int i = sCurrentLine.indexOf('\t', 1 + sCurrentLine.indexOf('\t', 1));
				String firstPart = sCurrentLine.substring(0, i);
				String secondPart = sCurrentLine.substring(i+1);
//				String fSecondPart = Double.valueOf(secondPart).intValue();
				
//				set2.add(Double.valueOf(secondPart));
				//list.add(firstPart+"\t"+Double.valueOf(secondPart).intValue());
//				list2.add(firstPart+"\t"+Double.valueOf(secondPart).intValue());
//				if (list.contains(firstPart+"\t"+Double.valueOf(secondPart).intValue())) {
//					list.remove(firstPart+"\t"+Double.valueOf(secondPart).intValue());
//				}
				if (!list.contains(firstPart+"\t"+Double.valueOf(secondPart).intValue())) {
					counterNotEqual.increment();
					System.out.println(firstPart+" "+map1.get(firstPart)+" not equal to "+Double.valueOf(secondPart));
					//System.exit(1);
				}
				else{
					counterEqual.increment();
					
				}
				counterLines.increment();
			}
			System.out.println("Size of list "+list.size()+" "+"Size of list2 "+list2.size());
			list.retainAll(list2);
			System.out.println("Site after retain "+list.size());
			FileUtil.writeDataToFile(list, "Comparison_of_Files_afterRetain",false);
			System.out.println("number of lines file1 "+counterLines.value());
			System.out.println("line2 Elements are in the set");
			//			System.out.println("size of the map "+map2.size());
//			System.out.println("set1 max "+Collections.max(set1)+" set1 size "+set1.size()+" set1 min "+Collections.min(set1));
//			System.out.println("set2 max "+Collections.max(set2)+" set2 size "+set2.size()+" set2 min "+Collections.min(set2));
			//			System.out.println("Comparing files");
			//			for(Entry<String,String> e :map1.entrySet()) {
			//				if (Double.compare(Double.valueOf(map2.get(e.getKey())), Double.valueOf(e.getValue()))!=0 ) {
			//					System.out.println(map2.get(e.getKey())+" not equal to "+Double.valueOf(e.getValue()));
			//				}
			//			}

			b.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void compareFiles(String file1, String file2) {
		try {
			List<String> lines1 = FileUtils.readLines(new File(file1), "utf-8");
			List<String> lines2 = FileUtils.readLines(new File(file2), "utf-8");
			System.out.println("Files are read line1 size "+ lines1.size()+" "+"line2 size "+ lines2.size());
			Map<String, String> map1 = new HashMap<>();
			Map<String, String> map2 = new HashMap<>();
			for(String line1: lines1) {
				int i = line1.indexOf('\t', 1 + line1.indexOf('\t', 1));
				String firstPart = line1.substring(0, i);
				String secondPart = line1.substring(i+1);
				map1.put(firstPart, secondPart);
				System.out.println("size of the map1 "+map1.size());
			}
			System.out.println("line1 Elements are in the map");
			for(String line2: lines2) {
				int i = line2.indexOf('\t', 1 + line2.indexOf('\t', 1));
				String firstPart = line2.substring(0, i);
				String secondPart = line2.substring(i+1);
				if (map1.containsKey(firstPart)) {
					map2.put(firstPart, secondPart);
				}
				System.out.println("size of the map2 "+map2.size());

			}
			System.out.println("line2 Elements are in the map");
		} catch (IOException e) {

			e.printStackTrace();
		}
	}
	public static List<Integer> random(int beging, int end, int length) {
		List<Integer> range = IntStream.range(beging, end).boxed()
				.collect(Collectors.toCollection(ArrayList::new));
		Collections.shuffle(range);
		if (length> range.size()) {
			System.out.println("Cannot generate a random list number, length is bigger than the size");
			System.exit(1);
		}
		List<Integer> sList = new ArrayList<>(range.subList(0, length));
		return sList;
	}

	public static void mergeFiles(String file1, String file2, String fileResult,List<Integer> range) {
		try {
			List<String> lines2 = FileUtils.readLines(new File(file2), "utf-8");
			List<String> lines1 = FileUtils.readLines(new File(file1), "utf-8");
			List<String> result = new ArrayList<>();
			if (lines2.size()==lines1.size()) {
				if (range==null) {
					for (int i = 0; i < lines1.size(); i++) {
						result.add(lines1.get(i)+" "+lines2.get(i));
					}
				}
				else {
					//					for(Integer i : range) {
					//						result.add(lines1.get(i)+" "+lines2.get(i));
					//					}
				}
			}
			FileUtil.writeDataToFile(result, fileResult);
			System.out.println("Finished writing to a file");
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
