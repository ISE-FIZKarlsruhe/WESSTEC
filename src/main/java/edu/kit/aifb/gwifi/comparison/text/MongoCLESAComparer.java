package edu.kit.aifb.gwifi.comparison.text;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Attribute;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.kit.aifb.gwifi.concept.IConceptIterator;
import edu.kit.aifb.gwifi.concept.IConceptVector;
import edu.kit.aifb.gwifi.concept.TroveConceptVector;
import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.mongo.Property;
import edu.kit.aifb.gwifi.util.HeapSort;
import edu.kit.aifb.gwifi.util.nlp.Language;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntIntHashMap;

/**
 * Performs search on the index located in database.
 * 
 * @author Lei Zhang <beyondlei@gmail.com>
 */
public class MongoCLESAComparer implements TextComparer {

	private DB _db;
	private DBCollection _sTermsCollection;
	private DBCollection _tTermsCollection;
	private DBCollection _sArticlesCollection;
	private DBCollection _tArticlesCollection;

	private Language sourceLang;
	private Language targetLang;

	private HashMap<Integer, Double> values = new HashMap<Integer, Double>();;
	private HashMap<String, Integer> freqMap = new HashMap<String, Integer>(30);
	private HashMap<String, Double> tfidfMap = new HashMap<String, Double>(30);
	private HashMap<String, Float> idfMap = new HashMap<String, Float>(30);

	private ArrayList<String> termList = new ArrayList<String>(30);

	private TIntIntHashMap inlinkMap = new TIntIntHashMap(300);

	private Analyzer sourceAnalyzer;
	private Analyzer targetAnalyzer;
	private CLConceptVectorSimilarity sim;

	private static float LINK_ALPHA = 0.5f;
	private int maxConceptId;

	public MongoCLESAComparer(String sLang, String tLang) throws IOException {
		sourceLang = Language.getLanguage(sLang);
		targetLang = Language.getLanguage(tLang);

		Property.setProperties("configs/configuration_esa.properties");
		_db = MongoResource.INSTANCE.getDB();
		_sTermsCollection = _db.getCollection(DBConstants.TERMS_COLLECTION + sourceLang.getLabel());
		_tTermsCollection = _db.getCollection(DBConstants.TERMS_COLLECTION + targetLang.getLabel());
		_sArticlesCollection = _db.getCollection(DBConstants.ARTICLES_COLLECTION + sourceLang.getLabel());
		_tArticlesCollection = _db.getCollection(DBConstants.ARTICLES_COLLECTION + targetLang.getLabel());
		maxConceptId = (int) Math.max(_sArticlesCollection.count(), _tArticlesCollection.count());

		if (sourceLang.equals(Language.EN))
			sourceAnalyzer = new WikipediaEnAnalyzer();
		else if (sourceLang.equals(Language.DE))
			sourceAnalyzer = new WikipediaDeAnalyzer();
		else if (sourceLang.equals(Language.ZH))
			sourceAnalyzer = new WikipediaZhAnalyzer();

		if (targetLang.equals(Language.EN))
			targetAnalyzer = new WikipediaEnAnalyzer();
		else if (targetLang.equals(Language.DE))
			targetAnalyzer = new WikipediaDeAnalyzer();
		else if (targetLang.equals(Language.ZH))
			targetAnalyzer = new WikipediaZhAnalyzer();

		sim = new CLConceptVectorSimilarity(new CosineScorer(), true);
	}

	/**
	 * Calculate semantic relatedness between documents
	 * 
	 * @param doc1
	 * @param doc2
	 * @return returns relatedness if successful, -1 otherwise
	 */
	public double getRelatedness(String doc1, String doc2) {
		try {
			// IConceptVector c1 = getCombinedVector(doc1, sourceAnalyzer, _sTermsCollection, _sArticlesCollection);
			// IConceptVector c2 = getCombinedVector(doc2, targetAnalyzer, _tTermsCollection, _tArticlesCollection);
			// IConceptVector c1 = getNormalVector(getConceptVector(doc1, sourceAnalyzer, _sTermsCollection),10);
			// IConceptVector c2 = getNormalVector(getConceptVector(doc2, targetAnalyzer, _tTermsCollection),10);

			IConceptVector c1 = getConceptVector(doc1, sourceAnalyzer, _sTermsCollection);
			IConceptVector c2 = getConceptVector(doc2, targetAnalyzer, _tTermsCollection);

			if (c1 == null || c2 == null) {
				// return 0;
				return -1; // undefined
			}

			final double rel = sim.calcSimilarity(c1, c2, sourceLang, targetLang);

			// mark for dealloc
			c1 = null;
			c2 = null;

			return rel;

		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}

	}

