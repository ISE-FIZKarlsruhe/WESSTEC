package edu.kit.aifb.gwifi.comparison.text;

import java.util.HashMap;

import com.mongodb.DB;
import com.mongodb.DBCollection;

import edu.kit.aifb.gwifi.concept.IConceptIterator;
import edu.kit.aifb.gwifi.concept.IConceptVector;
import edu.kit.aifb.gwifi.mongo.DBConstants;
import edu.kit.aifb.gwifi.mongo.MongoResource;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class CLConceptVectorSimilarity {

	IScorer m_scorer;

	private DB _db;
	private DBCollection _langlinksCollection;

	private HashMap<Integer, Integer> sId2tIdMap;
	private HashMap<Integer, Integer> tId2sIdMap;
	private boolean useMap;

	public CLConceptVectorSimilarity(IScorer scorer, boolean useMap) {
		m_scorer = scorer;

		_db = MongoResource.INSTANCE.getDB();
		_langlinksCollection = _db.getCollection(DBConstants.LANGLINKS_COLLECTION);

		this.useMap = useMap;
		if (useMap) {
			sId2tIdMap = new HashMap<Integer, Integer>();
			tId2sIdMap = new HashMap<Integer, Integer>();
			// TODO: add all mappings into idMapping ...
		}
	}

	public double calcSimilarity(IConceptVector v0, IConceptVector v1, Language l0, Language l1) {
		m_scorer.reset(v0.getData(), v1.getData(), 1);

		IConceptIterator it0 = v0.iterator();
		while (it0.next()) {
			int id0 = it0.getId();
			int id1 = id0;
			if (!l0.equals(l1)) {
				id1 = getMappedId(id0, l0, l1);
			}
			if (id1 != 0) {
				double value1 = v1.get(id1);
				if (value1 > 0) {
					m_scorer.addConcept(id0, it0.getValue(), id1, value1, 1);
				}
			}
		}

		m_scorer.finalizeScore(v0.getData(), v1.getData());

		return m_scorer.getScore();
	}

	public int getMappedId(int id0, Language l0, Language l1) {
		if (useMap == true) {
			Integer id = sId2tIdMap.get(id0);
			if (id != null) {
				return id.intValue();
			}
			id = tId2sIdMap.get(id0);
			if (id != null) {
				return id.intValue();
			}
		} else {
			// TODO: use mongodb
		}

		return 0;
	}

}
