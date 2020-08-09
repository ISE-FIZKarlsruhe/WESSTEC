package TEST;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fiz.ise.gwifi.util.SentenceSegmentator;

public class WordCountJava8 {
	public static void main(String[] args) throws IOException {
		Path path = Paths.get("book.txt");
		List<String> worldList = Arrays.asList("hello & & & hello hello.hello ", "bye", "ciao", "bye", "ciao");

		System.err.println(SentenceSegmentator.wordCount("in the file. , ? // $$$ In this case"));
		System.err.println(SentenceSegmentator.wordCount(worldList));

	}

	private static void countWordCountFromList(List<String> worldList) {
		worldList.stream().map(line -> CharactersUtils.normalizeTrainSentence(line)).flatMap(line -> Stream.of(line.split("\\s+"))).map(String::toLowerCase)
				.collect(Collectors.toMap(word -> word, word -> 1, Integer::sum)).entrySet().stream()
				.sorted((a, b) -> a.getValue() == b.getValue() ? a.getKey().compareTo(b.getKey())
						: b.getValue() - a.getValue())
				.forEach(System.out::println);
	}

	private static void countWordCountFromText(String text) {
		Arrays.asList(text.split("\\s+")).stream().map(String::toLowerCase)
				.collect(Collectors.toMap(word -> word, word -> 1, Integer::sum)).entrySet().stream()
				.sorted((a, b) -> a.getValue() == b.getValue() ? a.getKey().compareTo(b.getKey())
						: b.getValue() - a.getValue())
				.forEach(System.out::println);
	}

	private static void countWordCountFromFile(Path path) throws IOException {
		Files.lines(path).map(line -> CharactersUtils.normalizeTrainSentence(line)).flatMap(line -> Stream.of(line.split("\\s+"))).map(String::toLowerCase)
				.collect(Collectors.toMap(word -> word, word -> 1, Integer::sum)).entrySet().stream()
				.sorted((a, b) -> a.getValue() == b.getValue() ? a.getKey().compareTo(b.getKey())
						: b.getValue() - a.getValue())
				.forEach(System.out::println);
	}

}
