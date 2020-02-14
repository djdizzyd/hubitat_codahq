/**
 *  Jasco Z-Wave Plus Dimmer
 *  Z-Wave Alliance Pages:
 *  https://products.z-wavealliance.org/products/1442
 *  https://products.z-wavealliance.org/products/2105
 *  https://products.z-wavealliance.org/products/3323
 *  https://products.z-wavealliance.org/products/2168 (14299)
 *  
 *
 *  Copyright 2020 Ben Rimmasch
 *
 *  Derived from the work of NuttyTree's device handler for SmartThings
 *
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
 *	Author: Ben Rimmasch
 *	Date:   2020-02-05
 *
 *	Changelog:
 *
 *  1.0      2020-02-05 Initial Version
 *
 *
 *   Button Mappings:
 *
 *   ACTION          BUTTON#    BUTTON ACTION
 *   Double-Tap Up     1        pressed
 *   Double-Tap Down   2        pressed
 *
 */

metadata {
  definition (name: "Jasco Z-Wave Plus Dimmer", namespace: "codahq-hubitat", author: "Ben Rimmasch",
             importUrl: "https://raw.githubusercontent.com/codahq/hubitat_codahq/master/devicestypes/jasco-14294-switch.groovy") {
    capability "Actuator"
    capability "PushableButton"
    capability "DoubleTapableButton"
    capability "Configuration"
    capability "Indicator"
    capability "Polling"
    capability "Refresh"
    capability "Sensor"
    capability "Switch"
    capability "Switch Level"

    attribute "inverted", "enum", ["inverted", "not inverted"]
    attribute "zwaveSteps", "number"
    attribute "zwaveDelay", "number"
    attribute "manualSteps", "number"
    attribute "manualDelay", "number"
    attribute "allSteps", "number"
    attribute "allDelay", "number"

    command "flash"
    command "doubleUp"
    command "doubleDown"
    command "inverted"
    command "notInverted"
    command "levelUp"
    command "levelDown"
    command "setZwaveSteps"
    command "setZwaveDelay"
    command "setManualSteps"
    command "setManualDelay"
    command "setAllSteps"
    command "setAllDelay"

    // These include version because there are older firmwares that don't support double-tap or the extra association groups
    fingerprint mfr:"0063", prod:"4944", model:"3038", ver: "5.26", deviceJoinName: "GE Z-Wave Plus Wall Dimmer"
    fingerprint mfr:"0063", prod:"4944", model:"3038", ver: "5.27", deviceJoinName: "GE Z-Wave Plus Wall Dimmer"
    fingerprint mfr:"0063", prod:"4944", model:"3038", ver: "5.28", deviceJoinName: "GE Z-Wave Plus Wall Dimmer"
    fingerprint mfr:"0063", prod:"4944", model:"3038", ver: "5.29", deviceJoinName: "GE Z-Wave Plus Wall Dimmer"
    fingerprint mfr:"0063", prod:"4944", model:"3039", ver: "5.19", deviceJoinName: "GE Z-Wave Plus 1000W Wall Dimmer"
    fingerprint mfr:"0063", prod:"4944", model:"3130", ver: "5.21", deviceJoinName: "GE Z-Wave Plus Toggle Dimmer"
    fingerprint mfr:"0063", prod:"4944", model:"3135", ver: "5.26", deviceJoinName: "Jasco Z-Wave Plus Wall Dimmer"
    fingerprint mfr:"0063", prod:"4944", model:"3136", ver: "5.21", deviceJoinName: "Jasco Z-Wave Plus 1000W Wall Dimmer"
    fingerprint mfr:"0063", prod:"4944", model:"3137", ver: "5.20", deviceJoinName: "Jasco Z-Wave Plus Toggle Dimmer"
  }

  preferences {
    //Dimmer General Settings
    input(
      type: "paragraph",
      element: "paragraph",
      title: "Dimmer General Settings",
      description: ""
    )
    input name: "ledMode", type: "enum", title: "Indicator LED Behavior", multiple: false, options: ["0": "On when load is off (default)", "1": "On when load is on", "2": "Always off"], required: false, displayDuringSetup: true
    input name: "inverted", type: "bool", title: "Switch orientation is inverted?", defaultValue: false, displayDuringSetup: true
    input name: "flashRate", type: "enum", title: "Flash rate", options: [[750: "750ms"], [1000: "1s"], [2000: "2s"], [5000: "5s"]], defaultValue: 750
    //Dimmer Timing Settings
    input(
      type: "paragraph",
      element: "paragraph",
      title: "Dimmer Timing Settings. Total dimming time = steps*duration",
      description: ""
    )
    input "paramZSteps", "number", title: "Z-Wave Dimming Steps", multiple: false, defaultValue: "1", range: "1..99", required: false, displayDuringSetup: true
    input "paramZDuration", "number", title: "Z-Wave Dimming Duration (in 10ms increments)", multiple: false, defaultValue: "3", range: "1..255", required: false, displayDuringSetup: true
    input "paramPSteps", "number", title: "Physical Dimming Steps", multiple: false, defaultValue: "1", range: "1..99", required: false, displayDuringSetup: true
    input "paramPDuration", "number", title: "Physical Dimming Duration (in 10ms increments)", multiple: false, defaultValue: "3", range: "1..255", required: false, displayDuringSetup: true
    //Configure Association Groups
    input(
      type: "paragraph",
      element: "paragraph",
      title: "Configure Association Groups",
      description: "Devices in association group 2 will receive Basic Set commands directly from the switch when it is turned on or off. Use this to control another device as if it was connected to this switch.\n\n" +
      "Devices in association group 3 will receive Basic Set commands directly from the switch when it is double tapped up or down.\n\n" +
      "Devices are entered as a comma delimited list of IDs in hexadecimal format."
    )
    input (name: "requestedGroup2", title: "Association Group 2 Members (Max of 5):", type: "text", required: false)
    input (name: "requestedGroup3", title: "Association Group 3 Members (Max of 4):", type: "text", required: false)
    //Logging
    input(
      type: "paragraph",
      element: "paragraph",
      title: "Logging",
      description: "Configure the amount of logging.  Excessive logging can slow down the system."
    )
    input name: "descriptionTextEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
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

// parse events into attributes
def parse(String description) {
  logDebug "description: $description"
  def result = null
  //def cmd = zwave.parse(description, [0x20: 1, 0x25: 1, 0x26: 3, 0x56: 1, 0x70: 2, 0x72: 2, 0x85: 2])
  def cmd = zwave.parse(description, commandClassVersions)
  if (cmd) {
    result = zwaveEvent(cmd)
    logDebug "Parsed ${cmd} to ${result.inspect()}"
  }
  else {
    log.warn "Non-parsed event: ${description}"
  }
  result    
}

private getCommandClassVersions() {
  [
    0x22: 1,  // Application Status
    0x59: 1,  // Assocation Group Information V1
    0x85: 2,  // Association V2
    0x20: 1,  // Basic
    0x5B: 3,  // Central Scene V3
    0x70: 4,  // Configuration
    0x56: 1,  // CRC16 Encap
    0x5A: 1,  // Device Reset Locally
    0x7A: 4,  // Firmware Update Md V4
    0x72: 2,  // Manufacturer Specific
    0x73: 1,  // Powerlevel
    0x2B: 1,  // Scene Activation
    0x2C: 1,  // Scene Actuator Conf
    //S2 not implemented in HE
    //0xZZ: 1,  // Security 2
    0x6C: 1,  // Supervision
    0x25: 1,  // Switch Binary
    0x26: 4,  // Switch Multilevel V4
    0x55: 2,  // Transport Service V2
    0x86: 2,  // Version V2
    0x5E: 2   // Z-Wave+ Info V2   

    /*
    0x22: 1,  // Application Status (Model 0308)
    0x30: 2,	// Sensor Binary
    0x59: 1,  // AssociationGrpInfo (Model 0308)
    0x5A: 1,  // DeviceResetLocally (Model 0308)
    0x5E: 2,  // ZwaveplusInfo (Model 0308)
    0x7A: 2,  // Firmware Update MD (Model 0308)
    0x71: 3,  // Alarm v1 or Notification (v4)
    0x72: 2,  // ManufacturerSpecific
    0x73: 1,  // Powerlevel (Model 0308)
    0x80: 1,  // Battery
    0x84: 2,  // WakeUp
    0x85: 2,  // Association
    0x86: 1,  // Version (v2)
    0x98: 1   // Security (Model 0308)
    */
  ]
}

def zwaveEvent(hubitat.zwave.commands.crc16encapv1.Crc16Encap cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.crc16encapv1.Crc16Encap cmd)"
  logTrace "cmd: $cmd"
  
  def version = commandClassVersions[cmd.commandClass.toInteger()]
  logTrace "version $version"
  def encapsulatedCommand = zwave.getCommand(cmd.commandClass, cmd.command, cmd.data, version)
  if (!encapsulatedCommand) {
    log.warn("zwaveEvent(): Could not extract command from ${cmd}")
  }
  else {
    logDebug("zwaveEvent(): Extracted command ${encapsulatedCommand}")
    return zwaveEvent(encapsulatedCommand)
  }
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
	dimmerEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
	dimmerEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelSet cmd) {
	dimmerEvents(cmd)
}

private dimmerEvents(hubitat.zwave.Command cmd) {
  logDebug "dimmerEvents(hubitat.zwave.Command cmd)"
  logTrace "cmd: $cmd"

  def currval = device.currentValue("switch")
  def value = (cmd.value ? "on" : "off")
  def result = []
  def type
  if (value != currval) {
    type = "physical"
    result << createEvent(name: "switch", value: value, type: type)
  }
  else {
    type = "digital"
  }
  if (cmd.value && cmd.value <= 100) {
    result << createEvent(name: "level", value: cmd.value, unit: "%", type: type)
  }
  return result
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd)"
  logTrace "cmd: $cmd"
  if (cmd.value == 255) {
    createEvent(name: "doubleTapped", value: 1, descriptionText: "Double-tap up (button 1) on $device.displayName", isStateChange: true, type: "physical")
  }
  else if (cmd.value == 0) {
    createEvent(name: "doubleTapped", value: 2, descriptionText: "Double-tap down (button 2) on $device.displayName", isStateChange: true, type: "physical")
  }
}

def zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd)"
  logTrace "cmd: $cmd"
  if (cmd.groupingIdentifier == 3) {
    if (cmd.nodeId.toString().contains(zwaveHubNodeId.toString())) {
      createEvent(name: "numberOfButtons", value: 2, displayed: false)
    }
    else {
      sendHubCommand(new hubitat.device.HubAction(zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()))
      sendHubCommand(new hubitat.device.HubAction(zwave.associationV2.associationGet(groupingIdentifier: 3).format()))
      sendEvent(name: "numberOfButtons", value: 0, displayed: false)
    }
  }
}

def zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd)"
  logTrace "cmd: $cmd"
  def name = ""
  def value = ""
  def reportValue = cmd.scaledConfigurationValue
  switch (cmd.parameterNumber) {
    case 3:
      name = "indicatorStatus"
      value = reportValue == 1 ? "when on" : reportValue == 2 ? "never" : "when off"
      break
    case 4:
      name = "inverted"
      value = reportValue == 1 ? "true" : "false"
      break
    case 7:
      name = "zwaveSteps"
      value = reportValue
      break
    case 8:
      name = "zwaveDelay"
      value = reportValue
      break
    case 9:
      name = "manualSteps"
      value = reportValue
      break
    case 10:
      name = "manualDelay"
      value = reportValue
      break
    case 11:
      name = "allSteps"
      value = reportValue
      break
    case 12:
      name = "allDelay"
      value = reportValue
      break
    default:
      break
  }
  createEvent([name: name, value: value, displayed: false])
}

