package pb.limiter.locklimiter.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import pb.limiter.locklimiter.model.Request;

@Configuration
@ComponentScan(basePackages = { "pb.limiter.locklimiter" })
public class RequestServiceTestConfiguration {

	@Bean @ConditionalOnMissingBean(FulfillmentServiceClient.class)
	public FulfillmentServiceClient fulfillmentClient() {
		return new FulfillmentServiceClient() {

			public Void send(Request request, int timeout) {
				return null;
			}
		};
	}

}
