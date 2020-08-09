package org.fiz.ise.gwifi.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class FileUtil {

	
	public static <T, E> T writeFileAsCSV(Map<T, List<E>> map, final String fileName) {
		try {
			final FileWriter fw = new FileWriter(fileName, false);
			for (Entry<T, List<E>> e : map.entrySet()) {
				fw.write("\""+e.getValue().get(0)+"\",\""+e.getKey()+"\""+"\n");
		    }
			fw.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	    return null;
	}
	
	public static void writeDataToFile(Set<Integer> data,final String fileName) {
		try {
			final FileWriter fw = new FileWriter(fileName, false);
			for(Integer s:data) {
				fw.write(s+"\n");
			}
			fw.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void writeDataToFile(List<String> data,final String file) {
		try {
			final FileWriter fw = new FileWriter(file);
			for(String s:data) {
				fw.write(s+"\n");
			}
			fw.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
	public static void writeDataToFile(List<String> data,final String fileName,final boolean append) {
		try {
			final FileWriter fw = new FileWriter(fileName, append);
			for (String s : data) {
				fw.write(s+"\n");
			}
			fw.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
	public static <K, V> void writeDataToFile(Map<K, V> map,final String fileName) {
		try {
			final FileWriter fw = new FileWriter(fileName, false);
			for (Entry<K, V> s : map.entrySet()) {
				fw.write(s.getKey()+"\t"+s.getValue()+"\n");
			}
			fw.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeDataToFile(Map<String,Long> data,final String fileName,final boolean append) {
		try {
			final FileWriter fw = new FileWriter(fileName, append);
			for (Entry<String, Long> s : data.entrySet()) {
				fw.write(s.getKey()+"\t\t"+s.getValue()+"\n");
			}
			fw.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
	
	

	public static boolean createFolder(String folderName) {
		Path path = Paths.get(folderName);
		if (!Files.exists(path)) {
			try {
				Files.createDirectories(path);
				return true;
			} catch (IOException e) {
				return false;
			}
		}else{
			return false;
		}

	}

	public static void deleteFolder(String folder) {
		File index = new File(folder);
		final String[] entries = index.list();
		if(entries!=null) {
			for(String s: entries){
				File currentFile = new File(index.getPath(),s);
				currentFile.delete();
			}
			final File currentFile = new File(folder);
			currentFile.delete();
		}
	}
}