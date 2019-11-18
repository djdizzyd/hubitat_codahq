/**
 *  HomeSeer HS-WD200+ Dimmer
 *
 *  Copyright 2019 Eric G. Miller
 *
 *  Modified from Ben Rimmasch HomeSeer HS-WD200+ Dimmer
 *  Modified from the work by DarwinsDen device handler for the WD100 version 1.03 and from the work by HomeSeer for the HS-WD200+
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
 *	Author: Eric G. Miller
 *	Date: 2019-11-16
 *
 *  Changelog:
 *  1.0       2019-06-07 Initial Hubitat Version
 *  1.0.1     2019-06-08 Fixes for some hubs not liking BigDecimals passed as configurationValue
 *  1.0.2     2019-06-09 Small fix so that when setting LED colors on a fan and a dimmer 0 can be used for all as well as 8
 *  1.0.3     2019-09-12 Fixed the delay between level gets in setLevel
 *  1.0.4     2019-11-16 Clone from codahq-hubitat and remove tiles
 *
 *
 *	Previous Driver's Changelog:
 *	1.0.dd.9  13-Feb-2019 Added dummy setLevel command with duration for compatibility with HA Bridge, others? (darwin@darwinsden.com)
 *	1.0.dd.8  28-Jul-2018 Additional protection against floating point default preference values
 *	1.0.dd.6  27-Jul-2018 Added call to set led flash rate and added protection against floating point default preference values
 *	1.0.dd.5  26-Mar-2018 Corrected issues: 1) Turning off all LEDs did not return switch to Normal mode,
 *                        2) Turning off last lit LED would set Normal mode, but leave LED state as on (darwin@darwinsden.com)
 *	1.0.dd.4  28-Feb-2018 Updated all LED option to use LED=0 (8 will be depricated) and increased delay by 50ms (darwin@darwinsden.com)
 *	1.0.dd.3  19-Feb-2018 Corrected bit-wise blink off operator (darwin@darwinsden.com)
 *	1.0.dd.2  16-Feb 2018 Added button number labels to virtual buttons and reduced size (darwin@darwinsden.com)
 *	1.0.dd.1  15-Feb 2018 Added option to set all LED's simultaneously(darwin@darwinsden.com)
 *	1.0	      Jan    2017 Initial Version
 *
 *
 *   Button Mappings:
 *
 *   ACTION          BUTTON#    BUTTON ACTION
 *   Double-Tap Up     1        pushed
 *   Double-Tap Down   2        pushed
 *   Triple-Tap Up     3        pushed
 *   Triple-Tap Down   4        pushed
 *   Hold Up           5        pushed
 *   Hold Down         6        pushed
 *   Single-Tap Up     7        pushed
 *   Single-Tap Down   8        pushed
 *   4 taps up         9        pushed
 *   4 taps down       10       pushed
 *   5 taps up         11       pushed
 *   5 taps down       12       pushed
 *
 */

 private def scenes() {
   return [up: 1, down: 2]
 }

 private def ka() {
   [
     tap1: 0,
     unknown: 1,
     hold: 2,
     tap2: 3,
     tap3: 4,
     tap4: 5,
     tap5: 6
   ]
 }

 private def hold() {
   return -1
 }

