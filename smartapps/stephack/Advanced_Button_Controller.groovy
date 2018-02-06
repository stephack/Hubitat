/*	
 *
 *	Advanced Button Controller
 *
 *	Author: SmartThings, modified by Bruce Ravenel, Dale Coffing, Stephan Hackett
 * 
 *
 *  2/06/18 - converted code to hubitat format
 * 		removed ability to hid "held options"
 *		removed hwspecifics section as is no longer applicable
 *		adjusted device list to look for "capability.pushableButton"
 *		adjusted buttonDevice subscription (pushed, held, doubleTapped)
 *		adjusted buttonEvent() to swap "name" and "value" as per new rules
 *
 *
 */
def version(){"v0.2.180206"}

definition(
    name: "ABC Child Creator",
    namespace: "stephack",
    author: "Stephan Hackett",
    description: "Assign tasks to your Button Controller Devices",
    category: "My Apps",
    //parent: "stephack:ABC Manager",
    iconUrl: "https://cdn.rawgit.com/stephack/ABC/master/resources/images/abcNew.png",
    iconX2Url: "https://cdn.rawgit.com/stephack/ABC/master/resources/images/abcNew.png",
    iconX3Url: "https://cdn.rawgit.com/stephack/ABC/master/resources/images/abcNew.png",
)

preferences {
	page(name: "chooseButton")
	page(name: "configButtonsPage")
	page(name: "timeIntervalInput", title: "Only during a certain time") {
		section {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
		}
	}
}

def chooseButton() {
	dynamicPage(name: "chooseButton", install: true, uninstall: true) {
		section("Step 1: Select Your Button Device") {
			input "buttonDevice", "capability.pushableButton", title: "Button Device", multiple: false, required: true, submitOnChange: true
		}
        if(buttonDevice){
        	state.buttonType =  buttonDevice.typeName
            if(state.buttonType.contains("Aeon Minimote")) state.buttonType =  "Aeon Minimote"
            log.debug "Device Type is now set to: "+state.buttonType
            state.buttonCount = manualCount?: buttonDevice.currentValue('numberOfButtons')
            //if(state.buttonCount==null) state.buttonCount = buttonDevice.currentValue('numButtons')	//added for kyse minimote(hopefully will be updated to correct attribute name)
            section("Step 2: Configure Your Buttons"){
            	if(state.buttonCount<1) {
                	paragraph "The selected button device did not report the number of buttons it has. Please specify in the Advanced Config section below."
                }
                else {
                	for(i in 1..state.buttonCount){
                		href "configButtonsPage", title: "Button ${i} - (Tap to Edit)", description: getDescription(i), params: [pbutton: i]
                    }
            	}
            }
		}
        section("Set Custom Name (Optional)") {
        	label title: "Assign a name:", required: false
        }
        section("Advanced Config:", hideable: true, hidden: hideOptionsSection()) {
            	input "manualCount", "number", title: "Set/Override # of Buttons?", required: false, description: "Only set if DTH does not report", submitOnChange: true
                input "collapseAll", "bool", title: "Collapse Unconfigured Sections?", defaultValue: true
                //input "hwSpecifics", "bool", title: "Hide H/W Specific Details?", defaultValue: false
			}
        section(title: "Only Execute When:", hideable: true, hidden: hideOptionsSection()) {
			def timeLabel = timeIntervalLabel()
			href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null
			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
					options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
		}
	}
}

def configButtonsPage(params) {
	if (params.pbutton != null) state.currentButton = params.pbutton.toInteger()
	dynamicPage(name: "configButtonsPage", title: "CONFIGURE BUTTON ${state.currentButton}:\n${state.buttonType}", getButtonSections(state.currentButton))
}

