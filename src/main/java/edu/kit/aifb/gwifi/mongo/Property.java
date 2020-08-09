package edu.kit.aifb.gwifi.mongo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class Property {

	private static Properties prop = new Properties();

//	static {
//		FileInputStream in;
//		try {
//			// change to relative path
//			in = new FileInputStream("/Users/leizhang/Documents/workspace/asearcher/src/main/resources/configuration.properties");
//			prop.load(in);
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
	public static void setProperties(String configPath) {
		FileInputStream in;
		try {
			in = new FileInputStream(configPath);
			prop.load(in);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String getValue(String key) {
		return prop.getProperty(key);
	}
	
}
