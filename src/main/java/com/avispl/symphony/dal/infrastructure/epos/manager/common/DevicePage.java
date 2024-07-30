/*
 * Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.epos.manager.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;

/**
 * DevicePage represents a number of device per page
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/11/2024
 * @since 1.0.0
 */
public class DevicePage extends Paging {
	private final List<AggregatedDevice> aggregatedDevices = Collections.synchronizedList(new ArrayList<>());

	public DevicePage(int take, int skip, int totalItem) {
		super(take, skip, totalItem);
	}

	/**
	 * Retrieves {@link #aggregatedDevices}
	 *
	 * @return value of {@link #aggregatedDevices}
	 */
	public List<AggregatedDevice> getAggregatedDevices() {
		return new ArrayList<>(aggregatedDevices);
	}

	/**
	 * Add or update specific device by id
	 *
	 * @param deviceId id of device
	 * @param devices devices need to be add or update
	 */
	public void addOrUpdateDevices(String deviceId, List<AggregatedDevice> devices) {
		aggregatedDevices.removeIf(item -> item.getDeviceId().equals(deviceId));
		aggregatedDevices.addAll(devices);
	}
}