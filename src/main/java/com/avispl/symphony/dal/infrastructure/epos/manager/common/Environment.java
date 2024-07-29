/*
 * Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.epos.manager.common;

import java.util.Arrays;

/**
 * Environment represents api subdomain for specific environment configuration
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/29/2024
 * @since 1.0.0
 */
public enum Environment {
	STAGING("Staging", "identity-stage", "ucmp-stage"),
	PRODUCTION("Production", "identity", "ucmp");

	private String name;
	private String tokenGenerationSubDomain;
	private String apiCallSubDomain;

	Environment(String name, String tokenGenerationSubDomain, String apiCallSubDomain) {
		this.name = name;
		this.tokenGenerationSubDomain = tokenGenerationSubDomain;
		this.apiCallSubDomain = apiCallSubDomain;
	}

	/**
	 * Retrieves {@link #name}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets {@link #name} value
	 *
	 * @param name new value of {@link #name}
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Retrieves {@link #tokenGenerationSubDomain}
	 *
	 * @return value of {@link #tokenGenerationSubDomain}
	 */
	public String getTokenGenerationSubDomain() {
		return tokenGenerationSubDomain;
	}

	/**
	 * Sets {@link #tokenGenerationSubDomain} value
	 *
	 * @param tokenGenerationSubDomain new value of {@link #tokenGenerationSubDomain}
	 */
	public void setTokenGenerationSubDomain(String tokenGenerationSubDomain) {
		this.tokenGenerationSubDomain = tokenGenerationSubDomain;
	}

	/**
	 * Retrieves {@link #apiCallSubDomain}
	 *
	 * @return value of {@link #apiCallSubDomain}
	 */
	public String getApiCallSubDomain() {
		return apiCallSubDomain;
	}

	/**
	 * Sets {@link #apiCallSubDomain} value
	 *
	 * @param apiCallSubDomain new value of {@link #apiCallSubDomain}
	 */
	public void setApiCallSubDomain(String apiCallSubDomain) {
		this.apiCallSubDomain = apiCallSubDomain;
	}

	/**
	 * Get environment configuration by name
	 * @param name of environment
	 */
	public static Environment getEnvironmentByName(String name) {
		return Arrays.stream(values())
				.filter(item -> item.name.equals(name))
				.findFirst()
				.orElse(PRODUCTION);
	}
}