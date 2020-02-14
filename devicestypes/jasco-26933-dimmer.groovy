/**
 *  GE Z-Wave Plus Motion Dimmer
 *  https://products.z-wavealliance.org/products/2108
 *
 *  Thanks to original ST DTH author Matt Lebaugh (@mlebaugh)
 *  Thanks to Jason Bottjen for some HE changes
 *
 *	Author: Ben Rimmasch
 *	Date: 2020-01-21
 *
 *  Changelog:
 *  1.0.0     2020-01-21 Initial Release
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
 */

metadata {
  definition(name: "GE Z-Wave Plus Motion Dimmer", namespace: "codahq-hubitat", author: "Ben Rimmasch") {
    capability "Actuator"
    capability "Motion Sensor"
    capability "PushableButton"
    capability "Configuration"
    capability "Refresh"
    capability "Sensor"
    capability "Switch"
    capability "Switch Level"
    capability "Light"

    command "flash"
    command "setDefaultDimmerLevel", [[name: "Default Dimmer Level", type: "NUMBER", description: "Default Dimmer Level Used when Turning ON. (0=Last Dimmer Value)", range: "0..99"]]
  }

  preferences {
    //
    input(type: "paragraph", element: "paragraph", title: "Dimmer General Settings", description: "")
    input "paramLightTimer", "enum", title: "Light Timeout", description: "Length of time after no motion for the light to shut off in Occupancy/Vacancy modes", options: ["0" : "5 seconds", "1" : "1 minute", "5" : "5 minutes (default)", "15" : "15 minutes", "30" : "30 minutes", "255" : "disabled"], required: false, displayDuringSetup: true
    input "paramOperationMode", "enum", title: "Operating Mode", description: "Occupancy: Automatically turn on and off the light with motion\nVacancy: Manually turn on, automatically turn off light with no motion.", options: ["1" : "Manual", "2" : "Vacancy", "3" : "Occupancy (default)"], required: false, displayDuringSetup: true
    input "paramInverted", "enum", title: "Switch Buttons Direction", multiple: false, options: ["0" : "Normal (default)", "1" : "Inverted"], required: false, displayDuringSetup: true
    input "paramMotionEnabled", "enum", title: "Motion Sensor", description: "Enable/Disable Motion Sensor.", options: ["0" : "Disable", "1" : "Enable (default)"], required: false
    input "paramMotionSensitivity", "enum", title: "Motion Sensitivity", description: "Set a level", options: ["1" : "High", "2" : "Medium (default)", "3" : "Low"], required: false, displayDuringSetup: true
    input "paramLightSense", "enum", title: "Light Sensing", description: "If enabled, Occupancy mode will only turn light on if it is dark", options: ["0" : "Disabled", "1" : "Enabled (default)"], required: false, displayDuringSetup: true
    input "paramMotionResetTimer", "enum", title: "Motion Detection Reset Time", options: ["0" : "Disabled", "1" : "10 sec", "2" : "20 sec (default)", "3" : "30 sec", "4" : "45 sec", "110" : "27 mins"], required: false
    //
    input(type: "paragraph", element: "paragraph", title: "Dimmer Timing Settings. Total dimming time = steps*duration", description: "")
    input "paramZSteps", "number", title: "Z-Wave Dimming Steps", multiple: false, defaultValue: "1", range: "1..99", required: false, displayDuringSetup: true
    input "paramZDuration", "number", title: "Z-Wave Dimming Duration (in 10ms increments)", multiple: false, defaultValue: "3", range: "1..255", required: false, displayDuringSetup: true
    input "paramPSteps", "number", title: "Physical Dimming Steps", multiple: false, defaultValue: "1", range: "1..99", required: false, displayDuringSetup: true
    input "paramPDuration", "number", title: "Physical Dimming Duration (in 10ms increments)", multiple: false, defaultValue: "3", range: "1..255", required: false, displayDuringSetup: true
    input "paramSwitchMode", "enum", title: "Switch Mode Enable (physical switch buttons only do ON/OFF - no dimming)", multiple: false, options: ["0" : "Disable (default)", "1" : "Enable"], required: false, displayDuringSetup: true
    input "paramDefaultDimmerLevel", "number", title: "Default Dimmer Level (0=Last Dimmer Level)", multiple: false, defaultValue: "0", range: "0..99", required: false, displayDuringSetup: true
    input "paramDimUpRate", "enum", title: "Speed to Dim up the light to the default level", multiple: false, options: ["0" : "Quickly (Default)", "1" : "Slowly"], required: false, displayDuringSetup: true
    input name: "flashRate", type: "enum", title: "Flash rate", options: [[750: "750ms"], [1000: "1s"], [2000: "2s"], [5000: "5s"]], defaultValue: 750
    //	 
    input(type: "paragraph", element: "paragraph", title: "Logging", description: "")
    input name: "descriptionTextEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    input name: "traceLogEnable", type: "bool", title: "Enable trace logging", defaultValue: false
  }
}

