/*
 *	Boot Me Up Manager (Parent)
 *
 *	Author: Stephan Hackett
 * 
 * 
 * 
 */

definition(
    name: "Boot Me Up Scottie",
    namespace: "stephack",
    singleInstance: true,
    author: "Stephan Hackett",
    description: "Activate WOL Magic Packet - Parent Manager",
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
        if(!state.BmuInstalled) {
            section("Hit Done to install Boot Me Up Manager App!") {
        	}
        }
        else {
        	section("Create a new Boot Me Up Instance.") {
            	app(name: "childApps", appName: "Boot Me Up Child", namespace: "stephack", title: "New Boot Me Up Instance", multiple: true)
        	}
    	}
    }
}

def installed() {
    state.BmuInstalled = true
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
}
