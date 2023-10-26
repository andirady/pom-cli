package com.github.andirady.pomcli;

import java.util.ServiceLoader;

public interface Config {

    static Config getInstance() {
        return ServiceLoader.load(Config.class).findFirst().orElseThrow();
    }

    String getDefaultGroupId();
}
