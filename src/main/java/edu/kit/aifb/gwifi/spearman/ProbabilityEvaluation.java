package edu.kit.aifb.gwifi.spearman;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Wikipedia;

public class ProbabilityEvaluation {
	
	private Wikipedia wikipedia;
	private BufferedReader resFreqReader;
	private HashMap<String, Integer> res2freq;
	Map<String, Integer> groundTruth = new HashMap<String, Integer>();
//	private static final String ConfigPath = "/home/ls3data/users/lzh/zj/configs/wikipedia-template-en.xml";
//	private static final String FreqPath = "/home/ls3data/users/lzh/zj/statistics/ResourceLabelCoOccurrence-3/resFreqWithLabel_en.txt";
	private static final String ConfigPath = "configs/wikipedia-template-en.xml";
	private static final String FreqPath = "/Users/leizhang/Data/aida/KORE Datasets/KORE_entity_relatedness/statistics/ResourceLabelCoOccurrence-3/resFreqWithLabel_en.txt";

	public static void main(String[] args) throws Exception {

		ProbabilityEvaluation pe = new ProbabilityEvaluation(ConfigPath, FreqPath);
		
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		String tmp, seed = "";
		List<String> entityNames = new ArrayList<String>();
		int count = 0;
		List<Double> SpearmanAvr = new ArrayList<Double>();
		while ((tmp = reader.readLine()) != null) {
			if (!tmp.startsWith("\t")) {
				count++;
				if (count > 1){
					Map<String, Double> simMap = pe.getEntityEntityProbility(seed, entityNames);
					List<Map.Entry<String, Double>> simlst = pe.sortRank(simMap);
					double spearman = pe.computeSpearmanSim(pe.groundTruth, simlst);
					SpearmanAvr.add(spearman);
					pe.write(args[1], seed, simlst, spearman);
					System.out.println("seed " + seed + " finished!");
				}
				System.out.println("seed " + count + " start!");
				seed = tmp;
				entityNames.clear();
				pe.groundTruth.clear();
			} else {
				entityNames.add(tmp.substring(1));
			}
		}
		Map<String, Double> simMap = pe.getEntityEntityProbility(seed, entityNames);
		List<Map.Entry<String, Double>> simlst = pe.sortRank(simMap);
		double spearman = pe.computeSpearmanSim(pe.groundTruth, simlst);
		SpearmanAvr.add(spearman);
		pe.write(args[1], seed, simlst, spearman);
		System.out.println("seed " + seed + " finished!");
		reader.close();
		
		double total = 0;
		for(double i : SpearmanAvr)
			total += i;
		System.out.println("average spearman correlation is " + (total/SpearmanAvr.size()));
    }
	
	public ProbabilityEvaluation (String config, String freqPath) throws Exception{
		File databaseDirectory = new File(config);
		wikipedia = new Wikipedia(databaseDirectory, false);
		System.out.println("The Wikipedia environment has been initialized.");
		
		resFreqReader = new BufferedReader(new InputStreamReader(new FileInputStream(freqPath), "UTF-8"));
		res2freq = new HashMap<String, Integer>();
		load();
	}
	
	private Article getArticle(String name){
		Article article = wikipedia.getArticleByTitle(name);
		if(article == null)
			System.out.println(name + "\tis not found correspondent article!");
		return article;
	}
	
	private Map<Integer, Integer> loadCoOccurrenceFrequency(Article s_article){
		
		if (s_article == null) {
			System.out.println("Could not find exact match of seed.");
			return null;
		} else {
			Map<Integer, Integer> articleCo2Map = new HashMap<Integer, Integer>();
			for (Article a : s_article.getLinksIn()) {
				for (int i : a.getSentenceIndexesMentioning(s_article)) {
					int pre = i - Environment.NUM_SORROUNDING_SENTENCES_ON_ONE_SIDE < 0 ? 0 : i
							- Environment.NUM_SORROUNDING_SENTENCES_ON_ONE_SIDE;
					int sub = i + Environment.NUM_SORROUNDING_SENTENCES_ON_ONE_SIDE;
					for (; pre <= sub; pre++) {
						String sentence = a.getSentenceMarkup(i);

						Matcher m = Pattern.compile("((?<=\\u005B\\u005B).*?(?=\\u005D\\u005D))").matcher(sentence);
						while (m.find()) {
							String t_title = m.group();

							if (t_title.contains("|"))
								t_title = t_title.substring(0, t_title.indexOf("|"));

							Article t_article = wikipedia.getArticleByTitle(t_title);

							if (t_article == null || t_article.equals(""))
								continue;

							int t_id = t_article.getId();

							if (articleCo2Map.containsKey(t_id)) {
								int frequency = articleCo2Map.get(t_id);
								frequency++;
								articleCo2Map.put(t_id, frequency);
							} else
								articleCo2Map.put(t_id, 1);
						}
					}
				}
			}
			return articleCo2Map;
		}
	}
	