	/**
	 * Retrieves full vector for regular features
	 * 
	 * @param query
	 * @return Returns concept vector results exist, otherwise null
	 * @throws IOException
	 * @throws SQLException
	 */
	public IConceptVector getConceptVector(String query, Analyzer analyzer, DBCollection termsColl) throws IOException {
		String strTerm;
		int numTerms = 0;
		int doc;
		double score;
		int vint;
		double vdouble;
		double tf;
		double vsum;
		int plen;
		TokenStream ts = analyzer.tokenStream("contents", new StringReader(query));
		ByteArrayInputStream bais;
		DataInputStream dis;

		this.clean();

		ts.reset();

		while (ts.incrementToken()) {

			CharTermAttribute t = ts.addAttribute(CharTermAttribute.class);
			strTerm = t.toString();

			// record term IDF
			if (!idfMap.containsKey(strTerm)) {
				DBCursor cur = termsColl.find(new BasicDBObject("term", strTerm), new BasicDBObject("idf", 1));
				if (cur.hasNext()) {
					idfMap.put(strTerm, ((Double) cur.next().get("idf")).floatValue());
				}
			}

			// records term counts for TF
			if (freqMap.containsKey(strTerm)) {
				vint = freqMap.get(strTerm);
				freqMap.put(strTerm, vint + 1);
			} else {
				freqMap.put(strTerm, 1);
			}

			termList.add(strTerm);

			numTerms++;

		}

		ts.end();
		ts.close();

		if (numTerms == 0) {
			return null;
		}

		// calculate TF-IDF vector (normalized)
		vsum = 0;
		for (String tk : idfMap.keySet()) {
			tf = 1.0 + Math.log(freqMap.get(tk));
			vdouble = (idfMap.get(tk) * tf);
			tfidfMap.put(tk, vdouble);
			vsum += vdouble * vdouble;
		}
		vsum = Math.sqrt(vsum);

		// comment this out for canceling query normalization
		for (String tk : idfMap.keySet()) {
			vdouble = tfidfMap.get(tk);
			tfidfMap.put(tk, vdouble / vsum);
		}

		score = 0;

		for (String tk : termList) {
			DBCursor cur = termsColl.find(new BasicDBObject("term", tk), new BasicDBObject("vector", 1));

			if (cur.hasNext()) {
				bais = new ByteArrayInputStream((byte[]) cur.next().get("vector"));
				dis = new DataInputStream(bais);

				/**
				 * 4 bytes: int - length of array 4 byte (doc) - 8 byte (tfidf) pairs
				 */

				plen = dis.readInt();
				// System.out.println("vector len: " + plen);
				for (int k = 0; k < plen; k++) {
					doc = dis.readInt();
					score = dis.readFloat();
					double newVal = 0;
					if (values.containsKey(doc)) {
						newVal = values.get(doc);
					}
					newVal += score * tfidfMap.get(tk);
					values.put(doc, newVal);
				}

				bais.close();
				dis.close();
			}

		}

		// no result
		if (score == 0) {
			return null;
		}

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
		for (int i = ids.length - 1; i >= 0 && valuesArr[i] > 0; i--) {
			newCv.set(ids[i], valuesArr[i] / numTerms);
		}

		return newCv;

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

	public void clean() {
		freqMap.clear();
		tfidfMap.clear();
		idfMap.clear();

		synchronized (termList) {
			termList.clear();
		}

		inlinkMap.clear();

		values.clear();
	}

	@Override
	protected void finalize() throws Throwable {
		MongoResource.INSTANCE.finalizing();
	}

	/**
	 * Returns the intersection between two vectors
	 * 
	 * @param cv1
	 * @param cv2
	 * @return
	 */
	public IConceptVector getVectorIntersection(IConceptVector cv1, IConceptVector cv2) {
		IConceptIterator it1, it2;
		HashSet<Integer> cvids1 = new HashSet<Integer>();
		HashSet<Integer> cvids2 = new HashSet<Integer>();

		it1 = cv1.orderedIterator();
		it2 = cv2.orderedIterator();

		while (it1.next()) {
			cvids1.add(it1.getId());
		}

		while (it2.next()) {
			cvids2.add(it2.getId());
		}

		boolean firstIsLarger = cvids1.size() > cvids2.size();
		Set<Integer> tmpSet = new HashSet<Integer>(firstIsLarger ? cvids2 : cvids1);
		tmpSet.retainAll(firstIsLarger ? cvids1 : cvids2);

		TroveConceptVector intersec = new TroveConceptVector(tmpSet.size());
		for (Integer cvid : tmpSet) {
			if (cvids1.contains(cvid)) {
				intersec.add(cvid, cv1.get(cvid));
			} else {
				intersec.add(cvid, cv2.get(cvid));
			}
		}

		return intersec;
	}

	private TIntIntHashMap setInlinkCounts(Collection<Integer> ids, DBCollection articlesColl) {
		inlinkMap.clear();

		ArrayList<Integer> lstIds = new ArrayList<Integer>(ids);

		// collect inlink counts
		BasicDBObject query = new BasicDBObject("id", new BasicDBObject("$in", lstIds));
		BasicDBObject fields = new BasicDBObject("id", 1).append("inlinks", 1);
		DBCursor cur = articlesColl.find(query, fields);
		while (cur.hasNext()) {
			DBObject art = cur.next();
			inlinkMap.put((Integer) art.get("id"), (Integer) art.get("inlinks"));
		}

		return inlinkMap;
	}

	private Collection<Integer> getLinks(int id, DBCollection articlesColl) {
		ArrayList<Integer> links; // = new ArrayList<Integer>(100);

		DBObject art = articlesColl.findOne(new BasicDBObject("id", id), new BasicDBObject("pagelinks", 1));

		links = (ArrayList<Integer>) art.get("pagelinks");

		return links;
	}

	public IConceptVector getLinkVector(IConceptVector cv, int limit, DBCollection articlesColl) {
		if (cv == null)
			return null;
		return getLinkVector(cv, true, LINK_ALPHA, limit, articlesColl);
	}

	/**
	 * Computes secondary interpretation vector of regular features
	 * 
	 * @param cv
	 * @param moreGeneral
	 * @param ALPHA
	 * @param LIMIT
	 * @return
	 * @throws SQLException
	 */
	public IConceptVector getLinkVector(IConceptVector cv, boolean moreGeneral, double ALPHA, int LIMIT,
			DBCollection articlesColl) {
		IConceptIterator it;

		if (cv == null)
			return null;

		it = cv.orderedIterator();

		@SuppressWarnings("unused")
		int count = 0;
		ArrayList<Integer> pages = new ArrayList<Integer>();

		TIntFloatHashMap valueMap2 = new TIntFloatHashMap(1000);
		TIntFloatHashMap valueMap3 = new TIntFloatHashMap();

		ArrayList<Integer> npages = new ArrayList<Integer>();

		HashMap<Integer, Float> secondMap = new HashMap<Integer, Float>(1000);

		this.clean();

		// collect article objects
		while (it.next()) {
			pages.add(it.getId());
			valueMap2.put(it.getId(), (float) it.getValue());
			count++;
		}

		// prepare inlink counts
		setInlinkCounts(pages, articlesColl);

		for (int pid : pages) {
			Collection<Integer> raw_links = getLinks(pid, articlesColl);
			if (raw_links.isEmpty()) {
				continue;
			}
			ArrayList<Integer> links = new ArrayList<Integer>(raw_links.size());

			final double inlink_factor_p = Math.log(inlinkMap.get(pid));

			float origValue = valueMap2.get(pid);

			setInlinkCounts(raw_links, articlesColl);

			for (int lid : raw_links) {
				final double inlink_factor_link = Math.log(inlinkMap.get(lid));

				// check concept generality..
				if (inlink_factor_link - inlink_factor_p > 1) {
					links.add(lid);
				}
			}

			for (int lid : links) {
				if (!valueMap2.containsKey(lid)) {
					valueMap2.put(lid, 0.0f);
					npages.add(lid);
				}
			}

			float linkedValue = 0.0f;

			for (int lid : links) {
				if (valueMap3.containsKey(lid)) {
					linkedValue = valueMap3.get(lid);
					linkedValue += origValue;
					valueMap3.put(lid, linkedValue);
				} else {
					valueMap3.put(lid, origValue);
				}
			}

		}

		// for(int pid : pages){
		// if(valueMap3.containsKey(pid)){
		// secondMap.put(pid, (float) (valueMap2.get(pid) + ALPHA * valueMap3.get(pid)));
		// }
		// else {
		// secondMap.put(pid, (float) (valueMap2.get(pid) ));
		// }
		// }

		for (int pid : npages) {
			secondMap.put(pid, (float) (ALPHA * valueMap3.get(pid)));

		}

		// System.out.println("read links..");

		ArrayList<Integer> keys = new ArrayList<Integer>(secondMap.keySet());

		// Sort keys by values.
		final Map<Integer, Float> langForComp = secondMap;
		Collections.sort(keys, new Comparator<Object>() {
			public int compare(Object left, Object right) {
				Integer leftKey = (Integer) left;
				Integer rightKey = (Integer) right;

				Float leftValue = (Float) langForComp.get(leftKey);
				Float rightValue = (Float) langForComp.get(rightKey);
				return leftValue.compareTo(rightValue);
			}
		});
		Collections.reverse(keys);

		IConceptVector cv_link = new TroveConceptVector(maxConceptId);

		int c = 0;
		for (int p : keys) {
			cv_link.set(p, secondMap.get(p));
			c++;
			if (c >= LIMIT) {
				break;
			}
		}

		return cv_link;
	}

	public IConceptVector getCombinedVector(String query, Analyzer analyzer, DBCollection termsColl,
			DBCollection articlesColl) throws IOException {
		IConceptVector cvBase = getConceptVector(query, analyzer, termsColl);
		IConceptVector cvNormal, cvLink;

		if (cvBase == null) {
			return null;
		}

		cvNormal = getNormalVector(cvBase, 10);
		cvLink = getLinkVector(cvNormal, 5, articlesColl);

		cvNormal.add(cvLink);

		return cvNormal;
	}

}
