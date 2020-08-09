package org.fiz.ise.gwifi.test.afterESWC;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.deeplearning4j.parallelism.ConcurrentHashSet;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.ReadDataset;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.SynchronizedCounter;
import org.fiz.ise.gwifi.util.TimeUtil;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;

public class GenerateWideFeatureSetWithKB {
	private static final String DATASET_TEST_AG = Config.getString("DATASET_TEST_AG","");
	private static final String DATASET_TRAIN_AG = Config.getString("DATASET_TRAIN_AG","");
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");
	private static ExecutorService executor;
	private final static Integer NUMBER_OF_THREADS= Config.getInt("NUMBER_OF_THREADS",-1);
	//static Map<String, Integer> mapAllThePairs = new ConcurrentHashMap<String, Integer>();
	static Map<String, Integer> mapAllEntitiesandIndex = new ConcurrentHashMap<String, Integer>();
	static Map<String, Float> mapEntitySim = new ConcurrentHashMap<String, Float>();
	static Map<Integer, Article[]> mapOuterLinks = new ConcurrentHashMap<Integer, Article[]>();
	static Set<Integer> setAllEntities = new ConcurrentHashSet<Integer>();
	private static SynchronizedCounter synCountNumberOfPossibleDimentions;
	static Map<Integer, Integer> mapPossibleEntAndFreq = new ConcurrentHashMap<Integer, Integer>();

	public static void main(String[] args) throws Exception {
		GenerateWideFeatureSetWithKB featureSet = new GenerateWideFeatureSetWithKB();
		Long start =TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()) ;
		LINE_modelSingleton.getInstance();
		WikipediaSingleton.getInstance();
		AnnotationSingleton.getInstance();
		synCountNumberOfPossibleDimentions= new SynchronizedCounter();
		List<String> lstTrainDataset = new ArrayList<String>(ReadDataset.read_AG_BasedOnType(DATASET_TRAIN_AG, AG_DataType.TITLEANDDESCRIPTION));
		List<String> lstTestDataset = new ArrayList<String>(ReadDataset.read_AG_BasedOnType(DATASET_TEST_AG, AG_DataType.TITLEANDDESCRIPTION));

//		List<String> lstCombined = new ArrayList<String>(lstTrainDataset);
//		lstCombined.addAll(lstTestDataset);
		//featureSet.possibleDimentionsWithKB(lstCombined);

		//featureSet.possibleEntsandFreqWithKB(lstCombined);
		mapAllEntitiesandIndex=GenerateWideFeatureSet.readDataFromFileForIndexingMap("/home/rima/playground/DNNs/WideFeatures/AllEntAndConnectedEntitiesKBWithFrequency_filteredNoise_sorted.txt","\t");

		System.out.println("Size of the map-all the possible entities: "+mapAllEntitiesandIndex.size());
		System.out.println("Call find features...");