//Z-Wave Messages
def parse(String description) {  
  logDebug "parse(description)"
  logTrace "description: $description"

  def result = []
  def cmd = null
  try {
    cmd = zwave.parse(description, CommandClassVersions)
  }
  catch (e) {
    log.error "zwave.parse(description) failed to parse description:  ${description}"
  }

  if (cmd) {
    result += zwaveEvent(cmd)
  }
  else {
    log.warn "Unable to parse description: $description"
  }
  return result
}

private getCommandClassVersions() {
  [
    //The full list from https://products.z-wavealliance.org/products/2108/classes
    0x59: 1,  // AssociationGrpInfo
    0x85: 2,  // Association V2
    0x20: 1,  // Basic
    0x70: 1,  // Configuration
    0x56: 1,  // CRC16 Encap
    0x5A: 1,  // Device Reset Locally
    0x7A: 2,  // Firmware Update MD V2
    0x72: 2,  // Manufacturer Specific V2
    0x8E: 2,  // Multi Channel Association V2
    0x60: 4,  // Multi Channel V4
    0x71: 4,  // Notification V4
    0x73: 1,  // Powerlevel
    0x2B: 1,  // Scene Activation
    0x2C: 1,  // Scene Actuator Conf
    0x27: 1,  // Switch All
    0x26: 2,  // Switch Multilevel V2
    0x86: 2,  // Version V2
    0x5E: 2,  // ZwaveplusInfo V2
  ]
}


def zwaveEvent(hubitat.zwave.commands.crc16encapv1.Crc16Encap cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.crc16encapv1.Crc16Encap cmd)"
  logTrace "cmd: $cmd"

  def newVersion = 1

  // SwitchMultilevel = 38 decimal
  // Configuration = 112 decimal
  // Notification = 113 decimal
  // Manufacturer Specific = 114 decimal
  // Association = 133 decimal
  if (cmd.commandClass == 38) { newVersion = 3 }
  if (cmd.commandClass == 112) { newVersion = 2 }
  if (cmd.commandClass == 113) { newVersion = 3 }
  if (cmd.commandClass == 114) { newVersion = 2 }
  if (cmd.commandClass == 133) { newVersion = 2 }

  def encapsulatedCommand = zwave.getCommand(cmd.commandClass, cmd.command, cmd.data, newVersion)
  if (encapsulatedCommand) {
    zwaveEvent(encapsulatedCommand)
  }
  else {
    log.warn "Unable to extract CRC16 command from ${cmd}"
  }
}

