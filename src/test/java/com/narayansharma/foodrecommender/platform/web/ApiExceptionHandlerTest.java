package com.narayansharma.foodrecommender.platform.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ApiExceptionHandlerTest.TestController.class)
class ApiExceptionHandlerTest {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsStableValidationErrorContract() throws Exception {
		mockMvc.perform(post("/test")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.path").value("/test"))
				.andExpect(jsonPath("$.fieldViolations[0].field").value("name"));
	}

	@Test
	void hidesUnexpectedExceptionDetails() throws Exception {
		mockMvc.perform(post("/test/failure"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.message").value("The request could not be completed."));
	}

	@Test
	void returnsNotFoundForUnknownRoute() throws Exception {
		mockMvc.perform(get("/missing"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@RestController
	@RequestMapping("/test")
	static class TestController {
		@PostMapping
		void validate(@Valid @RequestBody TestRequest request) {
		}

		@PostMapping("/failure")
		void fail() {
			throw new IllegalStateException("sensitive detail");
		}
	}

	record TestRequest(@NotBlank String name) {
	}
}
