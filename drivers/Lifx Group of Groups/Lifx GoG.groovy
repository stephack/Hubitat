/**
 *  LIFX Group of Groups
 *
 *  Copyright 2016 ericvitale@gmail.com
 *  Edited and Upgraded for Hubitat by Stephan Hackett
 *
 *  Version 1.3.6 - Added more activity feed filtering. (10/9/2017) 
 *  Version 1.3.5 - Reduced activity feed chatter, also added a setting to disable on/off & setLevel messages. (10/8/2017)
 *  Version 1.3.4 - Fixed looping issue with retrying when lights are offline. (07/30/2017)
 *  Version 1.3.3 - Cleaned up a bit. (06/30/2017)
 *  Version 1.3.2 - Added the ability to use separate durations for on/off and setLevel commands. (06/26/2017)
 *  Version 1.3.1 - Added setLevelAndTemperature method to allow webCoRE set both with a single command. (06/25/2017)
 *  Version 1.3.0 - Updated to use the ST Beta Asynchronous API. (06/22/17)
 *  Version 1.2.5 - Added the apiFlash() methiod. apiFlash(cycles=5, period=0.5, brightness1=1.0, brightness2=0.0) (06/16/2017)
 *  Version 1.2.4 - Added saturation:0 to setColorTemperature per LIFX's recommendation. (05/22/2017)
 *  Version 1.2.3 - Fixed an issue with setColor() introduced by an api change. (05/19/2017)
 *  Version 1.2.2 - Fixed a bug introduced in version 1.2.1. (05/18/2017)
 *  Version 1.2.1 - Fixed an issue with poll not sending the correct group list to LIFX, they must have changed the api. (05/18/2017)
 *  Version 1.2.0 - Added the ability to execute the pulse and breathe command via CoRE or webCoRE using runEffect(effect="pulse", color="blue", from_color="red", cycles=5, period=0.5). (05/13/2017)
 *  Version 1.1.9 - Added custom command for runEffect and the ability to use "all" as a group. (05/07/2017)
 *  Version 1.1.8 - Added the power meter ability. (12/15/2016)
 *  Version 1.1.7 - Added the ability to sync with other groups using the LIFX Sync companion app. (11/8/2016)
 *  Version 1.1.6 - Added support for setLevel(level, duration), setHue, setSaturation. (10/05/2016)
 *  Version 1.1.5 - Changed lower end of color temperature from 2700K to 2500K per the LIFX spec.
 *  Version 1.1.4 - Further updated setLevel(...). No longer sends on command so that lights go to level immediatly and 
 *    not to the previous level. Same for color and color temperature. (07/29/2016)
 *  Version 1.1.3 - Updated setLevel(...) to be a bit more efficient and to prevent a possible but unlikely 
 *    NullPointerException. (07/16/2016)
 *  Version 1.1.2 - Added these version numbers (07/15/2016)
 *  Version 1.1.1 - Updated auto frequency to accept numbers only 1..* (07/09/2016)
 *  Version 1.1.0 - Added auto refresh (07/09/2016)
 *  Version 1.0.0 - Initial Release (07/06/2016)
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
 *  You can find the latest version of this device handler @ https://github.com/ericvitale/ST-LIFX-Group-of-Groups
 *  You can find the companion LIFX Sync app @ https://github.com/ericvitale/ST-LIFX-Sync
 *  You can find my other device handlers & SmartApps @ https://github.com/ericvitale
 *
 **/
 
//include 'asynchttp_v1'

import java.text.DecimalFormat;
def version(){return "v1.0.20181029"}

metadata {
    definition (name: "LIFX Group of Groups", namespace: "stephack", author: "Eric Vitale & Stephan Hackett") {
        capability "Polling"
        capability "Switch"
        capability "Switch Level"
        capability "Color Control"
        capability "Refresh"
		capability "Color Temperature"
		capability "Actuator"
        capability "Sensor"
        
        command "refresh"
        command "runEffect"
        command "apiFlash"
        command "apiBreathe"
        command "setState", ["MAP"]
        
        attribute "colorName", "string"
        attribute "lightStatus", "string"
    }
    
    preferences {
    	input "token", "text", title: "API Token", required: true       
        input "group01", "text", title: "Group 1", required: true
        input "group02", "text", title: "Group 2", required: false
        input "group03", "text", title: "Group 3", required: false
        input "group04", "text", title: "Group 4", required: false
        input "group05", "text", title: "Group 5", required: false
        input "group06", "text", title: "Group 6", required: false
        input "group07", "text", title: "Group 7", required: false
        input "group08", "text", title: "Group 8", required: false
        input "group09", "text", title: "Group 9", required: false
        input "group10", "text", title: "Group 10", required: false
       
       	input "defaultTransition", "decimal", title: "Level Transition Time (s)", required: true, defaultValue: 0.0
        input "defaultStateTransition", "decimal", title: "On/Off Transition Time (s)", required: true, defaultValue: 0.0
        input "useActLog", "bool", title: "On/Off/Level Act. Feed", required: true, defaultValue: true
        input "useActLogDebug", "bool", title: "Debug Act. Feed", required: true, defaultValue: false
        input "logging", "enum", title: "Log Level", required: false, defaultValue: "INFO", options: ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"]
    }
}

