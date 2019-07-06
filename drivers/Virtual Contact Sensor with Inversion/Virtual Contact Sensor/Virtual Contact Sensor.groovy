/**
*  Virtual Contact Sensor
*
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
*/

def version() {"v1.0.20190706"}

metadata {
    definition (name: "Virtual Contact Sensor", namespace: "stephack", author: "Stephan Hackett", importUrl: "https://raw.githubusercontent.com/stephack/Hubitat/master/drivers/Virtual%20Contact%20Sensor%20with%20Inversion/Virtual%20Contact%20Sensor/Virtual%20Contact%20Sensor.groovy") {
        capability "Actuator"
        capability "Sensor"
        capability "Contact Sensor"
        
        command "open"
        command "close"
    }
    preferences {
        input "nOpen", "bool", title: "Invert Open/Close Events?", required: false
    }
}

def parse() {
}

def open() {
    nOpen?sendEvent(name: "contact", value: "closed"):sendEvent(name: "contact", value: "open")
        
}

def close() {
    nOpen?sendEvent(name: "contact", value: "open"):sendEvent(name: "contact", value: "closed")
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
