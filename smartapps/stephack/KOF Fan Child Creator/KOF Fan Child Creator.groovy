/**
 *  KOF Child Device Creator
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
    name: "KOF Child Device Creator",
    namespace: "stephack",
    author: "Stephan Hackett",
    description: "Create Fan Speeds and Light for KOF Controller",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/stephack/Virtual/master/resources/images/power.png",
    iconX2Url: "https://raw.githubusercontent.com/stephack/Virtual/master/resources/images/power.png",
    iconX3Url: "https://raw.githubusercontent.com/stephack/Virtual/master/resources/images/power.png"
)



preferences {
    page(name: "setupPage")
    page(name: "createVirtual")
    page(name: "removeVirtual")
    page(name: "removalPage")
    page(name: "createPage")
}

def setupPage() {
    dynamicPage(name: "setupPage", install: true, uninstall: true) {
        section("Choose Fan") {
        	input("myFan", "capability.switch", title: "Select Fan", description: "", required: true)
        }
    }
}



def getFanName() {
	[
    "00":"Off",
    "01":"Low",
    "02":"Medium",
    "03":"Medium-High",
	"04":"High",
    "05":"Off",
    "06":"Comfort Breeze™",
    "07":"Light"
	]
}
/*

preferences {
	section("") {
		//input("phone", "phone", title: "Enter phone # to receive SMS notification:", description: "Phone Number", required: false)
        input("myFan", "capability.switch", title: "Choose Fan", description: "Choose Fan", required: true)
        //input("message", "text", title: "Enter message to send:")
	}
}

def installed() {
	log.info "Installing"
	initialize()
    log.info "Exiting Install"
}

def updated() {
	log.info "Updating"
    sendEvent(name: "checkInterval", value: 5 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
	if(state.oldLabel != device.label) {updateChildLabel()}
	initialize()
    response(refresh())
}

def initialize() {
	log.info "Initializing"
       	if(refreshChildren) {
            deleteChildren()
    		device.updateSetting("refreshChildren", false)
    	}
    	else {
			createFanChild()
    		createLightChild()
    	}
}
*/
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

def updateChildLabel() {
	log.info "Updating Device Labels"
	for(i in 1..6) {
    	def childDevice = getChildDevices()?.find {it.componentName == "fanMode${i}"}
        if (childDevice && i != 5) {childDevice.label = "${device.displayName} ${getFanName()["0${i}"]}"}
    }
    def childDeviceL = getChildDevices()?.find {it.componentName == "fanLight"}
    if (childDeviceL) {childDeviceL.label = "${device.displayName} Light"}
}

def createFanChild() {
	state.oldLabel = device.label  	//save the label for reference if it ever changes
	for(i in 1..6) {
    	def childDevice = getChildDevices()?.find {it.componentName == "fanMode${i}"}
        if (!childDevice && i != 5) {
           	log.info "Creating Fan Child ${childDevice}"
        	childDevice = addChildDevice("KOF Zigbee Fan Controller - Fan Speed Child Device", "${device.deviceNetworkId}-0${i}", null,[completedSetup: true,
            label: "${device.displayName} ${getFanName()["0${i}"]}", isComponent: true, componentName: "fanMode${i}",
            componentLabel: "${getFanName()["0${i}"]}", "data":["speedVal":"0${i}","parent version":version()]])
		}
       	else {
        	log.info "Fan Child ${i} already exists"
		}
	}
}

def createLightChild() {
    def childDevice = getChildDevices()?.find {it.componentName == "fanLight"}
    if (!childDevice) {
        log.info "Creating Light Child ${childDevice}"
		childDevice = addChildDevice("KOF Zigbee Fan Controller - Light Child Device", "${device.deviceNetworkId}-Light", null,[completedSetup: true,
        label: "${device.displayName} Light", isComponent: false, componentName: "fanLight",
        componentLabel: "Light", "data":["parent version":version()]])
    }
	else {
        log.info "Light Child already exists"
	}
}

