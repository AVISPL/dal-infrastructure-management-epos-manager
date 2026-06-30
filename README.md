# EPOS Manager Integration - Capabilities & Configuration
This document covers EPOS Manager Adapter Capabilities and Configuration.

Symphony integrates with EPOS Manager to provide comprehensive monitoring of EPOS audio devices across an organization. It leverages the EPOS UCMP Public API to aggregate device data from the EPOS cloud platform, delivering fleet-wide visibility including device online status, firmware versions, contact history, IP addressing, and tenant-level inventory.

## EPOS Manager: Main use cases 
- **Monitor** EPOS device fleet health, online status, firmware versions, and network contact history
- **Inventory** track all EPOS devices reporting into a tenant - device IDs, product IDs, vendors, and first/last seen timestamps
- **Aggregate** tenant-level information such as company name, tenant ID, and total device count
- **Provision** unprovisioned EPOS devices discovered by the aggregator into Symphony rooms

## Prerequisites to setup EPOS Manager Integration
The EPOS Manager Adapter communicates with the EPOS cloud platform at the EPOS UCMP API host `enterprise.eposaudio.com` over HTTPS.

Required credentials:
- **Partner ID** - a unique identifier assigned to the partner organization or third-party application; determines permissions and data access levels within the EPOS system
- **Partner Secret** - a confidential key/token associated with the Partner ID; grants access to EPOS API functionality and data

Supported Models: Any device that can report into EPOS Manager.

## EPOS Manager: Device Configuration and Provisioning
Once credentials are available, configure the Symphony device as follows:

| Field | Description |
| --- | --- |
| Device Type | Infrastructure |
| Category | Management |
| Manufacturer | EPOS |
| Model | Manager |
| Monitoring Service | Advanced Monitoring |
| Monitoring Source | Direct |
| Management Address | EPOS UCMP API host. Example: 'enterprise.eposaudio.com'. The actual hostname may vary. |
| Protocol | HTTPS |
| Username | Partner ID |
| Password | Partner Secret |
| Port Number | 443 |

## EPOS Manager: Adapter Configuration
The EPOS Manager aggregator supports three configuration parameters:

| Parameter | Description | Value |
| --- | --- | --- |
| tenantId | Required to monitor devices for the associated tenant | String |
| pingMode | Protocol used to test network connectivity | String (ICMP or TCP) |
| environment | URL used for authentication and API calls | String (Staging or Production); defaults to Production |

**Important notes:**
- If no Tenant ID is specified, the aggregator will not aggregate any devices and will show Unknown for Company Name, Tenant Name, and Tenant ID
- If the specified Tenant ID does not exist, the aggregator will not aggregate any devices and will show None for Company Name, Tenant Name, and display an invalid Tenant ID

For detailed information on the aggregator and its configuration, please refer to our knowledgebase -> https://symphony.knowledgeowl.com/help/epos-manager-aggregator-technical-breakdown

### EPOS Manager: Provisioning Aggregated Devices
When the device is configured, saved, and set active, the EPOS Manager Adapter will begin communicating with the EPOS cloud API to retrieve aggregator and device data.

By default, the unprovisioned devices will appear on Aggregated Devices -> Unprovisioned Devices tab.

To import a device for monitoring by the EPOS Manager Aggregator:
1. Open Aggregated Devices
2. Click the (+) icon on the unprovisioned device, or select unprovisioned devices from the list and click Import
3. Click OK to confirm import

Once successfully imported, the device will display an up-arrow icon indicating it is actively monitored by the EPOS Manager Aggregator.

## EPOS Manager: Available Monitored Data
EPOS Manager monitored data is organized into two levels:

### EPOS Manager: Aggregator-level properties 
| Property | Description |
| --- | --- |
| CompanyName | Name of the company associated with the tenant |
| TenantID | Unique identifier for the EPOS tenant |
| TenantName | Display name of the tenant |
| TotalDevices | Total number of devices reporting into the tenant |

### EPOS Manager: Device-level properties
| Property | Description |
| --- | --- |
| deviceId | Unique identifier for the device |
| deviceName | Display name of the device |
| deviceOnline | Whether the device is currently online |
| CurrentUserID | ID of the user currently associated with the device |
| ProductID | EPOS product identifier |
| Status | Current device status |
| Vendor | Device vendor |
| ID | Internal Symphony device ID |
| FirstSeen (GMT) | Timestamp of first contact with EPOS Manager |
| LastSeen (GMT) | Timestamp of most recent contact |
| FirstContactFWVersion | Firmware version at first contact |
| CurrentContactFWVersion | Current firmware version |
| FirstContactIPAddress | IP address at first contact |
| LastContactIPAddress | Most recent IP address |

## EPOS Manager: Troubleshooting
**Troubleshooting guidance**
- If an error occurs, focus only on troubleshooting steps that are provided in the section below.
- Do not include prerequisite/setup information.
- Do not add unrelated configuration details from other sections.
- If the document does not provide a direct error troubleshooting step, state that the document does not contain enough guidance for that specific issue.

**Login / Authentication Error**
- Verify the Partner ID (username) and Partner Secret (password) are correct and have not expired
- Confirm the Management Address is set to the correct EPOS UCMP API host, 'enterprise.eposaudio.co' and Protocol is HTTPS
- Ensure port 443 is not blocked by a firewall or proxy

**API Error**
- Check the API error description.
- Ensure that Partner ID (username) and Partner Secret (password) have not expired

**No devices aggregated / Unknown tenant fields**
- Verify that a valid Tenant ID is entered in the tenantId adapter configuration parameter
- Confirm the Partner ID and Partner Secret are correct
- Check that the specified Tenant ID exists in the EPOS system

**Link Error / Ping Timeout**
- Confirm the Cloud Connector has outbound internet access to EPOS UCMP API
- Try switching pingMode between ICMP and TCP, as certain protocols may be blocked by network policy

**Devices not appearing / Unprovisioned devices stuck**
- Check that the aggregator device is active and communicating (Extended Properties should be visible)
- Verify the environment parameter matches the EPOS environment in use (Staging or Production)

If none of the recommended steps help, please enter an SOS ticket at https://avi-spl.atlassian.net/servicedesk/customer/portals

## What AI Assistant can do with EPOS Manager Integration:
- Find the EPOS Manager aggregator device (Infrastructure | Management | EPOS | Manager) in Symphony
- Verify EPOS Manager adapter configuration - tenantId, pingMode, and environment settings
- Report on aggregator status - company name, tenant ID, tenant name, and total device count
- Report on individual device status - online state, firmware versions, product ID, first/last seen timestamps, and IP addresses
- Identify unprovisioned devices discovered by the aggregator

## What AI Assistant cannot do with EPOS Manager Integration:
- Provision devices into Symphony (must be done manually via the Symphony UI)
- Push firmware updates to EPOS devices
- Modify EPOS Manager cloud configuration directly
- Access device audio settings or call logs
