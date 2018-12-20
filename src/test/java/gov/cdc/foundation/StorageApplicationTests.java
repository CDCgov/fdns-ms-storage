package gov.cdc.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
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
import com.jayway.jsonpath.JsonPath;

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
public class StorageApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;
	@Autowired
	private MockMvc mvc;
	private JacksonTester<JsonNode> json;
	private String baseUrlPath = "/api/1.0/";

	@Before
	public void setup() {
		ObjectMapper objectMapper = new ObjectMapper();
		JacksonTester.initFields(this, objectMapper);
	}

	@Test
	public void indexPage() {
		ResponseEntity<String> response = this.restTemplate.getForEntity("/", String.class);
		assertThat(response.getStatusCodeValue()).isEqualTo(200);
		assertThat(response.getBody(), CoreMatchers.containsString("FDNS Storage Microservice"));
	}

	@Test
	public void indexAPI() {
		ResponseEntity<String> response = this.restTemplate.getForEntity(baseUrlPath, String.class);
		assertThat(response.getStatusCodeValue()).isEqualTo(200);
		assertThat(response.getBody(), CoreMatchers.containsString("version"));
	}

	@Test
	public void manageDrawer() throws IOException {
		String drawerName = Long.toString(Calendar.getInstance().getTime().getTime());
		createDrawer(drawerName);
		getDrawer(drawerName);
		List<String> drawers = getAllDrawers(-1);
		int currentSize = drawers.size();
		assertThat(drawers, hasItem(drawerName));
		deleteDrawer(drawerName);
		drawers = getAllDrawers(currentSize - 1);
		assertThat(drawers, not(hasItem(drawerName)));
		//deleteAllDrawer();
	}

	@Test
	public void manageNode() throws Exception {
		String drawerName = Long.toString(Calendar.getInstance().getTime().getTime());
		String nodeId = drawerName + ".txt";
		createDrawer(drawerName);
		List<String> nodes = getAllNodes(drawerName);
		assertThat(nodes.size()).isEqualTo(0);
		createNode(drawerName, nodeId);
		getNode(drawerName, nodeId);
		String binary = downloadNode(drawerName, nodeId);
		assertThat(binary).isEqualToIgnoringCase("Hello World");
		updateNode(drawerName, nodeId);
		binary = downloadNode(drawerName, nodeId);
		assertThat(binary).isEqualToIgnoringCase("Hello World Bis");
		getNode(drawerName, nodeId);
		deleteNode(drawerName, nodeId);
		deleteDrawer(drawerName);
		// deleteAllDrawer();
	}

	public void createDrawer(String drawerName) throws IOException {
		ResponseEntity<JsonNode> response = this.restTemplate.exchange(baseUrlPath + "/drawer/{name}", HttpMethod.PUT, null, JsonNode.class, drawerName);
		JsonContent<JsonNode> body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(body).hasJsonPathStringValue("@.name");
		assertThat(body).extractingJsonPathStringValue("@.name").isEqualTo(drawerName);
	}

	public void getDrawer(String drawerName) throws IOException {
		ResponseEntity<JsonNode> response = this.restTemplate.getForEntity(baseUrlPath + "/drawer/{name}", JsonNode.class, drawerName);
		JsonContent<JsonNode> body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(body).hasJsonPathStringValue("@.name");
		assertThat(body).extractingJsonPathStringValue("@.name").isEqualTo(drawerName);
	}

	public List<String> getAllDrawers(int expectedSize) throws IOException {
		ResponseEntity<JsonNode> response = this.restTemplate.getForEntity(baseUrlPath + "/drawer", JsonNode.class);
		JsonContent<JsonNode> body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		List<String> names = JsonPath.read(body.getJson(), "$[*].name");
		if (expectedSize >= 0)
			assertThat(names.size()).isEqualTo(expectedSize);
		return names;
	}

	public void deleteDrawer(String drawerName) throws IOException {
		ResponseEntity<JsonNode> response = this.restTemplate.exchange(baseUrlPath + "/drawer/{name}", HttpMethod.DELETE, null, JsonNode.class, drawerName);
		JsonContent<JsonNode> body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(body).hasJsonPathBooleanValue("@.success");
		assertThat(body).extractingJsonPathBooleanValue("@.success").isEqualTo(true);
	}

	public void deleteAllDrawer() throws IOException {
		List<String> drawers = getAllDrawers(-1);
		for (String drawerName : drawers) {
			this.restTemplate.exchange(baseUrlPath + "/drawer/{name}", HttpMethod.DELETE, null, JsonNode.class, drawerName);
		}
		getAllDrawers(0);
	}

	public List<String> getAllNodes(String drawerName) throws IOException {
		ResponseEntity<JsonNode> response = this.restTemplate.getForEntity(baseUrlPath + "/drawer/nodes/{name}", JsonNode.class, drawerName);
		JsonContent<JsonNode> body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		List<String> names = JsonPath.read(body.getJson(), "$[*].id");
		return names;
	}

	public void createNode(String drawerName, String id) throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "filename.txt", "text/plain", "Hello World".getBytes());
		MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart(baseUrlPath + "/node/" + drawerName + "?id=" + id);
		this.mvc.perform(builder.file(file)).andExpect(status().isCreated());
	}

	public void updateNode(String drawerName, String id) throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "filename.txt", "text/plain", "Hello World Bis".getBytes());
		MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart(baseUrlPath + "/node/" + drawerName + "?id=" + id);
		builder.with(new RequestPostProcessor() {
			@Override
			public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
				request.setMethod("PUT");
				return request;
			}
		});
		this.mvc.perform(builder.file(file)).andExpect(status().isOk());
	}

	public String downloadNode(String drawerName, String id) throws IOException {
		ResponseEntity<String> response = this.restTemplate.getForEntity(baseUrlPath + "/node/{name}/dl?id={id}", String.class, drawerName, id);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		return response.getBody();
	}

	public void getNode(String drawerName, String id) throws IOException {
		ResponseEntity<JsonNode> response = this.restTemplate.getForEntity(baseUrlPath + "/node/{name}?id={id}", JsonNode.class, drawerName, id);
		JsonContent<JsonNode> body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(body).hasJsonPathStringValue("@.id");
		assertThat(body).extractingJsonPathStringValue("@.id").isEqualTo(id);
	}

	public void deleteNode(String drawerName, String id) throws IOException {
		ResponseEntity<JsonNode> response = this.restTemplate.exchange(baseUrlPath + "/node/{name}?id={id}", HttpMethod.DELETE, null, JsonNode.class, drawerName, id);
		JsonContent<JsonNode> body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(body).hasJsonPathBooleanValue("@.success");
		assertThat(body).extractingJsonPathBooleanValue("@.success").isEqualTo(true);
	}

}
