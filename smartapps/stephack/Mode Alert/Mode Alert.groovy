/**
 *  Mode Alert
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
    name: "Mode Alert",
    namespace: "stephack",
    author: "Stephan Hackett",
    description: "Send notifications when Mode changes",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/stephack/Virtual/master/resources/images/power.png",
    iconX2Url: "https://raw.githubusercontent.com/stephack/Virtual/master/resources/images/power.png",
    iconX3Url: "https://raw.githubusercontent.com/stephack/Virtual/master/resources/images/power.png"
)

preferences {
	section("") {
		//input("phone", "phone", title: "Enter phone # to receive SMS notification:", description: "Phone Number", required: false)
        input("myDevice", "capability.speechSynthesis", title: "Send push notification to:", description: "Choose notification device:", required: false)
        //input("message", "text", title: "Enter message to send:")
	}
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

def initialize() {
	subscribe(location, "mode", modeChangeHandler)
}


def modeChangeHandler(evt){
	myDevice.speak("[L]${evt.value} Mode is now active")
}
