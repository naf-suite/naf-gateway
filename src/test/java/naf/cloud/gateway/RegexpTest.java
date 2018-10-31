/**
 * 
 */
package naf.cloud.gateway;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author DYG
 *
 */
public class RegexpTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String url = "/10086a/bar";
		System.out.println(url.replaceAll("/(?<foo>.*)/(?<seg>.*)", "/${seg}/${foo}"));
		System.out.println(url.replaceAll("/[^/]+/(?<seg>.*)", "/school/${seg}"));
		System.out.println("/api/naf/login".matches(".*/login"));
		
		String host = "10183.smart.localhost";
		Pattern p = Pattern.compile("^([0-9]+).smart.*");
		Matcher m = p.matcher(host);
		if(m.matches()) {
			System.out.printf("Mateched: %s", m.group(1));
		} else {
			System.out.println("Not Matched!");
		}
		
		System.out.println();
		String regx = ".*(ticket/verify|jobfair/today)";
		System.out.println("/api/jobfair/today".matches(regx));
		System.out.println("/api/jobfair/ticket/verify".matches(regx));
	}

}
