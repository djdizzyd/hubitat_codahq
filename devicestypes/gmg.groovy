/**
 *  Green Mountain Grill
 *
 *  Copyright 2020 Ben Rimmasch
 *
 *  Thanks to Aenima4six2 and his work at https://github.com/Aenima4six2/gmg for making this driver effort faster
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
 *  Change Log:
 *  2020-02-09: Initial
 *

Notes:
This driver behaves a lot like the GMG Android app.  It does not poll or have any awareness of the grill unless you connect to the
grill with the connect button.  Once you connect, it polls repeatedly for status and does so until you disconnect.  You may choose
the polling interval.  I have set a default of 60 seconds and have found that to be more than adequate.

WARNING!!!
I am very hesitant to allow a user to turn on the grill from this driver or even adjust the temperature.  This could have terrible
consequences.  Because of that this functionality is disabled.  You may enable it on your own AT YOUR OWN RISK.  I am not
responsible if you or somebody with access to your home uses it to start your grill unexpectedly or cause damage to your grill or
property.  You have been warned.  This is a very serious decision that can have terrible consequences.  I recommend that you do 
not enable it unless you have absolute control over all of the end points where this driver could be exposed e.g. IFTTT, 
Google, Alexa, Dashboards, the Hubitat Hub UI, etc.

 *
 */

import hubitat.device.HubAction
import hubitat.device.Protocol
import groovy.transform.Field

@Field int retryCount = 10

def getCommands(temp = "") {
  return [
    powerOn: 'UK001!',
    powerOff: 'UK004!',
    getGrillStatus: 'UR001!',
    getGrillId: 'UL!',
    setGrillTempF: "UT${temp}!",
    setFoodTempF: "UF${temp}!"
  ]
}

metadata {
  definition(name: "Green Mountain Grill", namespace: "codahq-hubitat", author: "Ben Rimmasch",
             importUrl: "https://raw.githubusercontent.com/codahq/hubitat_codahq/master/devicestypes/gmg.groovy") {
    capability "Refresh"
    capability "Initialize"
    capability "Sensor"
    capability "Switch"
    capability "TemperatureMeasurement"

    attribute "status", "string"
    attribute "connected", "boolean"
    attribute "fan", "string"
    attribute "grillTemperature", "number"
    attribute "targetGrillTemperature", "number"
    attribute "foodTemperature", "number"
    attribute "targetFoodTemperature", "number"
    attribute "pelletAlarm", "boolean"

    command "connect"
    command "disconnect"
    command "setTargetFoodTemperature", [
      [name:"Desired Food Temperature", type: "NUMBER", description:"Set the food temperature target of this cook.", constraints:["NUMBER"]],
    ]
    //I don't want to enable either
    //command "setTargetGrillTemperature", [
    //  [name:"Desired Grill Temperature", type: "NUMBER", description:"Set the grill temperature of this cook.", constraints:["NUMBER"]],
    //]
    command "discoverGrill"
  }

  preferences {
    input(type: "paragraph", element: "paragraph", title: "<b>Network</b>", description: "If you know the IP of the device fill it in.  Otherwise, discover grill can find it for you.")
    input("ipadd", "text", title: "<b>IP address</b>", description: "The IP address of your grill", required: true)
    input("port", "text", title: "<b>Port</b>", description: "The port of your grill", required: true, default: "8080")
    input(type: "paragraph", element: "paragraph", title: "<b>Polling</b>", description: "If you would like to use the grill as an outside temperature sensor there are provided two separate polling intervals.  If these intervals differ this device will automatically connect and start polling on initialize.")
    input name: "pollingIntervalC", type: "number", range: 3..7200, title: "<b>Polling interval when connected</b>", description: "Polling interval in seconds when connected", default: 60, required: true
    input name: "pollingIntervalO", type: "number", range: 3..600, title: "<b>Polling interval when on</b>", description: "Polling interval in seconds when on", default: 60, required: true
    input name: "descriptionTextEnable", type: "bool", title: "<b>Enable descriptionText logging</b>", defaultValue: true
    input name: "logEnable", type: "bool", title: "<b>Enable debug logging</b>", defaultValue: true
    input name: "traceLogEnable", type: "bool", title: "<b>Enable trace logging</b>", defaultValue: true
  }
}

