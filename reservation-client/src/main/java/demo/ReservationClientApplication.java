package demo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;



@EnableZuulProxy
@EnableBinding(Source.class)
//@EnableCircuitBreaker
@EnableHystrix
@EnableDiscoveryClient
@SpringBootApplication
public class ReservationClientApplication {

	@Bean
	@LoadBalanced
	RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	AlwaysSampler alwaysSampler(){
		return new AlwaysSampler();
	}
	
	public static void main(String[] args) {
			SpringApplication.run(ReservationClientApplication.class, args);
	}
}

@RestController
@RequestMapping("/reservations")
class ReservationApiGatewayRestController {

	@Autowired
	private RestTemplate restTemplate;
	
	@Output(Source.OUTPUT)
	@Autowired
	private MessageChannel messageChannel;
	

	public Collection<String> fallback() {
		return new ArrayList<>();
	}

	@RequestMapping(method = RequestMethod.POST)
	public void write(@RequestBody Reservation r) {
		System.out.println(r.getReservationName());
		this.messageChannel.send(MessageBuilder.withPayload(r.getReservationName()).build());
		// this.writer.write(r.getReservationName());
	}

	@HystrixCommand(fallbackMethod = "fallback")
	@RequestMapping(method = RequestMethod.GET, value = "/names")
	public Collection<String> names() {

		ParameterizedTypeReference<Resources<Reservation>> ptr = new ParameterizedTypeReference<Resources<Reservation>>() {
		};
		ResponseEntity<Resources<Reservation>> responseEntity = this.restTemplate
				.exchange("http://reservation-service/reservations", HttpMethod.GET, null, ptr);

		return responseEntity.getBody().getContent().stream().map(Reservation::getReservationName)
				.collect(Collectors.toList());
	}
}

class Reservation {
	private Long id;
	private String reservationName;

	
	public Long getId() {
		return id;
	}


	public void setId(Long id) {
		this.id = id;
	}


	@Override
	public String toString() {
		return "Reservation [id=" + id + ", reservationName=" + reservationName + "]";
	}


	public String getReservationName() {
		return reservationName;
	}
}
