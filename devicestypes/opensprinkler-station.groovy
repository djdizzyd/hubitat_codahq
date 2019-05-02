/**
 *  OpenSprinkler Station Driver
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

    command "open", [[name: "Delay Open", type: "NUMBER", description: "Enter a value in seconds to delay before opening"], [name: "Duration", type: "NUMBER", description: "Enter a value in seconds to run before closing"]]
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
  sendEvent(name: "switch", value: "on")
  sendEvent(name: "valve", value: "closed")
}

def refresh() {
  parent.refresh()
}

def open(BigDecimal delay = null, BigDecimal duration = null) {
  logDebug "open(${delay},${duration})"
  unschedule()
  if (delay != null) {
    logInfo "Opening ${device.label} in ${delay} seconds"
    runIn(delay.toInteger(), callbackOpen, [overwrite: false, data: [duration: duration]])
    return
  }
  else {
    logTrace "duration ${duration}"
    sendEvent([name: "valve", value: "open", isStateChange: true])
    def runTime = duration == null ? defaultRuntime.toInteger() : duration.toInteger()
    logInfo "Opening station ${device.label} for ${runTime} seconds"
    runIn(runTime + 5, refresh, [overwrite: false])

    parent.api(parent.getSTATION_RUN(), [sid: stationIndex, duration: runTime])
  }
}

def callbackOpen(data) {
  logDebug "callbackOpen($data)"
  open(null, data.duration)
}



def close() {
  logDebug "close()"
  unschedule()
  logInfo "Closing station ${device.label}"
  sendEvent([name: "valve", value: "closed", isStateChange: true])
  parent.api(parent.getSTATION_OFF(), [sid: stationIndex])
}

def duration() {
  logTrace "duration() ${runOnceRuntime}"
  return runOnceRuntime as Integer
}

def off() {
  //logDebug "off()"
  //unschedule()
  //close()
  //not implemented
}

def on() {
  //logDebug "on()"
  //not implemented
}

private getStationIndex() {
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