private def buttons() {
  return [
    [buttonNum: 1, direction: "Up", numTaps: 2, scene: scenes().up, keyAttribute: ka().tap2],
    [buttonNum: 2, direction: "Down", numTaps: 2, scene: scenes().down, keyAttribute: ka().tap2],
    [buttonNum: 3, direction: "Up", numTaps: 3, scene: scenes().up, keyAttribute: ka().tap3],
    [buttonNum: 4, direction: "Down", numTaps: 3, scene: scenes().down, keyAttribute: ka().tap3],
    [buttonNum: 5, direction: "Up", numTaps: hold(), scene: scenes().up, keyAttribute: ka().hold],
    [buttonNum: 6, direction: "Down", numTaps: hold(), scene: scenes().down, keyAttribute: ka().hold],
    [buttonNum: 7, direction: "Up", numTaps: 1, scene: scenes().up, keyAttribute: ka().tap1],
    [buttonNum: 8, direction: "Down", numTaps: 1, scene: scenes().down, keyAttribute: ka().tap1],
    [buttonNum: 9, direction: "Up", numTaps: 4, scene: scenes().up, keyAttribute: ka().tap4],
    [buttonNum: 10, direction: "Down", numTaps: 4, scene: scenes().down, keyAttribute: ka().tap4],
    [buttonNum: 11, direction: "Up", numTaps: 5, scene: scenes().up, keyAttribute: ka().tap5],
    [buttonNum: 12, direction: "Down", numTaps: 5, scene: scenes().down, keyAttribute: ka().tap5],
  ]
}

metadata {
  definition(name: "HS-WD200+ Dimmer Emiller", namespace: "codahq-hubitat", author: "Eric Miller",
    importUrl: "https://raw.githubusercontent.com/codahq/hubitat_codahq/master/devicestypes/homeseer-hs_wd200plus.groovy") {
    capability "Switch Level"
    capability "Actuator"
    capability "Indicator"
    capability "Switch"
    capability "Polling"
    capability "Refresh"
    capability "Sensor"
    capability "PushableButton"
    capability "Configuration"

    command "tapUp2"
    command "tapDown2"
    command "tapUp3"
    command "tapDown3"
    command "tapUp4"
    command "tapDown4"
    command "tapUp5"
    command "tapDown5"
    command "holdUp"
    command "holdDown"
    command "setStatusLed", [
      [name: "LED*", type: "NUMBER", range: 0..8, description: "1=LED 1 (bottom), 2=LED 2, 3=LED 3, 4=LED 4, 5=LED 5, 6=LED 6, 7=LED 7, 0 or 8=ALL"],
      [name: "Color*", type: "NUMBER", range: 0..7, description: "0=Off, 1=Red, 2=Green, 3=Blue, 4=Magenta, 5=Yellow, 6=Cyan, 7=White"],
      [name: "Blink?*", type: "NUMBER", range: 0..1, description: "0=No, 1=Yes", default: 0]
    ]
    command "setSwitchModeNormal"
    command "setSwitchModeStatus"
    command "setDefaultColor", [[name: "Set Normal Mode LED Color", type: "NUMBER", range: 0..6, description: "0=White, 1=Red, 2=Green, 3=Blue, 4=Magenta, 5=Yellow, 6=Cyan"]]
    command "setBlinkDurationMS", [[name: "Set Blink Duration", type: "NUMBER", description: "Milliseconds (0 to 25500)"]]

    fingerprint mfr: "000C", prod: "4447", model: "3036"
    //to add new fingerprints convert dec manufacturer to hex mfr, dec deviceType to hex prod, and dec deviceId to hex model
  }

  simulator {
    status "on": "command: 2003, payload: FF"
    status "off": "command: 2003, payload: 00"
    status "09%": "command: 2003, payload: 09"
    status "10%": "command: 2003, payload: 0A"
    status "33%": "command: 2003, payload: 21"
    status "66%": "command: 2003, payload: 42"
    status "99%": "command: 2003, payload: 63"

    // reply messages
    reply "2001FF,delay 5000,2602": "command: 2603, payload: FF"
    reply "200100,delay 5000,2602": "command: 2603, payload: 00"
    reply "200119,delay 5000,2602": "command: 2603, payload: 19"
    reply "200132,delay 5000,2602": "command: 2603, payload: 32"
    reply "20014B,delay 5000,2602": "command: 2603, payload: 4B"
    reply "200163,delay 5000,2602": "command: 2603, payload: 63"
  }

  preferences {
    input "singleTapToLevel", "bool", title: "Single-Tap Up sets to level", defaultValue: false, displayDuringSetup: true, required: false
    input("singleTapLevel", "number", title: "Single-Tap Up Level (1-99)", range: "1..99", required: false)
    input("doubleTapUpAction", "enum", title: "Action On Double-Tap Up", options: ["Set to level", "Increase by amount"], description: "Select Action", displayDuringSetup: true, required: false)
    input("doubleTapUpLevel", "number", title: "Double-Tap Up Level or change (1-99)", range: "1..99", required: false)
    input("doubleTapDownAction", "enum", title: "Action On Double-Tap Down", options: ["Set to level", "Decrease by amount"], description: "Select Action", displayDuringSetup: true, required: false)
    input("doubleTapDownLevel", "number", title: "Double-Tap Down Level or change (1-99)", range: "1..99", required: false)
    input "reverseSwitch", "bool", title: "Reverse Switch", defaultValue: false, displayDuringSetup: true, required: false
    input "bottomled", "bool", title: "Bottom LED On if Load is Off", defaultValue: false, displayDuringSetup: true, required: false
    input("localcontrolramprate", "number", title: "Press Configuration button after changing preferences\n\nLocal Ramp Rate: Duration (0-90)(1=1 sec) [default: 3]", defaultValue: 3, range: "0..90", required: false)
    input("remotecontrolramprate", "number", title: "Remote Ramp Rate: duration (0-90)(1=1 sec) [default: 3]", defaultValue: 3, range: "0..90", required: false)
    input("color", "enum", title: "Default LED Color", options: ["White", "Red", "Green", "Blue", "Magenta", "Yellow", "Cyan"], description: "Select Color", required: false)
    input name: "descriptionTextEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    input name: "traceLogEnable", type: "bool", title: "Enable trace logging", defaultValue: false
  }

}

