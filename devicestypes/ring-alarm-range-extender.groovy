/**
 *  Ring Alarm Range Extender
 *
 *  Copyright 2019 Ben Rimmasch
 *
 *  https://shop.ring.com/products/alarm-range-extender
 *  https://products.z-wavealliance.org/products/3688
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change Log:
 *  2020-01-01: Initial
 */
metadata {
  definition(
    name: "Ring Alarm Range Extender", namespace: "codahq-hubitat", author: "Ben Rimmasch",
      importUrl: "https://raw.githubusercontent.com/codahq/hubitat_codahq/master/devicestypes/ring-alarm-range-extender.groovy") {
    capability "Sensor"
    capability "Battery"
    capability "Configuration"
    capability "Refresh"
    capability "Health Check"
    capability "PowerSource"

    attribute "acStatus", "string"
    attribute "batteryStatus", "string"

    fingerprint mfr: "0346", prod:"0401", deviceId:"0101", deviceJoinName: "Ring Extender",
      inClusters:"0x5E,0x85,0x59,0x55,0x86,0x72,0x5A,0x73,0x98,0x9F,0x6C,0x71,0x70,0x80,0x7A"
    //to add new fingerprints convert dec manufacturer to hex mfr, dec deviceType to hex prod, and dec deviceId to hex model
  }

  preferences {

    input "batteryReportingInterval", "number", range: 4..70, title: "Battery Reporting Interval",
      description: "Battery reporting interval can be configured to report from 4 to 70 minutes",
      defaultValue: 70, required: false, displayDuringSetup: true
    input name: "descriptionTextEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    input name: "traceLogEnable", type: "bool", title: "Enable trace logging", defaultValue: false
  }
}

private logInfo(msg) {
  if (descriptionTextEnable) log.info msg
}

def logDebug(msg) {
  if (logEnable) log.debug msg
}

def logTrace(msg) {
  if (traceLogEnable) log.trace msg
}

// Sets flag so that configuration is updated the next time it wakes up.
def updated() {
  logTrace "updated()"
  
  return configure()
}

// Initializes the device state when paired and updates the device's configuration.
def configure() {
  logTrace "configure()"
  def cmds = []

  // Set and get current config parameter values
  def batteryInterval = batteryReportingInterval ? batteryReportingInterval.toInteger() : 70
  cmds << zwave.configurationV2.configurationSet(parameterNumber: 1, configurationValue: [batteryInterval], size: 1).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 1).format()
    
  cmds << zwave.versionV1.versionGet().format()
  cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
  
  return delayBetween(cmds, 500)
}

// Required for HealthCheck Capability, but doesn't actually do anything because this device sleeps.
def ping() {
  logDebug "ping()"
  zwave.batteryV1.batteryGet().format()
}

def refresh() {
  logDebug "refresh()"
  state.clear()
  
  def cmds = []
  cmds << zwave.batteryV1.batteryGet().format()
  cmds << zwave.notificationV3.notificationGet(notificationType: 8, v1AlarmType: 0).format()
  //can't seem to get the status of each individual event because the alarm value doesn't change
  //cmds << zwave.notificationV3.notificationGet(notificationType: 8, v1AlarmType: 0, event: 0x00).format()
  //cmds << zwave.notificationV3.notificationGet(notificationType: 8, v1AlarmType: 0, event: 0x02).format()
  //cmds << zwave.notificationV3.notificationGet(notificationType: 8, v1AlarmType: 0, event: 0x03).format()
  //cmds << zwave.notificationV3.notificationGet(notificationType: 8, v1AlarmType: 0, event: 0x0C).format()
  return delayBetween(cmds, 500)
}

// Processes messages received from device.
def parse(String description) {
  logDebug "parse(String description)"
  logTrace "Description: ${description}"
  def result = []

  def cmd = zwave.parse(description, commandClassVersions)
  if (cmd) {
    result += zwaveEvent(cmd)
  }
  else {
    log.warn "Unable to parse description: $description"
  }
  return result
}

private getCommandClassVersions() {
  //synchronized from https://products.z-wavealliance.org/products/3688/classes
  [
    0x59: 1,  // AssociationGrpInfo
    0x85: 2,  // Association V2
    0x80: 1,  // Battery
    0x70: 1,  // Configuration
    0x5A: 1,  // DeviceResetLocally
    0x7A: 4,  // Firmware Update MD
    0x72: 2,  // ManufacturerSpecific V2
    0x71: 8,  // Notification V8
    0x73: 1,  // Powerlevel
    0x98: 1,  // Security
    // Security 2 not implemented in Hubitat
    0x6C: 1,  // Supervision
    0x55: 2,  // Transport Service V2
    0x86: 1,  // Version (V2)
    0x5E: 2,  // ZwaveplusInfo V2
  ]
}

