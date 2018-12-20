package gov.cdc.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringRunner.class)
@SpringBootTest(
		webEnvironment = WebEnvironment.RANDOM_PORT, 
		properties = { 
					"logging.fluentd.host=fluentd",
					"logging.fluentd.port=24224",
					"repo.host=http://minio:9000",
					"repo.accessKey=minio",
					"repo.secretKey=minio123",
					"proxy.hostname=",
					"security.oauth2.resource.user-info-uri=",
					"security.oauth2.protected=",
					"security.oauth2.client.client-id=",
					"security.oauth2.client.client-secret=",
					"ssl.verifying.disable=false"
				})
@AutoConfigureMockMvc
public class StorageApplicationErrorTests {

	@Autowired
	private TestRestTemplate restTemplate;
	@Autowired
	private MockMvc mvc;
	private String baseUrlPath = "/api/1.0/";

	@Before
	public void setup() {
		ObjectMapper objectMapper = new ObjectMapper();
		JacksonTester.initFields(this, objectMapper);
	}

	@Test
	public void getDrawer() throws IOException {
		ResponseEntity<JsonNode> response = this.restTemplate.getForEntity(baseUrlPath + "/drawer/{name}", JsonNode.class, "notexist");
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	public void createDrawer() throws IOException {
		String drawerName = Long.toString(Calendar.getInstance().getTime().getTime());
		ResponseEntity<JsonNode> response = this.restTemplate.exchange(baseUrlPath + "/drawer/{name}", HttpMethod.PUT, null, JsonNode.class, drawerName);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		response = this.restTemplate.exchange(baseUrlPath + "/drawer/{name}", HttpMethod.PUT, null, JsonNode.class, drawerName);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	public void deleteDrawer() throws IOException {
		ResponseEntity<JsonNode> response = this.restTemplate.exchange(baseUrlPath + "/drawer/{name}", HttpMethod.DELETE, null, JsonNode.class, "notexist");
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	public void getAllNodes() throws IOException {
		ResponseEntity<JsonNode> response = this.restTemplate.getForEntity(baseUrlPath + "/drawer/nodes/{name}", JsonNode.class, "notexists");
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	public void createNode1() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "filename.txt", "text/plain", "Hello World".getBytes());
		MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart(baseUrlPath + "/node/" + "notexists" + "?id=" + "notexists");
		this.mvc.perform(builder.file(file)).andExpect(status().isBadRequest());
	}

	@Test
	public void createNode2() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "filename.txt", "text/plain", "Hello World".getBytes());
		MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart(baseUrlPath + "/node/" + "hl7" + "?id=");
		this.mvc.perform(builder.file(file)).andExpect(status().isBadRequest());
	}

	@Test
	public void updateNode1() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "filename.txt", "text/plain", "Hello World Bis".getBytes());
		MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart(baseUrlPath + "/node/" + "notexists" + "?id=" + "notexists");
		builder.with(new RequestPostProcessor() {
			@Override
			public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
				request.setMethod("PUT");
				return request;
			}
		});
		this.mvc.perform(builder.file(file)).andExpect(status().isBadRequest());
	}

	@Test
	public void updateNode2() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "filename.txt", "text/plain", "Hello World Bis".getBytes());
		MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart(baseUrlPath + "/node/" + "hl7" + "?id=" + "notexists");
		builder.with(new RequestPostProcessor() {
			@Override
			public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
				request.setMethod("PUT");
				return request;
			}
		});
		this.mvc.perform(builder.file(file)).andExpect(status().isBadRequest());
	}

	@Test
	public void downloadNode1() throws IOException {
		ResponseEntity<String> response = this.restTemplate.getForEntity(baseUrlPath + "/node/{name}/dl?id={id}", String.class, "notexists", "notexists");
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	public void downloadNode2() throws IOException {
		ResponseEntity<String> response = this.restTemplate.getForEntity(baseUrlPath + "/node/{name}/dl?id={id}", String.class, "hl7", new Date());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	public void getNode1() throws IOException {
		ResponseEntity<JsonNode> response = this.restTemplate.getForEntity(baseUrlPath + "/node/{name}?id={id}", JsonNode.class, "notexists", "notexists");
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	public void getNode2() throws IOException {
		ResponseEntity<JsonNode> response = this.restTemplate.getForEntity(baseUrlPath + "/node/{name}?id={id}", JsonNode.class, "hl7", new Date());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	public void deleteNode1() throws IOException {
		ResponseEntity<JsonNode> response = this.restTemplate.exchange(baseUrlPath + "/node/{name}?id={id}", HttpMethod.DELETE, null, JsonNode.class, "notexists", "notexists");
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	public void deleteNode2() throws IOException {
		ResponseEntity<JsonNode> response = this.restTemplate.exchange(baseUrlPath + "/node/{name}?id={id}", HttpMethod.DELETE, null, JsonNode.class, "hl7", new Date());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

}
