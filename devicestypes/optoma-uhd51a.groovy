/**
 *  Optoma UHD51A Projector Device Driver
 *  
 *  Copyright 2020 Ben Rimmasch
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
 *  The RS232 Spec for UHD51:
 *    https://www.optoma.de/ContentStorage/Documents/286c27f6-93f7-4274-acf9-67b225f99e34.pdf
 *  A different project for comparison:
 *    https://www.optomaeurope.com/uploads/RS232/EP782-RS232-en.pdf
 *
 *  Change Log:
 *  2019-04-13: Initial
 *  2020-01-28: Attempt to get filter hours (failed)
 *              Attempt alternate method to getting lamp hours and source (failed)
 *              Restructured to handled a higher diversity of responses and commands
 *              Changed labels on sources to make more sense for hoomins
 *              Added descriptionText logging where it made sense
 *
 *  Notes: This driver requires that the project be in lower power instead of off state to function all the way. In 
 *         addition, the telnet interface must be enabled.
 *         This driver has been tested with several other Optoma projector models and seems to function correctly.  The RS232
 *         spec is very similiar across Optoma models
 */

metadata {
  definition(name: "Optoma UHD51A", namespace: "codahq-hubitat", author: "Ben Rimmasch",
            importUrl: "https://raw.githubusercontent.com/codahq/hubitat_codahq/master/devicestypes/optoma-uhd51a.groovy") {
    capability "Sensor"
    capability "Telnet"
    capability "Initialize"
    capability "Switch"
    capability "Refresh"

    command "source1"
    command "source2"
    command "source3"
    command "source4"
    command "source5"

    attribute "lampHours", "number"
    attribute "source", "string"
    attribute "state", "string"
  }

  preferences {
    section("Device Settings:") {
      input "IP", "string", title: "IP Address", required: true, displayDuringSetup: true
      input name: "source1", type: "text", title: "Source 1", defaultValue: "HDM1"
      input name: "source2", type: "text", title: "Source 2", defaultValue: "HDM2"
      input name: "source3", type: "text", title: "Source 3", defaultValue: "VGA"
      input name: "source4", type: "text", title: "Source 4", defaultValue: "Component"
      input name: "source5", type: "text", title: "Source 5", defaultValue: "Media"
      input name: "descriptionTextEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false
      input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
      input name: "traceLogEnable", type: "bool", title: "Enable trace logging", defaultValue: false
    }
  }
}

//Leave this alone unless you know what you are doing
private getPROJECTOR_ID() {
  return "00"
}

def installed() {
  logInfo('installed()')
  initialize()
}

def uninstalled() {
  telnetClose()
}

def updated() {
  logInfo('updated()')
  updateDNI()
  initialize()
}

def initialize() {
  logInfo('initialize()')
  telnetClose()
  //telnetConnect([terminalType: 'VT100', termChars:[13]], IP, 23, null/*username*/, null/*password*/)
  telnetConnect([termChars: [13]], IP, 23, null/*username*/, null/*password*/)
  //telnetConnect([termChars:[13,10]], IP, 23, null/*username*/, null/*password*/)
}

def on() {
  logDebug "on()"
  sendMsg("on")
}

def off() {
  logDebug "off()"
  sendMsg("off")
}

def refresh() {
  state.clear()
  logDebug "refresh()"
  sendMsg("refresh")
}

def source1() {
  setSource(1)
}
def source2() {
  setSource(2)
}
def source3() {
  setSource(3)
}
def source4() {
  setSource(4)
}
def source5() {
  setSource(5)
}

def setSource(id) {
  logDebug "setSource(${id})"
  logInfo "Setting source to " + getSourceName(id)
  logTrace "Source ID: " + INPUT_IDS[getSourceName(id)]
  sendMsg("set-source", INPUT_IDS[getSourceName(id)])
}

def getSourceName(id) {
  if (settings ?."source${id}") {
    return settings."source${id}"
  }
  else {
    logTrace "Settings not set..."
    return ['HDMI1', 'HDMI2', 'VGA', 'Component', 'Media'].get(id - 1)
  }
}

def sendMsg(String msg, String param = null) {
  logDebug("Performing: " + msg)
  state.lastCommand = msg
  def telnetString = "~" + PROJECTOR_ID + COMMANDS[msg]
  if (param) {
    telnetString += " " + param
  }
  telnetString += "\r"
  logTrace "telnetString: $telnetString"
  return new hubitat.device.HubAction(telnetString, hubitat.device.Protocol.TELNET)
}

def parse(String msg) {
  /*trim because sometimes there's a bunch of weird whitespace characters?*/
  logTrace "msg: $msg"
  msg = msg.trim().replace("Optoma_PJ> ", "")
  logDebug("Parse: " + msg + " len: " + msg.length())
  switch (msg) {
    case {msg.equals("P")}:
      logTrace "Last command succeeded! ${state.lastCommand}"
      if (state.lastCommand.equals("set-source")) {
        sendMsg("refresh")
      }
      break
    case {msg.equals("F")}:
      logTrace "Last command failed! ${state.lastCommand}"
      //probably should do a reinitialize?
      break
    case {msg.startsWith("Ok")}:
      msg = msg.substring(2, msg.length())
      switch (msg) {
        case {state.lastCommand.equals("refresh")}:
          handleRefresh(msg)
          if (device.currentValue("switch").equals("on")) {
            //doesn't work... just a P is the response
            //sendMsg("get-source")
          }
          break
        case {state.lastCommand.equals("filter-hours")}:
          //this never works otherwise we would call it after we get the source
          //sendMsg("filter-hours")
          log.warn "this is never hit.  the spec is wrong.  it says it would be an Ok response but it's really jsut a P without a parameter"
          break
        default:
          log.warn "Unhandled \"Ok\" response ${msg}!"
      }
      break
    case {msg.startsWith("INFO")}:
      msg = msg.substring(4, msg.length())
      switch (msg) {
        case {msg.length() == 1}:
          handleState(msg)
          break
        default:
          log.warn "Unhandled \"INFO\" response ${msg}!"
      }
      break
    default:
      log.warn "Unhandled message $msg!"
      log.warn "len: " + msg.length()
      msg.each { logTrace it }
  }
}

