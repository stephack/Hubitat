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
def version() {"v1.0.20180411"}

preferences {
    input("apiKey", "text", title: "API Key:", description: "Join API Key")
    if(getValidated()){
  		input("deviceName", "enum", title: "Select Device:", description: "", multiple: true, required: false, options: getValidated("deviceList"))
        input("myTitle", "text", title: "Notification Title:", description: "")
  		//input("priority", "enum", title: "Default Message Priority (Blank = NORMAL):", description: "", defaultValue: "0", options:[["-1":"LOW"], ["0":"NORMAL"], ["1":"HIGH"]])
  		//input("sound", "enum", title: "Notification Sound (Blank = App Default):", description: "", options: getSoundOptions())
        input("myImage", "text", title: "Image/Icon URL:", description: "Image will be displayed in notification as well as status bar (transparent png preferred)")
  		input("url", "text", title: "URL:", description: "URL is opened when Notification is clicked:")
    }
}

metadata {
  	definition (name: "Join API Device", namespace: "stephack", author: "Stephan Hackett") {
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
}

def getValidated(type){
    if(type=="deviceList"){log.debug "Generating Device List..."}
	else {log.debug "Validating Key..."}
    
    def validated = false
	
    def params = [
        uri: "https://joinjoaomgcd.appspot.com/_ah/api/registration/v1/listDevices?apikey=${apiKey}",
  	]
    //log.info params
    
    if ((apiKey =~ /[A-Za-z0-9]{30}/)) {
        try{
        	httpGet(params){response ->
      			if(response.status != 200) {
        			log.error "Received HTTP error ${response.status}. Check your keys!"
      			}
      			else {
                    if(type=="deviceList"){
                        log.debug "Device list generated"
                        deviceOptions = response.data.records.deviceName
                        //log.info deviceOptions
                    }
                    else {
                        log.debug "Keys validated"
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

def speak(message) {
    deviceNotification(message)
}

def deviceNotification(message) {
	
  	if (deviceName) { log.debug "Sending Message: ${message} to Device: $deviceName"}
  	//else {log.debug "Sending Message: [${message}] Priority: [${priority}] to [All Devices]"}

    def apiUri =  "https://joinjoaomgcd.appspot.com/_ah/api/messaging/v1/sendPush?apikey=${apiKey}"
    def apiParams = ""
    if(deviceName) apiParams += "&deviceNames=" + URLEncoder.encode(deviceName, "UTF-8")
    if(myTitle) apiParams += "&title=" + URLEncoder.encode(myTitle, "UTF-8")
    if(message) apiParams += "&text=" + URLEncoder.encode(message, "UTF-8")
    if(myImage) apiParams += "&icon=" + URLEncoder.encode(myImage, "UTF-8")
    if(url) apiParams += "&url=" + URLEncoder.encode(url, "UTF-8")
    
    def params = [
        uri: apiUri + apiParams,
    ]
    //log.info params
  	
    if ((apiKey =~ /[A-Za-z0-9]{30}/)) {
    	httpGet(params){response ->
      		if(response.status != 200) {
        		log.error "Received HTTP error ${response.status}. Check your keys!"
      		}
      		else {
        		log.debug "Message Received by Join API Server"
      		}
    	}
  	}
  	else {
    	log.error "API key '${apiKey}' is not properly formatted!"
    }
}
