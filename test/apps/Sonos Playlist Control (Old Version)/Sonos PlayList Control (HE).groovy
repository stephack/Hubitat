/**
 *  Sonos Playlist Controller 
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

def version() {return "0.1.20180530"}

definition(
    name: "Sonos PlayList Control",
    namespace: "stephack",
    author: "Stephan Hackett",
    description: "Autoplay Stations/Playlists on Sonos speakers",
    category: "My Apps",
    //parent: "stephack:Sonos PlayList Control",
    iconUrl: "https://cdn.rawgit.com/stephack/Virtual/master/resources/images/spca.png",
    iconX2Url: "https://cdn.rawgit.com/stephack/Virtual/master/resources/images/spca.png",
    iconX3Url: "https://cdn.rawgit.com/stephack/SPC/Virtual/resources/images/spca.png"
)

preferences {
	//page(name: "startPage")
	//page(name: "parentPage")
	page(name: "mainPage", nextPage: confirmOptions)
	//page(name: "choosePlaylists", title: "Select stations for your Virtual Playlists", nextPage: confirmOptions)
	page(name: "confirmOptions", title: "Confirm All Settings Below")
    page(name: "aboutPage")
}

def startPage() {
    if (parent) {
        mainPage()
    } else {
        parentPage()
    }
}

def parentPage() {
	return dynamicPage(name: "parentPage", title: "", nextPage: "", install: true, uninstall: true) {
        section("Installed PlayList Controls") {
            app(name: "childApps", appName: appName(), namespace: "stephack", title: "Create New Playlist", multiple: true)
        }
        section("Version Info & User's Guide") {
       		href (name: "aboutPage", 
       		title: "Sonos PlayList Control\nver "+version(), 
       		description: "Tap for User's Guide and Info.",
       		image: "https://cdn.rawgit.com/stephack/Virtual/master/resources/images/spca.png",
       		required: false,
       		page: "aboutPage"
 	   		)
      	}
    }
}

private def appName() { return "${parent ? "VC Config" : "Sonos PlayList Control"}" }

def mainPage(){
	dynamicPage(name:"mainPage",uninstall:true){
    	section("Speaker to control with Virtual Playlists") {
        	input "sonos", "capability.musicPlayer", title: "Choose Speaker", submitOnChange: true, multiple:false, required: true, image: "https://cdn.rawgit.com/stephack/Virtual/master/resources/images/speakera.png"            
        }
        
        if(sonos){
        	section{
            	input "vbTotal", "number", title: "# of Presets to create", description:"Enter number: (1-6)", multiple: false, submitOnChange: true, required: true, image: "https://cdn.rawgit.com/stephack/Virtual/master/resources/images/spca.png", range: "1..6"
        	}        
        	if(vbTotal && vbTotal>=1 && vbTotal<=6){
        		for(i in 1..vbTotal) {
         			section("Virtual Preset ${i}"){
        				input "vLabel${i}", "text", title: "Preset Name", description: "Enter Name Here", multiple: false, required: true
            			input "tracks${i}","enum",title:"Playlist/Station to Run", description: "Tap to choose", required:true, multiple: false, options: stationSelections()
       				}
      			}
   			}
        	else if(vbTotal){
        		section{paragraph "Please choose a value between 1 and 6."}
        	}
        }
        
        section("Set Custom App Name") {
			label title: "Assign a Custom App Name", required: false
      	}                  
	}
}

def confirmOptions(){
	dynamicPage(name:"confirmOptions",install:true, uninstall:true){    
		section("Speaker Being Controlled"){
        		paragraph "${sonos}"
		}
        for(i in 1..vbTotal) {
           	def childLabel = app."vLabel${i}"
            def currTrack = app."tracks${i}"
       		section("VPL ${i} - Alexa Commands"){
        		paragraph "'Alexa, turn on ${childLabel}'\n\n"+
               	"This will turn on ${sonos} and start the playlist/station [${currTrack}]"
       		}
       	}    	
        if(state.oldVbTotal>vbTotal){
        	section("The following will be deleted"){
            	for(i in vbTotal+1..state.oldVbTotal) {
            		def childLabel = app."vLabel${i}"
                	def currTrack = app."tracks${i}"
        			paragraph "${childLabel} with [${currTrack}] will be removed."                	
       			}
          	}       	
        }
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
    //if(parent) { 
    	initChild() 
   // } else {
    //	initParent() 
   // }  
}

def initChild() {
	app.label==app.name?app.updateLabel(defaultLabel()):app.updateLabel(app.label)	
	    
    createContainer()
    if(state.savedPList==null){state.savedPList=[]} 
    savePlaylistsToState()
    state.oldVbTotal = vbTotal //keep track of number of changes to Virtual Device total to manage confirmOptions display of what will be deleted
    log.debug "Initialization Complete"
    createVB("Momentary Button")    
}

def initParent() {
	log.debug "Parent Initialized"
}

def defaultLabel() {
	return "${sonos}"-"Sonos"+"Presets"
}

def createContainer() {
	log.info "Creating Virtual Container"
    def childDevice = getAllChildDevices()?.find {it.device.deviceNetworkId == "VC_${app.id}"}        
    if (!childDevice) {
    	childDevice = addChildDevice("stephack", "Virtual Container", "VC_${app.id}", null,[completedSetup: true,
        label: app.label]) 
        log.info "Created Container [${childDevice}]"
        childDevice.sendEvent(name:"level", value: "1")
        for(i in 1..6){childDevice.sendEvent(name:"vlabel${i}", value:"--")}	///sets defaults for attributes...needed for inconsistent IOS tile display
	}
    else {
    	childDevice.label = app.label
        childDevice.name = app.label
        log.info "Container renamed to [${app.label}]"
	}
}

def createVB(vType){	//creates Preset Virtual Momentary Switches
    def vbInfo = []
	for(i in 1..vbTotal) {
    	vbInfo << [id:i, name:(app."vLabel${i}")]
    }
    def childDevice = getAllChildDevices()?.find {it.device.deviceNetworkId == "VC_${app.id}" }        
    childDevice.createChildVB(vbTotal,vbInfo,vType)
}

def containerOn(which) {	//sends Child station requests to Sonos speaker
    def currTrack = app."tracks${which}"
    selectPlaylist(currTrack)
    log.info "Child $which requested [$currTrack] playlist on $sonos"
}

def containerOff(which) {
}

def containerLevel(val, which) {
}



//{audioSource=Sonos Q, station=null, name=Lets Be Superheroes, artist=Bounce Patrol, album=Kids Songs, trackNumber=4, status=playing, level=37, mute=unmuted, uri=x-rincon-queue:RINCON_949F3E14DD0401400#0, trackUri=x-sonos-http:A0DvPDnowsKT-ql_Qx247D2ptuyjd2cMMy5gvQ4mccM_FRVVJe2eOQ.mp3?sid=151&flags=32&sn=4, transportUri=x-rincon-queue:RINCON_949F3E14DD0401400#0, enqueuedUri=x-rincon-cpcontainer:1004204cxwT8X2rh3H99IUYJD_vlFBi-kKPq1K-Q5Av-CV86IlVaoyhvOaCsYg?sid=151&flags=8268&sn=4, metaData=<DIDL-Lite xmlns:dc=\"http:\/\/purl.org\/dc\/elements\/1.1\/\" xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0\/upnp\/\" xmlns:r=\"urn:schemas-rinconnetworks-com:metadata-1-0\/\" xmlns=\"urn:schemas-upnp-org:metadata-1-0\/DIDL-Lite\/\"><item id=\"1004204cxwT8X2rh3H99IUYJD_vlFBi-kKPq1K-Q5Av-CV86IlVaoyhvOaCsYg\" parentID=\"100520649zyj7a88zQERCtGerSSbntY3TSIXb0wTJuH9zCxru_xuuDja4gDCqw\" restricted=\"true\"><dc:title>Kids Songs<\/dc:title><upnp:class>object.container.album.musicAlbum<\/upnp:class><desc id=\"cdudn\" nameSpace=\"urn:schemas-rinconnetworks-com:metadata-1-0\/\">SA_RINCON38663_X_#Svc38663-64d7a80d-Token<\/desc><\/item><\/DIDL-Lite>, trackMetaData=<DIDL-Lite xmlns:dc=\"http:\/\/purl.org\/dc\/elements\/1.1\/\" xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0\/upnp\/\" xmlns:r=\"urn:schemas-rinconnetworks-com:metadata-1-0\/\" xmlns=\"urn:schemas-upnp-org:metadata-1-0\/DIDL-Lite\/\"><item id=\"-1\" parentID=\"-1\" restricted=\"true\"><res protocolInfo=\"sonos.com-http:*:audio\/mpeg:*\" duration=\"0:03:32\">x-sonos-http:A0DvPDnowsKT-ql_Qx247D2ptuyjd2cMMy5gvQ4mccM_FRVVJe2eOQ.mp3?sid=151&amp;flags=32&amp;sn=4<\/res><r:streamContent><\/r:streamContent><r:radioShowMd><\/r:radioShowMd><upnp:albumArtURI>\/getaa?s=1&amp;u=x-sonos-http%3aA0DvPDnowsKT-ql_Qx247D2ptuyjd2cMMy5gvQ4mccM_FRVVJe2eOQ.mp3%3fsid%3d151%26flags%3d32%26sn%3d4<\/upnp:albumArtURI><dc:title>Let&apos;s Be Superheroes<\/dc:title><upnp:class>object.item.audioItem.musicTrack<\/upnp:class><dc:creator>Bounce Patrol<\/dc:creator><upnp:album>Kids Songs<\/upnp:album><\/item><\/DIDL-Lite>}
 

def stationSelections(){	//retrieves recently played stations from sonos speaker and presents as options when choosing station in VPLs
    def newOptions = state.savedPList.findAll()//{it.selected==true}	//ensures previously selected stations are included in the new list
    //log.error newOptions
    def states = sonos.statesSince("trackData", new Date(0), [max:30]) //??gets all previously played tracks from sonos speaker
    //log.info states
    
    states?.each{	//for each track in the fresh list
        def stationName
        def stationData = toJson(it.value)
        if(stationData.metaData && stationData.metaData != "null"){
            log.debug "meta:"+stationData.metaData
            if(stationData.station && stationData.station != "null") {
                stationName = stationData.station
            	log.debug "station:"+stationData.station
            	log.debug "stationName:"+stationName
			}
            else{
        		def preStation = stationData.metaData.replaceAll(".*<dc:title>", "")
        		//log.debug preStation
            	def postStation = preStation.split("<")[0]-"&apos;"
        		stationName = postStation
                log.debug "stationName:"+stationName
        	}
        
        	//log.debug stationName
        	//log.debug stationData.transportUri
        
        
            
        	if(stationName && !newOptions.find{fav->fav.station==stationName}){ //checks whether previously selected tracks(newOptions) exist in the new list and prevents adding second entry
   				newOptions << [uri:stationData.transportUri,station:stationName]
        	}
    	}
    }
    def options = newOptions.collect{it.station}.sort()
    log.info newOptions
    log.info options
    state.savedPList = newOptions
	return options
}

def savePlaylistsToState(){	//stores all the stations selected by the VPLs in savedPList state
	log.info "Saving Playlist info"
    def newStationList = []
    for(i in 1..vbTotal){
        def placeHold = app."tracks${i}"
    	if(placeHold){
        	newStationList << state.savedPList.find{it.station==placeHold} // add each selected station to savedPList state
        }
   	}
    state.savedPList = newStationList
}

def selectPlaylist(pList){	//receives playlist request from VPL and finds detailed track info stored in savedPList state
  	def myStations = state.savedPList
    if(myStations.size==0){
		log.debug "No Saved Playlists/Stations Found"
    }
    else{ 
    	def stationToPlay = myStations.find{stationToPlay->stationToPlay.station==pList}
    	playStation(stationToPlay)
    }    
}

def isPlaylistOrAlbum(trackData){ //determines type of playlist/station and formats properly for playback
	trackData.uri.startsWith('x-rincon-cpcontainer') ? true : false
}

def playStation(trackData){	//sends formatted play command to Sonos speaker
    if(isPlaylistOrAlbum(trackData)){
		log.debug "Working around some sonos device handler playlist wierdness ${trackData.station}. This seems to work"
        trackData.transportUri=trackData.uri
        trackData.enqueuedUri="savedqueues"
        trackData.trackNumber="1"
        sonos.setTrack(trackData)
        pause(1500)
        sonos.play()
    }
    else{
        log.info "sdfsdf"
    	sonos.playTrack(trackData.uri)
    }
}

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
