/**
 *  Virtual Container Generator
 *
 *  Copyright 2017 Stephan Hackett
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
 *	Icons by http://www.icons8.com	
 *
 */
 
def version() {return "0.1.20171106"}

definition(
    name: "Virtual Container Generator",
    namespace: "stephack",
    author: "Stephan Hackett",
    description: "Creates a container for virtual device types. Prevents clutter and easy management.",
    category: "My Apps",
    iconUrl: "https://cdn.rawgit.com/stephack/Virtual/master/resources/images/vcg.png",
    iconX2Url: "https://cdn.rawgit.com/stephack/Virtual/master/resources/images/vcg.png",
    iconX3Url: "https://cdn.rawgit.com/stephack/Virtual/master/resources/images/vcg.png") 


preferences {
	page(name: "startPage")
	page(name: "parentPage")
	page(name: "mainPage", nextPage: confirmOptions)
	page(name: "confirmOptions", title: "Confirm All Settings Below")
    page(name: "aboutPage")
}

def startPage() {
    if (parent) {
        mainPage()
    } else {
        parentPage()
    }
}

def parentPage() {
	return dynamicPage(name: "parentPage", title: "", nextPage: "", install: true, uninstall: true) {
        section("Installed Conatiners") {
            app(name: "childApps", appName: appName(), namespace: "stephack", title: "Create New Conatiner", multiple: true)
        }
        section("Version Info & User's Guide") {
       		href (name: "aboutPage", 
       		title: "Virtual Container Generator\nver "+version(), 
       		description: "Tap for User's Guide and Info.",
       		image: "https://cdn.rawgit.com/stephack/Virtual/master/resources/images/vcg.png",
       		required: false,
       		page: "aboutPage"
 	   		)
      	}
    }
}

private def appName() { return "${parent ? "VC Config" : "Virtual Container Generator"}" }

def mainPage(){
	dynamicPage(name:"mainPage", uninstall:true){
        if(!vbTotal || !vLabel1){
        	section("Step 1:"){
        		input name: "vType", type: "enum", title: "Choose Virtual Device Type", submitOnChange: true, options: ["Momentary Button", "Switch"]
        	}
        }
        else if(vbTotal){
        	section(""){
            	def pic = "${vType.toLowerCase()}"-" "+"on"
                paragraph image: "https://cdn.rawgit.com/stephack/Virtual/master/resources/images/${pic}a.png",
                  title: "$vType Container",
                  required: true,
                  "Container Type Cannot Be Changed!"
            }
        }
        
    	if(vType){        
        	section("Step 2:") {
				input "vbTotal", "number", title: "How many to create", description:"Enter number: (1-6)", multiple: false, submitOnChange: true, required: true, range: "1..6"
			}
			if(vbTotal>=1 && vbTotal<=6){
				section("Step 3:"){
                	for(i in 1..vbTotal) {
						input "vLabel${i}", "text", title: "Virtual $vType ${i}", description: "Enter Name Here", multiple: false, required: true
					}
				}
			}
			else if(vbTotal){
				section{paragraph "Please choose a value between 1 and 6."}
			}       
        }        
	}
}

def confirmOptions(){
	dynamicPage(name:"confirmOptions",install:true, uninstall:true){    
		section("Type of Container Being Created:"){
        		paragraph "${vType}"
		}
        section("Devices Being Generated:"){
        	def myMap=[]
            for(i in 1..vbTotal) {
           		def childLabel = app."vLabel${i}"
                myMap << childLabel
       		}
            paragraph "$myMap"
        }
        if(state.oldVbTotal>vbTotal){
        	section("Devices Being Deleted:"){
            	def myMap = []
                for(i in vbTotal+1..state.oldVbTotal) {
            		def childLabel = app."vLabel${i}"
                    myMap << childLabel
                }
        		paragraph "$myMap"
       		}
          }       	
 	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
    if(parent) { 
    	initChild() 
    } else {
    	initParent() 
    }  
}

def initChild() {
	app.label==app.name?app.updateLabel(defaultLabel()):app.updateLabel(app.label)    
    createContainer()
    state.oldVbTotal = vbTotal //keep track of changes to vbTotal to manage confirmOptions display of what will be deleted
    log.debug "Initialization Complete"
    if(vbTotal) createVB(vType)
}

def initParent() {
	log.debug "Parent Initialized"
}

def defaultLabel() {
	return "Virtual $vType Container"
}

def createContainer() {
	log.info "Creating Virtual Container"
    def childDevice = getAllChildDevices()?.find {it.device.deviceNetworkId == "VC_${app.id}"}        
    if (!childDevice) {
    	childDevice = addChildDevice("stephack", "Virtual Container", "VC_${app.id}", null,[completedSetup: true,
        label: app.label]) 
        log.info "Created Container [${childDevice}]"
        childDevice.sendEvent(name:"level", value: "1")
        for(i in 1..6){childDevice.sendEvent(name:"vlabel${i}", value:"--")}	///sets defaults for attributes...needed for inconsistent IOS tile display
	}
    else {
    	childDevice.label = app.label
        childDevice.name = app.label
        log.info "Container renamed to [${app.label}]"
	}
}

def createVB(vType){	//creates Virtual Switches of type: vType
    def vbInfo = []
	for(i in 1..vbTotal) {
    	vbInfo << [id:i, name:(app."vLabel${i}")]
    }
    def childDevice = getAllChildDevices()?.find {it.device.deviceNetworkId == "VC_${app.id}" }        
    childDevice.createChildVB(vbTotal,vbInfo,vType)
}

def containerOn(which) {	//when child # (which) is turned ON, this code will be executed
}


def containerOff(which) {	//when child # (which) is turned OFF, this code will be executed (not used by momentaries)
}

def containerLevel(val, which) {	//when child # (which) changes level to (val), this code should be excuted

}

def aboutPage() {
	dynamicPage(name: "aboutPage", title: none) {
     	section("User's Guide: Virtual Container Generator") {
        	paragraph "This smartApp allows you to creates containers for virtual device types. It Helps to prevent clutter and make management of multiple virtual devices easier."
        }        
		section("Other Stuff:About"){
            paragraph "Icons by Icons8 - http://www.icons8.com"
 		}
	}
}
