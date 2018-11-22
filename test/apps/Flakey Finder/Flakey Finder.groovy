/**
 *  Flakey Finder
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
    name: "Flakey Finder",
    namespace: "stephack",
    author: "Stephan Hackett",
    description: "Find Flakey/Stuck Devices",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/stephack/Virtual/master/resources/images/power.png",
    iconX2Url: "https://raw.githubusercontent.com/stephack/Virtual/master/resources/images/power.png",
    iconX3Url: "https://raw.githubusercontent.com/stephack/Virtual/master/resources/images/power.png"
)

preferences {
	section("") {
        input("myDevice", "capability.speechSynthesis", title: "Send push notification to:", description: "Choose notification device:", required: false)
		input("myMotion", "capability.motionSensor", title: "Choose Motion", description: "Choose motion device:", required: false, multiple: true)
        input("myContact", "capability.contactSensor", title: "Choose Contact", description: "Choose contact device:", required: false, multiple: true)
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unschedule()
	unsubscribe()
	initialize()
}

def initialize() {
	
    checkMotions()
    checkContacts()
    //runEvery3Hours(getReport)
}


def checkMotions(){
    myMotion.each{
    	def myStates = it.statesSince("motion", new Date()-1, [max:1000])//appears to be no limit on date (only what is stored in device event list).....max size appears to 1200
    	def myReport = myStates.size()//findAll{it.value=="inactive"}.size()
    	//log.debug myReport//myStates.date[0].getTime()/1000 - myStates.date[1].getTime()/1000
    	if(myReport > 500) log.debug "${it} is generating a lot of activity (${myReport} events)"
    	else if(myReport < 5) log.debug "${it} is showing very little activity (${myReport} events)"
    	else log.debug "${it} seems OK (Only ${myReport} events)"
    }
    
    //myDevice.speak("[L]${evt.value} Mode is now active")
}

def checkContacts(){
    myContact.each{
    	def myStates = it.statesSince("contact", new Date()-1, [max:200])
    	def myReport = myStates.size()
    	if(myReport > 100) log.debug "${it} is generating a lot of activity (${myReport} events)"
        else if(myReport < 2) log.debug "${it} is generating a lot of activity (${myReport} events)"
    	else log.debug "${it} seems OK (Only ${myReport} events)"
    }
}
