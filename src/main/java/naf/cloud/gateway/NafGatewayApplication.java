package naf.cloud.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import naf.cloud.gateway.filter.factory.HostToTenantGatewayFilterFactory;
import naf.cloud.gateway.filter.factory.JwtParserGatewayFilterFactory;
import naf.cloud.gateway.filter.factory.SetRequestHeaderExGatewayFilterFactory;
import naf.cloud.gateway.filter.factory.SetRequestParameterGatewayFilterFactory;
import naf.cloud.gateway.filter.factory.WeixinTokenGatewayFilterFactory;

@SpringBootApplication
@Configuration
public class NafGatewayApplication {

	final static Logger log = LoggerFactory.getLogger(NafGatewayApplication.class);

//	@Bean
//	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
//		return builder.routes()
//				.route("test", r -> r.path("/nginx/**")
//						.filters(f -> f.stripPrefix(1))
//						.uri("http://localhost:8000"))
//				.build();
//	}

	@Bean
	public SetRequestHeaderExGatewayFilterFactory setRequestHeaderExGatewayFilterFactory() {
		return new SetRequestHeaderExGatewayFilterFactory();
	}

	@Bean
	public JwtParserGatewayFilterFactory jwtParserGatewayFilterFactory() {
		return new JwtParserGatewayFilterFactory();
	}

	@Bean
	public HostToTenantGatewayFilterFactory hostToTenantGatewayFilterFactory() {
		return new HostToTenantGatewayFilterFactory();
	}

	@Bean
	public WeixinTokenGatewayFilterFactory weixinTokenGatewayFilterFactory() {
		return new WeixinTokenGatewayFilterFactory();
	}
	
	@Bean
	public SetRequestParameterGatewayFilterFactory setRequestParameterGatewayFilterFactory() {
		return new SetRequestParameterGatewayFilterFactory();
	}

//	@Bean
//	@Order(-100)
//	public GlobalFilter testFilter() {
//	    return (exchange, chain) -> {
//	        log.debug("testFilter: {}", exchange.getRequest().getURI().toString());
//			LinkedHashSet<URI> originalUris = exchange.getAttribute(GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
//	        log.debug("originalUris: {}", originalUris);
//		
//
//	        return chain.filter(exchange).then(Mono.empty());
//	    };
//	}

	public static void main(String[] args) {
		SpringApplication.run(NafGatewayApplication.class, args);
		JwtParserGatewayFilterFactory.doInit();
	}
}
