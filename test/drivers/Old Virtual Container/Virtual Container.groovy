/**
 *  Virtual Container (Device Handler)
 *
 *  Copyright 2017 Stephan Hackett
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/lhttps://graph.api.smartthings.com/hub/listicenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *	Icons by http://www.icons8.com	
 *	
 * 06/25/18 - changed cycleChild command to cycle
 *
 */
def version() {"v1.0.20180625"}

metadata {
	definition (name: "Virtual Container", namespace: "stephack", author: "Stephan Hackett") {
	capability "Switch"
	capability "Switch Level"
        capability "Refresh"
    
    
    attribute "noVS", "number"	//stores the total number of child switches created by the container
    
    for(i in 1..6){		//used to create preset number tiles
    	attribute "vlabel${i}", "string"
    }
	
    command "cycle"	//can be called by smartApps to cycle through switches one at a time
    
    }

	
}

def refresh(){
	log.info refresh
    def childDevices = getChildDevices()
    childDevices.each{
    	log.info it.updateSetting("test",[type:"enum", value: "2"])
    }
}

def on() {

}

def off() {

}

def setLevel(val) {		//if container shared with Alexa...can be used to turn on child switches by number...for example...to set station presets... "Set Bathroom Preset to 2"
    int totalDevs = device.currentValue('noVS')
    int lvl = val
    if(val > 0 && val <= totalDevs){		// only responds to values from 1 to the total number of child devices
		log.info "VS turned $lvl. Sending event to SmartApp"
		childOn(lvl)
    }
}

def childOn(whichChild) {
    log.info "Container received ON request from child $whichChild. Sending event to SmartApp"
	parent.containerOn(whichChild)
    sendEvent(name: "level", value: whichChild)
}

def childOff(whichChild) {
    log.info "Container received OFF request from child $whichChild. Sending event to SmartApp"
	parent.containerOff(whichChild)
}

def childLevel(val, whichChild){
    log.info "Child $whichChild level set to $val. Sending event to SmartApp"
    parent.containerLevel(val, whichChild)
}

def cycle() {			//called by smartApps to cycle through switches one at a time
	log.info "Cycling to next device: $which"
    int currDev = device.currentValue('level')
    int totalDevs = device.currentValue('noVS')
    int nextDev
    if(currDev >= totalDevs) {
    	nextDev = 1
    }
    else {
    	nextDev = currDev + 1
    }
    childOn(nextDev)
}

def createChildVB(newTotal,vDetails, vType) {			//creates child Momentary switches
    def oldTotal = device.currentValue('noVS')
    if(newTotal < oldTotal) {
    	for (i in newTotal+1..oldTotal){        	
    		deleteChildVB("${i}")
        }
    }
    
    for(i in 1..newTotal){
    	def vName = vDetails.find{it.id==i}?.name
        
        def childDevice = getChildDevices()?.find {it.data.componentName == "VS-${i}"}
        if (!childDevice) {
           	log.info "Creating VS ${i}:${vName}"
            log.debug parent.id
        	childDevice = addChildDevice("stephack", "Virtual $vType", "VS_${parent.id} ${i}", [label: "${vName}", name: "${vName}", isComponent: true, componentName: "VS-${i}", componentLabel: "Virtual Switch ${i}"])
            childDevice.sendEvent(name: "buttonNo", value: i)
            childDevice.sendEvent(name:"switch", value: "off")
            childDevice.sendEvent(name:"conLabel", value: device.label)
            sendEvent(name:"vlabel${i}", value: vName)
		}
        else {
        	log.info "VS ${i} already exists"
            childDevice.label=vName
            childDevice.name=vName
            log.info "VS ${i} name is now [${vName}]"
            childDevice.sendEvent(name: "buttonNo", value: i)
            sendEvent(name:"vlabel${i}", value: vName)
		}
        
        
        
    }
    sendEvent(name: "noVS", value: newTotal)    
}

