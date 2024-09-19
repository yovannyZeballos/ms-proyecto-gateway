package pe.com.yzm.webclient;

import pe.com.yzm.core.model.HeaderRequest;
import pe.com.yzm.webclient.model.TokenResponse;
import reactor.core.publisher.Mono;

public interface AuthWebClient {
    Mono<TokenResponse> validateToken(String token, HeaderRequest headerRequest);
}
