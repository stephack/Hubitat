/**
 *  Virtual Momentary Button Device Handler - For Use with Virtual Containers
 *
 *  Copyright 2017 Stephan Hackett
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
 *	Icons by http://www.icons8.com	
 *	
 */
def version() {"v1.0.20180530"}

metadata {
	definition (name: "Virtual Momentary Button", namespace: "stephack", author: "Stephan Hackett") {
	capability "Switch"
	capability "Sensor"
	capability "Actuator"
    capability "Momentary"
    attribute "conLabel", "string"	//stores container name for access by other smartApps
    attribute "buttonNo", "number"	//keeps track of what number this switch is in the container

	}
}


preferences {
    input ("test", "enum", title: "enter stuff", options: ["1","2","3"])
}


def push() {
	int whichChild = device.currentValue('buttonNo')
   	log.info "VMS $whichChild pushed"	
	sendEvent(name: "switch", value: "on", isStateChange: true, displayed: false)
	sendEvent(name: "momentary", value: "pushed", isStateChange: true)
    parent.childOn(whichChild)
    runIn(1,toggleMom)
}

def toggleMom(){
    sendEvent(name: "switch", value: "off")
}

def on() {		 
	push()
}

def off() {
	push()
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