def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd)"
  logTrace "cmd: $cmd"
  logDebug "manufacturerId:   ${cmd.manufacturerId}"
  logDebug "manufacturerName: ${cmd.manufacturerName}"
  logDebug "productId:        ${cmd.productId}"
  logDebug "productTypeId:    ${cmd.productTypeId}"
  def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
  def cmds = []
  if (!(msr.equals(getDataValue("MSR")))) {
    updateDataValue("MSR", msr)
    cmds << createEvent([descriptionText: "$device.displayName MSR: $msr", isStateChange: true, displayed: false])
  }
  if (!(cmd.manufacturerId.toString().equals(getDataValue("manufacturer")))) {
    updateDataValue("manufacturer", cmd.manufacturerId.toString())
    cmds << createEvent([descriptionText: "$device.displayName manufacturer ID: ${cmd.manufacturerId}", isStateChange: true, displayed: false])
  }
  if (!(cmd.manufacturerName.equals(getDataValue("manufacturerName")))) {
    updateDataValue("manufacturerName", cmd.manufacturerName)
    cmds << createEvent([descriptionText: "$device.displayName manufacturer name: ${cmd.manufacturerName}", isStateChange: true, displayed: false])
  }
  cmds
}

def zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport cmd)"
  logTrace "cmd: $cmd"
  logDebug("received Version Report")
  logDebug "applicationVersion:      ${cmd.applicationVersion}"
  logDebug "applicationSubVersion:   ${cmd.applicationSubVersion}"
  logDebug "zWaveLibraryType:        ${cmd.zWaveLibraryType}"
  logDebug "zWaveProtocolVersion:    ${cmd.zWaveProtocolVersion}"
  logDebug "zWaveProtocolSubVersion: ${cmd.zWaveProtocolSubVersion}"
  logDebug "firmware0Version:        ${cmd.firmware0Version}"
  logDebug "firmware0SubVersion:     ${cmd.firmware0SubVersion}"
  def ver = cmd.firmware0Version + '.' + cmd.firmware0SubVersion
  def cmds = []
  if (!(ver.equals(getDataValue("firmware")))) {
    updateDataValue("firmware", ver)
    cmds << createEvent([descriptionText: "Firmware version " + ver, isStateChange: true, displayed: false])
  }
  cmds
}


