/*
 * Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.epos.manager;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.security.auth.login.FailedLoginException;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.api.dal.monitor.aggregator.Aggregator;
import com.avispl.symphony.dal.aggregator.parser.AggregatedDeviceProcessor;
import com.avispl.symphony.dal.aggregator.parser.PropertiesMapping;
import com.avispl.symphony.dal.aggregator.parser.PropertiesMappingParser;
import com.avispl.symphony.dal.communicator.RestCommunicator;
import com.avispl.symphony.dal.infrastructure.epos.manager.common.AggregatedInformation;
import com.avispl.symphony.dal.infrastructure.epos.manager.common.DeviceNameMapping;
import com.avispl.symphony.dal.infrastructure.epos.manager.common.DevicePage;
import com.avispl.symphony.dal.infrastructure.epos.manager.common.Environment;
import com.avispl.symphony.dal.infrastructure.epos.manager.common.EposManagerConstant;
import com.avispl.symphony.dal.infrastructure.epos.manager.common.EposManagerUri;
import com.avispl.symphony.dal.infrastructure.epos.manager.common.LoginInfo;
import com.avispl.symphony.dal.infrastructure.epos.manager.common.PingMode;
import com.avispl.symphony.dal.infrastructure.epos.manager.common.Tenant;
import com.avispl.symphony.dal.infrastructure.epos.manager.common.TenantPage;
import com.avispl.symphony.dal.util.StringUtils;

/**
 * EposManagerCommunicator
 * Aggregator Monitoring Capabilities:
 * <ul>
 *   <li>TenantID</li>
 *   <li>TenantName</li>
 *   <li>CompanyName</li>
 * </ul>
 *
 * Aggregated Devices Monitoring Capabilities:
 * <ul>
 *   <li>ID</li>
 *   <li>ProductID</li>
 *   <li>deviceID</li>
 *   <li>deviceName</li>
 *   <li>deviceOnline</li>
 *   <li>Vendor</li>
 *   <li>FirstSeen</li>
 *   <li>LastSeen</li>
 *   <li>FirstContactFirmwareVersion</li>
 *   <li>CurrentContactFirmwareVersion</li>
 *   <li>FirstContactIPAddress</li>
 *   <li>LastContactIPAddress</li>
 *   <li>CurrentUserID</li>
 *   <li>Status</li>
 * </ul>
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 7/11/2024
 * @since 1.0.0
 */
public class EposManagerCommunicator extends RestCommunicator implements Aggregator, Monitorable {

	/**
	 * Process that is running constantly and triggers collecting data from Epos Manager API endpoints, based on the given timeouts and thresholds.
	 *
	 * @author Kevin / Symphony Dev Team<br>
	 * @since 1.0.0
	 */
	class EposManagerDataLoader implements Runnable {
		private volatile boolean inProgress;
		private volatile boolean flag = false;

		public EposManagerDataLoader() {
			this.inProgress = true;
		}

		@Override
		public void run() {
			loop:
			while (inProgress) {
				try {
					TimeUnit.MICROSECONDS.sleep(500);
				} catch (InterruptedException e) {
					logger.info(String.format("Sleep for 0.5 second was interrupted with error message: %s", e.getMessage()));
				}

				if (!inProgress) {
					break loop;
				}
				// next line will determine whether NanoSuite monitoring was paused
				updateAggregatorStatus();
				if (devicePaused) {
					continue loop;
				}

				if (logger.isDebugEnabled()) {
					logger.debug("Fetching other than aggregated device list");
				}

				long currentTimestamp = System.currentTimeMillis();
				if (!flag && nextDevicesCollectionIterationTimestamp <= currentTimestamp) {
					populateDeviceDetail();
					flag = true;
				}

				while (nextDevicesCollectionIterationTimestamp > System.currentTimeMillis()) {
					try {
						TimeUnit.MILLISECONDS.sleep(1000);
					} catch (InterruptedException e) {
						logger.info(String.format("Sleep for 1 second was interrupted with error message: %s", e.getMessage()));
					}
				}

				if (!inProgress) {
					break loop;
				}

				if (flag) {
					nextDevicesCollectionIterationTimestamp = System.currentTimeMillis() + 30000;
					flag = false;
				}

				if (logger.isDebugEnabled()) {
					logger.debug("Finished collecting devices statistics cycle at " + new Date());
				}
			}
		}

