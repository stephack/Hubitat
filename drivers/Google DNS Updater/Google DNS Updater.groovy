/**
 *	Import URL: https://raw.githubusercontent.com/stephack/Hubitat/master/drivers/Google%20DNS%20Updater/Google%20DNS%20Updater.groovy
 *
 *  Google DNS Updater
 *
 *  by Stephan Hackett
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
 * 
 */

def version() {"v1.1.20190126"}

metadata {
	definition (name: "Google DNS Updater", namespace: "stephack", author: "Stephan Hackett") {
        capability "Switch"
        capability "Momentary"
		
		attribute "ip", "string"
		
		command "manualUpdate", ["IP"]
	}
	preferences {
		input "username", "string", title: "Username:", required: true
		input "password", "string", title: "Password:", required: true
		input "hostname", "string", title: "Domain(Hostname):", required: true
		input "upInterval", "enum", title: "IP Check/Update Interval (minutes):", options: ["1Minute":"1 Minute", "5Minutes":"5 Minutes", "10Minutes":"10 Minutes", "15Minutes":"15 Minutes", "30Minutes":"30 Minutes", "1Hour":"1 Hour", "3Hours":"3 Hours"]
		input "logEnable", "bool", title: "Enable Debug Logging?"
	}
}

def parse(String description) {
	log.debug(description)
}

def push() {
    sendEvent(name: "switch", value: "on")
	sendEvent(name: "momentary", value: "pushed", isStateChange: true)
    runIn(1, toggleMom)
	getIp()
}

def toggleMom() {
    sendEvent(name: "switch", value: "off")
}

def on() {
	push()
}

def off() {
	push()
}

def installed(){
	initialize()
}

def updated(){
	initialize()
}

def initialize(){
	unschedule()
	if(upInterval){
		"runEvery${upInterval}"(getIp)
	}
}

def manualUpdate(ip){
	updateGoogle(ip)
}

def getIp(){
	def params = [
        uri: "https://domains.google.com/checkip",
        contentType: "text/html",
        requestContentType: "text/html",
    ]
	try{
		httpGet(params){response ->
			if(response.status != 200) {
				log.warn "Did not received valid data from IP check!"
			}
			else {
				if(logEnable) log.debug "Received IP: " + response.data
				if(response.data==device.currentValue("ip")){
					if(logEnable) log.debug "Ip has not changed. No need to send update request"
				}
				else updateGoogle(response.data)
			}
		}
	}
	catch (Exception e) {
        log.debug "HttpGet Error: ${e}"
	}  
}

def updateGoogle(myIp){
	if(logEnable) log.debug "Post URL: https://${username}:${password}@domains.google.com/nic/update?hostname=${hostname}&myip=${myIp}"
	def params = [
		uri: "https://${username}:${password}@domains.google.com/nic/update?hostname=${hostname}&myip=${myIp}",
        contentType: "text/html",
        requestContentType: "text/html",
    ]
	try{
		httpPost(params){response ->
			if(logEnable) log.debug "Response Status: " + response.status
			if(logEnable) log.debug "Response Data: " + response.data

			def responseCode = "${response.data}"
			if(response.status != 200) log.error "Invalid URL or Unable to contact domains.google.com - Enable Debug logging for details!"
			else if(responseCode.contains("good")){
				log.info "Ip successfully updated to: " + myIp
				sendEvent(name: "ip", value: myIp)
			}
			else if(responseCode.contains("nochg")) log.warn "The supplied IP address is already set for this host. You should not attempt another update until your IP address changes."
			else if(responseCode=="nohost")	log.warn "The hostname does not exist, or does not have Dynamic DNS enabled."
			else if(responseCode=="badauth") log.warn "The username / password combination is not valid for the specified host."
			else if(responseCode=="notfqdn") log.warn "The supplied hostname is not a valid fully-qualified domain name."
			else if(responseCode=="badagent") log.warn "Your Dynamic DNS client is making bad requests. Ensure the user agent is set in the request, and that you’re only attempting to set an IPv4 address. IPv6 is not supported."
			else if(responseCode=="abuse") log.warn "Dynamic DNS access for the hostname has been blocked due to failure to interpret previous responses correctly."
			else if(responseCode=="911") log.warn "An error happened on our end. Wait 5 minutes and retry."
		}
	}
	catch (Exception e) {
        log.debug "HttpPost Error: ${e}"
	} 
}