def initialize() {
  logInfo "Initializing..."
  if (pollingIntervalC != pollingIntervalO) {
    connect()
  }
  else if (device.currentValue("connected") == "true") {
    disconnect()
  }
}

def updated() {
  if (logEnable) log.warn "Debug logging enabled and scheduled off in 30 minutes."
  if (traceLogEnable) log.warn "Trace logging enabled and scheduled off in 30 minutes."
  unschedule(logsOff)
  if (logEnable || traceLogEnable) {
    runIn(1800, logsOff)
  }
  unschedule(refresh)
  if (state.polling) {
    logInfo "Rescheduling refresh to every ${getPollingInterval()} seconds"
    refresh()
  }
}

def logsOff(){
  log.warn "Debug/Trace logging turning off"
  device.updateSetting("logEnable", [value: "false", type: "bool"])
  device.updateSetting("traceLogEnable", [value: "false", type: "bool"])
}

def on() {
  refresh()
  //I don't want to be able to do this. Too dangerous.
  //runIn(5, setOn)
  log.error "Not implemented!!!"
}

def setOn() {
  if (device.currentValue("fan") == "off" && device.currentValue("switch") == "off") {
    logInfo "Turning on..."
    sendPoll(ipadd, "responseHandler", "powerOn")
  }
  else if (device.currentValue("switch") == "on") {
    log.warn "Already on..."
  }
  else {
    log.error "Turning on is not allowed while in fan mode!"
  }
}

def off() {
  refresh()
  runIn(5, setOff)
}

def setOff() {
  if (device.currentValue("fan") == "off" && device.currentValue("switch") == "on") {
    logInfo "Turning off..."
    sendPoll(ipadd, "responseHandler", "powerOff")
  }
  else if (device.currentValue("switch") == "off") {
    log.warn "Already off..."
  }
  else {
    log.error "Turning off is not allowed while in fan mode!"
  }
}

def setTargetFoodTemperature(temp) {
  logInfo "Setting target food temperature to ${temp}"
  if (device.currentValue("status") == "on") {
    sendPoll(ipadd, "responseHandler", "setFoodTempF", temp.toString())
  }
  else {
    log.error "Setting target food temperature only supported when the grill is on!"
  }
}

def setTargetGrillTemperature(temp) {
  logInfo "Setting grill temperature to ${temp}"
  if (device.currentValue("status") == "on") {
    sendPoll(ipadd, "responseHandler", "setGrillTempF", temp.toString())
  }
  else {
    log.error "Setting grill temperature only supported when the grill is on!"
  }
}

def connect() {
  logInfo "Attempting to connect..."
  if (device.currentValue("connected") == "false") {
    state.polling = true
    state.retry = 0
    refresh()
  }
  else {
    log.warn "Already connected!"
  }
}

def disconnect() {
  logInfo "Disconnecting!"
  unschedule()
  state.polling = false
  sendEvent([name: "connected", value: false, isStateChange: true])
}

def discoverGrill() {
  log.warn "This is going to try and connect to every device on your subnet.  It is going to cause a lot of " +
    "errors in the log because presumably 253 of the devices in the subnet will not reply.  I have not figured " +
    "out how to supress the time-out exception that will be thrown for every failed attempt."
  findDevices(100)
}

def refresh() {
  logDebug "refresh()"
  sendPoll(ipadd, "statusHandler", "getGrillStatus")
}

def findDevices(pollInterval) {
  def hub
  try { hub = location.hubs[0] }
  catch (error) { 
    logWarn "Hub not detected.  You must have a hub to install this app."
    return
  }
  def hubIpArray = hub.localIP.split('\\.')
  def networkPrefix = [
    hubIpArray[0],
    hubIpArray[1],
    hubIpArray[2]
  ].join(".")
  logInfo("findDevices: IP Segment = ${networkPrefix}")
  for (int i = 1; i < 255; i++) {
    def deviceIP = "${networkPrefix}.${i.toString()}"
    try {
      sendPoll(deviceIP, "discoverHandler", "getGrillId")
    }
    catch (e) {
      if (!(e instanceof SocketTimeoutException)) log.error e
    }
    pauseExecution(pollInterval)
  }
  pauseExecution(3000)
}

