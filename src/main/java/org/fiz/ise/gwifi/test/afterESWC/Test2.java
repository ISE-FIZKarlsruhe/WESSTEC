package org.fiz.ise.gwifi.test.afterESWC;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.GoogleModelSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.PTEModelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LabelsOfTheTexts;
import org.fiz.ise.gwifi.dataset.ReadDataset;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.Print;
import org.fiz.ise.gwifi.util.StopWordRemoval;
import org.fiz.ise.gwifi.util.StringUtil;
import org.fiz.ise.gwifi.util.VectorUtil;

import com.google.common.collect.Lists;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;

public class Test2 {
	public static HashMap<String, String> contentMap = new HashMap<String, String>();

	public static void readContentData (String contentInfoFile) {
		try {
			System.out.println("Read content data");
			FileReader reader = new FileReader(contentInfoFile);
			BufferedReader br = new BufferedReader(reader);

			String line = "";
			int count = 0;
			while ((line = br.readLine()) != null ) {

				if (count % 100 == 0) {
					//System.out.println("Processed content " + count + " lines...");
				}

				String[] tokens = line.split("\t");
				if (tokens.length>2) {
					tokens[1]=tokens[1]+" "+tokens[2];
					tokens=Arrays.copyOf(tokens, 2);
				}
				if (tokens.length != 2) 
					continue;

				String dataID = tokens[0];
				if (dataID.equals(String.valueOf(50298)) ) {
					System.out.println(line);
				}
				String content = tokens[1].replaceAll("[^a-zA-Z\\s]", " ").replaceAll("\\s+", " ");
				contentMap.put(dataID, content);

				count++;
			}
			br.close();
			reader.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		//System.out.println(contentMap.get(String.valueOf(4516)));
		System.out.println("4517: "+contentMap.get(String.valueOf(4517)));
		System.out.println("4518: "+contentMap.get(String.valueOf(4518)));
		System.exit(1);
		System.out.println("Finished reading ContentData");
	}
	public static void main(String[] args) throws Exception {

		String str2 = "full_part01.nt:<http://data.epo.org/linked-data/id/application/EP/96904109> ";
		if (str2.contains("http://data.epo.org/linked-data/def/patent/application")) {
			String[] split = str2.split("<http://data.epo.org/linked-data/def/patent/application>");
			String application=null;
			if (split[0].contains("/publication/EP/")) {
				int index_begin=split[0].indexOf("/publication/EP/");
				int index_end=split[0].indexOf("/->");
				String publicationID_type=null;
				try {
					if (index_end>0) {
						publicationID_type=split[0].substring(index_begin,index_end).replace("/publication/EP/", "");
						int index_end_app=split[1].indexOf("> .");
						application=split[1].substring(split[1].indexOf("id/application/EP/"),index_end_app).replace("id/application/EP/","");;
					}
				} catch (Exception e) {

				}
			}
		}
		readContentData("DBPedia_samples.txt");


		System.out.println(WikipediaSingleton.getInstance().wikipedia.getCategoryById(35068810));
		List<Article> lstCats = new ArrayList<Article>(LabelsOfTheTexts.getLables_DBP_article().values());


		Map<String, Double> temp= new HashMap<String, Double>();
		for(Article a : lstCats) {
			temp.put(a.getTitle(), 0.0);
		}

		LinkedHashMap<String, Double> sortedMap = new LinkedHashMap<>();
		temp.entrySet()
		.stream()
		.sorted(Map.Entry.comparingByKey())
		.forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));
		System.out.println(sortedMap);

		ArrayList<Article> lstFilter = new ArrayList<Article>(LabelsOfTheTexts.getLables_DBP_article().values());
		for(Article a : lstFilter) {
			if (GoogleModelSingleton.getInstance().google_model.hasWord(a.getTitle())) {
				//System.out.println(a.getTitle());
			}
			else {
				System.out.println("Not in vec space:"+a.getTitle());
			}
		}
		System.exit(1);
		VocabCache<VocabWord> vocab = GoogleModelSingleton.getInstance().google_model.vocab();
		System.out.println("Size of the vocab: "+vocab.words().size());
		int count =0;
		for (String w : vocab.words()) {
			System.out.println(w);
			if (count==10) {
				System.exit(0);
			}
			count++;
		}