def parse(String description) {
  def result = null
  def cmd = null
  logDebug("parse($description)")
  if (description != "updated") {
    // Use version 1 of 0x20 (Basic), 26 (SwitchMultilevel), and 70 (Configuration)
    cmd = zwave.parse(description, [0x20: 1, 0x26: 1, 0x70: 1])
    logTrace "cmd: $cmd"
    if (cmd) {
      result = zwaveEvent(cmd)
    }
  }
  if (result) {
    logDebug "Parsed ${cmd} to result ${result.inspect()}"
  } else {
    log.warn "Parse returned empty result for command ${cmd}"
  }
  return result
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
  dimmerEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
  dimmerEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd) {
  dimmerEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv1.SwitchMultilevelSet cmd) {
  dimmerEvents(cmd)
}

private dimmerEvents(hubitat.zwave.Command cmd) {
  logDebug "dimmerEvents(hubitat.zwave.Command cmd)"
  logTrace "cmd: $cmd"
  def value = (cmd.value ? "on" : "off")
  def result = [createEvent(name: "switch", value: value)]
  logInfo "Switch for ${device.label} is ${value}"
  state.lastLevel = cmd.value
  if (cmd.value && cmd.value <= 100) {
    result << createEvent(name: "level", value: cmd.value, unit: "%")
    logInfo "Level for ${device.label} is ${cmd.value}"
  }
  return result
}

def zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd)"
  logTrace "cmd: $cmd"
  def value = "when off"
  if (cmd.configurationValue[0] == 1) { value = "when on" }
  if (cmd.configurationValue[0] == 2) { value = "never" }
  logInfo "Indicator is on for fan: ${value}"
  createEvent([name: "indicatorStatus", value: value])
}

