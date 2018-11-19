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
 *  10/14/18 	- template to add 10 more custom device drivers to the createVirtualDevice() method
 * 				- ValidDeviceTypes state variable created so that all valid types are displayed on container device page for reference
 *  
 *	10/31/18	- old createVirtualDevice command is now quickCreate and uses a dropdown preference for the driver type.
 *				- createDevice command now supports any driver type.
 *
 *	11/15/18	- added more Hubitat Virtual drivers and @Cobra custom drivers to dropdown list
 *				- default deviceType is a virtual momentary switch (only virtual device type that cannot be created with the createDevice method)
 *				- added update checking code. Thanks to @Cobra for his guidance on this and the dropdown idea.
 *
 *	11/18/18	- added appCreateDevice(vName, vType, vSpace, vId) method for use by other smartApps. This method adds an additional parameter 'vId' that allows the smartapp to index all the children in some way.
 *				- added childComm(devComm, devData, devRef) that allows child devices to communicate back up to the parent smartApp (single parameter only).
 *				- added childList() to be used by parent smartApps to request a list of all childDevices.
 *
 */

def version() {"v1.1.20181118"}

metadata {
	definition (name: "Virtual Container", namespace: "stephack", author: "Stephan Hackett") {
        capability "Refresh"
        capability "Switch Level" //stores the current virtual switch - to be used with cycle() method AND can be used with voice control eg Alexa set Bathroonm Presets to 3
        attribute "containerSize", "number"	//stores the total number of child switches created by the container
    	command "cycle"	//can be called by smartApps to cycle through switches one at a time (3 Types)
        command "quickCreate", ["LABEL"] //creates a new supported Virtual Devices using minimal details
        command "createDevice", ["DEVICE LABEL", "DRIVER TYPE ", "NAMESPACE ."] //create any new Virtual Device
		command "checkForUpdate"
    }
}

preferences {
    input("cycleType", "enum", title: "Select how Cycle/Set Level will function:", defaultValue: 1, options: getCycleOption())
	input("deviceType", "enum", title: "Quick Create Device Template", options: preloaded("ref"))
	input("includeCobra", "bool", title: "Include Cobra's Custom Drivers?")
}

def childComm(devComm, devData, devRef){
	log.info "vchild received info"+devRef
	parent."${devComm}"(devData,devRef)
}

def childList(){
	def children = getChildDevices()
	return children
}

def quickCreate(vName){
    state.vsIndex = state.vsIndex + 1	//increment even on invalid device type
	def thisDev = preloaded("all").find{it.ref == deviceType}
	log.info "Creating ${thisDev.driver} Device: ${vName}"
	childDevice = addChildDevice(thisDev.namespace, thisDev.driver, "VS-${device.deviceNetworkId}-${state.vsIndex}", [label: "${vName}", isComponent: false])
	if (deviceType == "Virtual Momentary Switch (hubitat)") childDevice.updateSetting("autoOff",[type:"enum", value: "500"])
	updateSize()
}

def createDevice(vName, vType, vSpace){
    try{
    	state.vsIndex = state.vsIndex + 1	//increment even on invalid device type
    	log.info "Attempting to create Virtual Device: Namespace: ${vSpace}, Type: ${vType}, Label: ${vName}"
		childDevice = addChildDevice(vSpace, vType, "VS-${device.deviceNetworkId}-${state.vsIndex}", [label: "${vName}", isComponent: false])
    	log.debug "Success"
    	updateSize()
    }
    catch (Exception e){
         log.warn "Unable to create device. Please enter a valid driver type!!"
    }
}

def appCreateDevice(vName, vType, vSpace, vId){
    try{
    	state.vsIndex = state.vsIndex + 1	//increment even on invalid device type
    	log.info "Attempting to create Virtual Device: Namespace: ${vSpace}, Type: ${vType}, Label: ${vName}"
		childDevice = addChildDevice(vSpace, vType, "VS-${device.deviceNetworkId}-${state.vsIndex}", [label: "${vName}", isComponent: false, "vcId": "${vId}"])
    	log.debug "Success"
    	updateSize()
    }
    catch (Exception e){
         log.warn "Unable to create device. Please enter a valid driver type!!"
    }
}

