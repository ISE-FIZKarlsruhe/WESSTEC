package edu.kit.aifb.gwifi.textcategorization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

/**
 * 1,transform the unlabeled text into the weka instance 2,call the weka model
 * which is already trained by wekaoperation.java
 * 
 * @author aifb-ls3
 *
 */
public class MyFilteredClassifier {

	/**
	 * String that stores the text to classify
	 */
	String text;
	/**
	 * Object that stores the instance.
	 */
	Instances instances;
	/**
	 * Object that stores the classifier.
	 */
	FilteredClassifier classifier;

	// Instances vec_form;
	Map<String,Double> cate_prop;
	static List<String> cateName = new ArrayList<String>();
	String patStr = "template|list|wikipedia|List|Wikipedia|Template";
	
	public boolean categoryFilter(String a) {
		boolean z = false;
		Pattern pattern = Pattern.compile(patStr);
		Matcher matcher = pattern.matcher(a);
		z = matcher.find();
		return z;
	}
	
	public static List<String> getClassName(String fileName) throws Exception{
		DataSource frData = new DataSource( fileName );  
        Instances ins = frData.getDataSet();  
        ins.setClassIndex(1); 
        for(int i = 0; i<ins.classAttribute().numValues();i++){
        	cateName.add(ins.classAttribute().value(i));
        	}
        return cateName;
	}
	
//	public void readCategoryFile(String cateogryFileName) throws IOException{
//		//List<String> cateID = new ArrayList<String>();
//		cateName = new ArrayList<String>();
//		File file = new File(cateogryFileName);
//		BufferedReader bf = new BufferedReader(new FileReader(file));
//		String line = null;
//		while ((line = bf.readLine()) != null) {
//            String parts[] = line.split(",");
//            if(this.categoryFilter(parts[1])==false){
//            cateName.add(parts[1]);
//            }
//            }
//        bf.close();
//
//        }

