package edu.kit.aifb.gwifi.mongo;

/**
 * This class stores the connection information of the MongoDB Database and all
 * required collections in our system, including the fields of each collection.
 * 
 */
public final class DBConstants {
	
	/* ======================= Articles ======================= */

	public static final String ARTICLES_COLLECTION = "articles_";
	public static final String ARTICLES_ID = "id";
	public static final String ARTICLES_TOTAL_LINKS_OUT_COUNT = "outlinks";
	public static final String ARTICLES_TOTAL_LINKS_IN_COUNT = "inlinks";
	public static final String ARTICLES_DISTINCT_LINKS_OUT_COUNT = "distinct_outlinks";
	public static final String ARTICLES_DISTINCT_LINKS_IN_COUNT = "distinct_inlinks";
	public static final String ARTICLES_PAGELINKS = "pagelinks";
	public static final String ARTICLES_TEXT = "text";
	public static final String ARTICLES_TITLE = "title";
	public static final String ARTICLES_ABSTRACT = "abstract";
	
	/* ======================= Terms ======================= */

	public static final String TERMS_COLLECTION = "terms_";
	public static final String TERMS_TERM = "term"; 
	public static final String TERMS_IDF = "idf"; 
	public static final String TERMS_VECTOR = "vector";
	
	/* ======================= Langlinks ======================= */

	public static final String LANGLINKS_COLLECTION = "langlinks";
	public static final String LANGLINKS_SOURCE_ID = "s_id";
	public static final String LANGLINKS_SOURCE_ENTITY = "s_entity";
	public static final String LANGLINKS_SOURCE_LANGUAGE = "s_lang";
	public static final String LANGLINKS_TARGET_ID = "t_id";
	public static final String LANGLINKS_TARGET_ENTITY = "t_entity";
	public static final String LANGLINKS_TARGET_LANGUAGE = "t_lang";
	
	/* ======================= EntityIndex ======================= */

	public static final String ENTITY_COLLECTION = "EntityIndex_";
	public static final String ENTITY_GENERALITY = "generality";
	public static final String ENTITY_TOTAL_LINKS_OUT_COUNT = "totalLinksOutCount";
	public static final String ENTITY_TOTAL_LINKS_IN_COUNT = "totalLinksInCount";
	public static final String ENTITY_DISTINCT_LINKS_OUT_COUNT = "distinctLinksOutCount";
	public static final String ENTITY_DISTINCT_LINKS_IN_COUNT = "distinctLinksInCount";
	public static final String ENTITY_ID = "id";
	public static final String ENTITY_NAME = "name";
	public static final String ENTITY_TYPE = "type";
	
	/* ======================= LabelIndex ======================= */

	public static final String LABEL_COLLECTION = "LabelIndex_";
	public static final String LABEL_LINK_OCC_COUNT = "linkOccCount";
	public static final String LABEL_LINK_DOC_COUNT = "linkDocCount";
	public static final String LABEL_OCC_COUNT = "occCount";
	public static final String LABEL_DOC_COUNT = "docCount";
	// the probability that label appears in an article as an anchor text
	public static final String LABEL_PROBABILITY = "probability";
	public static final String LABEL_TEXT = "label";
	public static final String LABEL_NORM_TEXT = "nLabel";
	public static final String LABEL_PINYIN = "pinyin";
	public static final String LABEL_SOURCE = "source";
	
	/* ======================= LabelEntityIndex ======================= */

