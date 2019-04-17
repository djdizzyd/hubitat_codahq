/**
 *  OpenSprinkler Station
 *
 *
 *
 *  Copyright 2018 Ben Rimmasch
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
  definition(name: "OpenSprinkler Station", namespace: "codahq-hubitat", author: "Ben Rimmasch") {
		capability "Refresh"
		capability "Valve"
    capability "Actuator"
    capability "Switch"
    capability "Sensor"

    command "on", [[name: "Delay On", type: "NUMBER", description: "Enter a value in seconds to delay before turning on"], [name: "Duration", type: "NUMBER", description: "Enter a value in seconds to run before shutting off"]]
    command "off"
    command "open"
		command "close"
    
  }
	
	preferences {
		input name: "descriptionTextEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false
		input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
		input name: "traceLogEnable", type: "bool", title: "Enable trace logging", defaultValue: false
    input name: "defaultRuntime", type: "number", range: 1..64800, title: "Default Duration", description: "Default duration in seconds this station will run if no duration is specified", defaultValue: 60, required: true
    input name: "runOnceRuntime", type: "number", range: 1..64800, title: "Run-once Duration", description: "Duration in seconds this station will run when the system runs a run-once program", defaultValue: 60, required: true
	}
}

def installed() {
  initialize()
}

def updated() {
  initialize()
}

def initialize() {
  sendEvent(name: "switch", value: "off")
	sendEvent(name: "valve", value: "closed")
}

def refresh() {
	parent.refresh() 
}

def on(BigDecimal onTime = null, BigDecimal duration = null) {
	logDebug "on(${onTime},${duration})"
	unschedule()
	if (onTime != null) {
		logInfo "Turning on ${device.label} in ${onTime} seconds"
		runIn(onTime.toInteger(), on, [overwrite: false, data: [duration: duration]])
		return
	}
  open(duration)
}

def on(data) {
  logDebug "on($data)"
	on(null, data.duration)
}

def off() {
  logDebug "off()"
	unschedule()
	close()
}

def open(duration) {
  logDebug "open(${duration})"
	logInfo "Opening station ${device.label}"
	sendEvent([name: "valve", value:"open", isStateChange: true])
	sendEvent([name: "switch", value:"on", isStateChange: true])
  
  def runTime = duration == null ? defaultRuntime.toInteger() : duration.toInteger()
  logInfo "Turning off ${device.label} in ${runTime} seconds"
  runIn(runTime + 5, refresh, [overwrite: false])
  
  parent.api(parent.getSTATION_RUN(), [sid: stationIndex, duration: runTime])
}

def close() {
	logDebug "close()"
	logInfo "Closing station ${device.label}"
	sendEvent([name: "valve", value:"closed", isStateChange: true])
	sendEvent([name: "switch", value:"off", isStateChange: true])
  parent.api(parent.getSTATION_OFF(), [sid: stationIndex])
}

def duration() {
  logTrace "duration() ${runOnceRuntime}"
	return runOnceRuntime as Integer
}

private getStationIndex() {
  logTrace "getStationIndex()"
  return new String(device.deviceNetworkId).tokenize('-')[1]
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