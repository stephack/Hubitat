/**
*  Join API Device
*
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
* This driver uses sections of code derived from the original Pushover Driver that @ogiewon and I worked on. Thanks for you contributions Dan.
*
*	06/04/19 	- added Notification category
*				- switch From GET requests to POST for added security
*				- switched to asynch calls for efficiency
*				- added multiple inline options (Title, Devices, Category, SMS#, Actions)
*
*	06/11/19	- fixed sms bug
*			- added Notification Icon (if Status Bar icon is not set, this will set the image to both notification and status bar icon)
*
*
*
*
*/

def version() {"v1.0.20190611a"}

preferences {
	input("apikey", "text", title: "Join API Key: <small><a href='https://joaoapps.com/join/api/' target='_blank'>[api docs here]</a></small>", description: "")
    if(getValidated()){
  		input("deviceNames", "enum", title: "Select Device <b>[D]</b>:", description: "", multiple: true, required: false, options: getValidated("deviceList"))
        input("title", "text", title: "Notification Title <b>[T]</b>:", description: "")
		input("icon", "text", title: "Notification Icon URI:", description: "(Notification image; and status bar icon if not set)")
		input("smallicon", "text", title: "Status Bar Icon URI:", description: "(Image to be displayed in the status bar)")
  		input("url", "text", title: "URL:", description: "(URL to be opened when Notification is clicked)")
  		input("category", "text", title: "Notification Category <b>[C]</b>:", description: "(Android 8+ allows custom notication handling)")
		input("sound", "text", title: "Sound URI:", description: "(URL of notification sound to be played)")
		input("image", "text", title: "Image URI:", description: "(URL of image to be displayed in the notification body)")
		input("myApp", "text", title: "Open App by Name:", description: "(Name of Android app to open)")
		input("appPackage", "text", title: "Open App by Package:", description: "(Name of Android Package to open)")
		input("smsnumber", "number", title: "Phone # to send SMS text TO <b>[S]</b>:", description: "(Text will be sent FROM the Join Device selected above)")
		input("actions", "text", title: "Actions for Notification <b>[A]</b>:", description: "(separate multiple actions with commas)")
		input("logEnable", "bool", title: "Enable Debug Logging?:", required: true)

    }
}

metadata {
  	definition (name: "Join API Device", namespace: "stephack", author: "Stephan Hackett", importUrl: "https://raw.githubusercontent.com/stephack/Hubitat/master/drivers/Join%20API%20Driver/Join%20API%20Driver.groovy") {
    	capability "Notification"
    	capability "Actuator"
    	capability "Speech Synthesis"
  	}
}

def installed() {
 	initialize()
}

def updated() {
 	initialize()   
}

def initialize() {
    state.version = version()
	state.devices = deviceNames
	//if(myPackage) device.updateSetting("appPackage","test")
	device.removeSetting("deviceName")
	device.removeSetting("myImage")
	device.removeSetting("myTitle")
	device.removeSetting("apiKey")
	device.removeSetting("myPackage")
	device.removeSetting("action")
}

def getValidated(type){
	if(apikey){
		if(type=="deviceList" && logEnable){log.debug "Generating Device List..."}
		else if(logEnable) log.debug "Validating Key..."

		def validated = false

		def params = [
			uri: "https://joinjoaomgcd.appspot.com/_ah/api/registration/v1/listDevices?apikey=${apikey}",
		]
		if(logEnable) log.debug "Validation params: ${params}"

		if ((apikey =~ /[A-Za-z0-9]{30}/)) {
			try{
				httpGet(params){response ->
					if(response.status != 200) {
						log.error "Received HTTP error ${response.status}. Check your keys!"
					}
					else {
						if(type=="deviceList"){
							if(logEnable) log.debug "Device list generated"
							deviceOptions = response.data.records.deviceName
							if(logEnable) log.debug "Device List: ${deviceOptions}"
						}
						else {
							if(logEnable) log.debug "Keys validated"
							validated = true
						}
					}
				}
			}
			catch (Exception e) {
				log.error "An invalid key was probably entered. Join API Server Returned: ${e}"
			} 
		}
		else {
			log.error "API key '${apikey}' is not properly formatted!"
		}
		if(type=="deviceList") return deviceOptions
		return validated
	}
}

