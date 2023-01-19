package net.accelbyte;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import net.accelbyte.util.ServerAuthProvider;

@Profile("test")
@Configuration
public class TestConfiguration {

    @Bean
    @Primary
    public ServerAuthProvider testAuthorizationProvider() {
        return Mockito.mock(ServerAuthProvider.class);
    }
}
