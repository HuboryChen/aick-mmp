package com.aick.mmp;

import com.aick.mmp.central.CentralApplication;
import com.aick.mmp.edge.EdgeApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        // Check if a specific profile is set
        String activeProfile = System.getProperty("spring.profiles.active");
        
        if ("edge".equals(activeProfile)) {
            // Run edge node application
            EdgeApplication.main(args);
        } else if ("central".equals(activeProfile)) {
            // Run central server application
            CentralApplication.main(args);
        } else {
            // Default to central server if no profile specified
            System.setProperty("spring.profiles.active", "central");
            CentralApplication.main(args);
        }
    }

}