package edu.kit.aifb.gwifi.textcategorization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Random;

import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.converters.TextDirectoryLoader;
import weka.core.stemmers.NullStemmer;
import weka.filters.Filter;
import weka.filters.MultiFilter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.unsupervised.attribute.StringToWordVector;
/**
 * Train a classifier
 * @author aifb-ls3
 *
 */

public class WekaOperation {
	private Instances m_instances = null;
	private MultiFilter multifilter;
	private int numOfAttribute;
	private FilteredClassifier fc;
	
	
	/**
	 * convert the directory into a weka arrf dataset
	 * @param textDir
	 * @param output
	 * @throws IOException
	 */
	public void wekaArffGenerate(String textDir,String output) throws IOException{
		TextDirectoryLoader loader = new TextDirectoryLoader();
	    loader.setDirectory(new File(textDir));
	    Instances dataRaw = loader.getDataSet();
	    FileWriter fw = new FileWriter(output);
	       BufferedWriter bw = new BufferedWriter(fw );
	       bw.write( dataRaw.toString() );
	       bw.close();
	       fw.close();
	       }
	
   /**
    *  weka arff file read
    * @param fileName
    * @throws Exception
    */
    public void getFileInstances( String fileName ) throws Exception {  
        DataSource frData = new DataSource( fileName );  
        m_instances = frData.getDataSet();  
        m_instances.setClassIndex(1); 
        for(int i = 0; i<m_instances.classAttribute().numValues();i++){
        	System.out.print(m_instances.classAttribute().value(i)+"\n");
        }
        }
    /**
     * feature selection: 
     * 1, use string to word vector filter to transform the strings to vector.
     * 2, use information gain and choose the top N features
     * @param numberToSelect (the top N number of features to keep)
     * @throws Exception
     */
    public void featureSelect(int numberToSelect) throws Exception{
    	StringToWordVector stw = new StringToWordVector();
		stw.setAttributeIndices("first");
		//stw.setTFTransform(true);
		//stw.setIDFTransform(true);
		stw.setStemmer(new NullStemmer());
		stw.setInputFormat(m_instances);
		numOfAttribute = Filter.useFilter(m_instances, stw).numAttributes();
		//System.out.print(numOfAttribute);
		
        AttributeSelection as = new AttributeSelection();   
        InfoGainAttributeEval infoGainEval = new InfoGainAttributeEval();
		Ranker ranker = new Ranker();
		ranker.setNumToSelect(numOfAttribute/numberToSelect);
		as.setEvaluator(infoGainEval);
		as.setSearch(ranker);
        
        
        Filter[] filters = new Filter[2];
        filters[0] = stw;
        filters[1] = as;
        multifilter = new MultiFilter();
        multifilter.setFilters(filters);
    }
    /**
     * classification:
     * use smo algo. to classify and do N folds cross validation
     * @param fold (N folds)
     * @throws Exception
     */
    public void classify(int fold) throws Exception{
    	fc = new FilteredClassifier();
    	fc.setFilter(multifilter);
    	fc.setClassifier(new SMO());
    	fc.buildClassifier(m_instances);
    	Evaluation eval = new Evaluation(m_instances);
    	eval.crossValidateModel(fc,m_instances,fold,new Random(1));
    	System.out.println( eval.toSummaryString() );  
        System.out.println(eval.toClassDetailsString());
    }
    
    /**
	 * This method saves the trained model into a file. This is done by
	 * simple serialization of the classifier object.
	 * @param fileName The name of the file that will store the trained model.
	 */
	public void saveModel(String fileName) {
		try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName));
            out.writeObject(fc);
            out.close();
 			System.out.println("===== Saved model: " + fileName + " =====");
        } 
		catch (IOException e) {
			System.out.println("Problem found when writing: " + fileName);
		}
	}
    /**
     * main method:
     * args[0]:training text directory 
     * args[1]:arff file directory
     * args[2]:the number of features to select
     * args[3]:the number of folds for cross validation
     * args[4]:the directory of the saved model
     * @param args
     * @throws Exception
     */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		WekaOperation w = new WekaOperation();
		w.wekaArffGenerate(args[0], args[1]);
		w.getFileInstances(args[1]);
		w.featureSelect(Integer.parseInt(args[2]));
		w.classify(Integer.parseInt(args[3]));
		w.saveModel(args[4]);
		}

}
