/*
 * Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.epos.manager.common;

/**
 * AggregatedInformation represents a list of monitoring properties for aggregated device
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/11/2024
 * @since 1.0.0
 */
public enum AggregatedInformation {
	ID("ID"),
	PRODUCT_ID("ProductID"),
	VENDOR("Vendor"),
	FIRST_SEEN("FirstSeen(GMT)"),
	LAST_SEEN("LastSeen(GMT)"),
	FIRST_CONTACT_FW_VERSION("FirstContactFWVersion"),
	CURRENT_CONTACT_FW_VERSION("CurrentContactFWVersion"),
	FIRST_CONTACT_IP_ADDRESS("FirstContactIPAddress"),
	LAST_CONTACT_IP_ADDRESS("LastContactIPAddress"),
	CURRENT_USER_ID("CurrentUserID"),
	STATUS("Status");

	private String name;

	AggregatedInformation(String name) {
		this.name = name;
	}

	/**
	 * Retrieves {@link #name}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}
}