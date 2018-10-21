package naf.cloud.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import naf.cloud.gateway.filter.factory.HostToTenantGatewayFilterFactory;
import naf.cloud.gateway.filter.factory.JwtParserGatewayFilterFactory;
import naf.cloud.gateway.filter.factory.SetRequestHeaderExGatewayFilterFactory;

@SpringBootApplication
@Configuration
public class NafGatewayApplication {

	
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
	@Order(-1)
	public JwtParserGatewayFilterFactory jwtParserGatewayFilterFactory() {
		return new JwtParserGatewayFilterFactory();
	}

	@Bean
	@Order(-2)
	public HostToTenantGatewayFilterFactory hostToTenantGatewayFilterFactory() {
		return new HostToTenantGatewayFilterFactory();
	}

	public static void main(String[] args) {
		SpringApplication.run(NafGatewayApplication.class, args);
		JwtParserGatewayFilterFactory.doInit();
	}
}
