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
*
*/

def version() {"v1.0.20190603a"}

preferences {
    input("apiKey", "text", title: "Join API Key:", description: "")
    if(getValidated()){
  		input("deviceName", "enum", title: "Select Device:", description: "", multiple: true, required: false, options: getValidated("deviceList"))
        input("myTitle", "text", title: "Notification Title:", description: "")
		input("myImage", "text", title: "Icon URI:", description: "(Image to be displayed in notification as well as status bar)")
  		input("url", "text", title: "URL:", description: "(URL to be opened when Notification is clicked)")
		input("sound", "text", title: "Sound URI:", description: "(URL of notification sound to be played)")
		input("image", "text", title: "Image URI:", description: "(URL of image to be displayed in the notification body)")
		input("myApp", "text", title: "Open App by Name:", description: "(Name of Android app to open)")
		input("myPackage", "text", title: "Open App by Package:", description: "(Name of Android Package to open)")
		input("smsnumber", "number", title: "Phone # to send SMS text TO:", description: "(Text will be sent FROM the Join Device selected above)")
		input("action", "text", title: "Actions for Notification:", description: "(separate multiple actions with commas)")
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
	state.devices = deviceName

}

def getValidated(type){
	if(apiKey){
		if(type=="deviceList" && logEnable){log.debug "Generating Device List..."}
		else if(logEnable) log.debug "Validating Key..."

		def validated = false

		def params = [
			uri: "https://joinjoaomgcd.appspot.com/_ah/api/registration/v1/listDevices?apikey=${apiKey}",
		]
		if(logEnable) log.debug "Validation params: ${params}"

		if ((apiKey =~ /[A-Za-z0-9]{30}/)) {
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
			log.error "API key '${apiKey}' is not properly formatted!"
		}
		if(type=="deviceList") return deviceOptions
		return validated
	}
}

def speak(message) {
    if (deviceName) { log.info "Sending Speech Request: ${message} to Device: $deviceName"}

    def apiUri =  "https://joinjoaomgcd.appspot.com/_ah/api/messaging/v1/sendPush?apikey=${apiKey}"
    def apiParams = ""
	
	if(deviceName && deviceName instanceof List) {
		deviceName = deviceName.join(',')
	}
	
    if(deviceName) apiParams += "&deviceNames=" + URLEncoder.encode(deviceName, "UTF-8")
    if(message) apiParams += "&say=" + URLEncoder.encode(message, "UTF-8")
    
    def params = [
        uri: apiUri + apiParams,
    ]
	if(logEnable) log.debug "Speak params: ${params}"
  	
    if ((apiKey =~ /[A-Za-z0-9]{30}/)) {
    	httpGet(params){response ->
      		if(response.status != 200) {
        		log.error "Received HTTP error ${response.status}. Check your keys!"
      		}
      		else {
        		if(logEnable) log.debug "Message Received by Join API Server"
      		}
    	}
  	}
  	else {
    	log.error "API key '${apiKey}' is not properly formatted!"
    }
}

def deviceNotification(message) {
	def actionInline = ""
	if(message.contains("[A]")) {
		def multiMessage = message.split("\\[A\\]")
		message = multiMessage[0]
		actionInline = multiMessage[1]
	}
	   
	if (deviceName) { log.info "Sending Message: ${message} to Device: $deviceName"}

    def apiUri =  "https://joinjoaomgcd.appspot.com/_ah/api/messaging/v1/sendPush?apikey=${apiKey}"
    def apiParams = ""
	
	if(deviceName && deviceName instanceof List) {
		deviceName = deviceName.join(',')
	}
	
	if(deviceName) apiParams += "&deviceNames=${deviceName}"
    if(myTitle) apiParams += "&title=" + URLEncoder.encode(myTitle, "UTF-8")
    if(message) apiParams += "&text=" + URLEncoder.encode(message, "UTF-8")
    if(myImage) apiParams += "&icon=" + URLEncoder.encode(myImage, "UTF-8")
    if(url) apiParams += "&url=" + URLEncoder.encode(url, "UTF-8")
	
	if(sound) apiParams += "&sound=" + URLEncoder.encode(sound, "UTF-8")
    if(image) apiParams += "&image=" + URLEncoder.encode(image, "UTF-8")
    if(myApp) apiParams += "&app=" + URLEncoder.encode(myApp, "UTF-8")
	if(myPackage) apiParams += "&appPackage=" + URLEncoder.encode(myPackage, "UTF-8")
	if(smsnumber) apiParams += "&smsnumber=" + smsnumber + "&smstext=" + URLEncoder.encode(message, "UTF-8")
	
	//Inline action overide actions listed in the preferences
	if(actionInline != ""){
		if(logEnable) log.debug "Inline actions found. Over-riding driver preferences."
		action = actionInline.replace(",", "|||")
		apiParams += "&actions=" + URLEncoder.encode(action, "UTF-8")
	}	   
	else if(action){
		action = action.replace(",", "|||")
		apiParams += "&actions=" + URLEncoder.encode(action, "UTF-8")
	}
    
    def params = [
        uri: apiUri + apiParams,
    ]
    if(logEnable) log.debug params
  	
    if ((apiKey =~ /[A-Za-z0-9]{30}/)) {
    	httpGet(params){response ->
      		if(response.status != 200) {
        		log.error "Received HTTP error ${response.status}. Check your keys!"
      		}
      		else {
        		if(logEnable) log.debug "Message Received by Join API Server"
      		}
    	}
  	}
  	else {
    	log.error "API key '${apiKey}' is not properly formatted!"
    }
}
