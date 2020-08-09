package org.fiz.ise.gwifi.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

public class WikipediaFilesUtil {

	private static final Logger LOG = Logger.getLogger(WikipediaFilesUtil.class.getCanonicalName());
	/**
	 * This folder contains all the Wikipedia pages which are already cleaned by a
	 * python code from https://github.com/attardi/wikiextractor and contains the
	 * links and anchor text
	 */
	private static final String WIKI_FILES_FOLDER = "";

	/**
	 * Reading Wikipedia files by specifying only one folder
	 * @param pathToInsideFile
	 * @return
	 */
	public static List<Document> getDocuments(String pathToInsideFile) {
		final List<Document> result = new ArrayList<>();
		final Pattern titlePattern = Pattern.compile("<doc.* url=\".*\" title=\".*\">");
		try {
			final List<String> lines = Files.readAllLines(Paths.get(pathToInsideFile), StandardCharsets.UTF_8);
			String title = "";
			Integer id = null ;
			final List<String> content = new ArrayList<>();
			for (int i = 0; i < lines.size(); i++) {
				final String line = lines.get(i);
				if (line.isEmpty()) {
					continue;
				}
				final Matcher titleMatcher = titlePattern.matcher(line);
				if (titleMatcher.find()) {
					content.clear();
					String startID ="<doc id=\"";
					String endID="\" url=";
					id = Integer.parseInt(lines.get(i).substring(lines.get(i).indexOf(startID)+startID.length(), lines.get(i).indexOf(endID)));
					title = lines.get(++i);
					continue;
				} else if (line.equals("</doc>")) {
					final Document d = new Document(id,title.replace(" ", "_"), new ArrayList<>(content), pathToInsideFile);
					result.add(d);
				} else {
					content.add(line);
				}
			}
		} catch (final IOException e) {
			System.out.println(e.getMessage());
			LOG.error(e.getMessage());
		}
		return result;
	}

	/**
	 * Read all the documents at once from the same structure that
	 * we save Wikipedia files.
	 * @return
	 */
	public static List<Document> getDocuments() {
		final List<Document> result = new ArrayList<>();
		final File[] listOfFolders = new File(WIKI_FILES_FOLDER).listFiles();
		Arrays.sort(listOfFolders);
		for (int i = 0; i < listOfFolders.length; i++) {
			final String subFolder = listOfFolders[i].getName();
			final File[] listOfFiles = new File(WIKI_FILES_FOLDER + File.separator + subFolder + File.separator)
					.listFiles();
			Arrays.sort(listOfFiles);
			for (int j = 0; j < listOfFiles.length; j++) {
				final String file = listOfFiles[j].getName();
				result.addAll(getDocuments(
						WIKI_FILES_FOLDER + File.separator + subFolder + File.separator + File.separator + file));
			}
			
		}
		return result;
	}
}
