from flask import Flask, Response, request, render_template, jsonify
import cv2
import numpy as np
import time
from threading import Thread
import logging
import atexit
from network_utils import get_ip_address, broadcast_ip
from uart_utils import UARTController

# Load MobileNet SSD model
ssd_net = cv2.dnn.readNetFromCaffe("externalAI/MobileNetSSD_deploy.prototxt", "externalAI/MobileNetSSD_deploy.caffemodel")

# Detection mode: 0 = No detection, 1 = MobileNet SSD
mode = 1

app = Flask(__name__)

logging.getLogger("werkzeug").setLevel(logging.ERROR)

# Initialize camera
cap = cv2.VideoCapture(0)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 320)  # Lower resolution for speed
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 240)

# Global variables for detection and FPS
person_detected = False
person_rect = (0, 0, 0, 0)  # (x, y, w, h) of detected person
fps = 0
frame_count = 0  # Global frame counter

# Client data variables
direction = "None"
angle = 0.0
scan_mode = False

# Connection flag as a list for thread safety
connected = [False]

# Initialize UART controller
uart = UARTController(port='/dev/serial0', baudrate=9600)

def fps_calculator():
    global fps, frame_count
    
    previous_count = 0
    start_time = time.time()
    
    while True:
        time.sleep(1)
        
        current_time = time.time()
        elapsed = current_time - start_time
        current_count = frame_count
        
        if elapsed > 0:
            fps = (current_count - previous_count) / elapsed
        else:
            fps = 0
                    
        previous_count = current_count
        start_time = current_time

def frame_capture():
    global person_detected, person_rect, frame_count
    
    while True:
        ret, frame = cap.read()
        
        if not ret:
            print("Error: Failed to capture frame from camera")
            continue
        
        frame_count += 1
        
        processed_frame, detected, rect = process_frame(frame)
        
        person_detected = detected
        person_rect = rect
        
        yield frame

def process_frame(frame):
    global mode
    
    height, width = frame.shape[:2]
    detected = False
    rect = (0, 0, 0, 0)  # Default: no detection

    if mode == 1:  # MobileNet SSD detection
        blob = cv2.dnn.blobFromImage(frame, 0.007843, (300, 300), (127.5, 127.5, 127.5), False, False)
        ssd_net.setInput(blob)
        detections = ssd_net.forward()

        for i in range(detections.shape[2]):
            confidence = detections[0, 0, i, 2]
            class_id = int(detections[0, 0, i, 1])
            
            if confidence > 0.5 and class_id == 15:  # 15 - person class
                box = detections[0, 0, i, 3:7] * np.array([width, height, width, height])
                (x, y, x2, y2) = box.astype("int")
                cv2.rectangle(frame, (x, y), (x2, y2), (0, 255, 0), 2)
                detected = True
                rect = (x, y, x2 - x, y2 - y)
                break  # Stop after first person detection

    return frame, detected, rect

def generate_frames():
    global person_detected, person_rect
    
    while True:
        frame = next(frame_capture())

        if person_detected:
            x, y, w, h = person_rect
            cv2.rectangle(frame, (x, y), (x + w, y + h), (0, 255, 0), 2)

        ret, buffer = cv2.imencode('.jpg', frame, [cv2.IMWRITE_JPEG_QUALITY, 70])
        if not ret:
            continue
        frame = buffer.tobytes()
        yield (b'--frame\r\n'
               b'Content-Type: image/jpeg\r\n\r\n' + frame + b'\r\n')

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/video_feed')
def video_feed():
    return Response(generate_frames(), mimetype='multipart/x-mixed-replace; boundary=frame')

@app.route('/get_data', methods=['GET'])
def get_data():
    global fps, person_detected
    
    data = {
        'fps': fps,
        'person_detected': person_detected
    }
    
    return jsonify(data)

@app.route('/send_data', methods=['POST'])
def send_data():
    global direction, angle, scan_mode, mode, connected
    
    try:
        data = request.get_json()
        if not isinstance(data, dict):
            print("Error: Invalid JSON data received")
            return jsonify({'status': 'error', 'message': 'Invalid JSON data'}), 400
        
        if 'direction' in data:
            direction = data['direction']
        
        if 'scanMode' in data:
            scan_mode = data['scanMode']
        
        if 'angle' in data:
                angle = float(data['angle'])
        
        if 'mode' in data:
            mode = int(data['mode'])
        
        if not connected[0]:
            connected[0] = True
            print("Client connected, stopping broadcasts.")

        uartCommand = uart.send_command(direction, scan_mode)
        
        print(f"UART: {bin(uartCommand)}, Dir: {direction}, ScanMode: {scan_mode}, Mode: {mode}, Angle: {angle}")
        
        response = {
            'status': 'success',
            'current': {
                'direction': direction,
                'angle': angle,
                'scanMode': scan_mode,
                'mode': mode
            }
        }
        return jsonify(response), 200
    
    except Exception as e:
        print(f"Error processing POST request: {e}")
        return jsonify({'status': 'error', 'message': str(e)}), 400

def cleanup():
    print("Cleaning up resources...")
    
    uart.close()
    if cap.isOpened():
        cap.release()
        print("Camera released")
    print("Cleanup complete")

# Register cleanup function
atexit.register(cleanup)

if __name__ == "__main__":
    # Start broadcasting in a separate thread
    Thread(target=broadcast_ip, args=('wlan0', connected), daemon=True).start()
    # Start FPS calculator thread
    Thread(target=fps_calculator, daemon=True).start()
    try:
        app.run(host="0.0.0.0", port=5000, threaded=True)
    except KeyboardInterrupt:
        print("Server stopped by user")
    except Exception as e:
        print(f"Error running server: {e}")
