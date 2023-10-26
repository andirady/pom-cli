package com.github.andirady.pomcli.impl;

import java.util.Objects;

import com.github.andirady.pomcli.Config;

public class ConfigImpl implements Config {

	@Override
	public String getDefaultGroupId() {
        return Objects.requireNonNullElse(System.getenv("POM_CLI_DEFAULT_GROUP_ID"), "unnamed");
	}
}
