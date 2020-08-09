package edu.kit.aifb.gwifi.mingyuzuo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Properties;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import edu.kit.aifb.gwifi.mongo.Property;

public class DBBuilder
{
	private static final String configPath = "/home/zmy/workspace/gwifi/configs/MongoDBConfig.properties";
	public static MongoClient client_remus;
	public static MongoClient client_maia;
	public static DB db;
	
	public static DB getDB(){
	
		Properties prop = new Properties();
		
		try
		{
			prop.load(new FileInputStream(configPath));
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		try
		{
			String value = prop.getProperty("mongodb_url");
			MongoClientURI uri = new MongoClientURI(value);
			client_remus = new MongoClient(uri);
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}
		db = client_remus.getDB(prop.getProperty("mongodb_name"));
		
		return db;
	}
	
	public static DB getDB(String dbname){
		
		Properties prop = new Properties();
		
		try
		{
			prop.load(new FileInputStream(configPath));
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		try
		{
			String value = prop.getProperty("mongodb_url_maia");
			MongoClientURI uri = new MongoClientURI(value);
			client_maia = new MongoClient(uri);
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}
		db = client_maia.getDB(prop.getProperty("mongodb_name_KBP"));
		
		return db;
	}
	
}
