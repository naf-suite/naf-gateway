package naf.cloud.gateway.filter.factory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacSigner;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

/**
 * @author dyg
 */
public class JwtParserGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtParserGatewayFilterFactory.Config> {

	final static Logger log = LoggerFactory.getLogger(JwtParserGatewayFilterFactory.class);

	public static final String HEADER_AUTH = "Authorization";
	public static final String HEADER_TENANT = "X-Tenant";
	public static final String HEADER_USERID = "X-Userid";
	public static final String HEADER_ROLE = "X-Role";
	public static final String HEADER_TAGS = "X-Tags";
	public static final String JWT_CLAIMS_ATTRIBUTE = "NafGateway.jwtClaims";

	public static final String IGNORE_KEY = "ignore";

	@Value("${jwt.secret}")
	private String secret;

	public JwtParserGatewayFilterFactory() {
		super(Config.class);
	}

	@Override
	public List<String> shortcutFieldOrder() {
		return Arrays.asList(IGNORE_KEY);
	}

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			// TODO: 检查是否忽略uri
			URI uri = exchange.getRequest().getURI();
			if (config.getIgnore() != null && uri.getPath().matches(config.getIgnore())) {
				return chain.filter(exchange.mutate().build());
			}

			List<String> values = exchange.getRequest().getHeaders().get(HEADER_AUTH);
			if (values == null || values.size() == 0) {
				log.warn("Jwt token not found!");
				exchange.getResponse().beforeCommit(() -> {
					exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
					return Mono.empty();
				});
				return exchange.getResponse().setComplete();
				//return chain.filter(exchange);
			}
			String token = values.get(0);
			try {
				byte[] key = Arrays.copyOf(secret.getBytes(), 32);
				Jws<Claims> jws = Jwts.parser()
						.setSigningKey(key)
						.parseClaimsJws(token);

				Claims claims = jws.getBody();
				exchange.getAttributes().put(JWT_CLAIMS_ATTRIBUTE, claims);
				log.debug("Jwt claims: {}", claims);
				String subject = claims.getSubject();
				String[] tokens = subject.split("@", 2);
				String userid = tokens[0];
				String tenant = tokens.length>1?tokens[1]:null;

				ServerHttpRequest request = exchange.getRequest().mutate()
						.headers(httpHeaders -> {
							httpHeaders.set(HEADER_TENANT, tenant);
							httpHeaders.set(HEADER_USERID, userid);
							httpHeaders.set(HEADER_ROLE, claims.get("role", String.class));
							@SuppressWarnings("unchecked")
							ArrayList<String> tags = claims.get("tags", ArrayList.class);
							if(tags != null) {
								httpHeaders.set(HEADER_TAGS, String.join(",", tags));
							}
						}).build();
				return chain.filter(exchange.mutate().request(request).build());
			} catch (ExpiredJwtException ex) {
				log.warn("Jwt token expired");
				exchange.getResponse().beforeCommit(() -> {
					exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
					return Mono.empty();
				});
				return exchange.getResponse().setComplete();
			}
		};

	}

	// @Validated
	public static class Config {
		// @NotEmpty
		private String ignore;

		public String getIgnore() {
			return ignore;
		}

		public void setIgnore(String ignore) {
			this.ignore = ignore;
		}

	}

	public static void doInit() {
		String secret = "Ziyouyanfa!@#";
		byte[] key = Arrays.copyOf(secret.getBytes(), 32);
		new MacSigner(SignatureAlgorithm.HS256, key); // 这个操作比较耗时

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR_OF_DAY, 1);
		String signed = Jwts.builder().setIssuer("master").setSubject("platform").setExpiration(cal.getTime())
				.claim("userid", "admin").claim("name", "管理员").signWith(Keys.hmacShaKeyFor(key)).compact();
		Jwts.parser().setSigningKey(key).parseClaimsJws(signed);

	}
}
