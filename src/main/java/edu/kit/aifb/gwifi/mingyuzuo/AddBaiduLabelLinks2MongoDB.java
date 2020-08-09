package edu.kit.aifb.gwifi.mingyuzuo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class AddBaiduLabelLinks2MongoDB extends Source2014
{
	public AddBaiduLabelLinks2MongoDB(String folder) throws Exception
	{
		super(folder);
	}

	public void extractData(String Dictionarydir)
	{

		BufferedReader br = null;

		try
		{

			String sCurrentLine;

			br = new BufferedReader(new FileReader(Dictionarydir));

			while ((sCurrentLine = br.readLine()) != null)
			{
				String[] a = sCurrentLine.split("\t");
				
				a[0] = a[0].replace("・", "_");
				a[0] = a[0].replace("-", "_");
				a[0] = a[0].replace(":", "_");
				a[0] = a[0].replace("―", "_");
				a[0] = a[0].replace("�", "_");
				a[0] = a[0].replace("《", "_");
				a[0] = a[0].replace("》", "_");
				a[0] = a[0].replace("(", "_");
				a[0] = a[0].replace(")", "_");
				a[0] = a[0].replace(" ", "_");
				a[0] = a[0].replace("［", "_");
				a[0] = a[0].replace("］", "_");
				a[0] = a[0].replace("!", "_");
				a[0] = a[0].replace("“", "_");

				a[0] = a[0].trim();				

				String[] b = a[0].split("_");
				
				String lc = a[0].toLowerCase();
				
				String[] c = lc.split("_");

				
				this.saveDictionary2Mongodb(a[0], lc, a[1]);
				for (int i = 0; i < b.length; i++)
				{
					this.saveDictionary2Mongodb(b[i], c[i], a[1]);
				}
			}

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (br != null)
					br.close();
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
		}

	}

	public void saveDictionary2Mongodb(String zhWord, String lczhWord, String enWord)
	{
		DBObject insertData = new BasicDBObject();
		insertData.put("label", zhWord);
		insertData.put("lclabel", lczhWord);
		insertData.put("entity", enWord);
		insertData.put("source", "Baidu");
		baidu.insert(insertData);
	}
	
	public static void main(String[] args)
	{
		AddBaiduLabelLinks2MongoDB a = null;
		try
		{
			a = new AddBaiduLabelLinks2MongoDB("");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		a.extractData("/home/zmy/Buffer/zh-en_links.dat/zh-en_links.dat");

		System.out.println("DONE!");
	}

}
