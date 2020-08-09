package edu.kit.aifb.gwifi.util;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import weka.core.FastVector;
import edu.kit.aifb.gwifi.textcategorization.MyFilteredClassifier;

public class R128STextCategorizer implements TextCategorizer {
	private List<String> categories;
	private FastVector fvNominalVal;
	//private static String text2Classify;
	private MyFilteredClassifier mf = new MyFilteredClassifier();
	private static final String arffFile = "/home/ls3data/users/lzh/congliu/ReutersDataset_8.arff";
	private static final String modelFile = "/home/ls3data/users/lzh/congliu/ReutersDataset_8.model";
	
	public R128STextCategorizer(){
		classifierInitialization();
	}
	
	public void classifierInitialization() {
		try {
			categories = MyFilteredClassifier.getClassName(arffFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		fvNominalVal = new FastVector(2);
		for (String s : categories) {
			fvNominalVal.addElement(s);
		}
		mf.loadModel(modelFile);
	}
	


	//public static String getText2Classify() {
		//return text2Classify;
	//}

	//public static void setText2Classify(String text2Classify) {
		//R128STextCategorizer.text2Classify = text2Classify;
	//}
	/**
	 * get the classification probability of a given text to all the category we choose for example rd layer category
	 * in wiki.
	 * 
	 * @param <ValueComparator>
	 * @return key: category name value: the classification probability the a text is classified to this category
	 * @throws IOException
	 */
	@Override
	public Map<String, Double> getCategoryWithProbability(String text) {
		//text2Classify = GraphBasedTopicWeighter.getText2Classify();
		int n=1;
		mf.setText(text);
		mf.setCateName(categories);
		mf.makeInstance(fvNominalVal);
		mf.classify();
		Map<String, Double> catepro = mf.getCate_prop();
		List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(catepro.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
			public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});
		int i = 1;
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for (Iterator<Map.Entry<String, Double>> it = list.iterator(); it.hasNext();) {
			Map.Entry<String, Double> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
			i++;
			if (i > n) {
				break;
			}
		}
		return sortedMap;
	}
}
