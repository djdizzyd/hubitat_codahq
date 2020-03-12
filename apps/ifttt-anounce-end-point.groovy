/**
 * 	IFTTT Announce End Point Driver
 *
 *   The purpose of this app is to provide an end point for IFTTT's Webhooks to use to pass upcoming meeting or 
 *   appointment data.  This is intended to be used with Office365 calendar or Google Calendar to warn about 
 *   upcoming meetings or something along those lines.  It's time to make your toys work for you.
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
 *  2019-07-22: Initial
 *
 */

definition(
  name: "IFTTT Announce End Point",
  namespace: "codahq-hubitat",
  author: "Ben Rimmasch",
  description: "Provides an end point for IFTTT Webhook calls and speaks them.",
  category: "Nonsense",
  iconUrl: "",
  iconX2Url: ""
)

preferences {
  section("Settings") {
    input "announceDevice", "capability.speechSynthesis", required: true, title: "TTS Device?"
    input name: "constantVolume", type: "bool", title: "Attempt to use constant volume?"
    input name: "constantVolumeValue", type: "number", title: "Constant volume value"
    input name: "iftttBeforeValue", type: "number", title: "Number of minutes IFTTT normally sends messages (usually 10 or 15 minutes)"
    input name: "multipleWarnings", type: "string", title: "Comma separated list of seconds to use in delay to repeat the message (\"0,120\" would play the mssage immediately and again in 2 minutes)"
    input name: "descriptionTextEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
  }
}

mappings {
  path("/announce/:message") {
    action: [
      GET: "speak"
    ]
  }
}

private logInfo(msg) {
  if (descriptionTextEnable) log.info msg
}

def logDebug(msg) {
  if (logEnable) log.debug msg
}

def speak(data) {
  def message = params.message
  if (message) {

    logDebug "Encoded message: $message"
    message = java.net.URLDecoder.decode(message, "UTF-8")
    logDebug "Decoded message: $message"

    if (announceDevice) {
     
      if (multipleWarnings) {
        def intervals = multipleWarnings.split(',')
        intervals.each {
          if (it.trim() != null) {
            runIn(it.trim().toInteger() + 1, "doSpeech", [data: [message: message], overwrite: false])
            logInfo "Scheduled message to run in ${it.trim().toInteger()} second(s)"
          }
          else {
            log.warn "Multiple messages were specified but delay values could not be parsed!"
          }
        }
      }
      else {
        doSpeech([message: message])
      }

    }
  }
}

def doSpeech(data) {
  def vol
  if (constantVolume) {
    if (constantVolumeValue == null) {
      log.warn "Constant volume set but no volume given.  Check app settings!"
    }
    vol = getVolume(announceDevice)
    doVolume(announceDevice, constantVolumeValue.toInteger())
  }
  logInfo "Speaking message \"${data.message}\" on device ${announceDevice.label}"
  announceDevice.speak("${data.message} starts soon.")
  if (vol) {
    logDebug "Previous volume: $vol"
    atomicState.vol = vol
  }
}

def restoreVolume(event) {
  logDebug "$event"
  if (event.value == 'idle' && event.displayName == announceDevice.label) {
    if (atomicState.vol && getVolume(announceDevice) != atomicState.vol) {
      doVolume(announceDevice, atomicState.vol)
      logDebug "Previous volume ${atomicState.vol} restored."
      atomicState.remove("vol")
    }
  }
}

def getVolume(d) {
  try {
    return d.currentValue("volume")
  }
  catch (e) {
    try {
      return d.currentValue("level")
    }
    catch(ex){
      log.error "Device doesn't support volume!"
    }
  }
}

def doVolume(d, v) {
  try {
    d.setVolume(v)
  }
  catch (e) {
    try {
      d.setLevel(v)
    }
    catch(ex){
      log.error "Device doesn't support volume!"
    }
  }
}

def updated() {

  try {
    if (!atomicState.accessToken) {
      createAccessToken()
    }
  }
  catch (ex) {
    log.warn "Probs need to enable OATH in the app's code, dood/ette."
  }
  logDebug "Access token: ${atomicState.accessToken}"
  logDebug "Full API server URL: ${getFullApiServerUrl()}"
  def path = "${getFullApiServerUrl()}/announce/{{Subject}}?access_token=${atomicState.accessToken}"
  log.warn "Use this URL in IFTTT: ${path}"

  unsubscribe()
  subscribe(announceDevice, "status", restoreVolume)

  if (constantVolumeValue > 100) {
    app.updateSetting("constantVolumeValue", 100)
    log.warn "Volume capped at 100."
  }
  if (constantVolumeValue < 0) {
    app.updateSetting("constantVolumeValue", 0)
    log.warn "Volume capped at 0.  This doesn't make a lick of sense but you do you."
  }

}