package me.soknight.minigram.chats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {
        UserDetailsServiceAutoConfiguration.class,  // user details aren't needed
})
public class ServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }

}
