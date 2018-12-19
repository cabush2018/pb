package pb.limiter.locklimiter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import pb.limiter.locklimiter.service.FulfillmentServiceClient;

@SpringBootApplication
public class LockLimiterApplication {

	public static void main(String[] args) {
		SpringApplication.run(LockLimiterApplication.class, args);
	}
	
	@Bean
	@ConditionalOnMissingBean(FulfillmentServiceClient.class)
	public FulfillmentServiceClient client(){
		return new FulfillmentServiceClient() {
		};
	}
}

