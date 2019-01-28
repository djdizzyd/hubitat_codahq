/**
 *  Hubitat Driver: Yamaha Network Receiver
 *
 *  Author: Ben Rimmasch
 *  Derived from redloro@gmail.com's ST work for Yamaha Receivers
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
 *  https://github.com/PSeitz/yamaha-nodejs
 *  http://<RECEIVER_IP_ADDRESS>/YamahaRemoteControl/desc.xml
 */
import groovy.util.XmlSlurper

metadata {
  definition (name: "Yamaha Receiver", namespace: "codahq-hubitat", author: "Ben Rimmasch") {

  }  

  preferences {
		section("Yamaha Receiver") {
			//input name: "receiverName", type: "text", title: "Name", required: true, defaultValue: "Yamaha"
			input name: "receiverIp", type: "text", title: "IP", required: true
			input name: "receiverZones", type: "enum", title: "Zones", required: true, multiple: true, options: ["Main_Zone","Zone_B","Zone_2","Zone_3","Zone_4"]
		}
	}
}

def updated() {
  updateDNI()
	//removeChildDevices()
	addChildDevices()
}

def uninstalled() {
  removeChildDevices()
}

def parse(String description) {
  def map = parseLanMessage(description)  

  def body = getHttpBody(map.body);
  //log.trace "Headers: ${map.headers}"
  //log.trace "Body: ${body}"

  updateZoneDevices(body.children()[0])
}

private updateZoneDevices(evt) {
  //log.debug "updateZoneDevices: ${evt.toString()}"
  if (evt.name() == "System") {
    //log.debug "Update all zones"
    childDevices*.zone(evt)
    return
  }

  def zonedevice = getChildDevice(getDeviceId(evt.name()))
  if (zonedevice) {
    zonedevice.zone(evt)
  }

  //check for Zone_B
  zonedevice = getChildDevice(getDeviceId("Zone_B"))
  if (zonedevice && evt.name() == "Main_Zone") {
    zonedevice.zone(evt)
  }
}

private addChildDevices() {
  // add yamaha zone device(s)
	//settings.receiverZones.properties.each { log.info "derp ${it}" }
	
	//temporary workaround to add Strings to lists
	def selectedZones = []
	if (settings.receiverZones instanceof java.lang.String) {
		selectedZones = [settings.receiverZones]
	}
	else {
		selectedZones = settings.receiverZones
	}
  selectedZones.each {
    def deviceId = getDeviceId(it)
    if (!getChildDevice(deviceId)) {
      //addChildDevice("redloro-smartthings", "Yamaha Zone", deviceId, location.hubs[0].id, ["name": it, label: "Zone ${it}: ${it}", completedSetup: true])
			addChildDevice("redloro-smartthings", "Yamaha Zone", deviceId, [label: "${device.name} - Zone ${it}", isComponent: false])
      log.debug "Added Yamaha zone: ${deviceId}"
    }
  }

  childDevices*.refresh()
}

private removeChildDevices() {
  getChildDevices().each { 
		log.debug "deleting ${it}"
		deleteChildDevice(it.deviceNetworkId)
	}
}

private sendCommand(body) {
  log.debug "Yamaha Network Receiver send command: ${body}"

  def hubAction = new hubitat.device.HubAction(
      headers: [HOST: getReceiverAddress()],
      method: "POST",
      path: "/YamahaRemoteControl/ctrl",
      body: body
  )
  sendHubCommand(hubAction)
}

private getHttpHeaders(headers) {
  def obj = [:]
  new String(headers.decodeBase64()).split("\r\n").each {param ->
    def nameAndValue = param.split(":")
    obj[nameAndValue[0]] = (nameAndValue.length == 1) ? "" : nameAndValue[1].trim()
  }
  return obj
}

private getHttpBody(body) {
  def obj = null;
  if (body) {
    obj = new XmlSlurper().parseText(new String(body))
  }
  return obj
}

private getDeviceId(zone) {
  return "yamaha|${settings.receiverIp}|${zone}".toString()
}

private getReceiverAddress() {
  return settings.receiverIp + ":80"
}

private String convertIPtoHex(ipAddress) {
  return ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join().toUpperCase()
}

private String convertPortToHex(port) {
  return port.toString().format( '%04x', port.toInteger() ).toUpperCase()
}

/*
private setDeviceNetworkId(ip, port = null){
    def myDNI
    if (port == null) {
        myDNI = ip
    } else {
  	    def iphex = convertIPtoHex(ip)
  	    def porthex = convertPortToHex(port)
        
        myDNI = "$iphex:$porthex"
    }
    log.debug "Device Network Id set to ${myDNI}"
    return myDNI
}
*/

private updateDNI() {	
	//if (state.dni != null && state.dni != "" && device.deviceNetworkId != state.dni) {
    //   device.deviceNetworkId = state.dni
    //}
	def dni = convertIPtoHex(settings.receiverIp)
	if (dni != device.deviceNetworkId) {
		device.deviceNetworkId = dni
	}
}

private getHostAddress() {
    if(getDeviceDataByName("ip") && getDeviceDataByName("port")){
        return "${getDeviceDataByName("ip")}:${getDeviceDataByName("port")}"
    }else{
	    return "${ip}:80"
    }
}