def zwaveEvent(hubitat.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
  log.warn "This device has no children devices and as such I don't think it should ever send one of these.  However, it " +
    "seems to be.  This may be caused by a bad Z-Wave device that is sending super rogue messages.  I'll have to do some hunting.  " +
    "I can probably remove this later."
  
  //I copied and pasted this from another device and I'm leaving it all just to see what it does with it.
  
  def map = [ name: "switch$cmd.sourceEndPoint" ]

  def encapsulatedCommand = cmd.encapsulatedCommand([0x32: 3, 0x25: 1, 0x20: 1])
  if (encapsulatedCommand && cmd.commandClass == 50) {
    zwaveEvent(encapsulatedCommand, cmd.sourceEndPoint)
  }
  else {
    switch(cmd.commandClass) {
      case 32:
        if (cmd.parameter == [0]) {
          map.value = "off"
        }
        if (cmd.parameter == [255]) {
          map.value = "on"
        }
        createEvent(map)
        break
      case 37:
        if (cmd.parameter == [0]) {
          map.value = "off"
        }
        if (cmd.parameter == [255]) {
          map.value = "on"
        }
        createEvent(map)
        break
    }
  }
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd)"
  logTrace "cmd: $cmd"
  //createEvent(name: "switch", value: cmd.value ? "on" : "off", isStateChange: true)
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd)"
  logTrace "cmd: $cmd"
  def result = []
  return result
}

def zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd)"
  logTrace "cmd: $cmd"
  
  if (cmd.groupingIdentifier == 3) {
    if (cmd.nodeId.contains(zwaveHubNodeId)) {
      sendEvent(name: "numberOfButtons", value: 2, displayed: false)
    }
    else {
      sendEvent(name: "numberOfButtons", value: 0, displayed: false)
      zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()
      zwave.associationV2.associationGet(groupingIdentifier: 3).format()
    }
  }
}

def zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd)"
  logTrace "cmd: $cmd"
  
  def config = cmd.scaledConfigurationValue.toInteger()
  def result = []
  def name = ""
  def value = ""
  def reportValue = config // cmd.configurationValue[0]
  switch (cmd.parameterNumber) {
    case 1:
      name = "Light Timeout"
      value = reportValue == 0 ? "5 seconds" : reportValue == 1 ? "1 minute" : reportValue == 5 ? "5 minutes (default)" : reportValue == 15 ? "15 minutes" : reportValue == 30 ? "30 minutes" : reportValue == 255 ? "disabled" : "error"
      break
    case 3:
      name = "Operating Mode"
      value = reportValue == 1 ? "Manual" : reportValue == 2 ? "Vacancy" : reportValue == 3 ? "Occupancy (default)" : "error"
      break
    case 5:
      name = "Invert Buttons"
      value = reportValue == 0 ? "Disabled (default)" : reportValue == 1 ? "Enabled" : "error"
      break
    case 6:
      name = "Motion Sensor"
      value = reportValue == 0 ? "Disabled" : reportValue == 1 ? "Enabled (default)" : "error"
      break
    case 7:
      name = "Z-Wave Dimming Number of Steps"
      value = reportValue
      break
    case 8:
      name = "Z-Wave Dimming Step Duration"
      value = reportValue
      break
    case 9:
      name = "Physical Dimming Number of Steps"
      value = reportValue
      break
    case 10:
      name = "Physical Dimming Step Duration"
      value = reportValue
      break
    case 13:
      name = "Motion Sensitivity"
      value = reportValue == 1 ? "High" : reportValue == 2 ? "Medium (default)" : reportValue == 3 ? "Low" : "error"
      break
    case 14:
      name = "Light Sensing"
      value = reportValue == 0 ? "Disabled" : reportValue == 1 ? "Enabled (default)" : "error"
      break
    case 15:
      name = "Motion Reset Timer"
      value = reportValue == 0 ? "Disabled" : reportValue == 1 ? "10 seconds" : reportValue == 2 ? "20 seconds (default)" : reportValue == 3 ? "30 seconds" : reportValue == 4 ? "45 seconds" : reportValue == 110 ? "27 minutes" : "error"
      break
    case 16:
      name = "Switch Mode"
      value = reportValue == 0 ? "Disabled (default)" : reportValue == 1 ? "Enabled" : "error"
      break
    case 17:
      name = "Switch Mode Dimmer Level"
      value = reportValue
      break
    case 18:
      name = "Dimming Rate"
      value = reportValue == 0 ? "Quickly (default)" : reportValue == 1 ? "Slowly" : "error"
      break
    default:
      log.warn "Parameter ${cmd.parameterNumber} is not handled!"
      break
  }
  result << createEvent([name: name, value: value, displayed: false])
  return result
}

def zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd)"
  logTrace "cmd: $cmd"

  def value = cmd.value ? "on" : "off"
  def msg = "${device.label} is ${value}"
  logInfo msg
  createEvent([name: "switch", value: value, descriptionText: "$msg", isStateChange: true])
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
  updateDataValue("manufacturerName", cmd.manufacturerName)
  sendEvent([descriptionText: "$device.displayName MSR: $msr", isStateChange: false])
}

def zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport cmd)"
  logTrace "cmd: $cmd"
  
  def fw = "${cmd.firmware0Version}.${cmd.firmware0SubVersion}"
  updateDataValue("firmware", fw)
  logDebug "${device.displayName} is running firmware version: $fw, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
}

def zwaveEvent(hubitat.zwave.commands.hailv1.Hail cmd) {
  log.warn "Hail command received..."
  [name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false]
}

def zwaveEvent(hubitat.zwave.commands.notificationv3.NotificationReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.notificationv3.NotificationReport cmd)"
  logTrace "cmd: $cmd"

  def result = []
  if (cmd.notificationType == 0x07) {
    if (cmd.event == 0x00) {
      result << createEvent(name: "motion", value: "inactive", descriptionText: "$device.displayName motion has stopped", isStateChange: true)
      logInfo "Motion for device ${device.label} is inactive"
    }
    else if (cmd.event == 0x08) {
      result << createEvent(name: "motion", value: "active", descriptionText: "$device.displayName detected motion", isStateChange: true)
      logInfo "Motion for device ${device.label} is active"
    }
  }
  result
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd)"
  logTrace "cmd: $cmd"

  def value = cmd.value ? "on" : "off"
  def msg = "${device.label} is ${value}"
  if (cmd.value) {
    sendEvent(name: "level", value: cmd.value, unit: "%", descriptionText: "Level for ${device.label} is ${level}")
    if (device.currentValue("switch") == "off") {
      logInfo msg
      sendEvent(name: "switch", value: "on", descriptionText: "$msg", isStateChange: true, type: "physical")
    }
  }
  else {
    if (device.currentValue("switch") == "on") {
      logInfo msg
      sendEvent(name: "switch", value: "off", descriptionText: "$msg", isStateChange: true, type: "physical")
    }
  }
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelSet cmd) {
  log.warn "SwitchMultilevelSet Called. This doesn't do anything right now in this driver."
}

//Catch all for unhandled Z-Wave commands
def zwaveEvent(hubitat.zwave.Command cmd) {
  log.warn "${device.displayName} received unhandled command: ${cmd}"
}


//Commands
def on() {
  logDebug "on()"
  state.flashing = false
  def cmds = []
  sendEvent(name: "switch", value: "on", isStateChange: true, descriptionText: "${device.label} is on", type: "digital")
  cmds << zwave.basicV1.basicSet(value: 0xFF).format()
  cmds << zwave.switchMultilevelV2.switchMultilevelGet().format()
  delayBetween(cmds, 5000)
}

