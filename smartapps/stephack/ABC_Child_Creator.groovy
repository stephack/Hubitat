/*	
 *
 *	ABC Child Creator for Advanced Button Controller
 *
 *	Author: SmartThings, modified by Bruce Ravenel, Dale Coffing, Stephan Hackett
 * 
 * 8/01/18 - added Hubitat Safety Monitor Control (created new MODES section for Set Mode and Set HSM)
 * 		added level to setColor()
 *		added new detail parameter "myDetail.mul" (Mode and HSM set to multiple:false)
 *		removed section shallHide for sub inputs .... (section will be visible if primary input has a value...sub value no longer checked)
 *
 *
 * 7/03/18 - code cleanup
 *		Added pictures enhancements and reordered options for better flow
 *		Corrected default child app label (previously defaulted to "ABC Button Mapping" on first save)
 *
 *
 * 7/01/18 - added Released actions for all control sections
 *		Pushed/Held/DoubleTapped/Released hidden from Dimmer Ramp section based on devices capabilities
 *
 * 6/30/18 - adapted fan cycle to be compliant with fanControl capability (removed cycle support for custom driver)
 *		added ability to set specific fan speed
 *		added support for ramping (graceful dimming) - switch/bulb needs changeLevel capability and button device needs releaseableButton capability
 *		
 *
 *	6/02/18 - added ability to cycle custom Hampton Bay Zigbee Fan Controller
 *
 *
 *	4/21/18 - added support for new Sonos Player devices (play/pause, next, previous, mute/unmute, volumeup/down)
 *
 *
 *	3/28/18 - added option to set color and temp
 *		test code for custom commands (not yet working)
 *
 *  2/06/18 - converted code to hubitat format
 * 		removed ability to hide "held options"
 *		removed hwspecifics section as is no longer applicable
 *		adjusted device list to look for "capability.pushableButton"
 *		adjusted buttonDevice subscription (pushed, held, doubleTapped)
 *		adjusted buttonEvent() to swap "name" and "value" as per new rules
 * 2/08/18 - change formatting for Button Config Preview (Blue/Complete color)
 *		Added Double Tap inputs and edited shallHide() getDescription()
 *		added code for showDouble() to only display when controller support DT
 *		removed enableSpec and other Virtual Container Code as this is not supported in Hubitat
 *2/12/18
 * 		Updated to new detailsMap and modified Button Config/Preview pages
 *		hides secondary values if primary not set. When dispayed they are now "required". 
 *
 *2/12/18
 *		Switched to parent/child config	
 *		removed button pics and descriptive text (not utilized by hubitat)
 */
def version(){"v0.2.180801"}

