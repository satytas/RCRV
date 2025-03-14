
from flask import Flask, Response, request, render_template, jsonify
import cv2
import numpy as np
import time
from threading import Thread

# Load models only when needed
ssd_net = cv2.dnn.readNetFromCaffe("externalAI/MobileNetSSD_deploy.prototxt", "externalAI/MobileNetSSD_deploy.caffemodel")
face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")
hog = cv2.HOGDescriptor()
hog.setSVMDetector(cv2.HOGDescriptor_getDefaultPeopleDetector())

mode = 1  # 1-MobileNet SSD, 2-HOG, 3-Haar Cascade, 4-None

app = Flask(__name__)

cap = cv2.VideoCapture(0)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 320)  # Reduce resolution for faster processing
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 240)

# Global variables to store detection state
person_detected = False
person_rect = (0, 0, 0, 0)  # (x, y, w, h) of the detected person
fps = 0

def frame_capture():
    global person_detected, person_rect, fps
    frame_count = 0
    start_time = time.time()
    while True:
        ret, frame = cap.read()
        if not ret:
            continue

        # Process frame and detect person
        processed_frame, detected, rect = process_frame(frame)

        # Update global detection state
        person_detected = detected
        person_rect = rect
        
        # Calculate FPS in the capture loop
        frame_count += 1
        elapsed_time = time.time() - start_time
        if elapsed_time >= 1.0:
            fps = frame_count / elapsed_time
            frame_count = 0
            start_time = time.time()

        # Yield the processed frame for streaming
        yield frame

def process_frame(frame):
    global mode
    height, width = frame.shape[:2]
    detected = False
    rect = (0, 0, 0, 0)  # Default to no detection

    if mode == 1:
        # MobileNet SSD detection
        blob = cv2.dnn.blobFromImage(frame, 0.007843, (300, 300), (127.5, 127.5, 127.5), False, False)
        ssd_net.setInput(blob)
        detections = ssd_net.forward()

        for i in range(detections.shape[2]):
            confidence = detections[0, 0, i, 2]
            class_id = int(detections[0, 0, i, 1])
            if confidence > 0.5 and class_id == 15:  # 15-person
                box = detections[0, 0, i, 3:7] * np.array([width, height, width, height])
                (x, y, x2, y2) = box.astype("int")
                cv2.rectangle(frame, (x, y), (x2, y2), (0, 255, 0), 2)
                detected = True
                rect = (x, y, x2 - x, y2 - y)

    elif mode == 2:
        # HOG people detector
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        boxes, _ = hog.detectMultiScale(gray, winStride=(8, 8))
        for (x, y, w, h) in boxes:
            cv2.rectangle(frame, (x, y), (x + w, y + h), (255, 0, 0), 2)
            detected = True
            rect = (x, y, w, h)

    elif mode == 3:
        # Haar cascade face detection
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        faces = face_cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(30, 30))
        for (x, y, w, h) in faces:
            cv2.rectangle(frame, (x, y), (x + w, y + h), (0, 0, 255), 2)
            detected = True
            rect = (x, y, w, h)

    return frame, detected, rect

def generate_frames():
    global person_detected, person_rect
    while True:
        frame = next(frame_capture())  # Get the latest frame from capture thread

        # Draw the detected person (if any)
        if person_detected:
            x, y, w, h = person_rect
            cv2.rectangle(frame, (x, y), (x + w, y + h), (0, 255, 0), 2)

        # Encode frame and send it
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

@app.route('/get_data')
def get_data():
    return jsonify({'fps': fps, 'person_detected': person_detected})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, threaded=True)
