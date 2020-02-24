//IR
#define sensor1 A0 // PS1 S
#define sensor2 A1 // PS2 L
#define sensor3 A2 // PS3 S
#define sensor4 A3 // PS4 S       // >7
#define sensor5 A4 // PS5 S       // >7
#define sensor6 A5 // PS6 L       

//#define A0m 0.0482
//#define A0c -0.0081
//#define A1m 0.018
//#define A1c -0.0023
//#define A2m 0.0448
//#define A2c -0.0027
//#define A3m 0.045
//#define A3c -0.0029
//#define A4m 0.0415
//#define A4c -0.0017
//#define A5m 0.0194
//#define A5c -0.0033

#define A0m 0.0529
#define A0c -0.0108
#define A1m 0.0204
#define A1c -0.004
#define A2m 0.0529
#define A2c -0.0108
#define A3m 0.0454
#define A3c -0.004
#define A4m 0.0454
#define A4c -0.0046
#define A5m 0.0178
#define A5c -0.0012


//MotorEncorder 
#include <DualVNH5019MotorShield.h>
#include <EnableInterrupt.h>
#include "MedianFilterLib.h"
#define M1E1Right 11
#define M2E2Left 3
DualVNH5019MotorShield md;
volatile unsigned int E1Pos = 0;
volatile unsigned int E2Pos = 0;

MedianFilter<float> sense1(9);
MedianFilter<float> sense2(9);
MedianFilter<float> sense3(9);
MedianFilter<float> sense4(9);
MedianFilter<float> sense5(9);
MedianFilter<float> sense6(9);

void setup() {
  // put your setup code here, to run once:
  //MotorEncoder
  pinMode(M1E1Right, INPUT);
  digitalWrite(M1E1Right, HIGH);       // turn on pull-up resistor
  pinMode(M2E2Left, INPUT);
  digitalWrite(M2E2Left, HIGH);       // turn on pull-up resistor
  delay(5000);
  enableInterrupt(M1E1Right, E1, RISING);
  enableInterrupt(M2E2Left, E2, RISING);


  
  Serial.begin(115200); // start the serial port
  Serial.println("Dual VNH5019 Motor Shield");
  md.init();
  Serial.println("SETUP");
  Serial.println("start");  
}

void loop() {
  
    int unit = 450;
    while (unit > 0)
    {
      float median1;
      float median2;
      float median3;
      float median4;
      float median5;
      float median6;

      for(int i=0; i<9;i++){
//      float in1 = shortRangeIRSensor(sensor1);
//      float in2 = shortRangeIRSensor(sensor2);
//      float in3 = shortRangeIRSensor(sensor3);
//      float in4 = shortRangeIRSensor(sensor4);
//      float in5 = shortRangeIRSensor(sensor5);
//      float in6 = shortRangeIRSensor(sensor6);

//      float in1 = IRSensor(sensor1, A0m, A0c);
//      float in2 = IRSensor2(sensor2, A1m, A1c);
//      float in3 = IRSensor(sensor3, A2m, A2c);  
//      float in4 = IRSensor(sensor4, A3m, A3c);  
//      float in5 = IRSensor(sensor5, A4m, A4c);  
      float in6 = IRSensor6(sensor6, A5m, A5c);  
//
//      median1 = sense1.AddValue(in1);
//      median2 = sense2.AddValue(in2);
//      median3 = sense3.AddValue(in3);
//      median4 = sense4.AddValue(in4);
//      median5 = sense5.AddValue(in5);
      median6 = sense6.AddValue(in6);
      }
//      Serial.println("PS1: " + String(median1) + "V");
//      Serial.println("PS2: " + String(median2) + "V");
//      Serial.println("PS3: " + String(median3) + "V");
//      Serial.println("PS4: " + String(median4) + "V");
//      Serial.println("PS5: " + String(median5) + "V");
//      Serial.println("PS6: " + String(median6) + "V");
//      Serial.println(" ");
   
     // put your main code here, to run repeatedly:
//     Serial.println("PS1: " + String(median1) + "cm");   // print the distance
//     Serial.println("PS2: " + String(median2) + "cm");
//     Serial.println("PS3: " + String(median3) + "cm");   
     
//     Serial.println("PS4: " + String(median4) + "cm");
//     Serial.println("PS5: " + String(median5) + "cm");   
     Serial.println("PS6: " + String(median6) + "cm");
     Serial.println("");

    //unit = unit - 50;
    //Serial.println(String(unit));
    //results(unit);
    //if(distance1>20)
    //results(300);
    //else 
    //results(0);
    }
}


