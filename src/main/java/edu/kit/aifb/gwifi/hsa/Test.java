package edu.kit.aifb.gwifi.hsa;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {

	public static void main(String[] args) {
		String text = "These sporting events are taking place at 33 venues in the host city as well as "
				+ "at five venues in the cities of "
				+ "[[São Paulo]] (Brazil's largest city), [[Belo Horizonte]], [[Salvador, Bahia|Salvador]], "
				+ "[[Brasília]] (Brazil's capital), and [[Manaus]]. [[test1(test2)]]\n";
		System.out.println(text);
		
	    String text_out = text;
	    
		Matcher m_text = Pattern.compile("((?<=\\u005B\\u005B).*?(?=\\u005D\\u005D))").matcher(text);
		
		while(m_text.find()){
			String markupName = m_text.group();
			String entityName = markupName;
			if (entityName.contains("|"))
				entityName = entityName.substring(0, entityName.indexOf("|"));
			entityName = "_" + entityName.replaceAll("\\s|\\p{P}\\s*", "_");
		//	System.out.println(entityName);
			text_out = text_out.replace("[[" + markupName + "]]", entityName);
		}
		
		System.out.println(text_out);

	}

}
