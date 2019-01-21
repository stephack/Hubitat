definition(
    name: "Boot Me Up Child",
    namespace: "stephack",
    author: "Stephan Hackett",
    description: "Activate WOL Magic Packet",
    category: "Convenience",
    parent: "stephack:Boot Me Up Scottie",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "")

preferences {    
    section("Choose Switch") {
    	input "myDevice", "capability.switch", required: true, title: "Choose a Switch"
        input "myMac", "text", required: true, title: "MAC of workstation"
	input "logEnable", "bool", title: "Enable Debug Logging?"
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
	subscribe(myDevice, "switch.on", myHandler)
}

def myHandler(evt) {
	if(logEnable) log.debug "${myDevice} activated"
    sendHubCommand(createWOL())
}

def createWOL(evt) {
	def newMac = myMac.replaceAll(":","").replaceAll("-","")
    if(logEnable) log.debug "Sending Magic Packet to: $newMac"
    def result = new hubitat.device.HubAction (
       	"wake on lan $newMac",
       	hubitat.device.Protocol.LAN,
       	null
    )    
    return result
}
