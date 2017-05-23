package trittimo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import edu.stanford.nlp.pipeline.Annotation;

public class NLP {
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.println("Usgae: NLP <input folder> <search phrases...>");
			System.err.println("Example: NLP data/presidents \"Lincoln birthday\" \"George Washington Age\"");
			System.err.println("USAGE: NLP <input folder> -CLI");
			return;
		}
		File infolder = new File(args[0]);
		
		if (!infolder.isDirectory()) {
			System.err.println("The input folder must exist and be a directory");
			return;
		}
		
		if (args[1].equals("-CLI")) {
			List<Tuple<String, Annotation>> raw = new ArrayList<>();
			for (File f : infolder.listFiles()) {
				System.out.println("Analyzing document: " + f.getName());
				raw.add(new Tuple<>(f.getName(), Preprocessor.loadFile(f.toPath().toString())));
			}
			while (true) {
				Scanner s = new Scanner(System.in);
				System.out.println("Type 'exit' to quit"); 
				System.out.print("Query: ");
				String query = s.nextLine();
				if (query.equals("exit")) {
					return;
				}
				for (Tuple<String, Annotation> datum : raw) {
					System.out.print("Searching " + datum.first + " for phrase '" + query + "': ");
					if (DocumentSearcher.search(datum.second, Preprocessor.parseQuery(query), 0.5f)) {
						System.out.println("YES");
					} else {
						System.out.println("NO");
					}
				}
			}
		}
		
		List<Tuple<String, Annotation>> queries = new ArrayList<>();
		for (int i = 1; i < args.length; i++) {
			System.out.println("Analyzing query: " + args[i]);
			queries.add(new Tuple<>(args[i], Preprocessor.parseQuery(args[i])));
		}
		

		List<Tuple<String, Annotation>> raw = new ArrayList<>();
		for (File f : infolder.listFiles()) {
			System.out.println("Analyzing document: " + f.getName());
			raw.add(new Tuple<>(f.getName(), Preprocessor.loadFile(f.toPath().toString())));
		}
		
		
		for (Tuple<String, Annotation> datum : raw) {
			for (Tuple<String, Annotation> query : queries) {
				System.out.print("Searching " + datum.first + " for phrase '" + query.first + "': ");
				if (DocumentSearcher.search(datum.second, query.second, 0.5f)) {
					System.out.println("YES");
				} else {
					System.out.println("NO");
				}
			}
		}
	}
	

	public static void mute() {
		
	}
	
	public static void unmute() {
		
	}
}
