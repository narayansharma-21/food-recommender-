package com.narayansharma.foodrecommender.identity.auth;

import com.narayansharma.foodrecommender.identity.InternalUser;
import com.narayansharma.foodrecommender.identity.UserProvisioningService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class IdentityAuthenticationFilter extends OncePerRequestFilter {
	private static final String BEARER_PREFIX = "Bearer ";

	private final IdentityTokenVerifier tokenVerifier;
	private final UserProvisioningService provisioningService;
	private final ApiAuthenticationEntryPoint authenticationEntryPoint;
	private final AdminIdentityPolicy adminIdentityPolicy;

	public IdentityAuthenticationFilter(
			IdentityTokenVerifier tokenVerifier,
			UserProvisioningService provisioningService,
			ApiAuthenticationEntryPoint authenticationEntryPoint,
			AdminIdentityPolicy adminIdentityPolicy) {
		this.tokenVerifier = tokenVerifier;
		this.provisioningService = provisioningService;
		this.authenticationEntryPoint = authenticationEntryPoint;
		this.adminIdentityPolicy = adminIdentityPolicy;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String versionedApiPrefix = request.getContextPath() + "/v1/";
		return !request.getRequestURI().startsWith(versionedApiPrefix);
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String idToken = bearerToken(request.getHeader("Authorization"));
		if (idToken == null) {
			filterChain.doFilter(request, response);
			return;
		}
		try {
			VerifiedIdentity identity = tokenVerifier.verify(idToken);
			InternalUser user = provisioningService.findOrCreate(identity);
			UserPrincipal principal = new UserPrincipal(
					user.id(), identity.provider(), identity.subject());
			List<SimpleGrantedAuthority> authorities = adminIdentityPolicy.isAdmin(
					identity.provider(), identity.subject())
					? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
					: List.of();
			SecurityContextHolder.getContext().setAuthentication(
					UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities));
			filterChain.doFilter(request, response);
		} catch (InvalidIdentityTokenException | IllegalStateException exception) {
			SecurityContextHolder.clearContext();
			authenticationEntryPoint.commence(
					request,
					response,
					new BadCredentialsException("Identity token was rejected"));
		}
	}

	private String bearerToken(String authorization) {
		if (authorization == null
				|| !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
			return null;
		}
		String token = authorization.substring(BEARER_PREFIX.length());
		return token.isBlank() ? null : token;
	}
}
