/**
 *  Sonos Playlist Controller for Hubitat
 *
 *  Copyright 2017 Stephan Hackett
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
 *	 
 * Speacial Thanks to @GuyInATie for allowing me to use snippets from his Sonos Remote Control smartApp. They were used
 * as the foundation of the code for retrieving and storing the recently played station from the Sonos speakers.
 *
 *	Icons by http://www.icons8.com	
 *
 */

def version() {return "0.1.20181106"}

definition(
    name: "Sonos PlayList Controller",
    namespace: "stephack",
    author: "Stephan Hackett",
    description: "Autoplay Stations/Playlists on Sonos speakers",
    category: "My Apps",
    iconUrl: "https://cdn.rawgit.com/stephack/Virtual/master/resources/images/spca.png",
    iconX2Url: "https://cdn.rawgit.com/stephack/Virtual/master/resources/images/spca.png",
    iconX3Url: "https://cdn.rawgit.com/stephack/SPC/Virtual/resources/images/spca.png"
)

preferences {
	page(name: "mainPage")
    page(name: "manageStations")
    page(name: "createStation")
    page(name: "deleteStation")
    page(name: "editStation")
    page(name: "aboutPage")
}

def mainPage() {
	//app.clearSetting("")
    //app.clearSetting("")
	return dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        if(!state.parentInstalled){
            section("Hit Done to install SPC App!") {
        	}
        }
        else{
        	section(getFormat("header", "Installed PlayList Controllers")) {
            	app(name: "childApps", appName: "Sonos PlayList Controller Child", namespace: "stephack", title: "Create New Playlist Controller", multiple: true)
        	}
            section(getFormat("header", "Manage Stations")){
                href "manageStations", title: "Add/Edit/Delete Stations"
            }
        	section(getFormat("header", "Version Info & User's Guide")) {
       			href (name: "aboutPage", 
       			title: "Sonos PlayList Controller\nver "+version(), 
       			description: "Tap for User's Guide and Info.",
       			image: "https://cdn.rawgit.com/stephack/Virtual/master/resources/images/spca.png",
       			required: false,
       			page: "aboutPage"
 	   			)
      		}
        }
    }
}

def manageStations(){
    dynamicPage(name: "manageStations", title: "") {
    	app.clearSetting("tempLabel")
    	app.clearSetting("tempUri")
		
		section(getFormat("header", "Add New Stations")){
			href "createStation", title: getImage("New", 30) + "Build New Station", description: ""
		}
		section(getFormat("header", "View/Edit Existing Stations:")){
			state.savedPL.each{
				href "editStation", title: getImage("Button", 30) + "${it.name}", params: [pIndex: it.index], description: ""
			}
		}
        section(getFormat("header", "Delete Stations")){
			href "deleteStation", title: "Delete Station", description: ""
		}
       // section("Recent Stuff"){
		//	input("sonos", "capability.musicPlayer", title: "Select Sonos Speaker:")
            //input("useVC", "bool", title: "Use Virtual Container?")
            //input("vc", "device.VirtualContainer", title: "Select Virtual Container:")
	//	}
		if(plLabel && uri){
			section(getFormat("redhead", "Pending Save")){
				paragraph "Playlist Name: ${plLabel} (${state.plIndex})\nUri: ${uri}"
				input("savePL", "button", title: "Confirm Add")
			}
		}
        if(toRemove){
			section(getFormat("redhead", "Pending Deletion")){
                def myName = state.savedPL.find{"${it.index}" == toRemove}.name
				paragraph "Pending Delete:\nPlaylist Name: ${myName}\nURI Index: ${toRemove}"
				input("removePL", "button", title: "Confirm Delete")
			}
		}
    }
}

