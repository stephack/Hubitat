/*
 *	Occupancy Lighting (Parent App)
 *
 *	Author: Stephan Hackett
 * 
 * 
 * 
 */

definition(
    name: "Occupancy Lighting",
    namespace: "stephack",
    singleInstance: true,
    author: "Stephan Hackett",
    description: "Create Occupancy Lighting Child Apps",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences {
	page(name: "mainPage")
}

def mainPage() {
	return dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        if(!state.isInstalled) {
            section("Hit Done to install My Occupancy Lighting Apps!") {
        	}
        }
        else {
    		def childApps = getAllChildApps()
        	section("Create a new occupancy automation.") {
            	app(name: "childApps", appName: "Occupancy Child", namespace: "stephack", title: "<img src=https://raw.githubusercontent.com/stephack/Hubitat/master/resources/images/New.png height=50 width=50>      New Occupancy Settings", multiple: true)
        	}
        }
    }
}


def installed() {
    state.isInstalled = true
	initialize()
    
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
}

/*
def initialize() {
	//createContainer()
}

def createContainer() {
    log.info "Creating Virtual Container"
    def childDevice = getAllChildDevices()?.find {it.device.deviceNetworkId == "VC-${app.id}"}        
    if (!childDevice) {
    	childDevice = addChildDevice("stephack", "Virtual Container", "VC-${app.id}", null,[completedSetup: true, label: "Container - ${app.label}"]) 
        log.info "Created Container [${childDevice}]"
	}
    else {
    	log.info "Container already exists"   
    }
}

def createVS(appId,appName)   {
    log.info "Creating Virtual Switch within App Container"
    def childDevice = getAllChildDevices()?.find {it.device.deviceNetworkId == "VC-${app.id}"}        
    if (childDevice) {
    	childDevice.addVS(appId,appName)
	}
    else {
    	log.info "Virtual Container not found!!"   
    }
}

def removeVS(childId) {
    log.info "Sending delete request to Container: ${childId}"
	def childDevice = getAllChildDevices() 
    childDevice.each {
    	it.removeVS(childId)
    }
}

def uninstalled() {
    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

*/
