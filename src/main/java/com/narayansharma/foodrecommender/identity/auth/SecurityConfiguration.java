package com.narayansharma.foodrecommender.identity.auth;

import com.narayansharma.foodrecommender.identity.UserProvisioningService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {
	@Bean
	SecurityFilterChain apiSecurityFilterChain(
			HttpSecurity http,
			ApiAuthenticationEntryPoint authenticationEntryPoint,
			ObjectProvider<IdentityTokenVerifier> tokenVerifierProvider,
			UserProvisioningService provisioningService) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.httpBasic(httpBasic -> httpBasic.disable())
				.formLogin(formLogin -> formLogin.disable())
				.logout(logout -> logout.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
						.requestMatchers("/actuator/**").authenticated()
						.requestMatchers("/v1/**").authenticated()
						.anyRequest().permitAll());
		IdentityTokenVerifier tokenVerifier = tokenVerifierProvider.getIfAvailable();
		if (tokenVerifier != null) {
			http.addFilterBefore(
					new IdentityAuthenticationFilter(
							tokenVerifier,
							provisioningService,
							authenticationEntryPoint),
					UsernamePasswordAuthenticationFilter.class);
		}
		return http.build();
	}
}
