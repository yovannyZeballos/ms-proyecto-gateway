package pe.com.yzm.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import pe.com.yzm.core.model.ErrorResponse;
import pe.com.yzm.core.model.HeaderRequest;
import pe.com.yzm.core.model.HeadersConstant;
import pe.com.yzm.webclient.AuthWebClient;
import reactor.core.publisher.Mono;

import java.lang.management.MonitorInfo;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AuthFilter implements GatewayFilter {

    private final AuthWebClient authWebClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        List<String> authorizationHeaders = exchange
                .getRequest()
                .getHeaders()
                .get(HttpHeaders.AUTHORIZATION);

        if (authorizationHeaders == null || authorizationHeaders.isEmpty()) {
            return onError(exchange, "Authorization header is required");
        }

        final var tokenHeader =  authorizationHeaders.get(0);
        final var chunks = tokenHeader.split(" ");

        if (chunks.length != 2 || !chunks[0].equals("Bearer")) {
            return onError(exchange, "Authorization header must be Bearer token");
        }

        final var token = chunks[1];

        return authWebClient.validateToken(token, buildHeaderRequest(exchange))
                .map(tokenResponse -> exchange)
                /*.onErrorResume(throwable -> {
                    onError(exchange, throwable.getMessage());
                    return Mono.error(throwable);
                })*/
                .flatMap(chain::filter);

    }

    @SneakyThrows
    private Mono<Void> onError(ServerWebExchange exchange, String errorMessage) {
        final var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        final var bufferFactory = response.bufferFactory();
        final var errorResponse = new ErrorResponse();
        errorResponse.setMessage("Unauthorized");
        errorResponse.setDetails(List.of(errorMessage));
        final var errorData = new ObjectMapper().writeValueAsBytes(errorResponse);
        final var buffer = bufferFactory.wrap(errorData);
        return response.writeWith(Mono.just(buffer));
    }

    private HeaderRequest buildHeaderRequest(ServerWebExchange exchange) {
        List<String> transactionIdHeader = exchange
                .getRequest()
                .getHeaders()
                .get(HeadersConstant.TRANSACTION_ID);

        var builder = HeaderRequest.builder();

        if (transactionIdHeader == null || transactionIdHeader.isEmpty()) {
            return builder.build();
        }

        return builder
                .transactionId(transactionIdHeader.get(0))
                .build();
    }
}
