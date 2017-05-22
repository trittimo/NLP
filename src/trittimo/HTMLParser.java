package trittimo;

public class HTMLParser {
	/**
	 * 
	 * Ignores everything before the <body> tag, then removes all tags from the document. 
	 * 
	 * @param html String of HTML Data
	 */
	public static String deparse(String html) {
		final String START_BODY = "<body>";
		final String EMPTY = "";
		// skip header metadata
		String body = html.substring(html.indexOf(START_BODY) + START_BODY.length());
		// remove tags (this hits comments too)
		body = body.replaceAll("<.*?>", EMPTY);
		// return clean
		return body;
	}
}