def installed() {
	initialize()
}

def updated() {
	initialize()
}

def refresh() {
    poll()
}

def initialize() {
	log("Initializing...", "DEBUG")
    getGroups(true)
    setupSchedule()
    setDefaultTransitionDuration(defaultTransition)
    setDefaultStateTransitionDuration(defaultStateTransition)
    setUseActivityLog(useActLog)
    setUseActivityLogDebug(useActLogDebug)
}

def buildGroupList() {

	def groups = ""
	
    try {
        
        if(group01.toUpperCase() == "ALL") {
        	groups = "all"
            return groups
        } else {
	        groups = "group:" + group01
        }
        
        if(group02 != null) {
        	groups = groups + ",group:" + group02
        }
        
        if(group03 != null) {
            groups = groups + ",group:" + group03
        }
        
        if(group04 != null) {
			groups = groups + ",group:" + group04
        }
        
        if(group05 != null) {
            groups = groups + ",group:" + group05
        }
                
        if(group06 != null) {
            groups = groups + ",group:" + group06
        }
        
        if(group07 != null) {
            groups = groups + ",group:" + group07
        }
        
        if(group08 != null) {
            groups = groups + ",group:" + group08
        }
        
        if(group09 != null) {
            groups = groups + ",group:" + group09
        }
        
        if(group10 != null) {
            groups = groups + ",group:" + group10
        }
        
        return groups
    } catch(e) {
    	log(e, "ERROR")
        return ""
    }
}

def getGroups(refresh=false) {
	if(refresh || state.groups == null || state.groups == "") {
    	state.groups = buildGroupList()
    }
	return state.groups
}

private determineLogLevel(data) {
    switch (data?.toUpperCase()) {
        case "TRACE":
            return 0
            break
        case "DEBUG":
            return 1
            break
        case "INFO":
            return 2
            break
        case "WARN":
            return 3
            break
        case "ERROR":
        	return 4
            break
        default:
            return 1
    }
}

def log(data, type) {
    data = "LIFX-GoG -- ${device.label} -- ${data ?: ''}"
        
    if (determineLogLevel(type) >= determineLogLevel(settings?.logging ?: "INFO")) {
        switch (type?.toUpperCase()) {
            case "TRACE":
                log.trace "${data}"
                break
            case "DEBUG":
                log.debug "${data}"
                break
            case "INFO":
                log.info "${data}"
                break
            case "WARN":
                log.warn "${data}"
                break
            case "ERROR":
                log.error "${data}"
                break
            default:
                log.error "LIFX-GoG -- ${device.label} -- Invalid Log Setting"
        }
    }
}

def syncOn() {
	sendEvent(name: "switch", value: "on", displayed: getUseActivityLog(), data: [syncing: "true"])
}

def on(duration=getDefaultStateTransitionDuration()) {
	log("Turning on...", "INFO")
    sendLIFXCommand(["power" : "on", "duration" : duration])
    sendEvent(name: "switch", value: "on", displayed: getUseActivityLog(), data: [syncing: "false"])
    sendEvent(name: "level", value: "${state.level}", displayed: getUseActivityLog())
}

def off(duration=getDefaultStateTransitionDuration(), sync=false) {
	log("Turning off...", "INFO")
    sendLIFXCommand(["power" : "off", "duration" : duration])
    sendEvent(name: "switch", value: "off", displayed: getUseActivityLog(), data: [syncing: "false"])
}

def setLevel(level, duration=getDefaultTransitionDuration()) {
	log("Begin setting groups level to ${value} over ${duration} seconds.", "DEBUG")
    if (level > 100) {
		level = 100
	} else if (level <= 0 || level == null) {
		sendEvent(name: "level", value: 0)
		return off()
	}
    state.level = level
	sendEvent(name: "level", value: level, displayed: getUseActivityLog())
    sendEvent(name: "switch", value: "on", displayed: false)
    def brightness = level / 100
    sendLIFXCommand(["brightness": brightness, "power": "on", "duration" : duration])
}