def telnetStatus(String status) {
  log.error "telnetStatus: status: " + status
  if (status != "receive error: Stream is closed") {
    log.error "Connection was dropped."
    initialize()
  }
}

private getINPUTS() {
  return [
    "00": "Standby",
    "02": "VGA",
    "07": "HDMI1",
    "08": "HDMI2",
    "11": "Component",
    "17": "Media"
  ]
}

private getDISP_MODES() {
  return [
    "00": "Standby",
    "03": "Cinema"
  ]
}

private getSTATE() {
  return [
    "0": "Standby",
    "1": "Warming Up",
    "2": "Cooling Down",
    "3": "Out of Range",
    "4": "Lamp Fail",
    "5": "Thermal Switch Error",
    "6": "Fan Lock",
    "7": "Over Temperature",
    "8": "Lamp Hours Running Out",
    "9": "Cover Open",
    "10": "Lamp Ignite Fail",
    "11": "Format Board Power On Fail",
    "12": "Color Wheel Unexpected Stop",
    "13": "Over Temperature",
    "14": "FAN 1 Lock",
    "15": "FAN 2 Lock",
    "16": "FAN 3 Lock",
    "17": "FAN 4 Lock",
    "18": "FAN 5 Lock",
    "19": "LAN fail then restart",
    "20": "LD lower than 60%",
    "21": "LD NTC (1) Over Temperature",
    "22": "LD NTC (2) Over Temperature",
    "23": "High Ambient Temperature",
    "24": "System Ready"
  ]
}

private getINPUT_IDS() {
  return [
    "HDMI1": "1",
    "HDMI2": "15",
    "VGA": "5",
    "Component": "14",
    "Media": "23"
  ]
}

private getCOMMANDS() {
  return [
    "refresh": "150 1",
    "filter-hours": "321 1",
    "filter-installed": "320",
    "on": "00 1",
    "off": "00 2",
    "set-source": "12",
    "get-source": "121 1",
    "0": "0"
  ]
}

def handleState(msg) {
  logDebug "handleState($msg)"
  sendEvent(name: "state", value: STATE[msg])
  if (msg == "0") {
    logInfo "Switch for ${device.label} is off"
    sendEvent(name: "switch", value: "off")
  }
  else if (msg == "1") {
    sendEvent(name: "switch", value: "on")
    logInfo "Switch for ${device.label} is on"
    //run a refresh just to update the source and display mode just in case
    refresh()
  }
}

def handleRefresh(msg) {
  logDebug "handleRefresh($msg)"

  def switchFlag = msg.substring(0, 1)
  def switchvalue = switchFlag == "1" ? "on" : "off"
  if (device.currentValue("switch") != switchvalue) {
    logInfo "Switch for ${device.label} is ${switchvalue}"
    sendEvent(name: "switch", value: switchvalue)
  }

  //we only handle these values when the projector is on because they seem to be completely incorrect when it is off
  if (switchFlag == "1") {
    def lampHours = msg.substring(1, 6)  //The spec incorrectly states that this value is 4 characters but it is 5
    if (device.currentValue("lampHours") != lampHours.toInteger()) {
      logInfo "Lamp hours for ${device.label} are $lampHours"
      sendEvent(name: "lampHours", value: lampHours.toInteger())
    }

    def source = msg.substring(6, 8)
    if (device.currentValue("source") != INPUTS[source]) {
      logInfo "Source for ${device.label} is $source"
      sendEvent(name: "source", value: INPUTS[source])
    }

    def firmware = msg.substring(8, 12)
    if (getDataValue("firmware") != firmware) {
      logInfo "Firmware for ${device.label} is $firmware"
      updateDataValue("firmware", firmware)
    }

    def dispMode = msg.substring(12, 14)
    if (state.displayMode != DISP_MODES[dispMode]) {
      logInfo "Display mode for ${device.label} is $dispMode"
      state.displayMode = DISP_MODES[dispMode]
    }
  }
}

private updateDNI() {
  def dni = getHexHostAddress()
  if (dni != device.deviceNetworkId) {
    device.deviceNetworkId = dni
  }
}

private String convertIPtoHex(ipAddress) {
  return ipAddress.tokenize('.').collect { String.format('%02x', it.toInteger()) }.join().toUpperCase()
}

private String convertPortToHex(port) {
  String hexport = port.toString().format('%04x', 23)
  logDebug "The converted hex port is ${hexport}"
  return hexport.toUpperCase()
}

private getHexHostAddress() {
  def hosthex = convertIPtoHex(settings.IP)
  def porthex = convertPortToHex(23)
  if (porthex.length() < 4) {
    porthex = "00" + porthex
  }
  logDebug "Hosthex is : $hosthex"
  logDebug "Port in Hex is $porthex"
  return "${hosthex}:${porthex}"
  return "${hosthex}"
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