def deleteChildVB(which) {		//deletes specified child Momentary...or All children if specified..deletes always
	log.info "delete $which"
    def childDevice = getChildDevices()?.find{ it.deviceNetworkId.startsWith("VS") && it.deviceNetworkId.endsWith("${which}") }
    log.info "Deleting VS: [${childDevice}]"
    childDevice?deleteChildDevice(childDevice.deviceNetworkId):""
    sendEvent(name:"vlabel${which}", value: "--")
}

def installed() {
 	initialize()
}

def updated() {
    parent.updateLabel(device.label)
    def childDevice = getChildDevices()
    childDevice.each {child->
  		child.sendEvent(name:"conLabel", value: device.label)          
    }
 	initialize()
}

def initialize() {
    state.version = version()
}
/*****TEMPLATE FOR SMARTAPPS THAT USE THESE HANDLERS*******************************************************************************************

preferences {
	page(name: "mainPage")
	page(name: "vsPage")
}

def mainPage(){
	dynamicPage(name:"mainPage",install:true, uninstall:true){
        if(!vbTotal){
        	section(""){
        		input name: "vType", type: "enum", title: "Choose Virtual Device Type", submitOnChange: true, options: ["Momentary Button", "Switch"]
        	}
        }
    	if(vType){
    		section("Configure Virtual Container ($vType)"){
        		href "vsPage", title: "Device Type CANNOT be changed once configured", description: "Tap to Configure"//, description: getActionsDescript()
        	}
        }        
	}
}

def vsPage(){
	dynamicPage(name:"vsPage",uninstall:true){
		section("Create Virtual $vType") {
			input "vbTotal", "number", title: "How many to create", description:"Enter number: (1-6)", multiple: false, submitOnChange: true, required: true, range: "1..6"
		}
		if(vbTotal && vbTotal>=1 && vbTotal<=6){
			for(i in 1..vbTotal) {
				section("Virtual $vType ${i}"){
					input "vLabel${i}", "text", title: "Name of Device $i", description: "Enter Name Here", multiple: false, required: true
				}
			}
		}
		else if(vbTotal){
			section{paragraph "Please choose a value between 1 and 6."}
		}    
	}
}

//***********************************************************************************************************************************


def createContainer() {
	log.info "Creating Virtual Container"
    def childDevice = getAllChildDevices()?.find {it.device.deviceNetworkId == "VC_${app.id}"}        
    if (!childDevice) {
    	childDevice = addChildDevice("stephack", "Virtual Container", "VC_${app.id}", null,[completedSetup: true,
        label: app.label]) 
        log.info "Created Container [${childDevice}]"
        childDevice.sendEvent(name:"level", value: "1")
        for(i in 1..6){childDevice.sendEvent(name:"vlabel${i}", value:"--")}	///sets defaults for attributes...needed for inconsistent IOS tile display
	}
    else {
    	childDevice.label = app.label
        childDevice.name = app.label
        log.info "Container renamed to [${app.label}]"
	}
}

def createVB(vType){	//creates Virtual Switches of type: vType
    def vbInfo = []
	for(i in 1..vbTotal) {
    	vbInfo << [id:i, name:(app."vLabel${i}")]
    }
    def childDevice = getAllChildDevices()?.find {it.device.deviceNetworkId == "VC_${app.id}" }        
    childDevice.createChildVB(vbTotal,vbInfo,vType)
}

def containerOn(which) {	//when child # (which) is turned ON, this code will be executed
}


def containerOff(which) {	//when child # (which) is turned OFF, this code will be executed (not used by momentaries)
}

def containerLevel(val, which) {	//when child # (which) changes level to (val), this code should be excuted

}


//**********************************************************************************************************************************

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	app.label==app.name?app.updateLabel(defaultLabel()):app.updateLabel(app.label)	
    
    createContainer(vType)
    
    log.debug "Initialization Complete"
    if(vbTotal) createVB(vType)
}

def defaultLabel() {
	return "Virtual $vType Container"
}


*/
