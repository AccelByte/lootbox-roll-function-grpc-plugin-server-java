package net.accelbyte;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.accelbyte.util.ServerAuthProvider;

@Configuration
public class ApplicationConfiguration {
    @Bean
    public ServerAuthProvider authorizationProvider() {
        return new ServerAuthProvider();
    }
}
