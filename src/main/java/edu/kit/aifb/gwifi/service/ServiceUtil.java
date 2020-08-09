package edu.kit.aifb.gwifi.service;

import edu.kit.aifb.gwifi.service.Service.DisambiguationModel;
import edu.kit.aifb.gwifi.service.Service.KB;
import edu.kit.aifb.gwifi.service.Service.NLPModel;

public class ServiceUtil {
	
	public static DisambiguationModel getDisambiguationModel(String model) {
		if(model.toLowerCase().equals("prior"))
			return DisambiguationModel.PRIOR;
		else if(model.toLowerCase().equals("betweeness"))
			return DisambiguationModel.BETWEENESS;
		else if(model.toLowerCase().equals("distance"))
			return DisambiguationModel.DISTANCE;
		else if(model.toLowerCase().equals("degreee"))
			return DisambiguationModel.DEGREE;
		else if(model.toLowerCase().equals("eigenvector"))
			return DisambiguationModel.EIGENVECTOR;
		else if(model.toLowerCase().equals("pagerank"))
			return DisambiguationModel.PAGERANK;
		else if(model.toLowerCase().equals("hitshub"))
			return DisambiguationModel.HITSHUB;
		else 
			return DisambiguationModel.PAGERANK;
	}
	
	public static NLPModel getNLPModel(String model) {
		if(model.toLowerCase().equals("ngram"))
			return NLPModel.NGRAM;
		else if(model.toLowerCase().equals("pos"))
			return NLPModel.POS;
		else if(model.toLowerCase().equals("ner"))
			return NLPModel.NER;
		else 
			return NLPModel.NGRAM;
	}
	
	public static KB getKB(String kb) {
		if(kb.toLowerCase().equals("wikipedia"))
			return KB.WIKIPEDIA;
		else if(kb.toLowerCase().equals("dbpedia"))
			return KB.DBPEDIA;
		else 
			return KB.WIKIPEDIA;
	}
	
}
