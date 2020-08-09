package edu.kit.aifb.gwifi.factorization;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

public class FactorizedRelatedness {
	
	String file_A_e = "/home/ls3data/users/sto/factorization/A_e.txt";
	String file_A_a = "/home/ls3data/users/sto/factorization/A_a.txt";
	String file_A_c = "/home/ls3data/users/sto/factorization/A_c.txt";
	String file_A_p = "/home/ls3data/users/sto/factorization/A_p.txt";
	
	String ser_A_e = "/home/ls3data/users/sto/factorization/A_e.ser";
	String ser_A_a = "/home/ls3data/users/sto/factorization/A_a.ser";
	String ser_A_c = "/home/ls3data/users/sto/factorization/A_c.ser";
	String ser_A_p = "/home/ls3data/users/sto/factorization/A_p.ser";
	
	String file_articleMap = "/home/ls3data/users/sto/factorization/ArticlePageIDMatrixIDHashMap";
	String file_categoryMap = "/home/ls3data/users/sto/factorization/CategoryPageIDMatrixIDHashMap";
	String file_phraseMap = "/home/ls3data/users/sto/factorization/PhrasesMatrixIDHashMap";
	
	String count_index_A_e = "/home/ls3data/users/sto/factorization/COUNT_index50A_e";
	String count_index_A_a = "/home/ls3data/users/sto/factorization/COUNT_index50A_a";
	String count_index_A_c = "/home/ls3data/users/sto/factorization/COUNT_index50A_c";
	String sign_index_A_e = "/home/ls3data/users/sto/factorization/SIGN_index50A_e";
	String sign_index_A_a = "/home/ls3data/users/sto/factorization/SIGN_index50A_a";
	String sign_index_A_c = "/home/ls3data/users/sto/factorization/SIGN_index50A_c";
	String normal_index_A_e = "/home/ls3data/users/sto/factorization/NORMAL_index50A_e";
	String normal_index_A_a = "/home/ls3data/users/sto/factorization/NORMAL_index50A_a";
	String normal_index_A_c = "/home/ls3data/users/sto/factorization/NORMAL_index50A_c";
//	String index_A_p = "/home/ls3data/users/sto/factorization/index50A_p";
	
	private IndexSearcher searcherA_e;
	private IndexSearcher searcherA_a;
	private IndexSearcher searcherA_c;
	private IndexSearcher searcherA_p;
	
	double[][] A_e;
	double[][] A_a;
	double[][] A_c;
	double[][] A_p;
	
	HashMap<Integer, Integer> articleMap;
	HashMap<Integer, Integer> categoryMap;
	HashMap<String, Integer> phraseMap;
	
	@SuppressWarnings("unchecked")
	public FactorizedRelatedness(String model) throws FileNotFoundException, ClassNotFoundException, IOException {
		// load article -> matrix ID
		articleMap = (HashMap<Integer, Integer>) loadHashMap(file_articleMap);
//		// load category -> matrix ID
//		HashMap<Integer, Integer> categoryMap = (HashMap<Integer, Integer>) loadHashMap(file_categoryMap);
		// load label -> matrix ID
		phraseMap = (HashMap<String, Integer>) loadHashMap(file_phraseMap);		
		
		A_e = loadMatrix(ser_A_e);
		A_a = loadMatrix(ser_A_a);
////		A_c = loadMatrix(ser_A_c);
//		A_p = loadMatrix(ser_A_p);
		
		if(model.equals("count")) {
			searcherA_e = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(count_index_A_e))));	
			searcherA_a = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(count_index_A_a))));	
			searcherA_c = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(count_index_A_c))));	
//			searcherA_p = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(index_A_p))));	
		} else if(model.equals("normal")) {
			searcherA_e = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(normal_index_A_e))));	
			searcherA_a = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(normal_index_A_a))));	
			searcherA_c = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(normal_index_A_c))));	
//			searcherA_p = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(index_A_p))));
		} else if(model.equals("sign")) {
			searcherA_e = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(sign_index_A_e))));	
			searcherA_a = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(sign_index_A_a))));	
			searcherA_c = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(sign_index_A_c))));	
