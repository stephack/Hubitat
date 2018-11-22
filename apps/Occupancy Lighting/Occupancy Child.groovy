/**
 *  Occupancy Child
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
 *	todo: add levels per mode; set temps
 *
 *
 */

def version(){"v0.2.180826"}

definition(
    name: "Occupancy Child",
    namespace: "stephack",
    author: "Stephan Hackett",
    description: "Motion Lighting My way",
    category: "My Apps",
    parent: "stephack:Occupancy Lighting",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences {
    section("") {
        input "motionSensors", "capability.motionSensor", title: "Select Motion Sensors:", required: true, multiple:true
        input "mySwitches", "capability.switch", title: "Switches to Turn On:", multiple:true
        input "myDimmers", "capability.switchLevel", title: "Select Dimmers to Set:", multiple:true, submitOnChange: true
        if(myDimmers){
            input "onLevel", "number", title: "Level when Turned on:", required: true
            input "dimFirst", "bool", title: "Dim before turning off?", required: false, submitOnChange: true
            if(dimFirst){
            	input "dimLevel", "number", title: "Level when Dimmed:", required: true
        		input "dimTime", "number", title: "Inactivity Timeout before Lights Dim:", required: true
            }
        }
        input "offTime", "number", title: "Timeout before All Lights Turn Off:", required: true
        input "enableSwitch", "capability.switch", title: "Choose switch that enables/disables automation:", required: false
    }
}

def initialize() {
    state.version = version()
    if(!app.label || app.label == "default")app.updateLabel(defaultLabel())
    
    subscribe(motionSensors, "motion", motionHandler)
   //	parent.createVS(app.id,app.label)   
}

def defaultLabel() {
	return "${motionSensors} Settings"
}

//def uninstalled() {
//    parent.removeVS(app.id)
    
//}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
    
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def allInactive(){//ensures ALL motion sensors are inactive
    def allInactive = true
	motionSensors.each {eachMotion->
        if(eachMotion.currentValue("motion") == "active"){
            allInactive = false
        }
	}
	return allInactive
}


def motionHandler(evt) {
	if(!enableSwitch || enableSwitch.currentValue("switch") == "on"){
		//log.info evt.value 
    	if(evt.value == "inactive" && allInactive()) {
        	log.info "Inactive received. Starting Timeouts"
    		if(dimFirst) runIn(dimTime, dimLights)
        	runIn(offTime, setOff)
    	}
    	if(evt.value == "active") {
        	state.lastActive = now()
        	log.info "Active received. Setting Dimmers/Switches"
    		if(myDimmers) setDimmers()
        	if(mySwitches) setSwitch()
    	}
    }
}

def dimLights(){
    def delta = (now() - (state.lastActive ?:0))/1000
    if(delta < dimTime) {
        log.info "Cancelling Dim: Time Since Last Active = ${delta} and Dim Window = ${dimTime}"
    }
    else {
        myDimmers.each {eachLight->
    		if(eachLight.currentSwitch == "on") eachLight.setLevel(dimLevel)////only dims if light is on (prevents light from turning back on after physical off)
    	}
    }
}

def setDimmers() {
    myDimmers.setLevel(onLevel)
}

def setSwitch() {
    mySwitches.on()
}

def setOff() {
    def delta = (now() - (state.lastActive ?:0))/1000
    if(delta < offTime) {
        log.info "Cancelling Off: Time Since Last Active= ${delta} and Off Window = ${offTime}"
    }
    else if(myDimmers) myDimmers.off()
    else if(mySwitches) mySwitches.off()
}
