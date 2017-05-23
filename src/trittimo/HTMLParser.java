package trittimo;

import java.util.Arrays;
import java.util.List;

public class HTMLParser {
	/**
	 * 
	 * Ignores everything before the <body> tag, then removes all tags from the document. 
	 * 
	 * @param html String of HTML Data
	 */
	public static String deparse(String html) {
		final String START_BODY = "<body";
		final String EMPTY = "";
		// skip header metadata
		String body = html.substring(html.indexOf(START_BODY), html.lastIndexOf("<span class=\"mw-headline\" id=\"See_also\">See also</span>"));
		// remove tags (this hits comments too)
		body = body.replaceAll("\n", " ");
		body = body.replaceAll("(<script>.*?</script>)", EMPTY);
		body = body.replaceAll("(<style>.*?</style>)", EMPTY);
		body = body.replaceAll("(<!--.*?-->)", EMPTY);
		
		// Split it up on h2
		
		body = body.replaceAll("<.*?>", EMPTY);
		body = body.replaceAll("\\[\\d*?\\]", EMPTY);
		body = body.replaceAll("\\s+", " ").trim();
		// return clean
		return body;
	}
}
