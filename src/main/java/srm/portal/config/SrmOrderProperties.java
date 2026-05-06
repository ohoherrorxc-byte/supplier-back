package srm.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "srm.order")
public class SrmOrderProperties {
    private int confirmedStatus = 20;
    private int rejectedStatus = 90;

    public int getConfirmedStatus() {
        return confirmedStatus;
    }

    public void setConfirmedStatus(int confirmedStatus) {
        this.confirmedStatus = confirmedStatus;
    }

    public int getRejectedStatus() {
        return rejectedStatus;
    }

    public void setRejectedStatus(int rejectedStatus) {
        this.rejectedStatus = rejectedStatus;
    }
}

