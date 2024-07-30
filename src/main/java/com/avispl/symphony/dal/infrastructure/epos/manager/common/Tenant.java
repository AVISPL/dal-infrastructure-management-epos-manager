/*
 * Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.epos.manager.common;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Tenant represents information of tenant in EPOS
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/11/2024
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tenant {
	@JsonProperty("id")
	private String tenantId;
	private String tenantName;
	private String companyName;

	public Tenant() {

	}

	public Tenant(String tenantId, String tenantName, String companyName) {
		this.tenantId = tenantId;
		this.tenantName = tenantName;
		this.companyName = companyName;
	}

	/**
	 * Retrieves {@link #tenantId}
	 *
	 * @return value of {@link #tenantId}
	 */
	public String getTenantId() {
		return tenantId;
	}

	/**
	 * Sets {@link #tenantId} value
	 *
	 * @param tenantId new value of {@link #tenantId}
	 */
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	/**
	 * Retrieves {@link #companyName}
	 *
	 * @return value of {@link #companyName}
	 */
	public String getCompanyName() {
		return companyName;
	}

	/**
	 * Sets {@link #companyName} value
	 *
	 * @param companyName new value of {@link #companyName}
	 */
	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	/**
	 * Retrieves {@link #tenantName}
	 *
	 * @return value of {@link #tenantName}
	 */
	public String getTenantName() {
		return tenantName;
	}

	/**
	 * Sets {@link #tenantName} value
	 *
	 * @param tenantName new value of {@link #tenantName}
	 */
	public void setTenantName(String tenantName) {
		this.tenantName = tenantName;
	}
}