def zwaveEvent(hubitat.zwave.Command cmd) {
  log.warn "${device.displayName} received unhandled command: ${cmd}"
}

// handle commands
def configure() {
  logDebug "configure()"
  
  def cmds = []
  // Get current config parameter values
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 3).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 4).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 7).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 8).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 9).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 10).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 11).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 12).format()

  // Add the hub to association group 3 to get double-tap notifications
  cmds << zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()
  cmds << zwave.associationV2.associationGet(groupingIdentifier: 3).format()

  delayBetween(cmds, 500)
}

def updated() {
  logDebug "updated()"
  if (state.lastUpdated && now() <= state.lastUpdated + 3000) return
  state.lastUpdated = now()

  if (logEnable || traceLogEnable) {
    log.warn "Debug logging with auto-off in 30 minutes!"
    unschedule()
    runIn(1800, logsOff)
  }

  def nodes = []
  def cmds = []

  if (settings.requestedGroup2 != state.currentGroup2) {
    nodes = parseAssocGroupList(settings.requestedGroup2, 2)
    cmds << zwave.associationV2.associationRemove(groupingIdentifier: 2, nodeId: [])
    cmds << zwave.associationV2.associationSet(groupingIdentifier: 2, nodeId: nodes)
    cmds << zwave.associationV2.associationGet(groupingIdentifier: 2)
    state.currentGroup2 = settings.requestedGroup2
  }

  if (settings.requestedGroup3 != state.currentGroup3) {
    nodes = parseAssocGroupList(settings.requestedGroup3, 3)
    cmds << zwave.associationV2.associationRemove(groupingIdentifier: 3, nodeId: [])
    cmds << zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: nodes)
    cmds << zwave.associationV2.associationGet(groupingIdentifier: 3)
    state.currentGroup3 = settings.requestedGroup3
  }

  if (settings.inverted != state.inverted) {
    cmds << zwave.configurationV2.configurationSet(configurationValue: [settings.inverted ? 1 : 0], parameterNumber: 4, size: 1).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 4).format()
    state.inverted = settings.inverted
  }

  settings.ledMode = settings.ledMode ?.toInteger() ?: 0
  if (settings.ledMode != state.ledMode) {
    cmds << zwave.configurationV2.configurationSet(configurationValue: [settings.ledMode], parameterNumber: 3, size: 1).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 3).format()
    state.ledMode = settings.ledMode.toInteger()
  }

  // Set Z Steps
  settings.paramZSteps = settings.paramZSteps?.toInteger() ?: 1
  if (settings.paramZSteps != state.paramZSteps) {
    cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: settings.paramZSteps, parameterNumber: 7, size: 1).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 7).format()
    state.paramZSteps = settings.paramZSteps
  }

  // Set Z Duration
  settings.paramZDuration = settings.paramZDuration?.toInteger() ?: 3
  if (settings.paramZDuration != state.paramZDuration) {
    cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: settings.paramZDuration, parameterNumber: 8, size: 2).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 8).format()
    state.paramZDuration = settings.paramZDuration
  }

  // Set P Steps
  settings.paramPSteps = settings.paramPSteps?.toInteger() ?: 1
  if (settings.paramPSteps != state.paramPSteps) {
    cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: settings.paramPSteps, parameterNumber: 9, size: 1).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 9).format()
    state.paramPSteps = settings.paramPSteps
  }

  // Set P Duration
  settings.paramPDuration = settings.paramPDuration?.toInteger() ?: 3
  if (settings.paramPDuration != state.paramPDuration) {
    cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: settings.paramPDuration, parameterNumber: 10, size: 2).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 10).format()
    state.paramPDuration = settings.paramPDuration
  }

  //sendHubCommand(cmds.collect{ new hubitat.device.HubAction(it.format()) }, 500)
  if (cmds) {
    delayBetween(cmds, 500)
  }
}