def zwaveEvent(hubitat.zwave.commands.hailv1.Hail cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.hailv1.Hail cmd)"
  logTrace "cmd: $cmd"
  logInfo "Switch button was pressed"
  createEvent([name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false])
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
  if (!(cmd.manufacturerName.equals(getDataValue("manufacturer")))) {
    updateDataValue("manufacturer", cmd.manufacturerName)
    cmds << createEvent([descriptionText: "$device.displayName manufacturer: $msr", isStateChange: true, displayed: false])
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
  def ver = cmd.applicationVersion + '.' + cmd.applicationSubVersion
  def cmds = []
  if (!(ver.equals(getDataValue("firmware")))) {
    updateDataValue("firmware", ver)
    cmds << createEvent([descriptionText: "Firmware V" + ver, isStateChange: true, displayed: false])
  }
  cmds
}

def zwaveEvent(hubitat.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.firmwareupdatemdv2.FirmwareMdReport cmd)"
  logTrace "cmd: $cmd"
  logDebug("received Firmware Report")
  logDebug "checksum:       ${cmd.checksum}"
  logDebug "firmwareId:     ${cmd.firmwareId}"
  logDebug "manufacturerId: ${cmd.manufacturerId}"
  [:]
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv1.SwitchMultilevelStopLevelChange cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.switchmultilevelv1.SwitchMultilevelStopLevelChange cmd)"
  logTrace "cmd: $cmd"
  logInfo "Stop level change on device ${device.label}"
  [createEvent(name: "switch", value: "on"), response(zwave.switchMultilevelV1.switchMultilevelGet().format())]
}

def zwaveEvent(hubitat.zwave.Command cmd) {
  // Handles all Z-Wave commands we aren't interested in
  logDebug "zwaveEvent(hubitat.zwave.Command cmd)"
  logTrace "cmd: $cmd"
  [: ]
}

/*
//TODO
indicatorNever()
indicatorWhenOff()
indicatorWhenOn()
*/

private def setLevelDeviceCommands(level, dimmer) {
  def cmds = [
    zwave.basicV1.basicSet(value: level).format(),
    zwave.switchMultilevelV1.switchMultilevelGet().format()
  ]
  if (dimmer) {
    cmds << zwave.switchMultilevelV1.switchMultilevelGet().format()
  }
  delayBetween(cmds, 5000)
}

def on() {
  logDebug "on()"
  buttonResponse("digital", 1, "Up").each { sendEvent(it) }
  setLevelDeviceCommands(0xFF, false)
}

def off() {
  logDebug "off()"
  buttonResponse("digital", 1, "Down").each { sendEvent(it) }
  setLevelDeviceCommands(0x00, false)
}

private def setLevelComputeLevel(value) {
  logDebug "setLevelComputeLevel($value)"
  def valueaux = value as Integer
  def level = valueaux < 0 ? 0 : valueaux > 99 ? 99 : valueaux
  logDebug "computed Level: $level"
  return level
}

private def setLevelEventMaps(level) {
  logDebug "setLevelEventMaps Level: $level"
  def onoff = level > 0 ? "on" : "off"
  return [
    [name: "switch", value: onoff],
    [name: "level", value: level, unit: "%"]
  ]
}

// Switch level interface method
def setLevel(value) {
  def level = setLevelComputeLevel(value)
  setLevelEventMaps(level).each {
    sendEvent(it)
  }
  setLevelDeviceCommands(level, true)
}

private def setLevelNoSend(value) {
  def level = setLevelComputeLevel(value)
  def result = setLevelEventMaps(level).collect { createEvent(it) }
  result << response(delayBetween([
    zwave.basicV1.basicSet(value: level).format()
    ,zwave.switchMultilevelV1.switchMultilevelGet().format()
    ,zwave.switchMultilevelV1.switchMultilevelGet().format()
  ], 5000))

  return result
}

// dummy setLevel command with duration for compatibility with Home Assistant Bridge (others?)
def setLevel(value, duration) {
  logDebug "setLevel(value, duration)"
  setLevel(value)
}

private def changeLevelNoSend(value) {
  logDebug "changeLevelNoSend($value)"
  def lastLevel = 0
  if (state.lastLevel != null) {
    lastLevel = state.lastLevel
  }
  return setLevelNoSend(lastLevel + value)
}

