package cl.duoc.DisGasBackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DisGasBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(DisGasBackendApplication.class, args);
	}

}