	/**
	 * This method loads the text to be classified.
	 * 
	 * @param fileName
	 *            The name of the file that stores the text.
	 */
	public void load(String fileName) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			String line;
			text = "";
			while ((line = reader.readLine()) != null) {
				text = text + " " + line;
			}
			System.out
					.println("===== Loaded text data: " + fileName + " =====");
			reader.close();
			text = RetrieveArticlesInCategory.wordPreprocess(text);
			//System.out.println(text);
		} catch (IOException e) {
			System.out.println("Problem found when reading: " + fileName);
		}
	}

	/**
	 * This method loads the model to be used as classifier.
	 * 
	 * @param fileName
	 *            The name of the file that stores the text.
	 */
	public void loadModel(String fileName) {
		try {
			// classifier = (Classifier) weka.core.SerializationHelper
			// .read(fileName);
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(
					fileName));
			Object tmp = in.readObject();
			classifier = (FilteredClassifier) tmp;
			in.close();
			//System.out.println("===== Loaded model: " + fileName + " =====");
		} catch (Exception e) {
			// Given the cast, a ClassNotFoundException must be caught along
			// with the IOException
			System.out.println("Problem found when reading: " + fileName);
		}
	}

	/**
	 * This method creates the instance to be classified, from the text that has
	 * been read.
	 */
	public void makeInstance() {
		// Create the attributes, class and text
		FastVector fvNominalVal = new FastVector(2);
		for(String s:cateName){
			fvNominalVal.addElement(s);
		}
		
		//System.out.print(fvNominalVal);
		Attribute attribute1 = new Attribute("text", (FastVector) null);
		Attribute attribute2 = new Attribute("class", fvNominalVal);
		// Create list of instances with one element
		FastVector fvWekaAttributes = new FastVector(2);
		fvWekaAttributes.addElement(attribute1);
		fvWekaAttributes.addElement(attribute2);
		instances = new Instances("Test relation", fvWekaAttributes, 1);
		// Set class index
		instances.setClassIndex(1);
		// Create and add the instance
		Instance instance = new Instance(2);
		instance.setValue(attribute1, text);
		// Another way to do it:
		// instance.setValue((Attribute)fvWekaAttributes.elementAt(1), text);
		instances.add(instance);
		//System.out
				//.println("===== Instance created with reference dataset =====");
		//System.out.println(instances);
	}
	public void makeInstance(FastVector fvNominalVal) {
		// Create the attributes, class and text
//		fvNominalVal = new FastVector(2);
//		for(String s:cateName){
//			fvNominalVal.addElement(s);
//		}
		
		//System.out.print(fvNominalVal);
		Attribute attribute1 = new Attribute("text", (FastVector) null);
		Attribute attribute2 = new Attribute("class", fvNominalVal);
		// Create list of instances with one element
		FastVector fvWekaAttributes = new FastVector(2);
		fvWekaAttributes.addElement(attribute1);
		fvWekaAttributes.addElement(attribute2);
		instances = new Instances("Test relation", fvWekaAttributes, 1);
		// Set class index
		instances.setClassIndex(1);
		// Create and add the instance
		Instance instance = new Instance(2);
		instance.setValue(attribute1, text);
		// Another way to do it:
		// instance.setValue((Attribute)fvWekaAttributes.elementAt(1), text);
		instances.add(instance);
		//System.out
				//.println("===== Instance created with reference dataset =====");
		//System.out.println(instances);
	}

	/**
	 * This method performs the classification of the instance. Output is done
	 * at the command-line.
	 */
	public void classify() {
		cate_prop = new HashMap<String,Double>();
		//List<Map.Entry<String,Double>> cate_prop_list=new ArrayList<>(); 
		try {
			//double pred = classifier.classifyInstance(instances.instance(0));
			double[] pred = classifier.distributionForInstance(instances.instance(0));
			System.out.println("===== Classified instance =====");
			
			for(int i=0; i<pred.length;i++){
				cate_prop.put(instances.classAttribute().value((int)i), pred[i]);
				System.out.print(instances.classAttribute().value((int)i)+":"+pred[i]+"\n");
				
			}
//			cate_prop_list.addAll(cate_prop.entrySet()); 
//			ValueComparator bvc =  new ValueComparator(cate_prop);
//	        TreeMap<String,Double> sorted_map = new TreeMap<String,Double>(bvc);
//	        sorted_map.putAll(cate_prop);
//	        System.out.println("unsorted map: "+cate_prop);
//	        System.out.println("results: "+sorted_map+"\n");
	        
	        
	        
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
	class ValueComparator implements Comparator<String> {

	    Map<String, Double> base;
	    public ValueComparator(Map<String, Double> base) {
	        this.base = base;
	    }

	    // Note: this comparator imposes orderings that are inconsistent with equals.    
	    public int compare(String a, String b) {
	        if (base.get(a) >= base.get(b)) {
	            return -1;
	        } else {
	            return 1;
	        } // returning 0 would merge keys
	    }
	}
	

	public List<String> getCateName() {
		return cateName;
	}

	public void setCateName(List<String> cateName) {
		this.cateName = cateName;
	}
	

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
	

	public Map<String, Double> getCate_prop() {
		return cate_prop;
	}

	public void setCate_prop(Map<String, Double> cate_prop) {
		this.cate_prop = cate_prop;
	}

	/**
	 * Main method. It is an example of the usage of this class.
	 * 
	 * @param args0: arff file name
	 * 		  args1: text file
	 * 		  args2: model file
	 *            
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		//MyFilteredClassifier.getClassName(args[0]);
		MyFilteredClassifier.getClassName("res/weka/ReutersDataset_8.arff");
		MyFilteredClassifier classifier;
//		if (args.length < 2)
//			System.out
//					.println("Usage: java MyClassifier <fileData> <fileModel>");
//		else {
			classifier = new MyFilteredClassifier();
			//classifier.load(args[1]);
			classifier.load("res/test.txt");
			//classifier.loadModel(args[2]);
			classifier.loadModel("res/weka/ReutersDataset_8.model");
			classifier.makeInstance();
			classifier.classify();
//		}
	}
}