package edu.kit.aifb.gwifi.db;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.hadoop.record.CsvRecordInput;
import org.apache.hadoop.record.CsvRecordOutput;
import org.apache.log4j.Logger;

import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;

import edu.kit.aifb.gwifi.db.struct.DbLabel;
import edu.kit.aifb.gwifi.db.struct.DbSenseForLabel;
import edu.kit.aifb.gwifi.util.ProgressTracker;
import edu.kit.aifb.gwifi.util.WikipediaConfiguration;
import edu.kit.aifb.gwifi.util.text.TextProcessor;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.TIntHashSet;

/**
 * A {@link WDatabase} for associating Strings with statistics about the articles (senses) this string could refer to. 
 */
public class LabelDatabase extends WDatabase<String, DbLabel> {

	//TODO: Labels cached as patricia trie
	// It would be extremely cool to cache labels in a Patricia Trie rather than a HashMap. 
	// This would give us:
	// - iteration in lexicographical order
	// - prefix-based search (i.e. autocomplete queries)
	// - fast generation of labels within edit distance (i.e spelling correction)
	// - faster detection of labels within texts, because we wouldn't need to check every single n-gram.
	
	
	private TextProcessor textProcessor ;

	/**
	 * Creates or connects to a database, whose name and type will be {@link WDatabase.DatabaseType#label}. 
	 * This will index label statistics according to their raw, unprocessed texts. 
	 * 
	 * @param env the WEnvironment surrounding this database
	 */
	public LabelDatabase(WEnvironment env) {

		super(
				env, 
				DatabaseType.label, 
				new StringBinding(), 
				new RecordBinding<DbLabel>() {
					public DbLabel createRecordInstance() {
						return new DbLabel() ;
					}
				}
		) ;

		textProcessor = null ;
	}

	/**
	 * Creates or connects to a database, whose type will be {@link WDatabase.DatabaseType#label} and name will be 
	 * {@link WDatabase.DatabaseType#label} concatenated with {@link TextProcessor#getName()}. 
	 * This will index label statistics according to their texts, after processing with the given {@link TextProcessor}
	 * 
	 * @param env the WEnvironment surrounding this database
	 * @param tp a text processor to apply to texts before indexing
	 */
	public LabelDatabase(WEnvironment env, TextProcessor tp) {

		super(
				env, 
				DatabaseType.label, 
				"label" + tp.getName(), 
				new StringBinding(), 
				new RecordBinding<DbLabel>() {
					public DbLabel createRecordInstance() {
						return new DbLabel() ;
					}
				}
		) ;

		textProcessor = tp ; 
	}

	/**
	 * Returns the text processor used to modify texts before they are used to index documents (may be null).
	 * @return the text processor used to modify texts before they are used to index documents (may be null).
	 */
	public TextProcessor getTextProcessor() {
		return textProcessor ;
	}

	/**
	 * If this database uses a text processor, you must prepare it (by calling {@link #prepare(File,int)} before use. 
	 * This returns true if that call has been made.
	 * 
	 * @return true if the database has been prepared for use, otherwise false
	 */
	public boolean isPrepared() {
		return getDatabase(true) != null ;
	}

	/**
	 * Retrieves the label statistics associated with the given text key. 
	 * 
	 * <p>Note:<b> you should NOT apply text processors to the key; that will be done internally within this method.
	 * 
	 * @return true if the database has been prepared for use, otherwise false
	 */
	@Override
	public DbLabel retrieve(String key) {

		if (textProcessor == null)
			return super.retrieve(key) ;
		else
			return super.retrieve(textProcessor.processText(key)) ;
	}

	@Override
	public DbLabel filterCacheEntry(WEntry<String,DbLabel> e, WikipediaConfiguration conf) {

		TIntHashSet validIds = conf.getArticlesOfInterest() ;
		
		DbLabel label = e.getValue() ;

		if ((float)label.getLinkDocCount()/label.getTextDocCount() < conf.getMinLinkProbability())
			return null ;

		ArrayList<DbSenseForLabel> newSenses = new ArrayList<DbSenseForLabel>() ;

		for(DbSenseForLabel sense:label.getSenses()) {

			if (validIds != null && !validIds.contains(sense.getId()))
				continue ;

			if (!sense.getFromRedirect() && !sense.getFromTitle()) {

				if ((float)sense.getLinkDocCount()/label.getLinkDocCount() < conf.getMinSenseProbability()) 
					continue ;
			}
			newSenses.add(sense) ;
		}

		if (newSenses.size() == 0)
			return null ;

		label.setSenses(newSenses) ;

		return label ;
	}

