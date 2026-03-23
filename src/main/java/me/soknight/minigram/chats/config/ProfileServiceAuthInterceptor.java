package me.soknight.minigram.chats.config;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

@Component
public class ProfileServiceAuthInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public @NonNull ClientHttpResponse intercept(
            @NonNull HttpRequest request,
            byte @NonNull [] body,
            @NonNull ClientHttpRequestExecution execution
    ) throws IOException {
        var authorizationHeader = extractAuthorizationHeader();
        if (authorizationHeader != null && !authorizationHeader.isBlank())
            request.getHeaders().set(HttpHeaders.AUTHORIZATION, authorizationHeader);

        return execution.execute(request, body);
    }

    private @Nullable String extractAuthorizationHeader() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes))
            return null;

        HttpServletRequest request = servletAttributes.getRequest();
        return request.getHeader(HttpHeaders.AUTHORIZATION);
    }

}
