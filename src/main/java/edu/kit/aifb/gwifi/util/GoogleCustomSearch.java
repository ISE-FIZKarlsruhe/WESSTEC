package edu.kit.aifb.gwifi.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class GoogleCustomSearch {
	private static final String[] keyList = 
		{
		 "AIzaSyCDqx7RTFLzS44C8IqZZzuRPzQIMyrF-ck",
		 "AIzaSyCat9sjzjfG7p8mDe_21VEbWADvd2asuFA",
		 "AIzaSyAbbkqRy10papOze-wFRXjjGxlfaHnFV-k",
		 "AIzaSyC2lh9MX14qgWv3gYOtb9--8IvCQlP0qJ8",
		 "AIzaSyBQI37WavUNH2RAfr5AFnMGAOY5HtDLqpg",
		 "AIzaSyAJV1IDHzSGFhDc-qq16vAjVrxwKr2r5YY",
		 "AIzaSyA53HSDtQ6am6PZg6X8MXabYIdpi1ukdkU",
		 "AIzaSyBWki11YLVxr5gTkvi80hQrEf7xayExIu0",		 
		 "AIzaSyDDLNZfLf8colf-GrvZlAMrMPctt4ODmKE",
		 "AIzaSyCIskDvuFieukeaUMT4hxdOFGZhjAmTD70",
		 "AIzaSyCMyQNQCt8otF1bYI10ymv0VIGYWLqs5aQ",
		 "AIzaSyDCxxBKLUc1d81jLxFdA8xHar2SR8xYGBY",
		 "AIzaSyBeGiEkxyIdc-wznI47803QHa3KhU0Jsxc",		  
		 "AIzaSyA2ne0a8SMVJB_lQ-3trSQG-VGfdVTu8uY",
		 "AIzaSyC22T4ojBH24KWxxeNaMizCcnW3geZOj8Y",
		 "AIzaSyB-NGF8pbYMPYJZ5436x4Al9IXlpFVBEc0",
		 "AIzaSyApy_x9mM5CtFYDz8RiTR55B11LrkD-gkI",
		 "AIzaSyAyiAM6S7Fe2kLU4CFhy98RcBsuMr1LJ9c",
		 "AIzaSyDfiVfkbHHIW3AMa2yVn8HyvJN-sMxAkSk",
		 "AIzaSyC7rX_RT2PDaD6vI8-D7LMHrFB0pVaCFjA",
		 "AIzaSyDGao2P4_VwR8aGVPHW7Bb2rnqWsWWJ_Yo",
		 "AIzaSyBESJQEKOc3Yeplf2ZxtNYOPgxE3MUxUbo",
		 "AIzaSyCBiB_AZ6a4oHE17LulaClHZTGUSbc_3F4",
		 "AIzaSyAEjk3n9Anjr-Foit9CNNb_cHbDG6ZYTwY",
		 "AIzaSyACe_KmX4O54XNhe4StxEDAkEl5D8km5qs",
		 "AIzaSyC6kBLGovlhC6dlKnNHEkLNJNifUU3vgl4",
		 "AIzaSyBZcUnY43OhuRqP0h6zI2ceF6SNVXMyb_4",
		 "AIzaSyASF21IytDKnQyJsInUyxYVpgF0iszLbQA",
		 "AIzaSyCQB0wI_2a8W769ER9T4dFwN6Xf1Je9fA0",
		 "AIzaSyDc4ng_CIpVeKleNVPumfZNN6eKyeaGA6Q",
		 "AIzaSyC1k7F6lcu5O3wcxHEdLBIyDnwrafpbW48",
		 "AIzaSyBwl-c4gvS1E_qq8dH4NywbmRibWWguSq0",
		 "AIzaSyC4Sd00klG5wAM001CGgl89KJp7FMldwyM",
		 "AIzaSyBce6ITVW8SYhknvATb7EKhsVN8SVWZN-Y",
		 "AIzaSyDFmZ59aVLiQlxhqdZLcxFZ3mZMlbd0o9o",
		 "AIzaSyCjFRxoxMooQiULMjrzxWU1-uscK51d7-A",
		 "AIzaSyB9Dc7GBbVanMbpyTfPpEh2TPCQ37o_n8g",
		 "AIzaSyA4S0x41vSkd5vk4raEABK5hyjXsPRhBv8",
		 "AIzaSyCMpQYY4pIpzvuR8iOtrfVInLVkXDZqbAQ",
		 "AIzaSyDEkHPwng-elpMF7RBlEZxGqoHNcNvsArI",
		 "AIzaSyDrU2h6iBzJBp6lq-dCe-DQv0sEfreTc70",
		 "AIzaSyCuHQQ2ifeSHQ7QjR9EoOIJiVpPGEgNyQg",
		 "AIzaSyDR_-HqF0lEE8Ae_Rws8qUlDATEOzoaAxk",
		 "AIzaSyDuW-_b9BCMLH-M2bPcH-poSCWf9XMFNHU",
		 "AIzaSyBo81G21s2CbzAbhH9kKgiRnGnL3N0KHHo",
		 "AIzaSyCQQpyN0LN_uTGmCey2cSjwtk91ZnP8YY0",
		 "AIzaSyCfB4dPLGzXWDq971vk0LAlxZA4QavYyuQ",
		 "AIzaSyCfhnCcuRpnTjSmKpIewaP6gxZD6jCEV5c",
		 "AIzaSyDxf8q_R25lv8JSQaGCdezp8l6fRm1pfgs",
		 
		 "AIzaSyAS5zZgGzu_USx7Ni0GitaEDxE3En94wkw"
		};
	//use the same DEFAULT_SEARCH_ENGINE_ID with different keys
	//	"AIzaSyAS5zZgGzu_USx7Ni0GitaEDxE3En94wkw"	from lei
	//	"AIzaSyCDqx7RTFLzS44C8IqZZzuRPzQIMyrF-ck" 	from zuo1
	//	"AIzaSyCat9sjzjfG7p8mDe_21VEbWADvd2asuFA" 	from zuo2
	//	"AIzaSyAbbkqRy10papOze-wFRXjjGxlfaHnFV-k"	from zuo3
	//	"AIzaSyC2lh9MX14qgWv3gYOtb9--8IvCQlP0qJ8" 	from wang1
	//	"AIzaSyBQI37WavUNH2RAfr5AFnMGAOY5HtDLqpg"   from wang2
	//	"AIzaSyA53HSDtQ6am6PZg6X8MXabYIdpi1ukdkU"	from wang3
	//	"AIzaSyAJV1IDHzSGFhDc-qq16vAjVrxwKr2r5YY"	from xu1
	private static final String DEFAULT_API_KEY = "AIzaSyBWki11YLVxr5gTkvi80hQrEf7xayExIu0";
//	private static final  String DEFAULT_SEARCH_ENGINE_ID = "003967192256925203115:cmvfv06nilc";	//Zh.wikipedia.org
//	private static final  String DEFAULT_SEARCH_ENGINE_ID =  "003967192256925203115:p9g9y-ptigw";	//En.wikipedi.org
	private static final  String DEFAULT_SEARCH_ENGINE_ID = "014743660526794820828:wuzcws-jnak";	//zh and en
	
	private String APIKey;
	private String searchEngineId;
	private int keyNumber = 0;
	private int searchTimes = 0;
	private static final int maxSearchTimes = 95;
	
	public GoogleCustomSearch(String APIKey, String searchEngineId) {
		if(APIKey == null || APIKey.length() == 0)
			this.APIKey = keyList[keyNumber];
		else 
			this.APIKey = APIKey;
		if(searchEngineId == null || searchEngineId.length() == 0)
			this.searchEngineId = DEFAULT_SEARCH_ENGINE_ID;
		else
			this.searchEngineId = searchEngineId;
	}
	
	public String getOutputs(String query) {
		String output = "";
		try {
			URL url = new URL("https://www.googleapis.com/customsearch/v1?key=" + APIKey + "&cx=" + searchEngineId + "&q="
					+ query + "&alt=json");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			String line;
			while ((line = br.readLine()) != null) {
				output += line + "\n";
			}
			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return output;
	}
	
	public List<String> getLinks(String query) {
		List<String> links = new ArrayList<String>(); 
		try {
			URL url = new URL("https://www.googleapis.com/customsearch/v1?key=" + APIKey + "&cx=" + searchEngineId + "&q="
					+ query + "&alt=json");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			String line, output = "";
			while ((line = br.readLine()) != null) {
				output += line + "\n";
				if (line.contains("\"link\": \"")) {
					String link = line.substring(line.indexOf("\"link\": \"") + ("\"link\": \"").length(),
							line.indexOf("\","));
					link = URLDecoder.decode(link, "UTF-8");
					links.add(link);
				}
			}
			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return links;
	}
	
	public List<String> getLinksAutoChangeKeys(String query) {
		List<String> links = new ArrayList<String>(); 
		
		if(searchTimes > maxSearchTimes)
		{
			keyNumber++;
			this.APIKey = keyList[keyNumber];
		}
		
		try {
			URL url = new URL("https://www.googleapis.com/customsearch/v1?key=" + APIKey + "&cx=" + searchEngineId + "&q="
					+ query + "&alt=json");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			String line, output = "";
			while ((line = br.readLine()) != null) {
				output += line + "\n";
				if (line.contains("\"link\": \"")) {
					String link = line.substring(line.indexOf("\"link\": \"") + ("\"link\": \"").length(),
							line.indexOf("\","));
					link = URLDecoder.decode(link, "UTF-8");
					links.add(link);
				}
				searchTimes++;
			}
			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return links;
	}

	public static void main(String[] args) throws Exception {
//		GoogleCustomSearch gcs = new GoogleCustomSearch(null, null);
		GoogleCustomSearch gcs = new GoogleCustomSearch(DEFAULT_API_KEY, null);
		//输入的时候不能带空格
//		List<String> links = gcs.getLinksAutoChangeKeys("习大大");
//		for(String link : links) {
//			System.out.println(link);
//		}
		System.out.println(gcs.getOutputs("中国石化集团公司"));
	}
	
	
}