def setColor(value) {
	log("Begin setting groups color to ${value}.", "DEBUG")
    def hue = value.hue.toInteger() * 3.6
    def sat = value.saturation.toInteger() / 100
    def level = value.level.toInteger()
    if(level) setLevel(level)
    sendLIFXCommand([color: "saturation:${sat} hue:${hue}"])
    sendEvent(name: "hue", value: value.hue, displayed: getUseActivityLogDebug())
    sendEvent(name: "saturation", value: value.saturation, displayed: getUseActivityLogDebug())
    sendEvent(name: "color", value: value.hex, displayed: getUseActivityLogDebug())
    sendEvent(name: "switch", value: "on", displayed: getUseActivityLogDebug())
    sendEvent(name: "level", value: "${state.level}", displayed: getUseActivityLogDebug())
}

def setColorTemperature(value) {
	log("Begin setting groups color temperature to ${value}.", "DEBUG")
    if(value < 2500) {
    	value = 2500
    } else if(value > 9000) {
    	value = 9000
    }
    sendLIFXCommand([color: "kelvin:${value} saturation:0"])
	sendEvent(name: "colorTemperature", value: value, displayed: getUseActivityLogDebug())
	sendEvent(name: "color", value: "#ffffff", displayed: getUseActivityLogDebug())
	sendEvent(name: "saturation", value: 0, displayed: getUseActivityLogDebug())
    sendEvent(name: "level", value: "${state.level}", displayed: getUseActivityLogDebug())
}

def setHue(val) {
	log("Begin setting groups hue to ${val} (converted for Lifx to ${val * 3.6}).", "DEBUG")
    sendLIFXCommand([color: "hue:${val * 3.6}"])
    sendEvent(name: "hue", value: val, displayed: getUseActivityLogDebug())
    sendEvent(name: "switch", value: "on", displayed: getUseActivityLogDebug())
    sendEvent(name: "level", value: "${state.level}", displayed: getUseActivityLogDebug())
}

def setSaturation(val) {
	log("Begin setting groups saturation to ${val} (converted for Lifx to ${val /100}).", "DEBUG")
    sendLIFXCommand([color: "saturation:${val / 100}"])
    sendEvent(name: "saturation", value: val, displayed: getUseActivityLogDebug())
    sendEvent(name: "switch", value: "on", displayed: getUseActivityLogDebug())
    sendEvent(name: "level", value: "${state.level}", displayed: getUseActivityLogDebug())
}

def setState(value) {
    def myStateMap = stringToMap(value)
    def power = myStateMap.power
    def color = myStateMap.color
    def brightness = myStateMap.level
    if(brightness) brightness = brightness.toInteger()/100
    def duration = myStateMap.duration
    if(color == "random") {
        def clSize = colorList().size()
    	def myColor = getRandom(clSize)
        log.info "Setting random color: ${myColor.name}"
        color = ["hue":myColor.hue, "saturation":myColor.sat/100, "brightness":myColor.lvl/100]
        brightness = null
        log.info color
    }
    sendLIFXCommand(["power" : power, "color" : color, "brightness": brightness, "duration" : duration])
    sendEvent(name: "switch", value: "${power}", displayed: getUseActivityLogDebug())
}

def getRandom(clSize){
    //if(!state.newRandom) state.newRandom = 1
    //state.oldRandom = state.newRandom
    def tempRandom = Math.abs(new Random().nextInt() % clSize)
    def colorInfo = colorList()[tempRandom]
    
    //def randDiff = 1
    //def randVariance = 15
    //while(randDiff.abs() < randVariance){
    //    tempRandom = Math.abs(new Random().nextInt() % 100) + 1
    //    randDiff = tempRandom - state.oldRandom
    //    log.info tempRandom + "/" + randDiff
    //}
    //state.newRandom = tempRandom
    return colorInfo //["hue":colorInfo.hue, "saturation":colorInfo.sat, "brightness":colorInfo.lvl]
}