	public static final String LABEL_ENTITY_COLLECTION = "LabelEntityIndex_";
	public static final String LABEL_ENTITY_SENSE_LINK_OCC_COUNT = "slinkOccCount";
	public static final String LABEL_ENTITY_SENSE_LINK_DOC_COUNT = "slinkDocCount";
	// association strength between label and entity
	public static final String LABEL_ENTITY_ASSOCIATION_STRENGTH = "associationStrength";
	// the probability that this label is used as a link in Wikipedia
	public static final String LABEL_ENTITY_WEIGHT = "weight";
	public static final String LABEL_ENTITY_SOURCE_ENTITY_ID = "s_id";
	public static final String LABEL_ENTITY_SOURCE_ENTITY_NAME = "s_name";
	public static final String LABEL_ENTITY_TARGET_ENTITY_ID = "t_id";
	public static final String LABEL_ENTITY_TARGET_ENTITY_NAME = "t_name";
	public static final String LABEL_ENTITY_ENTITY_TYPE = "type";
	public static final String LABEL_ENTITY_LABEL_TEXT = "label";
	public static final String LABEL_ENTITY_NORM_LABEL_TEXT = "nLabel";
	public static final String LABEL_ENTITY_LABEL_PINYIN = "pinyin";
	public static final String LABEL_ENTITY_SOURCE = "source";

	/* ======================= CategoryIndex ======================= */
	
	public static final String CATEGORY_COLLECTION = "CategoryIndex_";
	public static final String CATEGORY_ID = "categoryID";
	public static final String CATEGORY_NAME = "category";
	public static final String CATEGROY_DEPTH = "categoryDepth";
	public static final String CATEGROY_GENERALITY = "categoryGenerality";
	public static final String CATEGROY_IS_LEAF = "categoryIsLeaf";
	public static final String CATEGROY_ENTITY_COUNT = "categoryEntityCount";
	public static final String CATEGORY_LC_NAME = "lcCategory";

	/* ======================= CategoryNameIndex ======================= */
	public static final String CATEGORY_NAME_COLLECTION = "LcCategoryNameIndex_";
	public static final String CATEGORY_NAME_LC_NAME = "lcCategoryName";
	
	/* ======================= CategoryEntityIndex ======================= */
	
	public static final String CATEGORY_ENTITY_COLLECTION = "CategoryEntityIndex_";
	public static final String CATEGORY_ENTITY_ENTITY_NAME = "entity";
	public static final String CATEGORY_ENTITY_CATEGORY_NAME = "category";
	public static final String CATEGORY_ENTITY_DISTANCE = "distance";

	/* ======================= ResourceRelatednessIndex ======================= */

	public static final String RESOURCERELATEDNESS_COLLECTION = "ResourceRelatednessIndex_";
	public static final String RESOURCERELATEDNESS_RESOURCE_LANG = "lang";
	public static final String RESOURCERELATEDNESS_SOURCE_ID = "s_id";
	public static final String RESOURCERELATEDNESS_SOURCE_ENTITY_NAME = "s_entity";
	public static final String RESOURCERELATEDNESS_SOURCE_LANGUAGE = "s_lang";
	public static final String RESOURCERELATEDNESS_TARGET_ID = "t_id";
	public static final String RESOURCERELATEDNESS_TARGET_ENTITY_NAME = "t_entity";
	public static final String RESOURCERELATEDNESS_SCORE = "score";
	public static final String RESOURCERELATEDNESS_LINK_OUT = "link_out";
	
	/* ======================= Source Field Values ======================= */
	
	public static final String SOURCE_WIKI_TITLE = "wikiTitle";
	public static final String SOURCE_WIKI_DISAMBIGUATION = "wikiDisambiguation";
	public static final String SOURCE_WIKI_LABEL = "wikiLabel";
	public static final String SOURCE_BAIDU = "baiduBaike";
	public static final String SOURCE_GOOGLE_SEARCH = "googleSearch";
	
	/* ======================= EntityEmbeddingIndex ======================= */

	public static final String ENTITY_EMBEDDING_COLLECTION = "EntityEmbeddingIndex_";
	public static final String ENTITY_EMBEDDING_NAME = "name";
	public static final String ENTITY_EMBEDDING_VECTOR = "vector";
	
	/* ======================= CategoryEmbeddingIndex ======================= */

	public static final String CATEGORY_EMBEDDING_COLLECTION = "CategoryEmbeddingIndex_";
	public static final String CATEGORY_EMBEDDING_NAME = "name";
	public static final String CATEGORY_EMBEDDING_VECTOR = "vector";

}