private sendPoll(ip, action, command, param = null) {
  logDebug "sendPoll($ip, $action)"

  def data = getCommandData(getCommands(param)."${command}")
  logTrace "data: $data"

  def myHubAction = new HubAction(
    data,
    Protocol.LAN,
    [
      type: HubAction.Type.LAN_TYPE_UDPCLIENT,
      destinationAddress: "${ip}:8080",
      //destinationPort: 8080,
      //encoding: hubitat.device.HubAction.Encoding.HEX_STRING,
      timeout: 3,
      callback: action
    ]
  )
  sendHubCommand(myHubAction)
}

def statusHandler(response) {
  logDebug "statusHandler(response)"
  logTrace "response: $response"

  def resp = parseLanMessage(response)
  logTrace "resp: $resp ${resp?.payload?.length()}"

  if (resp?.payload?.length() != 72) {
    unschedule()
    if (state.retry < retryCount) {
      runIn(3, refresh) //not enough data was received so try refresh again in 3 seconds
      logTrace "Partial response! Polling again"
      state.retry++
    }
    else {
      state.polling = false
      log.error "Too many attempts without success!  Not trying anymore!"
      sendEvent([name: "connected", value: false, isStateChange: true])
    }
    return
  }
  else if (state.polling) {
    sendEvent([name: "connected", value: true, isStateChange: true])
    def interval = getPollingInterval()
    logTrace "interval: $interval seconds"
    unschedule(refresh)
    runIn(interval, refresh)
    state.retry = 0
  }

  def status = getGrillState(resp.payload)
  logTrace "status: ${status}"
  if (status != device.currentValue("status")) {
    sendEvent([name: "status", value: status, isStateChange: true])
    logInfo "${device.label} status is $status"
  }
  def switchVal = (status == "on" || status == "fan mode") ? "on" : "off"
  if (switchVal != device.currentValue("switch")) {
    sendEvent([name: "switch", value: switchVal, isStateChange: true])
    logInfo "${device.label} switch is $switchVal"
  }
  def fanVal = status == "fan mode" ? "on" : "off"
  if (fanVal != device.currentValue("fan")) {
    sendEvent([name: "fan", value: fanVal, isStateChange: true])
    logInfo "${device.label} fan is $fanVal"
  }
  def grillTemp = getCurrentGrillTemp(resp.payload)
  logTrace "temp: ${grillTemp}"
  if (grillTemp != device.currentValue("grillTemperature")) {
    sendEvent([name: "grillTemperature", value: grillTemp, isStateChange: true, unit: "°F"])
    sendEvent([name: "temperature", value: grillTemp, isStateChange: true, unit: "°F"])
    logInfo "${device.label} grill temperature is $grillTemp"
  }
  def desiredGrillTemp = status == "on" ? getDesiredGrillTemp(resp.payload) : 0
  logTrace "desiredGrillTemp: ${desiredGrillTemp}"
  if (desiredGrillTemp != device.currentValue("targetGrillTemperature")) {
    sendEvent([name: "targetGrillTemperature", value: desiredGrillTemp, isStateChange: true, unit: "°F"])
    logInfo "${device.label} target grill temperature is $desiredGrillTemp"
  }
  def foodTemp = getCurrentFoodTemp(resp.payload)
  logTrace "foodTemp: ${foodTemp}"
  if (foodTemp != device.currentValue("foodTemperature")) {
    sendEvent([name: "foodTemperature", value: foodTemp, isStateChange: true, unit: "°F"])
    logInfo "${device.label} food temperature is $foodTemp"
  }
  def desiredFoodTemp = status == "on" ? getDesiredFoodTemp(resp.payload) : 0
  logTrace "desiredFoodTemp: ${desiredFoodTemp}"
  if (desiredFoodTemp != device.currentValue("targetFoodTemperature")) {
    sendEvent([name: "targetFoodTemperature", value: desiredFoodTemp, isStateChange: true, unit: "°F"])
    logInfo "${device.label} target food temperature is $desiredFoodTemp"
  }
  def lowPelletAlarmActive = getLowPelletAlarmActive(resp.payload)
  logTrace "pellet alarm: ${lowPelletAlarmActive}"
  if (lowPelletAlarmActive.toString() != device.currentValue("pelletAlarm")) {
    sendEvent([name: "pelletAlarm", value: lowPelletAlarmActive, isStateChange: true])
    log.warn "${device.label} pellart alarm is ${lowPelletAlarmActive ? 'active' : 'inactive'}"
  }
}