def colorList() {
	def colorMap =
    	[
        [name:"Alice Blue", rgb:"#F0F8FF", hue:208, sat:100, lvl:97],
        [name:"Antique White", rgb:"#FAEBD7", hue:34, sat:78, lvl:91],
        [name:"Aqua", rgb:"#00FFFF", hue:180, sat:100, lvl:50],
        [name:"Aquamarine", rgb:"#7FFFD4", hue:160, sat:100, lvl:75],
        [name:"Azure", rgb:"#F0FFFF", hue:180, sat:100, lvl:97],
        [name:"Beige", rgb:"#F5F5DC", hue:60, sat:56, lvl:91],
        [name:"Bisque", rgb:"#FFE4C4", hue:33, sat:100, lvl:88],
        [name:"Blanched Almond", rgb:"#FFEBCD", hue:36, sat:100, lvl:90],
        [name:"Blue", rgb:"#0000FF", hue:240, sat:100, lvl:50],
        [name:"Blue Violet", rgb:"#8A2BE2", hue:271, sat:76, lvl:53],
        [name:"Brown", rgb:"#A52A2A", hue:0, sat:59, lvl:41],
        [name:"Burly Wood", rgb:"#DEB887", hue:34, sat:57, lvl:70],
        [name:"Cadet Blue", rgb:"#5F9EA0", hue:182, sat:25, lvl:50],
        [name:"Chartreuse", rgb:"#7FFF00", hue:90, sat:100, lvl:50],
        [name:"Chocolate", rgb:"#D2691E", hue:25, sat:75, lvl:47],
        [name:"Cool White", rgb:"#F3F6F7", hue:187, sat:19, lvl:96],
        [name:"Coral", rgb:"#FF7F50", hue:16, sat:100, lvl:66],
        [name:"Corn Flower Blue", rgb:"#6495ED", hue:219, sat:79, lvl:66],
        [name:"Corn Silk", rgb:"#FFF8DC", hue:48, sat:100, lvl:93],
        [name:"Crimson", rgb:"#DC143C", hue:348, sat:83, lvl:58],
        [name:"Cyan", rgb:"#00FFFF", hue:180, sat:100, lvl:50],
        [name:"Dark Blue", rgb:"#00008B", hue:240, sat:100, lvl:27],
        [name:"Dark Cyan", rgb:"#008B8B", hue:180, sat:100, lvl:27],
        [name:"Dark Golden Rod", rgb:"#B8860B", hue:43, sat:89, lvl:38],
        [name:"Dark Gray", rgb:"#A9A9A9", hue:0, sat:0, lvl:66],
        [name:"Dark Green", rgb:"#006400", hue:120, sat:100, lvl:20],
        [name:"Dark Khaki", rgb:"#BDB76B", hue:56, sat:38, lvl:58],
        [name:"Dark Magenta", rgb:"#8B008B", hue:300, sat:100, lvl:27],
        [name:"Dark Olive Green", rgb:"#556B2F", hue:82, sat:39, lvl:30],
        [name:"Dark Orange", rgb:"#FF8C00", hue:33, sat:100, lvl:50],
        [name:"Dark Orchid", rgb:"#9932CC", hue:280, sat:61, lvl:50],
        [name:"Dark Red", rgb:"#8B0000", hue:0, sat:100, lvl:27],
        [name:"Dark Salmon", rgb:"#E9967A", hue:15, sat:72, lvl:70],
        [name:"Dark Sea Green", rgb:"#8FBC8F", hue:120, sat:25, lvl:65],
        [name:"Dark Slate Blue", rgb:"#483D8B", hue:248, sat:39, lvl:39],
        [name:"Dark Slate Gray", rgb:"#2F4F4F", hue:180, sat:25, lvl:25],
        [name:"Dark Turquoise", rgb:"#00CED1", hue:181, sat:100, lvl:41],
        [name:"Dark Violet", rgb:"#9400D3", hue:282, sat:100, lvl:41],
        [name:"Daylight White", rgb:"#CEF4FD", hue:191, sat:9, lvl:90],
        [name:"Deep Pink", rgb:"#FF1493", hue:328, sat:100, lvl:54],
        [name:"Deep Sky Blue", rgb:"#00BFFF", hue:195, sat:100, lvl:50],
        [name:"Dim Gray", rgb:"#696969", hue:0, sat:0, lvl:41],
        [name:"Dodger Blue", rgb:"#1E90FF", hue:210, sat:100, lvl:56],
        [name:"Fire Brick", rgb:"#B22222", hue:0, sat:68, lvl:42],
        [name:"Floral White", rgb:"#FFFAF0", hue:40, sat:100, lvl:97],
        [name:"Forest Green", rgb:"#228B22", hue:120, sat:61, lvl:34],
        [name:"Fuchsia", rgb:"#FF00FF", hue:300, sat:100, lvl:50],
        [name:"Gainsboro", rgb:"#DCDCDC", hue:0, sat:0, lvl:86],
        [name:"Ghost White", rgb:"#F8F8FF", hue:240, sat:100, lvl:99],
        [name:"Gold", rgb:"#FFD700", hue:51, sat:100, lvl:50],
        [name:"Golden Rod", rgb:"#DAA520", hue:43, sat:74, lvl:49],
        [name:"Gray", rgb:"#808080", hue:0, sat:0, lvl:50],
        [name:"Green", rgb:"#008000", hue:120, sat:100, lvl:25],
        [name:"Green Yellow", rgb:"#ADFF2F", hue:84, sat:100, lvl:59],
        [name:"Honeydew", rgb:"#F0FFF0", hue:120, sat:100, lvl:97],
        [name:"Hot Pink", rgb:"#FF69B4", hue:330, sat:100, lvl:71],
        [name:"Indian Red", rgb:"#CD5C5C", hue:0, sat:53, lvl:58],
        [name:"Indigo", rgb:"#4B0082", hue:275, sat:100, lvl:25],
        [name:"Ivory", rgb:"#FFFFF0", hue:60, sat:100, lvl:97],
        [name:"Khaki", rgb:"#F0E68C", hue:54, sat:77, lvl:75],
        [name:"Lavender", rgb:"#E6E6FA", hue:240, sat:67, lvl:94],
        [name:"Lavender Blush", rgb:"#FFF0F5", hue:340, sat:100, lvl:97],
        [name:"Lawn Green", rgb:"#7CFC00", hue:90, sat:100, lvl:49],
        [name:"Lemon Chiffon", rgb:"#FFFACD", hue:54, sat:100, lvl:90],
        [name:"Light Blue", rgb:"#ADD8E6", hue:195, sat:53, lvl:79],
        [name:"Light Coral", rgb:"#F08080", hue:0, sat:79, lvl:72],
        [name:"Light Cyan", rgb:"#E0FFFF", hue:180, sat:100, lvl:94],
        [name:"Light Golden Rod Yellow", rgb:"#FAFAD2", hue:60, sat:80, lvl:90],
        [name:"Light Gray", rgb:"#D3D3D3", hue:0, sat:0, lvl:83],
        [name:"Light Green", rgb:"#90EE90", hue:120, sat:73, lvl:75],
        [name:"Light Pink", rgb:"#FFB6C1", hue:351, sat:100, lvl:86],
        [name:"Light Salmon", rgb:"#FFA07A", hue:17, sat:100, lvl:74],
        [name:"Light Sea Green", rgb:"#20B2AA", hue:177, sat:70, lvl:41],
        [name:"Light Sky Blue", rgb:"#87CEFA", hue:203, sat:92, lvl:75],
        [name:"Light Slate Gray", rgb:"#778899", hue:210, sat:14, lvl:53],
        [name:"Light Steel Blue", rgb:"#B0C4DE", hue:214, sat:41, lvl:78],
        [name:"Light Yellow", rgb:"#FFFFE0", hue:60, sat:100, lvl:94],
        [name:"Lime", rgb:"#00FF00", hue:120, sat:100, lvl:50],
        [name:"Lime Green", rgb:"#32CD32", hue:120, sat:61, lvl:50],
        [name:"Linen", rgb:"#FAF0E6", hue:30, sat:67, lvl:94],
        [name:"Maroon", rgb:"#800000", hue:0, sat:100, lvl:25],
        [name:"Medium Aquamarine", rgb:"#66CDAA", hue:160, sat:51, lvl:60],
        [name:"Medium Blue", rgb:"#0000CD", hue:240, sat:100, lvl:40],
        [name:"Medium Orchid", rgb:"#BA55D3", hue:288, sat:59, lvl:58],
        [name:"Medium Purple", rgb:"#9370DB", hue:260, sat:60, lvl:65],
        [name:"Medium Sea Green", rgb:"#3CB371", hue:147, sat:50, lvl:47],
        [name:"Medium Slate Blue", rgb:"#7B68EE", hue:249, sat:80, lvl:67],
        [name:"Medium Spring Green", rgb:"#00FA9A", hue:157, sat:100, lvl:49],
        [name:"Medium Turquoise", rgb:"#48D1CC", hue:178, sat:60, lvl:55],
        [name:"Medium Violet Red", rgb:"#C71585", hue:322, sat:81, lvl:43],
        [name:"Midnight Blue", rgb:"#191970", hue:240, sat:64, lvl:27],
        [name:"Mint Cream", rgb:"#F5FFFA", hue:150, sat:100, lvl:98],
        [name:"Misty Rose", rgb:"#FFE4E1", hue:6, sat:100, lvl:94],
        [name:"Moccasin", rgb:"#FFE4B5", hue:38, sat:100, lvl:85],
        [name:"Navajo White", rgb:"#FFDEAD", hue:36, sat:100, lvl:84],
        [name:"Navy", rgb:"#000080", hue:240, sat:100, lvl:25],
        [name:"Old Lace", rgb:"#FDF5E6", hue:39, sat:85, lvl:95],
        [name:"Olive", rgb:"#808000", hue:60, sat:100, lvl:25],
        [name:"Olive Drab", rgb:"#6B8E23", hue:80, sat:60, lvl:35],
        [name:"Orange", rgb:"#FFA500", hue:39, sat:100, lvl:50],
        [name:"Orange Red", rgb:"#FF4500", hue:16, sat:100, lvl:50],
        [name:"Orchid", rgb:"#DA70D6", hue:302, sat:59, lvl:65],
        [name:"Pale Golden Rod", rgb:"#EEE8AA", hue:55, sat:67, lvl:80],
        [name:"Pale Green", rgb:"#98FB98", hue:120, sat:93, lvl:79],
        [name:"Pale Turquoise", rgb:"#AFEEEE", hue:180, sat:65, lvl:81],
        [name:"Pale Violet Red", rgb:"#DB7093", hue:340, sat:60, lvl:65],
        [name:"Papaya Whip", rgb:"#FFEFD5", hue:37, sat:100, lvl:92],
        [name:"Peach Puff", rgb:"#FFDAB9", hue:28, sat:100, lvl:86],
        [name:"Peru", rgb:"#CD853F", hue:30, sat:59, lvl:53],
        [name:"Pink", rgb:"#FFC0CB", hue:350, sat:100, lvl:88],
        [name:"Plum", rgb:"#DDA0DD", hue:300, sat:47, lvl:75],
        [name:"Powder Blue", rgb:"#B0E0E6", hue:187, sat:52, lvl:80],
        [name:"Purple", rgb:"#800080", hue:300, sat:100, lvl:25],
        [name:"Red", rgb:"#FF0000", hue:0, sat:100, lvl:50],
        [name:"Rosy Brown", rgb:"#BC8F8F", hue:0, sat:25, lvl:65],
        [name:"Royal Blue", rgb:"#4169E1", hue:225, sat:73, lvl:57],
        [name:"Saddle Brown", rgb:"#8B4513", hue:25, sat:76, lvl:31],
        [name:"Salmon", rgb:"#FA8072", hue:6, sat:93, lvl:71],
        [name:"Sandy Brown", rgb:"#F4A460", hue:28, sat:87, lvl:67],
        [name:"Sea Green", rgb:"#2E8B57", hue:146, sat:50, lvl:36],
        [name:"Sea Shell", rgb:"#FFF5EE", hue:25, sat:100, lvl:97],
        [name:"Sienna", rgb:"#A0522D", hue:19, sat:56, lvl:40],
        [name:"Silver", rgb:"#C0C0C0", hue:0, sat:0, lvl:75],
        [name:"Sky Blue", rgb:"#87CEEB", hue:197, sat:71, lvl:73],
        [name:"Slate Blue", rgb:"#6A5ACD", hue:248, sat:53, lvl:58],
        [name:"Slate Gray", rgb:"#708090", hue:210, sat:13, lvl:50],
        [name:"Snow", rgb:"#FFFAFA", hue:0, sat:100, lvl:99],
        [name:"Soft White", rgb:"#B6DA7C", hue:83, sat:44, lvl:67],
        [name:"Spring Green", rgb:"#00FF7F", hue:150, sat:100, lvl:50],
        [name:"Steel Blue", rgb:"#4682B4", hue:207, sat:44, lvl:49],
        [name:"Tan", rgb:"#D2B48C", hue:34, sat:44, lvl:69],
        [name:"Teal", rgb:"#008080", hue:180, sat:100, lvl:25],
        [name:"Thistle", rgb:"#D8BFD8", hue:300, sat:24, lvl:80],
        [name:"Tomato", rgb:"#FF6347", hue:9, sat:100, lvl:64],
        [name:"Turquoise", rgb:"#40E0D0", hue:174, sat:72, lvl:56],
        [name:"Violet", rgb:"#EE82EE", hue:300, sat:76, lvl:72],
        [name:"Warm White", rgb:"#DAF17E", hue:72, sat:20, lvl:72],
        [name:"Wheat", rgb:"#F5DEB3", hue:39, sat:77, lvl:83],
        [name:"White", rgb:"#FFFFFF", hue:0, sat:0, lvl:100],
        [name:"White Smoke", rgb:"#F5F5F5", hue:0, sat:0, lvl:96],
        [name:"Yellow", rgb:"#FFFF00", hue:60, sat:100, lvl:50],
        [name:"Yellow Green", rgb:"#9ACD32", hue:80, sat:61, lvl:50],
	]
    
    return colorMap
}

