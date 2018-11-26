/*	
 *
 *	ESPIR Controller
 *
 *	Author: Stephan Hackett
 * 
 * 
 */
def version(){"v0.2.180923"}

definition(
    name: "EspIR Controller",
    namespace: "stephack",
    author: "Stephan Hackett",
    description: "Configures EspIR devices with codes etc",
    category: "My Apps",
    parent: "stephack:EspIR Manager",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences {
	page(name: "configureESP", nextPage: "configControl")
    page(name: "configControl")
	page(name: "configCommandPage")
    page(name: "configIRCommand")
    page(name: "configMacroPage")
    page(name: "configMacro")
    
	
}

def configureESP() {
	dynamicPage(name: "configureESP", install: false, uninstall: true) {
		section("Step 1: Configure you ESP Server:") {
            input("myIp", "text", description: "", title: "${getImage("Device", "50")}ESP IP:", required: true, submitOnChange: true)
            input("pass", "text", title: "Password:", description: "")
		}
        if(myIp){
            section("\n\nStep 2: Configure Your IR Commands"){
                	href "configCommandPage", title: "Configure/Save IR Commands"
          	}
        }
        if(numCommand){
            section("\n\nStep 3: Configure Your IR Macros"){
                	href "configMacroPage", title: "Configure Macro Commands"
          	}
        }
	}
}

def configCommandPage() {
    dynamicPage(name: "configCommandPage", title: "CONFIGURE IR Commands:") {
     	section(""){
            input("numCommand", "number", title: "Number of Commands to Create:", description: "", required: true, submitOnChange: true)
            if(numCommand){
            	for(i in 1..numCommand){
            		href "configIRCommand", title: "${getImage("Button", "20")} Command ${i}", params: [codeNum: i]//, state: getDescription(i)!="Tap to configure"? "complete": null, description: getDescription(i)
            	}   
            }
    	}
    }
}

def configIRCommand(params) {
    dynamicPage(name: "configIRCommand", title: "CONFIGURE IR Code ${params.codeNum}:", getCommandSections(params.codeNum)) 
}


def configMacroPage() {
    dynamicPage(name: "configMacroPage", title: "CONFIGURE IR Macro:") {
     	section(""){
            input("numMacro", "number", title: "Number of Macros to Create:", description: "", required: true, submitOnChange: true)
            if(numMacro){
            	for(i in 1..numMacro){
            		href "configMacro", title: "${getImage("Button", "20")} Macro ${i}", params: [macroNum: i]//, state: getDescription(i)!="Tap to configure"? "complete": null, description: getDescription(i)
            	}   
            }
    	}
    }
}

def configMacro(params) {
    dynamicPage(name: "configMacro", title: "CONFIGURE Macro ${params.macroNum}:", getMacroSections(params.macroNum)) 
}

def configControl() {
    dynamicPage(name: "configControl", title: "CONFIGURE CONTROL:", install: true, uninstall: true){
    
        //if(numCommand){
        //    href "configIRCommand", title: "${getImage("Button", "20")} Command ${i}", params: [codeNum: i]
       // }
        
        section("Set Custom Name (Optional)") {
            
        	label title: "Assign a name:", required: false
        }
	}
}

def getCommandSections(codeNum) {
    return{
    section(""){
		input("irLabel${codeNum}", "text", title: "Name of Command:", description: "Name to give this IR command", required: true)
        input("irCode${codeNum}", "text", title: "IR Command:", description: "IR code data, may be simple HEX code such as 'A90' or an array of int values when transmitting a RAW sequence")
    	input("irType${codeNum}", "text", title: "Type:", description: "Type of signal transmitted. Example 'SONY', 'RAW', 'Delay' or 'Roomba' (and many others)")
    	input("irLength${codeNum}", "number", title: "Length:", description: "(conditional) Bit length, example 12. Parameter does not need to be specified for RAW or Roomba signals")
    	input("irPulse${codeNum}", "number", title: "Pulse:", description: "(optional) Repeat a signal rapidly. Default 1")
   		input("irPDdelay${codeNum}", "number", title: "Pulse Delay:", description: "(optional) Delay between pulses in milliseconds. Default 100")
    	input("irRepeat${codeNum}", "number", title: "Repeat:", description: "(optional) Number of times to send the signal. Default 1. Useful for emulating multiple button presses for functions like large volume adjustments or sleep timer")
    	input("irRDelay${codeNum}", "number", title: "Repeat Delay:", description: "(optional) Delay between repeats in milliseconds. Default 1000")
    	input("irKhz${codeNum}", "number", title: "Transmission Freq.:", description: "(conditional) Transmission frequency in kilohertz. Default 38. Only required when transmitting RAW signal")
    	input("irOut${codeNum}", "number", title: "Output Pin:", description: "(optional) Set which IRsend present to transmit over. Default 1. Choose between 1-4. Corresponding output pins set in the blueprint. Useful for a single ESP8266 that needs multiple LEDs pointed in different directions to trigger different devices.")
    }
    }
	
}

def getMacroSections(macroNum) {
    return{
    section(""){
        input("macroLabel${macroNum}", "text", title: "Name of Macro:", description: "Name to give this IR Macro", required: true, submitOnChange: true)
        input("numMacComm${macroNum}", "number", title: "Number of Commands for this Macro:", description: "Number of Commands for this Macro", required: true, submitOnChange: true)
        
        if(app."numMacComm${macroNum}"){
            for(i in 1..app."numMacComm${macroNum}"){
                input("myMacro${macroNum}_${i}", "enum", title: "Command:", description: "", options: getMacroOptions(), submitOnChange: true)
                if(app."myMacro${macroNum}_${i}"=="_Wait(millisecs)"){
                	input("myMacro${macroNum}_${i}_wait", "number", title: "Milliseconds to wait:", description: "", submitOnChange: true)
                }
            }
        }
    }
    section(""){
            	input("virtSwitch${macroNum}", "capability.switch", title: "Execute this macro using which Switch:", description: "", submitOnChange: true)
            }
    section(""){
        	input("test", "button", title: "test settings")
    }
    }
	
}

def getMacroOptions() {
    def macMap =[]
    for (i in 1..numCommand){
        macMap << [name:app."irLabel$i"]
     	//log.info macMap   
    }
    macMap << [name:"_Wait(millisecs)"]
    log.info macMap
    def options = macMap.collect{it.name}.sort()
    return options
    
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

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
    formCommands()
    formMacros()
	initialize()
}

def initialize() {
    log.debug "INITIALIZED with settings: ${settings}"
    log.info app.label
    if(!app.label || app.label == "default")app.updateLabel(defaultLabel())
	//app.label==app.name?app.updateLabel(defaultLabel()):app.updateLabel(app.label)
    def virts = settings.findAll{it.key.contains("virtSwitch")}
    virts.each{
        subscribe(it.value, "switch.on", switchHandler)
        
    }
   // subscribe(virtSwitch, "switch.on", switchHandler) 
}

def switchHandler(evt){
    log.info "${evt.displayName} turned on...sending IR Commands"
    def myComms = state.savedMacros.find{it.virtB==evt.deviceId}.macro.sort()
    def myCount = myComms.size()
    def mapToSend = []
    myComms.each{
        log.info it
        if (it.value.startsWith("wait_")){
            def myDelay = it.value - "wait_"
            mapToSend << [repeat: 2, rdelay: myDelay]
        }
        else{
        	mapToSend << state.savedCommands.find{comm->comm.name==it.value}.command
        	//log.info mapToSend
        }
    }
    sendToServer(mapToSend)
}

def formCommands(){
    state.savedCommands = []
    for (i in 1..numCommand){
    	def myMap = [:]
        def myCommand = [:]
        myMap << [simple:1]
        if (app."irCode$i") myMap << [data:app."irCode$i"]
        if (app."irType$i") myMap << [type:app."irType$i"]
        if (app."irLength$i") myMap << [length:app."irLength$i"]
        if (app."irPulse$i") myMap << [pulse:app."irPulse$i"]
        if (app."irPDdelay$i") myMap << [pdelay:app."irPDdelay$i"]
        if (app."irRepeat$i") myMap << [repeat:app."irRepeat$i"]
        if (app."irRDelay$i") myMap << [rdelay:app."irRDelay$i"]
        if (app."irKhz$i") myMap << [khz:app."irKhz$i"]
        if (app."irOut$i") myMap << [out:app."irOut$i"]
		//log.info myMap
        myCommand << [name:app."irLabel$i"]
        myCommand << [command:myMap]
        state.savedCommands << myCommand
    }
}




def appButtonHandler(btn) {
	//def abc = state.savedCommands
    if(btn=="test"){
    	state.savedMacros =  []
    	for (i in 1..numMacro){
        	log.info i
        	def myMap = [:]
        	def thisMacro = [:]
        	for(int j in 1..app."numMacComm${i}"){
                def h
                if (j < 10) h= "0"+j
                else h=j
                
                log.debug h
            	if (app."myMacro${i}_${j}" == "_Wait(millisecs)"){
                	log.info "hello"
                	myMap << ["comm$j":"wait_"+app."myMacro${i}_${j}_wait"]
            	}
            	else if (app."myMacro${i}_${j}" && j < 10){
                   // def xyz = state.savedCommands.find{it.name == app."myMacro${i}_${j}"}.command
                	myMap << ["comm$j":app."myMacro${i}_${j}"]
            	}
                else if (app."myMacro${i}_${j}"){
                   // def xyz = state.savedCommands.find{it.name == app."myMacro${i}_${j}"}.command
                	myMap << ["comm$j":app."myMacro${i}_${j}"]
            	}        	}
        	log.debug myMap
        	thisMacro << [name:app."macroLabel${i}"]
        	thisMacro << [virtB:app."virtSwitch${i}".deviceId]
        	thisMacro << [macro:myMap]
        	log.info thisMacro
        	state.savedMacros << thisMacro        
    	}
    	log.debug state.savedMacros
	}    
}

def formMacros(){
    state.savedMacros =  []
    for (i in 1..numMacro){
        def myMap = [:]
        def thisMacro = [:]
        for(j in 1..app."numMacComm${i}"){
            def h
            if (j < 10) h= "0"+j
            else h=j
            if (app."myMacro${i}_${j}" == "_Wait(millisecs)"){
                myMap << ["comm$h":"wait_"+app."myMacro${i}_${j}_wait"]
            }
            else if (app."myMacro${i}_${j}"){
                myMap << ["comm$h":app."myMacro${i}_${j}"]
            }
        }
        //log.debug myMap
        thisMacro << [name:app."macroLabel${i}"]
        thisMacro << [virtB:app."virtSwitch${i}".deviceId]
        thisMacro << [macro:myMap]
        //log.info thisMacro
        state.savedMacros << thisMacro        
    }
    //log.debug state.savedMacros
}

def defaultLabel() {
	return "${myIp} Controller"
}


def sendToServer(myComm) {
    log.debug myComm
    def params = [
        uri: "http://${myIp}/json",
		contentType: "application/x-www-form-urlencoded",
        requestContentType: "application/json",
        body: myComm
        
        
    ]
    //log.debug params
        try {
            httpPost(params){response ->
                if(response.status != 200) {
                    log.error "Received HTTP error ${response.status}."
                } else {
                    log.debug "IR Server Received Commands Successfully"
                }
            }
        } catch (e){
            log.error "error:${e.getResponse().getData()}"
        }
}
