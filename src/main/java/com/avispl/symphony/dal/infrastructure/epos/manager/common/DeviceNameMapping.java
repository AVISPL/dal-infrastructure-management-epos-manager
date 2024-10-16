/*
 * Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.epos.manager.common;

import java.util.Arrays;

/**
 * DeviceNameMapping represents the mapping value of device name between API responses and WebUI
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 8/2/2024
 * @since 1.0.0
 */
public enum DeviceNameMapping {
	EXPAND_CONTROL_BYOD("EXPAND Control (BYOD)", "EXPAND Control BYOD"),
	EXPAND_CONTROL_TEAMS("EXPAND Control (Teams)", "EXPAND Control T"),
	EXPAND_CONTROL_ZOOMS("EXPAND Control (Zoom)", "EXPAND Control Z"),
	EXPAND_CONTROL_3T_TEAMS("EXPAND Control 3T (Teams)", "EXPAND Control 3T"),
	EXPAND_CONTROL_PANEL("EXPAND Control Panel (Teams)", "EXPAND Control Panel"),
	EXPAND_VISION_TEAMS("EXPAND Vision 5 (Teams)", "EXPAND Vision 5T"),
	EXPAND_VISION_ZOOM("EXPAND Vision 5 (Zoom)", "EXPAND Vision 5Z"),
	EXPAND_VISION_BYOD("EXPAND Vision 5 (BYOD)", "EXPAND Vision 5BYOD");

	private String displayName;
	private String apiResponse;

	DeviceNameMapping(String displayName, String apiResponse) {
		this.displayName = displayName;
		this.apiResponse = apiResponse;
	}

	/**
	 * Retrieves {@link #displayName}
	 *
	 * @return value of {@link #displayName}
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Retrieves {@link #apiResponse}
	 *
	 * @return value of {@link #apiResponse}
	 */
	public String getApiResponse() {
		return apiResponse;
	}

	/**
	 * Get display name of device by name from api response
	 */
	public static DeviceNameMapping getDeviceNameByApiResponse(String apiResponse) {
		return Arrays.stream(values())
				.filter(item -> item.getApiResponse().equals(apiResponse))
				.findFirst()
				.orElse(null);
	}
}