def poll() {
	log("Polling...", "DEBUG")
    buildGroupList()
	sendLIFXInquiry()
}

def parse(description) {
}

def runEffect(effect="pulse", color="", from_color="", cycles=5, period=0.5, brightness=0.5) {
	log("runEffect(effect=${effect}, color=${color}: 1.0, from_color=${from_color}, cycles=${cycles}, period=${period}, brightness=${brightness}.", "DEBUG")
	if(effect != "pulse" && effect != "breathe") {
    	log("${effect} is not a value effect, defaulting to pulse.", "ERROR")
        effect = "pulse"
    }
    runLIFXEffect(["color" : "${color.toLowerCase()} brightness:${brightness}".trim(), "from_color" : "${from_color.toLowerCase()} brightness:${brightness}".trim(), "cycles" : "${cycles}" ,"period" : "${period}"], effect)
}

def apiFlash(cycles=5, period=0.5, brightness1=1.0, brightness2=0.0) {
    log("Running ApiFlash", "DEBUG")
    if(brightness1 < 0.0) {
    	brightness1 = 0.0
    } else if(brightness1 > 1.0) {
    	brightness1 = 1.0
    }
    if(brightness2 < 0.0) {
    	brightness2 = 0.0
    } else if(brightness2 > 1.0) {
    	brightness2 = 1.0
    }
	runLIFXEffect(["color" : "brightness:${brightness1}", "from_color" : "brightness:${brightness2}", "cycles" : cycles ,"period" : period], "pulse")
}

