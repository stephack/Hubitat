/**
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
 *  GE Zwave Plus Dimmer Switch (Supports Double Taps but not Associations)
 *
 *  Copyright 2018 Stephan Hackett
 *
 */
def version() {"v1.0.20180413"}

metadata {
	definition (name: "GE Zwave Plus Dimmer Switch", namespace: "stephack", author: "Stephan Hackett and Chris Nussbaum") {
		capability "Switch"
        capability "PushableButton"
		capability "Refresh"
		capability "Switch Level"
		capability "Sensor"
		capability "Actuator"
		capability "Light"
        
        command "doubleTapUpper"
        command "doubleTapLower"
        command "inverted"
        command "notInverted"
        command "configure"
        command "indicatorWhenOn"
        command "indicatorWhenOff"
        command "indicatorNever"

		// These include version because there are older firmwares that don't support double-tap or the extra association groups
        fingerprint mfr:"0063", prod:"4944", model:"3038", ver: "5.26", deviceJoinName: "GE Z-Wave Plus Wall Dimmer"
        fingerprint mfr:"0063", prod:"4944", model:"3039", ver: "5.19", deviceJoinName: "GE Z-Wave Plus 1000W Wall Dimmer"
        fingerprint mfr:"0063", prod:"4944", model:"3130", ver: "5.21", deviceJoinName: "GE Z-Wave Plus Toggle Dimmer"
        fingerprint mfr:"0063", prod:"4944", model:"3135", ver: "5.26", deviceJoinName: "Jasco Z-Wave Plus Wall Dimmer"
        fingerprint mfr:"0063", prod:"4944", model:"3136", ver: "5.21", deviceJoinName: "Jasco Z-Wave Plus 1000W Wall Dimmer"
        fingerprint mfr:"0063", prod:"4944", model:"3137", ver: "5.20", deviceJoinName: "Jasco Z-Wave Plus Toggle Dimmer"
	}
}

def getCommandClassVersions() {
	[
		0x20: 1,  // Basic
		0x26: 3,  // SwitchMultilevel
		0x56: 1,  // Crc16Encap
		0x70: 1,  // Configuration
	]
}

def parse(String description) {
    //log.debug "Description: ${description}"
	def result = null
	if (description != "updated") {
		def cmd = zwave.parse(description, commandClassVersions)
        //log.debug "CMD: ${cmd}"
		if (cmd) {
			result = zwaveEvent(cmd)
	        log.debug("'$description' parsed to $result")
		} else {
			log.debug("Couldn't zwave.parse '$description'")
		}
	}
    result
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    //log.info "bv1Report"
	dimmerEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
    //log.info "bv1BasicSet"
	buttonEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
    //log.info "sv3Report"
	dimmerEvents(cmd)
}

def zwaveEvent(hubitat.zwave.commands.crc16encapv1.Crc16Encap cmd) {
    //log.info "CrC"
	def versions = commandClassVersions
	def version = versions[cmd.commandClass as Integer]
	def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
	def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
	if (encapsulatedCommand) {
		zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(hubitat.zwave.Command cmd) {
    //log.info "not interested"
	// Handles all Z-Wave commands we aren't interested in
	[:]
}

def dimmerEvents(hubitat.zwave.Command cmd) {
    //log.info "dimmerEvent"
	def result = []
	def value = (cmd.value ? "on" : "off")
	def switchEvent = createEvent(name: "switch", value: value, descriptionText: "$device.displayName was turned $value")
	result << switchEvent
	if (cmd.value) {
		result << createEvent(name: "level", value: cmd.value, descriptionText: "$device.displayName brightness set to $cmd.value",  unit: "%")
	}	
	return result
}

def buttonEvents(hubitat.zwave.Command cmd){
    //log.info "buttonEvent"
    if (cmd.value == 255) {
    	createEvent(name: "pushed", value: 1, descriptionText: "$device.displayName Upper Paddle Double-tapped (Button 1)", isStateChange: true, type: "physical")
    }
	else if (cmd.value == 0) {
    	createEvent(name: "pushed", value: 2, descriptionText: "$device.displayName Lower Paddle Double-tapped (Button 2)", isStateChange: true, type: "physical")
    }    
}

def on() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.switchMultilevelV1.switchMultilevelGet().format(),
	], 3000)
}

def off() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0x00).format(),
		zwave.switchMultilevelV1.switchMultilevelGet().format(),
	], 3000)
}

def indicatorWhenOn() {
	sendEvent(name: "indicatorStatus", value: "when on", descriptionText: "Indicator now displayed when ON", display: false)
	zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 3, size: 1).format()
}

def indicatorWhenOff() {
	sendEvent(name: "indicatorStatus", value: "when off", descriptionText: "Indicator now displayed when OFF", display: false)
	zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 3, size: 1).format()
}

def indicatorNever() {
	sendEvent(name: "indicatorStatus", value: "never", descriptionText: "Indicator NEVER displayed", display: false)
	zwave.configurationV2.configurationSet(configurationValue: [2], parameterNumber: 3, size: 1).format()
}

def inverted() {
	sendEvent(name: "inverted", value: "inverted", descriptionText: "Paddle Buttons now INVERTED", display: false)
	zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 4, size: 1).format()
}

def notInverted() {
	sendEvent(name: "inverted", value: "not inverted", descriptionText: "Paddle Buttons NOT INVERTED",display: false)
	zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 4, size: 1).format()
}

def doubleTapUpper() {
	sendEvent(name: "pushed", value: 1, descriptionText: "Upper Paddle Double-tapped (Button 1) on $device.displayName", isStateChange: true, type: "digital")
}

def doubleTapLower() {
	sendEvent(name: "pushed", value: 2, descriptionText: "Lower Paddle Double-tapped (Button 2) on $device.displayName", isStateChange: true, type: "digital")
}

def configure(){
 	sendEvent(name: "numberOfButtons", value: 2, displayed: false)   
    response(refresh())
}

def refresh() {
	zwave.switchMultilevelV1.switchMultilevelGet().format()
}

def setLevel(level) {
	if(level > 99) level = 99
	delayBetween([
		zwave.basicV1.basicSet(value: level).format(),
		zwave.switchMultilevelV1.switchMultilevelGet().format()
	], 5000)
}

def setLevel(value, duration) {
	log.debug "setLevel >> value: $value, duration: $duration"
	def valueaux = value as Integer
    log.info valueaux
	def level = Math.max(Math.min(valueaux, 99), 0)
    log.info level
	def dimmingDuration = duration < 128 ? duration : 128 + Math.round(duration / 60)
    log.info dimmingDuration
	def getStatusDelay = duration < 128 ? (duration*1000)+2000 : (Math.round(duration / 60)*60*1000)+2000
    log.info getStatusDelay
	delayBetween ([
        zwave.switchMultilevelV2.switchMultilevelSet(value: level, dimmingDuration: dimmingDuration).format(),
		zwave.switchMultilevelV1.switchMultilevelGet().format()
    ], getStatusDelay.toInteger())
}

def installed() {
 	initialize()
}

def updated() {
 	initialize()
}

def initialize() {
    state.version = version()
}
