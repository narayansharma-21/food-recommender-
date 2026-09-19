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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public ApiAccessDeniedHandler(ObjectMapper objectMapper, Clock clock) {
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
				clock.instant(),
				HttpServletResponse.SC_FORBIDDEN,
				"AUTHORIZATION_DENIED",
				"You do not have permission to access this resource.",
				request.getRequestURI(),
				MDC.get(RequestIdFilter.MDC_KEY),
				List.of()));
	}
}