	@Override
	public WEntry<String,DbLabel> deserialiseCsvRecord(CsvRecordInput record) throws IOException {
		String text = record.readString(null) ;

		DbLabel l = new DbLabel() ;
		l.deserialize(record) ;
		
		return new WEntry<String,DbLabel>(text, l) ;
	}

	/**
	 * If this database uses a text processor, then you must prepare it before use. This involves copying all labels and statistics
	 * from the original label database (the one with no text processor), re-indexing all entries, and merging statistics whose
	 * processed texts collide with each other. 
	 * 
	 * This is done via an external sort, to avoid memory overflow. 
	 * 
	 * @param tempDir a directory for writing temporary files. Any files created will be deleted, but directories will not.
	 * @param passes the number of passes to break the task into (more = slower, but less memory required) 
	 * @throws IOException if the temporary directory is not writable. 
	 */
	public void prepare(File tempDir, int passes) throws IOException {

		if (textProcessor == null) 
			return ;

		WDatabase<String,DbLabel> originalLabels = env.getDbLabel(null) ; 
		long labelCount = originalLabels.getDatabaseSize() ;

		tempDir.mkdirs() ;

		ProgressTracker tracker = new ProgressTracker((2*passes)+1, LabelDatabase.class) ;

		for (int pass=0 ; pass<passes ; pass++) {

			tracker.startTask(labelCount, "Gathering and processing labels (pass " + (pass+1) + " of " + passes + ")") ;

			TreeMap<String, DbLabel> tmpProcessedLabels = new TreeMap<String, DbLabel>() ;

			WIterator<String,DbLabel> dbIter = originalLabels.getIterator() ;
			while (dbIter.hasNext()) {

				WEntry<String,DbLabel> e = dbIter.next();

				String processedText = textProcessor.processText(e.getKey()) ;

				if (Math.abs(processedText.hashCode()) % passes == pass) {

					DbLabel storedLabel = tmpProcessedLabels.get(processedText) ; 

					if (storedLabel == null) {
						tmpProcessedLabels.put(processedText, e.getValue()) ;
					} else {
						tmpProcessedLabels.put(processedText, mergeLabels(storedLabel, e.getValue())) ;
					}
				}
				tracker.update();
			}
			
			dbIter.close();

			//Dump gathered labels into temporary file
			tracker.startTask(tmpProcessedLabels.size(), "Dumping processed labels (pass " + (pass+1) + " of " + passes + ")") ;

			Iterator<Map.Entry<String,DbLabel>> mapIter = tmpProcessedLabels.entrySet().iterator() ;

			File tempFile = new File(tempDir.getPath() + File.separator + "tmpLabels" + pass + ".csv") ;
			tempFile.deleteOnExit() ;
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile)) ;

