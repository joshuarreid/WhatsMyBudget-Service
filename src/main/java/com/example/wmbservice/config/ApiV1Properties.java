package com.example.wmbservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "api.v1")
public class ApiV1Properties {

    public enum Mode {
        ACTIVE,
        DEPRECATED,
        DISABLED
    }

    private Mode mode = Mode.ACTIVE;

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }
}

