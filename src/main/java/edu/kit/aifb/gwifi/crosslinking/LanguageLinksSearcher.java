package edu.kit.aifb.gwifi.crosslinking;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;

public class LanguageLinksSearcher {

	private static String SOURCE_PAGE_ID_FIELD = "s_id";
	private static String TARGET_PAGE_TITLE_FIELD = "t_title";
	private static String SOURCE_PAGE_LANG_FIELD = "s_lang";
	private static String TARGET_PAGE_LANG_FIELD = "t_lang";

	private static IndexSearcher searcher;
	private static IndexReader reader;

	public LanguageLinksSearcher(String path) throws IOException {
		reader = DirectoryReader.open(FSDirectory.open(new File(path)));
		searcher = new IndexSearcher(reader);
	}

	public String getDescription(int sourceId, String sourceLang, String targetLang) throws IOException {
		BooleanQuery query = new BooleanQuery();
		Query tq = NumericRangeQuery.newIntRange(SOURCE_PAGE_ID_FIELD, 1, sourceId, sourceId, true, true);
		query.add(tq, Occur.MUST);
		Term term = new Term(SOURCE_PAGE_LANG_FIELD, sourceLang);
		tq = new TermQuery(term);
		query.add(tq, Occur.MUST);
		term = new Term(TARGET_PAGE_LANG_FIELD, targetLang);
		tq = new TermQuery(term);
		query.add(tq, Occur.MUST);

		ScoreDoc[] hits = searcher.search(query, 1).scoreDocs;
		if (hits == null || hits.length == 0)
			return null;
		ScoreDoc hit = hits[0];
		Document doc = searcher.doc(hit.doc);
		String targetTitle = doc.get(TARGET_PAGE_TITLE_FIELD);
		return targetTitle;
	}
	
	public void close() throws IOException {
		reader.close();
	}
	
	public static void main(String[] args) throws Exception {
//		LanguageLinksSearcher lls = new LanguageLinksSearcher("/Users/leizhang/Data/Wikipedia/langlinks/index/");
		LanguageLinksSearcher lls = new LanguageLinksSearcher("/Users/leizhang/Data/Wikipedia/langlinks/201410/index/");
		String title = lls.getDescription(33076, "ca", "fr");
		System.out.print(title);
	}

}
