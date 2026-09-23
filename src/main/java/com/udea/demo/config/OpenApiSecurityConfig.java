package com.udea.demo.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecuritySchemes({
    @SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP,
            scheme = "bearer", bearerFormat = "opaque"),
    @SecurityScheme(name = "dbaApiKey", type = SecuritySchemeType.APIKEY,
            in = SecuritySchemeIn.HEADER, paramName = "X-DBA-Key")
})
public class OpenApiSecurityConfig {
}
