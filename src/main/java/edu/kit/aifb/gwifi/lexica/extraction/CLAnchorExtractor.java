package edu.kit.aifb.gwifi.lexica.extraction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;

import edu.kit.aifb.gwifi.db.WEnvironment.StatisticName;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Label;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.util.LabelIterator;
import edu.kit.aifb.gwifi.util.PageIterator;

public class CLAnchorExtractor {

	private final static String[] languages = { "en", "de", "es", "ca", "sl", "zh" };

	private static String SOURCE_PAGE_ID_FIELD = "s_id";
	private static String TARGET_PAGE_ID_FIELD = "t_id";
	private static String SOURCE_PAGE_LANG_FIELD = "s_lang";
	private static String TARGET_PAGE_LANG_FIELD = "t_lang";
//	private static String SOURCE_PAGE_TITLE_FIELD = "s_title";
//	private static String TARGET_PAGE_TITLE_FIELD = "t_title";

	private static String blockProp = "<http://xlid-lexica/block>";
	private static String blockLangProp = "<http://xlid-lexica/block#lang>";
	private static String resSameAsProp = "<http://xlid-lexica/res#sameAs>";
	private static String resLinkDocCountProp = "<http://xlid-lexica/res#linkDocCount>";
	private static String resLinkOccCountProp = "<http://xlid-lexica/res#linkOccCount>";
	private static String resProp = "<http://xlid-lexica/res#probability>";

	private static String sfPrefix = "http://xlid-lexica/sf/";

	private static String sfLabelProp = "<http://xlid-lexica/sf#label>";
	private static String sfLinkDocCountProp = "<http://xlid-lexica/sf#linkDocCount>";
	private static String sfLinkOccCountProp = "<http://xlid-lexica/sf#linkOccCount>";
	private static String sfTextDocCountProp = "<http://xlid-lexica/sf#textDocCount>";
	private static String sfTextOccCountProp = "<http://xlid-lexica/sf#textOccCount>";
	private static String sfLinkProb = "<http://xlid-lexica/sf#linkProbability>";

	private static String resSfProp = "<http://xlid-lexica/res#sf>";
	private static String resSenseLinkDocCountProp = "<http://xlid-lexica/res#senseLinkDocCount>";
	private static String resSenseLinkOccCountProp = "<http://xlid-lexica/res#senseLinkOccCount>";
	private static String resPriorProbProp = "<http://xlid-lexica/res#priorProbability>";

	private static String integerLabel = "^^<http://www.w3.org/2001/XMLSchema#integer>";
	private static String doubleLabel = "^^<http://www.w3.org/2001/XMLSchema#double>";

	private static Map<String, Wikipedia> lang2wikipedia;
	private static Map<String, Long> lang2articleCount;
	private static IndexSearcher searcher;
	private static BufferedWriter out;
	private static long i = 0;
	private static String sourceLang;

	// java -Xmx20g -jar cl-anchor.jar /local/users/lzh/anchor/DBpedia_lexical_resource.nt /local/users/lzh/anchor/configs /local/users/lzh/anchor/mappings-en en
	
