package com.github.andirady.pomcli.impl;

import com.github.andirady.pomcli.Config;

public class ConfigTestImpl implements Config {

    private static ThreadLocal<String> defaultGroupIdTL = new ThreadLocal<>();

    private Config actualImpl = new ConfigImpl();

	public void setDefaultGroupId(String defaultGroupId) {
        defaultGroupIdTL.set(defaultGroupId);
    }

    @Override
    public String getDefaultGroupId() {
        var defaultGroupId = defaultGroupIdTL.get();
        if (defaultGroupId == null) {
            return actualImpl.getDefaultGroupId();
        }

        return defaultGroupId;
    }
}
