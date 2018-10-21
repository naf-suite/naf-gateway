package naf.cloud.gateway;

import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.impl.crypto.MacSigner;
import io.jsonwebtoken.security.Keys;

public class JwtTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String secret = "Ziyouyanfa!@#";
		byte[] key = Arrays.copyOf(secret.getBytes(), 32);
		new MacSigner(SignatureAlgorithm.HS256, key); // 这个操作比较耗时

		String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyaWQiOiJhZG1pbiIsIm5hbWUiOiLnrqHnkIblkZgiLCJyb2xlIjoic3VwZXIiLCJ0YWdzIjpbIueuoeeQhuWRmCIsIuaLm-iBmOezu-e7nyJdLCJpYXQiOjE1MzkzMTc1NDksImV4cCI6MTUzOTMyMTE0OSwiaXNzIjoibWFzdGVyIiwic3ViIjoicGxhdGZvcm0ifQ.MhwC6CoWgcLf9MEGWHgLg7WUNMJOokA6nRx6rDl_Dq4";
//		String payload = token.split("\\.")[1];
//		System.out.println(payload);
//		System.out.println(new String(Base64.getDecoder().decode(payload)));

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR_OF_DAY, 1);
		 String signed = Jwts.builder()
		 .setIssuer("master")
		 .setSubject("platform")
		 .setExpiration(cal.getTime())
		 .claim("userid", "admin")
		 .claim("name", "管理员")
		 .signWith(Keys.hmacShaKeyFor(key))
		 .compact();
		 Jwts.parser().setSigningKey(key).parseClaimsJws(signed);
		 System.out.println(signed);

		long start = Calendar.getInstance().getTimeInMillis();

		Jwt jwt = Jwts.parser().setSigningKey(key).parseClaimsJws(token);
		long end = Calendar.getInstance().getTimeInMillis();

		System.out.printf("first time esp: %d\n", end - start);

		start = Calendar.getInstance().getTimeInMillis();
		jwt = Jwts.parser().setSigningKey(key).parseClaimsJws(token);
		end = Calendar.getInstance().getTimeInMillis();
		System.out.printf("second time esp: %d\n", end - start);

		start = Calendar.getInstance().getTimeInMillis();
		jwt = Jwts.parser().setSigningKey(key).parseClaimsJws(token);
		end = Calendar.getInstance().getTimeInMillis();
		System.out.printf("third time esp: %d\n", end - start);

		start = Calendar.getInstance().getTimeInMillis();
		jwt = Jwts.parser().setSigningKey(key).parseClaimsJws(token);
		end = Calendar.getInstance().getTimeInMillis();
		System.out.printf("time 4 esp: %d\n", end - start);

		start = Calendar.getInstance().getTimeInMillis();
		jwt = Jwts.parser().setSigningKey(key).parseClaimsJws(token);
		end = Calendar.getInstance().getTimeInMillis();
		System.out.printf("time 5 esp: %d\n", end - start);

		Jws<Claims> jws = Jwts.parser().setSigningKey(key).parseClaimsJws(token);
		
		Claims claims = jws.getBody();
		System.out.println(jws.getHeader());
		System.out.println(claims);

	}

}
