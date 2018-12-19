package pb.limiter.locklimiter;

import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.inject.Inject;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import pb.limiter.locklimiter.endpoint.LockLimiterController;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@EnableWebMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LockLimiterApplicationTests {

	@Inject
	private LockLimiterController controller;

	@Inject
	private MockMvc mvc;

	@Test
	public void contextLoads() {
		assertNotNull(controller);
	}

	@Test
	public void test_1_CreateDatacenter() throws Exception {
		// test POST to /requests expect 2xx
		System.out.println("\n" + "POST /requests");
		this.mvc.perform(post("/requests").contentType(MediaType.APPLICATION_JSON).content(
				"[{\"dataCenterId\":\"1\", \"itemType\":\"DATACENTER\", \"attachToServerId\":\"0\", \"action\":\"CREATE\"}]"))
				.andExpect(status().is2xxSuccessful())
				.andReturn();
	}

	@Test
	public void test_2_Requests() throws Exception {
		// test POST to /requests expect 2xx
		System.out.println("\n" + "POST /requests");
		this.mvc.perform(post("/requests").contentType(MediaType.APPLICATION_JSON).content(
				"[{\"dataCenterId\":\"1\", \"itemType\":\"SERVER\", \"attachToServerId\":\"0\", \"action\":\"CREATE\"}, {\"dataCenterId\":\"1\", \"itemType\":\"SERVER\", \"attachToServerId\":\"0\", \"action\":\"CREATE\"}, {\"dataCenterId\":\"1\", \"itemType\":\"STORAGE\", \"attachToServerId\":\"0\", \"action\":\"CREATE\"}, {\"dataCenterId\":\"1\", \"itemType\":\"STORAGE\", \"itemId\":\"3\", \"attachToServerId\":\"2\", \"action\":\"UPDATE\"}]"))
				.andExpect(status().is2xxSuccessful())
				.andReturn();
	}

	@Test
	public void test_3_GetItems() throws Exception {
		// test GET to /requests expect 2xx
		System.out.println("\n" + "GET /items");
		this.mvc.perform(get("/items").accept(MediaType.APPLICATION_JSON_UTF8).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().is2xxSuccessful())
				.andReturn();
	}

	@Test
	public void test_4_Fulfillment() throws Exception {
		// test POST to /fulfillment expect 2xx
		System.out.println("\n" + "POST /fulfillment");
		this.mvc.perform(post("/fulfillment").contentType(MediaType.APPLICATION_JSON).content(
				"{\"dataCenterId\":\"1\", \"itemType\":\"SERVER\", \"itemId\":\"2\", \"attachToServerId\":\"0\", \"action\":\"CREATE\"}"))
				.andExpect(status().is2xxSuccessful())
				.andReturn();
	}
}
