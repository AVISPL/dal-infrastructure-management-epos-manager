/*
 * Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.epos.manager.common;

/**
 * EposManagerUri represents uri request used in epos manager adapter
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/11/2024
 * @since 1.0.0
 */
public class EposManagerUri {
	public static String REQUEST_TOKEN = "https://%s/connect/token";
	public static String TENANTS = "api/v1/tenants?Take=%s&Skip=%s";
	public static String DEVICES = "api/v1/devices?tenantId=%s&Take=%s&Skip=%s";
}