		public void stop() {
			this.inProgress = false;
		}
	}


	/**
	 * Indicates whether a device is considered as paused.
	 * True by default so if the system is rebooted and the actual value is lost -> the device won't start stats
	 * collection unless the {@link EposManagerCommunicator#retrieveMultipleStatistics()} method is called which will change it
	 * to a correct value
	 */
	private volatile boolean devicePaused = true;

	/**
	 * We don't want the statistics to be collected constantly, because if there's not a big list of devices -
	 * new devices' statistics loop will be launched before the next monitoring iteration. To avoid that -
	 * this variable stores a timestamp which validates it, so when the devices' statistics is done collecting, variable
	 * is set to currentTime + 30s, at the same time, calling {@link #retrieveMultipleStatistics()} and updating the
	 */
	private long nextDevicesCollectionIterationTimestamp;

	/**
	 * This parameter holds timestamp of when we need to stop performing API calls
	 * It used when device stop retrieving statistic. Updated each time of called #retrieveMultipleStatistics
	 */
	private volatile long validRetrieveStatisticsTimestamp;

	/**
	 * Aggregator inactivity timeout. If the {@link EposManagerCommunicator#retrieveMultipleStatistics()}  method is not
	 * called during this period of time - device is considered to be paused, thus the Cloud API
	 * is not supposed to be called
	 */
	private static final long retrieveStatisticsTimeOut = 3 * 60 * 1000;

	/**
	 * Update the status of the device.
	 * The device is considered as paused if did not receive any retrieveMultipleStatistics()
	 * calls during {@link EposManagerCommunicator}
	 */
	private synchronized void updateAggregatorStatus() {
		devicePaused = validRetrieveStatisticsTimestamp < System.currentTimeMillis();
	}

	/**
	 * Uptime time stamp to valid one
	 */
	private synchronized void updateValidRetrieveStatisticsTimestamp() {
		validRetrieveStatisticsTimestamp = System.currentTimeMillis() + retrieveStatisticsTimeOut;
		updateAggregatorStatus();
	}

	/**
	 * A mapper for reading and writing JSON using Jackson library.
	 * ObjectMapper provides functionality for converting between Java objects and JSON.
	 * It can be used to serialize objects to JSON format, and deserialize JSON data to objects.
	 */
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Executor that runs all the async operations
	 */
	private ExecutorService executorService;

	/**
	 * A private field that represents an instance of the EposManagerDataLoader class, which is responsible for loading device data for Epos Manger
	 */
	private EposManagerDataLoader deviceDataLoader;

	/**
	 * A private final ReentrantLock instance used to provide exclusive access to a shared resource
	 * that can be accessed by multiple threads concurrently. This lock allows multiple reentrant
	 * locks on the same shared resource by the same thread.
	 */
	private final ReentrantLock reentrantLock = new ReentrantLock();

	/**
	 * Private variable representing the local extended statistics.
	 */
	private ExtendedStatistics localExtendedStatistics;

	/**
	 * An instance of the AggregatedDeviceProcessor class used to process and aggregate device-related data.
	 */
	private AggregatedDeviceProcessor aggregatedDeviceProcessor;

	/**
	 * List of aggregated device
	 */
	private final List<AggregatedDevice> aggregatedDeviceList = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Login information
	 */
	private LoginInfo loginInfo;

	/**
	 * Ping mode
	 */
	private PingMode pingMode = PingMode.ICMP;

	/**
	 * Pagination for tenants object
	 */
	private TenantPage tenantPage = new TenantPage(100, 0, 0);

	/**
	 * Total number of tenants in epos system
	 */
	private int numberOfTenant = 0;

	/**
	 * Default hostname
	 */
	private String defaultHostName = EposManagerConstant.NONE;