/*
 *  Set dimmer to status mode, then set the color of the individual LED
 *
 *  led = 1-7
 *  color = 0=0ff
 *  1=red
 *  2=green
 *  3=blue
 *  4=magenta
 *  5=yellow
 *  6=cyan
 *  7=white
 */

def setBlinkDurationMS(newBlinkDuration) {
  logDebug "setBlinkDurationMS($newBlinkDuration)"
  def cmds = []
  if (0 < newBlinkDuration && newBlinkDuration < 25500) {
    logDebug "setting blink duration to: ${newBlinkDuration} ms"
    state.blinkDuration = newBlinkDuration.toInteger() / 100
    logDebug "blink duration config (parameter 30) is: ${state.blinkDuration}"
    cmds << zwave.configurationV2.configurationSet(configurationValue: [state.blinkDuration.toInteger()], parameterNumber: 30, size: 1).format()
  } else {
    log.warn "commanded blink duration ${newBlinkDuration} is outside range 0 .. 25500 ms"
  }
  return cmds
}

def setStatusLed(BigDecimal led, BigDecimal color, BigDecimal blink) {
  logDebug "setStatusLed($led, $color, $blink)"
  def cmds = []

  if (state.statusled1 == null) {
    state.statusled1 = 0
    state.statusled2 = 0
    state.statusled3 = 0
    state.statusled4 = 0
    state.statusled5 = 0
    state.statusled6 = 0
    state.statusled7 = 0
    state.blinkval = 0
  }

  /* set led # and color */
  switch (led) {
    case 1:
      state.statusled1 = color
      break
    case 2:
      state.statusled2 = color
      break
    case 3:
      state.statusled3 = color
      break
    case 4:
      state.statusled4 = color
      break
    case 5:
      state.statusled5 = color
      break
    case 6:
      state.statusled6 = color
      break
    case 7:
      state.statusled7 = color
      break
    case 0:
    case 8:
      // Special case - all LED's
      state.statusled1 = color
      state.statusled2 = color
      state.statusled3 = color
      state.statusled4 = color
      state.statusled5 = color
      state.statusled6 = color
      state.statusled7 = color
      break

  }

  if (state.statusled1 == 0 && state.statusled2 == 0 && state.statusled3 == 0 && state.statusled4 == 0 && state.statusled5 == 0 && state.statusled6 == 0 && state.statusled7 == 0) {
    // no LEDS are set, put back to NORMAL mode
    cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 13, size: 1).format()
  }
  else {
    // at least one LED is set, put to status mode
    cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 13, size: 1).format()
  }

  if (led == 8 | led == 0) {
    for (def ledToChange = 1; ledToChange <= 7; ledToChange++) {
      // set color for all LEDs
      cmds << zwave.configurationV2.configurationSet(configurationValue: [color.intValue()], parameterNumber: ledToChange + 20, size: 1).format()
    }
  }
  else {
    // set color for specified LED
    cmds << zwave.configurationV2.configurationSet(configurationValue: [color.intValue()], parameterNumber: led.intValue() + 20, size: 1).format()
  }

  // check if LED should be blinking
  def blinkval = state.blinkval

  if (blink) {
    switch (led) {
      case 1:
        blinkval = blinkval | 0x1
        break
      case 2:
        blinkval = blinkval | 0x2
        break
      case 3:
        blinkval = blinkval | 0x4
        break
      case 4:
        blinkval = blinkval | 0x8
        break
      case 5:
        blinkval = blinkval | 0x10
        break
      case 6:
        blinkval = blinkval | 0x20
        break
      case 7:
        blinkval = blinkval | 0x40
        break
      case 0:
      case 8:
        blinkval = 0x7F
        break
    }
    cmds << zwave.configurationV2.configurationSet(configurationValue: [blinkval], parameterNumber: 31, size: 1).format()
    state.blinkval = blinkval
    // set blink frequency if not already set, 5=500ms
    if (state.blinkDuration == null | state.blinkDuration < 0 | state.blinkDuration > 255) {
      cmds << zwave.configurationV2.configurationSet(configurationValue: [5], parameterNumber: 30, size: 1).format()
    }
  }
  else {

    switch (led) {
      case 1:
        blinkval = blinkval & 0xFE
        break
      case 2:
        blinkval = blinkval & 0xFD
        break
      case 3:
        blinkval = blinkval & 0xFB
        break
      case 4:
        blinkval = blinkval & 0xF7
        break
      case 5:
        blinkval = blinkval & 0xEF
        break
      case 6:
        blinkval = blinkval & 0xDF
        break
      case 7:
        blinkval = blinkval & 0xBF
        break
      case 0:
      case 8:
        blinkval = 0
        break
    }
    cmds << zwave.configurationV2.configurationSet(configurationValue: [blinkval], parameterNumber: 31, size: 1).format()
    state.blinkval = blinkval
  }
  logTrace "cmds: $cmds"
  delayBetween(cmds, 150)
}

