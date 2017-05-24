package trittimo;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import edu.stanford.nlp.pipeline.Annotation;

public class NLP {
	private static final float THRESHOLD = 0.5f;
	
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.println("Usgae: NLP <input folder> <search phrases...>");
			System.err.println("Example: NLP data/presidents \"Lincoln birthday\" \"George Washington Age\"");
			System.err.println("If you would prefer to parse the documents and enter multiple queries at your leisure");
			System.err.println("Usage: NLP <input folder> -CLI");
			return;
		}
		
		File infolder = new File(args[0]);
		
		if (!infolder.isDirectory()) {
			System.err.println("The input folder must exist and be a directory");
			return;
		}
		
		if (args[1].equals("-CLI")) {
			CLI(infolder);
		} else {
			analyze(infolder, args);
		}
	}

	private static void analyze(File infolder, String[] args) {
		List<Tuple<String, Annotation>> queries = new ArrayList<>();
		for (int i = 1; i < args.length; i++) {
			System.out.println("Analyzing query: " + args[i]);
			mute();
			queries.add(new Tuple<>(args[i], Preprocessor.parseQuery(args[i])));
			unmute();
		}
		

		List<Tuple<String, List<Annotation>>> raw = new ArrayList<>();
		for (File f : infolder.listFiles()) {
			System.out.println("Analyzing document: " + f.getName());
			mute();
			raw.add(new Tuple<>(f.getName(), Preprocessor.loadFile(f.toPath().toString())));
			unmute();
		}
		
		
		for (Tuple<String, List<Annotation>> datum : raw) {
			for (Tuple<String, Annotation> query : queries) {
				System.out.print("Searching " + datum.first + " for phrase '" + query.first + "': ");
				mute();
				for (Annotation a : datum.second) {
					if (DocumentSearcher.search(a, query.second, THRESHOLD)) {
						unmute();
						System.out.println("true => phrase exists");
						break;
					} else {
						unmute();
						System.out.println("false => phrase does not exist");
					}
				}
			}
		}
	}
	
	private static void CLI(File infolder) {
		List<Tuple<String, List<Annotation>>> raw = new ArrayList<>();
		for (File f : infolder.listFiles()) {
			System.out.println("Analyzing document: " + f.getName());
			mute();
			raw.add(new Tuple<>(f.getName(), Preprocessor.loadFile(f.toPath().toString())));
			unmute();
		}
		
		Scanner s = new Scanner(System.in);
		while (true) {
			System.out.println("Type 'exit' to quit"); 
			System.out.print("Query: ");
			String query = s.nextLine();
			if (query.equals("exit")) {
				break;
			}
			
			for (Tuple<String, List<Annotation>> datum : raw) {
				System.out.print("Searching " + datum.first + " for phrase '" + query + "': ");
				mute();
				for (Annotation a : datum.second) {
					if (DocumentSearcher.search(a, Preprocessor.parseQuery(query), THRESHOLD)) {
						unmute();
						System.out.println("true => phrase exists");
						break;
					} else {
						unmute();
						System.out.println("false => phrase does not exist");
					}
				}
			}
		}
		s.close();
	}
	
	private static PrintStream out = System.out;
	private static PrintStream err = System.err;
	private static PrintStream silenced = new PrintStream(new OutputStream() {public void write(int b) throws IOException {}});
	
	private static void mute() {
//		System.setErr(silenced);
//		System.setOut(silenced);
	}
	
	private static void unmute() {
		System.setOut(out);
		System.setErr(err);
	}
}
