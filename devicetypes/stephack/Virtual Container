/**
 *  Virtual Container
 *
 *  Copyright 2018 Stephan Hackett
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
 */

def version() {"v1.0.20180902"}

metadata {
	definition (name: "Virtual Container", namespace: "stephack", author: "Stephan Hackett") {
        capability "Refresh"
        capability "Switch Level" //stores the current virtual switch - to be used with cycle() method AND can be used with voice control eg Alexa set Bathroonm Presets to 3
        attribute "containerSize", "number"	//stores the total number of child switches created by the container
    	command "cycle"	//can be called by smartApps to cycle through switches one at a time
        command "createVirtualSwitch", ["STRING"] //creates a new Virtual Switch with entered label
    }
}

def installed() {
	log.debug "Installing and configuring Virtual Container"
    sendEvent(name: "level", value: 0)
    state.vsIndex = 0 //stores an index value so that each newly created Virtual Switch has a unique name (simply incremements as each new device is added and attached as a suffix to DNI)
}

def initialize() {
	log.debug "Initializing Virtual Container"
    state.version = version()
    updateSize()
}

def updated() {}

def refresh() {
	log.debug "Refreshing Container values"
    sendEvent(name: "level", value: 0)
    updateSize()
}

def setLevel(val) {
    int which = val.toInteger()
	sendEvent(name:"level", value:which)
    def childDevices = getChildDevices()?.sort{it.label}
    childDevices[which-1].on()
}

def updateSize() {
	int mySize = getChildDevices().size()
    sendEvent(name:"containerSize", value: mySize)
}

def createVirtualSwitch(vName){
	int newIndex = state.vsIndex + 1
	log.info "Creating Child Switch ${vName}"
	childDevice = addChildDevice("hubitat", "Virtual Switch", "VS-${device.deviceNetworkId}-${newIndex}", [completedSetup: true, label: "${vName}",
	isComponent: false, componentName: "${vName}"])
	state.vsIndex = newIndex
	childDevice.sendEvent(name:"switch", value: "on")
    updateSize()
}

def cycle() {			//called by smartApps to cycle through switches one at a time - primarily for Sonos Playlist Control
	log.info "Cycling to next device"
    updateSize()
    int currDev = device.currentValue('level')
    int totalDevs = device.currentValue('containerSize')
    int nextDev
    
    if(currDev >= totalDevs) {
    	nextDev = 1
    }
    else {
    	nextDev = currDev + 1
    }
    setLevel(nextDev)
}
