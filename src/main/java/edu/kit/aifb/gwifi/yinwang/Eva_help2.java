package edu.kit.aifb.gwifi.yinwang;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.stanford.nlp.util.IntPair;
import java_cup.action_part;

public class Eva_help2 {
	
	//input
	//1.query 2.entity
	//search in many txt; if found get the score, else return 0. put the scores in a Array
	//output
	
	public static void main(String[] args)throws IOException {
		
		 String inputFolder = "C:/Users/Nekromantik/Desktop/321";
		 String inputfile = "C:/Users/Nekromantik/Desktop/222/disagreements.txt";
		 
		 String outputfile = "C:/Users/Nekromantik/Desktop/222/result.txt";
		 
		 Eva_help2 test = new Eva_help2();
		 test.generate(inputfile, inputFolder, outputfile);
		
	}

	private void generate(String inputfile, String inputFolder, String outputfile)throws IOException {
		 
		Map<String, Map<String, ArrayList<Integer>>> queryWithScoreMap = new LinkedHashMap<String, Map<String, ArrayList<Integer>>>();
		File ofile = new File(inputfile);
		load(queryWithScoreMap, ofile);
		
		
		File folder = new File(inputFolder);
	    File[] files = folder.listFiles();
	 
	    int filename = 0;
	        for(File file : files) {
	        	String name = file.getName();
	        	name = name.substring(0, name.length() - 4);
	        	
	        	filename = Integer.parseInt(name)-1;
	        	addScore(queryWithScoreMap, file, filename);
	        }
	        
	    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputfile, false), "UTF-8"));
	    for(String query : queryWithScoreMap.keySet()) {
	           Map<String, ArrayList<Integer>> map = queryWithScoreMap.get(query);
	           
	        
	        for(String entity : map.keySet())
	        {
	        	String output = query+ "\n" + "\t" + entity + "\t" + "[";
	        	for(int i=0; i < map.get(entity).size(); i ++)
	        	{	
	        		output += map.get(entity).get(i) + " ";
	        	}
	        	output += "]"+"\n";
	        	out.write(output);
	        }
	    }
	        out.close();
		
	}

	private void load(Map<String, Map<String, ArrayList<Integer>>> queryWithScoreMap, File file)throws IOException {
		 BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
         
	        String tmp;
	        String query="";
	        while ((tmp = reader.readLine()) != null) {
	            if (!tmp.startsWith("\t")){
	                query = tmp;
	            }else{
	                 
	                String[] result = tmp.substring(1).split("\\^");
	                String relatedEntity = result[0];
	                 
//	              System.out.println(relatedEntity);
//	              System.out.println(result[1]);
	                 
	                ArrayList<Integer> a = new ArrayList<Integer>();
	                for(int i = 0; i < 11; i++)
	                {
	                	a.add(Integer.valueOf(0));
	                }
	 
	                Map<String, ArrayList<Integer>> entityWithScore;
	                if(!queryWithScoreMap.containsKey(query)){
	                    entityWithScore = new HashMap<String, ArrayList<Integer>>();
	                }else{
	                    entityWithScore = queryWithScoreMap.get(query);
	                }
	                entityWithScore.put(relatedEntity, a);
	                queryWithScoreMap.put(query, entityWithScore);
	            }
	        }
	        
	        reader.close();		
	}
	
	private void addScore(Map<String, Map<String, ArrayList<Integer>>> queryWithScoreMap, File file, int filename)throws IOException
	{
		 BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
         
	        String tmp;
	        String query="";
	        while ((tmp = reader.readLine()) != null) {
	           
	        	
	        	if (!tmp.startsWith("\t")){
	                query = tmp;
	            }else{
	                 
	                String[] result = tmp.substring(1).split("\\^");
	                String relatedEntity = result[0];
	                 
	                 
	                int score = Integer.parseInt(result[1]);
	                
	 
	                Map<String, ArrayList<Integer>> entityWithScore = new HashMap<>();
	                
	                if(!queryWithScoreMap.containsKey(query)){
	                   continue;
	                }else{
	                    entityWithScore = queryWithScoreMap.get(query);
	                    
	                    if(entityWithScore.containsKey(relatedEntity))
	                    {
	                    	entityWithScore.get(relatedEntity).set(filename, Integer.valueOf(score));
	                    	
	                    }
	                    
	                }
	   
	            }
	        }
	        
	        reader.close();
	}
	
}
