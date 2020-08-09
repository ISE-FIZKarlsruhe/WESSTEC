package edu.kit.aifb.gwifi.crosslinking;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class LanguageLinksIndexer {

	private static String SPLITTER = ",\t";

	private static String SOURCE_PAGE_ID_FIELD = "s_id";
	private static String TARGET_PAGE_TITLE_FIELD = "t_title";
	private static String SOURCE_PAGE_LANG_FIELD = "s_lang";
	private static String TARGET_PAGE_LANG_FIELD = "t_lang";

	public static void main(String[] args) throws Exception {

		// /Users/leizhang/Data/Wikipedia/langlinks/data/
		File dir = new File(args[0]);
		File[] files = dir.listFiles();

		// /Users/leizhang/Data/Wikipedia/langlinks/index/
		File indexDir = new File(args[1]);
		Directory index = FSDirectory.open(indexDir);
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_48);
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_48, analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		iwc.setRAMBufferSizeMB(256.0);
		IndexWriter indexWriter = new IndexWriter(index, iwc);

		for (File file : files) {
			String name = file.getName();
			if (!name.startsWith("langlinks_") || name.length() != 16 || !(name.endsWith(".csv")))
				continue;
			String sourceLang = name.substring(10, 12);

			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

			int i = 0;
			String line;
			while ((line = br.readLine()) != null) {
				if (++i % 10000 == 0)
					System.out.println(i + " langlinks have been processed.");
				String[] splits = line.split(SPLITTER);

				if (splits.length != 3)
					continue;

				String targetLang = splits[1].trim();
				String targetTitle = splits[2].trim();
				if (targetTitle.equals(""))
					continue;

				int sourcePageId = Integer.parseInt(splits[0]);

				createDocuments(sourcePageId, targetTitle, sourceLang, targetLang, indexWriter);
			}
		}

		// indexWriter.optimize();
		indexWriter.close();

	}

	public static void createDocuments(int sourceId, String targetTitle, String sourceLang, String targetLang,
			IndexWriter indexWriter) throws CorruptIndexException, IOException {
		Document doc = new Document();

		Field s_id = new IntField(SOURCE_PAGE_ID_FIELD, sourceId, Field.Store.YES);
		doc.add(s_id);

		Field t_title = new StringField(TARGET_PAGE_TITLE_FIELD, targetTitle, Field.Store.YES);
		doc.add(t_title);

		Field s_lang = new StringField(SOURCE_PAGE_LANG_FIELD, sourceLang, Field.Store.YES);
		Field t_lang = new StringField(TARGET_PAGE_LANG_FIELD, targetLang, Field.Store.YES);
		doc.add(s_lang);
		doc.add(t_lang);

		indexWriter.addDocument(doc);
	}

}