/*
 * Set Dimmer to Normal dimming mode (exit status mode)
 *
 */
def setSwitchModeNormal() {
  logDebug "setSwitchModeNormal()"
  def cmds = []
  cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 13, size: 1).format()
  delayBetween(cmds, 500)
}

/*
 * Set Dimmer to Status mode (exit normal mode)
 *
 */
def setSwitchModeStatus() {
  logDebug "setSwitchModeStatus()"
  def cmds = []
  cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 13, size: 1).format()
  delayBetween(cmds, 500)
}

/*
 * Set the color of the LEDS for normal dimming mode, shows the current dim level
 */
def setDefaultColor(color) {
  logDebug "setDefaultColor($color)"
  def cmds = []
  cmds << zwave.configurationV2.configurationSet(configurationValue: [color], parameterNumber: 14, size: 1).format()
  logTrace "cmds: $cmds"
  delayBetween(cmds, 500)
}


def poll() {
  logDebug "poll()"
  zwave.switchMultilevelV1.switchMultilevelGet().format()
}

def refresh() {
  logDebug "refresh()"
  configure()
}

def zwaveEvent(hubitat.zwave.commands.centralscenev1.CentralSceneNotification cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.centralscenev1.CentralSceneNotification cmd)"
  logTrace "cmd: $cmd"
  logDebug("sceneNumber: ${cmd.sceneNumber} keyAttributes: ${cmd.keyAttributes}")
  def result = []

  // Standard response events for every button push
  if (cmd.keyAttributes != ka().unknown) {
    result += buttonResponse("physical", cmd.sceneNumber, cmd.keyAttributes).collect { createEvent(it) }
  }

  /*
   * Emiller 2019-11-17 We don't have a button for keyAttribute 1. Is it ever triggered?
   * There seems to be some overlap with on/off commands
   * Unknown key attribute 1 was coded to set "on". Why? (I kept logic intact but don't understand it.)
   * Following if:
   *    Key attribute 1 (unknown): Set on or off depending on scene.
   *    Scene 1 (up with key attribute 0 (single tap) or 2 (hold): set on.
   *    Missing here but original code sent off event for scene 2, attribute 2 (hold down)
   *        That seems wrong to me.
   */
  if (cmd.keyAttributes == ka().unknown || (cmd.sceneNumber == scenes().up &&
      (cmd.keyAttributes == ka().tap1 || cmd.keyAttributes == ka().hold))) {
    // Key attribute 1 (unknown): Set on or off depending on scene.
    def switchValue = cmd.sceneNumber == scenes().up ? "on" : "off"
    result = createEvent([name: "switch", value: switchValue, type: "physical"])
  }

  // Set special values per device configuration
  if (singleTapToLevel && cmd.sceneNumber == scenes().up && cmd.keyAttributes == ka().tap1) {
    // Single tap up level
    result += setLevelNoSend(singleTapLevel)
  } else if (cmd.sceneNumber == scenes().up && cmd.keyAttributes == ka().tap2) {
    // Double tap up level
    if (doubleTapUpAction.equals("Set to level")) {
      result += setLevelNoSend(doubleTapUpLevel)
    } else if (doubleTapUpAction.equals("Increase by amount")) {
      result += changeLevelNoSend(doubleTapUpLevel)
    }
  } else if (cmd.sceneNumber == scenes().down && cmd.keyAttributes == ka().tap2) {
    // Double tap down level
    if (doubleTapDownAction.equals("Set to level")) {
      result += setLevelNoSend(doubleTapDownLevel)
    } else if (doubleTapDownAction.equals("Decrease by amount")) {
      result += changeLevelNoSend(-doubleTapDownLevel)
    }
  }

  if (!result) {
    // unexpected case
    log.warn("unexpected scene $cmd.sceneNumber and key attribute $cmd.keyAttribute.")
  }

  return result
}

