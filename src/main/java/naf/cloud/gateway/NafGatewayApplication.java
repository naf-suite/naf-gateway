package naf.cloud.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

	public static void main(String[] args) {
		SpringApplication.run(NafGatewayApplication.class, args);
	}
}