def getButtonSections(buttonNumber) {
	return {
    	//def picNameNoSpace = "${state.buttonType}${state.currentButton}.png"-" "-" "-" "-"/"-"-"
        //log.debug picNameNoSpace
        //section(){	//"Hardware specific info on button selection:") {
	//		if(hwSpecifics== false) paragraph image: "https://cdn.rawgit.com/stephack/ABC/master/resources/images/${picNameNoSpace}", "${getSpecText()}"
    	//}
        section("Switches (Turn On)", hideable: true, hidden: !shallHide("lightOn_${buttonNumber}")) {
			input "lightOn_${buttonNumber}_pushed", "capability.switch", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "lightOn_${buttonNumber}_held", "capability.switch", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
		}
		section("Switches (Turn Off)", hideable: true, hidden: !shallHide("lightOff_${buttonNumber}")) {
			input "lightOff_${buttonNumber}_pushed", "capability.switch", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "lightOff_${buttonNumber}_held", "capability.switch", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
		}
        section("Switches (Toggle On/Off)", hideable: true, hidden: !shallHide("lights_${buttonNumber}")) {
			input "lights_${buttonNumber}_pushed", "capability.switch", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "lights_${buttonNumber}_held", "capability.switch", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
		}
		section(" "){}
		section("Dimmers (On to Level - Group 1)", hideable: true, hidden: !(shallHide("lightDim_${buttonNumber}") || shallHide("valLight${buttonNumber}"))) {
			input "lightDim_${buttonNumber}_pushed", "capability.switchLevel", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "valLight${buttonNumber}_pushed", "number", title: "Bright Level", multiple: false, required: false, description: "0 to 100%"
			input "lightDim_${buttonNumber}_held", "capability.switchLevel", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
			input "valLight${buttonNumber}_held", "number", title: "Bright Level", multiple: false, required: false, description: "0 to 100%"
		}
		section("Dimmers (On to Level - Group 2)", hideable: true, hidden: !(shallHide("lightD2m_${buttonNumber}") || shallHide("valLight2${buttonNumber}"))) {
			input "lightD2m_${buttonNumber}_pushed", "capability.switchLevel", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "valLight2${buttonNumber}_pushed", "number", title: "Bright Level", multiple: false, required: false, description: "0 to 100%"
			input "lightD2m_${buttonNumber}_held", "capability.switchLevel", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
			input "valLight2${buttonNumber}_held", "number", title: "Bright Level", multiple: false, required: false, description: "0 to 100%"
		}
        section("Dimmers (Increase Level By)", hideable: true, hidden: !(shallHide("dimPlus_${buttonNumber}") || shallHide("valDimP${buttonNumber}"))) {
			input "dimPlus_${buttonNumber}_pushed", "capability.switchLevel", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
            input "valDimP${buttonNumber}_pushed", "number", title: "When Pushed Increase by", multiple: false, required: false, description: "0 to 15"
			input "dimPlus_${buttonNumber}_held", "capability.switchLevel", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
            input "valDimP${buttonNumber}_held", "number", title: "When Held Increase by", multiple: false, required: false, description: "0 to 15"
		}
          	section("Dimmers (Decrease Level By)", hideable: true, hidden: !(shallHide("dimMinus_${buttonNumber}") || shallHide("valDimM${buttonNumber}"))) {
			input "dimMinus_${buttonNumber}_pushed", "capability.switchLevel", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
            input "valDimM${buttonNumber}_pushed", "number", title: "When Pushed Decrease by", multiple: false, required: false, description: "0 to 15"
			input "dimMinus_${buttonNumber}_held", "capability.switchLevel", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
            input "valDimM${buttonNumber}_held", "number", title: "When Held Decrease by", multiple: false, required: false, description: "0 to 15"
		}
        section("Dimmers (Toggle OnToLevel/Off)", hideable: true, hidden: !(shallHide("lightsDT_${buttonNumber}") || shallHide("valDT${buttonNumber}"))) {
			input "lightsDT_${buttonNumber}_pushed", "capability.switchLevel", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "valDT${buttonNumber}_pushed", "number", title: "Bright Level", required: false, description: "0 to 100%"
			input "lightsDT_${buttonNumber}_held", "capability.switchLevel", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
			input "valDT${buttonNumber}_held", "number", title: "Bright Level", required: false, description: "0 to 100%"
		}
        section(" "){}
		section("Speakers (Toggle Play/Pause)", hideable: true, hidden: !shallHide("speakerpp_${buttonNumber}")) {
			input "speakerpp_${buttonNumber}_pushed", "capability.musicPlayer", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "speakerpp_${buttonNumber}_held", "capability.musicPlayer", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
		}
        section("Speakers (Increase Vol By)", hideable: true, hidden: !(shallHide("speakervu_${buttonNumber}") || shallHide("valSpeakU${buttonNumber}"))) {
			input "speakervu_${buttonNumber}_pushed", "capability.musicPlayer", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
            input "valSpeakU${buttonNumber}_pushed", "number", title: "When Pushed Increase by", multiple: false, required: false, description: "0 to 15"
			input "speakervu_${buttonNumber}_held", "capability.musicPlayer", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
            input "valSpeakU${buttonNumber}_held", "number", title: "When Held Increase by", multiple: false, required: false, description: "0 to 15"
		}
        section("Speakers (Decrease Vol By)", hideable: true, hidden: !(shallHide("speakervd_${buttonNumber}") || shallHide("valSpeakD${buttonNumber}"))) {
			input "speakervd_${buttonNumber}_pushed", "capability.musicPlayer", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
            input "valSpeakD${buttonNumber}_pushed", "number", title: "When Pushed Decrease by", multiple: false, required: false, description: "0 to 15"
			input "speakervd_${buttonNumber}_held", "capability.musicPlayer", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
            input "valSpeakD${buttonNumber}_held", "number", title: "When Held Decrease by", multiple: false, required: false, description: "0 to 15"
		}
        section("Speakers (Go to Next Track)", hideable: true, hidden: !shallHide("speakernt_${buttonNumber}")) {
			input "speakernt_${buttonNumber}_pushed", "capability.musicPlayer", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "speakernt_${buttonNumber}_held", "capability.musicPlayer", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
		}
        section("Speakers (Toggle Mute/Unmute)", hideable: true, hidden: !shallHide("speakermu_${buttonNumber}")) {
			input "speakermu_${buttonNumber}_pushed", "capability.musicPlayer", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "speakermu_${buttonNumber}_held", "capability.musicPlayer", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
		}
        section(" "){}
        section("Sirens (Toggle)", hideable: true, hidden: !shallHide("sirens_${buttonNumber}")) {
			input "sirens_${buttonNumber}_pushed","capability.alarm" ,title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "sirens_${buttonNumber}_held", "capability.alarm", title: "When Held", multiple: true, required: false, submitOnChange: true
		}
        section("Locks (Lock Only)", hideable: true, hidden: !shallHide("locks_${buttonNumber}")) {
			input "locks_${buttonNumber}_pushed", "capability.lock", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "locks_${buttonNumber}_held", "capability.lock", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
		}
		section("Fans (Adjust - Low, Medium, High, Off)", hideable: true, hidden: !shallHide("fanAdjust_${buttonNumber}")) {
			input "fanAdjust_${buttonNumber}_pushed", "capability.switchLevel", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "fanAdjust_${buttonNumber}_held", "capability.switchLevel", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
		}
		section("Shades (Adjust - Up, Down, or Stop)", hideable: true, hidden: !shallHide("shadeAdjust_${buttonNumber}")) {
			input "shadeAdjust_${buttonNumber}_pushed", "capability.doorControl", title: "When Pushed", multiple: true, required: false, submitOnChange: collapseAll
			input "shadeAdjust_${buttonNumber}_held", "capability.doorControl", title: "When Held", multiple: true, required: false, submitOnChange: collapseAll
		}
        section(" "){}
		section("Set Mode", hideable: true, hidden: !shallHide("mode_${buttonNumber}")) {
			input "mode_${buttonNumber}_pushed", "mode", title: "When Pushed", required: false, submitOnChange: collapseAll
			input "mode_${buttonNumber}_held", "mode", title: "When Held", required: false, submitOnChange: collapseAll
		}
		def phrases = location.helloHome?.getPhrases()*.label
		if (phrases) {
        	section("Run Routine", hideable: true, hidden: !shallHide("phrase_${buttonNumber}")) {
				//log.trace phrases
				input "phrase_${buttonNumber}_pushed", "enum", title: "When Pushed", required: false, options: phrases, submitOnChange: collapseAll
				input "phrase_${buttonNumber}_held", "enum", title: "When Held", required: false, options: phrases, submitOnChange: collapseAll
			}
		}
        section("Notifications:\nSMS, In App or Both", hideable: true, hidden: !shallHide("notifications_${buttonNumber}")) {
        paragraph "****************\nWHEN PUSHED\n****************"
			input "notifications_${buttonNumber}_pushed", "text", title: "Message", description: "Enter message to send", required: false, submitOnChange: collapseAll
            input "phone_${buttonNumber}_pushed","phone" ,title: "Send Text To", description: "Enter phone number", required: false, submitOnChange: collapseAll
            input "valNotify${buttonNumber}_pushed","bool" ,title: "Notify In App?", required: false, defaultValue: false, submitOnChange: collapseAll
            paragraph "*************\nWHEN HELD\n*************"
			input "notifications_${buttonNumber}_held", "text", title: "Message", description: "Enter message to send", required: false, submitOnChange: collapseAll
			input "phone_${buttonNumber}_held", "phone", title: "Send Text To", description: "Enter phone number", required: false, submitOnChange: collapseAll
			input "valNotify${buttonNumber}_held", "bool", title: "Notify In App?", required: false, defaultValue: false, submitOnChange: collapseAll			
		}
        if(enableSpec()){
        	section(" "){}
			section("Special", hideable: true, hidden: !shallHide("container_${buttonNumber}")) {
				input "container_${buttonNumber}_pushed", "device.VirtualContainer", title: "When Pushed", required: false, submitOnChange: collapseAll
				input "container_${buttonNumber}_held", "device.VirtualContainer", title: "When Held", required: false, submitOnChange: collapseAll
			}
        }
	}
}

