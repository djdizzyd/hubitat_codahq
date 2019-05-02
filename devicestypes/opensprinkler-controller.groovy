/**
 *  OpenSprinkler Controller Driver
 *
 *  Copyright 2018 Ben Rimmasch
 *
 *
 *  The repository will probably be found here:
 *  https://github.com/codahq/hubitat_codahq
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

import groovy.json.JsonSlurper
import java.security.MessageDigest

def getSTATUS() {
  return "status"
}

def getENABLED() {
  return "enabled"
}

def getSTATION_RUN() {
  return "station-run"
}

def getSTATION_OFF() {
  return "station-off"
}

def getRUN_ONCE_RUN() {
  return "run-once-run"
}

def getRUN_ONCE_OFF() {
  return "run-once-off"
}

def getALL() {
  return "all"
}

metadata {
  definition(name: "OpenSprinkler Controller", namespace: "codahq-hubitat", author: "Ben Rimmasch") {
    capability "Refresh"
    capability "Sensor"
    capability "Configuration"
    capability "Switch"
    capability "Valve"

    //attribute "operationEnabled", "bool"
  }

  preferences {
    input("password", "text", title: "Device Key", description: "Your OpenSprinker device password")
    input("ipadd", "text", title: "IP address", description: "The IP address of your OpenSprinkler unit", required: true)
    input("port", "text", title: "Port", description: "The port of your OpenSprinkler unit", required: true)
    input name: "descriptionTextEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    input name: "traceLogEnable", type: "bool", title: "Enable trace logging", defaultValue: true
    input name: "pollingInterval", type: "number", range: 5..3600, title: "Polling Interval", description: "Duration in seconds in between polls", defaultValue: 60, required: true
  }
}

def installed() {
  initialize()
}

def uninstalled() {
  getChildDevices().each {
    log.warn "Deleting device ${it.label} with DNI ${it.deviceNetworkId}"
    deleteChildDevice(it.deviceNetworkId)
  }
}

def updated() {
  if (!state.initialized) {
    initialize()
  }
  try {
    if (ipadd != null && port != null) {
      if (device.deviceNetworkId != getHexHostAddress()) {
        device.deviceNetworkId = getHexHostAddress()
        logInfo "Device Network ID set to: ${device.deviceNetworkId}"
      }
    }
    else {
      log.warn "IP and port must be configured in the device's preferences in the IDE."
    }
  }
  catch (Exception e) {
    log.warn "Couldn't set Device Network ID: ${e}"
  }
  if (password != null) {
    state.hash = generateMD5(password)
    device.updateSetting("password", [type: "STRING", value: ""])
  }
  if (state.hash == null) {
    log.warn "A password must be configured in the device's preferences in the IDE."
  }
  configure()
  refresh()
}

def initialize() {
  logDebug "Initialize triggered"

  state.initialized = 1

  logInfo "Receiving local POST on ${device.hub?.getDataValue('localIP')}:${device.hub?.getDataValue('localSrvPortTCP')}"
}

def configure() {
  api(ALL)
}

def refresh() {
  logInfo "Refreshing status from ${device.label}"
  unschedule()
  state.updatedDate = now()
  api(STATUS)
  //api(ENABLED)
  customPolling()
}

def open() {
  unschedule()
  def durations = "["
  state.stations.eachWithIndex {
    station, idx ->
    int dur
    if (!station.disabled) {
      def child = getChildDevice(getChildDeviceId(idx))
      dur = child.duration() == null ? 0 : child.duration()
    }
    else {
      dur = 0
    }
    durations += "${dur},"
  }
  durations = durations.substring(0, durations.length() - 1) + "]"
  logTrace durations

  api(RUN_ONCE_RUN, [durations: durations])
}

def close() {
  unschedule()
  api(RUN_ONCE_OFF)
}

def customPolling() {
  logTrace "customPolling(${pollingInterval}) now:${now()} state.lastUpdated:${state.lastUpdated}"
  if (!isConfigured()) {
    logInfo "Polling canceled. Please configure the device!"
    return
  }
  double timesSinceContact = (now() - state.updatedDate).abs() / 1000  //time since last update in seconds
  logDebug "Polling started.  timesSinceContact: ${timesSinceContact} seconds"
  if (timesSinceContact > pollingInterval) {
    logDebug "Polling interval exceeded"
    refresh()
  }
  runIn(pollingInterval, customPolling)  //time in seconds
}

def api(method, args = []) {
  logDebug "api(${method}, ${args})"
  def methods = [
    "status": [gdipadd: "${ipadd}", gdport: "${port}", gdpath: "/js?pw=${state.hash}", gdtype: "GET"],
    "station-run": [gdipadd: "${ipadd}", gdport: "${port}", gdpath: "/cm?pw=${state.hash}&sid=${args.sid}&en=1&t=${ args.duration != null ? args.duration : 30 }", gdtype: "GET"],
    "station-off": [gdipadd: "${ipadd}", gdport: "${port}", gdpath: "/cm?pw=${state.hash}&sid=${args.sid}&en=0", gdtype: "GET"],
    "run-once-run": [gdipadd: "${ipadd}", gdport: "${port}", gdpath: "/cr?pw=${state.hash}&t=${ args.durations != null ? args.durations : "[0, 0, 0, 0, 0, 0, 0, 0]" }", gdtype: "GET"],
    "run-once-off": [gdipadd: "${ipadd}", gdport: "${port}", gdpath: "/cv?pw=${state.hash}&rsn=1", gdtype: "GET"],
    "all": [gdipadd: "${ipadd}", gdport: "${port}", gdpath: "/ja?pw=${state.hash}", gdtype: "GET"],
    "enabled": [gdipadd: "${ipadd}", gdport: "${port}", gdpath: "/jc?pw=${state.hash}", gdtype: "GET"]
  ]

  if (method == STATION_RUN && device.currentValue("valve") != "open") {
    logInfo "A station valve is open!"
    sendEvent([name: "valve", value: "open", isStateChange: true])
  }

  def request = methods.getAt(method)
  doRequest(request.gdipadd, request.gdport, request.gdpath, request.gdtype)

  //http://10.10.10.250/jp?cr=ec5e317122e24ef7354e94697ef321c0&t=[5,0,5,0,5,0,5,0]

}

private doRequest(gdipadd, gdport, gdpath, gdtype) {
  logDebug "doRequest($gdipadd, $gdport, $gdpath, $gdtype)"
  if (!isConfigured()) {
    logInfo "Request canceled. Please configure the device!"
    return
  }

  def hexHostPort = getHexHostAddress()

  logTrace "Hex Host:Port is : ${hexHostPort}"
  logTrace "DNI is ${device.deviceNetworkId}"
  logTrace "And just for good measture: ${getHostAddress()}"
  logTrace "Path is: ${gdpath}"

  def headers = [: ]
  headers.put("HOST", "${gdipadd}:${gdport}")

  try {
    logTrace "About to create HubAction"
    def hubAction = new hubitat.device.HubAction(
      [
        method: gdtype,
        path: gdpath,
        headers: headers
      ]
      , "${hexHostPort}"
    )
    logTrace "After HubAction: ${hubAction}"
    return sendHubCommand(hubAction)
  }
  catch (Exception e)
  {
    logDebug "Hit exception in doRequest: ${hubAction}"
    logDebug e
  }
}

def parse(description) {
  logDebug "start parse"

  try {
    def msg = parseLanMessage(description)
    logTrace "msg: ${msg}"

    def slurper = new groovy.json.JsonSlurper()
    def json = slurper.parseText(msg.body)

    logTrace json

    if (json.settings) {
      handleSetup(json)
    }
    if (json.sn) {
      handleStationStatus(json)
    }
    if (json.en) {
      handleEnabled(json.en)
    }
    if (json.result || json.refresh) {
      if (json.result != 1) {
        log.warn "Last action was not successful! Result: ${json.result}"
      }
      logInfo "Update needed.  Doing refresh!"
      runIn(3, refresh, [overwrite: false])
    }
  }
  catch (Exception e)
  {
    logDebug "Hit exception in parse"
    logDebug e
  }
}

private handleSetup(json) {
  logDebug "handleSetup() ${json}"
  def stations = handleStationNames(json.stations)
  stations.eachWithIndex {
    station, idx ->
    if (idx < state.nstations && !station.disabled) {
      logDebug "Checking enabled station ${station} at index ${idx}"

      def deviceId = getChildDeviceId(idx)
      if (!getChildDevice(deviceId)) {
        addChildDevice("codahq-hubitat", "OpenSprinkler Station", deviceId, [name: "OpenSprinkler Station", label: "OS Station ${station.name}", isComponent: false])
        logInfo "Added station ${station.name} with device id ${deviceId}"
      }

    }
    else {
      def child = getChildDevice(getChildDeviceId(idx))
      if (child) {
        log.warn "Station ${child.label} is no longer enabled and will not be used."
      }
    }
  }
  handleEnabled(json.settings.en)
}

private handleStationStatus(json) {
  logDebug "handleStationStatus() ${json}"
  if (json.nstations && json.nstations != state.nstations) {
    state.nstations = json.nstations
    logInfo "Number of stations set to ${state.nstations}"
  }
  if (!json.sn) return
  def stations = []
  def valveOpen = false
  state.stations.eachWithIndex {
    station, idx ->
    logTrace "${station} index:${idx} open:${json.sn[idx]}"
    //if (json.sn[idx] == 1) {
    //  logInfo "Station ${station.name} (${idx}) is open"
    //}    

    def child = getChildDevice(getChildDeviceId(idx))
    if (child != null) {
      logTrace "child.state.switch: ${child.currentValue("switch")}"
      def switchState = station.disabled == 1 ? "off" : "on"
      if (child.currentValue("switch") != switchState) {
        child.sendEvent([name: "switch", value: switchState, isStateChange: true])
        logInfo "Station ${station.name} is ${switchState}"
      }

      logTrace "child.state.valve: ${child.currentValue("valve")}"
      def valveState = json.sn[idx] == 1 ? "open" : "closed"
      if (child.currentValue("valve") != valveState) {
        child.sendEvent([name: "valve", value: valveState, isStateChange: true])
        logInfo "Station ${station.name} is ${valveState}"
      }
    }

    stations << [name: station.name, open: json.sn[idx], disabled: station.disabled]
    valveOpen = valveOpen || json.sn[idx] == 1
  }

  def valveState = valveOpen ? "open" : "closed"
  if (device.currentValue("valve") != valveState) {
    logInfo "OpenSprinker is ${valveState}"
    sendEvent([name: "valve", value: valveState, isStateChange: true])
  }

  state.stations = stations
  return stations
}

private handleStationNames(json) {
  logDebug "handleStationNames() ${json}"
  if (!state.nstations) return
  if (!json.stn_dis) return

  def bits = convertIntToBitSet(json.stn_dis[0])
  logDebug bits

  boolean[] disabled = new boolean[state.nstations]

  int index = 0;
  for (int i = bits.length() - 1; i >= 0; i--)
  {
    disabled[i] = bits.charAt(index) == "1"
    index++
  }

  def stations = []
  json.snames.eachWithIndex {
    name, idx ->
    if (idx < state.nstations) {
      logDebug "Found station ${name} at index ${idx}"
      stations << [name: name, disabled: disabled[idx]]
    }
  }
  state.stations = stations
}

private handleEnabled(enabled) {
  logDebug "handleEnabled($enabled)"
  def value = enabled ? "on" : "off"
  sendEvent([name: "switch", value: value, displayed: true, isStateChange: true])
  if (device.currentValue("switch") != value) {
    logInfo "OpenSprinkler Controller is ${value}"
  }
}

/*General Helper Methods*/
private isConfigured() {
  return ipadd && port && state.hash
}


