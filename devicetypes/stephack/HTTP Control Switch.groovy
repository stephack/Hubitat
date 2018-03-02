/**
 *  HTTP Control Switch
 *
 *  Copyright 2018 Stephan Hackett
 *
 *
 * 
 */
metadata {
	definition (name: "HTTP Control Switch", namespace: "stephack", author: "Stephan Hackett") {
        capability "Switch"
	}

	preferences {
	}
}

def parse(String description) {
	log.debug(description)
}

def on() {
    parent.childOn()
}

def off() {
    parent.childOff()
}


def installed(){}
