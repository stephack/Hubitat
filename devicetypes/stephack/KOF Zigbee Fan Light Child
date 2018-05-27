/**
 *  King Of Fans Zigbee Fan Controller - Light Child Device
 *
 *  Copyright 2017 Stephan Hackett
 *  in collaboration with Ranga Pedamallu, Dale Coffing
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
def version() {"v1.0.20180522"}

metadata {
	definition (name: "KOF Zigbee Fan Light Child", namespace: "stephack", author: "Stephan Hackett") {
		capability "Actuator"
        capability "Switch"
        capability "Switch Level"
        capability "Light"
        capability "Sensor" 
   }
}

def on() {
	parent.lightOn()
}

def off() {
	parent.lightOff()
}

def setLevel(val) {
	parent.setLightLevel(val)
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
