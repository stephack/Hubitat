/**
 *  Hue Revert
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
 */
 
definition(
    name: "Hue Revert",
    namespace: "stephack",
    author: "Stephan Hackett",
    description: "Revert Hue to Custom Defaults",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/stephack/Virtual/master/resources/images/power.png",
    iconX2Url: "https://raw.githubusercontent.com/stephack/Virtual/master/resources/images/power.png",
    iconX3Url: "https://raw.githubusercontent.com/stephack/Virtual/master/resources/images/power.png"
)

preferences {
    section("") {
        input ("controlDevice", "capability.switch", title: "Select Control Switch:")
        input ("bulbs", "capability.colorTemperature", title: "Select bulbs:", multiple:true)
    	input ("defTemp", "number", title: "Set Default Temp to Revert To:")
    }
}
    
def initialize() {
    subscribe(controlDevice, "switch.on", revertHandler)
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def revertHandler(evt) {
        def bulbState
    	//runIn(5,test)
    	bulbs.each {
            it.refresh()
            log.info it.displayName + " were " + it.currentSwitch
            bulbState = it.currentSwitch
            it.setColorTemperature(defTemp)
            if(bulbState) it."${bulbState}"()
        }
}

def test(){
	log.info "test"
    
    bulbs.each {
            	
       }
}

