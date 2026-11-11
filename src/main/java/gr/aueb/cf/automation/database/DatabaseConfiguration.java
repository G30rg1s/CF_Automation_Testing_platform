package gr.aueb.cf.automation.database;

import com.zaxxer.hikari.HikariDataSource;
import gr.aueb.cf.automation.queryrunner.DatabaseTarget;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DatabaseConfiguration {


    /* ========================================
       PRODUCTION PROPERTIES
       ======================================== */

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties productionDataSourceProperties() {

        return new DataSourceProperties();
    }


    /* ========================================
       PRODUCTION DATASOURCE
       ======================================== */

    @Bean(name = "productionDataSource")
    public DataSource productionDataSource(
            @Qualifier("productionDataSourceProperties")
            DataSourceProperties properties
    ) {

        return properties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }


    /* ========================================
       FRONTEND TESTING PROPERTIES
       ======================================== */

    @Bean
    @ConfigurationProperties("app.datasource.frontendtesting")
    public DataSourceProperties frontendTestingDataSourceProperties() {

        return new DataSourceProperties();
    }


    /* ========================================
       FRONTEND TESTING DATASOURCE
       ======================================== */

    @Bean(name = "frontendTestingDataSource")
    public DataSource frontendTestingDataSource(
            @Qualifier("frontendTestingDataSourceProperties")
            DataSourceProperties properties
    ) {

        return properties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }


    /* ========================================
       API TESTING PROPERTIES
       ======================================== */

    @Bean
    @ConfigurationProperties("app.datasource.apitesting")
    public DataSourceProperties apiTestingDataSourceProperties() {

        return new DataSourceProperties();
    }


    /* ========================================
       API TESTING DATASOURCE
       ======================================== */

    @Bean(name = "apiTestingDataSource")
    public DataSource apiTestingDataSource(
            @Qualifier("apiTestingDataSourceProperties")
            DataSourceProperties properties
    ) {

        return properties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }


    /* ========================================
       ROUTING DATASOURCE
       ======================================== */

    @Bean
    @Primary
    public DataSource dataSource(
            @Qualifier("productionDataSource")
            DataSource productionDataSource,

            @Qualifier("frontendTestingDataSource")
            DataSource frontendTestingDataSource,

            @Qualifier("apiTestingDataSource")
            DataSource apiTestingDataSource
    ) {

        DatabaseRoutingDataSource routingDataSource =
                new DatabaseRoutingDataSource();


        Map<Object, Object> targets =
                new HashMap<>();


        targets.put(
                DatabaseTarget.PRODUCTION,
                productionDataSource
        );

        targets.put(
                DatabaseTarget.FRONTEND_TESTING,
                frontendTestingDataSource
        );

        targets.put(
                DatabaseTarget.API_TESTING,
                apiTestingDataSource
        );


        routingDataSource.setTargetDataSources(
                targets
        );

        routingDataSource.setLenientFallback(
                false
        );


        routingDataSource.afterPropertiesSet();

        return routingDataSource;
    }
}