def indicatorWhenOn() {
	sendEvent(name: "indicatorStatus", value: "when on", display: false)
	zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 3, size: 1).format()
}

def indicatorWhenOff() {
	sendEvent(name: "indicatorStatus", value: "when off", display: false)
	zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 3, size: 1).format()
}

def indicatorNever() {
	sendEvent(name: "indicatorStatus", value: "never", display: false)
	zwave.configurationV2.configurationSet(configurationValue: [2], parameterNumber: 3, size: 1).format()
}

def doubleUp() {
	sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "Double-tap up (button 1) on $device.displayName", isStateChange: true, type: "digital")
}

def doubleDown() {
	sendEvent(name: "button", value: "pushed", data: [buttonNumber: 2], descriptionText: "Double-tap down (button 2) on $device.displayName", isStateChange: true, type: "digital")
}

def setZwaveSteps(steps) {
	steps = Math.max(Math.min(steps, 99), 1)
	sendEvent(name: "zwaveSteps", value: steps, displayed: false)	
	zwave.configurationV2.configurationSet(scaledConfigurationValue: steps, parameterNumber: 7, size: 1).format()
}

def setZwaveDelay(delay) {
	delay = Math.max(Math.min(delay, 255), 1)
	sendEvent(name: "zwaveDelay", value: delay, displayed: false)
	sendHubCommand(new hubitat.device.HubAction(zwave.configurationV2.configurationSet(scaledConfigurationValue: delay, parameterNumber: 8, size: 2).format()))
}

def setManualSteps(steps) {
	steps = Math.max(Math.min(steps, 99), 1)
	sendEvent(name: "manualSteps", value: steps, displayed: false)	
	zwave.configurationV2.configurationSet(scaledConfigurationValue: steps, parameterNumber: 9, size: 1).format()
}

def setManualDelay(delay) {
	delay = Math.max(Math.min(delay, 255), 1)
	sendEvent(name: "manualDelay", value: delay, displayed: false)
	zwave.configurationV2.configurationSet(scaledConfigurationValue: delay, parameterNumber: 10, size: 2).format()
}

def setAllSteps(steps) {
	steps = Math.max(Math.min(steps, 99), 1)
	sendEvent(name: "allSteps", value: steps, displayed: false)	
	zwave.configurationV2.configurationSet(scaledConfigurationValue: steps, parameterNumber: 11, size: 1).format()
}

def setAllDelay(delay) {
	delay = Math.max(Math.min(delay, 255), 1)
	sendEvent(name: "allDelay", value: delay, displayed: false)
	zwave.configurationV2.configurationSet(scaledConfigurationValue: delay, parameterNumber: 12, size: 2).format()
}

def poll() {
  def cmds = []
  cmds << zwave.switchMultilevelV2.switchMultilevelGet().format()
  if (getDataValue("MSR") == null) {
    cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
  }
  delayBetween(cmds,500)
}

def refresh() {
  def cmds = []
  
  state.clear()
  
  cmds << zwave.switchMultilevelV2.switchMultilevelGet().format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 3).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 4).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 7).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 8).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 9).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 10).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 11).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 12).format()
  cmds << zwave.associationV2.associationGet(groupingIdentifier: 3).format()
  cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
  cmds << zwave.versionV1.versionGet().format()
  delayBetween(cmds, 500)
}

def on() {
  logDebug "on()"
  state.flashing = false
  sendEvent(name: "switch", value: "on", isStateChange: true, descriptionText: "$device.displayName is on", type: "digital")
  def cmds = []
  cmds << zwave.basicV1.basicSet(value: 0xFF).format()
  cmds << zwave.switchMultilevelV2.switchMultilevelGet().format()
  delayBetween(cmds, getDelay())
}

def off() {
  logDebug "off()"
  state.flashing = false
  sendEvent(name: "switch", value: "off", isStateChange: true, descriptionText: "$device.displayName is off", type: "digital")
  def cmds = []
  cmds << zwave.basicV1.basicSet(value: 0x00).format()
  cmds << zwave.switchMultilevelV2.switchMultilevelGet().format()
  delayBetween(cmds, getDelay())
}

def flash() {
  logDebug "flash()"
  logInfo "${device.getDisplayName()} was set to flash with a rate of ${flashRate} milliseconds"
  state.flashing = true
  flashOn()
}

def flashOn() {
  if (!state.flashing) return
  runInMillis(flashRate.toInteger(), flashOff)
  return [zwave.basicV1.basicSet(value: 0xFF).format()]
}

