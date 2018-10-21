package naf.cloud.gateway.handler.predicate;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

/**
 * @author dyg
 */
public class JwtRoutePredicateFactory extends AbstractRoutePredicateFactory<JwtRoutePredicateFactory.Config>{
	public static final String NAME_KEY = "name";
	public static final String VALUE_KEY = "value";
	public static final String HEADER_KEY = "Authorization";
	public static final String JWT_CLAIMS_ATTRIBUTE = "NafGateway.jwtClaims";

	
	@Value("${jwt.secret}")
	private String secret;

	public JwtRoutePredicateFactory() {
		super(Config.class);
	}

	@Override
	public List<String> shortcutFieldOrder() {
		return Arrays.asList(NAME_KEY, VALUE_KEY);
	}

	@Override
	public Predicate<ServerWebExchange> apply(Config config) {
		return exchange -> {
			List<String> values = exchange.getRequest().getHeaders().get(HEADER_KEY);
			if(values == null || values.size() == 0)
				return config.getName() == null;
			
			try{
				Jws<Claims> jws = Jwts.parser().
						setSigningKey(secret.getBytes())
						.parseClaimsJws(values.get(0));
				
				Claims claims = jws.getBody();
				exchange.getAttributes().put(JWT_CLAIMS_ATTRIBUTE, claims);
				
				if("issuer".equalsIgnoreCase(config.getName())) {
					return claims.getIssuer() != null && config.getValue().equalsIgnoreCase(claims.getIssuer());
				} else if ("subject".equalsIgnoreCase(config.getName())){
					return claims.getSubject() != null && config.getValue().equalsIgnoreCase(claims.getSubject());
				} else if (config.getName() != null){
					String val = (String) claims.get(config.getName());
					return val != null && val.equalsIgnoreCase(config.getValue());
				}
				return true;
			}catch(ExpiredJwtException ex){
				return false;
			}	
		};
	}

	public static class Config {
		private String name;
		private String value;
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	}
	
}
