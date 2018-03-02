/*	
 *
 *	Harmony Command Config
 *
 *	Author: Stephan Hackett
 * 
 * 
 */
def version(){"v0.1.180301"}

definition(
    name: "Harmony Command Config",
    namespace: "stephack",
    author: "Stephan Hackett",
    description: "Send commands to your harmony hub",
    category: "My Apps",
    parent: "stephack:Harmony Commander",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences {
	page(name: "chooseCommandType")
	
}

def chooseCommandType() {
	dynamicPage(name: "chooseCommandType", install: true, uninstall: true) {
		section("") {
            paragraph "\n\n****************************************\nSTEP 1: Select Device Type\n****************************************\n"
			input "commandType", "enum", title: "Http Device Type", multiple: false, required: true, submitOnChange: true, options: ["Activity Control","Device Command Sequence", "URL Path"]
		}
       // section("Set Custom Name (Optional)") {
        //	label title: "Assign a name:", required: false
       // }
        if(commandType){
         	section("") {
                paragraph "\n\n****************************************\nSTEP 2: Configure Http Server\n****************************************\n"
				input(name: "deviceIP", type: "string", title:"HTTP Server IP Address", description: "Enter IP Address of your HTTP server", required: true, submitOnChange: true)
				input(name: "devicePort", type: "string", title:"Server Port", description: "Enter Port of your HTTP server", required: true, submitOnChange: true)
				paragraph "\n\n****************************************\nSTEP 3: Configure Virtual Switch\n****************************************\n"
                input(name: "alexaName", type: "string", title:"Virtual Switch Name", description: "Use a name that works well with Alexa/GV", required: false, submitOnChange: true)
                if(commandType == "URL Path") {
                	input(name: "deviceMethod", type: "enum", title: "POST, GET, or PUT", options: ["POST","GET","PUT"], defaultValue: "POST", required: true, submitOnChange: true)
                    input(name: "devicePath", type: "string", title:"URL Path", description: "Rest of the URL, include forward slash.", submitOnChange: true)
                }
                else if(deviceIP && devicePort){
                	input "hub", "enum", title: "Select Hub", required: false, options: getFromApi("hub"), submitOnChange: true
        			if(hub && commandType == "Activity Control") input "activities", "enum", title: "Select Activity", required: false, options: getFromApi("activities"), submitOnChange: true
                    if(hub && commandType == "Device Command Sequence"){
                        input "devices", "enum", title: "Select Device", required: false, options: getFromApi("devices"), submitOnChange: true
                        input "numOfComms", "number", title: "Enter the # of commands to excute", required: true, submitOnChange: true
                        if(devices && numOfComms>0) {
                            paragraph "\n\n****************************************\nSTEP 4: Configure Commands\n****************************************\n"
                            getCommands(numOfComms)
                        }
                    }                    
                }
			}   
        }
    }    
}

private getOptionsInput(item) {
    input "${item}", "enum",
        title: "Select ${item}",
	//defaultValue: "none",
	required: false,
	displayDuringSetup: true,
	options: getFromApi(item)
}

def getCommands(num){
    for(i in 1..num) {
        input "deviceDelay${i}", "number", description: "sds", title: "Delay BEFORE Command ${i} (secs)"
    	input "deviceCommand${i}", "enum", description: "", title: "Command ${i}", options: getFromApi("commands")
        
    }
    
}

def getFromApi(item){
    def myOptions
	def myPath = ""
    def myServer = ""
    if(deviceIP) myServer = "http://${deviceIP}:${devicePort}"
    if(item=="hub") myPath = "/hubs"
    if(item=="activities") myPath = "/hubs/${hub}/activities"
    if(item=="commands") myPath = "/hubs/${hub}/devices/${devices}/commands"
    if(item=="devices") myPath = "/hubs/${hub}/devices"
    //log.info myPath
    def params = [
		uri: myServer,
        path: myPath
	]

	try {
    	httpGet(params) { resp ->
        	resp.headers.each {
        	//log.debug "${it.name} : ${it.value}"
    	}
    	//log.debug "response contentType: ${resp.contentType}"
    	//log.debug "response data: ${resp.data}"
       	if(item=="hub") myOptions = resp.data.hubs
        if(item=="activities") myOptions = resp.data.activities.slug
        if(item=="commands") myOptions = resp.data.commands.slug
        if(item=="devices") myOptions = resp.data.devices.slug
    	}
	} catch (e) {
		log.error "something went wrong: $e"
	}
     return myOptions
}

def installed() {
	initialize()
}

def uninstalled() {
    childDevices.each { deleteChildDevice(it.deviceNetworkId) }
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
    log.debug "INITIALIZED with settings: ${settings}"
    log.info "appLabel"+app.label
    log.info "appName"+app.name
	app.label==null?app.updateLabel(defaultLabel()):app.updateLabel(app.label)
    createVirtSwitch()
}

def createVirtSwitch() {
	log.info "Creating Virtual Switch"
    
    def childDevice = getChildDevices()?.find {it.device.deviceNetworkId == "HC_${app.id}"}      
    //log.error childDevice
    if (!childDevice) {
        childDevice = addChildDevice("stephack", "HTTP Control Switch", "HC_${app.id}", null,[completedSetup: true,
        label: alexaName]) 
    	
        log.info "Created HTTP Switch [${childDevice}]"
	}
    else {
    	childDevice.label = alexaName
        childDevice.name = alexaName
        log.info "Http Switch renamed to [${alexaName}]"
	}

}

def defaultLabel() {
	return alexaName+" ("+commandType+")"
}


def childOn(){
 	log.debug "childOn: "+commandType
    if(commandType == "Activity Control") runActivity()
    if(commandType == "Device Command Sequence") runSequence()
    if(commandType == "URL Path") runHttp(devicePath, deviceMethod)
    
}

def childOff(){
 	log.debug "childOff: "+commandType
    if(commandType == "Activity Control") runActivityOff()
    if(commandType == "Device Command Sequence") log.debug "off does nothing"
    if(commandType == "URL Path") log.debug "off does nothing"
}


def runActivity(){
	log.info "activity on"
    def path = "/hubs/"+hub+"/activities/"+activities
    log.info path
    runHttp(path, "POST") 
}

def runActivityOff() {
	log.info "activity off"
    def path = "/hubs/"+hub+"/off"
    log.info path
    runHttp(path, "PUT") 
}

def runSequence(){
    log.info "sequence"
    def nextDelay = 0
    for(i in 1..numOfComms){
    	nextDelay = nextDelay + settings["deviceDelay${i}"]
        log.error nextDelay
        runIn(nextDelay,"runCommand",[overwrite: false, data: i])
    }
}



def runCommand(data) {
    log.info data
    def path = "/hubs/"+hub+"/devices/"+devices+"/commands/"+settings["deviceCommand${data}"]
    log.info path
    runHttp(path, "POST")   
}

def runHttp(varCommand,method){
	def localDevicePort = (devicePort==null) ? "80" : devicePort
	def body = "" 
	def headers = [:] 
    headers.put("HOST", "${deviceIP}:${localDevicePort}")
	headers.put("Content-Type", "application/x-www-form-urlencoded")

	try {
		def hubAction = new hubitat.device.HubAction(
			method: method,
			path: varCommand,
			body: body,
			headers: headers
			)
		log.debug hubAction
		sendHubCommand(hubAction)
	}
	catch (Exception e) {
        log.debug "runCmd hit exception ${e} on ${hubAction}"
	}  
}

private def textHelp() {
	def text =
	section("User's Guide - Harmony Commander") {
    	paragraph ""
   	}
	
}