def apiBreathe(cycles=3, period=2.0, brightness1=1.0, brightness2=0.0) {
    if(brightness1 < 0.0) {
    	brightness1 = 0.0
    } else if(brightness1 > 1.0) {
    	brightness1 = 1.0
    }
    if(brightness2 < 0.0) {
    	brightness2 = 0.0
    } else if(brightness2 > 1.0) {
    	brightness2 = 1.0
    }
	runLIFXEffect(["color" : "brightness:${brightness1}", "from_color" : "brightness:${brightness2}", "cycles" : cycles ,"period" : period], "breathe")
}

def getHex(val) {
	if(val.toLowerCase() == "red") {
    	return "#ff0000"
   	} else if(val.toLowerCase() == "blue") {
    	return "#0000ff"
    } else if(val.toLowerCase() == "green") {
    	return "#00ff00"
    } else if(val.toLowerCase() == "orange") {
    	return "#ff8000"
    } else if(val.toLowerCase() == "yellow") {
    	return "#ffff00"
    } else if(val.toLowerCase() == "cyan") {
    	return "#00ffff"
    } else if(val.toLowerCase() == "purple") {
    	return "#800080"
    } else if(val.toLowerCase() == "pink") {
    	return "#ffb6c1"
    } else {
    	return "#ffffff"
    }
}