def responseHandler(response) {
  logDebug "responseHandler(response)"
  logTrace "response: $response"
  
  def resp = parseLanMessage(response)
  logTrace "resp: $resp ${resp.payload.length()}"
  
  if (resp.payload.length() != 4) {
    unschedule()
    runIn(3, refresh) //not enough data was received so try refresh again in 3 seconds
    logTrace "Partial response! Polling again"
    return
  }
  
  def msg = hexToAscii(resp.payload)
  logTrace "msg: $msg"
  
  if (msg != "OK") {
    log.warn "The previous request failed!"
  }
  else {
    refresh()
  }
}

private getGrillState(hex) {
  def statusCharacter = hex.charAt(61)
  def status = Integer.parseInt(statusCharacter.toString(), 10)
  if (status == 0) status = 'off'
  else if (status == 1) status = 'on'
  else if (status == 2) status = 'fan mode'
  else status = 'unknown'
  return status
}

private getCurrentGrillTemp(hex) {
  def first = getRawValue(hex, 4)
  def second = getRawValue(hex, 6)
  return first + (second * 256)
}

private getDesiredGrillTemp(hex) {
  def first = getRawValue(hex, 12)
  def second = getRawValue(hex, 14)
  return first + (second * 256)
}

private getCurrentFoodTemp(hex) {
  def first = getRawValue(hex, 8)
  def second = getRawValue(hex, 10)
  def currentFoodTemp = first + (second * 256)
  return currentFoodTemp >= 557 ? 0 : currentFoodTemp
}

private getDesiredFoodTemp(hex) {
  def first = getRawValue(hex, 56)
  def second = getRawValue(hex, 58)
  return first + (second * 256)
}

private getLowPelletAlarmActive(hex) {
  def first = getRawValue(hex, 48)
  def second = getRawValue(hex, 50)
  def value = first + (second * 256)
  return value == 128
}

private getRawValue(hex, position) {
  def value = hex.substring(position, position + 2)
  def parsed = Integer.parseInt(value, 16)
  return parsed
}

def discoverHandler(response) {
  logDebug "parseDeviceData(response)"
  logTrace "response: $response"
  def resp = parseLanMessage(response)
  logTrace "resp: $resp"
  
  def msg = hexToAscii(resp.payload)
  logDebug("parseDeviceData: ${convertHexToIP(resp.ip)} // ${msg}")
  
  if (msg.startsWith("GMG")) {
    logInfo "Found a grill at ${convertHexToIP(resp.ip)}.  Please refresh the device edit screen to see the new settings."
    state.serial = msg
    device.updateSetting("ipadd", [value: convertHexToIP(resp.ip), type: "text"])
    device.updateSetting("port", [value: convertHexToInt(resp.port), type: "text"])
  }
}

def getPollingInterval() {
  return device.currentValue("switch") == "on" ? pollingIntervalO : pollingIntervalC
}

private String hexToAscii(String hexStr) {
  StringBuilder output = new StringBuilder("");

  for (int i = 0; i < hexStr.length(); i += 2) {
    String str = hexStr.substring(i, i + 2);
    output.append((char) Integer.parseInt(str, 16));
  }

  return output.toString();
}

def getCommandData(command) {
  return "${command}\n"
  //def fullCommand = "${command}!\n"
  //const data = Buffer.from(fullCommand, 'ascii')
  //return data
  //return fullCommand
}

private Integer convertHexToInt(hex) {
  logDebug "Convert hex to int: ${hex}"
	return Integer.parseInt(hex,16)
}
private String convertHexToIP(hex) {
	logDebug "Convert hex to ip: $hex"
	[convertHexToInt(hex[0..1]), convertHexToInt(hex[2..3]), convertHexToInt(hex[4..5]), convertHexToInt(hex[6..7])].join(".")
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