		featureSet.generateWideFeatureEntsandFreqWithKB(lstTrainDataset);
		System.out.println("Seconds time : "+TimeUtil.getEnd(TimeUnit.MINUTES, start));
		System.out.println("Filtered 100");

	}
	private void generateWideFeatureEntsandFreqWithKB(List<String> lstTrainDataset) throws InterruptedException {
		executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);		
		for (int i = 0; i < lstTrainDataset.size(); i++) {
			executor.execute(handlegenerateWideFeatureEntsandFreqWithKB(lstTrainDataset.get(i),i));
		}
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		System.out.println("Size of the map-all the possible entities: "+mapAllEntitiesandIndex.size());
		System.out.println("Started to write to a file...");

	}

	public void test() throws Exception {
		String str="Quality Gets Swept Away Quality Distribution is hammered after reporting a large loss for the second quarter.";
		List<Annotation> lstAnnotations = new ArrayList<>();
		AnnotationSingleton.getInstance().service.annotate(str, lstAnnotations);
		System.out.println(lstAnnotations);
		System.out.println("Size of the initial annotation: " +lstAnnotations.size());
		int totalLink=0;
		for (Annotation s : lstAnnotations) {
			if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(s.getId())&&WikipediaSingleton.getInstance().getArticle(s.getTitle())!=null) {

				System.out.println(s.getTitle()+" started");
				Article[] linksOut;
				if (mapOuterLinks.containsKey(s.getId())) {
					linksOut = mapOuterLinks.get(s.getId());
				}
				else {
					linksOut = WikipediaSingleton.getInstance().wikipedia.getArticleById(s.getId()).getLinksOut();
					mapOuterLinks.put(s.getId(), linksOut);
				}
				System.out.println("Count links out: "+linksOut.length);
				totalLink+=linksOut.length;

			}
		}
		
		System.out.println("Total Link "+totalLink);
	}

	private Runnable handlegenerateWideFeatureEntsandFreqWithKB_python(String str,int count)  {
		return () -> {
			List<Annotation> lstAnnotations = new ArrayList<>();
			try {
				AnnotationSingleton.getInstance().service.annotate(str, lstAnnotations);
				StringBuilder result = new StringBuilder(str+"\t\t");
				for (Annotation s : lstAnnotations) {
					if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(s.getId())&&WikipediaSingleton.getInstance().getArticle(s.getTitle())!=null
							&&mapAllEntitiesandIndex.containsKey(String.valueOf(s.getId()))) {
						Article[] linksOut;
						if (mapOuterLinks.containsKey(s.getId())) {
							linksOut = mapOuterLinks.get(s.getId());
						}
						else {
							linksOut = WikipediaSingleton.getInstance().wikipedia.getArticleById(s.getId()).getLinksOut();
							mapOuterLinks.put(s.getId(), linksOut);
						}
						if (linksOut!=null) {
							int aId=s.getId();
							for (int i = 0; i < linksOut.length; i++) {
								String id=String.valueOf(linksOut[i].getId());
								if (mapAllEntitiesandIndex.containsKey(id)&&LINE_modelSingleton.getInstance().lineModel.hasWord(id)) {
									int index=mapAllEntitiesandIndex.get(id);
									result.append(index+"\t"+LINE_modelSingleton.getInstance().lineModel.similarity(id,String.valueOf(aId))+",");
								}
							}
						}
						int index=mapAllEntitiesandIndex.get(String.valueOf(s.getId()));
						result.append(index+"\t"+1.0+",");
					}
				}
				resultLog.info(result.toString().substring(0, result.toString().length() - 1));
				System.out.println("#sentences processed:"+ count+ " dimention:"+mapAllEntitiesandIndex.size()+" size mapOuterLinks "+mapOuterLinks.size());
			} catch (Exception e) {
				System.out.println("Exception could not create the feature");
				System.out.println(e.getMessage());
				e.printStackTrace();
				System.exit(1);
			}//annotate the given text
		};
	}
	private Runnable handlegenerateWideFeatureEntsandFreqWithKB(String str,int count)  {
		return () -> {
//			Long startMain =TimeUtil.getStart() ;
			List<Annotation> lstAnnotations = new ArrayList<>();
			try {
//				long start =TimeUtil.getStart() ;
				AnnotationSingleton.getInstance().service.annotate(str, lstAnnotations);
				StringBuilder result = new StringBuilder(str+"\t");
				double[] arr = new double[mapAllEntitiesandIndex.size()];
				for (Annotation s : lstAnnotations) {
					if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(s.getId())&&WikipediaSingleton.getInstance().getArticle(s.getTitle())!=null
							&&mapAllEntitiesandIndex.containsKey(String.valueOf(s.getId()))) {
						
						Article[] linksOut;
						if (mapOuterLinks.containsKey(s.getId())) {
							linksOut = mapOuterLinks.get(s.getId());
						}
						else {
							linksOut = WikipediaSingleton.getInstance().wikipedia.getArticleById(s.getId()).getLinksOut();
							mapOuterLinks.put(s.getId(), linksOut);
						}
						int aId=s.getId();
						
						if (linksOut!=null) {
							for (int i = 0; i < linksOut.length; i++) {
								String id=String.valueOf(linksOut[i].getId());
								if (mapAllEntitiesandIndex.containsKey(id)&&LINE_modelSingleton.getInstance().lineModel.hasWord(id)) {
									int index=mapAllEntitiesandIndex.get(id);
									arr[index]=LINE_modelSingleton.getInstance().lineModel.similarity(id,String.valueOf(aId));
								}
							}
						}
						int index=mapAllEntitiesandIndex.get(String.valueOf(aId));
						arr[index]=1.0;
					}
				}
//				System.out.println("After annotation : "+TimeUtil.getEnd(TimeUnit.MILLISECONDS, start));
//				
//				long start2 =TimeUtil.getStart();
//				System.err.println("arr.length: "+arr.length);
//				System.err.println("Result: "+result);
				for (int i = 0; i < arr.length; i++) {
					result.append(arr[i]).append(",");
				}
//				System.out.println("storeFC : "+TimeUtil.getEnd(TimeUnit.MILLISECONDS, start2));
//				
//				//storeFC(arr);
//
//				long start3 =TimeUtil.getStart() ;
				resultLog.info(result.toString().subSequence(0, result.toString().length()-1));
//				System.out.println("write log : "+TimeUtil.getEnd(TimeUnit.MILLISECONDS, start3));
				System.out.println("#sentences processed:"+ count+ " dimention:"+mapAllEntitiesandIndex.size());
//				System.out.println("Total Milisec time : "+TimeUtil.getEnd(TimeUnit.MILLISECONDS, startMain));
			
			} catch (Exception e) {
				System.out.println("Exception could not create the feature");
				System.out.println(e.getMessage());
				e.printStackTrace();
				System.exit(1);
			}//annotate the given text
		};
	}
	
	private void storeFC(double[] ints) {
		  FileOutputStream out = null;
		  try {
		    out = new FileOutputStream("fc.out");
		    FileChannel file = out.getChannel();
		    ByteBuffer buf = file.map(FileChannel.MapMode.READ_WRITE, 0, 8 * ints.length);
		    for (double i : ints) {
		      buf.putDouble(i);
		    }
		    file.close();
		  } catch (IOException e) {
		    throw new RuntimeException(e);
		  } finally {
		    safeClose(out);
		  }
		}
	
	private void safeClose(OutputStream out) {
		  try {
		    if (out != null) {
		      out.close();
		    }
		  } catch (IOException e) {
		    // do nothing
		  }
		}
	private Runnable handlegenerateWideFeatureEntsandFreqWithKB_(String str,int count)  {
		return () -> {
			List<Annotation> lstAnnotations = new ArrayList<>();
			try {
				AnnotationSingleton.getInstance().service.annotate(str, lstAnnotations);
				StringBuilder result = new StringBuilder(str+"\t");

				for (Annotation s : lstAnnotations) {
					if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(s.getId())&&WikipediaSingleton.getInstance().getArticle(s.getTitle())!=null
							&&mapAllEntitiesandIndex.containsKey(String.valueOf(s.getId()))) {

						Article[] linksOut;
						if (mapOuterLinks.containsKey(s.getId())) {
							linksOut = mapOuterLinks.get(s.getId());
						}
						else {
							linksOut = WikipediaSingleton.getInstance().wikipedia.getArticleById(s.getId()).getLinksOut();
							mapOuterLinks.put(s.getId(), linksOut);
						}
						
						if (linksOut!=null) {
							int aId=s.getId();
							double[] arr = new double[mapAllEntitiesandIndex.size()];
							for (int i = 0; i < linksOut.length; i++) {
								String id=String.valueOf(linksOut[i].getId());
								if (mapAllEntitiesandIndex.containsKey(id)) {
									int index=mapAllEntitiesandIndex.get(id);
									arr[index]=LINE_modelSingleton.getInstance().lineModel.similarity(id,String.valueOf(aId));
								}
							}
							int index=mapAllEntitiesandIndex.get(String.valueOf(aId));
							arr[index]=1.0;

							//System.out.println(s.getId()+" "+arr.length);

							//							for (Integer i : setEntityIndex) {
							//								int id=
							//								arr[i]=LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(mapAllEntitiesandIndex.get("index"+String.valueOf(i))), String.valueOf(s.getId()));
							//							}
							for (int i = 0; i < arr.length; i++) {
								result.append(arr[i]+",");
							}


							//							Set<Integer> setEntityIndex = new HashSet<Integer>();
							//							setEntityIndex.add(mapAllEntitiesandIndex.get(String.valueOf(s.getId())));
							//							
							//							for (int i = 0; i < linksOut.length; i++) {
							//								setEntityIndex.add(mapAllEntitiesandIndex.get(String.valueOf(linksOut[i].getId())));
							//							}
							//							
							//							System.out.println(s.getId()+" "+setEntityIndex);
							//							
							//							double[] arr = new double[mapAllEntitiesandIndex.size()];
							//							
							//							System.out.println(s.getId()+" "+arr.length);
							//							
							//							for (Integer i : setEntityIndex) {
							//								int id=
							//								arr[i]=LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(mapAllEntitiesandIndex.get("index"+String.valueOf(i))), String.valueOf(s.getId()));
							//							}
							//							for (int i = 0; i < arr.length; i++) {
							//								result.append(arr[i]+",");
							//							}
							//							



							//							System.out.println("Number of index: " + setEntityIndex.size()+
							//									" size map: "+mapAllEntities.size()+" annotation ID: "+s.getId()+ " links out: "+linksOut.length);
							//							System.out.println("set "+setEntityIndex);

							//							for (int j = 0; j < mapAllEntities.size(); j++) {
							//								if (setEntityIndex.contains(j)) {
							//									result.append(LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(mapAllEntities.get(String.valueOf(j))), String.valueOf(s.getId()))+",");
							//								}
							//								else {
							//									result.append(0+",");
							//								}
							//							}

						}
					}
				}
				resultLog.info(result.toString().subSequence(0, result.toString().length()-1));
				System.out.println("#sentences processed:"+ count+ " dimention:"+mapAllEntitiesandIndex.size());
			} catch (Exception e) {
				System.out.println("Exception could not create the feature");
				System.out.println(e.getMessage());
				e.printStackTrace();
				System.exit(1);
			}//annotate the given text
		};
	}

	private void possibleEntsandFreqWithKB(List<String> lstTrainDataset) throws InterruptedException {
		executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);		
		for (int i = 0; i < lstTrainDataset.size(); i++) {
			executor.execute(handlePossibleEntsandFreqWithKB(lstTrainDataset.get(i),i));
		}
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		System.out.println("Started to write to a file...");
		Map<Integer, Integer> sortByValueDescending = MapUtil.sortByValueDescending(mapPossibleEntAndFreq);
		for (Entry<Integer, Integer> e : sortByValueDescending.entrySet()) {
			resultLog.info(e.getKey()+"\t"+e.getValue());
		}
	}


	private Runnable handlePossibleEntsandFreqWithKB(String str,int count)  {
		return () -> {
			List<Annotation> lstAnnotations = new ArrayList<>();
			try {
				AnnotationSingleton.getInstance().service.annotate(str, lstAnnotations);
				for (Annotation s : lstAnnotations) {
					if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(s.getId())&&WikipediaSingleton.getInstance().getArticle(s.getTitle())!=null) {
						Article[] linksOut = WikipediaSingleton.getInstance().wikipedia.getArticleById(s.getId()).getLinksOut();
						if (linksOut!=null) {
							for (int i = 0; i < linksOut.length; i++) {
								int id=linksOut[i].getId();
								mapPossibleEntAndFreq.merge(id, 1, Integer::sum);
							}
						}
					}
				}
				System.out.println("processed: "+count);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}//annotate the given text
		};
	}

	public void possibleDimentionsandFreqWithKB(List<String> lst) {
		Long start =TimeUtil.getStart() ;
		List<Annotation> findAnnotationAll_FilterAG = AnnonatationUtil.findAnnotationAll_FilterAG(lst);
		System.out.println("Time took for annotation:"+ TimeUtil.getEnd(TimeUnit.MICROSECONDS, start));
		Set<Article> setPossibleEntities = new HashSet<Article>();
		int count=0;
		for (Annotation s : findAnnotationAll_FilterAG) {
			Article a = WikipediaSingleton.getInstance().wikipedia.getArticleById(s.getId()); 
			if (a!=null) {
				Article[] linksOut = a.getLinksOut();
				if (linksOut!=null) {
					HashSet<Article> hashSet = new HashSet<>(Arrays.asList(linksOut));
					setPossibleEntities.addAll(hashSet);
				}
				System.out.println("Size of the total annotation: "+ findAnnotationAll_FilterAG.size()+
						"count: "+count++ + "size of the set:"+setPossibleEntities.size());
			}
			System.out.println("Size of the set "+ setPossibleEntities.size());
		}
	}
	public void possibleDimentionsWithKB(List<String> lst) {
		Long start =TimeUtil.getStart() ;
		List<Annotation> findAnnotationAll_FilterAG = AnnonatationUtil.findAnnotationAll_FilterAG(lst);
		System.out.println("Time took for annotation:"+ TimeUtil.getEnd(TimeUnit.MICROSECONDS, start));
		Set<Article> setPossibleEntities = new HashSet<Article>();
		int count=0;
		for (Annotation s : findAnnotationAll_FilterAG) {
			Article a = WikipediaSingleton.getInstance().wikipedia.getArticleById(s.getId()); 
			if (a!=null) {
				Article[] linksOut = a.getLinksOut();
				if (linksOut!=null) {
					HashSet<Article> hashSet = new HashSet<>(Arrays.asList(linksOut));
					setPossibleEntities.addAll(hashSet);
				}
				System.out.println("Size of the total annotation: "+ findAnnotationAll_FilterAG.size()+
						"count: "+count++ + "size of the set:"+setPossibleEntities.size());
			}
			System.out.println("Size of the set "+ setPossibleEntities.size());
		}
	}
}
