/**
 *  Jasco Z-Wave Plus Switch
 *  Z-Wave Alliance Page: https://products.z-wavealliance.org/products/1879 (fw 5.2)
 *
 *  Copyright 2019 Ben Rimmasch
 *
 *  Modified from the work by NuttyTree device handler for SmartThings and smr device driver for Hubitat
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
 *	Date:   2019-07-13
 *
 *	Changelog:
 *
 *  1.0      2019-07-13 Initial Version
 *
 *
 *   Button Mappings:
 *
 *   ACTION          BUTTON#    BUTTON ACTION
 *   Double-Tap Up     1        double tapped
 *   Double-Tap Down   2        double tapped
 *
 */

metadata {
  definition(name: "Jasco Z-Wave Plus Switch", namespace: "codahq-hubitat", author: "Ben Rimmasch") {
    capability "Actuator"
    capability "PushableButton"
    capability "DoubleTapableButton"
    capability "Configuration"
    capability "Indicator"
    capability "Polling"
    capability "Refresh"
    capability "Sensor"
    capability "Switch"

    attribute "inverted", "enum", ["inverted", "not inverted"]

    command "doubleUp"
    command "doubleDown"
    command "flash"

    // These include version because there are older firmwares that don't support double-tap or the extra association groups
    fingerprint mfr: "0063", prod: "4952", model: "3036", ver: "5.20", deviceJoinName: "GE Z-Wave Plus Wall Switch"
    fingerprint mfr: "0063", prod: "4952", model: "3037", ver: "5.20", deviceJoinName: "GE Z-Wave Plus Toggle Switch"
    fingerprint mfr: "0063", prod: "4952", model: "3038", ver: "5.20", deviceJoinName: "GE Z-Wave Plus Toggle Switch"
    fingerprint mfr: "0063", prod: "4952", model: "3130", ver: "5.20", deviceJoinName: "Jasco Z-Wave Plus Wall Switch"
    fingerprint mfr: "0063", prod: "4952", model: "3131", ver: "5.20", deviceJoinName: "Jasco Z-Wave Plus Toggle Switch"
    fingerprint mfr: "0063", prod: "4952", model: "3132", ver: "5.20", deviceJoinName: "Jasco Z-Wave Plus Toggle Switch"
  }

  preferences {
    input(
      type: "paragraph",
      element: "paragraph",
      title: "Configure Association Groups:",
      description: "Devices in association group 2 will receive Basic Set commands directly from the switch when it is turned on or off. Use this to control another device as if it was connected to this switch.\n\n" +
      "Devices in association group 3 will receive Basic Set commands directly from the switch when it is double tapped up or down.\n\n" +
      "Devices are entered as a comma delimited list of IDs in hexadecimal format."
    )

    input(name: "requestedGroup2", title: "Association Group 2 Members (Max of 5):", type: "text", required: false)
    input(name: "requestedGroup3", title: "Association Group 3 Members (Max of 4):", type: "text", required: false)
    input name: "ledMode", type: "enum", title: "Indicator LED Behavior", multiple: false, options: ["0": "On when load is off (default)", "1": "On when load is on", "2": "Always off"], required: false, displayDuringSetup: true
    input name: "inverted", type: "bool", title: "Switch orientation is inverted?", defaultValue: false, displayDuringSetup: true
    input name: "flashRate", type: "enum", title: "Flash rate", options: [[750: "750ms"], [1000: "1s"], [2000: "2s"], [5000: "5s"]], defaultValue: 750
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
  logDebug "parse(String description)"
  logTrace "description: $description"
  def result = null
  //def cmd = zwave.parse(description, [0x20: 1, 0x25: 1, 0x56: 1, 0x70: 2, 0x72: 2, 0x85: 2])
  def cmd = zwave.parse(description, commandClassVersions)
  if (cmd) {
    result = zwaveEvent(cmd)
    logDebug "Parsed ${cmd} to ${result.inspect()}"
  } else {
    log.warn "Non-parsed event: ${description}"
  }
  result
}

private getCommandClassVersions() {
  [
    0x59: 1,  // Assocation Group Information V1
    0x85: 2,  // Command Class Association V2
    0x20: 1,  // Command Class Basic
    0x70: 2,  // Command Class Configuration
    0x56: 1,  // Command Class CRC16 Encap
    0x5A: 1,  // Command Class Device Reset Locally
    0x7A: 2,  // Command Class Firmware Update Md V2
    0x72: 2,  // Command Class Manufacturer Specific
    0x73: 1,  // Command Class Powerlevel
    0x2B: 1,  // Command Class Scene Activation
    0x2C: 1,  // Command Class Scene Actuator Conf
    0x25: 1,  // Switch Binary
    0x86: 2,  // Command Class Version V2
    0x5E: 2   // Command Class Z-Wave+ Info V2   

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

  // Configuration = 112 decimal
  // Manufacturer Specific = 114 decimal
  // Association = 133 decimal
  def version = [112, 114, 133].contains(cmd.commandClass) ? 2 : 1

  def encapsulatedCommand = zwave.getCommand(cmd.commandClass, cmd.command, cmd.data, version)
  if (!encapsulatedCommand) {
    logDebug "zwaveEvent(): Could not extract command from ${cmd}"
  } else {
    return zwaveEvent(encapsulatedCommand)
  }
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd)"
  logTrace "cmd: $cmd"
  createEvent(name: "switch", value: cmd.value ? "on" : "off", isStateChange: true, type: "digital")
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
      createEvent(name: "numberOfButtons", value: 0, displayed: false)
    }
  }
}

def zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd)"
  logTrace "cmd: $cmd"
  def name = ""
  def value = ""
  def reportValue = cmd.configurationValue[0]
  switch (cmd.parameterNumber) {
    case 3:
      name = "indicatorStatus"
      value = reportValue == 1 ? "when on" : reportValue == 2 ? "never" : "when off"
      break
    case 4:
      name = "inverted"
      value = reportValue == 1 ? "true" : "false"
      break
    default:
      break
  }
  createEvent([name: name, value: value, displayed: false])
}

def zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
  state.flashing = false
  logDebug "zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd)"
  logTrace "cmd: $cmd"
  createEvent(name: "switch", value: cmd.value ? "on" : "off", isStateChange: true, type: "physical")
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
  }
  if (!(cmd.manufacturerName.equals(getDataValue("manufacturer")))) {
    updateDataValue("manufacturer", cmd.manufacturerName)
  }
  cmds << createEvent([descriptionText: "$device.displayName MSR: $msr", isStateChange: true, displayed: false])
  cmds << createEvent([descriptionText: "$device.displayName manufacturer: $msr", isStateChange: true, displayed: false])
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
  def ver = cmd.applicationVersion + '.' + cmd.applicationSubVersion
  if (!(ver.equals(getDataValue("firmware")))) {
    updateDataValue("firmware", ver)
  }
  createEvent([descriptionText: "Firmware V" + ver, isStateChange: true, displayed: false])
}


def zwaveEvent(hubitat.zwave.Command cmd) {
  log.warn "${device.displayName} received unhandled command: ${cmd}"
}

// handle commands
def configure() {
  logDebug "configure()"

  //remove old state variables
  //state.remove("bin")
  //state.remove("manufacturer")

  def cmds = []
  // Get current config parameter values
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 3).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 4).format()

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
    cmds << zwave.associationV2.associationRemove(groupingIdentifier: 2, nodeId: []).format()
    cmds << zwave.associationV2.associationSet(groupingIdentifier: 2, nodeId: nodes).format()
    cmds << zwave.associationV2.associationGet(groupingIdentifier: 2).format()
    state.currentGroup2 = settings.requestedGroup2
  }

  if (settings.requestedGroup3 != state.currentGroup3) {
    nodes = parseAssocGroupList(settings.requestedGroup3, 3)
    cmds << zwave.associationV2.associationRemove(groupingIdentifier: 3, nodeId: []).format()
    cmds << zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: nodes).format()
    cmds << zwave.associationV2.associationGet(groupingIdentifier: 3).format()
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

  if (cmds) {
    delayBetween(cmds, 500)
  }
}

def indicatorWhenOn() {
  sendEvent(name: "indicatorStatus", value: "when on", display: false)
  sendHubCommand(new hubitat.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 3, size: 1).format()))
}

def indicatorWhenOff() {
  sendEvent(name: "indicatorStatus", value: "when off", display: false)
  sendHubCommand(new hubitat.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 3, size: 1).format()))
}

def indicatorNever() {
  sendEvent(name: "indicatorStatus", value: "never", display: false)
  sendHubCommand(new hubitat.device.HubAction(zwave.configurationV2.configurationSet(configurationValue: [2], parameterNumber: 3, size: 1).format()))
}

def doubleUp() {
  sendEvent(name: "doubleTapped", value: 1, descriptionText: "Double-tap up (button 1) on $device.displayName", isStateChange: true, type: "digital")
}

def doubleDown() {
  sendEvent(name: "doubleTapped", value: 2, descriptionText: "Double-tap down (button 2) on $device.displayName", isStateChange: true, type: "digital")
}

def poll() {
  def cmds = []
  cmds << zwave.switchBinaryV1.switchBinaryGet().format()
  if (getDataValue("MSR") == null) {
    cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
  }
  delayBetween(cmds, 500)
}

def refresh() {
  def cmds = []
  cmds << zwave.switchBinaryV1.switchBinaryGet().format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 3).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 4).format()
  cmds << zwave.associationV2.associationGet(groupingIdentifier: 3).format()
  cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
  cmds << zwave.versionV1.versionGet().format()
  delayBetween(cmds, 500)
}

def on() {
  state.flashing = false
  delayBetween([
    zwave.basicV1.basicSet(value: 0xFF).format(),
    zwave.basicV1.basicGet().format()
  ], 100)
}

def off() {
  state.flashing = false
  delayBetween([
    zwave.basicV1.basicSet(value: 0x00).format(),
    zwave.basicV1.basicGet().format()
  ], 100)
}

def flash() {
  def descriptionText = "${device.getDisplayName()} was set to flash with a rate of ${flashRate} milliseconds"
  if (txtEnable) log.info "${descriptionText}"
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


def logsOff(){
  log.warn "Debug/Trace logging turning off"
  device.updateSetting("logEnable", [value: "false", type: "bool"])
  device.updateSetting("traceLogEnable", [value: "false", type: "bool"])
}


// Private Methods

private parseAssocGroupList(list, group) {
  def nodes = group == 2 ? [] : [zwaveHubNodeId]
  if (list) {
    def nodeList = list.split(',')
    def max = group == 2 ? 5 : 4
    def count = 0

    nodeList.each {
      node ->
        node = node.trim()
      if (count >= max) {
        log.warn "Association Group ${group}: Number of members is greater than ${max}! The following member was discarded: ${node}"
      }
      else if (node.matches("\\p{XDigit}+")) {
        def nodeId = Integer.parseInt(node, 16)
        if (nodeId == zwaveHubNodeId) {
          log.warn "Association Group ${group}: Adding the hub as an association is not allowed (it would break double-tap)."
        }
        else if ((nodeId > 0) & (nodeId < 256)) {
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