def enableSpec() {
	return false
}

def shallHide(myFeature) {
	if(collapseAll) return (settings["${myFeature}_pushed"]||settings["${myFeature}_held"]||settings["${myFeature}"])
	return true
}

def getDescription(dNumber) {
    def descript = "Nothing Configured"
    def anySettings = settings.find{it.key.contains("_${dNumber}_")}
    if(anySettings) descript = "PUSHED:"+getDescDetails(dNumber,"_pushed")+"\n\nHELD:"+getDescDetails(dNumber,"_held")//"CONFIGURED : Tap to edit"
	return descript
}

def getDescDetails(bNum, type){
	def numType=bNum+type
	def preferenceNames = settings.findAll{it.key.contains("_${numType}")}.sort()		//get all configured settings that: match button# and type, AND are not false
    if(!preferenceNames){
    	return "  **Not Configured**"
    }
    else {
    	def formattedPage =""
    	preferenceNames.each {eachPref->
        	def prefDetail = getPreferenceDetails().find{eachPref.key.contains(it.id)}	//gets decription of action being performed(eg Turn On)
        	def prefDevice = " : ${eachPref.value}" - "[" - "]"											//name of device the action is being performed on (eg Bedroom Fan)
            def prefSubValue = settings[prefDetail.sub + numType]?:"(!Missing!)"
            if(prefDetail.type=="normal") formattedPage += "\n- ${prefDetail.desc}${prefDevice}"
            if(prefDetail.type=="hasSub") formattedPage += "\n- ${prefDetail.desc}${prefSubValue}${prefDevice}"
            if(prefDetail.type=="bool") formattedPage += "\n- ${prefDetail.desc}"
    	}
		return formattedPage
    }
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
    log.debug "INITIALIZED with settings: ${settings}"
	app.label==app.name?app.updateLabel(defaultLabel()):app.updateLabel(app.label)
	subscribe(buttonDevice, "pushed", buttonEvent)
	subscribe(buttonDevice, "held", buttonEvent)
	subscribe(buttonDevice, "doubleTapped", buttonEvent)
    state.lastshadesUp = true
}

