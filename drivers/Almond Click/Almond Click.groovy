/**
 *  Almond Click Button
 *
 *  Copyright 2017 Gene Eilebrecht
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
def version() {"v1.0.20180413"}

metadata {
	definition (name: "Almond Click", namespace: "geneiparq", author: "Gene Eilebrecht") {
        capability "Actuator"
		capability "PushableButton"
		capability "HoldableButton"
        capability "DoubleTapableButton"
        command "configure"
        command "push", ["number"]
        
fingerprint endpointId: "01", profileId: "0104", inClusters: "0000,0003,0500", outClusters: "0003,0501", model:"ZB2-BU01", manufacturer: "Securifi Ltd. ZB2-BU01"
	}
    
	tiles {
		standardTile("button", "device.button", width: 2, height: 2) {
			state "default", label: "", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#ffffff"
		}
        standardTile("configure", "configure", decoration: "flat", width: 1, height: 1) {
			state "default",  action:configure, icon:"st.secondary.configure"
        }
		main "button"
		details(["button", "configure"])
	}
}

def parse(String description) {
	//log.debug "parse description2: $description"
    def descMap = zigbee.parseDescriptionAsMap(description)
    //log.info descMap.data[0]
    if (descMap.clusterId == "0501" && descMap.data[0] == "03") {
        processEvent("pushed")
	}
    else if (descMap.clusterId == "0501" && descMap.data[0] == "02") {
		processEvent("held")
	}
    else if (descMap.clusterId == "0501" && descMap.data[0] == "00") {
		processEvent("doubleTapped")
	}
 	else {
    	log.debug "unhandled response"
        results = null
    }
            
	return results;
}

def processEvent(type){
    def recentEvents = device.eventsSince(new Date(now() - 5000)).findAll{it.name == type}
        log.info recentEvents.size
        if(recentEvents.size == 0){
        	sendEvent(name: type, value: "1", descriptionText: "${device.displayName} button 1 was ${type}", isStateChange: true, displayed: true)
            log.info "${device.displayName} Button 1 ${type}"
        }
    else {log.debug "Likely redundant event detected. Discarding second ${type} event."}
    
}

def installed() {
	initialize()
}

def updated() {
	initialize()
}

def initialize() {
	sendEvent(name: "numberOfButtons", value: 1)
    state.version = version()
}

def configure() {
	sendEvent(name: "numberOfButtons", value: 1)
}
