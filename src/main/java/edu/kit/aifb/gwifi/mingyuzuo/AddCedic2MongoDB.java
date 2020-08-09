package edu.kit.aifb.gwifi.mingyuzuo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class AddCedic2MongoDB extends Source2014
{
	private String lc;
	
	public AddCedic2MongoDB(String folder) throws Exception
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
				String[] a = sCurrentLine.split("/"); // a[0]是中文label，a[1]-a[n]是英文label

				String[] b = a[0].split(" "); // b[0] 繁体， b[1] 简体

				b[0] = b[0].replace(".", "_");
				b[1] = b[1].replace(".", "_");

				String[] c = b[0].split("_");
				String[] d = b[1].split("_");

				// 去掉小括号及小括号内部所有内容
				for (int i = 1; i < a.length; i++)
				{
					a[i] = a[i].replaceAll("\\(.*?\\)", "");
					a[i] = a[i].trim();	
					lc = a[i].toLowerCase();
					this.saveDictionary2Mongodb(b[0], a[i], lc);
					this.saveDictionary2Mongodb(b[1], a[i], lc);
				}

				for (int i = 0; i < c.length; i++)
				{
					for (int j = 1; j < a.length; j++)
					{
						lc = a[j].toLowerCase();
						this.saveDictionary2Mongodb(c[i], a[j], lc);
						this.saveDictionary2Mongodb(d[i], a[j], lc);
					}
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

	public void saveDictionary2Mongodb(String zhLabel, String enLable, String lc)
	{
		DBObject insertData = new BasicDBObject();
		insertData.put("zhlabel", zhLabel);
		insertData.put("enlabel", enLable);
		insertData.put("lcenlabel", lc);
		insertData.put("source", "cedict_ts");
		newDictionary.insert(insertData);
	}

	public static void main(String[] args)
	{
		AddCedic2MongoDB a = null;
		try
		{
			a = new AddCedic2MongoDB("");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		a.extractData("/home/zmy/Buffer/CEDICT/CEDICT/cedict_ts.u8");

		System.out.println("DONE!");
	}

}
