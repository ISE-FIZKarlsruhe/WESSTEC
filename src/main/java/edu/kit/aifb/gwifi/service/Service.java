package edu.kit.aifb.gwifi.service;

public interface Service {

	public final static String RESPONSE_TAG = "Response";
	public final static String WIKIFIED_DOC_TAG = "WikifiedDocument";
	public final static String ANNOS_TAG = "Annotations";
	public final static String ANNO_TAG = "Annotation";
	public final static String ANNO_ID_TAG = "id";
	public final static String ANNO_DISPLAYNAME_TAG = "displayName";
	public final static String ANNO_URL_TAG = "URL";
	public final static String ANNO_WEIGHT_TAG = "weight";
	public final static String MENTION_TAG = "mention";
	public final static String MENTION_LABEL_TAG = "label";
	public final static String MENTION_POS_TAG = "position";
	public final static String MENTION_LENGTH_TAG = "length";

	public enum SourceMode {
		AUTO, URL, HTML, WIKI
	};

	public enum LinkFormat {
		AUTO, WIKI, WIKI_ID, WIKI_ID_WEIGHT, HTML, HTML_ID, HTML_ID_WEIGHT
	};

	/**
	 * Options for tagging or ignoring repeat mentions of topics
	 */
	public enum RepeatMode {

		/**
		 * All mentions of a topic will be tagged
		 */
		ALL,

		/**
		 * Only the first mention of a topic will be tagged
		 */
		FIRST,

		/**
		 * Only the first mention within each region (e.g. DIV) will be tagged
		 */
		FIRST_IN_REGION
	};

	public enum ResponseFormat {
		XML, DIRECT
	};

	public enum ResponseMode {
		BEST, ALL
	};

	public enum MentionMode {
		OVERLAPPED, NON_OVERLAPPED
	};

	public enum KB {
		WIKIPEDIA, DBPEDIA
	};

	public enum DisambiguationModel {
		PRIOR, BETWEENESS, DISTANCE, DEGREE, EIGENVECTOR, PAGERANK, HITSHUB, PAGERANK_HITSHUB, PAGERANK_NP, HITSHUB_NP, 
		EIGEN_VECTOR_CENTRALITY, MARKOV_CENTRALITY, KSMARKOV, KSMARKOV_10
	};

	public enum NLPModel {
		POS, NER, NGRAM
	};
	
	public enum LabelMatchModel {
		DIRECT_MATCHER, CASE_FOLDER, CLEANER, PORTER_STEMMER, SIMPLE_STEMMER, TEXT_FOLDER
	};

	public enum EntityMode {
		ONLY_IN_KB, ALL
	}

}
