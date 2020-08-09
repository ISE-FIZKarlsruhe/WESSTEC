package edu.kit.aifb.gwifi.yxu.textcategorization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
import weka.core.converters.TextDirectoryLoader;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.stemmers.Stemmer;
import weka.filters.Filter;
import weka.filters.MultiFilter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.unsupervised.attribute.StringToWordVector;

public class TopicCategoryWekaClassifier {
	
	private String stwAttrRange = "first"; //first-3,5,6-last
	private String outputStopwordsFile = "res/stopwords.txt";
	
	private Instances instances;
	private Instances filteredInstances;
	
	private Filter filters;
	private Classifier classifier;
	private FilteredClassifier filteredClassifier;
	
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
		instances = data;
	}
	
	public void genArffOfTextDir(String textDir,String arff) throws IOException{
		TextDirectoryLoader loader = new TextDirectoryLoader();
		loader.setDirectory(new File(textDir));
		Instances dataRaw = loader.getDataSet();
		FileWriter fw = new FileWriter(arff);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(dataRaw.toString());
		bw.close();
		fw.close();
	}
	
	public void loadTrainingData(String trainingDataFile) throws Exception{
        DataSource frData = new DataSource(trainingDataFile);  
        instances = frData.getDataSet();  
        instances.setClassIndex(1);
//        for(int i = 0; i<m_instances.classAttribute().numValues();i++){
//        	System.out.print(m_instances.classAttribute().value(i)+"\n");
//        }
	}

	public Filter setupFilters(
			String attrRange, boolean tf, boolean idf, Stemmer stemmer, 
			String inputStopwordsFile, int wordsToKeep, int rankNum){
		// setup filter string to word vector by stop words and stemmer
    	StringToWordVector stw = setupSTWFilter(attrRange, tf, idf, stemmer, inputStopwordsFile, wordsToKeep);
    	if(stw == null) return null;
    	Instances stwFiltered = useFilter(instances, stw);
    	if(stwFiltered == null) return null;
    	// another filter attribute selection
        AttributeSelection as = setupASFilter(stwFiltered, rankNum);
		if(as == null) return null;
		stwFiltered = useFilter(stwFiltered, as);
    	if(stwFiltered == null) return null;
        Filter[] filterArray = new Filter[2];
        filterArray[0] = stw;
        filterArray[1] = as;
        MultiFilter multifilter = new MultiFilter();
        multifilter.setFilters(filterArray);
        filters = multifilter;
        filteredInstances = stwFiltered;
        return multifilter;
	}
	
	public Instances useFilter(Instances origin, Filter filter){
    	Instances filtered;
    	try {
    		filtered = Filter.useFilter(origin, filter);
		} catch (Exception e) {
			System.out.println("The filter can't be used on the instances successfully! "+filter.toString());
			return null;
		}
    	return filtered;
	}
	
	//stemmer = new WekaPorterStemmer()
	//stopwords = new Stopwords() // rainbow by default
	//wordsToKeep = 2000
	public StringToWordVector setupSTWFilter(
			String attrRange, boolean tf, boolean idf, Stemmer stemmer, 
			String inputStopwordsFile, int wordsToKeep){
    	StringToWordVector stw = new StringToWordVector();
    	try {
			stw.setInputFormat(instances);
		} catch (Exception e) {
			System.out.println("The input format of stw can't be set successfully!");
			return null;
		}
    	stw.setTFTransform(tf);
    	stw.setIDFTransform(idf);
		stw.setStemmer(stemmer);
		if(WekaStopwords.genOutputStopwordsFile(inputStopwordsFile, outputStopwordsFile))
			stw.setStopwords(new File(outputStopwordsFile));
    	stw.setWordsToKeep(wordsToKeep);
    	try {
    		stw.setAttributeIndices(stwAttrRange);
    	} catch (IllegalArgumentException e){
    		stw.setAttributeIndices(attrRange);
    		return stw;
    	}
		return stw;
	}
	
	public AttributeSelection setupASFilter(Instances stwInstances, int rankNum) {
		AttributeSelection as = new AttributeSelection();
		try {
			as.setInputFormat(instances);
		} catch (Exception e) {
			System.out.println("The input format of as can't be set successfully!");
			return null;
		}
        InfoGainAttributeEval infoGainEval = new InfoGainAttributeEval();
		as.setEvaluator(infoGainEval);
		Ranker ranker = new Ranker();
		int numOfAttribute = stwInstances.numAttributes();
		if(rankNum<0){
			ranker.setNumToSelect(-1);
		}else{
			ranker.setNumToSelect(numOfAttribute/rankNum);
		}
		as.setSearch(ranker);
		return as;
	}
	
	public Classifier setupClassifier() {
    	NaiveBayes model=new NaiveBayes();
    	
//    	SMO smo = new SMO();
//    	smo.setKernel(new RBFKernel());
//    	
//    	CVParameterSelection cp = new CVParameterSelection();
//    	cp.setClassifier(smo);
//        try {
//			cp.setNumFolds(5);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}  // using 5-fold CV
//        try {
//			cp.addCVParameter("C 2 8 4");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
    	classifier = model;
        return model;
	}
	
	public FilteredClassifier setupFilteredClassifier(Filter multifilter, Classifier model, Instances m_instances) {
        FilteredClassifier fc = new FilteredClassifier();
    	fc.setFilter(multifilter);
    	fc.setClassifier(model);
    	filteredClassifier = fc;
//    	fc.buildClassifier(m_instances);
    	return fc;
	}
	
	public static void saveClassifierAsModel(Classifier classifier, String modelFile){
        ObjectOutputStream out;
//		try {
//			out = new ObjectOutputStream(new FileOutputStream(modelFile));
//	        out.writeObject(classifier);
//	        out.close();
// 			System.out.println("===== Saved model: " + fileName + " =====");
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//		} catch (IOException e) {
//			System.out.println("Problem found when writing: " + fileName);
//		}
        
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
		TopicCategoryWekaClassifier classifer = new TopicCategoryWekaClassifier();
		// build training data 
		
		// load training data .arff and set class
//        classifer.loadTrainingData("");
//        
//        Filter filters = classifer.setupFilters("first", true, true, null, "", 5000, 10);
//        
//        // setup classifier
//    	Classifier model=setupClassifier();
//    	
//		// setup filtered classifier with filter and classifier
//        FilteredClassifier fc = setupFilteredClassifier(multifilter, model, m_instances);
//    	
//    	// save classifier as model
//        saveClassifierAsModel(fc, "");
//        
//
//    	fc = new FilteredClassifier();
//    	fc.setFilter(multifilter);
//    	fc.setClassifier(new SMO());
//    	fc.buildClassifier(m_instances);
//    	Evaluation eval = new Evaluation(m_instances);
//    	eval.crossValidateModel(fc,m_instances,fold,new Random(1));//fold (N folds)
//    	System.out.println( eval.toSummaryString() );  
//        System.out.println(eval.toClassDetailsString());
//		// train the training data to get classifier model
//    	Evaluation eval = new Evaluation(m_instances);
//    	eval.crossValidateModel(fc,m_instances,1,new Random(1));
//    	eval.toSummaryString();
//    	eval.toClassDetailsString();
//    	
//		// evaluate the testing data with classifier model
//    	// load testing data
//		BufferedReader reader = new BufferedReader(new FileReader(""));
//		String line;
//		String text = "";
//		while ((line = reader.readLine()) != null) {
//			text = text + " " + line;
//		}
//		reader.close();
//    	PorterStemmer stemmer = new PorterStemmer();
//		String test = stemmer.processText(text); // words stemming
//	    test = test.replaceAll("\\P{InBasic_Latin}|\\d", "");// delete non-english character
//	    test = Stopword.removeStopwords(test); 
//	    
//	    // build testing data
//	    
//	    // load classifier model
//		ObjectInputStream in = new ObjectInputStream(new FileInputStream(""));
//		Object tmp = in.readObject();
//		FilteredClassifier classifier = (FilteredClassifier) tmp;
//		in.close();
//		
//		// evaluate testing data
//		Map<String,Double> cate_prop = new HashMap<String,Double>();
//		double[] pred = classifier.distributionForInstance(m_instances.instance(0));
//		for(int i=0; i<pred.length;i++){
//			cate_prop.put(m_instances.classAttribute().value((int)i), pred[i]);
//		}
		

//		WekaOperation w = new WekaOperation();
//		w.wekaArffGenerate(args[0], args[1]);
//		w.getFileInstances(args[1]);
//		w.featureSelect(Integer.parseInt(args[2]));
//		w.classify(Integer.parseInt(args[3]));
//		w.saveModel(args[4]);

	}

}
