/*
 * Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.epos.manager.common;

import java.time.LocalDateTime;

/**
 * LoginInfo contains information about login session
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/11/2024
 * @since 1.0.0
 */
public class LoginInfo {
	private String token;
	private LocalDateTime expirationTime;

	public LoginInfo() {

	}

	public LoginInfo(String token, long expireIn) {
		this.token = token;
		this.expirationTime = LocalDateTime.now().plusSeconds(expireIn);
	}

	/**
	 * Retrieves {@link #token}
	 *
	 * @return value of {@link #token}
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Sets {@link #token} value
	 *
	 * @param token new value of {@link #token}
	 */
	public void setToken(String token) {
		this.token = token;
	}

	/**
	 * Retrieves {@link #expirationTime}
	 *
	 * @return value of {@link #expirationTime}
	 */
	public LocalDateTime getExpirationTime() {
		return expirationTime;
	}

	/**
	 * Sets {@link #expirationTime} value
	 *
	 * @param expirationTime new value of {@link #expirationTime}
	 */
	public void setExpirationTime(LocalDateTime expirationTime) {
		this.expirationTime = expirationTime;
	}

	/**
	 * Check token still valid
	 */
	public boolean isTokenExpired() {
		return LocalDateTime.now().isAfter(this.expirationTime);
	}
}
