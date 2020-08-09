package TEST;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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

public class MergeFiles_lines {
	private static ExecutorService executor = Executors.newFixedThreadPool(15);
	private static final String INPUT_FOLDER = "/home/rima/playground/JavaProjectsRun/gwifi/bin/Merged5";
	//private static final String INPUT_FOLDER = "NewData";
	private static final int SIZE = 2;
	private static final int NUMBEROFFILES = 5000;
	private static final String OUTPUT_FOLDER = "Merged"+SIZE+"_Second";
	private final static Map<String, Long> map = new ConcurrentHashMap<>();
	private static final SynchronizedCounter atomic = new SynchronizedCounter();
	public static void main(String[] args) throws InterruptedException {
		System.out.println("Class MergeFiles_lines");
		final long now = System.currentTimeMillis();
		startMerging_parallel();
		FileUtil.writeDataToFile(map, OUTPUT_FOLDER + File.separator + "MergeFiles_output", false);
		System.out.println("Total time minutes " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
	}

	private static void startMerging_parallel() throws InterruptedException {
		int j=0;
		long mainStart = TimeUtil.getStart();
		final File[] listOfFolders = new File(INPUT_FOLDER).listFiles();
		FileUtil.createFolder(OUTPUT_FOLDER);
		Arrays.sort(listOfFolders);
		//int i=0;
		for(File f:listOfFolders) {
			long start = TimeUtil.getStart();
			executor.execute(handle(f,j++));
		}
		System.out.println("main sn "+TimeUtil.getEnd(TimeUnit.SECONDS, mainStart));
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

	}
	private static Runnable handle(File f, int j) {
		return () -> {
			long start = TimeUtil.getStart();
			List<String> lines;
			try {
				lines = Files.readAllLines(Paths.get(f.getPath()), StandardCharsets.UTF_8);
				for(String line :lines) {
					final long value = Long.parseLong(line.substring(line.indexOf("\t\t"), line.length()).trim());
					final String  key =line.substring(0,line.indexOf("\t\t")).trim();
					addToMap(value, key);
				}
				System.out.println("size of the map "+map.size()+" number of files: "+j);
				System.out.println("a file took sn: "+TimeUtil.getEnd(TimeUnit.SECONDS, start));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		};
	}

	private static void addToMap(final long value, final String key) {
		Long long1 = map.get(key);
		if(long1==null) {
			map.put(key, value);
		}else {
			map.put(key, long1+value);	
		}
	}

	private static void handleLines(List<String> lines) {
		//		for(String line :lines) {
		//			final long value = Long.parseLong(line.substring(line.indexOf("\t\t"), line.length()).trim());
		//			final String  key =line.substring(0,line.indexOf("\t\t")).trim();
		//			Long long1 = map.get(key);
		//			if(long1==null) {
		//				map.put(key, value);
		//			}else {
		//				map.put(key, long1+value);	
		//			}
		//		}
	}
	private static void handleFiles(List<File> fileList, int i) {
		long atomicInitial = atomic.value();
		atomic.incrementbyValue(fileList.size());
		long atomicFinal = atomic.value();
		//	final Map<String, Long> localMap = new HashMap<>();
		//System.err.println("Thread "+i+" started with "+fileList.size()+" files");
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
					addToMap(value, key);

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
				System.out.println("main sn "+TimeUtil.getEnd(TimeUnit.SECONDS, mainStart)+" it took sn: "+TimeUtil.getEnd(TimeUnit.SECONDS, start));
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
		//	FileUtil.writeDataToFile(map, OUTPUT_FOLDER + File.separator + atomicInitial + "_"+atomicFinal, false);
		System.out.println("number of files "+atomic.value()+" processed..");

		//new Date().getTime();
	}

}
