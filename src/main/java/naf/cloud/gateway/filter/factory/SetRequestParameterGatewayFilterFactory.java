package naf.cloud.gateway.filter.factory;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractNameValueGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.util.UriComponentsBuilder;

import io.jsonwebtoken.Claims;

/**
 * @author dyg
 */
public class SetRequestParameterGatewayFilterFactory extends AbstractNameValueGatewayFilterFactory {

	final static Logger log = LoggerFactory.getLogger(SetRequestParameterGatewayFilterFactory.class);
	final static Pattern P_JWTVAR = Pattern.compile("\\{jwt:(.*)\\}");
	final static Pattern P_WXVAR = Pattern.compile("\\{wx:(.*)\\}");

	@Override
	public GatewayFilter apply(NameValueConfig config) {
		return (exchange, chain) -> {
			URI uri = exchange.getRequest().getURI();
			String query = uri.getRawQuery();
			if(query != null) {
				// TODO: 清空已有参数
				String regex = String.format("(?i)%s=([^&]*)", config.getName());
				query = query.replaceAll(regex, "").replaceAll("&&", "&");
				if (query.startsWith("&")) {
					query = query.substring(1);
				} else if (query.endsWith("&")) {
					query = query.substring(0, query.length() - 1);
				}
			} else {
				query = "";
			}
			

			// TODO: 处理参数值, 支持Jwt Claim变量 ${jwt:xxx}
			String value = config.getValue();
			if (value != null && !value.isEmpty()) {
				value = patternReplace(value, P_JWTVAR, 
						(Claims) exchange.getAttributes().get(JwtParserGatewayFilterFactory.JWT_CLAIMS_ATTRIBUTE));
				value = patternReplace(value, P_WXVAR, 
						(Claims) exchange.getAttributes().get(WeixinTokenGatewayFilterFactory.WEIXIN_CLAIMS_ATTRIBUTE));
			}

			// TODO: 生成新的query
			String part = "";
			if (value != null && !value.isEmpty()) {
				part = String.format("%s=%s", config.getName(), value);
			}
			if (!part.isEmpty() && !query.isEmpty()) {
				query += "&" + part;
			} else if (query.isEmpty()) {
				query = part;
			}

			try {
				URI newUri = UriComponentsBuilder.fromUri(uri).replaceQuery(query).build(true).toUri();

				ServerHttpRequest request = exchange.getRequest().mutate().uri(newUri).build();

				return chain.filter(exchange.mutate().request(request).build());
			} catch (RuntimeException ex) {
				throw new IllegalStateException("Invalid URI query: \"" + query.toString() + "\"");
			}
		};
	}

	String patternReplace(String value, Pattern p, Claims claims) {
		Matcher m = p.matcher(value);
		if (m.matches()) {
			try {
				String key = m.group(1);
				if (claims != null && claims.containsKey(key)) {
					value = claims.get(key, String.class);
				} else {
					value = null;
				}
			} catch (Throwable e) {
				log.warn("extract {} fail: {}", value, e.getMessage());
				if(log.isDebugEnabled()) e.printStackTrace();
				value = null;
			}
		}
		return value;
	}
	
}
