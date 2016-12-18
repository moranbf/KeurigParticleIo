/**
|  Keurig Using Particle.io Photon
|
|  Author: moranbf@gmail.com using base code from juano23@gmail.com
|  Date: 2016-12-18
|  Schematic: http://imgur.com/a/0pTzb
|
*/
 // you can get the device ID from https://console.particle.io/devices
 // token from https://build.particle.io then settings
 // You will enter them as parameters when you add a device
 
 preferences {
    input("deviceId", "text", title: "Device ID", displayDuringSetup: true)
    input("token", "text", title: "Access Token", displayDuringSetup: true)
}

metadata {
	// define the device handler name
	definition (name: "KeurigParticleIo", namespace: "moranbf", author: "moranbf@gmail.com") {
    	// set the device capabilities
        // switch is a simple device capable of on/off
        // acuator can have custom actions
        // polling allows us to poll the device to check and see if is on or off
    	capability "Switch"
        capability "Actuator"
        capability "Polling"
        // setup the commands to be executed, each is a function
        command "brewLarge"
        command "brewMedium"
        command "brewSmall"
	}

    // The tiles are what is shown on the mobile app
    // There can be two sets, the main tile, then a set of tiles when you click on the device name
    // scale:2 sets the size of the grid for the icons to 6 wide
	tiles (scale:2) {
    	// this will be the main tile/icon
        // the action switch.off will call the function off and switch.on will call the function on
		standardTile("switch", "device.switch", icon: "st.Appliances.appliances3", width: 6, height: 6, canChangeIcon: true) {
			state ("on", label: '${name}', action: "switch.off", icon: "st.Appliances.appliances3", backgroundColor: "#79b821", nextState: "off")
			state ("off", label: '${name}', action: "switch.on", icon: "st.Appliances.appliances3", backgroundColor: "#ffffff", nextState: "on")
		}
        // these will be added to the detail screen
        standardTile("brewSmall", "device.switch", width: 2, height: 2) {
    		state "brewSmall", label: "Small", icon: "st.Appliances.appliances14", backgroundColor: "#ffffff", action: "brewSmall"
        }
        standardTile("brewMedium", "device.switch", width: 2, height: 2) {
    		state "brewMedium", label: "Medium", icon: "st.Appliances.appliances14", backgroundColor: "#ffffff", action: "brewMedium"
        }
        standardTile("brewLarge", "device.switch", width: 2, height: 2) {
    		state "brewLarge", label: "Large", icon: "st.Appliances.appliances14", backgroundColor: "#ffffff", action: "brewLarge"
		}
        // this is where you define what is on the main screen and details screen
		main ("switch")
		details (["switch", "brewSmall", "brewMedium","brewLarge"])
        }
}
def parse(String description) {
	// NA 
	return null
}

def on() {
    log.error "Turning On"
    // this will send 1 to the particle power function 
	particleTurnOnOff ('1')
    // lets check the power state 1 for on 0 for off
    def powerState = particlePowerState()
    // if we are on update the state
	if (powerState == 1) {
    	sendEvent(name: "switch", value: "on", isStateChange: true, display: false, displayed: false)
        def switchState = device.currentState("switch").value
    	log.error "Device Status " + switchState
		log.error "On function return val " + 0
        return 0
    	}
    // if the powerstate is 0 its not on so make sure the switch state is set to off, this will also update the icon to off
    else if  (powerState == 0){
    	sendEvent(name: "switch", value: "off", isStateChange: true, display: false, displayed: false)
        def switchState = device.currentState("switch").value
    	log.error "Device Status " + switchState
		log.error "On function return val " + -1
        return -1
    }
}

def off() {
    log.error "Turning off"
    // this will send 0 to the particle power function 
	particleTurnOnOff ('0')
    // lets check the power state 1 for on 0 for off
    def powerState = particlePowerState()
    // if we are off update the state
	if (powerState == 0) {
    	sendEvent(name: "switch", value: "off", isStateChange: true, display: false, displayed: false)
        def switchState = device.currentState("switch").value
    	log.error "Device Status " + switchState
		log.error "Off function return val " + 0
        return 0
    	}
    // otherwise we are out of sync so set the state to on
    else if  (powerState == 1){
    	sendEvent(name: "switch", value: "on", isStateChange: true, display: false, displayed: false)
        def switchState = device.currentState("switch").value
    	log.error "Device Status " + switchState
		log.error "Off function return val " + -1
        return -1
    }
}
// functions for brewing
def brewLarge() {
   particleBrew("large")
}
def brewMedium() {
   particleBrew("medium")
}
def brewSmall() {
   particleBrew("small")
}
// this will poll the device state
def poll() {
   // Using Pollster to call poll every 5 minutes 
   // Pollster location https://github.com/statusbits/smartthings/blob/master/Pollster.md
   // Call particle powerstate, it will return 1 for on 0 for off
   def powerState = particlePowerState()
   // Next check the switch state
   def switchState = device.currentState("switch").value
   log.error "Polled"
   // If particle powerstate and switchstate are mismatched fix it
   if (powerState == 1 && switchState == "off"){
   		log.error "Updating Power On"
   		sendEvent(name: "switch", value: "on", isStateChange: true, display: false, displayed: false)
   }
   else if (powerState == 0 && switchState == "on"){
   		log.error "Updating Power Off"
   		sendEvent(name: "switch", value: "off", isStateChange: true, display: false, displayed: false)
   }
}

// wrapper for particle function power
// deviceid and token entered as parameters when a device is created in the app
private particleTurnOnOff(onOff) {
    //Spark Photon API Call
	httpPost(
		uri: "https://api.spark.io/v1/devices/${deviceId}/power",
        body: [access_token: token, command: onOff],  
	) {response -> return response.data.return_value}
}
// wrapper for particle brew function 
private particleBrew(size) {
    //Spark Photon API Call
	httpPost(
		uri: "https://api.spark.io/v1/devices/${deviceId}/brew",
        body: [access_token: token, command: size],  
	) {response -> log.debug (response.data.return_value)}
}
// wrapper to for particle powerstate function, it will check and see what the particle power state is 
private particlePowerState() {
    //Spark Photon API Call
	httpPost(
		uri: "https://api.spark.io/v1/devices/${deviceId}/powerState",
        body: [access_token: token, command: "a"],  
	) {response -> return response.data.return_value}
}