def defaultLabel() {
	return "${buttonDevice} Mapping"
}

def getPreferenceDetails(){
	def detailMappings =
    	[[id:'lights_',desc:'Toggle On/Off',comm:toggle, type:"normal"],
         [id:'lightsDT_', desc:'Toggle Off/Dim to ', comm:dimToggle, sub:"valDT", type:"hasSub"],
     	 [id:'lightOn_',desc:'Turn On ',comm:turnOn, type:"normal"],
     	 [id:"lightOff_",desc:'Turn Off',comm:turnOff, type:"normal"],
     	 [id:"lightDim_",desc:'Dim to ',comm:turnDim, sub:"valLight", type:"hasSub"],
     	 [id:"lightD2m_",desc:'Dim to ',comm:turnDim, sub:"valLight2", type:"hasSub"],
         [id:'dimPlus_',desc:'Brightness +',comm:levelUp, sub:"valDimP", type:"hasSub"],
     	 [id:'dimMinus_',desc:'Brightness -',comm:levelDown, sub:"valDimM", type:"hasSub"],
     	 [id:"fanAdjust_",desc:'Adjust',comm:adjustFan, type:"normal"],
     	 [id:"shadeAdjust_",desc:'Adjust',comm:adjustShade, type:"normal"],
     	 [id:"locks_",desc:'Lock',comm:setUnlock, type:"normal"],
         [id:"speakerpp_",desc:'Toggle Play/Pause',comm:speakerplaystate, type:"normal"],
         [id:'speakernt_',desc:'Next Track',comm:speakernexttrack, type:"normal"],
    	 [id:'speakermu_',desc:'Mute',comm:speakermute, type:"normal"],
     	 [id:'speakervu_',desc:'Volume +',comm:levelUp, sub:"valSpeakU", type:"hasSub"],
     	 [id:"speakervd_",desc:'Volume -',comm:levelDown, sub:"valSpeakD", type:"hasSub"],
     	 [id:"mode_",desc:'Set Mode',comm:changeMode, type:"normal"],
         [id:"notifications_",desc:'Send Push Notification',comm:messageHandle, sub:"valNotify", type:"bool"],
         [id:'sirens_',desc:'Toggle',comm:toggle, type:"normal"],
     	 [id:"phone_",desc:'Send SMS',comm:smsHandle, sub:"notifications_", type:"normal"],
     	 [id:"phrase_",desc:'Run Routine',comm:runRout, type:"normal"],
         [id:"container_",desc:'Cycle Playlist',comm:cyclePL, type:"normal"],         
        ]
    return detailMappings
}

