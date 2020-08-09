package edu.kit.aifb.gwifi.textcategorization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReadCategoryFile {
	private String categoryFile ="/home/ls3data/users/lzh/congliu/mapping_en_dep2.csv.dic";
	//private String categoryFile = "/Users/aifb-ls3/MasterThesis/wekaExpenlabel.csv.dic";
	public String[] id;
	public String[] name;
	public int[] id2int;
	public ReadCategoryFile() throws IOException{
		   readCategoryFile(categoryFile);
		   
	   }
	
	public void readCategoryFile(String cateogryFileName) throws IOException{
		List<String> cateID = new ArrayList<String>();
		List<String> cateName = new ArrayList<String>();
		File file = new File(cateogryFileName);
		BufferedReader bf = new BufferedReader(new FileReader(file));
		String line = null;
		while ((line = bf.readLine()) != null) {
            String parts[] = line.split(",");
            cateID.add(parts[0]);
            cateName.add(parts[1]);
            
        }
        bf.close();
        id =cateID.toArray(new String [cateID.size()]);
        name = cateName.toArray(new String[cateName.size()]);
        id2int = new int[id.length];
        for(int i=0;i< id.length;i++){
        	id2int[i] = Integer.parseInt(id[i]);
        }
        }
	public int [] getCateid2int(){
		return id2int;
	}
	public String [] getCatename(){
		return name;
	}
	public String[] getcateID(){
		return id;
	}
	
	public void printall(){
		for(int i=0; i< name.length;i++){
			System.out.print(name[i]+" ");
		}
		System.out.print("\r\n");
		for(int i=0; i< id.length;i++){
			System.out.print(id[i]+" ");
		}
		System.out.print("\r\n");
		for(int i=0; i< id2int.length;i++){
			System.out.print(id2int[i]+ " ");
		}
	}
	public static void main(String[] args) throws IOException{
		ReadCategoryFile f= new ReadCategoryFile();
		f.printall();
	}
}
