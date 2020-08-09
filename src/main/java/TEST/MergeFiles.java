package TEST;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.SynchronizedCounter;
import org.fiz.ise.gwifi.util.TimeUtil;

public class MergeFiles {
	private static ExecutorService executor = Executors.newFixedThreadPool(55);
	private static final String INPUT_FOLDER = "/home/rima/playground/JavaProjectsRun/gwifi/bin/Merged5";
	//private static final String INPUT_FOLDER = "NewData";
	private static final int SIZE = 2;
	private static final int NUMBEROFFILES = 5000;
	private static final String OUTPUT_FOLDER = "Merged"+SIZE+"_Second";
	//private final static Map<String, Long> map = new ConcurrentHashMap<>();
	private static final SynchronizedCounter atomic = new SynchronizedCounter();
	public static void main(String[] args) throws InterruptedException {
		final long now = System.currentTimeMillis();
		//startMerging_parallel();
		startMerging();
		System.out.println("Total time minutes " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
	}
	private static void startMerging() {
		System.out.println("Sequential Merging");
		final File[] listOfFolders = new File(INPUT_FOLDER).listFiles();
		FileUtil.createFolder(OUTPUT_FOLDER);
		Arrays.sort(listOfFolders);
		final List<File> files = new ArrayList<>();
		int i=0;
		for(File f:listOfFolders) {
				files.add(f);
				i++;
				if(i%NUMBEROFFILES==0)
				{
					final long now = System.currentTimeMillis();
					handleFiles(files, i);
					files.clear();
					System.out.println(i+" files are processed  "+ TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
				}
		}
		if (files.size()>0) {
			handleFiles(files, i);
			System.out.println(i+" files are processed");
		}
	}
	private static void startMerging_parallel() throws InterruptedException {
		
		final File[] listOfFolders = new File(INPUT_FOLDER).listFiles();
		FileUtil.createFolder(OUTPUT_FOLDER);
		Arrays.sort(listOfFolders);
		final List<File> files = new ArrayList<>();
		int i=1;
		for(File f:listOfFolders) {
			if(files.size()<SIZE) {
				files.add(f);
			}else {				
				executor.execute(handle(new ArrayList<>(files),i++));
				files.clear();
				files.add(f);
			}
		}
		if (files.size()>0) {
			executor.execute(handle(new ArrayList<>(files),i));	
		}
		
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		
	}
	private static Runnable handle(List<File> fileList, int i) {
		return () -> {
			handleFiles(fileList, i);
		};
	}
	private static void handleFiles(List<File> fileList, int i) {
		long atomicInitial = atomic.value();
		atomic.incrementbyValue(fileList.size());
		long atomicFinal = atomic.value();
		final Map<String, Long> map = new HashMap<>();
		int j=0;
		long mainStart = TimeUtil.getStart();
		for(File f:fileList) {
			long start = TimeUtil.getStart();
			try(BufferedReader br = new BufferedReader(new FileReader(f)))
			{
				String line=null;
				while ((line = br.readLine()) != null) 
				{
					final long value = Long.parseLong(line.substring(line.indexOf("\t\t"), line.length()).trim());
					final String  key =line.substring(0,line.indexOf("\t\t")).trim();
					Long long1 = map.get(key);
					if(long1==null) {
						map.put(key, value);
					}else {
						map.put(key, long1+value);	
					}
					
//					final String[] split = line.split("\t\t");
//					final String key = split[0];
//					final long value = Long.parseLong(split[1]);
//					Long long1 = localMap.get(key);
//					if(long1==null) {
//						localMap.put(key, value);
//					}else {
//						localMap.put(key, long1+value);	
//					}
				}
				System.out.println("size of the map "+map.size()+" number of files: "+j++);
				System.out.println("main dk "+(TimeUtil.getEnd(TimeUnit.SECONDS, mainStart)/60)+" it took sn: "+TimeUtil.getEnd(TimeUnit.SECONDS, start));
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
		FileUtil.writeDataToFile(map, OUTPUT_FOLDER + File.separator + atomicInitial + "_"+atomicFinal, false);
		System.out.println("number of files "+atomic.value()+" processed..");
		
		//new Date().getTime();
	}

}
