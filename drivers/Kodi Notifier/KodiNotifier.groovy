/**
*  Kodi Notifier
*
*
*  2019 Stephan Hackett
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
* This driver uses sections of code derived from the original Kodi Media Center driver developed by @josh (Josh Lyon)
*
*
*
*
*/

def version() {"v1.0.20190621"}

preferences {
	input("ip", "text", title: "Kodi Server Ip Address", description: "", required:true)
    input("port", "text", title: "Kodi Server Port", description: "", required:true)
    input("title", "text", title: "Notification Title", description: "", required:true)
    input("username", "text", title: "Kodi Server Username", description: "")
    input("password", "text", title: "Kodi Server Password", description: "")
    input("image", "text", title: "Image", description: "")
    input("displaytime", "number", title: "Notification Timeout(seconds)", description: "")
    input("logEnable", "bool", title: "Enable Debug Logging?:", required: true)

    
}

metadata {
  	definition (name: "Kodi Notifier", namespace: "stephack", author: "Stephan Hackett", importUrl: "https://raw.githubusercontent.com/stephack/Hubitat/master/drivers/Kodi%20Notifier/KodiNotifier.groovy") {
    	capability "Notification"
    	capability "Actuator"
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

def deviceNotification(message){
    
    //BUILD PARAMS
    def myParams = [
        "title":title,
        "message":message
    ]
    if(image) myParams.put("image", image)
    if(displaytime) myParams.put("displaytime", displaytime*1000)
    
    //BUILD BODY
    def content = [
       "jsonrpc":"2.0",
        "method":"GUI.ShowNotification",
        "params": myParams,
        "id":1
    ]
    
    //BUILD HEADER    
    def myHeaders = [
        "HOST": ip + ":" + port,
        "Content-Type":"application/json"
    ]
    if(username){
    	def pair ="$username:$password"
        def basicAuth = pair.bytes.encodeBase64();
    	myHeaders.put("Authorization", "Basic " + basicAuth )
    }
    
    //SEND NOTIFICATION
    def result = new hubitat.device.HubAction(
        method: "POST",
        path: "/jsonrpc",
        body: content,
        headers: myHeaders
	)
    
    result
}

def parse(String description) {
   // log.debug description
}
