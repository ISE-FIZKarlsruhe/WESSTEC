package edu.kit.aifb.gwifi.spearman;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.util.pageview.WikipediaTrendsComparer;

public class WikiTrendsEvaluation {

	private Wikipedia wikipedia;
	private static WikipediaTrendsComparer comparer;
//	private static final String ConfigPath = "/home/ls3data/users/lzh/zj/configs/wikipedia-template-en.xml";
	private static String ConfigPath = "configs/wikipedia-template-en.xml";
	private static String WikiTrendsPath = "res/WikiTrends/";

	public static void main(String[] args) throws Exception {

		WikiTrendsEvaluation se = new WikiTrendsEvaluation(ConfigPath);

		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		String tmp, seed = "";
		Article seedArticle = null;
		List<String> entityNames = new ArrayList<String>();
		List<Article> article = new ArrayList<Article>();
		int count = 0;
		List<Double> SpearmanAvr = new ArrayList<Double>();
		while ((tmp = reader.readLine()) != null) {
			if (!tmp.startsWith("\t")) {
				count++;
				if (count > 1) {
					Map<String, Double> simMap = new HashMap<String, Double>();
					Map<String, Integer> groundTruth = new HashMap<String, Integer>();
					for (int i = 0; i < entityNames.size(); i++) {
						String str = entityNames.get(i);
						groundTruth.put(str, (entityNames.size() - i));
						System.out.println("calculate similarity between:\t" + seed + "\t" + str);
						Article targetArticle = article.get(i);
						double sim = 0.0;
						if (targetArticle != null) {
//							sim = comparer.calc(seedArticle.getTitle(), targetArticle.getTitle(), WikiTrendsPath);
							
							String outputPath = WikiTrendsPath + seedArticle.getTitle() + "_" + targetArticle.getTitle() + ".txt";
							File file = new File(outputPath);
							sim = comparer.calc(file, "2014/01/01", "2015/01/01");
						}	
						simMap.put(str, sim);
					}
					List<Map.Entry<String, Double>> simlst = se.sortRank(simMap);
					double spearman = se.computeSpearmanSim(groundTruth, simlst);
					SpearmanAvr.add(spearman);
					se.write(args[1], seed, simlst, spearman);
					System.out.println("seed " + seed + " finished!");
				}
				System.out.println("seed " + count + " start!");
				seed = tmp;
				seedArticle = se.getArticle(seed);
				entityNames.clear();
				article.clear();
			} else {
				entityNames.add(tmp.substring(1));
				article.add(se.getArticle(tmp.substring(1)));
			}
		}
		Map<String, Double> simMap = new HashMap<String, Double>();
		Map<String, Integer> groundTruth = new HashMap<String, Integer>();
		for (int i = 0; i < entityNames.size(); i++) {
			String str = entityNames.get(i);
			groundTruth.put(str, (entityNames.size() - i));
			System.out.println("calculate similarity between:\t" + seed + "\t" + str);
			Article targetArticle = article.get(i);
			double sim = 0.0;
			if (targetArticle != null) {
//				sim = comparer.calc(seedArticle.getTitle(), targetArticle.getTitle(), WikiTrendsPath);
				
				String outputPath = WikiTrendsPath + seedArticle.getTitle() + "_" + targetArticle.getTitle() + ".txt";
				File file = new File(outputPath);
				sim = comparer.calc(file, "2014/01/01", "2015/01/01");
			}	
			simMap.put(str, sim);
		}
		List<Map.Entry<String, Double>> simlst = se.sortRank(simMap);
		double spearman = se.computeSpearmanSim(groundTruth, simlst);
		SpearmanAvr.add(spearman);
		se.write(args[1], seed, simlst, spearman);
		System.out.println("seed " + seed + " finished!");
		reader.close();

		double total = 0;
		for (double i : SpearmanAvr)
			total += i;
		se.write(args[1], (total / SpearmanAvr.size()));
	}

	public WikiTrendsEvaluation(String config) throws Exception {
		File databaseDirectory = new File(config);
		wikipedia = new Wikipedia(databaseDirectory, false);
		comparer = new WikipediaTrendsComparer();
	}

	private Article getArticle(String name) {
		Article article = wikipedia.getArticleByTitle(name);
		if (article == null)
			System.out.println(name + "\tis not found correspondent article!");
		return article;
	}

	private List<Map.Entry<String, Double>> sortRank(Map<String, Double> map) {
		List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>(map.entrySet());

		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
			public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
				return (o2.getValue().compareTo(o1.getValue()));
			}
		});
		return list;
	}

	private double computeSpearmanSim(Map<String, Integer> groundTruth, List<Map.Entry<String, Double>> compareTo) {
		int length = compareTo.size();
		if (groundTruth.size() != length)
			return 0.0;

		double d = 0;
		for (int i = 0; i < compareTo.size(); i++) {
			Map.Entry<String, Double> taget = compareTo.get(i);
			String mention = taget.getKey();
			int src = groundTruth.get(mention);
			int tag = length - i;
			d += Math.pow((src - tag), 2);
		}
		double spearmanSim = 1 - ((6 * d) / (length * (Math.pow(length, 2) - 1)));
		return spearmanSim;
	}

	private void write(String path, String seed, List<Map.Entry<String, Double>> list, double score) throws Exception {
		File file = new File(path);
		if (!file.exists())
			file.createNewFile();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, true), "UTF-8"));
		String out = seed + "\n";
		for (Map.Entry<String, Double> m : list) {
			out += "\t" + m.getKey() + "\t\t" + m.getValue() + "\n";
		}
		out += "----------------------------------------------------------------------\nSpearman Similarity with groundtruth is "
				+ score + "\n\n";
		writer.write(out);
		writer.close();
	}

	private void write(String path, double avgscore) throws Exception {
		File file = new File(path);
		if (!file.exists())
			file.createNewFile();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, true), "UTF-8"));
		String out = "\n----------------------------------------------------------------------\naverage spearman correlation is "
				+ avgscore + "\n\n";
		writer.write(out);
		writer.close();
	}
}
