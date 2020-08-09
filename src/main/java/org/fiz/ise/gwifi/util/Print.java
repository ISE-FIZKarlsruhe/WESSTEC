package org.fiz.ise.gwifi.util;

import java.util.Map;
import java.util.Set;

import edu.kit.aifb.gwifi.model.Category;

public class Print 
{
	
	public static<K, V> void printMap (Map<K, V> hm) {
		hm.forEach((k,v) -> System.out.println("key:\t"+k+"\tvalue:\t"+v));
	}
	
	public static void printMapSize(Map<Category, Set<Category>> map) {
		map.forEach((k, v) -> System.out.println("key: "+k+" value:"+v.size()));
	}
	
//	void printMap(Map<K, V>)
//	{
//		for (TypeKey name: example.keySet()){
//
//            String key =name.toString();
//            String value = example.get(name).toString();  
//            System.out.println(key + " " + value);  
//
//
//		} 
//	}
}
