package gr.aueb.cf.automation.license.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        LicenseRuntimeProperties.class
)
public class LicenseRuntimeConfiguration {
}