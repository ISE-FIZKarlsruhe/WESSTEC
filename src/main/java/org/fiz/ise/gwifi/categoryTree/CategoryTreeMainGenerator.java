package org.fiz.ise.gwifi.categoryTree;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.fiz.ise.gwifi.util.Config;

public class CategoryTreeMainGenerator {

	private static int DEPTH_OF_CAT_TREE = Config.getInt("DEPTH_OF_CAT_TREE", 0);
	private static String SKOS_CATEGORY_FILE = Config.getString("SKOS_CATEGORY_FILE", "");
	/**
	 * This directory contain a file which contains the seeds(roots) for category
	 * tree generation. Each line contains one seed
	 */
	//private static String SEED_FILE = Config.getString("ADDRESS_OF_CATEGORY_SEEDS", "");

	public static void main(String[] args) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println("program started "+dateFormat.format(date)); //2016/11/16 12:08:43
		final Thread t = new Thread(run());
		t.setDaemon(false);
		t.start();
	}
	public static Runnable run() {
		return () -> {
			//final CategorySeedloader seedLoader = new CategorySeedLoaderFileBased(SEED_FILE);
			final CategorySeedloader seedLoader = new CategorySeedLoaderFromMemory();
			seedLoader.loadSeeds();

			final CategoryFileParser fileParser = new CategoryFileParser(SKOS_CATEGORY_FILE);
			long now = System.currentTimeMillis();
			fileParser.parse();
			System.err.println(
					"Reading " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - now) + " seconds");

			now = System.currentTimeMillis();
			new FastLookUpSubjectObject(ListOfSubjectObject.getListOfSubjectObjects());
			System.err.println(
					"Speedup " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - now) + " seconds");

			now = System.currentTimeMillis();
			
			System.out.println("DEPTH_OF_CAT_TREE "+DEPTH_OF_CAT_TREE);
			new CategoryTreeGenerator(seedLoader.getSeeds(), DEPTH_OF_CAT_TREE).printTreesToFile();
			System.err.println("Generating Trees " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - now)
					+ " seconds");
		};
	}

}
