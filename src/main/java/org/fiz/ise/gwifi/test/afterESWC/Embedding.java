package org.fiz.ise.gwifi.test.afterESWC;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;


public class Embedding {
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	public static void main(String[] args) {
//		String fileName1st = "/home/rima/playground/LINE/linux/Models/entity-category-complex/afterESWC/txt/vec_1st_EntEnt.txt";
		String fileName1st = "/home/rima/playground/LINE/linux/Data/entity-category-complex/afterESWC/dataset_complete_EntEnt_filtered1_08.01.2019.txt";
		String fileName2nd = "/home/rima/playground/LINE/linux/Models/entity-category-complex/afterESWC/txt/vec_2nd_EntEnt.txt";
		int count = 0;
		Map<String, String> map1st = new HashMap<String, String>();
		Map<String, String> map2nd = new HashMap<String, String>();
		Set<String> wrongNumbers = new HashSet<>();
		try (BufferedReader br = new BufferedReader(new FileReader(fileName1st))) {
			System.out.println("Reading "+fileName1st);
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				String[] split = sCurrentLine.split("\t");
				int i = Integer.valueOf(split[0]);
				int i2 = Integer.valueOf(split[1]);
				int i3 = Integer.valueOf(split[2]);
				if (i<1||i2<1||i3<1)
				{
					System.out.println(sCurrentLine);
					wrongNumbers.add(split[0]);
				}
				map1st.put(split[0], sCurrentLine);
			}
			System.out.println("Size of the wrong numbers "+ wrongNumbers.size());
			System.exit(0);

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		
		try (BufferedReader br = new BufferedReader(new FileReader(fileName2nd))) {
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				String[] split = sCurrentLine.split(" ");
				System.out.println(split[0]+" "+split.length);
				map2nd.put(split[0], sCurrentLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("size of the 1st map "+ map1st.size());
		System.out.println("size of the 2nd map "+ map2nd.size());
		count=0;
		
		for(Entry<String,String> e: map1st.entrySet()) {
			if (!map2nd.containsKey(e.getKey())) {
				System.out.println(e.getKey());
			} else {
				count++;
			}
		}
		System.out.println("count: "+count);
		
	}

}
