#include <SoftwareSerial.h>

// Define the pins for SoftwareSerial
SoftwareSerial BTSerial(2, 3); // RX, TX

const int led1Pin = 11; // LED1 pin 7 Light Bulb
const int led2Pin = 5; // LED2 pin 5 YELLOW LED
const int led3Pin = 6; // LED3 pin 6 ORANGE LED
const int led4Pin = 9; // LED4 pin 9 RED LED
const int potPin = A0; // Potentiometer pin A0
const int switchPin = 10; // Switch pin

unsigned long previousMillis = 0;
unsigned long duration = 0;
boolean ledState = false;
unsigned long lastPotSendTime = 0;
const unsigned long potSendInterval = 1000; // Interval to send potentiometer values (in ms)

bool stopReading = false;

void setup() {
  // Set LED pins as output
  pinMode(led1Pin, OUTPUT);
  pinMode(led2Pin, OUTPUT);
  pinMode(led3Pin, OUTPUT);
  pinMode(led4Pin, OUTPUT);
  pinMode(potPin, INPUT); // Set potentiometer pin as input
  pinMode(switchPin, INPUT_PULLUP); // Set switch pin as input with internal pull-up resistor

  // Initialize LEDs to OFF state
  analogWrite(led1Pin, 0);
  digitalWrite(led2Pin, LOW);
  digitalWrite(led3Pin, LOW);
  digitalWrite(led4Pin, LOW);

  // Start serial communication for debugging
  Serial.begin(9600); // Increase baud rate for faster communication

  // Start software serial communication with the Bluetooth module
  BTSerial.begin(9600); // Ensure this matches the baud rate of your Bluetooth module

  Serial.println("Arduino ready to receive commands");
}

void loop() {
  // Check switch state
  bool switchState = digitalRead(switchPin) == LOW; // Assuming switch is LOW when ON

  if (switchState) {
    // Read the incoming Bluetooth command if available
    while (BTSerial.available() > 0) {
      // Read the incoming Bluetooth command
      String command = BTSerial.readStringUntil('\n');
      command.trim(); // Remove any leading/trailing whitespace

      Serial.println("Received command: " + command); // Debug print

      // Check if command is enclosed within '<' and '>'
      if (command.startsWith("<") && command.endsWith(">")) {
        // Remove the '<' and '>' markers
        command = command.substring(1, command.length() - 1);

        // Parse the command
        int commaIndex = command.indexOf(',');
        if (commaIndex != -1) {
          String cmd = command.substring(0, commaIndex);
          String durationStr = command.substring(commaIndex + 1);
          duration = durationStr.toInt();

          if (cmd == "1") {
            stopReading = true; // Stop reading potentiometer
            delay(200);
            analogWrite(led1Pin, 150);
            digitalWrite(led2Pin, LOW);
            digitalWrite(led3Pin, LOW);
            digitalWrite(led4Pin, LOW);

          } else if (cmd == "2") {
            stopReading = true; // Stop reading potentiometer
            delay(500);

          } else if (cmd == "3") {
            stopReading = true; // Stop reading potentiometer
            delay(200);
            // Flash LEDs
            digitalWrite(led1Pin, LOW);
            digitalWrite(led2Pin, HIGH);
            digitalWrite(led3Pin, HIGH);
            digitalWrite(led4Pin, HIGH);
            delay(1000);
            digitalWrite(led1Pin, LOW);
            digitalWrite(led2Pin, LOW);
            delay(200);
            digitalWrite(led3Pin, LOW);
            delay(200);
            digitalWrite(led4Pin, LOW);
            delay(200);
            digitalWrite(led3Pin, HIGH);
            delay(200);
            digitalWrite(led4Pin, HIGH);
            delay(200);
            digitalWrite(led2Pin, HIGH);
            delay(200);
            digitalWrite(led1Pin, LOW);
            digitalWrite(led2Pin, LOW);
            digitalWrite(led3Pin, LOW);
            digitalWrite(led4Pin, LOW);
            delay(200);
            stopReading = false; // Resume reading potentiometer
            lastPotSendTime = millis(); // Update the time to ensure the next read happens correctly

            Serial.println("All LEDs flashed");

            // Record the time when the LEDs were turned on
            previousMillis = millis();
            ledState = true;
          } else if (cmd == "4") {
            stopReading = true; // Stop reading potentiometer
            delay(200);
            // Turn off all LEDs
            digitalWrite(led1Pin, LOW);
            digitalWrite(led2Pin, LOW);
            digitalWrite(led3Pin, LOW);
            digitalWrite(led4Pin, LOW);
            delay(2000);
            stopReading = false; // Resume reading potentiometer
            lastPotSendTime = millis(); // Update the time to ensure the next read happens correctly

            Serial.println("All LEDs turned OFF");

            // Record the time when the LEDs were turned off
            previousMillis = millis();
            ledState = false;
          } else {
            // Turn off all LEDs for any other command
            digitalWrite(led1Pin, LOW);
            digitalWrite(led2Pin, LOW);
            digitalWrite(led3Pin, LOW);
            digitalWrite(led4Pin, LOW);

            Serial.println("All LEDs turned off");
            ledState = false;
          }
        }
      }
      stopReading = false; // Resume reading potentiometer
      lastPotSendTime = millis(); // Update the time to ensure the next read happens correctly
    }

    // Check if it's time to turn off the LEDs
    if (ledState && (millis() - previousMillis >= duration)) {
      digitalWrite(led1Pin, LOW);
      digitalWrite(led2Pin, LOW);
      digitalWrite(led3Pin, LOW);
      digitalWrite(led4Pin, LOW);

      Serial.println("All LEDs turned off after duration");
      ledState = false;
    }

    // Read and send potentiometer values at regular intervals if not stopped
    if (!stopReading && (millis() - lastPotSendTime >= potSendInterval)) {
      lastPotSendTime = millis();
      int potValue = analogRead(potPin);
      int outputValue = map(potValue, 0, 1023, 10, 35); // Adjust mapping as needed

      // Read potentiometer and control LEDs
      if (outputValue >= 20 && outputValue <= 25) {
        digitalWrite(led2Pin, HIGH);
        digitalWrite(led3Pin, LOW);
        digitalWrite(led4Pin, LOW);

      } else if (outputValue >= 26 && outputValue <= 31) {
        digitalWrite(led2Pin, LOW);
        digitalWrite(led3Pin, HIGH);
        digitalWrite(led4Pin, LOW);
      } else if (outputValue >= 32 && outputValue <= 35) {
        digitalWrite(led2Pin, LOW);
        digitalWrite(led3Pin, LOW);
        digitalWrite(led4Pin, HIGH);

      } else {
        digitalWrite(led2Pin, LOW);
        digitalWrite(led3Pin, LOW);
        digitalWrite(led4Pin, LOW);

      }

      // Send potentiometer value over Bluetooth
      BTSerial.print("Temperature: ");
      BTSerial.println(outputValue);

      Serial.print("Temperature: ");
      Serial.println(outputValue); // Debug print
    }
  } else {
    // If switch is off, turn off all LEDs
    digitalWrite(led1Pin, LOW);
    digitalWrite(led2Pin, LOW);
    digitalWrite(led3Pin, LOW);
    digitalWrite(led4Pin, LOW);
  }
}
