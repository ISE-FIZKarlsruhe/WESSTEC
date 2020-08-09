package org.fiz.ise.gwifi.dataset.shorttext.baseline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.fiz.ise.gwifi.util.FileUtil;

import com.google.common.io.Files;

public class PartitioningDataset {

	public static void main(String[] args) {
		// AG_Extract();
		//WEB_Extract();
		AG_Random();
	}

	private static void AG_Random() {
		try {
			List<String> lines = FileUtils.readLines(new File("/home/rima/playground/GeneralFiles/gwifi/Dataset_ShortTextClassification/ag_news_csv/train.csv"), "utf-8");
			//			List<String> lines = FileUtils.readLines(new File("/home/rtue/eclipse-workspace/Dataset_ShortTextClassification/ag_news_csv/train.csv"), "utf-8");
			List<Integer> samples = new ArrayList<>();
			samples.add(100);
			samples.add(200);
			samples.add(500);
			samples.add(1000);
			samples.add(2000);
			for (int j = 0; j< 10; j++) {
				for(int i: samples) {
					Collections.shuffle(lines);
					List<String> sub = new ArrayList<>(lines.subList(0, i));
					final Map<String, List<String>> data = new HashMap<>();
					for(String line:sub) {
						String[] split = line.split("\",\"");
						String label = split[0].replace("\"", "");
						String title = split[1].replace("\"", "");
						String description = split[2].replace("\"", "");
						String text = title + " " + description;

						List<String> list = data.get(label);
						if (list == null) {
							List<String> newList = new ArrayList<>();
							newList.add(text);
							data.put(label, newList);
						} else {
							list.add(text);
							data.put(label, new ArrayList<>(list));
						}
					}
					for (Entry<String, List<String>> e : data.entrySet()) {
						final List<String> value = e.getValue();
						String fileName = "";
						if (e.getKey().equals("1")) {
							fileName = "World";
						} else if (e.getKey().equals("2")) {
							fileName = "Sports";
						} else if (e.getKey().equals("3")) {
							fileName = "Business";
						} else if (e.getKey().equals("4")) {
							fileName = "Technology";
						}
						handle(i, value, fileName + "-" + i+ "-" +j );
					}
				}


			} 
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	private static void WEB_Extract() {

		final Map<String, List<String>> data = new HashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(
				"/home/rima/playground/GeneralFiles/gwifi/Dataset_ShortTextClassification/data-web-snippets/train.txt"))) {
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				String[] split = sCurrentLine.split(" ");
				String label = split[split.length - 1];
				String text = sCurrentLine.substring(0, sCurrentLine.length() - (label).length()).trim();

				List<String> list = data.get(label);
				if (list == null) {
					List<String> newList = new ArrayList<>();
					newList.add(text);
					data.put(label, newList);
				} else {
					list.add(text);
					data.put(label, new ArrayList<>(list));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<Integer> numberOfSamples = Arrays.asList(3000);
		for(int i=0;i<numberOfSamples.size();i++) {
			int numberOfSample = numberOfSamples.get(i);
			for(Entry<String, List<String>> e:data.entrySet()) {
				final List<String> value = e.getValue();
				Collections.shuffle(value);
				String fileName="";
				if (e.getKey().equals("culture-arts-entertainment")) {
					fileName="Arts";
				}
				else if (e.getKey().equals("education-science")) {
					fileName="Education";

				}
				else if (e.getKey().equals("politics-society")) {
					fileName="Society";

				}
				else {
					fileName=StringUtils.capitalize(e.getKey());
				}
				handle(numberOfSample,value,fileName+"-"+numberOfSample);
			}
		}
	}

	private static void AG_Extract() {
		final Map<String, List<String>> data = new HashMap<>();
		// try (BufferedReader br = new BufferedReader(new
		// FileReader("/home/rtue/eclipse-workspace/Dataset_ShortTextClassification/ag_news_csv/train.csv")))
		// {
		try (BufferedReader br = new BufferedReader(new FileReader(
				"/home/rima/playground/GeneralFiles/gwifi/Dataset_ShortTextClassification/ag_news_csv/train.csv"))) {
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				String[] split = sCurrentLine.split("\",\"");
				String label = split[0].replace("\"", "");

				String title = split[1].replace("\"", "");
				String description = split[2].replace("\"", "");
				String text = title + " " + description;

				List<String> list = data.get(label);
				if (list == null) {
					List<String> newList = new ArrayList<>();
					newList.add(text);
					data.put(label, newList);
				} else {
					list.add(text);
					data.put(label, new ArrayList<>(list));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		List<Integer> numberOfSamples = Arrays.asList(5000, 10000, 20000);
		for (int i = 0; i < numberOfSamples.size(); i++) {
			int numberOfSample = numberOfSamples.get(i);
			for (Entry<String, List<String>> e : data.entrySet()) {
				final List<String> value = e.getValue();
				Collections.shuffle(value);
				String fileName = "";
				if (e.getKey().equals("1")) {
					fileName = "World";
				} else if (e.getKey().equals("2")) {
					fileName = "Sports";
				} else if (e.getKey().equals("3")) {
					fileName = "Business";
				} else if (e.getKey().equals("4")) {
					fileName = "Technology";
				}
				handle(numberOfSample, value, fileName + "-" + numberOfSample);
			}
		}
	}

	private static void handle(int numberOfSample, List<String> value, String fileName) {
		List<String> subList;
		if (numberOfSample>=value.size()) {
			subList = new ArrayList<>(value);
		}
		else
		{
			subList = value.subList(0, numberOfSample);
		}
		writeToFile(subList, fileName);
	}

	private static void writeToFile(List<String> subList, String fileName) {
		String folderName = "Train_TFIDF_AG_original_Partition"+fileName.split("-")[2] + File.separator + fileName.split("-")[1] + File.separator
				+ fileName.split("-")[0];
		FileUtil.createFolder(folderName);
		int i = 0;
		for (String str : subList) {
			FileUtil.writeDataToFile(Arrays.asList(str), folderName + File.separator + i, false);
			i++;
		}
	}

}
