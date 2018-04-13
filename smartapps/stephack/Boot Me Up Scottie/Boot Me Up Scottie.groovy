definition(
    name: "Boot Me Up Scottie",
    namespace: "stephack",
    author: "Stephan Hackett",
    description: "Activate WOL Magic Packet",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/stephack/Virtual/master/resources/images/power.png",
    iconX2Url: "https://raw.githubusercontent.com/stephack/Virtual/master/resources/images/power.png",
    iconX3Url: "https://raw.githubusercontent.com/stephack/Virtual/master/resources/images/power.png")

preferences {    
    section("Choose Switch") {
    	input "myDevice", "capability.switch", required: true, title: "Choose a Switch"
        input "myMac", "text", required: true, title: "MAC of workstation"
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
	log.info "${myDevice} activated"
    sendHubCommand(createWOL())
}

def createWOL(evt) {
    log.debug "Sending Magic Packet to: $myMac"
    def result = new hubitat.device.HubAction (
       	"wake on lan $myMac",
       	hubitat.device.Protocol.LAN,
       	null
    )    
    return result
}
