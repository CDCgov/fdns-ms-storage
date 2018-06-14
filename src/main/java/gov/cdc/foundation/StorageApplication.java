package gov.cdc.foundation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

import gov.cdc.foundation.helper.MimeTypes;

@SpringBootApplication
@EnableResourceServer
public class StorageApplication {

	public static void main(String[] args) {
		MimeTypes.init();
		SpringApplication.run(StorageApplication.class, args);
	}

}