def buttonEvent(evt) {
	if(allOk) {
    	def buttonNumber = evt.value
		def pressType = evt.name
		log.debug "$buttonDevice: Button $buttonNumber was $pressType"
    	def preferenceNames = settings.findAll{it.key.contains("_${buttonNumber}_${pressType}")}
    	preferenceNames.each{eachPref->
        	def prefDetail = getPreferenceDetails()?.find{eachPref.key.contains(it.id)}		//returns the detail map of id,desc,comm,sub
        	def PrefSubValue = settings["${prefDetail.sub}${buttonNumber}_${pressType}"]	//value of subsetting (eg 100)
        	if(prefDetail.sub) "$prefDetail.comm"(eachPref.value,PrefSubValue)
        	else "$prefDetail.comm"(eachPref.value)
    	}
	}
}

def turnOn(devices) {
	log.debug "Turning On: $devices"
	devices.on()
}

def turnOff(devices) {
	log.debug "Turning Off: $devices"
	devices.off()
}

def turnDim(devices, level) {
	log.debug "Dimming (to $level): $devices"
	devices.setLevel(level)
}

def adjustFan(device) {
	log.debug "Adjusting: $device"
	def currentLevel = device.currentLevel
	if(device.currentSwitch == 'off') device.setLevel(15)
	else if (currentLevel < 34) device.setLevel(50)
  	else if (currentLevel < 67) device.setLevel(90)
	else device.off()
}