def flashOff() {
  if (!state.flashing) return
  runInMillis(flashRate.toInteger(), flashOn)
  return [zwave.basicV1.basicSet(value: 0x00).format()]
}

def setLevel(value) {
  logDebug "setLevel($value)"
  state.flashing = false
  def valueaux = value as Integer
  def level = Math.max(Math.min(valueaux, 99), 0)
  def currval = device.currentValue("switch")
  //def delay = 0

  if (level > 0 && currval == "off") {
    sendEvent(name: "switch", value: "on", descriptionText: "${device.label} is on")
  }
  else if (level == 0 && currval == "on") {
    sendEvent(name: "switch", value: "off", descriptionText: "${device.label} is off")
    //delay += 2000
  }
  sendEvent(name: "level", value: level, unit: "%", descriptionText: "Level for ${device.label} is ${level}")
  def zsteps = device.currentValue("zwaveSteps") ?: 1
  def zdelay = device.currentValue("zwaveDelay") ?: 3
  def delay = (zsteps * zdelay * level / 100).longValue() + 1000
  logDebug "values|| zsteps: $zsteps, zdelay: $zdelay, level: $level, delay: $delay"
  delayBetween ([
    zwave.basicV1.basicSet(value: level).format(),
    zwave.switchMultilevelV1.switchMultilevelGet().format()
  ], delay )
}

def setLevel(value, duration) {
  logDebug "setLevel($value, $duration)"
  state.flashing = false
  def currval = device.currentValue("switch")
  value = Math.max(Math.min(value.toInteger(), 99), 0)
  if (value > 0 && currval == "off") {
    sendEvent(name: "switch", value: "on", descriptionText: "${device.label} is on")
  }
  else if (value == 0 && currval == "on") {
    sendEvent(name: "switch", value: "off", descriptionText: "${device.label} is off")
    getStatusDelay += 2000
  }
  
  def dimmingDuration = duration < 128 ? duration : 128 + Math.round(duration / 60)
  def getStatusDelay = duration < 128 ? (duration * 1000) + 2000 : (Math.round(duration / 60)*60*1000)+2000
  logDebug "values|| getStatusDelay: $getStatusDelay, dimmingDuration: $dimmingDuration"
  delayBetween ([
    zwave.switchMultilevelV2.switchMultilevelSet(value: value, dimmingDuration: dimmingDuration).format(),
    zwave.switchMultilevelV1.switchMultilevelGet().format()
  ], getStatusDelay)
}

def levelUp() {
  int nextLevel = device.currentValue("level") + 10
  if( nextLevel > 100) {
    nextLevel = 100
  }
  setLevel(nextLevel)
}
	
def levelDown() {
  int nextLevel = device.currentValue("level") - 10
  if( nextLevel < 0) {
    nextLevel = 0
  }
  if (nextLevel == 0) {
    off()
  }
  else {
    setLevel(nextLevel)
  }
}

def logsOff(){
  log.warn "Debug/Trace logging turning off"
  device.updateSetting("logEnable", [value: "false", type: "bool"])
  device.updateSetting("traceLogEnable", [value: "false", type: "bool"])
}

// Private Methods

private getDelay() {
  def zsteps = device.currentValue("zwaveSteps") ?: 1
  def zdelay = device.currentValue("zwaveDelay") ?: 3
  def result = (zsteps * zdelay * 1000).longValue() + 1000
  logTrace "delay: $result"
  return result
}

private parseAssocGroupList(list, group) {
  def nodes = group == 2 ? [] : [zwaveHubNodeId]
  if (list) {
    def nodeList = list.split(',')
    def max = group == 2 ? 5 : 4
    def count = 0

    nodeList.each { node ->
      node = node.trim()
      if ( count >= max) {
        log.warn "Association Group ${group}: Number of members is greater than ${max}! The following member was discarded: ${node}"
      }
      else if (node.matches("\\p{XDigit}+")) {
        def nodeId = Integer.parseInt(node,16)
        if (nodeId == zwaveHubNodeId) {
          log.warn "Association Group ${group}: Adding the hub as an association is not allowed (it would break double-tap)."
        }
        else if ( (nodeId > 0) & (nodeId < 256) ) {
          nodes << nodeId
          count++
            }
        else {
          log.warn "Association Group ${group}: Invalid member: ${node}"
        }
      }
      else {
        log.warn "Association Group ${group}: Invalid member: ${node}"
      }
    }
  }

  return nodes
}