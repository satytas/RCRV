# Remote Controlled Reconnaissance Vehicle (RCRV)  

### Overview
The **RCRV** is a **concept project** for a compact, remote-controlled reconnaissance robot designed for urban warfare scenarios.  
This is one of my final engineering project in **Electronics Engineering** and **Software Engineering**.  

The robot provides soldiers with a safer and more efficient way to navigate hostile environments by offering real-time video streaming, manual navigation, and automated enemy detection.  

---

### Project Features
- **Live Video Feed**: The robot streams real-time footage from its onboard camera to a mobile application.  
- **Remote Navigation**: Users can manually control the robot via an Android app.  
- **Enemy Detection**: The robot can scan a room for potential threats using facial recognition technology.  
- **Compact & Agile Design**: Small enough to maneuver through tight spaces.  
- **LED Flashlight**: Enhances visibility in dark environments.  
- **Autonomous Scanning Mode**: The robot systematically scans an area and reports threat status.  

---

### Technology Stack
#### Software
- **Android Application** – Developed in **Java (Android Studio)** for remote control and video streaming.  
- **RP Flask Server** – Manages communication between the app and the robot.  
- **AI Model & Facial Recognition** – Runs on a **Python script** that loads a pre-trained AI model.  
- **AI Model Used**: Public source **MobileNetSSD** for object detection and facial recognition.  


#### **Hardware**  
- **ESP32 microcontroller** – Handles communication and control.  
- **Raspberry Pi 3** – Runs the AI-based facial recognition.  
- **Infrared Camera** – Provides night vision capabilities.  
- **4 DC Motors** – Enable movement and navigation.  
- **Stepper Motor** – Controls camera movement for scanning.  
- **Motor Driver Shield** – Interfaces with the motors.  
- **White LED & LED Driver** – Used for low-light environments. 