def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd)"
  logTrace "BatteryReport: $cmd"
  
  def val = (cmd.batteryLevel == 0xFF ? 1 : cmd.batteryLevel)
  if (val > 100) {
    val = 100
  }
  else if (val <= 0) {
    val = 1
  }
  state.lastBatteryReport = convertToLocalTimeString(new Date())
  logInfo "Battery: ${val}%"
  return createEvent([name: "battery", value: val, descriptionText: "battery ${val}%", unit: "%"])
}

def zwaveEvent(hubitat.zwave.commands.associationv1.AssociationReport cmd) {
    log.info "AssociationReport- groupingIdentifier:${cmd.groupingIdentifier}, maxNodesSupported:${cmd.maxNodesSupported}, nodes:${cmd.nodeId}"
}

def zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport cmd)"
  logTrace "cmd: $cmd"
  
  def firmware = "${cmd.applicationVersion}.${cmd.applicationSubVersion}"
  updateDataValue("firmware", firmware)
  logDebug "${device.displayName} is running firmware version: $firmware, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
}

def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd)"
  logTrace "cmd: $cmd"
  
  logDebug "manufacturerId:   ${cmd.manufacturerId}"
  logDebug "manufacturerName: ${cmd.manufacturerName}"
  state.manufacturer = cmd.manufacturerName
  logDebug "productId:        ${cmd.productId}"
  logDebug "productTypeId:    ${cmd.productTypeId}"
  def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  updateDataValue("MSR", msr)
  updateDataValue("manufacturer", cmd.manufacturerId.toString())
  updateDataValue("manufacturerName", cmd.manufacturerName ? cmd.manufacturerName : "Ring")
  sendEvent([descriptionText: "$device.displayName MSR: $msr", isStateChange: false])
}

def zwaveEvent(hubitat.zwave.commands.notificationv3.NotificationReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.notificationv3.NotificationReport cmd)"
  logTrace "NotificationReport3: $cmd"
  
  def result = []
  switch (cmd.notificationType) {
    //Power management
    case 8:
      logTrace "Power management"
      switch (cmd.event) {
        case 0x00:
          def msg = "${device.label}'s batteryStatus is ok (not charging)"
          logInfo msg
          result << createEvent([name: "batteryStatus", value: "ok", descriptionText: msg])
          break
        case 0x02:
          def msg = "${device.label}'s acStatus is disconnected"
          logInfo msg
          result << createEvent([name: "acStatus", value: "disconnected", descriptionText: msg])
          result << createEvent([name: "powerSource", value: "battery"])
          break
        case 0x03:
          def msg = "${device.label}'s acStatus is connected"
          logInfo msg
          result << createEvent([name: "acStatus", value: "connected", descriptionText: msg])
          result << createEvent([name: "powerSource", value: "mains"])
          break
        case 0x0C:
          def msg = "${device.label}'s batteryStatus is charging"
          logInfo msg
          result << createEvent([name: "batteryStatus", value: "charging", descriptionText: msg])
          break
        default:
          log.warn "Unhandled event ${cmd.event} for ${cmd.notificationType} notification type!"
          break
      }
      break
    default:
      log.warn "Unhandled notification type! ${cmd.notificationType}"
      break
  }
  
  if (result == []) {
    logIncompatible(cmd)
  }
  return result
}

def zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd)"
  logTrace "cmd: $cmd"
  
  result = []
  switch (cmd.parameterNumber) {
    //battery reporting interval
    case 1:
      result << createEvent([name: "Battery Reporting Interval", value: cmd.configurationValue, displayed: false])
      break
    default:
      log.warn "Unhandled parameter ${cmd.parameterNumber} from configuration report!"
      break
  }
  return result
}

// Logs unexpected events from the device.
def zwaveEvent(hubitat.zwave.Command cmd) {
  logDebug "zwaveEvent(hubitat.zwave.Command cmd)"
  logTrace "Command: $cmd"
  logIncompatible(cmd)
  return []
}

private secureCmd(cmd) {
  if (zwaveInfo ?.zw ?.contains("s") || ("0x98" in device.rawDescription ?.split(" "))) {
    return zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
  }
  else {
    return cmd.format()
  }
}

private convertToLocalTimeString(dt) {
  def timeZoneId = location ?.timeZone ?.ID
	if (timeZoneId) {
    return dt.format("MM/dd/yyyy hh:mm:ss a", TimeZone.getTimeZone(timeZoneId))
  }
  else {
    return "$dt"
  }
}

private logIncompatible(cmd) {
  log.error "This is probably not the correct device driver for this device!"
  log.warn "cmd: ${cmd}"
}
