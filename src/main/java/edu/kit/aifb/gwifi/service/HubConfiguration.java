package edu.kit.aifb.gwifi.service;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class HubConfiguration {

	private enum ParamName {
		proxy, langlinks, categories, unknown
	};

	private String proxyHost;
	private String proxyPort;
	private String proxyUser;
	private String proxyPassword;

	private String langlinksPath;
	private String categoriesPath;

	public String getProxyHost() {
		return proxyHost;
	}

	public String getProxyPort() {
		return proxyPort;
	}

	public String getProxyUser() {
		return proxyUser;
	}

	public String getProxyPassword() {
		return proxyPassword;
	}

	public String getLanglinksPath() {
		return langlinksPath;
	}

	public String getCategoriesPath() {
		return categoriesPath;
	}
	
	public HubConfiguration(File configFile) throws ParserConfigurationException, IOException, ClassNotFoundException,
			InstantiationException, IllegalAccessException, SAXException {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(configFile);
		doc.getDocumentElement().normalize();

		initFromXml(doc.getDocumentElement());

	}

	private void initFromXml(Element xml) throws IOException, ClassNotFoundException, InstantiationException,
			IllegalAccessException {

		NodeList children = xml.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {

			Node xmlChild = children.item(i);

			if (xmlChild.getNodeType() == Node.ELEMENT_NODE) {

				Element xmlParam = (Element) xmlChild;

				String paramName = xmlParam.getNodeName();

				switch (resolveParamName(xmlParam.getNodeName())) {

				case proxy:
					proxyHost = xmlParam.getAttribute("host");
					proxyPort = xmlParam.getAttribute("port");
					proxyUser = xmlParam.getAttribute("user");
					proxyPassword = xmlParam.getAttribute("password");
					break;
				case langlinks:
					langlinksPath = xmlParam.getAttribute("path");
					break;
				case categories:
					categoriesPath = xmlParam.getAttribute("path");
					break;
				default:
					Logger.getLogger(HubConfiguration.class).warn("Ignoring unknown parameter: '" + paramName + "'");
				}
				;
			}

		}
	}

	private ParamName resolveParamName(String name) {
		try {
			return ParamName.valueOf(name.trim());
		} catch (Exception e) {
			return ParamName.unknown;
		}
	}

}
