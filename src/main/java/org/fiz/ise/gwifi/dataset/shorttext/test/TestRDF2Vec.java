package org.fiz.ise.gwifi.dataset.shorttext.test;

import java.util.ArrayList;
import java.util.List;

import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.dataset.ReadDataset;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;

import edu.kit.aifb.gwifi.annotation.Annotation;

public class TestRDF2Vec {

	public static void main(String[] args) {
		System.out.println("TestRDF2Vec:Started..");
		
		System.err.println(LINE_modelSingleton.getInstance().lineModel.getVocab().words().size());
		
		
		
		
		List<String> dataset = new ArrayList<>(ReadDataset.read_AG_BasedOnType(Config.getString("DATASET_TEST_AG",""),AG_DataType.TITLEANDDESCRIPTION));
		List<Annotation> lstAllAnnotation = new ArrayList<>(AnnonatationUtil.findAnnotationAll(dataset));
		int totalExist =0;
		System.out.println("TestRDF2Vec:Size of list: "+lstAllAnnotation.size());
		for(Annotation a: lstAllAnnotation) {
//			if (LINE_modelSingleton.getInstance().lineModel.hasWord(HeuristicApproachForRDF2Vec.convertURIToRDF2VecBased(a.getURL(),true))) {
			if (LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(a.getId()))) {
				totalExist++;
			}
			else {
//				System.out.println(HeuristicApproachForRDF2Vec.convertURIToRDF2VecBased(a.getURL(),true));
				//System.out.println(String.valueOf(a.getId()));
			}
		}
		
		System.out.println("TestRDF2Vec:Total entity exist: "+totalExist);

	}
	
		
	
}
