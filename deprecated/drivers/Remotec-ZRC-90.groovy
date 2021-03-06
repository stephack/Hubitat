/**
 *  Remotec ZRC-90 Scene Master
 *  Copyright 2015 Eric Maycock
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
 *
 *	Edits/Updates by Stephan Hackett (stephack)
 *	02/06/18 - 	removed Button capability and added pushable and doubleTapped
 *			zwaveEvent case 3 adjusted to send doubleTapped event instead 
 *			buttonevent "name" and "value" fields adjusted to match new standards
 *			numberOfButtons adjusted to 8
 *			Removed Simulator and Tiles code
 *
 * 02/08/18 - adjusted Preferences so they could be properly displayed in hubitat DTH (bool instead of boolean/addition[] for holdMode
 *			
 *	
 * 02/12/18 - added methods for push(), hold(), doubleTap() - allows execution via device page
 *
 */
def version() {"v1.0.20180413"}

metadata {
	definition (name: "Remotec ZRC-90 Scene Master", namespace: "stephack", author: "Stephan Hackett") {
		capability "Actuator"
		capability "PushableButton"
        capability "Holdable Button"
		capability "DoubleTapableButton"
		capability "Configuration"
		capability "Sensor"
        capability "Battery"
        capability "Health Check"
        
        attribute "sequenceNumber", "number"
        attribute "numberOfButtons", "number"
        
        fingerprint mfr: "5254", prod: "0000", model: "8510"

		fingerprint deviceId: "0x0106", inClusters: "0x5E,0x85,0x72,0x21,0x84,0x86,0x80,0x73,0x59,0x5A,0x5B,0xEF,0x5B,0x84"
	}
	
    
    preferences {         
        input name: "holdMode", type: "enum", title: "Multiple \"held\" events on botton hold? With this option, the controller will send a \"held\" event about every second while holding down a button. If set to No it will send a \"held\" event a single time when the button is released.", options: [["1":"Yes"],["2":"No"]], description: "", required: true
    	input name: "debug", type: "bool", title: "Enable Debug?", defaultValue: false, displayDuringSetup: false, required: false
}

        
    
}

def parse(String description) {
	def results = []
     logging("${description}")
	if (description.startsWith("Err")) {
	    results = createEvent(descriptionText:description, displayed:true)
	} else {
		def cmd = zwave.parse(description, [0x2B: 1, 0x80: 1, 0x84: 1])
		if(cmd) results += zwaveEvent(cmd)
		if(!results) results = [ descriptionText: cmd, displayed: false ]
	}
    
    if(state.isConfigured != "true") configure()
    
	return results
}

def zwaveEvent(hubitat.zwave.commands.centralscenev1.CentralSceneNotification cmd) {
        logging(cmd)
        logging("keyAttributes: $cmd.keyAttributes")
        logging("sceneNumber: $cmd.sceneNumber")
        logging("sequenceNumber: $cmd.sequenceNumber")

        sendEvent(name: "sequenceNumber", value: cmd.sequenceNumber, displayed:false)
    switch (cmd.keyAttributes) {
           case 0:
              buttonEvent(cmd.sceneNumber, "pushed")
           break
           case 1:
              if (settings.holdMode == "2") buttonEvent(cmd.sceneNumber, "held")
           break
           case 2:
              if (!settings.holdMode || settings.holdMode == "1") buttonEvent(cmd.sceneNumber, "held")
           break
           case 3:
              buttonEvent(cmd.sceneNumber, "doubleTapped")
           break
           default:
               logging("Unhandled CentralSceneNotification: ${cmd}")
           break
        }
}

private def logging(message) {
    if (settings.debug == "true") log.debug "$message"
}


def zwaveEvent(hubitat.zwave.commands.wakeupv1.WakeUpNotification cmd) {
	def results = [createEvent(descriptionText: "$device.displayName woke up", isStateChange: false)]
	results << response(zwave.wakeUpV1.wakeUpNoMoreInformation().format())
	return results
}

def buttonEvent(value, type) {
	createEvent(name: type, value: value, descriptionText: "$device.displayName button $value was $type", isStateChange: true)
}

def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
	def map = [ name: "battery", unit: "%" ]
	if (cmd.batteryLevel == 0xFF) {
		map.value = 1
		map.descriptionText = "${device.displayName} battery is low"
		map.isStateChange = true
	} else {
		map.value = cmd.batteryLevel
	}
	createEvent(map)
}

def zwaveEvent(hubitat.zwave.Command cmd) {
	logging("Unhandled zwaveEvent: ${cmd}")
}

def installed() {
    logging("installed()")
    configure()
}

def updated() {
    logging("updated()")
    configure()
}

def configure() {
	logging("configure()")
    sendEvent(name: "checkInterval", value: 2 * 60 * 12 * 60 + 5 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
    sendEvent(name: "numberOfButtons", value: 8, displayed: true)
    state.isConfigured = "true"
    state.version = version()
}

def ping() {
    logging("ping()")
	logging("Battery Device - Not sending ping commands")
}

def push(value) {    
    sendEvent(name: "pushed", value: value, descriptionText: "$device.displayName button $value was pushed", isStateChange: true)
}

def hold(value) {
    sendEvent(name: "held", value: value, descriptionText: "$device.displayName button $value was held", isStateChange: true)
}

def doubleTap(value) {
    sendEvent(name: "doubleTapped", value: value, descriptionText: "$device.displayName button $value was double tapped", isStateChange: true)
}
