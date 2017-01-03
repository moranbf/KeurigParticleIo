/*
| Code to connet a Particle.io Photon up to a Keurig K40
| Author: moranbf@gmail.com
| Date: 12/18/16
|
*/

// Cut the black wire from the kcup sensor
// Black Wire from kcup sensor, will be an input
int kcupClosedIn = D5;
// Black wire from kcup sensor going to main control unit, will be in output
int kcupClosedOut = D7;
// Spliced into pin 13 on display control wires, We will use this to determine if the Keurig is on or off
int powerLedIn = D1;
// Cut wire connected goint to  pin 11 on display control unit
// The wire from the display unit will the the input read when a button is pressed
int btnsIn = A1;
// The wire to the main control unit will be the output and wired to the DAC
int btnsOut = DAC;
// SplicE into the Add Water Sensor, there are two black lines, one sends 5v to the magnetic sensor the other
// is at +5v when there is water
int addWater = D3;

int heaterVal = A3;
int dataPin = D2;
int toggle =0;
int toggleLoop = 0;
// All of the buttons (pwr, lrg, med, sml, menu) on the Kuerig are wired to one lead on the main control board
// Resistors bump the voltage up, or to ground for toggling power, and it appears a ADC on the main control unit read 
// different voltages for different buttons, so lets define them
// divide voltage by 3.3 and multiply by 4095 to get the value to set the DAC to
// dac = voltabe/3.3 * 4095
int dacOn = (3.3 - 2.4)/3.3 * 4095;
int dacMenu = (3.3 - 2.5)/3.3  * 4095 ;
int dacOff = 4095;
int dacMedium = (3.3 - 2.6)/3.3  * 4095;
int dacSmall =  (3.3 - 2.8)/3.3  * 4095;
int dacLarge = 0;

void setup() {
pinMode(btnsOut,OUTPUT);
pinMode(btnsIn,INPUT);
pinMode(kcupClosedOut, OUTPUT);
pinMode(heaterVal, INPUT);
pinMode(powerLedIn, INPUT_PULLDOWN);
pinMode(kcupClosedIn, INPUT_PULLDOWN);
pinMode(addWater,INPUT_PULLDOWN);
pinMode(dataPin,INPUT_PULLDOWN);
Particle.function("powerOn",powerOn);
Particle.function("powerOff",powerOff);
Particle.function("powerState",powerState);
Particle.function("brew",brew);
Particle.function("dac_out",dac_out);
Particle.function("read_value",read_value);
Particle.function("power",power);
Particle.function("heaterCheck",heaterCheck);
Particle.function("waterCheck",waterCheck);
Particle.function("checkDataPin",checkDataPin);
// setup the brew/power/menu line to a steady state which is aprox 2.4v when plugged in
analogWrite(btnsOut, dacOn);
// Set the Time on the keurig clock using the menu, sml, and lrg buttons
setTime();
Particle.publish("Setup finished");
}

void loop() {
    //the main loop will capture when a button is pressed and brew the correct size
      int newButtonValue = analogRead(btnsIn);
    // value range for when the large button is pressed
    if (newButtonValue < 4100 && newButtonValue > 3900){
       brew("large");
    }
   //  value range for when the medium button is pressed
    else if (newButtonValue < 3300 && newButtonValue > 3100){
        brew("medium");
    }
    // value range for when the small button is pressed
    else if (newButtonValue < 3500 && newButtonValue > 3350){
        brew("small");
    }
   //  value range for when the power button is pressed
    else if (newButtonValue < 100){
        togglePower();
    }
    
}

int dataCheck(){
   if (toggle == 0){
       toggle = 1;
   }
   else if (toggle == 1){
       toggle = 0;
   }
}

int checkDataPin(String blah){
    int value = digitalRead(dataPin);
    return toggle;
}
int heaterCheck(String blah){
    return analogRead(heaterVal);
}

// Set the Time on the Unit using the menu, lrg and sml buttons
int setTime(){
    int i;
    // Wait a minute make sure the display PIC is up
    delay(10000);
    int powerStatus = powerState("a");
    if ( powerStatus == 0){
        powerOn("a");
    }
    delay(2000);
    // Set Timezone, get the our hours in miltary format and minutes
    Time.zone(-6);
    int hour = Time.hour();
    int minute = Time.minute();
    // Hit the menu button, this should get you to the time set option
    // the sml button will increment hours, lrg minutes
    menu();
    for (i = 0; i < minute; i++) {
    // Loop through once for minutes hitting the lrg button
        lrg(); 
    }
    // Loop through once for hours hitting the sml button
    for (i = 0; i < hour; i++) {
        sml();
    }
    // Hit the menu button 5 more times to get out of menu mode
    menu();
    menu();
    menu();
    menu();
    menu();
    // power down
    powerOff("a");
    return 0;
    }

// Toggle the power, by setting btnsOut to ground and back to dacOn
int togglePower() {
         analogWrite(btnsOut, dacOff);
         delay(300);
         analogWrite(btnsOut, dacOn);
         return 0;
}
int waterCheck(String command){
    return digitalRead(addWater);
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

// Functions used to set the time
int menu(){
    delay(300);
    analogWrite(btnsOut, dacMenu); 
    delay(500);
    analogWrite(btnsOut, dacOn); 
    delay(300);
    return 0;
}
int lrg(){
    analogWrite(btnsOut, dacLarge); 
    delay(100);
    analogWrite(btnsOut, dacOn); 
    delay(100);
    return 0;
}
int sml(){
    analogWrite(btnsOut, dacSmall); 
    delay(100);
    analogWrite(btnsOut, dacOn); 
    delay(100);
    return 0;
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
    if (waterCheck("a") == 0){
        return -999;
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
int dac_out(String val){
    //float f_volt = voltage.toFloat();
    //float val = f_volt/3.3 * 4095;
    analogWrite(btnsOut, val.toInt());

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

