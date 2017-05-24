package trittimo;

import java.util.ArrayList;
import java.util.List;

public class HTMLParser {
	/**
	 * 
	 * Ignores everything before the <body> tag, then removes all tags from the document. 
	 * 
	 * @param html String of HTML Data
	 */
	public static List<String> deparse(String html) {
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
		List<String> split = new ArrayList<>();
		for (String s : body.split("<h2>")) {
			s = s.replaceAll("<h\\d>\\s*?</h\\d>", EMPTY);
			s = s.replaceAll("<div class=\"hatnote\">\\s*?</div>", EMPTY);
			s = s.replaceAll("<div id=\"toctitle\">\\s*?</div>", EMPTY);
			s = s.replaceAll("<.*?>", EMPTY);
			s = s.replaceAll("\\[\\d*?\\]", EMPTY);
			s = s.replaceAll("\\s+", " ").trim();
			split.add(s);
		}
		for (String s : split) {
			System.out.println(s);
		}
//		System.exit(0);
		// return clean
		return split;
	}
}
