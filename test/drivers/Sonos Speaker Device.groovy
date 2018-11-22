/**
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
*
*/
def version() {"v1.0.20180415"}

preferences {
    input("myServer", "text", title: "Sonos Api Server:", description: "", submitOnChange: true)
  	input("myPort", "number", title: "Server Port:", description: "", submitOnChange: true)
    if(myServer && myPort){
    	input("mySpeaker", "enum", title: "Choose Sonos Speaker:", description: "", multiple: false, required: false, options: apiGet("sList"))
    	input("numFavs", "number", title: "How many Favorites to save?", required: false, submitOnChange: true)
    	if(numFavs){
    		for(i in 1..numFavs){
            	input("myFavorites${i}", "enum", title: "Choose Favorites #${i}:", description: "", required: false, options: apiGet("sFavorites"))		
			}
    	}
    }
}

metadata {
  	definition (name: "Sonos Speaker Device", namespace: "stephack", author: "Stephan Hackett") {
    	capability "Notification"
    	capability "Actuator"
    	capability "Speech Synthesis"
        capability "Music Player"
        //capability "Polling"
        capability "Switch"
        //capability "Switch Level"
        capability "Refresh"
		capability "Actuator"
        
        command "cycle"
        command "togPlay"
  	}
}

def installed() {
 	initialize()
}

def updated() {
 	initialize()
    createPresetList()
}

def initialize() {
    state.version = version()
}

def createPresetList(){
    def favMap = []
    if(numFavs){
    	for(i in 1..numFavs){
        	log.debug settings["myFavorites${i}"]
        	favMap << settings["myFavorites${i}"]
    	}
    	log.info favMap
    	state.myPresets = favMap
        if(!state.currentPreset) state.currentPreset = 0
}
}

/////////////////////////////////////////  PLAY/PAUSE   //////////////

def on(){
    play()
}

def off(){
    pause()
}

def stop(){
    pause()
}

def play(){
    apiGet("${mySpeaker}/play")
    runIn(1,refresh)
    //sendEvent(name: "switch", value: "on")
}

def pause(){
    apiGet("${mySpeaker}/pause")
    runIn(1,refresh)
    //sendEvent(name: "switch", value: "off")
}


def togPlay(){
	//def currStatus = refresh().playbackState
    //if(currStatus.contains("PLAYING")){
    //	pause()
    //}
    //else {
    //       play()
    //}
    apiGet("${mySpeaker}/playpause")
    runIn(1,refresh)
}

/////////////////////////////////////////  TRACK CONTROL   //////////////

def nextTrack() {
    apiGet("${mySpeaker}/next")
    runIn(1,refresh)
}

def previousTrack(){
    apiGet("${mySpeaker}/previous")
    runIn(1,refresh)
}

def resumeTrack(){///todo
}

def restoreTrack(){///todo
}

def setTrack(track){
    playPreset(track)
}

def playTrack(track){
  //  apiGet("${mySpeaker}/playlist/${track}")
  //  runIn(1,refresh)
}

/////////////////////////////////////////  SOUND   //////////////

def setLevel(val){
    apiGet("${mySpeaker}/volume/${val}")
    runIn(1,refresh)
}

def mute(){
    apiGet("${mySpeaker}/mute")
    runIn(1,refresh)
}

def unmute(){
    apiGet("${mySpeaker}/unmute")
    runIn(1,refresh)
}

/////////////////////////////////////////  TTS   //////////////

def speak(msg){
    apiGet("${mySpeaker}/say/${msg}")
    runIn(1,refresh)
}

def deviceNotification(msg) {
    speak(msg)
}

def playText(msg){
    speak(msg)
}

/////////////////////////////////////////  CUSTOM   //////////////

def playPreset(track){
    apiGet("${mySpeaker}/favorite/${track}")
    runIn(1,refresh)
}

def cycle(){
    def newPreset
    if(state.currentPreset >= numFavs - 1){
   		newPreset = 0
    }
    else{
        newPreset = state.currentPreset + 1
    	 
    }    
    state.currentPreset = newPreset
    log.info state.currentPreset
    playPreset(state.myPresets[state.currentPreset])
    
}

def refresh(){
    def myStatus = apiGet("sRefresh")
    log.debug myStatus
    if(myStatus.playbackState.contains("PLAYING") || myStatus.playbackState.contains("TRANSITIONING")){ 
        sendEvent(name: "switch", value: "on")
    }
    else{
        sendEvent(name: "switch", value: "off")
    }
    sendEvent(name: "level", value: myStatus.volume)
    myStatus.mute? sendEvent(name:"mute", value:"mute"):sendEvent(name:"mute", value:"unmuted")
    sendEvent(name: "Type", value: myStatus.currentTrack.type)
    sendEvent(name: "Station", value: myStatus.currentTrack.stationName)
    sendEvent(name: "Artist", value:myStatus.currentTrack.artist)
    sendEvent(name: "Album", value:myStatus.currentTrack.album)
    sendEvent(name: "Title", value:myStatus.currentTrack.title)
    sendEvent(name: "status", value: myStatus.playbackState)
    log.info myStatus.currentTrack.uri
    return myStatus
}

/////////////////////////////////////////  API CALLS   //////////////

def apiGet(data){
    def myUri = "http://${myServer}:${myPort}/"
    def myPath =""
    def myOptions
    
    if(data == "sList") {myPath = "zones"}
    else if(data == "sRefresh"){myPath = "${mySpeaker}/state"}
    else if(data == "sFavorites"){myPath = "favorites"}
    else {myPath = data}
    
    def params = [
		uri: myUri,
        path: myPath
	]

	try {
        log.debug params
    	httpGet(params) { resp ->
        	//resp.headers.each {
    		//}
            if(data == "sList") {result = resp.data.coordinator.roomName}
            if(data == "sRefresh" || data == "sFavorites") {result = resp.data}
            return result
    	}
	}
    catch (e) {
		log.error "Check your Http Server Settings. Server returned: $e"
	}
}
