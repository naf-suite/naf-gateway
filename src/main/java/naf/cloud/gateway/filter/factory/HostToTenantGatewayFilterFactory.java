package naf.cloud.gateway.filter.factory;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.constraints.NotEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.style.ToStringCreator;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.validation.annotation.Validated;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacSigner;
import io.jsonwebtoken.security.Keys;

/**
 * @author dyg
 */
public class HostToTenantGatewayFilterFactory extends AbstractGatewayFilterFactory<HostToTenantGatewayFilterFactory.Config> {

	final static Logger log = LoggerFactory.getLogger(HostToTenantGatewayFilterFactory.class);

	public static final String HEADER_AUTH = "Authorization";
	public static final String HEADER_TENANT = "X-Tenant";
	public static final String HEADER_USERID = "X-Userid";
	public static final String HEADER_ROLE = "X-Role";
	public static final String HEADER_TAGS = "X-Tags";
	public static final String JWT_CLAIMS_ATTRIBUTE = "NafGateway.jwtClaims";

	public static final String PATTERN_KEY = "pattern";

	@Value("${jwt.secret}")
	private String secret;

	public HostToTenantGatewayFilterFactory() {
		super(Config.class);
	}

	@Override
	public List<String> shortcutFieldOrder() {
		return Arrays.asList(PATTERN_KEY);
	}

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			// TODO: 提取Host
			String host = exchange.getRequest().getHeaders().getFirst("Host");
//			host = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Host");
//			if(StringUtil.isNullOrEmpty(host))
//				host = exchange.getRequest().getHeaders().getFirst("Host");

			Pattern p = Pattern.compile(config.getPattern());
			Matcher m = p.matcher(host);
			if(m.matches()) {
				String tenant = m.group(1);
				ServerHttpRequest request = exchange.getRequest().mutate()
						.headers(httpHeaders -> {
							httpHeaders.set(HEADER_TENANT, tenant);
						}).build();
				log.debug("Set X-Tenant use Host: {}", tenant);
				return chain.filter(exchange.mutate().request(request).build());
			}
			return chain.filter(exchange.mutate().build());		
		};
	}

	@Validated
	public static class Config {
		@NotEmpty
		private String pattern;

		public String getPattern() {
			return pattern;
		}

		public Config setPattern(String pattern) {
			this.pattern = pattern;
			return this;
		}

		@Override
		public String toString() {
			return new ToStringCreator(this)
					.append("pattern", pattern)
					.toString();
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
