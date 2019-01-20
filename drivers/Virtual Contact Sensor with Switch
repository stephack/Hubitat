/*
 * Virtual Contact Sensor with Switch
 *
 * Created by Stephan Hackett
 * 
 */

metadata {
    definition (name: "Virtual Contact Sensor with Switch", namespace: "stephack", author: "Stephan Hackett") {
		capability "Sensor"
        capability "Contact Sensor"
        capability "Switch"
		
		command "open"
		command "close"
    }
	preferences {
        input name: "reversed", type: "bool", title: "Reverse Action"
	}
}

def open(){
	sendEvent(name: "contact", value: "open")
	if(reversed) switchVal = "off"
	else switchVal = "on"
	sendEvent(name: "switch", value: switchVal)
}

def close(){
	sendEvent(name: "contact", value: "closed")
	if(reversed) switchVal = "on"
	else switchVal = "off"
	sendEvent(name: "switch", value: switchVal)
}

def on(){
    sendEvent(name: "switch", value: "on")
	if(reversed==true) contactVal = "closed"
	else contactVal = "open"
	sendEvent(name: "contact", value: contactVal)
}

def off(){
    sendEvent(name: "switch", value: "off")
	if(reversed==true) contactVal = "open"
	else contactVal = "closed"
	sendEvent(name: "contact", value: contactVal)
}

def installed(){
	initialize()
}

def updated(){
	initialize()
}

def initialize(){
	sendEvent(name: "switch", value: "off")
	sendEvent(name: "contact", value: "closed")
	
}
