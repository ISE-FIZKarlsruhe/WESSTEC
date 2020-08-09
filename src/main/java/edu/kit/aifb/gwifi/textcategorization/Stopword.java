package edu.kit.aifb.gwifi.textcategorization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

//import com.clor.base.backend.Enviorment.ConfigParams;
//import com.clor.base.configuration.Configurable;
//import com.clor.base.configuration.Configuration;
//import com.clor.base.configuration.DefaultConfigurator;


//public class Stopword implements Configurable<Map<String, Object>>{
public class Stopword{
	private static String stopwordFileString = "res/stopwords/en-stopwords.txt";
	//private static String stopwordFileString = "/home/ls3data/users/lzh/congliu/stopword-list.txt";
    private static Set<String> stopwords = new HashSet<String>();
	private static Stopword instance;
	private Stopword() throws IOException{
//		Configuration<Map<String, Object>> configurator = new DefaultConfigurator();
//		configurator.configure(this);
		loadStopwords();
	}
	
	private void loadStopwords() throws IOException{
		File f = new File(stopwordFileString);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = br.readLine();
		while(line != null){
			line = line.toLowerCase();
			stopwords.add(line);
			line = br.readLine();
		}
		br.close();
	}
	
	private static Stopword getInstance() throws IOException{
		if(instance == null){
			instance = new Stopword();
		}
		return instance;
	}
	
	protected boolean containStopword(String val){
		return stopwords.contains(val);
	}
	
	
	public static boolean isStopword(String val){
		try {
			return Stopword.getInstance().containStopword(val.toLowerCase());
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static String removeStopwords(String val){
		String[] data = val.split(" ");
		StringBuilder sb = new StringBuilder();
		for(String s : data){
			if(!isStopword(s)){
				sb.append(s).append(" ");
			}
		}
		return sb.toString().trim();
	}

//	@Override
//	public void accept(Configuration<Map<String, Object>> configuration) {
//		this.stopwordFileString = configuration.getAttribute(ConfigParams.STOP_WORD, "./stopword-list.txt");
//	}
}
