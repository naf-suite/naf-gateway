/**
 * 
 */
package naf.cloud.gateway;

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
	}

}
