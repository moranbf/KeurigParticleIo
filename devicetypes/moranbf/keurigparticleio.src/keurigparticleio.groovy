/**
|  Keurig Using Particle.io Photon
|
|  Author: moranbf@gmail.com using base code from juano23@gmail.com
|  Date: 2016-12-18
|  Schematic: http://imgur.com/a/0pTzb
|
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


/********** Particle.io Code Follows ********************************************
/*
| Particle.io Code to connect a Particle.io Photon up to a Keurig K40
| Author: moranbf@gmail.com
| Date: 12/18/16
|
*

// Cut the black wire from the kcup sensor
// Black Wire from kcup sensor, will be an input
int kcupClosedIn = D5;
// Black wire from kcup sensor going to main control unit, will be in output
int kcupClosedOut = D7;
// Spliced into pin 13 on display control wires, We will use this to determine if the Keurig is on or off
int powerLedIn = D1;
// Cut wire connected going to pin 11 on display control unit
// The wire from the display unit will the the input read when a button is pressed
int btnsIn = A1;
// The wire to the main control unit will be the output and wired to the DAC
int btnsOut = DAC;
// All of the buttons on the Kuerig are wired to one lead on the main control board
// Resistors bump the voltage up, or to ground for toggling power, and it appears a ADC on the main control unit read 
// different voltages for different buttons, so lets define them
// divide voltage by 3.3 and multiply by 4095 to get the value to set the DAC to
// dac = voltabe/3.3 * 4095
int dacOn = 2.4/3.3 * 4095;
int dacOff = 0.0;
int dacMedium = 2.6/3.3 * 4095;
int dacSmall =  2.8/3.3 * 4095;
int dacLarge = 4095;

void setup() {
pinMode(btnsOut,OUTPUT);
pinMode(btnsIn,INPUT);
pinMode(kcupClosedOut, OUTPUT);
pinMode(powerLedIn, INPUT_PULLDOWN);
pinMode(kcupClosedIn, INPUT_PULLDOWN);
Particle.function("powerOn",powerOn);
Particle.function("powerOff",powerOff);
Particle.function("powerState",powerState);
Particle.function("brew",brew);
Particle.function("dac_out",dac_out);
Particle.function("read_value",read_value);
Particle.function("power",power);
// setup the brew/power/menu line to a steady state which is aprox 2.4v when plugged in
analogWrite(btnsOut, dacOn);
}

void loop() {
    // the main loop will capture when a button is pressed and brew the correct size
    int newButtonValue = analogRead(btnsIn);
    // value range for when the large button is pressed
    if (newButtonValue < 4100 && newButtonValue > 3900){
        brew("large");
    }
    // value range for when the medium button is pressed
    else if (newButtonValue < 3300 && newButtonValue > 3100){
        brew("medium");
    }
    // value range for when the small button is pressed
    else if (newButtonValue < 3500 && newButtonValue > 3350){
        brew("small");
    }
    // value range for when the power button is pressed
    else if (newButtonValue < 100){
        togglePower();
    }
}

// Toggle the power, by setting btnsOut to ground and back to dacOn
int togglePower() {
         analogWrite(btnsOut, dacOff);
         delay(300);
         analogWrite(btnsOut, dacOn);
         return 0;
}

int power(String onOff) {
    // Check and see what power state we are in
    int powerState = 0;
    powerState = digitalRead(powerLedIn);
    // if we are turning on/1 and currently the power is off or
    // if we are turning off/0 and currently the power is on
    if ((onOff.equals("1") && powerState == LOW)  ||  (onOff.equals("0") && powerState == HIGH)) {
         analogWrite(btnsOut, dacOff);
         delay(300);
         analogWrite(btnsOut, dacOn);
         delay(300);
         powerState = digitalRead(powerLedIn);
         return 0;
    }
    else {
         return -1;
    }
}

int powerOn(String command) {
    // Check and see what power state we are in
    int powerOn = 0;
    powerOn = digitalRead(powerLedIn);
    if (powerOn)
    {
        return -1;
    }
    else 
    {
         analogWrite(btnsOut, dacOff);
         delay(300);
         analogWrite(btnsOut, dacOn);
         delay(300);
         powerOn = digitalRead(powerLedIn);
         return powerOn;
    }
}

int powerOff(String command) {
    // Check and see what power state we are in
    int powerOn = 0;
    powerOn = digitalRead(powerLedIn);
    if (powerOn)
    {
         analogWrite(btnsOut, dacOff);
         delay(300);
         analogWrite(btnsOut, dacOn);
         delay(300);
         powerOn = digitalRead(powerLedIn);
         return powerOn;
    }
    else
    {
        return -1;
    }
}

int brew(String cSize){
    // First make sure the kcup handle is closed if so toggle the sensor
    int kcupState = digitalRead(kcupClosedIn);
    if (kcupState == HIGH) {
        digitalWrite(kcupClosedOut,LOW);
        delay(1000);
        digitalWrite(kcupClosedOut,HIGH);
        delay(1000);
    }
    // if the kcup compartment isn't close exit
    else {
        return -1;
    }
    if (cSize.equals("large")){
        analogWrite(btnsOut, dacLarge); 
        delay(300);
        analogWrite(btnsOut, dacOn); 
    return 3;
    }
    else if (cSize.equals("medium")){
        analogWrite(btnsOut, dacMedium);
        delay(300);
        analogWrite(btnsOut, dacOn); 
        return 2;
    }
    else if (cSize.equals("small")){
        analogWrite(btnsOut, dacSmall); 
        delay(300);
        analogWrite(btnsOut, dacOn); 
        return 1;
    }
    else {
        return -1;
    }
}

// Diagnostic Function, Suply a voltage and the DAC will output it
int dac_out(String voltage){
    float f_volt = voltage.toFloat();
    float val = f_volt/3.3 * 4095;
    analogWrite(DAC1, val);
    return 0;
}
// Diagnostic Function, Read the values when a button is pressed 
int read_value(String command){
    return analogRead(btnsIn);
}

// Diagnostic Function, Check the status of the kcup sensor
int kCupState(String command) {
    return digitalRead(kcupClosedIn);
}

// Diagnostic Function, Check Power State
int powerState(String command) {
    int powerOn = digitalRead(powerLedIn);
    if (powerOn)
    {
        return 1;
    }
    else
    {
        return 0;
    }
}
*/