def adjustShade(device) {
	log.debug "Shades: $device = ${device.currentMotor} state.lastUP = $state.lastshadesUp"
	if(device.currentMotor in ["up","down"]) {
    	state.lastshadesUp = device.currentMotor == "up"
    	device.stop()
    } else {
    	state.lastshadesUp ? device.down() : device.up()
//    	if(state.lastshadesUp) device.down()
//        else device.up()
        state.lastshadesUp = !state.lastshadesUp
    }
}

def speakerplaystate(device) {
	log.debug "Toggling Play/Pause: $device"
	device.currentValue('status').contains('playing')? device.pause() : device.play()
}
   
def speakernexttrack(device) {
	log.debug "Next Track Sent to: $device"
	device.nextTrack()
}   

def speakermute(device) {
	log.debug "Toggling Mute/Unmute: $device"
	device.currentValue('mute').contains('unmuted')? device.mute() : device.unmute()
} 

def levelUp(device, inclevel) {
	log.debug "Incrementing Level (by +$inclevel: $device"
	def currentVol = device.currentValue('level')[0]	//currentlevel return a list...[0] is first item in list ie volume level
    def newVol = currentVol + inclevel
  	device.setLevel(newVol)
    log.debug "Level increased by $inclevel to $newVol"
}

def levelDown(device, declevel) {
	log.debug "Decrementing Level (by -declevel: $device"
	def currentVol = device.currentValue('level')[0]
    def newVol = currentVol.toInteger()-declevel
  	device.setLevel(newVol)
    log.debug "Level decreased by $declevel to $newVol"
}

def setUnlock(devices) {
	log.debug "Locking: $devices"
	devices.lock()
}

def toggle(devices) {
	log.debug "Toggling: $devices"
	if (devices*.currentValue('switch').contains('on')) {
		devices.off()
	}
	else if (devices*.currentValue('switch').contains('off')) {
		devices.on()
	}
	else if (devices*.currentValue('alarm').contains('off')) {
        devices.siren()
    }
	else {
		devices.on()
	}
}

def dimToggle(devices, dimLevel) {
	log.debug "Toggling On/Off | Dimming (to $dimLevel): $devices"
	if (devices*.currentValue('switch').contains('on')) devices.off()
	else devices.setLevel(dimLevel)
}

def runRout(rout){
	log.debug "Running: $rout"
	location.helloHome.execute(rout)
}

def messageHandle(msg, inApp) {
	if(inApp==true) {
    	log.debug "Push notification sent"
    	sendPush(msg)
	}
}

def smsHandle(phone, msg){
    log.debug "SMS sent"
    sendSms(phone, msg ?:"No custom text entered on: $app.label")
}

def changeMode(mode) {
	log.debug "Changing Mode to: $mode"
	if (location.mode != mode && location.modes?.find { it.name == mode }) setLocationMode(mode)
}

def cyclePL(device) {
	//int currPL = device.currentValue('lastRun')
   // int nextPL = currPL+1
    device.cycleChild()
    //device.on(nextPL)

}

// execution filter methods
private getAllOk() {
	modeOk && daysOk && timeOk
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days.contains(day)
	}
	log.trace "daysOk = $result"
	result
}

