package trittimo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;

public class Preprocessor {
	
	private static final Properties PROPS = PropertiesUtils.asProperties("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
	
	private static MessageDigest digest;
	
	/**
	 * 
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

	private static Annotation loadCacheFile(File cache) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Annotation loadAndCacheFile(File f, String hash, boolean isHTML) throws IOException {
		
		// parse if html
		String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
		if (isHTML) {
			content = HTMLParser.deparse(content);
		}
		
		StanfordCoreNLP pipeline = new StanfordCoreNLP(PROPS);
		Annotation document = new Annotation(content);
		pipeline.annotate(document);
		
		// serialize here
		
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
	
}
