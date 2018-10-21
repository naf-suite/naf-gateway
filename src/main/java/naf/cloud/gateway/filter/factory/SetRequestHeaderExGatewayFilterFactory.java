package naf.cloud.gateway.filter.factory;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.addOriginalRequestUrl;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractNameValueGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.util.UriTemplate;
import org.springframework.web.util.pattern.PathPattern.PathMatchInfo;

/**
 * @author dyg
 */
public class SetRequestHeaderExGatewayFilterFactory extends AbstractNameValueGatewayFilterFactory {

	public static final String TEMPLATE_KEY = "template";

	@Override
	public GatewayFilter apply(NameValueConfig config) {
		return (exchange, chain) -> {
			UriTemplate uriTemplate = new UriTemplate(config.getValue());

			PathMatchInfo variables = exchange.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
			ServerHttpRequest req = exchange.getRequest();
			addOriginalRequestUrl(exchange, req.getURI());
			Map<String, String> uriVariables;

			if (variables != null) {
				uriVariables = variables.getUriVariables();
			} else {
				uriVariables = Collections.emptyMap();
			}

			URI uri = uriTemplate.expand(uriVariables);
			String newPath = uri.getRawPath();

			ServerHttpRequest request = exchange.getRequest().mutate()
					.headers(httpHeaders -> httpHeaders.set(config.getName(), newPath))
					.build();

			return chain.filter(exchange.mutate().request(request).build());
		};
		
	}
}