float IRSensor(char sensor, float m, float c) {
   // for sensors 1-5
  int raw=analogRead(sensor);
  int voltFromRaw=map(raw, 0, 1023, 0, 5000);
    
  float volts = voltFromRaw/1000.0;
  float distance = (1/((volts)*m + c))-0.42;
  delay(10);                    // decrease value for faster readings 
  //return distance;
  if (distance<12) {
    return distance - 0.5;
  }
  else if (distance> 20) {
    return distance+1;
  }
  else if(distance>17.5){
    return distance+0.5;
  }
  else{
    return distance;
  }
}

float IRSensor2(char sensor, float m, float c) {
  //only for sensor 6, eff range 22-60cm, >60cm range deviation too large
  int raw=analogRead(sensor);
  int voltFromRaw=map(raw, 0, 1023, 0, 5000);
    
  float volts = voltFromRaw/1000.0;
  float distance = (1/(volts*m + c))-1.52;          
  delay(10);                    // decrease value for faster readings 
  if (distance>30){
    return distance+1;
  }
  else{
    return distance;
  }
}

float IRSensor6(char sensor, float m, float c) {
  //only for sensor 6, eff range 22-60cm, >60cm range deviation too large
  int raw=analogRead(sensor);
  int voltFromRaw=map(raw, 0, 1023, 0, 5000);
    
  float volts = voltFromRaw/1000.0;
  float distance = (1/(volts*m + c))-1.52;          
  delay(10);                    // decrease value for faster readings 
  if (distance<25){
    return distance-1;
  }
  else{
    return distance;
  }
//   return distance;
}
float shortRangeIRSensor(char sensor) {
  //function for calibration only
  int raw=analogRead(sensor);
  int voltFromRaw=map(raw, 0, 1023, 0, 5000);
    
  float volts = voltFromRaw/1000.0;
  // float distance = 61.573*pow(volts - 0.253, -0.8) - 9.5;;; // LONG RANGE IR (20cm - 70cm)
  //float distance = 27.728*pow(volts + 0.01, -1.2045) + 0.15; // SHORT RANGE IR (10-30cm)
  delay(50); // slow down serial port 
  return volts;
  //return distance;
}


//MotorEncoder
void results(int unit) {
  for (int i = 1; i <= 5; i++)
   {
    md.setM1Speed(unit);
    md.setM2Speed(unit+25);
    delay(100);
    Serial.println(i);
    Serial.print("Reading value for unit speed ");
    Serial.println(unit);
    Serial.print("M1E1Right RPM = ");
    Serial.println(E1Pos/562.25 * 60);
    Serial.print("M2E2Left RPM = ");
    Serial.println(E2Pos/562.25 * 60);
    E1Pos = 0;
    E2Pos = 0;
   }
}
void E1() {
  /* If pinA and pinB are both high or both low, it is spinning
     forward. If they're different, it's going backward.

     For more information on speeding up this process, see
     [Reference/PortManipulation], specifically the PIND register.
  */
  E1Pos++;
  //Serial.print(E1Pos);
}

void E2() {
  /* If pinA and pinB are both high or both low, it is spinning
     forward. If they're different, it's going backward.

     For more information on speeding up this process, see
     [Reference/PortManipulation], specifically the PIND register.
  */
  E2Pos++;
}