private getTimeOk() {
	def result = true
	if (starting && ending) {
		def currTime = now()
		def start = timeToday(starting).time
		def stop = timeToday(ending).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOk = $result"
	result
}

private hhmm(time, fmt = "h:mm a") {
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private hideOptionsSection() {
	(starting || ending || days || modes || manualCount) ? false : true
}

private timeIntervalLabel() {
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}

private def textHelp() {
	def text =
	section("User's Guide - Advanced Button Controller") {
    	paragraph "This smartapp allows you to use a device with buttons including, but not limited to:\n\n  Aeon Labs Minimotes\n"+
    	"  HomeSeer HS-WD100+ switches**\n  HomeSeer HS-WS100+ switches\n  Lutron Picos***\n\n"+
		"It is a heavily modified version of @dalec's 'Button Controller Plus' which is in turn"+
        " a version of @bravenel's 'Button Controller+'."
   	}
	section("Some of the included changes are:"){
        paragraph "A complete revamp of the configuration flow. You can now tell at a glance, what has been configured for each button."+
        "The button configuration page has been collapsed by default for easier navigation."
        paragraph "The original apps were hardcoded to allow configuring 4 or 6 button devices."+
        " This app will automatically detect the number of buttons on your device or allow you to manually"+
        " specify (only needed if device does not report on its own)."
		paragraph "Allows you to give your buton device full speaker control including: Play/Pause, NextTrack, Mute, VolumeUp/Down."+
    	"(***Standard Pico remotes can be converted to Audio Picos)\n\nThe additional control options have been highlighted below."
	}
	section("Available Control Options are:"){
        paragraph "	Switches - Toggle \n"+
        "	Switches - Turn On \n"+
        "	Switches - Turn Off \n"+
        "	Dimmers - Toggle \n"+
        "	Dimmers - Set Level (Group 1) \n"+
        "	Dimmers - Set Level (Group 2) \n"+
        "	*Dimmers - Inc Level \n"+
        "	*Dimmers - Dec Level \n"+
        "	Fans - Low, Medium, High, Off \n"+
        "	Shades - Up, Down, or Stop \n"+
        "	Locks - Unlock Only \n"+
        "	Speaker - Play/Pause \n"+
        "	*Speaker - Next Track \n"+
        "	*Speaker - Mute/Unmute \n"+
        "	*Speaker - Volume Up \n"+
        "	*Speaker - Volume Down \n"+
        "	Set Modes \n"+
        "	Run Routines \n"+
        "	Sirens - Toggle \n"+
        "	Push Notifications \n"+
        "	SMS Notifications"
	}
	section ("** Quirk for HS-WD100+ on Button 5 & 6:"){
        paragraph "Because a dimmer switch already uses Press&Hold to manually set the dimming level"+
        " please be aware of this operational behavior. If you only want to manually change"+
        " the dim level to the lights that are wired to the switch, you will automatically"+
        " trigger the 5/6 button event as well. And the same is true in reverse. If you"+ 
        " only want to trigger a 5/6 button event action with Press&Hold, you will be manually"+
        " changing the dim level of the switch simultaneously as well.\n"+
        "This quirk doesn't exist of course with the HS-HS100+ since it is not a dimmer."
	}
	section("*** Lutron Pico Requirements:"){
        paragraph "Lutron Picos are not natively supported by SmartThings. A Lutron SmartBridge Pro, a device running @njschwartz's python script (or node.js) and the Lutron Caseta Service Manager"+
    	" SmartApp are also required for this functionality!\nSearch the forums for details."
	}
}
  
def getSpecText(){
	if(state.buttonType == "Lutron Pico") {
    	switch (state.currentButton){
        	case 1: return "Top Button"; break
			case 2: return "Bottom Button"; break
			case 3: return "Middle Button";break
			case 4: return "Up Button"; break
			case 5: return "Down Button"; break        
        }
    }
	if(state.buttonType.contains("Aeon Minimote")) {
    	switch (state.currentButton){
        	case 1: return "Top Left Button"; break
			case 2: return "Top Right Button"; break
			case 3: return "Lower Left Button";break
			case 4: return "Lower Right"; break
        }
    }
	if(state.buttonType.contains("WD100+ Dimmer")) {
    	switch (state.currentButton){
        	case 1: return "Double-Tap Upper Paddle"; break
			case 2: return "Double-Tap Lower Paddle"; break
			case 3: return "Triple-Tap Upper Paddle";break
			case 4: return "Triple-Tap Lower Paddle"; break
            case 5: return "Press & Hold Upper Paddle\n(See user guide for quirks)"; break
			case 6: return "Press & Hold Lower Paddle\n(See user guide for quirks)"; break
			case 7: return "Single Tap Upper Paddle\n(See user guide for quirks)"; break
			case 8: return "Single Tap Lower Paddle\n(See user guide for quirks)"; break
        }
    }
    if(state.buttonType.contains("WS100+ Switch")) {
    	switch (state.currentButton){
        	case 1: return "Double-Tap Upper Paddle"; break
			case 2: return "Double-Tap Lower Paddle"; break
			case 3: return "Triple-Tap Upper Paddle";break
			case 4: return "Triple-Tap Lower Paddle"; break
            case 5: return "Press & Hold Upper Paddle"; break
			case 6: return "Press & Hold Lower Paddle"; break
			case 7: return "Single Tap Upper Paddle";break
			case 8: return "Single Tap Lower Paddle"; break
        }
    }
    if(state.buttonType.contains("Inovelli")) {
    	switch (state.currentButton){
        	case 1: return "NOT OPERATIONAL - DO NOT USE"; break
        	case 2: return "2X Tap Upper Paddle = Pushed\n2X Tap Lower Paddle = Held"; break
			case 3: return "3X Tap Upper Paddle = Pushed\n3X Tap Lower Paddle = Held"; break
			case 4: return "4X Tap Upper Paddle = Pushed\n4X Tap Lower Paddle = Held";break
			case 5: return "5X Tap Upper Paddle = Pushed\n5X Tap Lower Paddle = Held"; break
            case 6: return "Hold Upper Paddle = Pushed\nHold Lower Paddle = Held"; break
        }
    }
    if(state.buttonType.contains("ZRC-90")) {
    	switch (state.currentButton){
        	case 1: return "Tap or Hold Button 1"; break
        	case 2: return "Tap or Hold Button 2"; break
			case 3: return "Tap or Hold Button 3"; break
			case 4: return "Tap or Hold Button 4";break
			case 5: return "Tap or Hold Button 5"; break
            case 6: return "Tap or Hold Button 6"; break
            case 7: return "Tap or Hold Button 7"; break
        	case 8: return "Tap or Hold Button 8"; break
			case 9: return "2X Tap Button 1\nHold Not Available"; break
			case 10: return "2X Tap Button 2\nHold Not Available";break
			case 11: return "2X Tap Button 3\nHold Not Available"; break
            case 12: return "2X Tap Button 4\nHold Not Available"; break
            case 13: return "2X Tap Button 5\nHold Not Available"; break
        	case 14: return "2X Tap Button 6\nHold Not Available"; break
			case 15: return "2X Tap Button 7\nHold Not Available"; break
			case 16: return "2X Tap Button 8\nHold Not Available";break			
        }
    }
    return "Not Specified By Device"
}


/*FOR NEW INPUTS//////////////
1. add input to config
2. add info to detailMappings including subvalue if needed
3. ensure correct type is used in map..or create a new one with its own formattedPage



FOR NEW BUTTON DEVICE TYPES///////////////
1. ensure device reports buttonNumber
2. if not, add sendEvent to DTH as needed OR just enter manually
3. add any special instructions to getSpecText() using dth name
4. create pics for each button using dthName+dNumber






*/