definition(
    name: "ABC Button Mapping",
    namespace: "stephack",
    author: "Stephan Hackett",
    description: "Assign tasks to your Button Controller Devices",
    category: "My Apps",
    parent: "stephack:Advanced Button Controller",
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
            input "buttonDevice", "capability.pushableButton", title: "${getImage("Device", "50")}Button Device", description: "Tap to Select", multiple: false, required: true, submitOnChange: true
		}
        if(buttonDevice){
        	state.buttonType =  buttonDevice.typeName
            if(state.buttonType.contains("Aeon Minimote")) state.buttonType =  "Aeon Minimote"
            log.debug "Device Type is now set to: "+state.buttonType
            state.buttonCount = manualCount?: buttonDevice.currentValue('numberOfButtons')
            section("Step 2: Configure Your Buttons"){
            	if(state.buttonCount<1) {
                	paragraph "The selected button device did not report the number of buttons it has. Please specify in the Advanced Config section below."
                }
                else {
                	for(i in 1..state.buttonCount){
                		href "configButtonsPage", title: "${getImage("Button", "20")} Button ${i}", state: getDescription(i)!="Tap to configure"? "complete": null, description: getDescription(i), params: [pbutton: i]
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
        def myDetail
        section("\n${getImage("Switches", "36")} SWITCHES"){}
        for(i in 1..25) {//Build 1st 25 Button Config Options
        	myDetail = getPrefDetails().find{it.sOrder==i}
        	//
    section(title: getImage(myDetail.secLabel, "10") + myDetail.secLabel, hideable: true, hidden: !(shallHide("${myDetail.id}${buttonNumber}"))) {
				if(showPush(myDetail.desc)) input "${myDetail.id}${buttonNumber}_pushed", myDetail.cap, title: "When Pushed", multiple: myDetail.mul, required: false, submitOnChange: collapseAll, options: myDetail.opt
				if(myDetail.sub && isReq("${myDetail.id}${buttonNumber}_pushed")) input "${myDetail.sub}${buttonNumber}_pushed", myDetail.subType, title: myDetail.sTitle, multiple: false, required: isReq("${myDetail.id}${buttonNumber}_pushed"), description: myDetail.sDesc, options: myDetail.subOpt
                if(myDetail.sub2 && isReq("${myDetail.id}${buttonNumber}_pushed")) input "${myDetail.sub2}${buttonNumber}_pushed", myDetail.subType, title: myDetail.s2Title, multiple: false, required: isReq("${myDetail.id}${buttonNumber}_pushed"), description: myDetail.s2Desc, options: myDetail.subOpt
                if(myDetail.sub3 && isReq("${myDetail.id}${buttonNumber}_pushed")) input "${myDetail.sub3}${buttonNumber}_pushed", title: myDetail.s3Title, multiple: false, required: isReq("${myDetail.id}${buttonNumber}_pushed"), description: myDetail.s3Desc, myDetail.subOpt
				
        		if(showHeld(myDetail.desc)) input "${myDetail.id}${buttonNumber}_held", myDetail.cap, title: "When Held", multiple: myDetail.mul, required: false, submitOnChange: collapseAll, options: myDetail.opt
                if(myDetail.sub && isReq("${myDetail.id}${buttonNumber}_held")) input "${myDetail.sub}${buttonNumber}_held", myDetail.subType, title: myDetail.sTitle, multiple: false, required: isReq("${myDetail.id}${buttonNumber}_held"), description: myDetail.sDesc, options: myDetail.subOpt
                	
        		if(showDouble(myDetail.desc)) input "${myDetail.id}${buttonNumber}_doubleTapped", myDetail.cap, title: "When Double Tapped", multiple: myDetail.mul, required: false, submitOnChange: collapseAll, options: myDetail.opt
                if(myDetail.sub && isReq("${myDetail.id}${buttonNumber}_doubleTapped")) input "${myDetail.sub}${buttonNumber}_doubleTapped", myDetail.subType, title: myDetail.sTitle, multiple: false, required: isReq("${myDetail.id}${buttonNumber}_held"), description: myDetail.sDesc, options: myDetail.subOpt
                
        		if(showRelease(myDetail.desc)) input "${myDetail.id}${buttonNumber}_released", myDetail.cap, title: "When Released", multiple: myDetail.mul, required: false, submitOnChange: collapseAll, options: myDetail.opt
                if(myDetail.sub && isReq("${myDetail.id}${buttonNumber}_released")) input "${myDetail.sub}${buttonNumber}_released", myDetail.subType, title: myDetail.sTitle, multiple: false, required: isReq("${myDetail.id}${buttonNumber}_released"), description: myDetail.sDesc, options: myDetail.subOpt
			}
            if(i==3) section("${getImage("Dimmers", "36")} DIMMERS"){}
            if(i==9) section("${getImage("Color", "36")} COLOR LIGHTS"){}
            if(i==11) section("${getImage("Speakers", "36")} SPEAKERS"){}
            if(i==17) section("${getImage("Fans", "36")} FANS"){}
            if(i==20) section("${getImage("Mode", "36")} MODES"){}
            if(i==22) section("${getImage("Other", "36")} OTHER"){} 
        }
		/*
		def phrases = location.helloHome?.getPhrases()*.label
		if (phrases) {
        	section("Run Routine", hideable: true, hidden: !shallHide("phrase_${buttonNumber}")) {
				//log.trace phrases
				input "phrase_${buttonNumber}_pushed", "enum", title: "When Pushed", required: false, options: phrases, submitOnChange: collapseAll
				if(showHeld())input "phrase_${buttonNumber}_held", "enum", title: "When Held", required: false, options: phrases, submitOnChange: collapseAll
                if(showDouble()) input "phrase_${buttonNumber}_doubleTapped", "enum", title: "When Double Tapped", required: false, options: phrases, submitOnChange: collapseAll
                if(showRelease())input "phrase_${buttonNumber}_released", "enum", title: "When Released", required: false, options: phrases, submitOnChange: collapseAll
			}
		}
        */
        section("${getImage("SMS", "36")}Notifications (SMS):        ", hideable:true , hidden: !shallHide("notifications_${buttonNumber}")) {
        paragraph "****************\nPUSHED\n****************"
			input "notifications_${buttonNumber}_pushed", "text", title: "Message To Send When Pushed:", description: "Enter message to send", required: false, submitOnChange: collapseAll
            input "phone_${buttonNumber}_pushed","phone" ,title: "Send Text To:", description: "Enter phone number", required: false, submitOnChange: collapseAll
            //input "valNotify${buttonNumber}_pushed","bool" ,title: "Notify in App?", required: false, defaultValue: false, submitOnChange: collapseAll
            if(showHeld()) {
            	paragraph "\n\n*************\nHELD\n*************"
				input "notifications_${buttonNumber}_held", "text", title: "Message To Send When Held:", description: "Enter message to send", required: false, submitOnChange: collapseAll
				input "phone_${buttonNumber}_held", "phone", title: "Send Text To:", description: "Enter phone number", required: false, submitOnChange: collapseAll
				//input "valNotify${buttonNumber}_held", "bool", title: "Notify in App?", required: false, defaultValue: false, submitOnChange: collapseAll
            }
            if(showDouble()) {
            	paragraph "\n\n*************************\nDOUBLE TAPPED\n*************************"
				input "notifications_${buttonNumber}_doubleTapped", "text", title: "Message To Send When Double Tapped:", description: "Enter message to send", required: false, submitOnChange: collapseAll
				input "phone_${buttonNumber}_doubleTapped", "phone", title: "Send Text To:", description: "Enter phone number", required: false, submitOnChange: collapseAll
				//if(showDouble())input "valNotify${buttonNumber}_doubleTapped", "bool", title: "Notify in App?", required: false, defaultValue: false, submitOnChange: collapseAll           
            }
            if(showRelease()) {
            	paragraph "\n\n*************\nRELEASED\n*************"
				input "notifications_${buttonNumber}_released", "text", title: "Message To Send When Released:", description: "Enter message to send", required: false, submitOnChange: collapseAll
				input "phone_${buttonNumber}_released", "phone", title: "Send Text To:", description: "Enter phone number", required: false, submitOnChange: collapseAll
				//input "valNotify${buttonNumber}_released", "bool", title: "Notify in App?", required: false, defaultValue: false, submitOnChange: collapseAll
            }
		}
    	section("${getImage("Speech", "36")}Notifications (Speech):    ", hideable:true , hidden: !shallHide("speechDevice_${buttonNumber}")) {
        paragraph "****************\nPUSHED\n****************"
        	input "speechDevice_${buttonNumber}_pushed","capability.speechSynthesis" ,title: "Send Message To:", description: "Select Speech Device", required: false, submitOnChange: collapseAll
        	input "speechTxt${buttonNumber}_pushed", "text", title: "Message To Speak When Pushed:", description: "Enter message to speak", required: false, submitOnChange: collapseAll
        	if(showHeld()) {
             	paragraph "\n\n*************\nHELD\n*************"
			 	input "speechDevice_${buttonNumber}_held", "capability.speechSynthesis", title: "Send Message To:", description: "Select Speech Device", required: false, submitOnChange: collapseAll
             	input "speechTxt${buttonNumber}_held", "text", title: "Message To Speak When Held:", description: "Enter message to speak", required: false, submitOnChange: collapseAll
            }
            if(showDouble()) {
            	paragraph "\n\n*************************\nDOUBLE TAPPED\n*************************"
				input "speechDevice_${buttonNumber}_doubleTapped", "capability.speechSynthesis", title: "Send Message To:", description: "Select Speech Device", required: false, submitOnChange: collapseAll
                input "speechTxt${buttonNumber}_doubleTapped", "text", title: "Message To Speak When Double Tapped:", description: "Enter message to speak", required: false, submitOnChange: collapseAll
            }
            if(showRelease()) {
             	paragraph "\n\n*************\nRELEASED\n*************"
			 	input "speechDevice_${buttonNumber}_released", "capability.speechSynthesis", title: "Send Message To:", description: "Select Speech Device", required: false, submitOnChange: collapseAll
             	input "speechTxt${buttonNumber}_released", "text", title: "Message To Speak When Released:", description: "Enter message to speak", required: false, submitOnChange: collapseAll
            }
		}       
	}
}

def getImage(type, mySize) {
    def loc = "<img src=https://raw.githubusercontent.com/stephack/Hubitat/master/resources/images/"
    if(type.contains("Device")) return "${loc}Device.png height=${mySize} width=${mySize}>   "
    if(type.contains("Button")) return "${loc}Button.png height=${mySize} width=${mySize}>   "
    if(type.contains("Switches")) return "${loc}Switches.png height=${mySize} width=${mySize}>   "
    if(type.contains("Color")) return "${loc}Color.png height=${mySize} width=${mySize}>   "
    if(type.contains("Dimmers")) return "${loc}Dimmers.png height=${mySize} width=${mySize}>   "
    if(type.contains("Speakers")) return "${loc}Speakers.png height=${mySize} width=${mySize}>   "
    if(type.contains("Fans")) return "${loc}Fans.png height=${mySize} width=${mySize}>   "
    if(type.contains("HSM")) return "${loc}Mode.png height=${mySize} width=${mySize}>   "
    if(type.contains("Mode")) return "${loc}Mode.png height=${mySize} width=${mySize}>   "
    if(type.contains("Other")) return "${loc}Other.png height=${mySize} width=${mySize}>   "
    if(type.contains("Locks")) return "${loc}Locks.png height=30 width=30>   "
    if(type.contains("Sirens")) return "${loc}Sirens.png height=30 width=30>   "
    if(type.contains("Shades")) return "${loc}Shades.png height=30 width=30>   "
    if(type.contains("SMS")) return "${loc}SMS.png height=30 width=30>   "
    if(type.contains("Speech")) return "${loc}Audio.png height=30 width=30>   "
}

def shallHide(myFeature) {
	if(collapseAll) return (settings["${myFeature}_pushed"]||settings["${myFeature}_held"]||settings["${myFeature}_doubleTapped"]||settings["${myFeature}_released"]||settings["${myFeature}"])
	return true
}

def isReq(myFeature) {
    (settings[myFeature])? true : false
}

def showPush(desc) {
    if(buttonDevice.capabilities.find{it.name=="PushableButton"}){ 	//is device pushable?
        if(desc.contains("Ramp")){									
            if(buttonDevice.capabilities.find{it.name=="HoldableButton"}) return false	//if this is the Ramp section and device is also Holdable, then hide Pushed option
        }
        return true
    }
	return false
}

def showHeld(desc) {
    def devCaps = buttonDevice.capabilities.find{it.name=="HoldableButton"}
    if(devCaps) return true
	return false
}

def showDouble(desc) {
    if(desc && desc.contains("Ramp")) return false //remove DoubleTapped option when setting smooth dimming button/devices
    def devCaps = buttonDevice.capabilities.find{it.name=="DoubleTapableButton"}
    if(devCaps) return true
	return false
}

def showRelease(desc) {
    if(desc && desc.contains("Ramp")) return false //remove On Release option when setting smooth dimming button/devices
    def devCaps = buttonDevice.capabilities.find{it.name=="ReleasableButton"}
    if(devCaps) return true
	return false
}

def getDescription(dNumber) {
    def descript = ""
    if(!(settings.find{it.key.contains("_${dNumber}_")})) return "Tap to configure"
    if(settings.find{it.key.contains("_${dNumber}_pushed")}) descript = "\nPUSHED:"+getDescDetails(dNumber,"_pushed")+"\n"
    if(settings.find{it.key.contains("_${dNumber}_held")}) descript = descript+"\nHELD:"+getDescDetails(dNumber,"_held")+"\n"
    if(settings.find{it.key.contains("_${dNumber}_doubleTapped")}) descript = descript+"\nTAPx2:"+getDescDetails(dNumber,"_doubleTapped")+"\n"
    if(settings.find{it.key.contains("_${dNumber}_released")}) descript = descript+"\nRELEASED:"+getDescDetails(dNumber,"_released")+"\n"
	return descript
}

def getDescDetails(bNum, type){
	def numType=bNum+type
	def preferenceNames = settings.findAll{it.key.contains("_${numType}")}.sort()		//get all configured settings that: match button# and type, AND are not false
    if(!preferenceNames){
    	return "  **Not Configured** "
    }
    else {
    	def formattedPage =""
    	preferenceNames.each {eachPref->
        	def prefDetail = getPrefDetails().find{eachPref.key.contains(it.id)}	//gets decription of action being performed(eg Turn On)
        	def prefDevice = " : ${eachPref.value}" - "[" - "]"											//name of device the action is being performed on (eg Bedroom Fan)
            def prefSubValue = settings[prefDetail.sub + numType]?:"(!Missing!)"
            def sub2Value = settings[prefDetail.sub2 + numType]
            def sub3Value = settings[prefDetail.sub3 + numType]
            if(sub2Value) prefSubValue += ", S: ${sub2Value}"
            if(sub3Value) prefSubValue += ", L: ${sub3Value}"
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
    log.info app.label
    if(!app.label || app.label == "default")app.updateLabel(defaultLabel())
	//app.label==app.name?app.updateLabel(defaultLabel()):app.updateLabel(app.label)
	subscribe(buttonDevice, "pushed", buttonEvent)
	subscribe(buttonDevice, "held", buttonEvent)
	subscribe(buttonDevice, "doubleTapped", buttonEvent)
    subscribe(buttonDevice, "released", buttonEvent)
    state.lastshadesUp = true
}

def defaultLabel() {
	return "${buttonDevice} Mapping"
}

def getPrefDetails(){
	def detailMappings =
    	[[id:'lightOn_', sOrder:1, desc:'Turn On ', comm:turnOn, type:"normal", secLabel:     "Switches (Turn On)          ", cap: "capability.switch", mul: true],
     	 [id:"lightOff_", sOrder:2, desc:'Turn Off', comm:turnOff, type:"normal", secLabel:   "Switches (Turn Off)          ", cap: "capability.switch", mul: true],
         [id:'lights_', sOrder:3, desc:'Toggle On/Off', comm:toggle, type:"normal", secLabel: "Switches (Toggle On/Off)", cap: "capability.switch", mul: true],
         
         [id:"lightDim_", sOrder:4, desc:'Dim to ', comm:turnDim, sub:"valLight", subType:"number", type:"hasSub", secLabel: "Dimmers (On to Level - Group 1)", cap: "capability.switchLevel", sTitle: "Bright Level", sDesc:"0 to 100%", mul: true],
     	 [id:"lightD2m_", sOrder:5, desc:'Dim to ', comm:turnDim, sub:"valLight2", subType:"number", type:"hasSub", secLabel: "Dimmers (On to Level - Group 2)", cap: "capability.switchLevel", sTitle: "Bright Level", sDesc:"0 to 100%", mul: true],
         [id:'dimPlus_', sOrder:6, desc:'Brightness +', comm:levelUp, sub:"valDimP", subType:"number", type:"hasSub", secLabel: "Dimmers (Increase Level By)      ", cap: "capability.switchLevel", sTitle: "Increase by", sDesc:"0 to 15", mul: true],
     	 [id:'dimMinus_', sOrder:7, desc:'Brightness -', comm:levelDown, sub:"valDimM", subType:"number", type:"hasSub", secLabel: "Dimmers (Decrease Level By)    ", cap: "capability.switchLevel", sTitle: "Decrease by", sDesc:"0 to 15", mul: true],
         [id:'lightsDT_', sOrder:8, desc:'Toggle Off/Dim to ', comm:dimToggle, sub:"valDT", subType:"number", type:"hasSub", secLabel: "Dimmers (Toggle OnToLevel/Off)", cap: "capability.switchLevel", sTitle: "Bright Level", sDesc:"0 to 100%", mul: true],
         [id:'lightsRamp_', sOrder:9, desc:'Ramp ', comm:rampUp, sub:"valDir", subType:"enum", subOpt:['up','down'], type:"hasSub", secLabel: "Dimmers (Ramp Up/Down)         ", cap: "capability.changeLevel", sTitle: "Ramp Direction (Up/Down)", sDesc:"Up or Down", mul: true],
         
         [id:'lightColorTemp_', sOrder:10, desc:'Set Light Color Temp to ', comm:colorSetT, sub:"valColorTemp", subType:"number", type:"hasSub", secLabel: "Color Lights (Set Temp To)", cap: "capability.colorTemperature", sTitle: "Color Temp", sDesc:"2000 to 9000", mul: true],
         [id:'lightColor_', sOrder:11, desc:'Set Light Color H:', comm:colorSet, sub:"valHue", subType:"number", sub2:"valSat", sub3:"valLvl", type:"hasSub", secLabel: "Color Lights (Set Color To) ", cap: "capability.colorControl", sTitle: "Hue", s2Title: "Saturation", s3Title: "Level", sDesc:"0 to 100", s2Desc:"0 to 100", s3Desc:"0 to 100", mul: true],
     	          
         [id:"speakerpp_", sOrder:12, desc:'Toggle Play/Pause', comm:speakerplaystate, type:"normal", secLabel: "Speakers (Toggle Play/Pause)", cap: "capability.musicPlayer", mul: true],
     	 [id:'speakervu_', sOrder:13, desc:'Volume +', comm:levelUp, sub:"valSpeakU", subType:"number", type:"hasSub", secLabel: "Speakers (Increase Vol By)     ", cap: "capability.musicPlayer", sTitle: "Increase by", sDesc:"0 to 15", mul: true],
     	 [id:"speakervd_", sOrder:14, desc:'Volume -', comm:levelDown, sub:"valSpeakD", subType:"number", type:"hasSub", secLabel: "Speakers (Decrease Vol By)   ", cap: "capability.musicPlayer", sTitle: "Decrease by", sDesc:"0 to 15", mul: true],
         [id:'speakernt_', sOrder:15, desc:'Next Track', comm:speakernexttrack, type:"normal", secLabel: "Speakers (Go to Next Track)   ", cap: "capability.musicPlayer", mul: true],
    	 [id:'speakermu_', sOrder:16, desc:'Mute', comm:speakermute, type:"normal", secLabel: "Speakers (Toggle Mute)          ", cap: "capability.musicPlayer", mul: true],
         [id:"musicPreset_", sOrder:17, desc:'Cycle Preset', comm:cyclePlaylist, type:"normal", secLabel: "Speakers Preset to Cycle        ", cap: "capability.switch", mul: true],         
         
         [id:'fanSet_', sOrder:18, desc:'Set Fan to ', comm:setFan, sub:"valSpeed", subType:"enum", subOpt:['off','low','medium-low','medium','high'], type:"hasSub", secLabel: "Fans (Set Speed)          ", cap: "capability.fanControl", sTitle: "Set Speed to", sDesc:"L/ML/M/H", mul: true],
         [id:"fanCycle_", sOrder:19, desc:'Cycle Fan Speed', comm:cycle, type:"normal", secLabel: "Fans to Cycle Speed     ", cap: "capability.fanControl", mul: true],         
         [id:"fanAdjust_", sOrder:20,desc:'Adjust', comm:adjustFan, type:"normal", secLabel: "Fans to Cycle (Legacy)", cap: "capability.switchLevel", mul: true],
         
         [id:"mode_", sOrder:21, desc:'Set Mode', comm:changeMode, type:"normal", secLabel: "Set Mode               ", cap: "mode", mul: false],
     	 [id:"hsm_", sOrder:22, desc:'Set HSM', comm:setHSM, type:"normal", secLabel: "Set HSM               ", cap: "enum", opt:['armAway','armHome','disarm','armRules','disarmRules','disarmAll','armAll','cancelAlerts'], mul: false],
         
         [id:"locks_", sOrder:23, desc:'Lock', comm:setUnlock, type:"normal", secLabel: "Locks (Lock Only)          ", cap: "capability.lock", mul: true],
         [id:"shadeAdjust_", sOrder:24,desc:'Adjust', comm:adjustShade, type:"normal", secLabel: "Shades (Up/Down/Stop)", cap: "capability.doorControl", mul: true],
         [id:'sirens_', sOrder:25, desc:'Toggle', comm:toggle, type:"normal", secLabel: "Sirens (Toggle)               ", cap: "capability.alarm", mul: true],
     	 
     	 [id:"phrase_", desc:'Run Routine', comm:runRout, type:"normal"],
		 [id:"notifications_", desc:'Send Push Notification', comm:messageHandle, sub:"valNotify", type:"bool"],
     	 [id:"phone_", desc:'Send SMS', comm:smsHandle, sub:"notifications_", type:"normal"],
         [id:"speechDevice_", desc:'Send Msg To', comm:speechHandle, sub:"speechTxt", type:"normal"],
        ]
    return detailMappings
}

def buttonEvent(evt) {
	if(allOk) {
    	def buttonNumber = evt.value
		def pressType = evt.name
		log.debug "$buttonDevice: Button $buttonNumber was $pressType"
        
        //detects if button is used for graceful hold to dim function then calls stopLevelChange()
        if(pressType == "released" && settings["lightsRamp_${buttonNumber}_pushed"]){
        	rampEnd(settings["lightsRamp_${buttonNumber}_pushed"])
        }
        if(pressType == "released" && settings["lightsRamp_${buttonNumber}_held"]){
        	rampEnd(settings["lightsRamp_${buttonNumber}_held"])
        }        
        
    	def preferenceNames = settings.findAll{it.key.contains("_${buttonNumber}_${pressType}")}
    	preferenceNames.each{eachPref->
        	def prefDetail = getPrefDetails()?.find{eachPref.key.contains(it.id)}		//returns the detail map of id,desc,comm,sub
        	def PrefSubValue = settings["${prefDetail.sub}${buttonNumber}_${pressType}"] //value of subsetting (eg 100)
            def PrefSub2Value = settings["${prefDetail.sub2}${buttonNumber}_${pressType}"] //value of subsetting (eg 100)
            def PrefSub3Value = settings["${prefDetail.sub3}${buttonNumber}_${pressType}"]	//value of subsetting (eg 100)
        	if(prefDetail.sub3) "$prefDetail.comm"(eachPref.value,PrefSubValue, PrefSub2Value, PrefSub3Value)
            	else if(prefDetail.sub2) "$prefDetail.comm"(eachPref.value,PrefSubValue, PrefSub2Value)
        		else if(prefDetail.sub) "$prefDetail.comm"(eachPref.value,PrefSubValue)
        	else "$prefDetail.comm"(eachPref.value)
    	}
	}
}

def speechHandle(devices, msg){
    log.debug "Sending ${msg} to ${device}"
    devices.speak(msg)
    
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

def colorSet(devices,hueVal,satVal,lvlVal) {
    log.debug "Setting Color (to H:$hueVal, S:$satVal, L:$lvlVal): $devices"
    def myColor = [:]
    myColor.hue = hueVal.toInteger()
    myColor.saturation = satVal.toInteger()
    myColor.level = lvlVal.toInteger()
    //log.info myColor
    devices.setColor(myColor)//([hue:hueVal,saturation:satVal,level:50]) 
}

def colorSetT(devices, temp) {
    log.debug "Setting Color Temp (to $temp): $devices"
    devices.setColorTemperature(temp)    
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

def setFan(devices, speed){
    devices.setSpeed(speed)
}

def speakerplaystate(device) {
	log.debug "Toggling Play/Pause: $device"
    //log.info device.currentDriverType[0]
    //if(device.currentDriverType[0] == "custom"){device.togPlay()}
    //else{
		device.currentStatus.contains('playing')? device.pause() : device.play()
    //}
    
}
   
def speakernexttrack(device) {
	log.debug "Next Track Sent to: $device"
	device.nextTrack()
}   

def speakermute(device) {
	log.debug "Toggling Mute/Unmute: $device"
	device.currentMute.contains('unmuted')? device.mute() : device.unmute()
} 

def levelUp(device, inclevel) {
	log.debug "Incrementing Level (by +$inclevel: $device"
	def currentVol = device.currentLevel[0]//device.currentValue('level')[0]	//currentlevel return a list...[0] is first item in list ie volume level
    def newVol = currentVol + inclevel
  	device.setLevel(newVol)
    log.debug "Level increased by $inclevel to $newVol"
}

def levelDown(device, declevel) {
	log.debug "Decrementing Level (by -declevel: $device"
	def currentVol = device.currentLevel[0]//device.currentValue('level')[0]
    def newVol = currentVol - declevel
  	device.setLevel(newVol)
    log.debug "Level decreased by $declevel to $newVol"
}

def rampUp(devices, dir){
    log.info " ramping ${dir}"
    devices.startLevelChange(dir)
}

def rampEnd(device){
    device.stopLevelChange()    
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

def setHSM(hsmMode) {
    sendLocationEvent(name: "hsmSetArm", value: hsmMode)

}

def changeMode(mode) {
	log.debug "Changing Mode to: $mode"
	if (location.mode != mode && location.modes?.find { it.name == mode[0] }) setLocationMode(mode)
}

def cycle(devices) {
    log.debug "Cycling: $devices"
    log.info devices.currentSpeed
    def mySpeed = devices.currentSpeed
    if(mySpeed[0] == "off") devices.setSpeed(1) 
    if(mySpeed[0] == "low") devices.setSpeed(2) 
    if(mySpeed[0] == "medium-low") devices.setSpeed(3) 
    if(mySpeed[0] == "medium") devices.setSpeed(4)
    if(mySpeed[0] == "high") devices.setSpeed(0) 
}

def cyclePlaylist(devices){
    devices.cycle()
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
