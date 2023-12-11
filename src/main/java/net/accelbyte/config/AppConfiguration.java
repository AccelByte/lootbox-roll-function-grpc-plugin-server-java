package net.accelbyte.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.accelbyte.sdk.core.AccelByteConfig;
import net.accelbyte.sdk.core.AccelByteSDK;
import net.accelbyte.sdk.core.client.OkhttpClient;
import net.accelbyte.sdk.core.repository.DefaultConfigRepository;
import net.accelbyte.sdk.core.repository.DefaultTokenRefreshRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfiguration {
    @Bean
    public AccelByteSDK accelbyteSdk() {
        var configRepository = new DefaultConfigRepository();

        // Enable local token validation
        configRepository.setLocalTokenValidationEnabled(true);
        configRepository.setJwksRefreshInterval(300);
        configRepository.setRevocationListRefreshInterval(300);

        final AccelByteConfig config = new AccelByteConfig(
                new OkhttpClient(), new DefaultTokenRefreshRepository(),
                configRepository);

        return new AccelByteSDK(config);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
