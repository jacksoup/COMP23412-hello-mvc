package hello.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import hello.Hello;
import hello.dao.GreetingService;
import hello.entities.Greeting;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Hello.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GreetingControllerRestTest {

	private MockMvc mvc;

	@Autowired
	private RequestMappingHandlerAdapter adapter;

	@Mock
	private Greeting greeting;

	@Mock
	private GreetingService greetingService;

	@InjectMocks
	private GreetingControllerRest greetingController;

	@Before
	public void setup() {
		List<HttpMessageConverter<?>> converters = adapter.getMessageConverters();
		HttpMessageConverter<?> a[] = new HttpMessageConverter<?>[converters.size()];

		MockitoAnnotations.initMocks(this);
		mvc = MockMvcBuilders.standaloneSetup(greetingController).setMessageConverters(converters.toArray(a)).build();
	}

	@Test
	public void getEmptyGreetingsList() throws Exception {
		when(greetingService.findAll()).thenReturn(Collections.<Greeting> emptyList());

		mvc.perform(MockMvcRequestBuilders.get("/greeting").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk()).andExpect(handler().methodName("list"))
		.andExpect(jsonPath("$.length()", equalTo(1)))
		.andExpect(jsonPath("$._links.self.href", endsWith("/greeting")));
	}

	@Test
	public void getGreetingsList() throws Exception {
		Greeting g = new Greeting("%s");
		when(greetingService.findAll()).thenReturn(Collections.<Greeting> singletonList(g));

		mvc.perform(MockMvcRequestBuilders.get("/greeting").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk()).andExpect(handler().methodName("list"))
		.andExpect(jsonPath("$.length()", equalTo(2)))
		.andExpect(jsonPath("$._links.self.href", endsWith("/greeting")))
		.andExpect(jsonPath("$._embedded.greetings.length()", equalTo(1)));
	}

	@Test
	public void getGreeting() throws Exception {
		Greeting g = new Greeting("%s");
		when(greetingService.findOne(1)).thenReturn(g);

		mvc.perform(MockMvcRequestBuilders.get("/greeting/1").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
		.andExpect(handler().methodName("greeting")).andExpect(jsonPath("$.template", equalTo("%s")));
	}

	@Test
	public void getNewGreeting() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/greeting/new").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isNotAcceptable()).andExpect(handler().methodName("newGreeting"));
	}

	@Test
	public void postGreeting() throws Exception {
		ArgumentCaptor<Greeting> arg = ArgumentCaptor.forClass(Greeting.class);

		mvc.perform(MockMvcRequestBuilders.post("/greeting").contentType(MediaType.APPLICATION_JSON)
				.content("{ \"template\": \"Howdy, %s!\" }").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isCreated()).andExpect(content().string(""))
		.andExpect(header().string("Location", containsString("/greeting/")))
		.andExpect(handler().methodName("createGreeting"));

		verify(greetingService).save(arg.capture());
		assertThat("Howdy, %s!", equalTo(arg.getValue().getTemplate()));
	}

	@Test
	public void postBadGreeting() throws Exception {
		mvc.perform(MockMvcRequestBuilders.post("/greeting").contentType(MediaType.APPLICATION_JSON)
				.content("{ \"template\": \"no placeholder\" }").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
		.andExpect(handler().methodName("createGreeting"));

		verify(greetingService, never()).save(greeting);
	}

	@Test
	public void postLongGreeting() throws Exception {
		mvc.perform(MockMvcRequestBuilders.post("/greeting").contentType(MediaType.APPLICATION_JSON)
				.content("{ \"template\": \"abcdefghij %s klmnopqrst uvwxyz\" }").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
		.andExpect(handler().methodName("createGreeting"));

		verify(greetingService, never()).save(greeting);
	}

	@Test
	public void postEmptyGreeting() throws Exception {
		mvc.perform(MockMvcRequestBuilders.post("/greeting").contentType(MediaType.APPLICATION_JSON)
				.content("{ \"template\": \"\" }").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
		.andExpect(handler().methodName("createGreeting"));

		verify(greetingService, never()).save(greeting);
	}
}
