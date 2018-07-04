/*
 *	Advanced Button Controller (Parent/Child Version)
 *
 *	Author: Stephan Hackett
 * 
 *
 * 
 */

definition(
    name: "Advanced Button Controller",
    namespace: "stephack",
    singleInstance: true,
    author: "Stephan Hackett",
    description: "Configure devices with buttons like the Aeon Labs Minimote and Lutron Pico Remotes.",
    category: "My Apps",
    iconUrl: "https://cdn.rawgit.com/stephack/ABC/master/resources/images/abcNew.png",
    iconX2Url: "https://cdn.rawgit.com/stephack/ABC/master/resources/images/abcNew.png",
    iconX3Url: "https://cdn.rawgit.com/stephack/ABC/master/resources/images/abcNew.png",
)

preferences {
	page(name: "mainPage")
    page(name: "aboutPage", nextPage: "mainPage")
	
}

def mainPage() {
	return dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        if(!state.abcInstalled) {
            section("Hit Done to install ABC App!") {
        }
            
        }
        else {
    	def childApps = getAllChildApps()
        def childVer = "Initial Setup - Version Unknown"
        if(childApps.size() > 0) {
        	childVer = childApps.first().version()
        }
        section("Create a new button device mapping.") {
            app(name: "childApps", appName: "ABC Button Mapping", namespace: "stephack", title: "<img src=https://raw.githubusercontent.com/stephack/Hubitat/master/resources/images/New.png height=50 width=50>      New Button Device Mapping", multiple: true)
        }
        section("Version Info, User's Guide") {
       	href (name: "aboutPage", title: "<img src=https://raw.githubusercontent.com/stephack/Hubitat/master/resources/images/abc2.png height=80 width=80>   Advanced Button Controller \n"+childVer, 
       		description: "Tap to get Smartapp Info and User's Guide.",
       		//image: verImgCheck(childVer), required: false, // check repo for image that matches current version. Displays update icon if missing
       		page: "aboutPage"
		)		
   		}
        //remove("Uninstall ABC App","WARNING!!","This will remove the ENTIRE SmartApp, including all configs listed above.")
        }
    }
}

def installed() {
    state.abcInstalled = true
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
	section("<img src=https://raw.githubusercontent.com/stephack/ABC/master/resources/images/abcNew.png height=36 width=36> User's Guide - Advanced Button Controller") {
    	paragraph "This smartapp allows you to use a device with buttons including, but not limited to:\n\n  Aeon Labs Minimotes\n"+
    	"  HomeSeer HS-WD100+ switches**\n  HomeSeer HS-WS100+ switches\n  Lutron Picos***\n\n"+
		"It is a heavily modified version of @dalec's 'Button Controller Plus' which is in turn"+
        " a version of @bravenel's 'Button Controller+'."
   	}
	section("Some of the included changes are:"){
        paragraph "A complete revamp of the configuration flow. You can now tell at a glance, what has been configured for each button."+
        "The button configuration page has been collapsed by default for easier navigation."
        paragraph "The original apps were hardcoded to allow configuring 4 or 6 button devices."+
        " This app will automatically detect the number of buttons on your device or allow you to manually"+
        " specify (only needed if device does not report on its own)."
		paragraph "Allows you to give your buton device full speaker control including: Play/Pause, NextTrack, Mute, VolumeUp/Down."+
    	"(***Standard Pico remotes can be converted to Audio Picos)\n\nThe additional control options have been highlighted below."
	}
	section("Available Control Options are:"){
        paragraph "	Switches - Toggle \n"+
        "	Switches - Turn On \n"+
        "	Switches - Turn Off \n"+
        "	Switches - Toggle \n"+
            
        "	Dimmers - Set Level (Group 1) \n"+
        "	Dimmers - Set Level (Group 2) \n"+
        "	*Dimmers - Inc Level \n"+
        "	*Dimmers - Dec Level \n"+
        "	*Dimmers - Toggle on to Level \n"+
        "	*Dimmers - Ramp Up/Down (Smooth Dimming) \n"+
            
        "	*Color Lights - Set Temperature \n"+
        "	*Color Lights - Set Color \n"+
        
        "	*Speaker - Toggle Play/Pause \n"+
        "	*Speaker - Increment Volume \n"+
        "	*Speaker - Decrement Volume \n"+
        "	*Speaker - Next Track \n"+
        "	*Speaker - Mute/Unmute \n"+
        "	*Speaker - Cycle Preset \n"+
            
         "	Fans - Set Speed \n"+
        "	Fans - Cycle Speed \n"+    
        "	Fans - Legacy Cycle (Low, Medium, High, Off) \n"+  
            
        "	Locks - Unlock Only \n"+
        "	Shades - Up, Down, or Stop \n"+
        "	Sirens - Toggle \n"+
        "	Set Modes \n"+
        "	Speech Notifications \n"+
        "	SMS Notifications"
	}
	section ("** Quirk for HS-WD100+ on Button 5 & 6:"){
        paragraph "Because a dimmer switch already uses Press&Hold to manually set the dimming level"+
        " please be aware of this operational behavior. If you only want to manually change"+
        " the dim level to the lights that are wired to the switch, you will automatically"+
        " trigger the 5/6 button event as well. And the same is true in reverse. If you"+ 
        " only want to trigger a 5/6 button event action with Press&Hold, you will be manually"+
        " changing the dim level of the switch simultaneously as well.\n"+
        "This quirk doesn't exist of course with the HS-HS100+ since it is not a dimmer."
	}
	section("*** Lutron Pico:"){
        paragraph "There are 2 types of Pico configurations in HE:\n 1. The Standard Picos - with pushed events and held events (followed by released events).\n"+
    	"2. The Fast Picos - with pushed events followed by released events (no held events)."
	}
}