		List<Article> labels_Snippets = Categories.getLabels_Snippets();
		for(Article a : labels_Snippets) {
			System.out.println(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(a.getTitle()));
		}
		Article a = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Politician");
		System.out.println(a.getFirstParagraphMarkup());
		String s_1="Rima				Türker";

		//String s_1=StringUtil.removePuntionation(StopWordRemoval.removeStopWords(a.getFirstParagraphMarkup()));
		//System.out.println(s_1);
		List<String> tokensStr = new ArrayList<String>(StringUtil.tokinizeString(s_1));
		System.out.println(tokensStr);

		String s_2=StringUtil.removePunctuation(a.getFirstParagraphMarkup());
		//System.out.println(s_2);
		tokensStr = new ArrayList<String>(StringUtil.tokinizeString(s_2));
		System.out.println(tokensStr);

		System.out.println(StringUtil.removePunctuation(StopWordRemoval.removeStopWords(a.getFirstParagraphMarkup())).trim().replaceAll(" +", " "));
		String amainCatAbstract = a.getFirstParagraphMarkup().replaceAll("[^\\w\\s]",""); 
		System.out.println(amainCatAbstract);
		//System.out.println(MapUtil.getKeyByValue(LabelsOfTheTexts.getLables_DBP_article(), a));
		//		try {
		//			List<String> lines = FileUtils.readLines(new File("/home/rtue/Desktop/Writing_sub_classes.txt"), "utf-8");
		//			for(String str:lines) {
		//				if (WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str)==null) {
		//					System.out.println(str);
		//				}
		//				//System.out.println(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(str).getId()));
		//			}
		//		
		//		
		//		} catch (IOException e1) {
		//			// TODO Auto-generated catch block
		//			e1.printStackTrace();
		//		}


		List<String> enrich = new ArrayList<String>();

		enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Office-holder").getId()));
		if (!LINE_modelSingleton.getInstance().lineModel.hasWord(enrich.get(0))) {
			System.out.println("NOOOOOOO");
		}
		System.exit(1);
		//		enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Photographer").getId()));
		//		enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Painter").getId()));
		//		enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Musical artist").getId()));
		//		enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Humorist").getId()));
		//		enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Fashion Designer").getId()));
		//		enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Dancer").getId()));
		////		enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Comics Creator").getId()));
		//		enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Comedian").getId()));
		//		enrich.add(String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Actor").getId()));
		System.out.println(enrich);
		System.out.println(WikipediaSingleton.getInstance().wikipedia.getArticleById(145418));
		Map<Integer, Article> lables_Yahoo_article = LabelsOfTheTexts.getLables_DBP_article();
		Print.printMap(lables_Yahoo_article);
		List<String> list = Lists.newArrayList(null, "5042765", "3194908", null,  null, "ben", "ben");

		List<String> listWithoutNulls = list.parallelStream()
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		for(String str : list) {
			if (!StringUtil.isNumeric(str)) {
				System.out.println(str);
			}
		}
		System.out.println(list);
		System.out.println(listWithoutNulls);

		System.out.println(Arrays.toString(VectorUtil.getSentenceVector(list, LINE_modelSingleton.getInstance().lineModel)));
		System.out.println(Arrays.toString(VectorUtil.getSentenceVector(listWithoutNulls, LINE_modelSingleton.getInstance().lineModel)));

		System.exit(1);
		try (BufferedReader br = Files.newBufferedReader(Paths.get("sample"))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] split = line.split("\t")[1].split(",");

				List<Integer> lst=  Arrays.asList(2825899, 55458, 731976, 2892840, 1644925, 2288615, 2990475, 78694, 3088902, 2815712, 957317, 3024609, 2003185, 2647259, 2429366, 2847837, 1060999, 1510351, 1299337, 1887143, 724567, 1775239, 1262318);
				for (int i :lst) {
					System.out.println(split[i]);
				}
				System.out.println();
				for (int i = 0; i < split.length; i++) {
					if (split[i].equals("1")) {
						System.out.println(i);
					}
				}

			}

		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
		}

	}

}