def createStation(){
    dynamicPage(name: "createStation", title: getFormat("header", "Add Station:")) {
        def index = state.plIndex
     	section("Playlist Index: ${index}"){
        	input("plLabel", "text", title: "Name of Playlist:", submitOnChange:true, required: true)
			input("uri", "text", title: "Playlist URI:", submitOnChange:true, required: true)
            if(uri && sonos){
				input("testPlay", "button", title: "Test URI")
				input("sonos", "capability.musicPlayer", title: "Select Sonos Speaker:",submitOnChange:true)

			}
            input("showRecents", "bool", title: "Show Recently Played URI's ?", submitOnChange: true)
		}
        if(showRecents){
        	section(getFormat("header","Recently Played URI's")){
            	if(state.recents) {
            		def i=1
            		state.recents.each{
						//if(it.type == "Radio Stream") log.debug "tt"
                		paragraph getFormat("centerBold", "${i}. Station Type=${it.type}\n${it.name}\n") + getFormat("hlight", "${it.uri}")
                		i++
            		}
            	}
                input("getRecents", "button", title: "Refresh Recent List")
            }
      	}
    }
}

def editStation(params){
    dynamicPage(name: "editStation", title: "View/Edit URI's:") {
        def index = params.pIndex
        def myStation = state.savedPL.find{it.index == index}
        state.currentStation = myStation.index
        if(!tempLabel) status = "Nothing Changed"
        else if(tempLabel != myStation.name) status = "New Label"
        else if(tempLabel == myStation.name) status = "Nothing Changed"
        section(""){paragraph "${status}"} 
       // log.debug pIndex
        section("Edit Station:"){
            input("tempLabel", "text", title: "Name:", defaultValue: myStation.name, submitOnChange:true)
            input("tempUri", "text", title: "Uri:", defaultValue: myStation.uri, submitOnChange:true)
            input("saveEdit", "button", title: "Save Edits")
          //  paragraph "${myStation.name}"
        }
    }
}

def deleteStation(){
    dynamicPage(name: "deleteStation", title: "Delete Station:") {
     	section("Select Station to Remove"){
        	input("toRemove", "enum", title: "Name of Playlist to Remove:", required: true, options: getStations())
			//input("savePL", "button", title: "Save")
		}
    }
}

def getRecents(){
    def states = sonos.statesSince("trackData", new Date(0), [max:30]).value
    def recentData = []
    states?.each{
		def myName
		def myUri
        def info = toJson(it)
		myName = info.station
		if(info.metaData && info.metaData != "null"){
			if(!info.station){
				def preStation = info.metaData?.replaceAll(".*<dc:title>", "")
				def postStation = preStation.split("<")[0]-"&apos;"
				myName = postStation
			}
			//if("${info.audioSource}" == "Radio Stream") myUri = info.uri
			//else myUri = info.transportUri
			recentData << ["name":myName, "uri":info.uri, "type":info.audioSource]//, "meta":postStation]
			}
    }
    state.recents = recentData.unique()
}

def getStations(){
	def stationMap = []
    state.savedPL.each{
        stationMap << ["${it.index}":it.name]
    }
    return stationMap
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    state.parentInstalled = true
    initialize()    
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    initialize()
}

def initialize() {
	log.debug "Parent Initialized"
    if(!state.plIndex) state.plIndex = 1
    if(!state.savedPL) state.savedPL = []
}

////////////FORMATTING CODE//////////////////////
def getImage(type, mySize) {
    def loc = "<img src=https://raw.githubusercontent.com/stephack/Hubitat/master/resources/images/"
    if(type == "Device") return "${loc}Device.png height=${mySize} width=${mySize}>   "
    if(type == "Button") return "${loc}Button.png height=${mySize} width=${mySize}>   "
    if(type == "Switches") return "${loc}Switches.png height=${mySize} width=${mySize}>   "
    if(type == "Color") return "${loc}Color.png height=${mySize} width=${mySize}>   "
    if(type == "Dimmers") return "${loc}Dimmers.png height=${mySize} width=${mySize}>   "
    if(type == "Speakers") return "${loc}Speakers.png height=${mySize} width=${mySize}>   "
    if(type == "Fans") return "${loc}Fans.png height=${mySize} width=${mySize}>   "
    if(type == "HSM") return "${loc}Mode.png height=${mySize} width=${mySize}>   "
    if(type == "Mode") return "${loc}Mode.png height=${mySize} width=${mySize}>   "
    if(type == "Other") return "${loc}Other.png height=${mySize} width=${mySize}>   "
    if(type == "Custom") return "${loc}Custom.png height=${mySize} width=${mySize}>   "
    if(type == "Locks") return "${loc}Locks.png height=30 width=30>   "
    if(type == "Sirens") return "${loc}Sirens.png height=30 width=30>   "
    if(type == "Scenes") return "${loc}Scenes.png height=30 width=30>   "
    if(type == "Shades") return "${loc}Shades.png height=30 width=30>   "
    if(type == "SMS") return "${loc}SMS.png height=30 width=30>   "
    if(type == "Speech") return "${loc}Audio.png height=30 width=30>   "
	if(type == "New") return "${loc}New.png height=30 width=30>   "
}

