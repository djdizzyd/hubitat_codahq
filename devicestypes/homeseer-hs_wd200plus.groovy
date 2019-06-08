/**
 *  HomeSeer HS-WD200+ Dimmer
 *
 *  Copyright 2019 Ben Rimmasch
 *
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
 *	Author: Ben Rimmasch
 *	Date: 2019-06-07
 *
 *  Changelog:
 *  1.0       2019-06-07 Initial Hubitat Version
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

metadata {
  definition(name: "HS-WD200+ Dimmer", namespace: "codahq-hubitat", author: "Ben Rimmasch",
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
      [name: "LED*", type: "NUMBER", range: 1..8, description: "1=LED 1 (bottom), 2=LED 2, 3=LED 3, 4=LED 4, 5=LED 5, 6=LED 6, 7=LED 7, 8=ALL"],
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
    input "doubleTapToFullBright", "bool", title: "Double-Tap Up sets to full brightness", defaultValue: false, displayDuringSetup: true, required: false
    input "singleTapToFullBright", "bool", title: "Single-Tap Up sets to full brightness", defaultValue: false, displayDuringSetup: true, required: false
    input "doubleTapDownToDim", "bool", title: "Double-Tap Down sets to 25% level", defaultValue: false, displayDuringSetup: true, required: false
    input "reverseSwitch", "bool", title: "Reverse Switch", defaultValue: false, displayDuringSetup: true, required: false
    input "bottomled", "bool", title: "Bottom LED On if Load is Off", defaultValue: false, displayDuringSetup: true, required: false
    input("localcontrolramprate", "number", title: "Press Configuration button after changing preferences\n\nLocal Ramp Rate: Duration (0-90)(1=1 sec) [default: 3]", defaultValue: 3, range: "0..90", required: false)
    input("remotecontrolramprate", "number", title: "Remote Ramp Rate: duration (0-90)(1=1 sec) [default: 3]", defaultValue: 3, range: "0..90", required: false)
    input("color", "enum", title: "Default LED Color", options: ["White", "Red", "Green", "Blue", "Magenta", "Yellow", "Cyan"], description: "Select Color", required: false)
    input name: "descriptionTextEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    input name: "traceLogEnable", type: "bool", title: "Enable trace logging", defaultValue: false
  }

  tiles(scale: 2) {
    multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
      tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "on", label: '${name}', action: "switch.off", icon: "st.Home.home30", backgroundColor: "#79b821", nextState: "turningOff"
        attributeState "off", label: '${name}', action: "switch.on", icon: "st.Home.home30", backgroundColor: "#ffffff", nextState: "turningOn"
        attributeState "turningOn", label: '${name}', action: "switch.off", icon: "st.Home.home30", backgroundColor: "#79b821", nextState: "turningOff"
        attributeState "turningOff", label: '${name}', action: "switch.on", icon: "st.Home.home30", backgroundColor: "#ffffff", nextState: "turningOn"
      }
      tileAttribute("device.level", key: "SLIDER_CONTROL") {
        attributeState "level", action: "switch level.setLevel"
      }
      tileAttribute("device.status", key: "SECONDARY_CONTROL") {
        attributeState("default", label: '${currentValue}', unit: "")
      }
    }

    standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
      state "default", label: '', action: "refresh.refresh", icon: "st.secondary.configure"
    }

    valueTile("firmwareVersion", "device.firmwareVersion", width: 2, height: 2, decoration: "flat", inactiveLabel: false) {
      state "default", label: '${currentValue}'
    }

    valueTile("level", "device.level", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
      state "level", label: '${currentValue} %', unit: "%", backgroundColor: "#ffffff"
    }

    valueTile("tapUp2", "device.button", width: 1, height: 1, decoration: "flat") {
      state "default", label: "Button 1\nTap\n▲▲", backgroundColor: "#ffffff", action: "tapUp2"
    }

    valueTile("tapDown2", "device.button", width: 1, height: 1, decoration: "flat") {
      state "default", label: "Button 2\nTap\n▼▼", backgroundColor: "#ffffff", action: "tapDown2"
    }

    valueTile("tapUp3", "device.button", width: 1, height: 1, decoration: "flat") {
      state "default", label: "Button 3\nTap\n▲▲▲", backgroundColor: "#ffffff", action: "tapUp3"
    }

    valueTile("tapDown3", "device.button", width: 1, height: 1, decoration: "flat") {
      state "default", label: "Button 4\nTap\n▼▼▼", backgroundColor: "#ffffff", action: "tapDown3"
    }
    valueTile("tapUp1", "device.button", width: 1, height: 1, decoration: "flat") {
      state "default", label: "Button 7\nTap\n▲", backgroundColor: "#ffffff", action: "tapUp1"
    }

    valueTile("tapDown1", "device.button", width: 1, height: 1, decoration: "flat") {
      state "default", label: "Button 8\nTap\n▼", backgroundColor: "#ffffff", action: "tapDown1"
    }

    valueTile("tapUp4", "device.button", width: 1, height: 1, decoration: "flat") {
      state "default", label: "Button 9\nTap\n▲▲▲▲", backgroundColor: "#ffffff", action: "tapUp4"
    }

    valueTile("tapDown4", "device.button", width: 1, height: 1, decoration: "flat") {
      state "default", label: "Button 10\nTap\n▼▼▼▼", backgroundColor: "#ffffff", action: "tapDown4"
    }

    valueTile("tapUp5", "device.button", width: 1, height: 1, decoration: "flat") {
      state "default", label: "Button 11\nTap\n▲▲▲▲▲", backgroundColor: "#ffffff", action: "tapUp5"
    }

    valueTile("tapDown5", "device.button", width: 1, height: 1, decoration: "flat") {
      state "default", label: "Button 12\nTap\n▼▼▼▼▼", backgroundColor: "#ffffff", action: "tapDown5"
    }

    valueTile("holdUp", "device.button", width: 1, height: 1, decoration: "flat") {
      state "default", label: "Button 5\nHold\n▲", backgroundColor: "#ffffff", action: "holdUp"
    }

    valueTile("holdDown", "device.button", width: 1, height: 1, decoration: "flat") {
      state "default", label: "Button 6\nHold\n▼", backgroundColor: "#ffffff", action: "holdDown"
    }

    main(["switch"])

    details(["switch", "tapUp2", "tapDown2", "tapUp3", "tapDown3", "holdUp", "holdDown", "tapUp1", "tapDown1", "tapUp4", "tapDown4", "tapUp5", "tapDown5", "level", "firmwareVersion", "refresh"])
  }
}

def parse(String description) {
  def result = null
  logDebug("parse($description)")
  if (description != "updated") {
    def cmd = zwave.parse(description, [0x20: 1, 0x26: 1, 0x70: 1])
    logTrace "cmd: $cmd"
    if (cmd) {
      result = zwaveEvent(cmd)
    }
  }
  if (!result) {
    log.warn "Parse returned ${result} for command ${cmd}"
  }
  else {
    logDebug "Parse returned ${result}"
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

def on() {
  logDebug "on()"
  sendEvent(tapUp1Response("digital"))
  delayBetween([
    zwave.basicV1.basicSet(value: 0xFF).format(),
    zwave.switchMultilevelV1.switchMultilevelGet().format()
  ], 5000)
}

def off() {
  logDebug "off()"
  sendEvent(tapDown1Response("digital"))
  delayBetween([
    zwave.basicV1.basicSet(value: 0x00).format(),
    zwave.switchMultilevelV1.switchMultilevelGet().format()
  ], 5000)
}

def setLevel(value) {
  logDebug "setLevel($value)"
  def valueaux = value as Integer
  def level = Math.max(Math.min(valueaux, 99), 0)
  if (level > 0) {
    sendEvent(name: "switch", value: "on")
  } else {
    sendEvent(name: "switch", value: "off")
  }
  sendEvent(name: "level", value: level, unit: "%")
  def result = []

  result += response(zwave.basicV1.basicSet(value: level))
  result += response("delay 5000")
  result += response(zwave.switchMultilevelV1.switchMultilevelGet())
  result += response("delay 5000")
  result += response(zwave.switchMultilevelV1.switchMultilevelGet())
}

// dummy setLevel command with duration for compatibility with Home Assistant Bridge (others?)
def setLevel(value, duration) {
  logDebug "setLevel(value, duration)"
  setLevel(value)
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

  switch (cmd.sceneNumber) {
    case 1:
      // Up
      switch (cmd.keyAttributes) {
        case 0:
          // Press Once
          result += createEvent(tapUp1Response("physical"))
          result += createEvent([name: "switch", value: "on", type: "physical"])

          if (singleTapToFullBright) {
            result += setLevel(99)
            result += response("delay 5000")
            result += response(zwave.switchMultilevelV1.switchMultilevelGet())
          }
          break
        case 1:
          result = createEvent([name: "switch", value: "on", type: "physical"])
          break
        case 2:
          // Hold
          result += createEvent(holdUpResponse("physical"))
          result += createEvent([name: "switch", value: "on", type: "physical"])
          break
        case 3:
          // 2 Times
          result += createEvent(tapUp2Response("physical"))
          if (doubleTapToFullBright) {
            result += setLevel(99)
            result += response("delay 5000")
            result += response(zwave.switchMultilevelV1.switchMultilevelGet())
          }
          break
        case 4:
          // 3 times
          result = createEvent(tapUp3Response("physical"))
          break
        case 5:
          // 4 times
          result = createEvent(tapUp4Response("physical"))
          break
        case 6:
          // 5 times
          result = createEvent(tapUp5Response("physical"))
          break
        default:
          logDebug("unexpected up press keyAttribute: $cmd.keyAttributes")
      }
      break

    case 2:
      // Down
      switch (cmd.keyAttributes) {
        case 0:
          // Press Once
          result += createEvent(tapDown1Response("physical"))
          result += createEvent([name: "switch", value: "off", type: "physical"])
          break
        case 1:
          result = createEvent([name: "switch", value: "off", type: "physical"])
          break
        case 2:
          // Hold
          result += createEvent(holdDownResponse("physical"))
          result += createEvent([name: "switch", value: "off", type: "physical"])
          break
        case 3:
          // 2 Times
          result += createEvent(tapDown2Response("physical"))
          if (doubleTapDownToDim) {
            result += setLevel(25)
            result += response("delay 5000")
            result += response(zwave.switchMultilevelV1.switchMultilevelGet())
          }
          break
        case 4:
          // 3 Times
          result = createEvent(tapDown3Response("physical"))
          break
        case 5:
          // 4 Times
          result = createEvent(tapDown4Response("physical"))
          break
        case 6:
          // 5 Times
          result = createEvent(tapDown5Response("physical"))
          break
        default:
          logDebug("unexpected down press keyAttribute: $cmd.keyAttributes")
      }
      break

    default:
      // unexpected case
      log.warn("unexpected scene: $cmd.sceneNumber")
  }
  return result
}

def tapUp1Response(String buttonType) {
  sendEvent(name: "status", value: "Tap ▲")
  [name: "pushed", value: 7, descriptionText: "$device.displayName Tap-Up-1 (button 7) pressed", isStateChange: true]
}

def tapDown1Response(String buttonType) {
  sendEvent(name: "status", value: "Tap ▼")
  [name: "pushed", value: 8, descriptionText: "$device.displayName Tap-Down-1 (button 8) pressed", isStateChange: true]
}

def tapUp2Response(String buttonType) {
  sendEvent(name: "status", value: "Tap ▲▲")
  [name: "pushed", value: 1, descriptionText: "$device.displayName Tap-Up-2 (button 1) pressed", isStateChange: true]
}

def tapDown2Response(String buttonType) {
  sendEvent(name: "status", value: "Tap ▼▼")
  [name: "pushed", value: 2, descriptionText: "$device.displayName Tap-Down-2 (button 2) pressed", isStateChange: true]
}

def tapUp3Response(String buttonType) {
  sendEvent(name: "status", value: "Tap ▲▲▲")
  [name: "pushed", value: 3, descriptionText: "$device.displayName Tap-Up-3 (button 3) pressed", isStateChange: true]
}

def tapUp4Response(String buttonType) {
  sendEvent(name: "status", value: "Tap ▲▲▲▲")
  [name: "pushed", value: 9, descriptionText: "$device.displayName Tap-Up-4 (button 9) pressed", isStateChange: true]
}

def tapUp5Response(String buttonType) {
  sendEvent(name: "status", value: "Tap ▲▲▲▲▲")
  [name: "pushed", value: 11, descriptionText: "$device.displayName Tap-Up-5 (button 11) pressed", isStateChange: true]
}

def tapDown3Response(String buttonType) {
  sendEvent(name: "status", value: "Tap ▼▼▼")
  [name: "pushed", value: 4, descriptionText: "$device.displayName Tap-Down-3 (button 4) pressed", isStateChange: true]
}

def tapDown4Response(String buttonType) {
  sendEvent(name: "status", value: "Tap ▼▼▼▼")
  [name: "pushed", value: 10, descriptionText: "$device.displayName Tap-Down-3 (button 10) pressed", isStateChange: true]
}

def tapDown5Response(String buttonType) {
  sendEvent(name: "status", value: "Tap ▼▼▼▼▼")
  [name: "pushed", value: 12, descriptionText: "$device.displayName Tap-Down-3 (button 12) pressed", isStateChange: true]
}

def holdUpResponse(String buttonType) {
  sendEvent(name: "status", value: "Hold ▲")
  [name: "pushed", value: 5, descriptionText: "$device.displayName Hold-Up (button 5) pressed", isStateChange: true]
}

def holdDownResponse(String buttonType) {
  sendEvent(name: "status", value: "Hold ▼")
  [name: "pushed", value: 6, descriptionText: "$device.displayName Hold-Down (button 6) pressed", isStateChange: true]
}

def tapUp1() {
  sendEvent(tapUp1Response("digital"))
}

def tapDown1() {
  sendEvent(tapDown1Response("digital"))
}

def tapUp2() {
  sendEvent(tapUp2Response("digital"))
}

def tapDown2() {
  sendEvent(tapDown2Response("digital"))
}

def tapUp3() {
  sendEvent(tapUp3Response("digital"))
}

def tapDown3() {
  sendEvent(tapDown3Response("digital"))
}

def tapUp4() {
  sendEvent(tapUp4Response("digital"))
}

def tapDown4() {
  sendEvent(tapDown4Response("digital"))
}

def tapUp5() {
  sendEvent(tapUp5Response("digital"))
}

def tapDown5() {
  sendEvent(tapDown5Response("digital"))
}

def holdUp() {
  sendEvent(holdUpResponse("digital"))
}

def holdDown() {
  sendEvent(holdDownResponse("digital"))
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
    //log.debug localcontrolramprate
    def localRamprate = Math.max(Math.min(localcontrolramprate.toInteger(), 90), 0)
    cmds << zwave.configurationV2.configurationSet(configurationValue: [localRamprate.toInteger()], parameterNumber: 12, size: 1).format()
  }

  if (remotecontrolramprate != null) {
    //log.debug remotecontrolramprate
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