package trittimo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class NLP {
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.println("Usgae: NLP <input folder> <search phrases...>");
			System.err.println("Example: NLP data/presidents \"Lincoln birthday\" \"George Washington Age\"");
			return;
		}
		File infolder = new File(args[0]);
		
		if (!infolder.isDirectory()) {
			System.err.println("The input folder must exist and be a directory");
			return;
		}
		
		List<String[]> rawData = new ArrayList<String[]>();
		for (File f : infolder.listFiles()) {
			String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
			if (f.getName().endsWith(".html") || f.getName().endsWith(".html")) {
				// TODO html parsing
			}
			rawData.add(new String[] {f.getName(), content});
		}
		
		
		
		Properties arguments = new Properties();
		arguments.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		
	}
}
