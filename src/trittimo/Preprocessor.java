package trittimo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.CorefChain.CorefMention;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;

public class Preprocessor {
	
	private static final Properties PROPS = PropertiesUtils.asProperties("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
	
	private static MessageDigest digest;
	private static StanfordCoreNLP pipeline;
	
	public static Annotation parseQuery(String query) {
		Annotation annotation = new Annotation(query);
		getPipeline().annotate(annotation);
		return annotation;
	}
	
	/**
	 * Creates a {@link edu.stanford.nlp.pipeline.Annotation Annotation} document for a file.
	 * 
	 * If a cached/serialized {@link edu.stanford.nlp.pipeline.Annotation Annotation} exists, the serialized version will be loaded instead. 
	 * This method will automatically detect if the cache/serialized version is stale by checking the MD5 of the file to be loaded against the cached version.
	 * 
	 * @param fname File to process / load from cache
	 */
	public static Annotation loadFile(String fname) {

		File infile = new File(fname);
		
		if (infile.isDirectory()) {
			throw new RuntimeException("The input file can't be a directory");
		}
		
		try {
			
			String hash = getFileHashString(infile);
			
			// check for cached copy
			String cacheFile = infile.getAbsolutePath() + ".cache-" + hash;
			File cache = new File(cacheFile);
			if (cache.exists()) {
				return loadCacheFile(cache);
			}
			
			// check to see if it's html or regular
			String ext = infile.getPath();
			ext = ext.substring(ext.lastIndexOf(".") + 1);
			boolean isHTML = ext.equalsIgnoreCase("html") || ext.equalsIgnoreCase("htm");
			
			return loadAndCacheFile(infile, hash, isHTML);
			
		} catch (Exception e) {
			// bubble up error
			throw new RuntimeException(e);
		}
		
	}

	/**
	 *  Loads the serialized {@link edu.stanford.nlp.pipeline.Annotation Annotation} from a file.
	 * @param cache The file for the cached/serialized {@link edu.stanford.nlp.pipeline.Annotation Annotation}
	 * @return An {@link edu.stanford.nlp.pipeline.Annotation Annotation} representing the document.
	 * @throws IOException If an Exception occurs in handling the IO
	 * @throws ClassNotFoundException If the serialized object's class cannot be found in the classpath at runtime.
	 */
	private static Annotation loadCacheFile(File cache) throws IOException, ClassNotFoundException {
		System.out.println("Loading Cached Object: " + cache.getName());
		FileInputStream fis = new FileInputStream(cache);
        ObjectInputStream ois = new ObjectInputStream(fis);
        Object o = ois.readObject();
        ois.close();
        fis.close();
        Annotation a = (Annotation) o;
		return a;
	}

// Test with serializing by replacing words like "he" with the object they link to
//	public static void serialize(Annotation doc) {
//		
//		Map<Integer, CorefChain> corefs = doc.get(CorefChainAnnotation.class);
//		List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
//
//	    List<String> resolved = new ArrayList<String>();
//
//	    for (CoreMap sentence : sentences) {
//
//	        List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
//
//	        for (CoreLabel token : tokens) {
//
//	            Integer corefClustId= token.get(CorefCoreAnnotations.CorefClusterIdAnnotation.class);
//	            System.out.println(token.word() +  " --> corefClusterID = " + corefClustId);
//
//
//	            CorefChain chain = corefs.get(corefClustId);
//	            System.out.println("matched chain = " + chain);
//
//
//	            if (chain==null || chain.getMentionsInTextualOrder().size() == 1) {
//	                resolved.add(token.word());
//	            } else{
//
//	                int sentINdx = chain.getRepresentativeMention().sentNum -1;
//	                CoreMap corefSentence = sentences.get(sentINdx);
//	                List<CoreLabel> corefSentenceTokens = corefSentence.get(TokensAnnotation.class);
//
//	                String newwords = "";
//	                CorefMention reprMent = chain.getRepresentativeMention();
//	                if (token.index() < reprMent.startIndex || token.index() > reprMent.endIndex) {
//
//	                    for (int i = reprMent.startIndex; i < reprMent.endIndex; i++) {
//	                        CoreLabel matchedLabel = corefSentenceTokens.get(i - 1); 
//	                        resolved.add(matchedLabel.word());
//
//	                        newwords += matchedLabel.word() + " ";
//
//	                    }
//	                }
//
//	                else {
//	                    resolved.add(token.word());
//
//	                }
//
//	                System.out.println("converting " + token.word() + " to " + newwords);
//	            }
//
//	        }
//	    }
//
//	    String resolvedStr ="";
//	    System.out.println();
//	    for (String str : resolved) {
//	        resolvedStr+=str+" ";
//	    }
//	    System.out.println(resolvedStr);
//
//	}

	/**
	 * Reads a file, creates an {@link edu.stanford.nlp.pipeline.Annotation Annotation}, and saves it to a cached file.
	 * 
	 * @param f
	 * @param hash
	 * @param isHTML
	 * @return
	 * @throws IOException
	 */
	private static Annotation loadAndCacheFile(File f, String hash, boolean isHTML) throws IOException {
		
		// parse if html
		String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
		if (isHTML) {
			content = HTMLParser.deparse(content);
		}
		
		// Create the annotation
		StanfordCoreNLP pipeline = getPipeline();
		Annotation document = new Annotation(content);
		pipeline.annotate(document);
		
		// serialize here -- CoreNLP DOESN"T SERIALIZE OBJECT CORRECTLY, THIS IS BROKEN
//		String finPath = f.getAbsolutePath();
//		String foutPath = finPath + ".cache-" + hash;
//		System.out.println("Document: ");
//		FileOutputStream fos = new FileOutputStream(foutPath);
//		ObjectOutputStream oos = new ObjectOutputStream(fos);
//		oos.writeObject(document);
//		oos.close();
//		fos.close();
//		System.out.printf("CACHING: %s --> %s%n",
//				finPath.substring(finPath.lastIndexOf(File.separator) + 1),
//				foutPath.substring(foutPath.lastIndexOf(File.separator) + 1));
		
		return document;
	}

	private static String getFileHashString(File infile) throws IOException, NoSuchAlgorithmException {
		
		// get digest
		if (digest == null) {
			digest = MessageDigest.getInstance("MD5");
		} else {
			digest.reset();
		}
		
		// compute digest
		InputStream is = new FileInputStream(infile);
		byte[] bytes = new byte[1024];
		int numBytes;
		while ((numBytes = is.read(bytes)) != -1) {
			digest.update(bytes, 0, numBytes);
		}
		is.close();
		
		// get md5 out
		byte[] output = digest.digest();
		String result = "";
		for (byte b : output) {
			result += String.format("%02x", b);
		}
		return result;
	}
	
	public static StanfordCoreNLP getPipeline() {
		if (pipeline == null)
			pipeline = new StanfordCoreNLP(PROPS);
		return pipeline;
	}
}