	/**
	 * Pagination contain a list of all device related to specific tenant
	 */
	private DevicePage devicePage = new DevicePage(100, 0, 0);

	/**
	 * Total number of devices in epos system
	 */
	private int numberOfDevice = 0;

	/**
	 * Environment configuration specify subdomain for api call and authentication
	 */
	private String environment;

	/**
	 * tenantId configuration specify selected tenant to fetch all devices
	 */
	private String tenantId = EposManagerConstant.EMPTY;

	/**
	 * Retrieves {@link #environment}
	 *
	 * @return value of {@link #environment}
	 */
	public String getEnvironment() {
		return environment;
	}

	/**
	 * Sets {@link #environment} value
	 *
	 * @param environment new value of {@link #environment}
	 */
	public void setEnvironment(String environment) {
		this.environment = environment;
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
	 * Retrieves {@link #pingMode}
	 *
	 * @return value of {@link #pingMode}
	 */
	public String getPingMode() {
		return pingMode.name();
	}

	/**
	 * Sets {@link #pingMode} value
	 *
	 * @param pingMode new value of {@link #pingMode}
	 */
	public void setPingMode(String pingMode) {
		this.pingMode = PingMode.ofString(pingMode);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 *
	 * Check for available devices before retrieving the value
	 * ping latency information to Symphony
	 */
	@Override
	public int ping() throws Exception {
		if (this.pingMode == PingMode.ICMP) {
			return super.ping();
		} else if (this.pingMode == PingMode.TCP) {
			if (isInitialized()) {
				long pingResultTotal = 0L;

				for (int i = 0; i < this.getPingAttempts(); i++) {
					long startTime = System.currentTimeMillis();

					try (Socket puSocketConnection = new Socket(this.host, this.getPort())) {
						puSocketConnection.setSoTimeout(this.getPingTimeout());
						if (puSocketConnection.isConnected()) {
							long pingResult = System.currentTimeMillis() - startTime;
							pingResultTotal += pingResult;
							if (this.logger.isTraceEnabled()) {
								this.logger.trace(String.format("PING OK: Attempt #%s to connect to %s on port %s succeeded in %s ms", i + 1, host, this.getPort(), pingResult));
							}
						} else {
							if (this.logger.isDebugEnabled()) {
								logger.debug(String.format("PING DISCONNECTED: Connection to %s did not succeed within the timeout period of %sms", host, this.getPingTimeout()));
							}
							return this.getPingTimeout();
						}
					} catch (SocketTimeoutException | ConnectException tex) {
						throw new SocketTimeoutException("Socket connection timed out");
					} catch (UnknownHostException tex) {
						throw new SocketTimeoutException("Socket connection timed out" + tex.getMessage());
					} catch (Exception e) {
						if (this.logger.isWarnEnabled()) {
							this.logger.warn(String.format("PING TIMEOUT: Connection to %s did not succeed, UNKNOWN ERROR %s: ", host, e.getMessage()));
						}
						return this.getPingTimeout();
					}
				}
				return Math.max(1, Math.toIntExact(pingResultTotal / this.getPingAttempts()));
			} else {
				throw new IllegalStateException("Cannot use device class without calling init() first");
			}
		} else {
			throw new IllegalArgumentException("Unknown PING Mode: " + pingMode);
		}
	}


	/**
	 * Constructs a new instance of EPOS Manager.
	 */
	public EposManagerCommunicator() throws IOException {
		Map<String, PropertiesMapping> mapping = new PropertiesMappingParser().loadYML(EposManagerConstant.MODEL_MAPPING_AGGREGATED_DEVICE, getClass());
		aggregatedDeviceProcessor = new AggregatedDeviceProcessor(mapping);
		this.setTrustAllCertificates(true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {
		reentrantLock.lock();
		try {
			checkAuthentication();
			Map<String, String> statistics = new HashMap<>();
			ExtendedStatistics extendedStatistics = new ExtendedStatistics();
			getTenantsInfo();
			populateTenantInfo(statistics);

			getNumberOfDevice(statistics);
			extendedStatistics.setStatistics(statistics);
			localExtendedStatistics = extendedStatistics;
		} finally {
			reentrantLock.unlock();
		}

		return Collections.singletonList(localExtendedStatistics);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<AggregatedDevice> retrieveMultipleStatistics() throws Exception {
		if (executorService == null) {
			executorService = Executors.newFixedThreadPool(1);
			executorService.submit(deviceDataLoader = new EposManagerDataLoader());
		}
		nextDevicesCollectionIterationTimestamp = System.currentTimeMillis();
		updateValidRetrieveStatisticsTimestamp();
		if (devicePage.getAggregatedDevices().isEmpty()) {
			return Collections.emptyList();
		}
		return cloneAndPopulateAggregatedDeviceList();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<AggregatedDevice> retrieveMultipleStatistics(List<String> list) throws Exception {
		return retrieveMultipleStatistics().stream().filter(aggregatedDevice -> list.contains(aggregatedDevice.getDeviceId())).collect(Collectors.toList());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void authenticate() throws Exception {

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalInit() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Internal init is called.");
		}
		String apiSubDomain = getCurrentEnvironment().getApiCallSubDomain();
		this.defaultHostName = this.getHost();
		this.setHost(createRequestUrl(apiSubDomain, this.defaultHostName));
		this.executorService = Executors.newFixedThreadPool(1);
		this.executorService.submit(deviceDataLoader = new EposManagerDataLoader());
		super.internalInit();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalDestroy() {
		if (logger.isDebugEnabled()) {
			logger.debug("Internal destroy is called.");
		}
		if (deviceDataLoader != null) {
			this.deviceDataLoader.stop();
			this.deviceDataLoader = null;
		}
		if (executorService != null) {
			this.executorService.shutdownNow();
			this.executorService = null;
		}
		if (localExtendedStatistics != null && localExtendedStatistics.getStatistics() != null) {
			localExtendedStatistics.getStatistics().clear();
		}
		this.nextDevicesCollectionIterationTimestamp = 0;
		this.aggregatedDeviceList.clear();
		this.tenantPage = new TenantPage(100, 0, 0);
		this.devicePage = new DevicePage(100, 0, 0);
		this.numberOfTenant = 0;
		this.numberOfDevice = 0;
		this.loginInfo = null;
		this.defaultHostName = EposManagerConstant.NONE;
		this.tenantId = EposManagerConstant.EMPTY;
		super.internalDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected HttpHeaders putExtraRequestHeaders(HttpMethod httpMethod, String uri, HttpHeaders headers) throws Exception {
		headers.setContentType(MediaType.APPLICATION_JSON);
		if (loginInfo != null) {
			headers.setBearerAuth(loginInfo.getToken());
		}
		return super.putExtraRequestHeaders(httpMethod, uri, headers);
	}

	/**
	 * Verify access token is valid or expire, if token expired it will request the new one.
	 */
	private void checkAuthentication() throws Exception {
		if (StringUtils.isNullOrEmpty(this.getLogin()) || StringUtils.isNullOrEmpty(this.getPassword())) {
			throw new FailedLoginException("Username or Password field is empty. Please check device credentials");
		}
		if (loginInfo == null || loginInfo.isTokenExpired()) {
			loginInfo = getLoginInfo();
		}
	}

	/**
	 * Retrieve authentication information by sending POST request to authentication API.
	 */
	private LoginInfo getLoginInfo() throws Exception {
		LoginInfo loginInfo = null;
		try {
			// Set up headers
			HttpHeaders headers = new HttpHeaders();
			headers.setBasicAuth(this.getLogin(), this.getPassword());
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			// Set up request body
			MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
			body.add("grant_type", "client_credentials");

			// Send POST request to device
			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
			String loginSubDomain = getCurrentEnvironment().getTokenGenerationSubDomain();

			ResponseEntity<JsonNode> responseEntity = this.obtainRestTemplate()
					.exchange(String.format(EposManagerUri.REQUEST_TOKEN, createRequestUrl(loginSubDomain, this.defaultHostName)), HttpMethod.POST, request, JsonNode.class);
			JsonNode response = responseEntity.getBody();

			if (response == null || !response.has(EposManagerConstant.ACCESS_TOKEN)) {
				throw new ResourceNotReachableException("Failed to retrieve authentication information, endpoint not reachable");
			}

			String token = response.get(EposManagerConstant.ACCESS_TOKEN).asText();
			long expiresIn = response.get(EposManagerConstant.EXPIRES_IN).asLong();
			loginInfo = new LoginInfo(token, expiresIn);

		} catch (HttpClientErrorException e) {
			boolean badRequest = false;
//			try {
//				org.springframework.http.HttpStatusCode status = e.getStatusCode();
//				badRequest = status.value() == HttpStatus.UNAUTHORIZED.value();
//			} catch (NoSuchMethodError nsme) {
				logger.warn("No springframework:6.x.x found in classpath, switching to deprecated getRawStatusCode() call for status code retrieval.");
				int code = e.getRawStatusCode();
				badRequest = code == HttpStatus.BAD_REQUEST.value();
//			}

			if (badRequest) {
				JsonNode response = objectMapper.readTree(e.getResponseBodyAsString());
				if (response.has("error") && "invalid_client".equalsIgnoreCase(response.get("error").asText())) {
					throw new FailedLoginException("Unable to login. Please check device credentials");
				}
			}
			throw new ResourceNotReachableException("An error occurred while retrieving authentication information", e);
		} catch (Exception exception) {
			throw new ResourceNotReachableException("Failed to retrieve authentication information, endpoint not reachable", exception);
		}
		return loginInfo;
	}

	/**
	 * Populate tenant information into provided stats map
	 *
	 * @param stats the map to store statistic properties.
	 */
	private void populateTenantInfo(Map<String, String> stats) {
		try {
			List<Tenant> tenants = tenantPage.getTenants();
			if (tenants != null && !tenants.isEmpty()) {
				if (!Objects.equals(tenantId, EposManagerConstant.EMPTY)) {
					Tenant selectedTenant = tenants.stream().filter(tenant -> tenant.getTenantId().equals(tenantId)).findFirst().orElse(null);
					if (selectedTenant != null) {
						tenantPage.setSelectedTenant(selectedTenant);
					}
					stats.put(EposManagerConstant.TENANT_ID, getDefaultValueForNullData(tenantId));
					stats.put(EposManagerConstant.COMPANY_NAME, getDefaultValueForNullData(selectedTenant != null ? selectedTenant.getCompanyName() : EposManagerConstant.EMPTY));
					stats.put(EposManagerConstant.TENANT_NAME, getDefaultValueForNullData(selectedTenant != null ? selectedTenant.getTenantName() : EposManagerConstant.EMPTY));
				} else {
					stats.put(EposManagerConstant.TENANT_ID, EposManagerConstant.UNKNOWN);
					stats.put(EposManagerConstant.COMPANY_NAME, EposManagerConstant.UNKNOWN);
					stats.put(EposManagerConstant.TENANT_NAME, EposManagerConstant.UNKNOWN);
				}
			}
		} catch (Exception e) {
			logger.error("Failed to populate tenants information with tenantId " + tenantId, e);
		}
	}

	/**
	 * Retrieve tenant information by sending GET request to Epos API endpoint.
	 */
	private void getTenantsInfo() throws FailedLoginException {
		getNumberOfTenant();
		if (numberOfTenant != tenantPage.getTotalItem()) {
			numberOfTenant = tenantPage.getTotalItem();
			tenantPage = new TenantPage(100, 0, numberOfTenant);
		}

		try {
			if (!tenantPage.hasReachedEndPage()) {
				String url = String.format(EposManagerUri.TENANTS, tenantPage.getTake(), tenantPage.getSkip());
				JsonNode response = this.doGet(url, JsonNode.class);

				if (response != null && response.has("items")) {
					List<Tenant> tenants = objectMapper.readerFor(new TypeReference<List<Tenant>>() {}).readValue(response.get("items"));

					if (tenants == null || tenants.isEmpty()) return;
					tenants.forEach(tenant -> tenantPage.addOrUpdateTenant(tenant));
				}
				tenantPage.nextPage();
			} else {
				tenantPage.reset();
			}
		} catch (FailedLoginException e) {
			loginInfo = null;
			logger.error("Authentication credentials are invalid, access token might be expired", e);
		} catch (Exception e) {
			throw new ResourceNotReachableException("Failed to retrieve tenant information", e);
		}
	}

	/**
	 * Retrieve number of total tenants by sending GET request to Epos API endpoint.
	 */
	private void getNumberOfTenant() {
		try {
			int take = 1, skip = 0;
			String url = String.format(EposManagerUri.TENANTS, take, skip);
			JsonNode response = this.doGet(url, JsonNode.class);

			if (response != null && response.has("total")) {
				tenantPage.setTotalItem(response.get("total").asInt());
			}
		} catch (FailedLoginException e) {
			loginInfo = null;
			logger.error("Authentication credentials are invalid, access token might be expired", e);
		} catch (Exception e) {
			throw new ResourceNotReachableException("Failed to retrieve number of tenants", e);
		}
	}

	/**
	 * Populate device details by making GET request to retrieve all aggregated device info from EPOS Manager.
	 * By default take 100 devices per request.
	 */
	private void populateDeviceDetail(){
		if (numberOfDevice != devicePage.getTotalItem()) {
			numberOfDevice = devicePage.getTotalItem();
			devicePage = new DevicePage(100, 0, numberOfDevice);
		}

		if (Objects.equals(tenantId, EposManagerConstant.EMPTY) || tenantPage.getSelectedTenant() == null) return;

		try {
			if (!devicePage.hasReachedEndPage()) {
				String url = String.format(EposManagerUri.DEVICES, tenantPage.getSelectedTenant().getTenantId(), devicePage.getTake(), devicePage.getSkip());
				JsonNode response = this.doGet(url, JsonNode.class);

				if (response != null && response.has("items")) {
					for (JsonNode item : response.get("items")) {
						JsonNode node = objectMapper.createArrayNode().add(item);
						String deviceId = item.get("deviceId").asText();
						devicePage.addOrUpdateDevices(deviceId, aggregatedDeviceProcessor.extractDevices(node));
					}
				}
				devicePage.nextPage();
			} else {
				devicePage.reset();
			}
		} catch (FailedLoginException e) {
			loginInfo = null;
			logger.error("Authentication credentials are invalid, access token might be expired", e);
		} catch (Exception e) {
			throw new ResourceNotReachableException("Failed to retrieve device information for tenantId " + tenantId, e);
		}
	}

	/**
	 * Retrieve total number of device by sending GET request to EPOS Manager API.
	 */
	private void getNumberOfDevice(Map<String, String> stats){
		try {
			int take = 1, skip = 0;
			Tenant tenant = tenantPage.getSelectedTenant();
			if (tenant == null){
				stats.put(EposManagerConstant.TOTAL_DEVICES, "0");
				return;
			}

			String url = String.format(EposManagerUri.DEVICES, tenant.getTenantId(), take, skip);
			JsonNode response = this.doGet(url, JsonNode.class);

			int deviceNumber = 0;
			if (response != null && response.has("total")) {
				deviceNumber = response.get("total").asInt();
				devicePage.setTotalItem(deviceNumber);
			}
			stats.put(EposManagerConstant.TOTAL_DEVICES, String.valueOf(deviceNumber));
		} catch (FailedLoginException e) {
			loginInfo = null;
			logger.error("Authentication credentials are invalid, access token might be expired", e);
		} catch (Exception e) {
			throw new ResourceNotReachableException("Failed to retrieve total of devices", e);
		}
	}

	/**
	 * Clones and populates a new list of aggregated devices with mapped monitoring properties.
	 *
	 * @return A new list of {@link AggregatedDevice} objects with mapped monitoring properties.
	 */
	private List<AggregatedDevice> cloneAndPopulateAggregatedDeviceList() {
		aggregatedDeviceList.clear();
		synchronized (devicePage.getAggregatedDevices()) {
			for (AggregatedDevice device : devicePage.getAggregatedDevices()) {
				AggregatedDevice aggregatedDevice = new AggregatedDevice();
				Map<String, String> properties = device.getProperties();
				aggregatedDevice.setDeviceId(device.getDeviceId());
				String deviceName = device.getDeviceName();
				DeviceNameMapping deviceNameMapping = DeviceNameMapping.getDeviceNameByApiResponse(deviceName);
				if (deviceNameMapping != null) {
					deviceName = deviceNameMapping.getDisplayName();
				}
				aggregatedDevice.setDeviceName(deviceName);
				aggregatedDevice.setDeviceOnline(device.getDeviceOnline());

				Map<String, String> stats = new HashMap<>();
				populateMonitoringProperties(properties, stats);
				aggregatedDevice.setProperties(stats);
				aggregatedDeviceList.add(aggregatedDevice);
			}
		}
		return aggregatedDeviceList;
	}

	/**
	 * Get all monitoring property cache from device page to statistic
	 *
	 * @param properties the map contain cache data
	 * @param stats the map contain monitoring properties.
	 */
	private void populateMonitoringProperties(Map<String, String> properties, Map<String, String> stats) {
		for (AggregatedInformation property : AggregatedInformation.values()) {
			String propertyName = property.getName();
			String propertyValue = getDefaultValueForNullData(properties.get(propertyName));
			switch (property) {
				case FIRST_SEEN:
				case LAST_SEEN:
					stats.put(propertyName, convertDateTimeFormat(propertyValue));
					break;
				case STATUS:
					stats.put(propertyName, formatDeviceStatus(propertyValue));
					break;
				default:
					stats.put(propertyName, propertyValue);
			}
		}
	}

	/**
	 * Convert ISO-8601 data time value to readable format
	 *
	 * @param value - ISO-8601 string
	 * @return format datetime
	 */
	private String convertDateTimeFormat(String value) {
		try {
			SimpleDateFormat inputFormat = new SimpleDateFormat(EposManagerConstant.SOURCE_FORMAT_DATETIME);
			inputFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

			SimpleDateFormat outputFormat = new SimpleDateFormat(EposManagerConstant.YYYY_MM_DD_HH_MM);
			outputFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

			Date date = inputFormat.parse(value);
			return outputFormat.format(date);
		} catch (Exception e) {
			logger.warn("Can't convert the date time value");
			return EposManagerConstant.NONE;
		}
	}

	/**
	 * Format device status from api response to symphony format
	 * @param value api response value
	 * @return format device status
	 */
	private String formatDeviceStatus(String value) {
		if (value.equalsIgnoreCase("InActive")) {
			return EposManagerConstant.INACTIVE_STATUS;
		}
		return value;
	}

	/**
	 * check value is null or empty
	 *
	 * @param value input value
	 * @return value after checking
	 */
	private String getDefaultValueForNullData(String value) {
		return StringUtils.isNotNullOrEmpty(value) && !"null".equalsIgnoreCase(value) ? value : EposManagerConstant.NONE;
	}

	/**
	 * Get current environment configuration
	 */
	private Environment getCurrentEnvironment() {
		Environment environment = Environment.PRODUCTION;
		if (getDefaultValueForNullData(this.environment).equals(Environment.STAGING.getName())) {
			environment = Environment.STAGING;
		}
		return environment;
	}

	/**
	 * Building a full api endpoint for specific request based on subdomain
	 *
	 * @param domain subdomain part
	 */
	private String createRequestUrl(String domain, String hostName) {
		String subDomain = getDefaultValueForNullData(domain);
		if (Objects.equals(subDomain, EposManagerConstant.NONE)) {
			return subDomain;
		}
		return subDomain + EposManagerConstant.DOT + hostName;
	}
}