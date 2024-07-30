/*
 * Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.epos.manager;


import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;

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
		List<AdvancedControllableProperty> advancedControllableProperties = extendedStatistic.getControllableProperties();

		extendedStatistic = (ExtendedStatistics) eposManagerCommunicator.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();

		Assert.assertEquals(4, statistics.size());
		Assert.assertEquals(1, advancedControllableProperties.size());
	}

	/**
	 * Test case for getting aggregated device info success
	 */
	@Test
	void testAggregatedDeviceInfo() throws Exception {
		eposManagerCommunicator.getMultipleStatistics();
		eposManagerCommunicator.retrieveMultipleStatistics();
		Thread.sleep(20000);
		List<AggregatedDevice> aggregatedDeviceList = eposManagerCommunicator.retrieveMultipleStatistics();

		String deviceId = "A004530221400032";
		Optional<AggregatedDevice> aggregatedDevice = aggregatedDeviceList.stream().filter(item -> item.getDeviceId().equals(deviceId)).findFirst();
		if (aggregatedDevice.isPresent()) {
			Map<String, String> stats = aggregatedDevice.get().getProperties();
			Assert.assertEquals(11, stats.size());
			Assert.assertEquals("A004530221400032", aggregatedDevice.get().getDeviceId());
			Assert.assertEquals("EXPAND Control T", aggregatedDevice.get().getDeviceModel());
			Assert.assertEquals("Active", stats.get("Status"));
			Assert.assertEquals("10.71.160.5", stats.get("LastContactIPAddress"));
			Assert.assertEquals("2024-07-22 12:13", stats.get("FirstSeen"));
			Assert.assertEquals("bd020520-fd25-4074-9ae8-948735208394", stats.get("CurrentUserID"));
			Assert.assertEquals("2.0.24113.03", stats.get("FirstContactFWVersion"));
			Assert.assertEquals("2.0.24113.03", stats.get("CurrentContactFWVersion"));
			Assert.assertEquals("10.71.160.5", stats.get("FirstContactIPAddress"));
			Assert.assertEquals("31439", stats.get("ProductID"));
			Assert.assertEquals("99013dee-142a-44cd-85da-cacb882aca04", stats.get("ID"));
			Assert.assertEquals("EPOS", stats.get("Vendor"));
			Assert.assertEquals("2024-07-30 02:15", stats.get("LastSeen"));
		}
	}

	/**
	 * Test case for control tenant successfully
	 */
	@Test
	void testControlTenantNameSuccessFull() throws Exception {
		eposManagerCommunicator.getMultipleStatistics();
		eposManagerCommunicator.retrieveMultipleStatistics();
		Thread.sleep(20000);
		eposManagerCommunicator.retrieveMultipleStatistics();
		ControllableProperty control = new ControllableProperty();
		String propertyName = "TenantName";
		String propertyValue = "Symphony";
		control.setProperty(propertyName);
		control.setProperty(propertyValue);
		eposManagerCommunicator.controlProperty(control);

		extendedStatistic = (ExtendedStatistics) eposManagerCommunicator.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();
		Assert.assertEquals(propertyValue, statistics.get(propertyName));
	}

	/**
	 * Testcase for control tenant failure
	 */
	@Test
	void testControlTenantNameFailure() throws Exception {
		eposManagerCommunicator.getMultipleStatistics();
		eposManagerCommunicator.retrieveMultipleStatistics();
		Thread.sleep(20000);
		eposManagerCommunicator.retrieveMultipleStatistics();
		ControllableProperty control = new ControllableProperty();
		String propertyName = "TenantName";
		String propertyValue = "Symphony1";
		control.setProperty(propertyName);
		control.setValue(propertyValue);

		Assertions.assertThrows(IllegalArgumentException.class, () -> eposManagerCommunicator.controlProperty(control));
	}
}