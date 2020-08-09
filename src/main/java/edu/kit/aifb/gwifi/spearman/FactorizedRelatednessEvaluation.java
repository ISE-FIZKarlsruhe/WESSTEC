package edu.kit.aifb.gwifi.spearman;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.kit.aifb.gwifi.factorization.FactorizedRelatedness_new;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Wikipedia;

//java -cp spearman.jar edu.kit.aifb.gwifi.spearman.FactorizedRelatednessEvaluation /home/ls3data/users/lzh/zj/spearman/input.txt count
//java -cp spearman.jar edu.kit.aifb.gwifi.spearman.FactorizedRelatednessEvaluation /slow/users/lzh/eval_facterization/spearman/input.txt 20

public class FactorizedRelatednessEvaluation {
	
	private Wikipedia wikipedia;
	private static final String ConfigPath = "/home/ls3data/users/lzh/zj/configs/wikipedia-template-en.xml";
	
	public static void main(String[] args) throws Exception {
		
		FactorizedRelatedness_new fr = new FactorizedRelatedness_new(args[1]);
		FactorizedRelatednessEvaluation se = new FactorizedRelatednessEvaluation(ConfigPath);
		
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		String tmp, seed = "";
		int seedID = -1;
		List<String> entityNames = new ArrayList<String>();
		List<Integer> entityID = new ArrayList<Integer>();
		int count = 0;
		List<Double> SpearmanAvr = new ArrayList<Double>();
		while ((tmp = reader.readLine()) != null) {
			if (!tmp.startsWith("\t")) {
				count++;
				if (count > 1){
					Map<String, Double> simMap = new HashMap<String, Double>();
					Map<String, Integer> groundTruth = new HashMap<String, Integer>();
					for(int i=0;i<entityNames.size();i++){
						String str = entityNames.get(i);
						groundTruth.put(str, (entityNames.size()-i));
						System.out.println("calculate similarity between:\t" + seed + "\t" + str);
						int id = entityID.get(i);
						double sim = 0.0;
						if(id != -1)
							sim = fr.getEntityArticleRelatednessLucene(seedID, id);
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
				seedID = se.getPageId(seed);
				entityNames.clear();
				entityID.clear();
			} else {
				entityNames.add(tmp.substring(1));
				entityID.add(se.getPageId(tmp.substring(1)));
			}
		}
		Map<String, Double> simMap = new HashMap<String, Double>();
		Map<String, Integer> groundTruth = new HashMap<String, Integer>();
		for(int i=0;i<entityNames.size();i++){
			String str = entityNames.get(i);
			groundTruth.put(str, (entityNames.size()-i));
			System.out.println("calculate similarity between:\t" + seed + "\t" + str);
			int id = entityID.get(i);
			double sim = 0.0;
			if(id != -1)
				sim = fr.getEntityArticleRelatednessLucene(seedID, id);
			simMap.put(str, sim);
		}
		List<Map.Entry<String, Double>> simlst = se.sortRank(simMap);
		double spearman = se.computeSpearmanSim(groundTruth, simlst);
		SpearmanAvr.add(spearman);
		se.write(args[1], seed, simlst, spearman);
		System.out.println("seed " + seed + " finished!");
		reader.close();
		
		double total = 0;
		for(double i : SpearmanAvr)
			total += i;
		se.write(args[1], (total/SpearmanAvr.size()));
    }
	
	public FactorizedRelatednessEvaluation (String config) throws Exception{
		File databaseDirectory = new File(config);
		wikipedia = new Wikipedia(databaseDirectory, false);
	}
	
	private int getPageId(String name){
		Article article = wikipedia.getArticleByTitle(name);
		int id = -1;
		if(article != null)
			id = article.getId();
		else
			System.out.println(name + "\tis not found correspondent article!");
		return id;
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
	
	private void write(String path, double avgscore) throws Exception{
		File file = new File(path);
		if(!file.exists())
			file.createNewFile();
		BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
		String out = "\n----------------------------------------------------------------------\naverage spearman correlation is " + avgscore + "\n\n";
		writer.write(out);
		writer.close();
	}
}