def off() {
  logDebug "off()"
  state.flashing = false
  def cmds = []
  sendEvent(name: "switch", value: "off", isStateChange: true, descriptionText: "${device.label} is off", type: "digital")
  cmds << zwave.basicV1.basicSet(value: 0x00).format()
  cmds << zwave.switchMultilevelV2.switchMultilevelGet().format()
  delayBetween(cmds, 5000)
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
  def delay = 0
  state.level = level

  if (level > 0 && currval == "off") {
    sendEvent(name: "switch", value: "on", descriptionText: "${device.label} is on")
  }
  else if (level == 0 && currval == "on") {
    sendEvent(name: "switch", value: "off", descriptionText: "${device.label} is off")
    delay += 2000
  }
  sendEvent(name: "level", value: level, unit: "%", descriptionText: "Level for ${device.label} is ${level}")
  def zsteps = settings.paramZSteps ?: 1
  def zdelay = settings.paramZDuration ?: 3
  delay = delay + (zsteps * zdelay * 10 + 1000).toInteger()
  logDebug "setLevel >> value: $level, delay: $delay"
  delayBetween([
    zwave.basicV1.basicSet(value: level).format(),
    zwave.switchMultilevelV1.switchMultilevelGet().format()
  ], delay)
}

def setLevel(value, duration) {
  logDebug "setLevel($value, $duration)"
  state.flashing = false
  def currval = device.currentValue("switch")
  def getStatusDelay = (duration * 1000 + 1000).toInteger()
  value = Math.max(Math.min(value.toInteger(), 99), 0)
  state.level = value
  if (value > 0 && currval == "off") {
    sendEvent(name: "switch", value: "on", descriptionText: "${device.label} is on")
  }
  else if (value == 0 && currval == "on") {
    sendEvent(name: "switch", value: "off", descriptionText: "${device.label} is off")
    getStatusDelay += 2000
  }
  sendEvent(name: "level", value: value, unit: "%", descriptionText: "Level for ${device.label} is ${level}")
  logDebug "setLevel(value, duration) >> value: $value, duration: $duration, delay: $getStatusDelay"
  delayBetween([
    zwave.switchMultilevelV2.switchMultilevelSet(value: value, dimmingDuration: duration).format(),
    zwave.switchMultilevelV1.switchMultilevelGet().format()
  ], getStatusDelay)
}

def setDefaultDimmerLevel(value) {
  logDebug "setDefaultDimmerLevel($value)"
  def cmds = []
  cmds << zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: 17, size: 1).format()
  cmds << zwave.configurationV1.configurationGet(parameterNumber: 17).format()
  delayBetween(cmds, 500)
}

def refresh() {
  logDebug "refresh()"

  def cmds = []
  //cmds << zwave.switchBinaryV1.switchBinaryGet().format()
  cmds << zwave.switchMultilevelV1.switchMultilevelGet().format()
  cmds << zwave.notificationV3.notificationGet(notificationType: 7, v1AlarmType: 0).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 1).format()
  //cmds << zwave.configurationV2.configurationGet(parameterNumber: 2).format() // change brightness of associated light bulb(s) is not implemented
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 3).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 5).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 6).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 7).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 8).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 9).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 10).format()
  //cmds << zwave.configurationV2.configurationGet(parameterNumber: 11).format() // adjust Steps/levels with “ALL ON/OFF” command is not implemented
  //cmds << zwave.configurationV2.configurationGet(parameterNumber: 12).format() // adjust timing of steps/levels with “ALL ON/OFF” command is not implemented
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 13).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 14).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 15).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 16).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 17).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 18).format()
  if (getDataValue("MSR") == null) {
    cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
  }
  delayBetween(cmds, 500)
}

def installed() {
  logDebug "installed()"
  configure()
}