def cycle() {			//called by smartApps to cycle through switches one at a time - primarily for Sonos Playlist Control
    if (cycleType?.toInteger() >  1){
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
    else{log.info "Cycle has not been enabled on OR is not compatible with this Container."}
}

def refresh() {
	log.debug "Refreshing Container values"
    sendEvent(name: "level", value: 0)
    updateLabels()
    updateSize()
	clearUnused()
    if(!state.vsIndex) state.vsIndex = 0
    checkForUpdate()
}

def checkForUpdate(){
	def params = [uri: "https://raw.githubusercontent.com/stephack/Hubitat/master/drivers/Virtual%20Container/update.json",
				   	contentType: "application/json"]
       	try {
			httpGet(params) { response ->
				def results = response.data
				def driverStatus
				def serverVersion = results.currVersion
				def localVersion = version()
				state.DriverOnServer = serverVersion + "${results.rawCode}"
				
				if(localVersion == serverVersion) driverStatus = "${results.noUpdateImg}"
				else driverStatus = "${results.updateImg}${results.changeLog}"
				state.DriverOnHub = localVersion + "<br>${driverStatus}"
			}
		} 
        catch (e) {
        	log.error "Error:  $e"
    		}
}

def installed() {
	log.debug "Installing and configuring Virtual Container"
    sendEvent(name: "level", value: 0)
    state.vsIndex = 0 //stores an index value so that each newly created Virtual Switch has a unique name (simply incremements as each new device is added and attached as a suffix to DNI)
	device.updateSetting("deviceType",[type:"enum", value: "Virtual Momentary Switch (hubitat)"])
    //initialize()
	refresh()
}

def updated() {
	initialize()
}

def initialize() {
	log.debug "Initializing Virtual Container"
    checkForUpdate()
	updateSize()
}

def setLevel(val) {
    int which = val.toInteger()
	sendEvent(name:"level", value:which)
    def childDevices = getChildDevices()?.sort{it.label}
    def myComm
    if(cycleType == "2") myComm = "on"
    if(cycleType == "3") myComm = "off"
    if(cycleType == "4" && childDevices[which-1].currentValue("switch") == "on") myComm = "off"
    if(cycleType == "4" && childDevices[which-1].currentValue("switch") == "off") myComm = "on"
    if(childDevices[which-1].hasCapability("PushableButton")) childDevices[which-1].push(1)
    else if(childDevices[which-1].hasCapability("Switch")) childDevices[which-1]."${myComm}"()
}

def updateSize() {
	int mySize = getChildDevices().size()
    sendEvent(name:"containerSize", value: mySize)
}

def updateLabels() { // syncs device label with componentLabel data value
    def myChildren = getChildDevices()
    myChildren.each{
        if(it.label != it.data.label) {
            it.updateDataValue("label", it.label)
        }
    }
}

def clearUnused(){
	if(state.customDevices || state.customDevices == []) state.remove('customDevices')
	if(state.idIndex) state.remove('idIndex')
	if(state.ValidDeviceTypes) state.remove('ValidDeviceTypes')
	if(state.version) state.remove("version")
}

def getCycleOption(){
 	def options = [[1:"Disable"],[2:"Turn On Device"],[3:"Turn Off Device"],[4:"Toggle Device"]]   
}

def preloaded(myKey) {
	def myMap = []
	myMap << [namespace:"hubitat", driver:"Virtual Switch", ref:"Virtual Switch (hubitat)"]
	myMap << [namespace:"hubitat", driver:"Virtual Switch", ref:"Virtual Momentary Switch (hubitat)"]
	myMap << [namespace:"hubitat", driver:"Virtual Dimmer", ref:"Virtual Dimmer (hubitat)"]
	myMap << [namespace:"hubitat", driver:"Virtual Button", ref:"Virtual Button (hubitat)"]
	myMap << [namespace:"hubitat", driver:"Virtual Lock", ref:"Virtual Lock (hubitat)"]
	myMap << [namespace:"hubitat", driver:"Virtual Contact Sensor", ref:"Virtual Contact Sensor (hubitat)"]
	myMap << [namespace:"hubitat", driver:"Virtual Motion Sensor", ref:"Virtual Motion Sensor (hubitat)"]
	myMap << [namespace:"hubitat", driver:"Virtual Presence", ref:"Virtual Presence (hubitat)"]
	
	if(includeCobra){
		myMap << [namespace:"Cobra", driver:"Average All Device", ref:"Average All Device (Cobra)"]
		myMap << [namespace:"Cobra", driver:"Custom WU Driver", ref:"Custom WU Driver (Cobra)"]
		myMap << [namespace:"Cobra", driver:"Switch Timer", ref:"Switch Timer (Cobra)"]
		myMap << [namespace:"Cobra", driver:"Virtual Presence Plus", ref:"Virtual Presence Plus (Cobra)"]
		myMap << [namespace:"Cobra", driver:"Weewx Weather Driver - With External Forecasting", ref:"Weewx Weather Driver - With External Forecasting (Cobra)"]
	}
	
    //def custom1 = [namespace:"Enter device namespace here", driver:"Enter Driver Name here", ref:"Enter the label you will use in Create Virtual Device to reference your driver"]
    //def custom2 = [namespace:"Enter device namespace here", driver:"Enter Driver Name here", ref:"Enter the label you will use in Create Virtual Device to reference your driver"]
    //def custom3 = [namespace:"Enter device namespace here", driver:"Enter Driver Name here", ref:"Enter the label you will use in Create Virtual Device to reference your driver"]
    //def custom4 = [namespace:"Enter device namespace here", driver:"Enter Driver Name here", ref:"Enter the label you will use in Create Virtual Device to reference your driver"]
    //def custom5 = [namespace:"Enter device namespace here", driver:"Enter Driver Name here", ref:"Enter the label you will use in Create Virtual Device to reference your driver"]
    //def custom6 = [namespace:"Enter device namespace here", driver:"Enter Driver Name here", ref:"Enter the label you will use in Create Virtual Device to reference your driver"]
    //def custom7 = [namespace:"Enter device namespace here", driver:"Enter Driver Name here", ref:"Enter the label you will use in Create Virtual Device to reference your driver"]
    //def custom8 = [namespace:"Enter device namespace here", driver:"Enter Driver Name here", ref:"Enter the label you will use in Create Virtual Device to reference your driver"]
    //def custom9 = [namespace:"Enter device namespace here", driver:"Enter Driver Name here", ref:"Enter the label you will use in Create Virtual Device to reference your driver"]
    //def custom10 = [namespace:"Enter device namespace here", driver:"Enter Driver Name here", ref:"Enter the label you will use in Create Virtual Device to reference your driver"]
    if(custom1) myMap << custom1
    if(custom2) myMap << custom2
    if(custom3) myMap << custom3
    if(custom4) myMap << custom4
    if(custom5) myMap << custom5
    if(custom6) myMap << custom6
    if(custom7) myMap << custom7
    if(custom8) myMap << custom8
    if(custom9) myMap << custom9
    if(custom10) myMap << custom10
    
	if(myKey == "ref") newMap = myMap.ref
	if(myKey == "all") newMap = myMap
		
    return newMap
}

/*
SAMPLE CUSTOM:
def custom1 = [namespace:"stephack", driver:"Virtual Test Driver", ref:"Test"]
*/
