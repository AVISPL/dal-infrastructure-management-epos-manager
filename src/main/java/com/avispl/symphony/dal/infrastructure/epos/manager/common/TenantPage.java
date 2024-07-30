/*
 * Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.epos.manager.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TenantPage represents number of tenant per page
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/11/2024
 * @since 1.0.0
 */
public class TenantPage extends Paging {
	private final List<Tenant> tenants = Collections.synchronizedList(new ArrayList<>());
	private Tenant selectedTenant;

	public TenantPage(int take, int skip, int total) {
		super(take, skip, total);
	}

	/**
	 * Retrieves {@link #selectedTenant}
	 *
	 * @return value of {@link #selectedTenant}
	 */
	public Tenant getSelectedTenant() {
		return selectedTenant;
	}

	/**
	 * Sets {@link #selectedTenant} value
	 *
	 * @param selectedTenant new value of {@link #selectedTenant}
	 */
	public void setSelectedTenant(Tenant selectedTenant) {
		this.selectedTenant = selectedTenant;
	}

	/**
	 * Retrieves {@link #tenants}
	 *
	 * @return value of {@link #tenants}
	 */
	public List<Tenant> getTenants() {
		return new ArrayList<>(tenants);
	}

	/**
	 * Add or update a specific tenant
	 *
	 * @param tenant need to be added or updated
	 */
	public void addOrUpdateTenant(Tenant tenant) {
		tenants.removeIf(item -> item.getTenantId().equals(tenant.getTenantId()));
		tenants.add(tenant);
	}
}