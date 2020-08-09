package edu.kit.aifb.gwifi.annotation.preprocessing;

import java.util.ArrayList;
import java.util.HashSet;

import edu.kit.aifb.gwifi.annotation.preprocessing.PreprocessedDocument.RegionTag;

public class DummyPreprocessor extends DocumentPreprocessor {
	
	public DummyPreprocessor() {
		super(null, null, null) ;
	}

	public PreprocessedDocument preprocess(String content) {

		StringBuffer context = new StringBuffer() ;
		ArrayList<RegionTag> regionTags = getRegionTags(content) ;
		HashSet<Integer> bannedTopics = new HashSet<Integer>() ;
		
		return new PreprocessedDocument(content, content, context.toString(), regionTags, bannedTopics) ;
	}

}
