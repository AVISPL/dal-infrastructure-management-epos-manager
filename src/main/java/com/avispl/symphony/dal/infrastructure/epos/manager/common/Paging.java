/*
 * Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.epos.manager.common;

/**
 * Paging abstract class for creating paging object
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/11/2024
 * @since 1.0.0
 */
public abstract class Paging {
	private int take;
	private int skip;
	private int totalItem;

	public Paging(int take, int skip, int totalItem) {
		this.take = take;
		this.skip = skip;
		this.totalItem = totalItem;
	}

	/**
	 * Retrieves {@link #take}
	 *
	 * @return value of {@link #take}
	 */
	public int getTake() {
		return take;
	}

	/**
	 * Sets {@link #take} value
	 *
	 * @param take new value of {@link #take}
	 */
	public void setTake(int take) {
		this.take = take;
	}

	/**
	 * Retrieves {@link #skip}
	 *
	 * @return value of {@link #skip}
	 */
	public int getSkip() {
		return skip;
	}

	/**
	 * Sets {@link #skip} value
	 *
	 * @param skip new value of {@link #skip}
	 */
	public void setSkip(int skip) {
		this.skip = skip;
	}

	/**
	 * Retrieves {@link #totalItem}
	 *
	 * @return value of {@link #totalItem}
	 */
	public int getTotalItem() {
		return totalItem;
	}

	/**
	 * Sets {@link #totalItem} value
	 *
	 * @param totalItem new value of {@link #totalItem}
	 */
	public void setTotalItem(int totalItem) {
		this.totalItem = totalItem;
	}

	/**
	 * Check if we can request another items
	 */
	public boolean hasReachedEndPage() {
		return this.skip >= this.totalItem;
	}

	/**
	 * request another item on next page
	 */
	public void nextPage() {
		if (!hasReachedEndPage()) {
			this.skip += take;
		}
	}

	/**
	 * Set default for paging process
	 */
	public void reset() {
		this.take = 100;
		this.skip = 0;
	}
}