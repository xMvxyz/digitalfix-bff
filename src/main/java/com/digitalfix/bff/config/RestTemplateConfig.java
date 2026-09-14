package com.digitalfix.bff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // Apache HttpClient 5 sí soporta PATCH (el factory JDK por defecto lanza
        // "Invalid HTTP method: PATCH" al reenviar cambios de estado a los microservicios).
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }
}