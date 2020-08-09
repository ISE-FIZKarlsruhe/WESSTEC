package edu.kit.aifb.gwifi.demo;

import java.io.File;

import edu.kit.aifb.gwifi.model.Label;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.util.LabelIterator;

public class LabelIteratorDemo {
	
	public static void main(String[] args) throws Exception {
		File databaseDirectory = new File("configs/wikipedia-template-zh.xml");
		Wikipedia wikipedia = new Wikipedia(databaseDirectory, false);
		System.out.println("The Wikipedia environment has been initialized.");
		
		LabelIterator labelIterator = wikipedia.getLabelIterator(null);
		int i = 0;
		while (labelIterator.hasNext()) {
			if (++i % 100000 == 0) {
				System.out.println(i + " lables have been processed!");
			}
			Label label = labelIterator.next();
			System.out.println("text: " + label.getText());
			System.out.println("docCount: " + label.getDocCount());
			System.out.println("linkCount: " + label.getLinkDocCount());
		}	
	}

}
