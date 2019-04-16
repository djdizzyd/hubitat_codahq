/**
 *  zooZ motion sensor ZSE-02 v0.1
 *
 *  Copyright 2018 Ben Rimmasch
 *  
 *  Original code derived from "V 1.2 of zooZ motion sensor ZSE-02 code 5/14/16" by Robert Vandervoort
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

---------------------------------------------------------------------------------------------------------------------------
   INSTRUCTIONS:
   - First you will need to create a new device type in Hubitat. Log in to the hub and click on "Drivers Code" from the left menu.
   - Click "New Driver" and paste the entire contents of this file.  Click "Save" in the top right.
   - Navigate to the device you paired and choose the newly created driver as the type
   
 */

metadata {
  definition(name: "zooZ motion sensor ZSE-02", namespace: "codahq-hubitat", author: "Ben Rimmasch",
      importUrl: "https://raw.githubusercontent.com/codahq/hubitat_codahq/master/devicestypes/zooz-motion-sensor-zse02.groovy") {
    capability "Motion Sensor"
    capability "Acceleration Sensor"
    capability "Sensor"
    capability "Battery"

    // RAW Description: 0 0 0x0701 0 0 0 a 0x5E 0x85 0x59 0x71 0x80 0x5A 0x73 0x84 0x72 0x86
    attribute "tamper", "enum", ["detected", "clear"]
    fingerprint deviceId: "0x0701", inClusters: "0x5E 0x85 0x59 0x71 0x80 0x5A 0x73 0x84 0x72 0x86"
  }
  simulator {
    status "no motion" : "command: 9881, payload: 00300300"
    status "motion"    : "command: 9881, payload: 003003FF"
    status "clear" : " command: 9881, payload: 0071050000000007030000"
    status "tamper" : "command: 9881, payload: 007105000000FF07030000"

    for (int i in [0, 5, 10, 15, 50, 99, 100]) {
      status "battery ${i}%": new hubitat.zwave.Zwave().securityV1.securityMessageEncapsulation().encapsulate(
        new hubitat.zwave.Zwave().batteryV1.batteryReport(
          batteryLevel: i
        )
      ).incomingMessage()
    }
    status "low battery alert": new hubitat.zwave.Zwave().securityV1.securityMessageEncapsulation().encapsulate(
      new hubitat.zwave.Zwave().batteryV1.batteryReport(
        batteryLevel: 255
      )
    ).incomingMessage()
    status "wake up": "command: 8407, payload:"
  }
  tiles(scale: 2) {
    multiAttributeTile(name: "main", type: "generic", width: 6, height: 4){
      tileAttribute("device.motion", key: "PRIMARY_CONTROL") {
        attributeState "active", label: 'motion', icon: "st.motion.motion.active", backgroundColor: "#53a7c0"
        attributeState "inactive", label: 'no motion', icon: "st.motion.motion.inactive", backgroundColor: "#ffffff"
      }
    }
    standardTile("acceleration", "device.acceleration", width: 2, height: 2) {
      state("active", label: 'DISABLED', backgroundColor: "#ff0000")
      state("inactive", label: 'ENABLED', backgroundColor: "#00ff00")
    }
    valueTile("battery", "device.battery", decoration: "flat", width: 2, height: 2) {
      state "battery", label: '${currentValue}% battery', unit: ""
    }
    main(["main"])
    details(["main", "acceleration", "battery"])
  }
  preferences {
    input name: "descriptionTextEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    input name: "traceLogEnable", type: "bool", title: "Enable trace logging", defaultValue: false
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

def updated() {
  updateDataValue("configured", "false")
}

def parse(String description) {
  logDebug "parse(String description)"
  logTrace "description: $description"
  def result = null
  if (description != "updated") {
    def cmd = zwave.parse(description, [0x31: 5, 0x71: 3, 0x7A: 2, 0x81: 1, 0x84: 2, 0x86: 1])
    if (cmd) {
      result = zwaveEvent(cmd)
    }
  }
  logDebug "Parsed '${description}' to ${result.inspect()}"
  return result
}

def zwaveEvent(hubitat.zwave.commands.wakeupv2.WakeUpNotification cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.wakeupv2.WakeUpNotification cmd)"
  logTrace "cmd: $cmd"
  def result = createEvent(descriptionText: "${device.displayName} woke up", displayed: true)
  def cmds = []
  if (!isConfigured()) {
    // we're still in the process of configuring a newly joined device
    logTrace("late configure")
    [result, response(configure())]
  } else if (isConfigured()) {
    cmds << zwave.batteryV1.batteryGet().format()
    cmds << "delay 3000"
    cmds << zwave.wakeUpV1.wakeUpNoMoreInformation().format()
    [result, response(cmds)]
  }
}

def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd)"
  logTrace "cmd: deviceIdDataFormat: ${cmd.deviceIdDataFormat}, deviceIdDataLengthIndicator: ${cmd.deviceIdDataLengthIndicator}, deviceIdType: ${cmd.deviceIdType}, payload: ${cmd.payload}"
}