			while (mapIter.hasNext()) {
				tracker.update();

				Map.Entry<String, DbLabel> e = mapIter.next();

				ByteArrayOutputStream outStream = new ByteArrayOutputStream() ;

				CsvRecordOutput cro = new CsvRecordOutput(outStream) ;
				cro.writeString(e.getKey(), null) ;
				e.getValue().serialize(cro) ;

				writer.write(outStream.toString("UTF-8")) ;

			}
		}


		Database db = getDatabase(false) ;

		long bytesToRead = 0 ;
		long bytesRead = 0 ;
		BufferedReader[] readers = new BufferedReader[passes] ;

		String[] currKeys = new String[passes] ;
		DbLabel[] currValues = new DbLabel[passes] ;

		String line ;
		
		File[] tempFiles = new File[passes] ;

		for (int pass=0 ; pass<passes ; pass++) {

			File tempFile = new File(tempDir.getPath() + File.separator + "tmpLabels" + pass + ".csv") ;
			tempFiles[pass] = tempFile ;
			bytesToRead = bytesToRead + tempFile.length() ;

			readers[pass] = new BufferedReader(new FileReader(tempFile)) ;

			if ((line=readers[pass].readLine()) != null) {
				bytesRead = bytesRead + line.length() + 1 ;
				//System.out.println(line) ;

				line = line + "\n" ;

				try {
					CsvRecordInput cri = new CsvRecordInput(new ByteArrayInputStream(line.getBytes("UTF8"))) ;
					currKeys[pass] = cri.readString(null) ;
					currValues[pass] = new DbLabel() ;
					currValues[pass].deserialize(cri) ;
				} catch (Exception e) {
					Logger.getLogger(LabelDatabase.class).error("Could not parse '" + line + "'") ;
					currKeys[pass] = null ;
					currValues[pass] = null ;
				}
			} else {
				currKeys[pass] = null ;
				currValues[pass] = null ;
			}
		}

		tracker.startTask(bytesToRead, "Storing processed labels") ;

		while (true) {


			String lowestKey = null ;
			int pass = -1 ;

			for (int i=0 ; i<passes ; i++) {

				if (currKeys[i] != null && (lowestKey == null || currKeys[i].compareTo(lowestKey) < 0)) {
					lowestKey = currKeys[i] ;
					pass = i ;
				}
			}

			if (pass < 0) {
				//all readers are finished
				break ;
			} else {
				//save lowestKey and associated value
				DatabaseEntry k = new DatabaseEntry() ;
				keyBinding.objectToEntry(lowestKey, k) ;

				DatabaseEntry v = new DatabaseEntry() ;
				valueBinding.objectToEntry(currValues[pass], v) ;

				db.put(null, k, v) ;


				//advance reader

				if ((line=readers[pass].readLine()) != null) {
					bytesRead = bytesRead + line.length() + 1 ;
					tracker.update(bytesRead) ;

					line = line + "\n" ;

					try {
						CsvRecordInput cri = new CsvRecordInput(new ByteArrayInputStream(line.getBytes("UTF8"))) ;
						currKeys[pass] = cri.readString(null) ;
						currValues[pass] = new DbLabel() ;
						currValues[pass].deserialize(cri) ;
					} catch (Exception e) {
						Logger.getLogger(LabelDatabase.class).error("Could not parse '" + line + "'") ;
						currKeys[pass] = null ;
						currValues[pass] = null ;
					}
				} else {
					currKeys[pass] = null ;
					currValues[pass] = null ;
				}
			}
		}

		for (BufferedReader r:readers) 
			r.close();
		
		
		for (File tempFile:tempFiles)
			tempFile.delete() ;

		env.cleanAndCheckpoint() ;
		getDatabase(true) ;

	}

	private DbLabel mergeLabels(DbLabel lblA, DbLabel lblB) {

		THashMap<Integer,DbSenseForLabel> senseHash = new THashMap<Integer,DbSenseForLabel>() ;


		if (lblA.getSenses() != null) {
			for (DbSenseForLabel s:lblA.getSenses()) 
				senseHash.put(s.getId(), s) ;
		}

		if (lblB.getSenses() != null) {
			for (DbSenseForLabel s1:lblB.getSenses()) {

				DbSenseForLabel s2 = senseHash.get(s1.getId()) ;
				if (s2 == null) {
					senseHash.put(s1.getId(), s1) ;
				} else {
					DbSenseForLabel s3 = new DbSenseForLabel() ;
					s3.setId(s1.getId()) ;
					s3.setLinkDocCount(s1.getLinkDocCount() + s2.getLinkDocCount()) ;
					s3.setLinkOccCount(s1.getLinkOccCount() + s2.getLinkOccCount()) ;
					s3.setFromRedirect(s1.getFromRedirect() || s2.getFromRedirect()) ;
					s3.setFromTitle(s1.getFromTitle() || s2.getFromTitle()) ;

					senseHash.put(s3.getId(), s3) ;
				}
			}
		}

		ArrayList<DbSenseForLabel> mergedSenses = new ArrayList<DbSenseForLabel>() ;

		for (DbSenseForLabel s:senseHash.values()) 
			mergedSenses.add(s) ;

		Collections.sort(mergedSenses, new Comparator<DbSenseForLabel>() {

			public int compare(DbSenseForLabel a, DbSenseForLabel b) {

				int cmp = new Long(b.getLinkOccCount()).compareTo(a.getLinkOccCount()) ;
				if (cmp != 0)
					return cmp ;

				cmp = new Long(b.getLinkDocCount()).compareTo(a.getLinkDocCount()) ;
				if (cmp != 0)
					return cmp ;

				return(new Integer(a.getId()).compareTo(b.getId())) ;
			}
		}) ;


		DbLabel mergedLabel = new DbLabel() ;
		mergedLabel.setLinkDocCount(lblA.getLinkDocCount() + lblB.getLinkDocCount()) ;
		mergedLabel.setLinkOccCount(lblA.getLinkOccCount() + lblB.getLinkOccCount()) ;
		mergedLabel.setTextDocCount(lblA.getTextDocCount() + lblB.getTextDocCount()) ;
		mergedLabel.setTextOccCount(lblA.getTextOccCount() + lblB.getTextOccCount()) ;
		mergedLabel.setSenses(mergedSenses) ;

		return mergedLabel ;
	}

}
