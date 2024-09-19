package pe.com.yzm.config;

import io.netty.resolver.DefaultAddressResolverGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.com.yzm.filter.AuthFilter;
import reactor.netty.http.client.HttpClient;

@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final AuthFilter authFilter;
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder
                .routes()
                .route(route -> route
                        .path("/ms-gestiona-proyectos/**")
                        .filters(f -> f.filter(authFilter))
                        .uri("lb://ms-gestiona-proyectos"))
                .route(route -> route
                        .path("/ms-proyecto-auth-server/**")
                        .uri("lb://ms-proyecto-auth-server"))
                .build();
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.create().resolver(DefaultAddressResolverGroup.INSTANCE);
    }
}