def setupSchedule() {
	log("Begin setupSchedule().", "DEBUG")
    try {
	    unschedule(refresh)
    } catch(e) {
        log("Failed to unschedule! Exception ${e}", "ERROR")
        return
    }
    runEvery1Minute(refresh)
}

def updateLightStatus(lightStatus) {
	def finalString = lightStatus
    if(finalString == null) {
    	finalString = "--"
    }
	sendEvent(name: "lightStatus", value: finalString, displayed: getUseActivityLogDebug())
}

def getUseActivityLog() {
	if(state.useActivityLog == null) {
    	state.useActivityLog = true
    }
	return state.useActivityLog
}

def setUseActivityLog(value) {
	state.useActivityLog = value
}

def getUseActivityLogDebug() {
	if(state.useActivityLogDebug == null) {
    	state.useActivityLogDebug = false
    }
    return state.useActivityLogDebug
}

def setUseActivityLogDebug(value) {
	state.useActivityLogDebug = value
}

def getLastCommand() {
	return state.lastCommand
}

def setLastCommand(command) {
	state.lastCommand = command
}

def incRetryCount() {
	state.retryCount = state.retryCount + 1
}

def resetRetryCount() {
	state.retryCount = 0
}

def getRetryCount() {
	return state.retryCount
}

def getMaxRetry() {
	return 3
}

def getRetryWait(base, count) {
    if(count == 0) {
    	return base
    } else {
    	return base * (6 * count)
    }
}

def setDefaultTransitionDuration(value) {
	state.transitionDuration = value
}

def getDefaultTransitionDuration() {
	return state.transitionDuration
}

def setDefaultStateTransitionDuration(value) {
	state.onOffTransitionDuration = value
}

def getDefaultStateTransitionDuration() {
	if(state.onOffTransitionDuration == null) {
    	state.onOffTransitionDuration = 0.0
    }
	return state.onOffTransitionDuration
}

def retry() {
	if(getRetryCount() < getMaxRetry()) {
    	log("Retrying command...", "INFO")
        incRetryCount()
		runIn(getRetryWait(5, getRetryCount()), sendLastCommand )
    } else {
    	log("Too many retries...", "WARN")
        resetRetryCount()
    }
}

def sendLastCommand() {
	sendLIFXCommand(getLastCommand())
}

def sendLIFXCommand(commands) {
	setLastCommand(commands)
    def params = [
        uri: "https://api.lifx.com",
		path: "/v1/lights/" + getGroups() + "/state",
        requestContentType: "application/json",
        headers: ["Content-Type": "application/json", "Accept": "application/json", "Authorization": "Bearer ${token}"],
        body: commands
    ]
    asynchttpPut('putResponseHandler', params)
}

def runLIFXEffect(commands, effect) {
	def params = [
        uri: "https://api.lifx.com",
		path: "/v1/lights/" + getGroups() + "/effects/" + effect,
        requestContentType: "application/json",
        headers: ["Content-Type": "application/json", "Accept": "application/json", "Authorization": "Bearer ${token}"],
        body: commands
    ]
    asynchttpPost('postResponseHandler', params)
}