def updated() {
  logDebug "updated()"
  log.warn "Trace logging is: ${traceLogEnable == true}"
  log.warn "Debug logging is: ${logEnable == true}"
  log.warn "descriptionText logging is: ${descriptionTextEnable == true}"
  if (logEnable || traceLogEnable) { runIn(1800, logsOff) }

  if (state.lastUpdated && now() <= state.lastUpdated + 3000) {
    logTrace "Updated too recently so skipping"
    return
  }
  state.lastUpdated = now()

  def cmds = []
  cmds << zwave.associationV1.associationSet(groupingIdentifier: 1, nodeId: zwaveHubNodeId).format()
  cmds << zwave.associationV1.associationRemove(groupingIdentifier: 2, nodeId: zwaveHubNodeId).format()
  cmds << zwave.associationV1.associationRemove(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()

  // Set Light Timer param
  if (paramLightTimer == null) {
    paramLightTimer = 5
  }
  cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: paramLightTimer.toInteger(), parameterNumber: 1, size: 1).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 1).format()

  // Set Operation Mode param
  if (paramOperationMode == null) {
    paramOperationMode = 3
  }
  cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: paramOperationMode.toInteger(), parameterNumber: 3, size: 1).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 3).format()

  // Set Inverted param
  if (paramInverted == null) {
    paramInverted = 0
  }
  cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: paramInverted.toInteger(), parameterNumber: 5, size: 1).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 5).format()

  // Set Motion Enabled param
  if (paramMotionEnabled == null) {
    paramMotionEnabled = 1
  }
  cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: paramMotionEnabled.toInteger(), parameterNumber: 6, size: 1).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 6).format()

  // Set Z Steps
  if (paramZSteps == null) {
    paramZSteps = 1
  }
  cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: paramZSteps.toInteger(), parameterNumber: 7, size: 1).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 7).format()

  // Set Z Duration
  if (paramZDuration == null) {
    paramZDuration = 3
  }
  cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: paramZDuration.toInteger(), parameterNumber: 8, size: 2).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 8).format()

  // Set P Steps
  if (paramPSteps == null) {
    paramPSteps = 1
  }
  cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: paramPSteps.toInteger(), parameterNumber: 9, size: 1).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 9).format()

  // Set P Duration
  if (paramPDuration == null) {
    paramPDuration = 3
  }
  cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: paramPDuration.toInteger(), parameterNumber: 10, size: 2).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 10).format()

  // Set Motion Sensitivity param
  if (paramMotionSensitivity == null) {
    paramMotionSensitivity = 2
  }
  cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: paramMotionSensitivity.toInteger(), parameterNumber: 13, size: 1).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 13).format()

  // Set Light Sense param
  if (paramLightSense == null) {
    paramLightSense = 1
  }
  cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: paramLightSense.toInteger(), parameterNumber: 14, size: 1).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 14).format()

  // Set Motion Reset Timer param
  if (paramMotionResetTimer == null) {
    paramMotionResetTimer = 2
  }
  cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: paramMotionResetTimer.toInteger(), parameterNumber: 15, size: 2).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 15).format()

  // Set Switch Mode
  if (paramSwitchMode == null) {
    paramSwitchMode = 0
  }
  cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: paramSwitchMode.toInteger(), parameterNumber: 16, size: 1).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 16).format()

  // Set Default Dimmer Level
  if (paramDefaultDimmerLevel == null) {
    paramDefaultDimmerLevel = 0
  }
  cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: paramDefaultDimmerLevel.toInteger(), parameterNumber: 17, size: 1).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 17).format()

  // Set Dim Up Rate
  if (paramDimUpRate == null) {
    paramDimUpRate = 0
  }
  cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: paramDimUpRate.toInteger(), parameterNumber: 18, size: 1).format()
  cmds << zwave.configurationV2.configurationGet(parameterNumber: 18).format()

  delayBetween(cmds, 500)
}

def configure() {
  logDebug "configure()"
  def cmds = []
  cmds << zwave.associationV1.associationSet(groupingIdentifier: 1, nodeId: zwaveHubNodeId).format()
  cmds << zwave.associationV1.associationRemove(groupingIdentifier: 2, nodeId: zwaveHubNodeId).format()
  cmds << zwave.associationV1.associationRemove(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()
  cmds << zwave.versionV1.versionGet().format()
  cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
  delayBetween(cmds, 500)
}

def logsOff() {
  if (logEnable) {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
  }
  if (traceLogEnable) {
    log.warn "trace logging disabled..."
    device.updateSetting("traceLogEnable", [value: "false", type: "bool"])
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