def buttonResponse(String buttonType, int buttonNum, String direction, int numTaps) {
  logDebug "buttonResponse buttonType ${buttonType}, num ${buttonNum}, dir ${direction}, numTaps ${numTaps}"
  def result = []
  def tapArrow = direction.equals("Down") ? "▼" : "▲"
  def numArrows = numTaps == hold() ? 1 : numTaps
  def tapValue = ""
  for (int ii = 0; ii < numArrows; ii++) {
    tapValue += tapArrow
  }
  def action = numTaps == hold() ? "Hold" : "Tap"
  result << [name: "status", value: "${action} ${tapValue}"]
  result << [name: "pushed", value: buttonNum, descriptionText: "$device.displayName ${action}-${direction}-${numTaps} (button ${buttonNum}) pressed", isStateChange: true]
  logDebug "buttonResponse result ${result}"
  return result

}

def buttonResponse(String buttonType, int scene, int keyAttribute) {
  def button = buttons().find { scene == it.scene && keyAttribute == it.keyAttribute }
  if (button) {
    return buttonResponse(buttonType, button.buttonNum, button.direction, button.numTaps)
  }
}

def buttonResponse(String buttonType, int numTaps, String direction) {
  def button = buttons().find { numTaps == it.numTaps && direction.equals(it.direction) }
  if (button) {
    return buttonResponse(buttonType, button.buttonNum, button.direction, button.numTaps)
  }
}

def tapUp1() {
  buttonResponse("digital", 1, "Up").each { sendEvent(it) }
}

def tapDown1() {
  buttonResponse("digital", 1, "Down").each { sendEvent(it) }
}

def tapUp2() {
  buttonResponse("digital", 2, "Up").each { sendEvent(it) }
}

def tapDown2() {
  buttonResponse("digital", 2, "Down").each { sendEvent(it) }
}

def tapUp3() {
  buttonResponse("digital", 3, "Up").each { sendEvent(it) }
}

def tapDown3() {
  buttonResponse("digital", 3, "Down").each { sendEvent(it) }
}

def tapUp4() {
  buttonResponse("digital", 4, "Up").each { sendEvent(it) }
}

def tapDown4() {
  buttonResponse("digital", 4, "Down").each { sendEvent(it) }
}

def tapUp5() {
  buttonResponse("digital", 5, "Up").each { sendEvent(it) }
}

def tapDown5() {
  buttonResponse("digital", 5, "Down").each { sendEvent(it) }
}

def holdUp() {
  buttonResponse("digital", hold(), "Up").each { sendEvent(it) }
}

def holdDown() {
  buttonResponse("digital", hold(), "Down").each { sendEvent(it) }
}

