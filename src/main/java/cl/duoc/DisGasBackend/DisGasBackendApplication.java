package cl.duoc.DisGasBackend;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DisGasBackendApplication {

	public static void main(String[] args) {
		// En EC2 la hora por defecto es UTC; con esto las fechas de las boletas quedan en hora de Chile.
		TimeZone.setDefault(TimeZone.getTimeZone("America/Santiago"));
		SpringApplication.run(DisGasBackendApplication.class, args);
	}

}
