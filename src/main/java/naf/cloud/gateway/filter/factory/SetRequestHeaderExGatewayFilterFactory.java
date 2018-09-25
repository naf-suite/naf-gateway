/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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
 * @author Spencer Gibb
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
