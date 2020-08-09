package edu.kit.aifb.gwifi.lexica.extraction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.model.Wikipedia;


// Used to create xLiD-Lexica
public class CLMappingExtractor {

	private static String SPLITTER = ",\t";

	private final static String[] languages = { "en", "de", "es", "ca", "sl", "zh" };

	private static String SOURCE_PAGE_ID_FIELD = "s_id";
	private static String TARGET_PAGE_ID_FIELD = "t_id";
	private static String SOURCE_PAGE_LANG_FIELD = "s_lang";
	private static String TARGET_PAGE_LANG_FIELD = "t_lang";
	private static String SOURCE_PAGE_TITLE_FIELD = "s_title";
	private static String TARGET_PAGE_TITLE_FIELD = "t_title";

	public static void main(String[] args) throws Exception {

		Set<String> langSet = new HashSet<String>();
		Map<String, Wikipedia> lang2wiki = new HashMap<String, Wikipedia>();

		// /Users/leizhang/Data/Wikipedia/langlinks_en.csv
		File langlinks = new File(args[0]);
		BufferedReader br = new BufferedReader(new FileReader(langlinks));

		for (String lang : languages) {
			// /Users/leizhang/Data/Wikipedia/configs
			String wikiConfigPath = args[1] + "/wikipedia-template-" + lang + ".xml";
			Wikipedia wikipedia = new Wikipedia(new File(wikiConfigPath), false);
			lang2wiki.put(lang, wikipedia);
			langSet.add(lang);
		}

		// /Users/leizhang/Data/Wikipedia/mappings
		File indexDir = new File(args[2]);
		Directory index = FSDirectory.open(indexDir);
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_48);
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_48, analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		iwc.setRAMBufferSizeMB(256.0);
		IndexWriter indexWriter = new IndexWriter(index, iwc);

		// en
		String sourceLang = args[3];
		Wikipedia sourceWikipedia = lang2wiki.get(sourceLang);

		int i = 0;
		String line;
		while ((line = br.readLine()) != null) {
			if (++i % 10000 == 0)
				System.out.println(i + " langlinks have been processed.");
			String[] splits = line.split(SPLITTER);

			if (splits.length != 3)
				continue;

			String targetLang = splits[1].trim();
			if (!langSet.contains(targetLang))
				continue;
			String targetTitle = splits[2].trim();
			if (targetTitle.equals(""))
				continue;

			int sourcePageId = Integer.parseInt(splits[0]);
			Page sourcePage = sourceWikipedia.getPageById(sourcePageId);
			if (sourcePage == null)
				continue;
			if (!sourcePage.getType().equals(PageType.article))
				continue;
			Article sourceArticle = (Article) sourcePage;

			Wikipedia targetWikipedia = lang2wiki.get(targetLang);
			if (targetWikipedia == null)
				continue;
			Article targetArticle = targetWikipedia.getArticleByTitle(targetTitle);
			if (targetArticle == null)
				continue;

			createDocuments(sourceArticle, targetArticle, sourceLang, targetLang, indexWriter);
		}

//		indexWriter.optimize();
		
		indexWriter.close();

	}

	public static void createDocuments(Article sourceArticle, Article targetArticle, String sourceLang,
			String targetLang, IndexWriter indexWriter) throws CorruptIndexException, IOException {
		Document doc = new Document();

		Field s_id = new Field(SOURCE_PAGE_ID_FIELD, String.valueOf(sourceArticle.getId()), Field.Store.YES,
				Field.Index.NOT_ANALYZED_NO_NORMS);
		Field t_id = new Field(TARGET_PAGE_ID_FIELD, String.valueOf(targetArticle.getId()), Field.Store.YES,
				Field.Index.NOT_ANALYZED_NO_NORMS);
		doc.add(s_id);
		doc.add(t_id);

		Field s_lang = new Field(SOURCE_PAGE_LANG_FIELD, sourceLang, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS);
		Field t_lang = new Field(TARGET_PAGE_LANG_FIELD, targetLang, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS);
		doc.add(s_lang);
		doc.add(t_lang);

		Field s_title = new Field(SOURCE_PAGE_TITLE_FIELD, String.valueOf(sourceArticle.getTitle()), Field.Store.YES,
				Field.Index.NOT_ANALYZED_NO_NORMS);
		Field t_title = new Field(TARGET_PAGE_TITLE_FIELD, String.valueOf(targetArticle.getTitle()), Field.Store.YES,
				Field.Index.NOT_ANALYZED_NO_NORMS);
		doc.add(s_title);
		doc.add(t_title);

		indexWriter.addDocument(doc);
	}

}