package pe.com.yzm.webclient.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pe.com.yzm.core.exception.BusinessException;
import pe.com.yzm.core.logger.LoggerUtil;
import pe.com.yzm.core.model.ErrorResponse;
import pe.com.yzm.core.model.HeaderRequest;
import pe.com.yzm.core.model.HeadersConstant;
import pe.com.yzm.core.model.Wrapper;
import pe.com.yzm.util.Constant;
import pe.com.yzm.webclient.AuthWebClient;
import pe.com.yzm.webclient.model.TokenResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthWebClientImpl implements AuthWebClient {

    private final WebClient webClient;

    @Override
    public Mono<TokenResponse> validateToken(String token, HeaderRequest headerRequest) {

        log.info("Consuming service to validate token");
        LoggerUtil.logInput(headerRequest.getTransactionId(), headerRequest.toString(), token);

        return webClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path(Constant.URL_AUTH_JWT)
                        .build())
                .headers(httpHeaders -> {
                    httpHeaders.add(Constant.ACCESS_TOKEN_HEADER_NAME, token);
                    httpHeaders.add(HeadersConstant.TRANSACTION_ID, headerRequest.getTransactionId());
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> onCatchStatusError(headerRequest.getTransactionId(), "Validate JWT").apply(clientResponse))
                .bodyToMono(TokenResponse.class)
                .doOnNext(tokenResponse -> LoggerUtil.logOutput(headerRequest.getTransactionId(),
                        tokenResponse.toString(), tokenResponse.toString()))
                .doOnError(error -> log.error("Error while consuming the service Validate JWT", error));
    }

    private static Function<ClientResponse, Mono<WebClientResponseException>> onCatchStatusError(String uuid, String metodo) {
        return clientResponse -> clientResponse.bodyToMono(new ParameterizedTypeReference<Wrapper<ErrorResponse>>() {})
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(errorResponse -> log.error("{} - An error occurred {}, Body error: {}", uuid, metodo, errorResponse))
                .flatMap(errorResponse -> {
                    if (errorResponse.getData() == null) {
                        return Mono.error(BusinessException.builder()
                                .httpStatus((HttpStatus) clientResponse.statusCode())
                                .message(((HttpStatus) clientResponse.statusCode()).name())
                                .details(List.of(((HttpStatus) clientResponse.statusCode()).getReasonPhrase()))
                                .build());
                    }
                    return Mono.error(BusinessException.builder()
                            .httpStatus((HttpStatus) clientResponse.statusCode())
                            .message(errorResponse.getData().getMessage())
                            .details(errorResponse.getData().getDetails())
                            .build());
                });
    }
}