	public static void main(String[] args) throws Exception {

		// /Users/leizhang/Data/Wikipedia/DBpedia_lexical_resource.nt
		File file = new File(args[0]);
		out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));

		lang2wikipedia = new HashMap<String, Wikipedia>();
		lang2articleCount = new HashMap<String, Long>();

		for (String lang : languages) {
			// /Users/leizhang/Data/Wikipedia/configs
			String wikiConfigPath = args[1] + "/wikipedia-template-" + lang + ".xml";
			Wikipedia wikipedia = new Wikipedia(new File(wikiConfigPath), false);
			lang2wikipedia.put(lang, wikipedia);
			lang2articleCount.put(lang, wikipedia.getEnvironment().retrieveStatistic(StatisticName.articleCount));
			System.out.println("The Wikipedia environment for langauge " + lang + " has been initialized.");
		}

		// /Users/leizhang/Data/Wikipedia/mappings-en
		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(args[2])));
		searcher = new IndexSearcher(reader);
		
		// en
		sourceLang = args[3];
		Wikipedia sourceWikipedia = lang2wikipedia.get(sourceLang);
		PageIterator pageIterator = sourceWikipedia.getPageIterator(PageType.article);

		int j = 0;
		while (pageIterator.hasNext()) {

			j++;
			if (j % 1000 == 0)
				System.out.println(j + " articles have been processed!");

			Page page = pageIterator.next();

			if (!page.getType().equals(PageType.article))
				continue;

			Article sourceArticle = (Article) page;
			int sourceId = sourceArticle.getId();
			String sourceTitle = sourceArticle.getTitle();

			if (sourceTitle == null || sourceTitle.equals(""))
				continue;

//			if(sourceTitle.endsWith("Anarchism"))
//				System.out.print("Id: " + sourceId + "\n");
			
			for (String targetLang : languages) {
				if (targetLang.equals(sourceLang)) {
					if (sourceArticle == null) {
						System.out.println("Could not find exact match. Searching through anchors instead");
						continue;
					} else {
						print(sourceTitle, sourceArticle, targetLang);
					}
				} else {
					int targetId = getTargetId(sourceId, targetLang);
					if (targetId != 0) {
						Article targetArticle = lang2wikipedia.get(targetLang).getArticleById(targetId);
						print(sourceTitle, targetArticle, targetLang);
					}
				}
			}
		}

		for (String lang : languages) {
			Wikipedia wikipedia = lang2wikipedia.get(lang);
			LabelIterator labelIterator = wikipedia.getLabelIterator(null);
			j = 0;
			while (labelIterator.hasNext()) {

				j++;
				if (j % 1000 == 0)
					System.out.println(j + " labels for language " + lang + " have been processed!");

				Label label = labelIterator.next();
				String anchorText = label.getText();
				if (anchorText == null || anchorText.equals(""))
					continue;			
				
				// the statistics of a surface form
				printLabel(anchorText, label, lang);
				
			}
			labelIterator.close();
		}

		pageIterator.close();
		reader.close();
		out.close();
	}

	public static int getTargetId(int sourceId, String targetLang) throws IOException {
		BooleanQuery query = new BooleanQuery();
		Term term = new Term(SOURCE_PAGE_ID_FIELD, String.valueOf(sourceId));
		Query tq = new TermQuery(term);
		query.add(tq, Occur.MUST);
		term = new Term(SOURCE_PAGE_LANG_FIELD, sourceLang);
		tq = new TermQuery(term);
		query.add(tq, Occur.MUST);
		term = new Term(TARGET_PAGE_LANG_FIELD, targetLang);
		tq = new TermQuery(term);
		query.add(tq, Occur.MUST);

		ScoreDoc[] hits = searcher.search(query, 1).scoreDocs;
		if (hits == null || hits.length == 0)
			return 0;
		ScoreDoc hit = hits[0];
		Document doc = searcher.doc(hit.doc);
		int targetId = Integer.valueOf(doc.get(TARGET_PAGE_ID_FIELD));
		return targetId;
	}

	public static void print(String sourceTitle, Article article, String lang) throws IOException {
		// the count and probability of incoming page links of a resource
		printResource(sourceTitle, article, lang);

		Map<Article.Label, Double> labels = new HashMap<Article.Label, Double>();

		for (Article.Label at : article.getLabels()) {
			String anchorText = at.getText();
			Label label = lang2wikipedia.get(lang).getLabel(anchorText, null);
			if (label == null)
				continue;

			for (Label.Sense sense : label.getSenses()) {
				if (sense.getId() == article.getId()) {
					labels.put(at, sense.getPriorProbability());
				}
			}
		}

		for (Article.Label at : labels.keySet()) {
			// the count and probability of surface forms referring a resource
			printResourceLabel(sourceTitle, at, lang, labels);
		}
	}

	public static void printResource(String title, Article article, String lang) throws IOException {
		int reslinkOccCount = article.getTotalLinksInCount();
		int reslinkDocCount = article.getDistinctLinksInCount();
		double priorProb = ((double) reslinkDocCount) / lang2articleCount.get(lang);
		
		String res = "<http://dbpedia.org/resource/" + processSpecialChar(title).replace(" ", "_") + ">";
		String block = "_:b" + ++i;

		out.write(res + " " + blockProp + " " + block + " .\n");
		out.write(block + " " + blockLangProp + " " + "\"" + lang + "\"" + " .\n");
		if (!sourceLang.equals(lang)) {
			String sameRes = "<http://" + lang + ".dbpedia.org/resource/" + processSpecialChar(title).replace(" ", "_") + ">";
			out.write(block + " " + resSameAsProp + " " + sameRes + " .\n");
		}
		out.write(block + " " + resLinkOccCountProp + " " + "\"" + reslinkOccCount + "\"" + integerLabel + " .\n");
		out.write(block + " " + resLinkDocCountProp + " " + "\"" + reslinkDocCount + "\"" + integerLabel + " .\n");
		out.write(block + " " + resProp + " " + "\"" + priorProb + "\"" + doubleLabel + " .\n");
	}

	public static void printResourceLabel(String title, Article.Label at, String lang, Map<Article.Label, Double> labels)
			throws IOException {
		long senseLinkDocCount = at.getLinkDocCount();
		senseLinkDocCount = senseLinkDocCount == 0 ? 1 : senseLinkDocCount;
		long senseLinkOccCount = at.getLinkOccCount();
		senseLinkOccCount = senseLinkOccCount == 0 ? 1 : senseLinkOccCount;
		
		String res = "<http://dbpedia.org/resource/" + processSpecialChar(title).replace(" ", "_") + ">";
		String anchroText = at.getText();
		String sf = "<" + sfPrefix + lang + "/" + processSpecialChar(anchroText).replace(" ", "_") + ">";
		String block = "_:b" + ++i;

		out.write(res + " " + blockProp + " " + block + " .\n");
		out.write(block + " " + blockLangProp + " " + "\"" + lang + "\"" + " .\n");
		out.write(block + " " + resSfProp + " " + sf + " .\n");
		out.write(block + " " + resSenseLinkDocCountProp + " " + "\"" + senseLinkDocCount + "\"" + integerLabel + " .\n");
		out.write(block + " " + resSenseLinkOccCountProp + " " + "\"" + senseLinkOccCount + "\"" + integerLabel + " .\n");
		out.write(block + " " + resPriorProbProp + " " + "\"" + labels.get(at) + "\"" + doubleLabel + " .\n");
	}
	
	public static void printLabel(String labelText, Label label, String lang) throws IOException {
		long textDocCount = label.getDocCount();
		textDocCount = textDocCount == 0 ? 1 : textDocCount;
		long textOccCount = label.getOccCount();
		textOccCount = textOccCount == 0 ? 1 : textOccCount;
		long linkDocCount = label.getLinkDocCount();
		long linkOccCount = label.getLinkOccCount();
		double linkProb = (double) linkDocCount/textDocCount;
		
		String sf = "<" + sfPrefix + lang + "/" + processSpecialChar(labelText).replace(" ", "_") + ">";
		String block = "_:b" + ++i;

		out.write(sf + " " + blockProp + " " + block + " .\n");
		out.write(block + " " + blockLangProp + " " + "\"" + lang + "\"" + " .\n");
		out.write(block + " " + sfLabelProp + " " + "\"" + processSpecialChar(labelText) + "\"" + "@" + lang + " .\n");
		out.write(block + " " + sfTextDocCountProp + " " + "\"" + textDocCount + "\"" + integerLabel + " .\n");
		out.write(block + " " + sfTextOccCountProp + " " + "\"" + textOccCount + "\"" + integerLabel + " .\n");
		out.write(block + " " + sfLinkDocCountProp + " " + "\"" + linkDocCount + "\"" + integerLabel + " .\n");
		out.write(block + " " + sfLinkOccCountProp + " " + "\"" + linkOccCount + "\"" + integerLabel + " .\n");
		out.write(block + " " + sfLinkProb + " " + "\"" + linkProb + "\"" + doubleLabel + " .\n");
	}
	
	public static String processSpecialChar(String text) {
		text = text.replace("<", "-");
		text = text.replace(">", "-");
		text = text.replace("\"", "-");
		text = text.replace("\\", "-");
		return text;
	}
}