/*To Hex Helper Methods*/
private String convertIPToHex(ipAddress) {
  String hex = ipAddress.tokenize('.').collect { String.format('%02x', it.toInteger()) }.join()
  logTrace "IP address entered is ${ipAddress} and the converted hex code is ${hex}"
  return hex.toUpperCase()
}
private String convertPortToHex(port) {
  String hexport = port.toString().format('%04x', port.toInteger())
  logTrace "Port entered is ${port} and the converted hex port is ${hexport}"
  return hexport.toUpperCase()
}


/*Out of Hex Help Methods*/
//private Integer convertHexToInt(hex) {
//    if (isDebug()) log.debug "Convert hex to int: ${hex}"
//	return Integer.parseInt(hex,16)
//}
//private String convertHexToIP(hex) {
//	if (isDebug()) log.debug("Convert hex to ip: $hex") //	a0 00 01 6
//	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
//}
//private getHostAddress() {
//	def parts = device.deviceNetworkId.split(":")
//    if (isDebug()) log.debug "Device Network ID: $device.deviceNetworkId"
//	def ip = convertHexToIP(parts[0])
//	def port = convertHexToInt(parts[1])
//	return ip + ":" + port
//}
private getHostAddress() {
  return "${ipadd}:${port}"
}

private getHexHostAddress() {
  def hosthex = convertIPToHex(ipadd)
  def porthex = convertPortToHex(port)
  if (porthex.length() < 4) {
    porthex = "00" + porthex
  }
  logTrace "Hosthex is : $hosthex"
  logTrace "Port in Hex is $porthex"
  return "${hosthex}:${porthex}"
}

def generateMD5(String s){
  MessageDigest.getInstance("MD5").digest(s.bytes).encodeHex().toString()
}

private convertIntToBitSet(int bits) {
  logTrace bits
  Integer.toBinaryString(bits)
}

private getChildDeviceId(index) {
  return "${device.deviceNetworkId}-${index}".toString()
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
