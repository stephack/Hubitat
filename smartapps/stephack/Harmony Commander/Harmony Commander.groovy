/*
 *	Harmony Commander
 *
 *	Author: Stephan Hackett, Daniel Ogorchock
 * 
 *
 * 
 */

definition(
    name: "Harmony Commander",
    namespace: "stephack",
    singleInstance: true,
    author: "Stephan Hackett, Daniel Ogorchock",
    description: "Send Commands to Harmony Hub",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences {
	page(name: "mainPage")
    page(name: "aboutPage", nextPage: "mainPage")
	
}

def mainPage() {
	return dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        if(!state.hcInstalled) {
            section("Hit Done to install Harmony Commander App!") {
        }
            
        }
        else {
    		def childApps = getAllChildApps()
        	def childVer = "Initial Setup - Version Unknown"
        	if(childApps.size() > 0) {
        		childVer = childApps.first().version()
        	}
        	section("Create a harmony command or sequence of commands.") {
           		app(name: "childApps", appName: "HC Config", namespace: "stephack", title: "New Harmony Command(s)", multiple: true)
        	}
        	section("Version Info, User's Guide") {
       			href (name: "aboutPage", title: "Harmony Commander \n"+childVer, 
       			description: "Tap to get Smartapp Info and User's Guide.",
       			//image: verImgCheck(childVer), required: false, // check repo for image that matches current version. Displays update icon if missing
       			page: "aboutPage"
				)		
   			}
        }
    }
}

def installed() {
    state.hcInstalled = true
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {

}

def aboutPage() {
	dynamicPage(name: "aboutPage", title: none){
        textHelp()
	}
}

private def textHelp() {
	def text =
	section("User's Guide - Harmony Commander") {
    	paragraph ""
   	}
	
	
}
