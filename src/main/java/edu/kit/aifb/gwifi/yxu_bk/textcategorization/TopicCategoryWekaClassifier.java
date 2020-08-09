package edu.kit.aifb.gwifi.yxu_bk.textcategorization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import edu.kit.aifb.gwifi.textcategorization.Stopword;
import edu.kit.aifb.gwifi.util.text.PorterStemmer;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.classifiers.meta.CVParameterSelection;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Stopwords;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.MultiFilter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.unsupervised.attribute.StringToWordVector;

public class TopicCategoryWekaClassifier {
	
	public TopicCategoryWekaClassifier(){
		//do nothing
	}
	
	public Instances buildData(){
		Instances data;
		return null;
	}
	
	public FastVector buildAttrs(){
		return null;
	}
	
	public void fillInstances(Instances data){
		
	}
	
	public static Classifier trainData(Instances trainingData) throws Exception{
		Classifier cModel = (Classifier)new NaiveBayes();
		cModel.buildClassifier(trainingData);
		return cModel;
	}
	
	public static Evaluation testData(Instances testingData, Instances trainingData, Classifier cModel) throws Exception{
		Evaluation eTest = new Evaluation(trainingData);
		eTest.evaluateModel(cModel, testingData);
		return eTest;
	}
	
	public String getSummaryOfEva(Evaluation eva){
		return eva.toSummaryString();
	}
	
	public double[][] getConfusionMatrixOfEva(Evaluation eva){
		return eva.confusionMatrix();
	}
	
	public double[] getFDistributionForOneData(Classifier cModel, Instance oneData) throws Exception{
		return cModel.distributionForInstance(oneData);
	}

	public static void main(String[] args) throws Exception {
		// build training data 
		// load training data .arff and set class
        DataSource frData = new DataSource("");  
        Instances m_instances = frData.getDataSet();  
        m_instances.setClassIndex(1);
        
		// setup filter string to word vector by stop words and stemmer
    	StringToWordVector stw = new StringToWordVector();
    	stw.setInputFormat(m_instances);
    	stw.setAttributeIndices("");
    	stw.setTFTransform(true);
    	stw.setIDFTransform(true);
		stw.setStemmer(new WekaPorterStemmer());
		Stopwords sw = new Stopwords();
		sw.read("");
    	stw.setStopwords(new File(""));
    	stw.setWordsToKeep(2000);
    	// use filter
    	Filter.useFilter(m_instances, stw);
    	// another filter attribute selection
        AttributeSelection as = new AttributeSelection();
		as.setInputFormat(m_instances);
        InfoGainAttributeEval infoGainEval = new InfoGainAttributeEval();
		as.setEvaluator(infoGainEval);
		Ranker ranker = new Ranker();
//		ranker.setNumToSelect(numOfAttribute/numberToSelect);
		as.setSearch(ranker);
		
        Filter[] filters = new Filter[2];
        filters[0] = stw;
        filters[1] = as;
        MultiFilter multifilter = new MultiFilter();
        multifilter.setFilters(filters);
        
        // setup classifier
    	NaiveBayes model=new NaiveBayes();
    	
    	SMO smo = new SMO();
    	smo.setKernel(new RBFKernel());
    	CVParameterSelection cp = new CVParameterSelection();
    	cp.setClassifier(smo);
        cp.setNumFolds(5);  // using 5-fold CV
        cp.addCVParameter("C 2 8 4");
    	
		// setup filtered classifier with filter and classifier
        FilteredClassifier fc = new FilteredClassifier();
    	fc.setFilter(multifilter);
    	fc.setClassifier(model);
    	fc.buildClassifier(m_instances);
    	
    	fc.setFilter(multifilter);
    	fc.setClassifier(new SMO());
    	fc.buildClassifier(m_instances);
    	
    	// save classifier as model
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(""));
        out.writeObject(fc);
        out.close();
		// train the training data to get classifier model
    	Evaluation eval = new Evaluation(m_instances);
    	eval.crossValidateModel(fc,m_instances,1,new Random(1));
    	eval.toSummaryString();
    	eval.toClassDetailsString();
    	
		// evaluate the testing data with classifier model
    	// load testing data
		BufferedReader reader = new BufferedReader(new FileReader(""));
		String line;
		String text = "";
		while ((line = reader.readLine()) != null) {
			text = text + " " + line;
		}
		reader.close();
    	PorterStemmer stemmer = new PorterStemmer();
		String test = stemmer.processText(text); // words stemming
	    test = test.replaceAll("\\P{InBasic_Latin}|\\d", "");// delete non-english character
	    test = Stopword.removeStopwords(test); 
	    
	    // build testing data
	    
	    // load classifier model
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(""));
		Object tmp = in.readObject();
		FilteredClassifier classifier = (FilteredClassifier) tmp;
		in.close();
		
		// evaluate testing data
		Map<String,Double> cate_prop = new HashMap<String,Double>();
		double[] pred = classifier.distributionForInstance(m_instances.instance(0));
		for(int i=0; i<pred.length;i++){
			cate_prop.put(m_instances.classAttribute().value((int)i), pred[i]);
		}

	}

}