	private void load() throws IOException {
		String lineResFreq;
		while ((lineResFreq = resFreqReader.readLine()) != null) {
			String[] parts = lineResFreq.split("\t\t");
			if (parts.length == 2) {
				res2freq.put(parts[0], Integer.parseInt(parts[1]));
			} else {
				System.out.println("Parse error: " + lineResFreq);
			}
		}
		resFreqReader.close();
	}
	
	private int getSingleOccurrenceFrequency(Article article){
		String title = article.getTitle();
		
		if (title == null || title.equals("") || title.length() >= Environment.INDEX_LENGTH_THRESHOLD)
			return 1;
		if(!res2freq.containsKey(title))
			return 1;
		int frequency = res2freq.get(title);
		if (frequency == 0)
			frequency = 1;
		return frequency;
	}
	
	private Map<String, Double> getEntityEntityProbility(String seed, List<String> entityNames){
		Map<String, Double> simMap = new HashMap<String, Double>();
		
		Article seedArticle = getArticle(seed);
		System.out.println("load cooccurrence frequency:\t" + seed);
		Map<Integer, Integer> cooccurrenceMap = loadCoOccurrenceFrequency(seedArticle);
		
		for(int i=0;i<entityNames.size();i++){
			String t_title = entityNames.get(i);
			groundTruth.put(t_title, (entityNames.size()-i));
			double sim = 0.0;
			if(cooccurrenceMap == null)
				simMap.put(t_title,sim);
			else{
				System.out.println("calculate probility between:\t" + seed + "\t" + t_title);
				Article targetArticle = getArticle(entityNames.get(i));
				if(targetArticle == null){
					simMap.put(t_title, sim);
					continue;
				}
				int t_id = targetArticle.getId();
				if(!cooccurrenceMap.containsKey(t_id)){
					System.out.println(t_title + "\tcan not find in CoOccurrenceMap!");
					simMap.put(t_title, sim);
					continue;
				}
				int cofrequency = cooccurrenceMap.get(t_id);
				int s_frequency = getSingleOccurrenceFrequency(seedArticle);
				//System.out.println(t_title + "\tconfrequency:\t" + cofrequency + "\ts_frequency:\t" + s_frequency);
				sim = ((double)cofrequency)/s_frequency;
				//System.out.println("probility of " + t_title + " is " + sim);
				simMap.put(t_title, sim);
			}
		}
		return simMap;
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

	private double computeSpearmanSim(Map<String,Integer> groundTruth, List<Map.Entry<String, Double>> compareTo){
		int length = compareTo.size();
		if(groundTruth.size() != length)
			return 0.0;
		
		double d=0;
		for(int i=0;i<compareTo.size();i++){
			Map.Entry<String, Double> taget = compareTo.get(i);
			String mention = taget.getKey();
			int src = groundTruth.get(mention);
			int tag = length - i;
			d += Math.pow((src-tag),2);
		}
		double spearmanSim = 1-((6*d)/(length*(Math.pow(length,2)-1)));
		return spearmanSim;
	}

	private void write(String path, String seed, List<Map.Entry<String, Double>> list, double score) throws Exception{
		File file = new File(path);
		if(!file.exists())
			file.createNewFile();
		BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
		String out = seed + "\n";
		for (Map.Entry<String, Double> m : list) {
			out += "\t" + m.getKey() + "\t\t" + m.getValue() + "\n";
		}
		out += "----------------------------------------------------------------------\nSpearman Similarity with groundtruth is "+ score + "\n\n";
		writer.write(out);
		writer.close();
	}
}
