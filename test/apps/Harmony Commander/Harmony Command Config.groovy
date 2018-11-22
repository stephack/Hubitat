/*	
 *
 *	Harmony Command Config
 *
 *	Author: Stephan Hackett, Daniel Ogorchock
 * 
 * 
 */
def version(){"v0.1.180302"}

definition(
    name: "Harmony Command Config",
    namespace: "stephack",
    author: "Stephan Hackett, Daniel Ogorchock",
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
			input "commandType", "enum", title: "Http Device Type", multiple: false, required: true, submitOnChange: true, options: ["Activity Control","Device Commands"]
		}
       // section("Set Custom Name (Optional)") {
        //	label title: "Assign a name:", required: false
       // }
        if(commandType){
         	section("") {
                paragraph "\n\n****************************************\nSTEP 2: Configure Http Server\n****************************************\n"
				input(name: "deviceIP", type: "string", title:"HTTP Server IP Address", description: "Enter IP Address of your HTTP server", required: true, submitOnChange: true)
				input(name: "devicePort", type: "string", title:"Server Port", description: "Enter Port of your HTTP server", required: true, submitOnChange: true)
				if(deviceIP && devicePort){
                	paragraph "\n\n****************************************\nSTEP 3: Configure Virtual Switch\n****************************************\n"
                	input(name: "alexaName", type: "string", title:"Virtual Switch Name", description: "Voice compatible name recommended", required: true, submitOnChange: true)
                }
                if(deviceIP && devicePort && alexaName){
                    paragraph "\n\n****************************************\nSTEP 4: Configure Hub Settings\n****************************************\n"
                	input "hub", "enum", title: "Select Hub", required: false, options: getFromApi("hub"), submitOnChange: true
        			if(hub && commandType == "Activity Control") input "activities", "enum", title: "Select Activity", required: false, options: getFromApi("activities"), submitOnChange: true
                    if(hub && commandType == "Device Commands"){
                        input "devices", "enum", title: "Select Device", required: false, options: getFromApi("devices"), submitOnChange: true
                        if(devices) {
                            paragraph "\n\n****************************************\nSTEP 5: Configure Commands\n****************************************\n"
                            input "numOfComms", "number", title: "Enter the # of commands to excute", required: true, submitOnChange: true
                            if(numOfComms>0) getCommands(numOfComms)
                        }
                    }                    
                }
			}   
        }
    }    
}

private def getCommands(num){
    for(i in 1..num) {
    	input "deviceCommand${i}", "enum", description: "", title: "COMMAND ${i}", options: getFromApi("commands")
        input "deviceDelay${i}", "number", description: "Delay in seconds BEFORE command ${i} is executed", title: "Delay Command ${i}"        
    }
    
}

private def getFromApi(item){
    def myOptions
	def myPath = ""
    def myServer = ""
    if(deviceIP) myServer = "http://${deviceIP}:${devicePort}"
    if(item=="hub") myPath = "/hubs"
    if(item=="activities") myPath = "/hubs/${hub}/activities"
    if(item=="commands") myPath = "/hubs/${hub}/devices/${devices}/commands"
    if(item=="devices") myPath = "/hubs/${hub}/devices"
    def params = [
		uri: myServer,
        path: myPath
	]

	try {
    	httpGet(params) { resp ->
        	resp.headers.each {
    	}
       	if(item=="hub") myOptions = resp.data.hubs
        if(item=="activities") myOptions = resp.data.activities.slug
        if(item=="commands") myOptions = resp.data.commands.slug
        if(item=="devices") myOptions = resp.data.devices.slug
    	}
	} catch (e) {
		log.error "Check your Http Server Settings. Server returned: $e"
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
    //log.info "appLabel"+app.label
    //log.info "appName"+app.name
	app.label==null?app.updateLabel(defaultLabel()):app.updateLabel(app.label)
    createVirtSwitch()
}

def createVirtSwitch() {    
    def childDevice = getChildDevices()?.find {it.device.deviceNetworkId == "HC_${app.id}"}      
    if (!childDevice) {
        log.info "Creating Virtual Switch"
        childDevice = addChildDevice("stephack", "HTTP Control Switch", "HC_${app.id}", null,[completedSetup: true,
        label: alexaName]) 
    	
        log.info "Created HTTP Switch [${childDevice}]"
	}
    else {
        log.info "Updating Virtual Switch"
    	childDevice.label = alexaName
        childDevice.name = alexaName
        log.info "Http Switch renamed to [${alexaName}]"
	}
}

def defaultLabel() {
    return "${alexaName} (${commandType})"
}

def childOn(){
    log.debug "childOn: ${commandType}"
    if(commandType == "Activity Control") runActivity()
    if(commandType == "Device Commands") runSequence()
}

def childOff(){
 	log.debug "childOff: ${commandType}"
    if(commandType == "Activity Control") runActivityOff()
    if(commandType == "Device Commands") log.info "Request ignored. Device Commands does not respond to off()."
}


def runActivity(){
    def path = "/hubs/"+hub+"/activities/"+activities
    runHttp(path, "POST") 
}

def runActivityOff() {
    def path = "/hubs/"+hub+"/off"
    runHttp(path, "PUT") 
}

def runSequence(){
    log.info "Command Sequence Started"
    def nextDelay = 0
    for(i in 1..numOfComms){
    	nextDelay = nextDelay + settings["deviceDelay${i}"]
        runIn(nextDelay,"runCommand",[overwrite: false, data: i])
    }
}

def runCommand(data) {
    log.info "Executing command ${data}"
    def path = "/hubs/"+hub+"/devices/"+devices+"/commands/"+settings["deviceCommand${data}"]
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
        log.debug "Method = ${method}, Path = ${varCommand}"
		sendHubCommand(hubAction)
	}
	catch (Exception e) {
        log.debug "runHttp hit exception ${e} on ${hubAction}"
	}  
}
