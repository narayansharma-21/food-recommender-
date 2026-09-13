package com.narayansharma.foodrecommender.identity.auth;

import com.narayansharma.foodrecommender.platform.web.ApiErrorResponse;
import com.narayansharma.foodrecommender.platform.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public ApiAuthenticationEntryPoint(ObjectMapper objectMapper, Clock clock) {
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authenticationException) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
				clock.instant(),
				HttpServletResponse.SC_UNAUTHORIZED,
				"AUTHENTICATION_REQUIRED",
				"A valid sign-in token is required.",
				request.getRequestURI(),
				MDC.get(RequestIdFilter.MDC_KEY),
				List.of()));
	}
}
