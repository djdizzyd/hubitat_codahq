/**
 *  Optomoa UHD51A Device Driver
 *  
 *  Copyright 2019 Ben Rimmasch
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
 *
 *  Change Log:
 *  2019-04-13: Initial
 *
 */

metadata {
  definition(name: "Optoma UHD51A", namespace: "codahq-hubitat", author: "Ben Rimmasch") {
    capability "Sensor"
    capability "Telnet"
    capability "Initialize"
    capability "Switch"
    capability "Refresh"

    command "source0"
    command "source1"
    command "source2"
    command "source3"
    command "source4"

    attribute "lampHours", "number"
    attribute "source", "string"
    attribute "state", "string"
  }

  preferences {
    section("Device Settings:") {
      input "IP", "string", title: "IP Address", description: "IP Address", required: true, displayDuringSetup: true
      input name: "source0", type: "text", title: "Source 1", defaultValue: "HDM1"
      input name: "source1", type: "text", title: "Source 2", defaultValue: "HDM2"
      input name: "source2", type: "text", title: "Source 3", defaultValue: "VGA"
      input name: "source3", type: "text", title: "Source 4", defaultValue: "Component"
      input name: "source4", type: "text", title: "Source 5", defaultValue: "Media"
      input name: "descriptionTextEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false
      input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
      input name: "traceLogEnable", type: "bool", title: "Enable trace logging", defaultValue: false
    }
  }
}

def installed() {
  logInfo('installed()')
  initialize()
}

def updated() {
  logInfo('updated()')
  updateDNI()
  initialize()
}

def initialize() {
  logInfo('initialize()')
  state.refresh = false
  telnetClose()
  //telnetConnect([terminalType: 'VT100', termChars:[13]], IP, 23, null/*username*/, null/*password*/)
  telnetConnect([termChars: [13]], IP, 23, null/*username*/, null/*password*/)
  //telnetConnect([termChars:[13,10]], IP, 23, null/*username*/, null/*password*/)
}

def on() {
  logDebug "on()"
  sendMsg("~0000 1")
}

def off() {
  logDebug "off()"
  sendMsg("~0000 2")
}

def refresh() {
  logDebug "refresh()"
  sendMsg("~00150 1")
}

def source0() {
  setSource(0)
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

def setSource(id) {
  logDebug "setSource(${id})"
  state.refresh = true
  logInfo "Setting source to " + getSourceName(id)
  logTrace "Source ID: " + INPUT_IDS[getSourceName(id)]
  sendMsg("~0012 ${INPUT_IDS[getSourceName(id)]}")
}

def getSourceName(id) {
  if (settings ?."source${id}") {
    return settings."source${id}"
  }
  else {
    logTrace "Settings not set..."
    return ['HDMI1', 'HDMI2', 'VGA', 'Component', 'Media'].get(id)
  }
}

def sendMsg(String msg) {
  logDebug("Sending telnet msg: " + msg)
  return new hubitat.device.HubAction(msg, hubitat.device.Protocol.TELNET)
}

def parse(String msg) {
  /*trim because sometimes there's a bunch of weird whitespace characters?*/
  msg = msg.trim().replace("Optoma_PJ> ", "")
  logDebug("Parse: " + msg + " len: " + msg.length())
  if (msg.length() == 16 && msg.startsWith("Ok")) {
    msg = msg.substring(2, msg.length())
    handleRefresh(msg)
  }
  else if (msg.length() == 5 && msg.startsWith("INFO")) {
    msg = msg.substring(4, 5)
    handleState(msg)
  }
  else if (msg.equals("P")) {
    logTrace "Last command succeeded! Refresh? ${state.refresh}"
    if (state.refresh) {
      state.refresh = false
      //runIn(2, refresh)
      refresh()
    }
  }
  else if (msg.equals("F")) {
    logTrace "Last command failed!"
    //probably should do a reinitialize?
  }
  else {
    log.warn "Unhandled msg: '$msg' len: " + msg.length()
    //print every char so we can see where we failed or see where unprintable characters were
    msg.each { logTrace it }
  }
}

def telnetStatus(String status){
  log.warn "telnetStatus: error: " + status
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
    "1": "Warming/On",
    "2": "Cooling",
    "3": "Out of Range",
    "4": "Lamp Fail!"
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

def handleState(msg) {
  logDebug "handleState($msg)"
  sendEvent(name: "state", value: STATE[msg])
  if (msg == "0") {
    sendEvent(name: "switch", value: "off")
  }
  else if (msg == "1") {
    sendEvent(name: "switch", value: "on")
    //run a refresh just to update the source and display mode just in case
    refresh()
  }
}

def handleRefresh(msg) {
  logDebug "handleRefresh($msg)"

  def switchFlag = msg.substring(0, 1)
  def switchvalue = switchFlag == "1" ? "on" : "off"
  if (device.currentValue("switch") != switchvalue) {
    logTrace "state: $switchFlag"
    sendEvent(name: "switch", value: switchvalue)
  }

  def lampHours = msg.substring(1, 6)  //The spec incorrectly states that this value is 4 characters but it is 5
  if (device.currentValue("lampHours") != lampHours.toInteger()) {
    logTrace "lampHours: $lampHours"
    sendEvent(name: "lampHours", value: lampHours.toInteger())
  }

  def source = msg.substring(6, 8)
  if (device.currentValue("source") != INPUTS[source]) {
    logTrace "source: $source"
    sendEvent(name: "source", value: INPUTS[source])
  }

  def fwVer = msg.substring(8, 12)
  if (state.fwVer != fwVer) {
    logTrace "fwVer: $fwVer"
    state.fwVer = fwVer
  }

  def dispMode = msg.substring(12, 14)
  if (state.displayMode != DISP_MODES[dispMode]) {
    logTrace "dispMode: $dispMode"
    state.displayMode = DISP_MODES[dispMode]
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