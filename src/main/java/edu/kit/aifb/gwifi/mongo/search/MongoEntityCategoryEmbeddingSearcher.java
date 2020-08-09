package edu.kit.aifb.gwifi.mongo.search;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

import edu.kit.aifb.gwifi.concept.ConceptVectorSimilarity;
import edu.kit.aifb.gwifi.concept.CosineScorer;
import edu.kit.aifb.gwifi.concept.IConceptIterator;
import edu.kit.aifb.gwifi.concept.IConceptVector;
import edu.kit.aifb.gwifi.concept.TroveConceptVector;
import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.mongo.Property;
import edu.kit.aifb.gwifi.util.HeapSort;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class MongoEntityCategoryEmbeddingSearcher {

	private DBCollection _dbEntityEmbeddingCollection;
	private DBCollection _dbCategoryEmbeddingCollection;

	private Language _lang;


	private Map<String,IConceptVector> article2Vector;
	private Map<String,IConceptVector> cate2Vector;
	
	int maxConceptId;

	ConceptVectorSimilarity sim = new ConceptVectorSimilarity(new CosineScorer());

	public MongoEntityCategoryEmbeddingSearcher(Language lang) {
		_lang = lang;
		_dbEntityEmbeddingCollection = MongoResource.INSTANCE.getDB()
				.getCollection(DBConstants.ENTITY_EMBEDDING_COLLECTION + _lang.getLabel());
		_dbCategoryEmbeddingCollection = MongoResource.INSTANCE.getDB()
				.getCollection(DBConstants.CATEGORY_EMBEDDING_COLLECTION + _lang.getLabel());
		this.article2Vector = new HashMap<String, IConceptVector>();
		this.cate2Vector = new HashMap<String, IConceptVector>();
	}

	synchronized public IConceptVector getConceptVector(String name, boolean isCate) throws IOException {
		DBCursor cur = null;

		if (isCate) {
			if(cate2Vector.containsKey(name)) return cate2Vector.get(name);
			cur = _dbCategoryEmbeddingCollection.find(new BasicDBObject(DBConstants.CATEGORY_EMBEDDING_NAME, name),
					new BasicDBObject(DBConstants.ENTITY_EMBEDDING_VECTOR, 1));
		} else {
			if(article2Vector.containsKey(name)) return article2Vector.get(name);
			cur = _dbEntityEmbeddingCollection.find(new BasicDBObject(DBConstants.ENTITY_EMBEDDING_NAME, name),
					new BasicDBObject(DBConstants.ENTITY_EMBEDDING_VECTOR, 1));
		}

		if (cur != null && cur.hasNext()) {
			ByteArrayInputStream bais = new ByteArrayInputStream((byte[]) cur.next().get("vector"));
			DataInputStream dis = new DataInputStream(bais);

			/**
			 * 4 bytes: int - length of array 4 byte (id) - 8 byte (weight) pairs
			 */

			int plen = dis.readInt();
			HashMap<Integer, Double> values = new HashMap<Integer, Double>();
			for (int k = 0; k < plen; k++) {
				int id = dis.readInt();
				double weight = dis.readDouble();
				values.put(id, weight);
			}

			bais.close();
			dis.close();

			double[] valuesArr = new double[values.size()];
			int[] ids = new int[values.size()];
			int elemCount = 0;

			for (int id : values.keySet()) {
				ids[elemCount] = id;
				valuesArr[elemCount] = values.get(id);
				elemCount++;
			}

			HeapSort.heapSort(valuesArr, ids);

			IConceptVector newCv = new TroveConceptVector(ids.length);
			for (int i = ids.length - 1; i >= 0 ; i--) {
				newCv.set(ids[i], valuesArr[i]);
			}

			if(isCate) cate2Vector.put(name, newCv);
			else article2Vector.put(name, newCv);
			return newCv;

		}
		if(isCate) cate2Vector.put(name, null);
		else article2Vector.put(name, null);
//		System.out.println(isCate + ": vector not found in mongo: " + name);
		return null;
	}

	/**
	 * Returns trimmed form of concept vector
	 * 
	 * @param cv
	 * @return
	 */
	public IConceptVector getNormalVector(IConceptVector cv, int LIMIT) {
		IConceptVector cv_normal = new TroveConceptVector(LIMIT);
		IConceptIterator it;

		if (cv == null)
			return null;

		it = cv.orderedIterator();

		int count = 0;
		while (it.next()) {
			if (count >= LIMIT)
				break;
			cv_normal.set(it.getId(), it.getValue());
			count++;
		}

		return cv_normal;
	}

	/**
	 * Calculate semantic relatedness between entities or categories
	 * 
	 * @param name1
	 * @param name2
	 * @return returns relatedness if successful, -1 otherwise
	 */
	public double getRelatedness(String name1, boolean isCate1, String name2, boolean isCate2) {
		try {
			IConceptVector c1 = getConceptVector(name1.toLowerCase(), isCate1);
			IConceptVector c2 = getConceptVector(name2.toLowerCase(), isCate2);

			if (c1 == null || c2 == null) {
				// return 0;
				return -1; // undefined
			}

			final double rel = sim.calcSimilarity(c1, c2);

			// mark for dealloc
			c1 = null;
			c2 = null;

			return rel;

		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}

	}

	public static void main(String[] args) throws IOException {
		Property.setProperties("configs/MongoConfig_esa.properties");
		MongoEntityCategoryEmbeddingSearcher searcher = new MongoEntityCategoryEmbeddingSearcher(Language.EN);

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			System.out.println("\nEnter entity and category names (or enter to quit): ");
			String line = in.readLine();

			if (line.equals("exit"))
				break;

			String[] names = line.split(" ");
			if(names.length != 2)
				continue;
			String name1 = names[0].substring(2).replace("_", " ");
			String name2 = names[1].substring(2).replace("_", " ");

			double relatedness = searcher.getRelatedness(name1, names[0].startsWith("c_"), name2, names[1].startsWith("c_"));
			System.out.println("\nThe relatedness between \"" + names[0] + "\" and \"" + names[1] + "\" is: " + relatedness);
		}
	}

}
