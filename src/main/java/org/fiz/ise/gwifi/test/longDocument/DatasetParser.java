package org.fiz.ise.gwifi.test.longDocument;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.LexedTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.WordToSentenceProcessor;

public class DatasetParser {

	public static void main(String[] args) {
		String folder="/home/rtue/eclipse-workspace/Dataset_ShortTextClassification/20news-18828/";
		File[] folders = new File(folder).listFiles();
		try {
			Collections.sort(Arrays.asList(folders));
			for (int i = 0; i < folders.length; i++) {
				System.out.println(folders[i].getName());
				File[] files = new File(folders[i].getPath()).listFiles(); 
				Collections.sort(Arrays.asList(files));
				for (int j = 0; j < files.length; j++) {
					
					String content = readFileToString(files[i].getPath());
					final ArrayList<String> sentenceList = segment2Sentence(content);
					for(String sentence : sentenceList) {
						System.out.println(sentence);
					}
					
//					List<String> lines = new ArrayList<>(FileUtils.readLines(new File(files[i].getPath()), "utf-8"));
//					for (String line: lines) {
//						if (line.equals(anObject)) {
//							
//						}
//					}
				
				}
				
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	private static String readFileToString(String filePath)
    {
        StringBuilder contentBuilder = new StringBuilder();
        try 
        {
        	List<String> lines = new ArrayList<>(FileUtils.readLines(new File(filePath), "utf-8"));
        	for(String line:lines) {
        		contentBuilder.append(line).append("\n");
        	}
//        	lines.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
 
        return contentBuilder.toString();
    }
	
	public static ArrayList<String> segment2Sentence(String text) {
		final List<CoreLabel> tokens = new ArrayList<CoreLabel>();

		final LexedTokenFactory<CoreLabel> tokenFactory = new CoreLabelTokenFactory();

		final PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<CoreLabel>(new StringReader(text), tokenFactory, "untokenizable=noneDelete");

		while (tokenizer.hasNext()) {
			tokens.add(tokenizer.next());
		}

		final List<List<CoreLabel>> sentences = new WordToSentenceProcessor<CoreLabel>().process(tokens);
		int end;
		int start = 0;
		final ArrayList<String> sentenceList = new ArrayList<String>();
		for (List<CoreLabel> sentence: sentences) {
			end = sentence.get(sentence.size()-1).endPosition();
			sentenceList.add(text.substring(start, end).trim());
			start = end;
		}
		return sentenceList;
	}

}
