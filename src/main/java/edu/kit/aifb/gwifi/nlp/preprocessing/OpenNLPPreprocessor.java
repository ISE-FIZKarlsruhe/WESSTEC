package edu.kit.aifb.gwifi.nlp.preprocessing;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.kit.aifb.gwifi.util.Position;
import edu.kit.aifb.gwifi.util.nlp.Language;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.util.Span;

// support NER for Spanish
public class OpenNLPPreprocessor implements NLPPreprocessor {

	private NameFinderME locationFinder;
	private NameFinderME personFinder;
	private NameFinderME organizationFinder;
	private NameFinderME miscFinder;
	public OpenNLPPreprocessor(String configFile, String langstr) {
		this(configFile,Language.getLanguage(langstr));
	}
	
	public OpenNLPPreprocessor(String configFile, Language lang) {
		Properties properties = new Properties();
		try {
			FileInputStream inputFile = new FileInputStream(configFile);
			properties.load(inputFile);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (lang.equals(Language.ES)) {
			InputStream locationIn = null;
			InputStream miscIn = null;
			InputStream personIn = null;
			InputStream organizationIn = null;
			try {
				locationIn = new FileInputStream(properties.getProperty("spanishNERLocation"));
				miscIn = new FileInputStream(properties.getProperty("spanishNERMisc"));
				personIn = new FileInputStream(properties.getProperty("spanishNERPerson"));
				organizationIn = new FileInputStream(properties.getProperty("spanishNEROrganization"));
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
			try {
				TokenNameFinderModel nerLocationModel = new TokenNameFinderModel(locationIn);
				this.locationFinder = new NameFinderME(nerLocationModel);
				TokenNameFinderModel nerMiscModel = new TokenNameFinderModel(miscIn);
				this.miscFinder = new NameFinderME(nerMiscModel);
				TokenNameFinderModel nerOrganizationModel = new TokenNameFinderModel(organizationIn);
				this.organizationFinder = new NameFinderME(nerOrganizationModel);
				TokenNameFinderModel nerPersonModel = new TokenNameFinderModel(personIn);
				this.personFinder = new NameFinderME(nerPersonModel);

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (locationIn != null) {
					try {
						locationIn.close();
					} catch (IOException e) {
					}
				}
				if (miscIn != null) {
					try {
						miscIn.close();
					} catch (IOException e) {
					}
				}
				if (personIn != null) {
					try {
						personIn.close();
					} catch (IOException e) {
					}
				}
				if (organizationIn != null) {
					try {
						organizationIn.close();
					} catch (IOException e) {
					}
				}
			}
		}
	}

	@Override
	public LinkedHashMap<String, List<Position>> NEREntityAndPositions(String input) {
		LinkedHashMap<String, List<Position>> results = new LinkedHashMap<String, List<Position>>();
		String[] whitespaceTokenizerLine = SimpleTokenizer.INSTANCE.tokenize(input);

		List<Position> positions = getPositions(whitespaceTokenizerLine, input);
		for (Position position : positions) {
			String entity = input.substring(position.getStart(), position.getEnd());
			List<Position> entityPositions = results.get(entity);
			if (entityPositions == null) {
				entityPositions = new ArrayList<Position>();
				results.put(entity, entityPositions);
			}
			entityPositions.add(position);
		}

		return results;
	}

	@Override
	public LinkedHashMap<Position, String> NERPositionAndType(String input) {
		LinkedHashMap<Position, String> pos2type = new LinkedHashMap<Position, String>();
		TreeMap<Position, String> map = new TreeMap<Position, String>();
		
		String whitespaceTokenizerLine[] = SimpleTokenizer.INSTANCE.tokenize(input);
		List<Position> perPos = getPositions(whitespaceTokenizerLine, input, personFinder.find(whitespaceTokenizerLine));
		List<Position> locPos = getPositions(whitespaceTokenizerLine, input, locationFinder.find(whitespaceTokenizerLine));
		List<Position> orgPos = getPositions(whitespaceTokenizerLine, input, organizationFinder.find(whitespaceTokenizerLine));
		List<Position> miscPos = getPositions(whitespaceTokenizerLine, input, miscFinder.find(whitespaceTokenizerLine));
		personFinder.clearAdaptiveData();
		locationFinder.clearAdaptiveData();
		organizationFinder.clearAdaptiveData();
		miscFinder.clearAdaptiveData();
		
		for(Position pos : perPos) {
			map.put(pos, "PERSON");
		}
		for(Position pos : locPos) {
			map.put(pos, "LOCATION");
		}
		for(Position pos : orgPos) {
			map.put(pos, "ORGANIZATION");
		}
		for(Position pos : miscPos) {
			map.put(pos, "MISC");
		}
		for(Position pos : map.keySet()) {
			pos2type.put(pos, map.get(pos));
		}
		
		return pos2type;
	}

	@Override
	public List<Position> NERPosition(String input) {
		String whitespaceTokenizerLine[] = SimpleTokenizer.INSTANCE.tokenize(input);
		return getPositions(whitespaceTokenizerLine, input);
	}
	
	private List<Position> getPositions(String[] whitespaceTokenizerLine, String input) {
		List<Position> results = new ArrayList<Position>();
		
		results.addAll(getPositions(whitespaceTokenizerLine, input, personFinder.find(whitespaceTokenizerLine)));
		results.addAll(getPositions(whitespaceTokenizerLine, input, locationFinder.find(whitespaceTokenizerLine)));
		results.addAll(getPositions(whitespaceTokenizerLine, input, organizationFinder.find(whitespaceTokenizerLine)));
		results.addAll(getPositions(whitespaceTokenizerLine, input, miscFinder.find(whitespaceTokenizerLine)));
		personFinder.clearAdaptiveData();
		locationFinder.clearAdaptiveData();
		organizationFinder.clearAdaptiveData();
		miscFinder.clearAdaptiveData();

		Collections.sort(results);
		return results;
	}
	
	private List<Position> getPositions(String[] whitespaceTokenizerLine, String input, Span[] names) {
		List<Position> results = new ArrayList<Position>();
		
		int index = 0;
		for (Span span : names) {
			String entityPattern = "";
			for (int i = span.getStart(); i < span.getEnd(); i++) {
				entityPattern = entityPattern + whitespaceTokenizerLine[i] + "\\s+";
			}
			entityPattern = entityPattern.substring(0, entityPattern.length()-3);
			
			int start = -1;
			int end = -1;
			Pattern pattern = Pattern.compile(entityPattern);
			Matcher matcher = pattern.matcher(input);
			if (matcher.find()) {
				start = matcher.start();
				end = matcher.end();
			}
			if (start != -1 && end != -1) {
				Position position = new Position(start+index, end+index);
				results.add(position);
				input = input.substring(end);
				index += end;
			}
		}

		return results;
	}
	
	@Override
	public String POSTagging(String input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LinkedHashMap<Position, String> POSTaggingPositionAndTag(String input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String segmentation(String input) {
		return input;
	}

	@Override
	public List<Position> segmentationPosition(String input) {
		// TODO Auto-generated method stub
		return null;
	}

	public static void main(String[] args) {
		OpenNLPPreprocessor preprocessor = new OpenNLPPreprocessor("configs/NLPConfig.properties", Language.ES);

//		String source = "La Copa\t\t   del Rey ya tiene a su primer semifinalista: el Real Madrid. El conjunto blanco ha dejado en el camino al Espanyol, al que ha vuelto a derrotar 1-0, el mismo resultado que en la ida y en Liga. Suficiente para conseguir su pase a la penúltima ronda del torneo del K.O. [Datos y estadísticas del Real Madrid 1 - Espanyol 0]";
		String source = "REGLAMENTO CEE EURATOM CECA 261/68 DEL CONSEJO de 29 de febrero de 1968 por el que se modifica el Reglamento 423/67/CEE 6/67/Euratom del Consejo de 25 de julio de 1967 por el que se establece el régimen pecuniario de los miembros de las Comisiones de la CEE de la CEEA asi como de la Alta Autoridad que no hayan sido nombrados miembros de la Comision unica de las Comunidades Europeas EL CONSEJO DE LAS COMUNIDADES EUROPEAS Visto el Tratado por el que se constituye un Consejo unico una Comision unica de las Comunidades Europeas en particular su articulo 34 Considerando que corresponde al Consejo establecer el régimen pecuniario de los antiguos miembros de la Alta Autoridad de las Comisiones de la Comunidad Economica Europea de la Comunidad Europea de la Energia Atomica que habiendo cesado en sus funciones no hubieran sido nombrados miembros de la Comision HA ADOPTADO EL PRESENTE REGLAMENTO Articulo El articulo del Reglamento 423/67/CEE 6/67/Euratom del Consejo de 25 de julio de 1967 por el que se establece el régimen pecuniario de los miembros de las Comisiones de la CEE de la CEEA asi como de la Alta Autoridad que no hayan sido nombrados miembros de la Comision unica de las Comunidades Europeas sera completado con efecto desde el de enero de 1968 con un tercer parrafo redactado como sigue %quot% No obstante lo dispuesto en el articulo del Reglamento 422/67/Euratom la pension de los antiguos miembros de la Alta Autoridad de las Comisiones de la Comunidad Economica Europea de la Comunidad Europea de la Energia Atomica que figuran en el articulo que hayan desempenado sus funciones durante dos anos como minimo no podra ser inferior al 15 del ultimo sueldo base percibido %quot% Articulo El presente Reglamento entrara en vigor el dia siguiente al de su publicacion en el Diario Oficial de las Comunidades Europeas El presente Reglamento sera obligatorio en todos sus elementos directamente aplicable en cada Estado miembro Hecho en Bruselas el 29 de febrero de 1968 Por el Consejo El Presidente COUVE de MURVILLE DO 152 de 13 1967 DO 187 de 1967";

		
		System.out.println(preprocessor.NERPosition(source) + "\n");
		System.out.println(preprocessor.NERPositionAndType(source) + "\n");
		
		LinkedHashMap<String, List<Position>> results = preprocessor.NEREntityAndPositions(source);
		for (String entity : results.keySet()) {
			List<Position> positions = results.get(entity);
			System.out.println("Entity: " + entity);
			for (Position pos : positions) {
				System.out.println("Position: " + pos + ",\t" + source.substring(pos.getStart(), pos.getEnd()));
			}
		}
	}

}
