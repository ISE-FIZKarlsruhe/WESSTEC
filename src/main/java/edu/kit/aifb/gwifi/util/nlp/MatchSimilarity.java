package edu.kit.aifb.gwifi.util.nlp;

import org.apache.commons.lang3.StringUtils;

public class MatchSimilarity {
	
	public static void main(String[] args) {
		
		String s1 = "泰森";
		String s2 = "麦克·泰森";
		int dis = StringUtils.getLevenshteinDistance(s1, s2);
		double sim = 1 - ((double) dis) / (Math.max(s1.length(), s2.length()));
		
		System.out.println(dis);
		System.out.println(sim);
	}
	

}
