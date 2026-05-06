package srm.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("srm.portal.mapper")
public class SrmPortalBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(SrmPortalBackendApplication.class, args);
    }
}