def configure() {
  logDebug("configure()")
  cleanup()
  sendEvent(name: "numberOfButtons", value: 12, displayed: false)
  def cmds = []
  cmds += setPrefs()
  cmds << zwave.switchMultilevelV1.switchMultilevelGet().format()
  cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
  cmds << zwave.versionV1.versionGet().format()
  delayBetween(cmds, 500)
}

def setPrefs() {
  logDebug "setPrefs()"
  def cmds = []

  if (logEnable || traceLogEnable) {
    log.warn "Debug logging is on and will be scheduled to turn off automatically in 30 minutes."
    unschedule()
    runIn(1800, logsOff)
  }

  if (color) {
    switch (color) {
      case "White":
        cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 14, size: 1).format()
        break
      case "Red":
        cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 14, size: 1).format()
        break
      case "Green":
        cmds << zwave.configurationV2.configurationSet(configurationValue: [2], parameterNumber: 14, size: 1).format()
        break
      case "Blue":
        cmds << zwave.configurationV2.configurationSet(configurationValue: [3], parameterNumber: 14, size: 1).format()
        break
      case "Magenta":
        cmds << zwave.configurationV2.configurationSet(configurationValue: [4], parameterNumber: 14, size: 1).format()
        break
      case "Yellow":
        cmds << zwave.configurationV2.configurationSet(configurationValue: [5], parameterNumber: 14, size: 1).format()
        break
      case "Cyan":
        cmds << zwave.configurationV2.configurationSet(configurationValue: [6], parameterNumber: 14, size: 1).format()
        break
    }
  }

  if (localcontrolramprate != null) {
    //logDebug localcontrolramprate
    def localRamprate = Math.max(Math.min(localcontrolramprate.toInteger(), 90), 0)
    cmds << zwave.configurationV2.configurationSet(configurationValue: [localRamprate.toInteger()], parameterNumber: 12, size: 1).format()
  }

  if (remotecontrolramprate != null) {
    //logDebug remotecontrolramprate
    def remoteRamprate = Math.max(Math.min(remotecontrolramprate.toInteger(), 90), 0)
    cmds << zwave.configurationV2.configurationSet(configurationValue: [remoteRamprate.toInteger()], parameterNumber: 11, size: 1).format()
  }

  if (reverseSwitch) {
    cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 4, size: 1).format()
  }
  else {
    cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 4, size: 1).format()
  }

  if (bottomled) {
    cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 3, size: 1).format()
  }
  else {
    cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 3, size: 1).format()
  }

  //Enable the following configuration gets to verify configuration in the logs
  //cmds << zwave.configurationV1.configurationGet(parameterNumber: 7).format()
  //cmds << zwave.configurationV1.configurationGet(parameterNumber: 8).format()
  //cmds << zwave.configurationV1.configurationGet(parameterNumber: 9).format()
  //cmds << zwave.configurationV1.configurationGet(parameterNumber: 10).format()

  logTrace "cmds: $cmds"
  return cmds
}

def logsOff() {
  log.info "Turning off debug logging for device ${device.label}"
  device.updateSetting("logEnable", [value: "false", type: "bool"])
  device.updateSetting("traceLogEnable", [value: "false", type: "bool"])
}

def updated() {
  logDebug "updated()"
  def cmds = []
  cmds += setPrefs()
  delayBetween(cmds, 500)
}

def installed() {
  logDebug "installed()"
  cleanup()
}

def cleanup() {
  unschedule()

  logDebug "cleanup()"
  if (state.lastLevel != null) {
    state.remove("lastLevel")
  }
  if (state.blinkval != null) {
    state.remove("blinkval")
  }
  if (state.bin != null) {
    state.remove("bin")
  }
  if (state.blinkDuration != null) {
    state.remove("blinkDuration")
  }
  for (int i = 1; i <= 7; i++) {
    if (state."statusled${i}" != null) {
      state.remove("statusled" + i)
    }
  }
  for (int i = 1; i <= 7; i++) {
    if (state."${i}" != null) {
      state.remove(String.valueOf(i))
    }
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
