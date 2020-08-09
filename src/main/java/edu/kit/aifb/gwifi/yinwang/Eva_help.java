package edu.kit.aifb.gwifi.yinwang;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
 
public class Eva_help {
     
    public static void main(String[] args) throws Exception {
 
        String inputFolder = "C:/Users/Nekromantik/Desktop/txt/evater/newdata";
        String outfile = "C:/Users/Nekromantik/Desktop/txt/evater/newdata/result1.txt";
         
        Eva_help test = new Eva_help();
        test.generate(inputFolder, outfile);
    }
     
    public void generate(String inputFolder, String outputFile) throws IOException {
        Map<String, Map<String, Integer>> queryWithScoreMap = new LinkedHashMap<String, Map<String, Integer>>();
        File folder = new File(inputFolder);
        File[] files = folder.listFiles();
 
        for(File file : files) {
            load(queryWithScoreMap, file);
        }
        
        
        
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile, false), "UTF-8"));
        for(String query : queryWithScoreMap.keySet()) {
            Map<String, Integer> map = queryWithScoreMap.get(query);
            System.out.println(query + ": " + map.size());
            List<Map.Entry<String, Integer>> scoreLst = sortScore(map);
             
            String output = query + "\n";
            for (int i=0; i<scoreLst.size(); i++) {
                Map.Entry<String, Integer> e = scoreLst.get(i);
                String rEntity = e.getKey();
                int score = e.getValue();
                output += "\t" + rEntity + "^" + score + "\n";
            }
            out.write(output);
        }
        out.close();
    }
     
    private void load(Map<String, Map<String, Integer>> queryWithScoreMap, File file) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
         
        String tmp;
        String query="";
        while ((tmp = reader.readLine()) != null) {
            if (!tmp.startsWith("\t")){
                query = tmp;
            }else{
                 
                String[] result = tmp.substring(1).split("\\^");
                String relatedEntity = result[0];
                 
//              System.out.println(relatedEntity);
//              System.out.println(result[1]);
                 
                int score = Integer.parseInt(result[1]);
 
                Map<String, Integer> entityWithScore;
                if(!queryWithScoreMap.containsKey(query)){
                    entityWithScore = new HashMap<String, Integer>();
                }else{
                    entityWithScore = queryWithScoreMap.get(query);
                }
                entityWithScore.put(relatedEntity, score);
                queryWithScoreMap.put(query, entityWithScore);
            }
        }
        reader.close();
    }
     
    private List<Map.Entry<String, Integer>> sortScore(Map<String, Integer> map) {
        List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
 
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return (o2.getValue().compareTo(o1.getValue()));
            }
        });
        return list;
    }
}