//			searcherA_p = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(index_A_p))));
		}
	}
	
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
		FactorizedRelatedness fr = new FactorizedRelatedness(args[0]);
		int id1 = Integer.parseInt(args[0]);
		int id2 = Integer.parseInt(args[1]);
		System.out.println(id1);
		System.out.println(id2);
		System.out.println(fr.getEntityArticleRelatedness(id1, id2));
		System.out.println(fr.getEntityArticleRelatednessLucene(id1, id2));
		System.out.println(fr.getEntityPhraseRelatedness(id1, "Bill Gates"));
		System.out.println(fr.getEntityPhraseRelatednessLucene(id1, "Bill Gates"));
    }
	
	private double[] getRowFromA_e(int id) throws IOException {

		Query query = NumericRangeQuery.newIntRange("index", id, id, true, true);

		TopDocs result = searcherA_e.search(query, 1);
		if(result.scoreDocs.length == 0)
			return null;
		Document firstHit = searcherA_e.doc(result.scoreDocs[0].doc);
		String s = firstHit.get("values");
		
		String[] vals = s.split(",");
		double[] values = new double[vals.length]; 
		
		// special handling of first and last
		values[0] = Double.parseDouble(vals[0].replace('[', ' '));				
		for (int i = 1; i < vals.length - 1; i++) {
			values[i] = Double.parseDouble(vals[i]);
		}
		values[vals.length - 1] = Double.parseDouble(vals[vals.length - 1].replace(']', ' '));		
		
		return values;
	}
	
	private double[] getRowFromA_a(int id) throws IOException {

		Query query = NumericRangeQuery.newIntRange("index", id, id, true, true);

		TopDocs result = searcherA_a.search(query, 1);
		if(result.scoreDocs.length == 0)
			return null;
		Document firstHit = searcherA_a.doc(result.scoreDocs[0].doc);
		String s = firstHit.get("values");
		
		String[] vals = s.split(",");
		double[] values = new double[vals.length]; 
		
		// special handling of first and last
		values[0] = Double.parseDouble(vals[0].replace('[', ' '));				
		for (int i = 1; i < vals.length - 1; i++) {
			values[i] = Double.parseDouble(vals[i]);
		}
		values[vals.length - 1] = Double.parseDouble(vals[vals.length - 1].replace(']', ' '));
				
		return values;
	}
	
	@SuppressWarnings("unused")
	private double[] getRowFromA_c(int id) throws IOException {

		Query query = NumericRangeQuery.newIntRange("index", id, id, true, true);

		TopDocs result = searcherA_c.search(query, 1);
		if(result.scoreDocs.length == 0)
			return null;
		Document firstHit = searcherA_c.doc(result.scoreDocs[0].doc);
		String s = firstHit.get("values");
		
		String[] vals = s.split(",");
		double[] values = new double[vals.length]; 
		
		// special handling of first and last
		values[0] = Double.parseDouble(vals[0].replace('[', ' '));				
		for (int i = 1; i < vals.length - 1; i++) {
			values[i] = Double.parseDouble(vals[i]);
		}
		values[vals.length - 1] = Double.parseDouble(vals[vals.length - 1].replace(']', ' '));
				
		return values;
	}
	
	private double[] getRowFromA_p(int id) throws IOException {

		Query query = NumericRangeQuery.newIntRange("index", id, id, true, true);

		TopDocs result = searcherA_p.search(query, 1);
		if(result.scoreDocs.length == 0)
			return null;
		Document firstHit = searcherA_p.doc(result.scoreDocs[0].doc);
		String s = firstHit.get("values");
		
		String[] vals = s.split(",");
		double[] values = new double[vals.length]; 
		
		// special handling of first and last
		values[0] = Double.parseDouble(vals[0].replace('[', ' '));				
		for (int i = 1; i < vals.length - 1; i++) {
			values[i] = Double.parseDouble(vals[i]);
		}
		values[vals.length - 1] = Double.parseDouble(vals[vals.length - 1].replace(']', ' '));
				
		return values;
	}
	
	/**
	 * Returns the relatedness between two wikipedia articles.
	 * 
	 * @param PageID1 id of wikipedia page
	 * @param PageID2 id of wikipedia page
	 * @return relatedness between the two pages
	 */
	public double getEntityArticleRelatednessLucene(int PageID1, int PageID2) throws IOException, InterruptedException {

		if(!articleMap.containsKey(PageID1) || !articleMap.containsKey(PageID2))
			return 0.0;
		
		double[] vector1 = getRowFromA_e(articleMap.get(PageID1));
		double[] vector2 = getRowFromA_a(articleMap.get(PageID2));
						
		double sum = 0;
		for (int i = 0; i < vector1.length; i++) {
			sum += vector1[i] * vector2[i];
		}
		
		return sum;
	}
	
	/**
	 * Returns the relatedness between two wikipedia articles.
	 * 
	 * @param PageID1 id of wikipedia page
	 * @param PageID2 id of wikipedia page
	 * @return relatedness between the two pages
	 */
	public double getEntityArticleRelatedness(int PageID1, int PageID2) throws IOException, InterruptedException {
			
		if(!articleMap.containsKey(PageID1) || !articleMap.containsKey(PageID2))
			return 0.0;
			
		int index1 = articleMap.get(PageID1);
		int index2 = articleMap.get(PageID2);
		
		if(index1 > A_e.length || index2 > A_a.length)
			return 0.0;
		double[] vector1 = A_e[index1 - 1];
		double[] vector2 = A_a[index2 - 1];
						
		double sum = 0;
		for (int i = 0; i < vector1.length; i++) {
			sum += vector1[i] * vector2[i];
		}
		
		return sum;
	}
	
	/**
	 * Returns the relatedness between wikipedia article and label 
	 * 
	 * @param PageID1 id of wikipedia page
	 * @param label 
	 * @return relatedness between the two pages
	 */
	public double getEntityPhraseRelatedness(int PageID1, String label) throws IOException, InterruptedException {
		double[] vector1 = A_e[articleMap.get(PageID1) - 1];
		double[] vector2 = A_p[phraseMap.get(label) - 1];
				
		double sum = 0;
		for (int i = 0; i < vector1.length; i++) {
			sum += vector1[i] * vector2[i];
		}
		
		return sum;
	}
	
	/**
	 * Returns the relatedness between wikipedia article and label 
	 * 
	 * @param PageID1 id of wikipedia page
	 * @param label 
	 * @return relatedness between the two pages
	 */
	public double getEntityPhraseRelatednessLucene(int PageID1, String label) throws IOException, InterruptedException {

		double[] vector1 = getRowFromA_e(articleMap.get(PageID1));
		double[] vector2 = getRowFromA_p(phraseMap.get(label));
						
		double sum = 0;
		for (int i = 0; i < vector1.length; i++) {
			sum += vector1[i] * vector2[i];
		}
		
		return sum;
	}
		
