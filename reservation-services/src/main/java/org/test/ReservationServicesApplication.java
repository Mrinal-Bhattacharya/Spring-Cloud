package org.test;

import java.util.Collection;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableDiscoveryClient
@EnableBinding(Sink.class)
public class ReservationServicesApplication {

	@Bean
	CommandLineRunner runner(ReservationRepository rr) {
		return args -> {
			Stream.of("Dave", "George", "Rod", "Mattias").forEach(name -> rr.save(new Reservation(name)));
			rr.findAll().forEach(System.out::println);
		};
	}

	

	public static void main(String[] args) {
		SpringApplication.run(ReservationServicesApplication.class, args);
	}
}

@MessageEndpoint
class MessageReservationReciever {

	@ServiceActivator(inputChannel = Sink.INPUT)
	public void acceptReservation(String rn) {
		System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"+rn);
		Reservation save = this.reservationRepository.save(new Reservation(rn));
		System.out.println(save.toString());
	}

	@Autowired
	private ReservationRepository reservationRepository;
}

@RepositoryRestResource
interface ReservationRepository extends JpaRepository<Reservation, Long> {
	@RestResource(path = "by-name")
	Collection<Reservation> findByReservationName(@Param("rn") String rn);
}

@RefreshScope
@RestController
class MessageRestController {
	@Value("${message}")
	private String message;

	@RequestMapping("/message")
	String getMessage() {
		return this.message;
	}
}