package naf.cloud.gateway.filter.factory;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Arrays;
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

/**
 * 微信认证Token过滤器，通过Cookie读取认证Jwt
 * @author dyg
 */
public class WeixinTokenGatewayFilterFactory
		extends AbstractGatewayFilterFactory<JwtParserGatewayFilterFactory.Config> {

	final static Logger log = LoggerFactory.getLogger(WeixinTokenGatewayFilterFactory.class);

	public static final String COOKIE_TOKEN = "wxtoken";
	public static final String HEADER_OPENID = "X-OpenID";
	public static final String WEIXIN_CLAIMS_ATTRIBUTE = "NafGateway.weixinClaims";

	public static final String IGNORE_KEY = "ignore";
	public static final String REDIRECT_KEY = "redirect";
	public static final String ORDER_KEY = "order";

	@Value("${jwt.secret}")
	private String secret;
	
	public WeixinTokenGatewayFilterFactory() {
		super(JwtParserGatewayFilterFactory.Config.class);
	}

	@Override
	public List<String> shortcutFieldOrder() {
		return Arrays.asList(REDIRECT_KEY, IGNORE_KEY, ORDER_KEY);
	}

	@Override
	public GatewayFilter apply(JwtParserGatewayFilterFactory.Config config) {
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
			if(accepts != null && accepts.size() > 0) {
				MediaType accept = exchange.getRequest().getHeaders().getAccept().get(0);
				isHtml = accept.includes(MediaType.TEXT_HTML);
			}
			
			HttpCookie cookie = exchange.getRequest().getCookies().getFirst(COOKIE_TOKEN);
			String token = cookie != null ? cookie.getValue() : null;
			if (token == null || StringUtils.isEmpty(token)
					|| "null".equalsIgnoreCase(token) || "undefined".equalsIgnoreCase(token)) {
				log.warn("Weixin Jwt not found [{} {}]", method, url);
				token = null;
				// throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED,
				// "用户未认证");
			}

			if (token != null) {
				try {
					byte[] key = Arrays.copyOf(secret.getBytes(), 32);
					Jws<Claims> jws = Jwts.parser().setSigningKey(key).parseClaimsJws(token);

					Claims claims = jws.getBody();
					exchange.getAttributes().put(WEIXIN_CLAIMS_ATTRIBUTE, claims);
					log.debug("Weixin claims: {}", claims);
					String subject = claims.getSubject();
					String issuer = claims.getIssuer();
					if("weixin".equalsIgnoreCase(issuer)) {
						ServerHttpRequest request = exchange.getRequest().mutate().headers(httpHeaders -> {
							httpHeaders.set(HEADER_OPENID, subject);
						}).build();
						return chain.filter(exchange.mutate().request(request).build());
					} else {
						log.warn("Jwt issuer invalid: {} [{} {}]", issuer, method, url);
					}
				} catch (ExpiredJwtException ex) {
					log.warn("Weixin Jwt expired [{} {}]", method, url);
				} catch (MalformedJwtException ex) {
					log.warn("Weixin Jwt is invalid: MalformedJwtException [{} {}]", method, url);
					if (log.isDebugEnabled()) {
						log.debug(token);
						ex.printStackTrace();
					}
				}
			}
			
			if(config.getRedirect() != null && isHtml) {
				// TODO: 重定向到授权地址
				String redirect_uri = config.getRedirect();
				if(redirect_uri.contains("?")) {
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

}
