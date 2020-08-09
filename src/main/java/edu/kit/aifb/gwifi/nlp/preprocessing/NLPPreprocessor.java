package edu.kit.aifb.gwifi.nlp.preprocessing;

import java.util.LinkedHashMap;
import java.util.List;

import edu.kit.aifb.gwifi.util.Position;

public interface NLPPreprocessor {
	
	public LinkedHashMap<String, List<Position>> NEREntityAndPositions(String input);
	
	public LinkedHashMap<Position, String> NERPositionAndType(String input);
	
	public List<Position> NERPosition(String input);

	public String POSTagging(String input); 
	
	public LinkedHashMap<Position, String> POSTaggingPositionAndTag(String input);

	public String segmentation(String input);
	
	public List<Position> segmentationPosition(String input);
	
}
