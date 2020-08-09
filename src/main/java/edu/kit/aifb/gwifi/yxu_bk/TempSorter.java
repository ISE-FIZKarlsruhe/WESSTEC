package edu.kit.aifb.gwifi.yxu_bk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TempSorter {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		File oFile = new File("f:/kit16/...");
		File tFile = new File("f:/kit16/...");
		File hFile = new File("f:/kit16/...");
		
		Map<Double, String> oTable = new HashMap<Double, String>();
		Map<Double, String> hTable = new HashMap<Double, String>();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(oFile));
		    String line;
		    while ((line = br.readLine()) != null) {
		       String[] spLine = line.split("");
		       //
		    }
		    br = new BufferedReader(new FileReader(hFile));
		    while ((line = br.readLine()) != null) {
		       String[] spLine = line.split("");
		       //
		    }
		    
		}catch(IOException e){
			//
		}
		
		for(String s: oTable.values()){
			for(String h: hTable.values()){
				if(s.equals(h)){
				}
			}
		}
		
	}

}
