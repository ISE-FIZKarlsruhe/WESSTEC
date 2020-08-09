package org.fiz.ise.gwifi.embedding.dataset.train;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.util.SynchronizedCounter;
import org.fiz.ise.gwifi.util.TimeUtil;

public class NormalizeLINE_Dataset {
	private static final Logger LOG = Logger.getLogger(DatasetGeneration_EntityCategory_CooccuranceFreq.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger thirdLOG = Logger.getLogger("reportsLogger");
	private static SynchronizedCounter countLine;

	public static void main(String[] args) {
		//max:107742.0     min:1.0
		Long now = TimeUtil.getStart();
		countLine= new SynchronizedCounter();
		System.out.println("Thread started...");
		final Thread t = new Thread (new Runnable() {
			@Override
			public void run() {
				while(true) {
					//					System.out.println("number of article processed "+ countArticle.value()+" minutes "+ TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now)+ " globalSetSize "+globalSet.size()+" globalList "+globalList.size());
					System.out.println("number of lines "+ countLine.value()+" minutes "+ TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
					//System.out.println("number of article processed "+ countArticle.value()+" minutes "+ TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}			
				}
			}
		});
		t.setDaemon(true);
		t.start();
//		String fileName="/home/rima/playground/LINE/linux/Data/entity-category-complex-lei/dataset_dense_model3.txt";
		String fileName="sample_dataset";
		int rangeS=1;
		int rangeB=100;
		double max=107742.0;
		double min=2.0;		
		normalizeDataset(fileName,rangeS,rangeB,min,max);
		//Set<Double> set = new HashSet<>(getAllTheValues(fileName,rangeS,rangeB));
//		Double max =Collections.max(set);
//		Double min =Collections.min(set);
//		System.out.println("it took: "+TimeUtil.getEnd(TimeUnit.SECONDS, now));
//		System.out.println(max+" "+min);
	}
	private static Set<Double> getAllTheValues(String fileName, int rangeS, int rangeB) {
		Set<Double> result = new HashSet<>();
		int count =0;
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			String line;
			while ((line = br.readLine()) != null) {
				int index= line.lastIndexOf("\t");
				double v= Double.valueOf((line.substring(index+1, line.length()).trim()));
				result.add(v);
				count++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Number of lines read: "+count);
		return result;
	}
	//	private static double normalizeAValue(Double d, int rangeS, int rangeB) {
	//		
	//	}
	public static void normalizeDataset(String fileName,int rangeS,int rangeB,double min,double max) {
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			String line;
			while ((line = br.readLine()) != null) {
				int index= line.lastIndexOf("\t");
				String s = line.substring(0, index);
				double v= Double.parseDouble(line.substring(index+1, line.length()).trim());
				//secondLOG.info(s+"\t"+normlizeAValue(v,rangeS,rangeB,min,max));
				System.out.println(s+"\t"+normlizeAValue(v,rangeS,rangeB,min,max));
				countLine.increment();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private static double normlizeAValue(double v,int rangeS,int rangeB,  double min, double max) {
		double n = (v-min)*(rangeB-rangeS);
		double d =max-min;
		return rangeS+(n/d);
	}

}