def getFormat(type, myText=""){
    if(type == "section") return "<div style='color:#78bf35;font-weight: bold'>${myText}</div>"
    if(type == "hlight") return "<div style='color:#78bf35'>${myText}</div>"
    if(type == "header") return "<div style='color:#ffffff;background-color:#392F2E;text-align:center'>${myText}</div>"
    if(type == "redhead") return "<div style='color:#ffffff;background-color:red;text-align:center'>${myText}</div>"
    if(type == "line") return "\n<hr style='background-color:#78bf35; height: 2px; border: 0;'></hr>"
    if(type == "centerBold") return "<div style='font-weight:bold;text-align:center'>${myText}</div>"	
	
}
////////////FORMATTING CODE//////////////////////

//////////BUTTON CODE////////////////////////////
def appButtonHandler(btn) {
    if(btn=="savePL"){savePL()}
    if(btn=="saveEdit"){saveEdit()}
    if(btn=="removePL"){saveDelete()}
    if(btn=="testPlay"){testPlay()}
    if(btn=="getRecents"){getRecents()}
    
}

def testPlay(){
    if(sonos && uri){
        sonos.playTrack(uri)
    }
}

def saveDelete(){
    def deleteMe = state.savedPL.find{"${it.index}" == toRemove}
    log.warn deleteMe
    state.savedPL.remove(deleteMe)
	app.clearSetting("toRemove")
}

def savePL(){
    //state.savedPL = []
    state.plIndex++
    state.savedPL << ["name":plLabel, "uri":uri, "index":state.plIndex]
	app.clearSetting("plLabel")
	app.clearSetting("uri")
}

def saveEdit(){
    def myStation = state.savedPL.find{it.index == state.currentStation}
    def statIndex = state.savedPL.indexOf(myStation)
    def newData = ["name":tempLabel, "uri":tempUri, "index":state.currentStation]
    state.savedPL.putAt(statIndex, newData)
    
}
//////////BUTTON CODE////////////////////////////

def toJson(str){
    def slurper = new groovy.json.JsonSlurper()
    def json = slurper.parseText(str)
}

def aboutPage() {
	dynamicPage(name: "aboutPage", title: none) {
     	section("User's Guide: Sonos Playlist Control") {
        	paragraph "This smartApp allows you to create voice commands for your integrated Sonos speakers. These commands are available to connect"+
            " with other smartApps like Alexa and Google Home. There are 2 types of 'Voice Commands' you can create."
        }
        section("1. Virtual Playlists"){
        	paragraph "These allow you to turn on the speaker and automatically start playing a station or playlist.\n"+
            "They are also exposed as dimmable switches, they should be used more like station presets buttons. They do NOT process 'OFF' commands.\nSee Best Practices below."
		}
		section("Best Practices:"){
        	paragraph "You should set your Virtual Playlist name to the voice command you would use to start playback of a particular station."+
            " While it can be used for volume control, it would be more practical to use the Virtual Speaker for that instead.\n"+
            "By design, it cannot be used to turn off the speaker. Again, the Virtual Speaker should be used instead.\n\n"+
            " - 'Alexa, turn on [Jazz in the Dining Room]'\n"+
            " Starts playback of the associated Jazz station."
 		}
	}
}
