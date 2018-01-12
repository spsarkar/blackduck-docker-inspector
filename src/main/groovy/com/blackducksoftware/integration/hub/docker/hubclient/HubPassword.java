package com.blackducksoftware.integration.hub.docker.hubclient;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.docker.config.Config;

@Component
public class HubPassword {

    @Autowired
    private Config config;

    public String get() {
        String hubPassword = config.getHubPasswordEnvVar();
        if (!StringUtils.isBlank(config.getHubPassword())) {
            hubPassword = config.getHubPassword();
        }
        return hubPassword;
    }
}
