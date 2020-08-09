package edu.kit.aifb.gwifi.textcategorization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.Mongo;

/**
 * get a mapping (csv file) of category id, article name, and category article
 * depth the depth is the shortest
 * @author aifb-ls3
 *
 */
public class EnglishTextExtract {
	public Mongo mongo;
	public DB db;
	public DBCollection engMapping_dep1;
	public BasicDBObject artQuery;
	private PrintWriter pwr;
	private File outputDirectory;
	private File outputFileName;
	final String patternStr = "template|list|wikipedia|List|Wikipedia|Template";
	private List<String> cateid = new ArrayList<String>();
	private Set<String> artNames = new HashSet<String>();
	private BasicDBObject artNameQuery = new BasicDBObject();
	private BasicDBObject artDepQuery = new BasicDBObject();

	/**
	 * 
	 * @param dir
	 *            :directory name
	 * @param fileName
	 *            : the min_mapping csv filename
	 * @param cateFile
	 *            : the dictionary of category(category id,category name)
	 * @param mongoAddress
	 *            : mongodb address
	 * @throws Exception
	 */
	public EnglishTextExtract(String dir, String fileName, String cateFile,
			String mongoAddress) throws Exception {
		outputDirectory = new File(dir);
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs();
		}
		outputFileName = new File(outputDirectory.getAbsolutePath()
				+ File.separator + fileName);
		pwr = new PrintWriter(new FileWriter(outputFileName, true));
		try {
			// mongo = new Mongo("localhost", 27017);
			mongo = new Mongo(mongoAddress, 19010);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		db = mongo.getDB("congDB");
		engMapping_dep1 = db.getCollection("engMapping");
		artQuery = new BasicDBObject();
		loadAllCateIDwithFilter(cateFile);
		// loadAllCateIDwithFilter("/Users/aifb-ls3/MasterThesis/English_operation/eng_cate.csv");
	}

	/**
	 * import the mapping file with category id,article name and depth(all the
	 * depth but not the shortest) to the mongodb
	 * 
	 * @param inputFile
	 *            : mapping filename
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public void importEngMapping(String inputFile)
			throws NumberFormatException, IOException {
		File input = new File(inputFile);
		BufferedReader br = new BufferedReader(new FileReader(input));
		String line = null;
		int count = 1;
		engMapping_dep1.drop();
		while (((line = br.readLine()) != null)) {
			if (!line.contains(",")) {
				continue;
			}
			int artDep;
			String cateID = line.substring(0, line.indexOf(","));
			String artTitle = line.substring(line.indexOf(",") + 1,
					line.lastIndexOf(","));
			String art2CatDep = line.substring(line.lastIndexOf(",") + 1);
			artDep = Integer.parseInt(art2CatDep);
			BasicDBObject doc = new BasicDBObject();
			doc.append("cateID", cateID);
			doc.append("artTitle", artTitle);
			doc.append("artDep", artDep);
			engMapping_dep1.insert(doc);
			count++;
			if (count % 10000 == 0) {
				System.out.println(count);
			}
		}
	}

	/**
	 * load all the articles from the mongodb to a set
	 */
	public void loadAllArtsinSets() {
		DBCursor cur = engMapping_dep1.find();
		while (cur.hasNext()) {
			String art = cur.next().get("artTitle").toString();
			artNames.add(art);
		}
	}

	/**
	 * all the category (without the category"list,template...") id is loaded
	 * 
	 * @param inputFile
	 *            : mapping dic file
	 * @throws IOException
	 */
	public void loadAllCateIDwithFilter(String inputFile) throws IOException {
		File input = new File(inputFile);
		BufferedReader br = new BufferedReader(new FileReader(input));
		String line = null;
		while (((line = br.readLine()) != null)) {
			String idName[] = line.split(",");
			if (this.categoryFilter(idName[1]) == false) {
				cateid.add(idName[0]);
			}
		}
	}

	/**
	 * to delelte the unnecesary cateogry for example: the category name with
	 * "list, template,.."
	 * 
	 * @param a
	 *            :the string to compare
	 * @return
	 */
	public boolean categoryFilter(String a) {
		boolean z = false;
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(a);
		z = matcher.find();
		return z;
	}

	/**
	 * find the minmum depth mapping between the category and the articles
	 */
	public void findMinPath() {
		int m = 0;
		
	    for (String s : artNames) {
			artNameQuery.clear();
			artNameQuery.put("artTitle", s);
			DBCursor artCollection = engMapping_dep1.find(artNameQuery);
			
	
			// the min depth//
			int minDepth = Integer.parseInt((engMapping_dep1.find(artNameQuery))
					.sort(new BasicDBObject("artDep", 1)).limit(1).next()
					.get("artDep").toString());
//			int secminDepth = Integer.parseInt((engMapping.find(artNameQuery))
//					.sort(new BasicDBObject("artDep", 1)).limit(2).next()
//					.get("artDep").toString());
			 
			while (artCollection.hasNext()) {
				int a = Integer.parseInt(artCollection.next().get("artDep")
						.toString());
				String id = artCollection.curr().get("cateID").toString();
				if (a == minDepth && cateid.contains(id)) {
					pwr.println(artCollection.curr().get("cateID").toString()
							+ ","
							+ artCollection.curr().get("artTitle").toString()
							+ ","
							+ artCollection.curr().get("artDep").toString());
					pwr.flush();
					System.out.print(artCollection.curr().get("cateID")
							.toString()
							+ ","
							+ artCollection.curr().get("artTitle").toString()
							+ ","
							+ artCollection.curr().get("artDep").toString()
							+ "\n");
				}

			}
			m++;
			if ((m % 1) == 0) {
				System.out.println(":::::::::::" + m + " / " + artNames.size()
						+ "::::::::::::");
			}
		}

		pwr.close();

	}
/**
 * 
 * @param args0: directory of the file("/home/ls3data/users/lzh/congliu")
 *        args1: minimum depth mapping csv file
 *        args2: mapping file (category name and id)
 *        
 * @throws Exception
 */
	public static void main(String[] args) throws Exception {
//		 EnglishTextExtract ee = new
//		 EnglishTextExtract("/Users/aifb-ls3/MasterThesis/English_operation","minDep_mapping_en.csv","/Users/aifb-ls3/MasterThesis/English_operation/eng_cate.csv","19010");
//		 ee.importEngMapping("/Users/aifb-ls3/MasterThesis/English_operation/testInput.csv");
//		 ee.loadAllArtsinSets();
//		 ee.findMinPath();

		EnglishTextExtract ee = new EnglishTextExtract(
				args[0], args[1],args[2],"aifb-ls3-remus.aifb.kit.edu");
		//ee.importEngMapping(args[2]);
		System.out.print("load cateFile finished");
		ee.loadAllArtsinSets();
		System.out.print("load article name finished");
		ee.findMinPath();
		System.out.print("all finished");
	}

}