def sendLIFXInquiry() {
	def params = [
        uri: "https://api.lifx.com",
		path: "/v1/lights/" + getGroups(),
        requestContentType: "application/json",
        headers: ["Content-Type": "application/x-www-form-urlencoded", "Authorization": "Bearer ${token}"]
    ]
    asynchttpGet('getResponseHandler', params)
}

def postResponseHandler(response, data) {
    if(response.getStatus() == 200 || response.getStatus() == 207) {
		log("Response received from LFIX in the postReponseHandler.", "DEBUG")
    } else {
    	log("LIFX failed to adjust group. LIFX returned ${response.getStatus()}.", "ERROR")
        log("Error = ${response.getErrorData()}", "ERROR")
    }
}

def putResponseHandler(response, data) {
    if(response.getStatus() == 200 || response.getStatus() == 207) {
		log("Response received from LFIX in the putReponseHandler.", "DEBUG")
        log("Response = ${response.getJson()}", "DEBUG")
        def totalBulbs = response.getJson().results.size()
        def results = response.getJson().results
        def bulbsOk = 0
        
        for(int i=0;i<totalBulbs;i++) {
        	if(results[i].status != "ok") {
        		log("${results[i].label} is ${results[i].status}.", "WARN")
            } else {
            	bulbsOk++
            	log("${results[i].label} is ${results[i].status}.", "TRACE")
            }
        }
        if(bulbsOk == totalBulbs) { 
            log("${bulbsOk} of ${totalBulbs} bulbs returned ok.", "INFO")
            resetRetryCount()
        } else {
        	log("${bulbsOk} of ${totalBulbs} bulbs returned ok.", "WARN")
            log("Retry Count = ${getRetryCount()}.", "INFO")
            retry()
        }
        updateLightStatus("${bulbsOk} of ${totalBulbs}")
    } else {
    	log("LIFX failed to adjust group. LIFX returned ${response.getStatus()}.", "ERROR")
        log("Error = ${response.getErrorData()}", "ERROR")
    }
}

def getResponseHandler(response, data) {
    if(response.getStatus() == 200 || response.getStatus() == 207) {
		log("Response received from LFIX in the getReponseHandler.", "DEBUG")
        log("Response ${response.data}", "DEBUG")
       	response.getJson().each {
        	log("${it.label} is ${it.power}.", "TRACE")
        	log("Bulb Type: ${it.product.name}.", "TRACE")
        	log("Has variable color temperature = ${it.product.capabilities.has_variable_color_temp}.", "TRACE")
            log("Has color = ${it.product.capabilities.has_color}.", "TRACE")
            log("Has ir = ${it.product.capabilities.has_ir}.", "TRACE")
            log("Has Multizone = ${it.product.capabilities.has_multizone}.", "TRACE")
        	log("Brightness = ${it.brightness}.", "TRACE")
        	log("Color = [saturation:${it.color.saturation}], kelvin:${it.color.kelvin}, hue:${it.color.hue}.", "TRACE")
        	DecimalFormat df = new DecimalFormat("###,##0.0#")
        	DecimalFormat dfl = new DecimalFormat("###,##0.000")
        	DecimalFormat df0 = new DecimalFormat("###,##0")
            if(it.power == "on") {
                sendEvent(name: "switch", value: "on")
                if(it.color.saturation == 0.0) {
                    log("Saturation is 0.0, setting color temperature.", "TRACE")
                    def b = df0.format(it.brightness * 100)
                    sendEvent(name: "colorTemperature", value: it.color.kelvin, displayed: getUseActivityLogDebug())
                    sendEvent(name: "color", value: "#ffffff", displayed: getUseActivityLogDebug())
                    sendEvent(name: "level", value: b, displayed: getUseActivityLogDebug())
                    sendEvent(name: "switch", value: "on", displayed: getUseActivityLogDebug())
                } else {
                    log("Saturation is > 0.0, setting color.", "TRACE")
                    def h = df.format(it.color.hue / 3.6)
                    def s = df.format(it.color.saturation * 100)
                    def b = df0.format(it.brightness * 100)
                    log("h = ${h}, s = ${s}, b = ${b}.", "TRACE")
                    sendEvent(name: "hue", value: h, displayed: getUseActivityLogDebug())
                    sendEvent(name: "saturation", value: s, displayed: getUseActivityLogDebug())
                    sendEvent(name: "kelvin", value: it.color.kelvin, displayed: getUseActivityLogDebug())
                    sendEvent(name: "level", value: b, displayed: getUseActivityLogDebug())
                    sendEvent(name: "switch", value: "on", displayed: getUseActivityLogDebug())
                }
            } else if(it.power == "off") {
                sendEvent(name: "switch", value: "off", displayed: getUseActivityLogDebug())
            }
        }
    } else {
    	log("LIFX failed to update the group. LIFX returned ${response.getStatus()}.", "ERROR")
        log("Error = ${response.getErrorData()}", "ERROR")
    }
}