def zwaveEvent(hubitat.zwave.commands.versionv1.VersionCommandClassReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.versionv1.VersionCommandClassReport cmd)"
  logTrace "cmd: class version: ${cmd.commandClassVersion} - payload: ${cmd.payload}"
}

def zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport cmd)"
  logTrace "cmd: firmware version: $fw, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
  def fw = "${cmd.applicationVersion}.${cmd.applicationSubVersion}"
  updateDataValue("fw", fw)
}

def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd)"
  logTrace "cmd: battery level: ${cmd.batteryLevel}"
  def result = []
  def map = [name: "battery", unit: "%"]
  if (cmd.batteryLevel == 0xFF) {
    map.value = 1
    map.descriptionText = "${device.displayName} battery is low"
    map.isStateChange = true
  } else {
    map.value = cmd.batteryLevel
    map.descriptionText = "${device.displayName} battery is at ${cmd.batteryLevel}"
    map.isStateChange = true
  }
  state.lastbatt = now()
  result << createEvent(map)
  result
}

def motionEvent(value) {
  def map = [name: "motion"]
  if (value != 0) {
    map.value = "active"
    map.descriptionText = "$device.displayName detected motion"
  } else {
    map.value = "inactive"
    map.descriptionText = "$device.displayName motion has stopped"
  }
  createEvent(map)
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd)"
  logTrace "cmd: value: ${cmd.value}"
  motionEvent(cmd.value)
}

def zwaveEvent(hubitat.zwave.commands.notificationv3.NotificationReport cmd) {
  logDebug "zwaveEvent(hubitat.zwave.commands.notificationv3.NotificationReport cmd)"
  logTrace "cmd:  eventParametersLength: ${cmd.eventParametersLength} eventParameter: ${cmd.eventParameter} notificationType: ${cmd.notificationType} event: ${cmd.event}"
  def result = []
  if (cmd.notificationType == 7) {
    if (cmd.eventParameter == [8] && cmd.event == 0x00) {
      result << motionEvent(0)
    }
    else if (cmd.event == 0x03) {
      result << createEvent(name: "acceleration", value: "active", descriptionText: "$device.displayName has been deactivated by the switch.")
    }
    else if (cmd.eventParameter == [3] && cmd.event == 0X00) {
      result << createEvent(name: "acceleration", value: "inactive", descriptionText: "$device.displayName has been reactivated by the switch.")
    }
    else if (cmd.event == 0x08) {
      result << motionEvent(255)
    }
  }
  else {
    result << createEvent(descriptionText: cmd.toString(), isStateChange: false)
  }
  result
}

def zwaveEvent(hubitat.zwave.Command cmd) {
  logDebug "zwaveEvent(hubitat.zwave.Command cmd)"
  logTrace "cmd: $cmd"
  createEvent(descriptionText: cmd.toString(), isStateChange: false)
}

def configure() {
  logDebug "configure()"
  logTrace "settings: ${settings.inspect()}, state: ${state.inspect()}"
  setConfigured()
  def request = [
    // set wakeup interval to 20 mins
    zwave.wakeUpV1.wakeUpIntervalSet(seconds: 1200, nodeid: zwaveHubNodeId),

    // Can use the zwaveHubNodeId variable to add the hub to the device's associations:
    zwave.associationV1.associationSet(groupingIdentifier: 2, nodeId: zwaveHubNodeId),

    //Get association
    zwave.associationV1.associationGet(groupingIdentifier: 1),
    zwave.associationV1.associationGet(groupingIdentifier: 2),

    // Get Version information
    zwave.versionV1.versionGet(),

    // get updated battery data
    zwave.batteryV1.batteryGet(),
  ]
  commands(request) + ["delay 20000", zwave.wakeUpV1.wakeUpNoMoreInformation().format()]
}

private setConfigured() {
  updateDataValue("configured", "true")
  return []
}

private isConfigured() {
  getDataValue("configured") == "true"
}

private command(hubitat.zwave.Command cmd) {
  cmd.format()
}

private commands(commands, delay = 1000) {
  delayBetween(commands.collect{ command(it) }, delay)
}