def speak(message) {
    if (deviceNames) { log.info "Sending Speech Request: '${message}' to Device: $deviceNames"}
	
	if(deviceNames && deviceNames instanceof List) {
		deviceNames = deviceNames.join(',')
	}
    def apiParams = ["apikey":apikey,"deviceNames":deviceNames, "say":message]
	
    def params = [
        uri: "https://joinjoaomgcd.appspot.com/_ah/api/messaging/v1/sendPush?",
		requestContentType: 'application/json',
		contentType: 'application/json',
		body : apiParams
    ]
	
	if(logEnable) log.debug "Speak params: ${params}"
  	
    if ((apikey =~ /[A-Za-z0-9]{30}/)) {
    	asynchttpPost('myPostResponse', params)
  	}
  	else {
    	log.error "API key '${apikey}' is not properly formatted!"
    }
}

def deviceNotification(message) {
	if(message.startsWith("[") || message.endsWith("]")|| message.contains("][")){
		log.warn "Improperly formatted message!"
		return
	}
	
	def apiParams = buildMessage(message)
	if(logEnable) log.debug "Merged Settings: " + apiParams
	if (apiParams.deviceNames) { log.info "Sending Message: '${apiParams.text}' to Device: $apiParams.deviceNames"}

	if(apiParams.deviceNames && apiParams.deviceNames instanceof List) {	//if multiple devices selected, convert list to string
		apiParams.deviceNames = apiParams.deviceNames.join(',')
	}
	
    def params = [
        uri: "https://joinjoaomgcd.appspot.com/_ah/api/messaging/v1/sendPush?",
		requestContentType: 'application/json',
		contentType: 'application/json',
		body : apiParams
    ]
	
    if(logEnable) log.debug params

    if ((apikey =~ /[A-Za-z0-9]{30}/)) {
		asynchttpPost('myPostResponse', params)
  	}
  	else {
    	log.error "API key '${apikey}' is not properly formatted!"
    }
}

def myPostResponse(response,data){
	if(response.status != 200) {
		log.error "Received HTTP error ${response.status}. Check your keys!"
	}
    else {
    	if(logEnable) log.debug "Message Received by Join API Server"
    }
}

def buildMessage(message){
	def current = settings
	if(logEnable) log.debug "Settings: " + current
	def currentList = message.tokenize("[]")	//separate complete message into a list with shortcut,value
	if(logEnable) log.debug "Current Split List: " + currentList
	def currentSize = currentList.size()
	def myMap = [:]
	myMap["text"] = currentList[0]
	if(currentSize > 1){
		def count = 1
		def totalPairs = currentSize / 2	//get the total number of key value pairs to create
		for (i in 1..totalPairs){
			def myKey = getPrefName()[currentList[count]]	//set key to actual preference name :  eg. A=actions or D=deviceNames
			def myVal = currentList[count+1]
			myMap[myKey] = myVal
			count = count + 2
		}
	}
	if(logEnable) log.debug "Custom Settings: " + myMap
	current << myMap	//merge driver settings with custom setting
	if(current.smsnumber) current["smstext"] = myMap.text	//when an smsnumber is included, set smstext to message
	current.remove("logEnable")	//remove uneeded preferences
	
	if(current.actions){	//format actions using ||| delimiter as per api
		log.info current.actions
		log.info current.actions.replace(",", "|||")
		current["actions"] = current.actions.replace(",", "|||")
	}
	return current
}

def getPrefName(){	//convert inline shortcut to preference name
	return [
		"D":"deviceNames",
		"T":"title",
		"C":"category",
		"A":"actions",
		"S":"smsnumber"
	]
}
