/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sample;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.session.ExpiringSession;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.RequestContextFilter;

import sample.session.SessionDetailsFilter;

/**
 *
 * @author Rob Winch
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = HandsOnSecurityApplication.class)
@WebAppConfiguration
public class HandsOnSecurityApplicationTests {
	@Autowired
	SessionRepositoryFilter<? extends ExpiringSession> springSessionRepositoryFilter;
	@Autowired
	SessionDetailsFilter sessionDetailsFilter;
	@Autowired
	RequestContextFilter requestContextFilter;

	@Autowired
	WebApplicationContext wac;

	MockMvc mockMvc;

	@Before
	public void setup() {
		mockMvc = MockMvcBuilders
				.webAppContextSetup(wac)
				.defaultRequest(get("/").header("X-FORWARDED-FOR", "184.154.83.119").header("User-Agent", "JUnit").accept(MediaType.APPLICATION_JSON))
				.alwaysDo(print())
				.addFilters(springSessionRepositoryFilter,sessionDetailsFilter,requestContextFilter)
				.apply(springSecurity())
				.build();
	}

	@Test
	public void inboxRequiresAuthenication() throws Exception {
		mockMvc
			.perform(get("/messages/search/inbox"))
			.andExpect(status().isUnauthorized());
	}

	@WithUserDetails("rob@example.com")
	@Test
	public void inbox() throws Exception {
		mockMvc
			.perform(get("/messages/search/inbox"))
			.andExpect(status().isOk());
	}

	@Test
	public void sent() throws Exception {
		mockMvc
			.perform(get("/messages/search/sent"))
			.andExpect(status().isOk());
	}

	@Test
	public void compose() throws Exception {
		String content = "{\"text\":\"Compose Test\",\"summary\":\"Test\",\"from\":\"http://localhost/users/0\",\"toEmail\":\"rob@example.com\",\"to\":\"http://localhost/users/1\"}";
		mockMvc
			.perform(post("/messages/").content(content))
			.andExpect(status().isCreated());
	}

	@Test
	public void composeError() throws Exception {
		String content = "";
		mockMvc
			.perform(post("/messages/").content(content))
			.andExpect(status().isBadRequest());
	}

	@Test
	public void authenticateXRequestedWith() throws Exception {
		MockHttpServletRequestBuilder authenticate = get("/users/search/self")
				.header("X-Requested-With", "XMLHttpRequest");
		mockMvc
			.perform(authenticate)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email", is("rob@example.com")))
			.andExpect(header().doesNotExist("x-auth-token"))
			.andExpect(cookie().exists("SESSION"));
	}

	@Test
	public void authenticateBrowser() throws Exception {
		MockHttpServletRequestBuilder authenticate = get("/users/search/self")
				.accept(MediaType.TEXT_HTML, MediaType.ALL);
		mockMvc
			.perform(authenticate)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email", is("rob@example.com")))
			.andExpect(header().doesNotExist("x-auth-token"))
			.andExpect(cookie().exists("SESSION"));
	}

	@Test
	public void authenticateCurl() throws Exception {
		MockHttpServletRequestBuilder authenticate = get("/users/search/self")
				.accept(MediaType.ALL);
		mockMvc
			.perform(authenticate)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email", is("rob@example.com")))
			.andExpect(header().string("x-auth-token",notNullValue()))
			.andExpect(cookie().doesNotExist("SESSION"));
	}

	@Test
	public void csrfToken() throws Exception {
		mockMvc
			.perform(get("/csrf"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.parameterName", is("_csrf")))
			.andExpect(jsonPath("$.headerName", is("X-CSRF-TOKEN")))
			.andExpect(jsonPath("$.token", notNullValue()));
	}

	@Test
	public void logout() throws Exception {
		mockMvc
			.perform(post("/logout"))
			.andExpect(status().isNoContent());
	}

	@Test
	public void eveCannotAccessRobsMessage() throws Exception {
		mockMvc
			.perform(get("/messages/100"))
			.andExpect(status().isNotFound());
	}

	@Test
	public void robCanAccessRobsMessage() throws Exception {
		mockMvc
			.perform(get("/messages/100"))
			.andExpect(status().isOk());
	}
}
