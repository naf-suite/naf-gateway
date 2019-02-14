package naf.cloud.gateway.filter.factory;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacSigner;
import io.jsonwebtoken.security.Keys;

/**
 * 用户认证Token过滤器，通过Authorization或Cookie读取认证Jwt
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
	public static final String COOKIE_TOKEN = "token";

	public static final String IGNORE_KEY = "ignore";
	public static final String REDIRECT_KEY = "redirect";
	public static final String ORDER_KEY = "order";

	@Value("${jwt.secret}")
	private String secret;

	public JwtParserGatewayFilterFactory() {
		super(Config.class);
	}

	@Override
	public List<String> shortcutFieldOrder() {
		return Arrays.asList(REDIRECT_KEY, IGNORE_KEY, ORDER_KEY);
	}

	@SuppressWarnings("unchecked")
	@Override
	public GatewayFilter apply(Config config) {
		return new OrderedGatewayFilter((exchange, chain) -> {
			// TODO: 检查是否忽略uri
			URI uri = exchange.getRequest().getURI();
			if (config.getIgnore() != null && uri.getPath().matches(config.getIgnore())) {
				return chain.filter(exchange.mutate().build());
			}
			String url = exchange.getRequest().getURI().toString();
			String method = exchange.getRequest().getMethodValue();

			List<MediaType> accepts = exchange.getRequest().getHeaders().getAccept();
			boolean isHtml = false;
			if (accepts != null && accepts.size() > 0) {
				MediaType accept = exchange.getRequest().getHeaders().getAccept().get(0);
				isHtml = accept.includes(MediaType.TEXT_HTML);
			}

			// TODO: 优先读取Head中的Authorization
			List<String> values = exchange.getRequest().getHeaders().get(HEADER_AUTH);
			String token = (values != null && values.size() > 0) ? values.get(0) : null;
			if (token != null && token.startsWith("Bearer ")) {
				token = token.substring(7);
			}

			// TODO: 读取cookie中的token
			if (token == null) {
				HttpCookie cookie = exchange.getRequest().getCookies().getFirst(COOKIE_TOKEN);
				token = cookie != null ? cookie.getValue() : null;
			}

			if (token == null || StringUtils.isEmpty(token) || "null".equalsIgnoreCase(token)
					|| "undefined".equalsIgnoreCase(token)) {
				log.debug("Jwt not found [{} {}]", method, url);
				token = null;
			}

			if (token != null) {
				try {
					byte[] key = Arrays.copyOf(secret.getBytes(), 32);
					Jws<Claims> jws = Jwts.parser().setSigningKey(key).parseClaimsJws(token);

					Claims claims = jws.getBody();
					exchange.getAttributes().put(JWT_CLAIMS_ATTRIBUTE, claims);
					log.debug("Jwt claims: {}", claims);
					String subject = claims.getSubject();
					String[] tokens = subject.split("@", 2);
					@SuppressWarnings("unused")
					String userid = tokens[0];
					String tenant = tokens.length > 1 ? tokens[1] : null;

					ServerHttpRequest request = exchange.getRequest().mutate().headers(httpHeaders -> {
						if(tenant != null) 
							httpHeaders.set(HEADER_TENANT, tenant);
						if(subject != null) 
							httpHeaders.set(HEADER_USERID, subject);
						if(claims.get("role", String.class) != null) 
							httpHeaders.set(HEADER_ROLE, claims.get("role", String.class));
						ArrayList<String> tags = claims.get("tags", ArrayList.class);
						if (tags != null) {
							httpHeaders.set(HEADER_TAGS, String.join(",", tags));
						}
					}).build();
					return chain.filter(exchange.mutate().request(request).build());
				} catch (ExpiredJwtException ex) {
					log.warn("Jwt token expired [{} {}]", method, url);
				} catch (MalformedJwtException ex) {
					log.warn("Jwt token is invalid: MalformedJwtException [{} {}]", method, url);
					if (log.isDebugEnabled()) {
						log.debug("token is: {}", token);
						ex.printStackTrace();
					}
				}
			}

			if (config.getRedirect() != null && isHtml) {
				// TODO: 重定向到授权地址
				String redirect_uri = config.getRedirect();
				if (redirect_uri.contains("?")) {
					redirect_uri += "&";
				} else {
					redirect_uri += "?";
				}
				try {
					System.out.println(uri.toString());
					redirect_uri += "redirect_uri=" + URLEncoder.encode(uri.toString(), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					log.warn("URL编码错误", e);
					redirect_uri += "recirect_uri=" + uri.toString();
				}
				setResponseStatus(exchange, HttpStatus.TEMPORARY_REDIRECT);
				final ServerHttpResponse response = exchange.getResponse();
				response.getHeaders().set(HttpHeaders.LOCATION, redirect_uri);
			} else {
				setResponseStatus(exchange, HttpStatus.UNAUTHORIZED);
			}

			return exchange.getResponse().setComplete();
		}, config.getOrder());

	}

	// @Validated
	public static class Config {
		// @NotEmpty
		private String ignore;
		// @NotEmpty
		private String redirect;
		private int order = -1;

		public String getIgnore() {
			return ignore;
		}

		public void setIgnore(String ignore) {
			this.ignore = ignore;
		}

		public String getRedirect() {
			return redirect;
		}

		public void setRedirect(String redirect) {
			this.redirect = redirect;
		}

		public int getOrder() {
			return order;
		}

		public void setOrder(int order) {
			this.order = order;
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
