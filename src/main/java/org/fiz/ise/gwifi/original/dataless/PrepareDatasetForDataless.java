package org.fiz.ise.gwifi.original.dataless;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.dataset.LabelsOfTheTexts;
import org.fiz.ise.gwifi.dataset.ReadDataset;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.Config;

import edu.kit.aifb.gwifi.model.Article;


public class PrepareDatasetForDataless {
	private final static Dataset TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger thirdLOG = Logger.getLogger("resultLogger");
	
	public static void main(String[] args) {
		try {
//			List<String> lines = FileUtils.readLines(new File(Config.getString("DATASET_DBP_TEST","")), "utf-8");
			Map<String, List<Article>> map_dataset_DBPedia_SampleLabel = ReadDataset.read_dataset_DBPedia_SampleLabel(Config.getString("DATASET_DBP_TEST",""));
			List<String> lstCat = new ArrayList<>(LabelsOfTheTexts.getLabels_DBP());
			int count =0;
			for(Entry<String, List<Article>> e : map_dataset_DBPedia_SampleLabel.entrySet()) {
				secondLOG.info(count++ +"\t"+e.getKey());
			}
			for(String c : lstCat) {
				thirdLOG.info(c);
			}
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
