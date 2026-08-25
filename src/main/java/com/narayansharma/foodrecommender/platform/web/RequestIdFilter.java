package com.narayansharma.foodrecommender.platform.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
	static final String MDC_KEY = "requestId";
	static final String REQUEST_ID_HEADER = "X-Request-ID";
	private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,100}");

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
		String previousRequestId = MDC.get(MDC_KEY);

		try {
			MDC.put(MDC_KEY, requestId);
			response.setHeader(REQUEST_ID_HEADER, requestId);
			filterChain.doFilter(request, response);
		} finally {
			if (previousRequestId == null) {
				MDC.remove(MDC_KEY);
			} else {
				MDC.put(MDC_KEY, previousRequestId);
			}
		}
	}

	private String resolveRequestId(String candidate) {
		if (candidate != null && VALID_REQUEST_ID.matcher(candidate).matches()) {
			return candidate;
		}
		return UUID.randomUUID().toString();
	}
}
