package pb.limiter.locklimiter.service;

import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import pb.limiter.locklimiter.model.Request;

public interface FulfillmentServiceClient {

	@Value("${fulfillment.url}")
	String fulfillmentUrl="http://localhost:8080/fulfillment/request";
	
	@Value("${fulfillment.timeout.seconds}")
	int timeoutSeconds=5;
	
	@PostConstruct
	default void checkFulfillmentUrl() throws MalformedURLException{
		new URL(fulfillmentUrl);	
	}

	default Void send(Request request, int timeout){
		RestTemplate template= new RestTemplate();

		//		Object result = template.postForLocation(fulfillmentUrl, request);
		try {
			System.out.println("Forwarding to fulfillment: "+request);
		} catch (Exception e) {
			throw new LockLimiterException(e);
		}
		return null;
	}
}
