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
		if (args.length < 3) {
			System.err.println("Usgae: NLP <input folder> <output file> <search phrases...>");
			System.err.println("Example: NLP data/presidents output.txt \"Lincoln birthday\" \"George Washington Age\"");
			return;
		}
		File infolder = new File(args[0]);
		File outfile = new File(args[1]);
		
		if (!infolder.isDirectory()) {
			System.err.println("The input folder must exist and be a directory");
			return;
		}
		
		List<String> rawData = new ArrayList<String>();
		for (File f : infolder.listFiles()) {
			rawData.add(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
		}
		
		Properties arguments = new Properties();
		arguments.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		
	}
}
