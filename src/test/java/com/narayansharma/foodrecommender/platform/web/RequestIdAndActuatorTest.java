package com.narayansharma.foodrecommender.platform.web;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RequestIdAndActuatorTest {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void preservesValidClientRequestId() throws Exception {
		mockMvc.perform(get("/actuator/health/liveness")
				.header(RequestIdFilter.REQUEST_ID_HEADER, "client-request-123"))
				.andExpect(status().isOk())
				.andExpect(header().string(RequestIdFilter.REQUEST_ID_HEADER, "client-request-123"))
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void generatesRequestIdWhenMissing() throws Exception {
		mockMvc.perform(get("/actuator/health/readiness"))
				.andExpect(status().isOk())
				.andExpect(header().string(
						RequestIdFilter.REQUEST_ID_HEADER,
						matchesPattern("[0-9a-f-]{36}")));
	}
}
