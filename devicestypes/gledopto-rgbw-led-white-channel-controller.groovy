/**
 *  Gledopto RGBW LED White Channel Controller
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
  definition(name: "Gledopto RGBW LED White Channel Controller", namespace: "codahq-hubitat", author: "Ben Rimmasch") {
    capability "Switch Level"
    capability "Actuator"
    capability "Switch"
    capability "Sensor"

    command "on"
    command "off"
    command "setLevel"
    command "toggle"
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
}

def on(onTime = null) {
  log.debug "on()"
	sendEvent([name: "switch", value:"on", isStateChange: true])
  parent.whiteOn(onTime)
}

def off() {
  log.debug "off()"
  sendEvent([name: "switch", value:"off", isStateChange: true])
  parent.whiteOff()
}

// adding duration to enable transition time adjustments
def setLevel(value, duration = 21) {
  log.debug "setLevel: ${value}"

  if (value == 0) {
    sendEvent(name: "switch", value: "off")
  }
  else if (device.currentValue("switch") == "off") {
    sendEvent(name: "switch", value: "on")
  }
  sendEvent(name: "level", value: value)

  parent.setWhiteLevel(value, duration)
}

def toggle() {
	log.debug "toggle()"
	if (device.currentValue("switch") == "off") {
		on()
	}
	else {
		off()
	}
}