def deleteChildren() {
	log.info "Deleting children"
	def children = getChildDevices()
    children.each {child->
  		deleteChildDevice(child.deviceNetworkId)
    }
}

def configure() {
	log.info "Configuring Reporting and Bindings."
    sendEvent(name: "checkInterval", value: 5 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
	return 	zigbee.configureReporting(0x0006, 0x0000, 0x10, 0, 600, null)+	//light on/off state - report min 0 max 600secs(10mins)
			zigbee.configureReporting(0x0008, 0x0000, 0x20, 0, 600, 0x01)+	//light level state - report min 1 max 600secs(10mins)
			zigbee.configureReporting(0x0202, 0x0000, 0x30, 0, 600, null)	//fan mode state - report min 0 max 600secs(10mins)
            //zigbee.onOffConfig()+
    		//zigbee.configSetup("8", "0", "0x20", "1", "100", "{01}")+
            //zigbee.configSetup("202", "0", "0x30", "0", "100", "{01}")
    		//zigbee.onOffConfig(0, 300)+
            //zigbee.levelConfig(0, 300)+
}

def on() {
	log.info "Resuming Previous Fan Speed"
	def lastFan =  device.currentValue("lastFanMode")	 //resumes previous fanspeed
    return setFanSpeed(lastFan)
}

def off() {
	log.info "Turning fan Off"
    def fanNow = device.currentValue("fanMode")    //save fanspeed before turning off so it can be resumed when turned back on
    if(fanNow != "00") sendEvent("name":"lastFanMode", "value":fanNow)  //do not save lastfanmode if fan is already off
    zigbee.writeAttribute(0x0202, 0x0000, 0x30, 00)
}

def lightOn()  {
	log.info "Turning Light On"
	zigbee.on()
}

def lightOff() {
	log.info "Turning Light Off"
	zigbee.off()
}

def lightLevel(val) {
	log.info "Adjusting Light Brightness Level"
    zigbee.setLevel(val) + (val?.toInteger() > 0 ? zigbee.on() : [])
}

def setFanSpeed(speed) {
	log.info "Adjusting Fan Speed to "+ getFanName()[speed]
    zigbee.writeAttribute(0x0202, 0x0000, 0x30, speed)
}

def fanSync(whichFan) {
	def children = getChildDevices()
   	children.each {child->
       	def childSpeedVal = child.getDataValue('speedVal')
        if(childSpeedVal == whichFan) {
            child.sendEvent(name:"fanSpeed", value:"on${childSpeedVal}")
            sendEvent(name:"switch",value:"on")
           	child.sendEvent(name:"switch",value:"on")
        }
        else {
           	if(childSpeedVal!=null){
                child.sendEvent(name:"fanSpeed", value:"off${childSpeedVal}")
           		child.sendEvent(name:"switch",value:"off")
           	}
        }
   	}
    if(whichFan == "00") sendEvent(name:"switch",value:"off")
}

def ping() {
    return zigbee.onOffRefresh()
}

def refresh() {
	getChildVer()
	return zigbee.onOffRefresh() + zigbee.levelRefresh() + zigbee.readAttribute(0x0202, 0x0000)
}

def getChildVer() {
	log.info "Updating Child Versioning"
	def FchildDevice = getChildDevices()?.find {it.componentName == "fanMode1"}
	if(FchildDevice){	//find a fan device, 1. get version info and store in FchildVer, 2. check child version is current and set color accordingly
    	sendEvent(name:"FchildVer", value: FchildDevice.version())
    	FchildDevice.version() != currVersions("fan")?sendEvent(name:"FchildCurr", value: "Update"):sendEvent(name:"FchildCurr", value: "OK")
    }
    def LchildDevice = getChildDevices()?.find {it.componentName == "fanLight"}
	if(LchildDevice) {	    //find the light device, get version info and store in LchildVer
    	sendEvent(name:"LchildVer", value: LchildDevice.version())
    	LchildDevice.version() != currVersions("light")?sendEvent(name:"LchildCurr", value: "Update"):sendEvent(name:"LchildCurr", value: "OK")
	}
}
