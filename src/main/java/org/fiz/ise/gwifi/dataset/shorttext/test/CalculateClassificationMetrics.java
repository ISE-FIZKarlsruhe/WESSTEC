package org.fiz.ise.gwifi.dataset.shorttext.test;

import java.util.Map;
import java.util.Map.Entry;

import edu.kit.aifb.gwifi.model.Category;

public class CalculateClassificationMetrics {
	public void evaluateResults(Map<Category, Integer> truePositive, Map<Category, Integer> falsePositive,Map<Category, Integer> numberOfElements) {
		for (Entry<Category, Integer>entry:truePositive.entrySet()) {
			double precision=precision(entry.getValue(),falsePositive.get(entry.getKey()));
			double recall=recall(entry.getValue(),numberOfElements.get(entry.getKey()));
			
			System.out.println(entry.getKey().getTitle()+"\tprecision\t"+precision);
			System.out.println(entry.getKey().getTitle()+"\trecall\t"+recall);
		}
		
		System.out.println("\nMicro F-measure "+microFmeasure(truePositive, falsePositive, numberOfElements));
		System.out.println("Macro F-measure "+macroFmeasure(truePositive, falsePositive, numberOfElements));
	}
	private double microFmeasure(Map<Category, Integer> truePositive, Map<Category, Integer> falsePositive,Map<Category, Integer> numberOfElements) {
		double total_TP=0.0;
		double total_Predicted=0.0;
		double total_TPFN=0.0;
		for (Entry<Category, Integer>entry:truePositive.entrySet()) {
			total_TP+=entry.getValue();
			total_Predicted+=(entry.getValue()+falsePositive.get(entry.getKey()));
			total_TPFN+=(numberOfElements.get(entry.getKey()));
		}
		double microPrecision=total_TP/total_Predicted;
		double microRecall=total_TP/total_TPFN;
		//System.out.println("micro precision: "+microPrecision);
		//System.out.println("micro recall: "+microRecall);
		return calculateFmeasure(microPrecision,microRecall);
	}
	private double macroFmeasure(Map<Category, Integer> truePositive, Map<Category, Integer> falsePositive,Map<Category, Integer> numberOfElements) {
		double total_Precision=0.0;
		double total_Recall=0.0;
		for (Entry<Category, Integer>entry:truePositive.entrySet()) {
			total_Precision+=precision(entry.getValue(),falsePositive.get(entry.getKey()));
			total_Recall+=recall(entry.getValue(),numberOfElements.get(entry.getKey()));
		}
		double macroPrecision=total_Precision/truePositive.size();
		double macroRecall=total_Recall/truePositive.size();
		System.out.println("macro precision: "+macroPrecision);
		System.out.println("macro recall: "+macroRecall);
		return calculateFmeasure(macroPrecision,macroRecall);
	}
	
	private double precision(double TP,double FP) {
		return TP/(TP+FP);
	}
	private double recall(double TP,double Total_GT) {
		return TP/Total_GT;
	}
	private double calculateFmeasure(double precison, double recall) {
		return (2*precison*recall)/(precison+recall);
	}
}
