/*
 * Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.epos.manager;


import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.dal.infrastructure.epos.manager.common.EposManagerConstant;

/**
 * EposManagerCommunicatorTest
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/11/2024
 * @since 1.0.0
 */
class EposManagerCommunicatorTest {
	private ExtendedStatistics extendedStatistic;
	private EposManagerCommunicator eposManagerCommunicator;
	private final String defaultTenantId = "43e73958-9f96-4658-a6cd-28b34db80b0b";

	@BeforeEach
	void setUp() throws Exception {
		eposManagerCommunicator = new EposManagerCommunicator();
		eposManagerCommunicator.setEnvironment("Staging");
		eposManagerCommunicator.setHost("");
		eposManagerCommunicator.setLogin("");
		eposManagerCommunicator.setPassword("");
		eposManagerCommunicator.setPort(443);
		eposManagerCommunicator.setTrustAllCertificates(true);
		eposManagerCommunicator.init();
		eposManagerCommunicator.connect();
		eposManagerCommunicator.setTenantId(defaultTenantId);
	}

	@AfterEach
	void destroy() throws Exception {
		eposManagerCommunicator.disconnect();
		eposManagerCommunicator.destroy();
	}

	/**
	 * Test case for getting all aggregator data success
	 */
	@Test
	void testGetAggregatorData() throws Exception {
		extendedStatistic = (ExtendedStatistics) eposManagerCommunicator.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		extendedStatistic = (ExtendedStatistics) eposManagerCommunicator.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();

		Assert.assertEquals(4, statistics.size());
		Assert.assertEquals("AVI-SPL", statistics.get("CompanyName"));
		Assert.assertEquals("Symphony", statistics.get("TenantName"));
		Assert.assertEquals(defaultTenantId, statistics.get("TenantID"));
	}

	@Test
	void testTenantIdIsNotSpecify() throws Exception {
		eposManagerCommunicator.setTenantId(EposManagerConstant.EMPTY);
		extendedStatistic = (ExtendedStatistics) eposManagerCommunicator.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		extendedStatistic = (ExtendedStatistics) eposManagerCommunicator.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();

		Assert.assertEquals(4, statistics.size());
		Assert.assertEquals("Unknown", statistics.get("CompanyName"));
		Assert.assertEquals("Unknown", statistics.get("TenantName"));
		Assert.assertEquals("Unknown", statistics.get("TenantID"));
		Assert.assertEquals("0", statistics.get("TotalDevices"));
	}

	@Test
	void testTenantIdIsInvalid() throws Exception {
		eposManagerCommunicator.setTenantId("invalid tenantID");
		extendedStatistic = (ExtendedStatistics) eposManagerCommunicator.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		extendedStatistic = (ExtendedStatistics) eposManagerCommunicator.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();

		Assert.assertEquals(4, statistics.size());
		Assert.assertEquals("None", statistics.get("CompanyName"));
		Assert.assertEquals("None", statistics.get("TenantName"));
		Assert.assertEquals("invalid tenantID", statistics.get("TenantID"));
		Assert.assertEquals("0", statistics.get("TotalDevices"));
	}

	/**
	 * Test case for getting aggregated device info success
	 */
	@Test
	void testAggregatedDeviceInfo() throws Exception {
		eposManagerCommunicator.getMultipleStatistics();
		eposManagerCommunicator.retrieveMultipleStatistics();
		Thread.sleep(30000);
		List<AggregatedDevice> aggregatedDeviceList = eposManagerCommunicator.retrieveMultipleStatistics();

		String deviceId = "A004530221400032";
		Optional<AggregatedDevice> aggregatedDevice = aggregatedDeviceList.stream().filter(item -> item.getDeviceId().equals(deviceId)).findFirst();
		if (aggregatedDevice.isPresent()) {
			Map<String, String> stats = aggregatedDevice.get().getProperties();
			Assert.assertEquals(11, stats.size());
			Assert.assertEquals("A004530221400032", aggregatedDevice.get().getDeviceId());
			Assert.assertEquals("InActive", stats.get("Status"));
			Assert.assertEquals("10.71.160.5", stats.get("LastContactIPAddress"));
			Assert.assertEquals("bd020520-fd25-4074-9ae8-948735208394", stats.get("CurrentUserID"));
			Assert.assertEquals("2.0.24113.03", stats.get("FirstContactFWVersion"));
			Assert.assertEquals("2.0.24113.03", stats.get("CurrentContactFWVersion"));
			Assert.assertEquals("10.71.160.5", stats.get("FirstContactIPAddress"));
			Assert.assertEquals("31439", stats.get("ProductID"));
			Assert.assertEquals("99013dee-142a-44cd-85da-cacb882aca04", stats.get("ID"));
			Assert.assertEquals("EPOS", stats.get("Vendor"));
		}
	}
}