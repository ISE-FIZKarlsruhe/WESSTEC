package edu.kit.aifb.gwifi.util.pageview;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WikipediaTrendsComparer {

	private static final String UTF8_BOM = "\uFEFF";

	private static final String URL = "http://www.wikipediatrends.com/csv.php?";
	private static final String QUERY = "query[]=";

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");

	private static final int BUFFER_SIZE = 10 * 1024 * 1024;

	public void read(String filePath, String outputFolder) throws Exception {
		File file = new File(filePath);
		BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
		BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
		String line;
		String first = null;
		boolean head = true;
		while ((line = reader.readLine()) != null) {
			if (!line.startsWith("\t")) {
				if (head) {
					first = line.replace(UTF8_BOM, "");
					head = false;
				} else {
					first = line;
				}
			} else {
				line = line.replace("\t", "");
				String outputPath = outputFolder + first + "_" + line + ".txt";
				httpDownload(url(first, line), outputPath);
			}
		}
		reader.close();
	}

	private String url(String title1, String title2) throws Exception {
		title1 = decorateUrl(title1);
		title2 = decorateUrl(title2);
		String url = URL + title1 + "&" + title2;
		return url;
	}

	private String decorateUrl(String title) throws Exception {
		title = URLEncoder.encode(title, "UTF-8");
		title = title.replaceAll(" ", "+");
		title = QUERY + title;
		return title;
	}

	private void httpDownload(String httpUrl, String filePath) {
		int byteread = 0;
		URL url = null;
		try {
			url = new URL(httpUrl);
		} catch (Exception e) {
		}
		try {
			File outputFile = new File(filePath);
			outputFile.getParentFile().mkdirs();
			URLConnection conn = url.openConnection();
			InputStream inStream = conn.getInputStream();
			FileOutputStream fs = new FileOutputStream(outputFile);
			byte[] buffer = new byte[BUFFER_SIZE];
			while ((byteread = inStream.read(buffer)) != -1) {
				fs.write(buffer, 0, byteread);
			}
			inStream.close();
			fs.close();
		} catch (Exception e) {
		}
	}

	public void calcAll(String folderPath) throws Exception {
		File dir = new File(folderPath);
		File[] files = dir.listFiles();
		for (File file : files) {
			calc(file);
		}
	}

	public double calc(File file, String start, String end) throws Exception {
		BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
		BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
		String line;
		boolean first = true;
		String title1 = null;
		String title2 = null;
		List<Integer> list1 = new ArrayList<Integer>();
		List<Integer> list2 = new ArrayList<Integer>();
		boolean begin = false;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			} else {
				if (first) {
					String[] lineSplits = line.split(", ");
					title1 = info(lineSplits[1]);
					title2 = info(lineSplits[2]);
					first = false;
				} else {
					String[] lineSplits = line.split(", ");
					int value1 = Integer.parseInt(lineSplits[1]);
					int value2 = Integer.parseInt(lineSplits[2]);
					Date startDate = sdf.parse(start);
					Date endDate = sdf.parse(end);
					Date date = sdf.parse(lineSplits[0].substring(1, 10));
					
					if ((value1 > 0 && value2 > 0) && (date.after(startDate) && date.before(endDate))) {
						begin = true;
					}
					if (begin) {
						list1.add(value1);
						list2.add(value2);
					}
				}
			}
		}
		reader.close();
		if (!begin) {
			System.out.println(title1 + "\t" + title2 + "\t" + 0);
			return 0;
		}
		double average1 = getAverage(list1);
		double average2 = getAverage(list2);
		double average = getAverage(list1, list2);
		double standardDeviation1 = getStandardDeviation(list1, average1);
		double standardDeviation2 = getStandardDeviation(list2, average2);
		double correlation = (average - average1 * average2) / (standardDeviation1 * standardDeviation2);
		System.out.println(title1 + "\t" + title2 + "\t" + correlation);
		return correlation;
	}

	public double calc(File file) throws Exception {
		BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
		BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
		String line;
		boolean first = true;
		String title1 = null;
		String title2 = null;
		List<Integer> list1 = new ArrayList<Integer>();
		List<Integer> list2 = new ArrayList<Integer>();
		boolean begin = false;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			} else {
				if (first) {
					String[] lineSplits = line.split(", ");
					title1 = info(lineSplits[1]);
					title2 = info(lineSplits[2]);
					first = false;
				} else {
					String[] lineSplits = line.split(", ");
					int value1 = Integer.parseInt(lineSplits[1]);
					int value2 = Integer.parseInt(lineSplits[2]);
					if (value1 > 0 && value2 > 0) {
						begin = true;
					}
					if (begin) {
						list1.add(value1);
						list2.add(value2);
					}
				}
			}
		}
		reader.close();
		if (!begin) {
			System.out.println(title1 + "\t" + title2 + "\t" + 0);
			return 0;
		}
		double average1 = getAverage(list1);
		double average2 = getAverage(list2);
		double average = getAverage(list1, list2);
		double standardDeviation1 = getStandardDeviation(list1, average1);
		double standardDeviation2 = getStandardDeviation(list2, average2);
		double correlation = (average - average1 * average2) / (standardDeviation1 * standardDeviation2);
		System.out.println(title1 + "\t" + title2 + "\t" + correlation);
		return correlation;
	}

	private String info(String info) {
		info = info.replaceAll("\"", "");
		return info;
	}

	private double getAverage(List<Integer> list) {
		long sum = 0;
		for (int value : list) {
			sum += value;
		}
		double average = sum / list.size();
		return average;
	}

	private double getStandardDeviation(List<Integer> list, double average) {
		long sum = 0;
		for (int value : list) {
			sum += Math.pow(value - average, 2);
		}
		double standardDeviation = Math.sqrt(sum / list.size());
		return standardDeviation;
	}

	private double getAverage(List<Integer> list1, List<Integer> list2) {
		long sum = 0;
		for (int i = 0; i < list1.size(); i++) {
			sum += list1.get(i) * list2.get(i);
		}
		double average = sum / list1.size();
		return average;
	}

	public double calc(String title1, String title2, String outputFolder) throws Exception {
		String outputPath = outputFolder + title1 + "_" + title2 + ".txt";
		httpDownload(url(title1, title2), outputPath);
		File file = new File(outputPath);
		return calc(file);
	}

	public static void main(String[] args) throws Exception {
		WikipediaTrendsComparer wikipediatrends = new WikipediaTrendsComparer();

		// String filePath1 = "D:" + File.separator + "input.txt";
		// String outputFolder = "D:" + File.separator + "data" +
		// File.separator;
		// wikipediatrends.read(filePath1, outputFolder);

		// String filePath2 = "D:" + File.separator + "data" + File.separator;
		// wikipediatrends.calcAll(filePath2);

		 String title1 = "Angelina Jolie";
		 String title2 = "2005 Kashmir earthquake";
		 String filePath3 = "res/WikiTrends" + File.separator + "test" + File.separator;
		 wikipediatrends.calc(title1, title2, filePath3);
	}

}