//	public double[] getRow(int row, String file) throws IOException, InterruptedException {
//		Process p;
//		String[] command = new String[] {"sed", "-n", "'" + row+"{p;q;}'",file};
//
//		p = Runtime.getRuntime().exec(command);
//		BufferedReader br = new BufferedReader(new InputStreamReader(
//				p.getInputStream()));
//
//		String s = br.readLine();
//		
//		String[] valuesString = s.trim().split("\\s+");
//		double[] valuesDouble = new double[valuesString.length];
//
//		for (int i = 0; i < valuesString.length; i++) {
////			System.out.println(valuesString[i]);
//			valuesDouble[i] = Double.parseDouble(valuesString[i]);
//		}
//		p.waitFor();
//		p.destroy();
//		return valuesDouble;
//	}
	
	public static HashMap<?, ?> loadHashMap(String file) throws FileNotFoundException, IOException, ClassNotFoundException {
		ObjectInputStream i = new ObjectInputStream(new FileInputStream(file));
		HashMap<?, ?> map = (HashMap<?, ?>) i.readObject();
		i.close();
		return map;
	}
	
	public static double[][] loadMatrix(String file) throws FileNotFoundException, IOException, ClassNotFoundException {
		ObjectInputStream i = new ObjectInputStream(new FileInputStream(file));
		double[][] matrix = (double[][]) i.readObject();
		i.close();
		return matrix;
	}
}
