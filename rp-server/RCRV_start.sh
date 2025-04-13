#!/bin/bash

# Initialize GPIO
/usr/bin/python3 - <<END
import RPi.GPIO as GPIO
GPIO.setmode(GPIO.BCM)
GPIO.setup(17, GPIO.OUT)
GPIO.setup(18, GPIO.OUT)
GPIO.output(17, GPIO.LOW)
GPIO.output(18, GPIO.LOW)
END

/usr/bin/nmcli radio wifi on
echo "(RCRV) Opening Wifi"

# Turn on LED1
/usr/bin/python3 - <<END
import RPi.GPIO as GPIO
GPIO.setmode(GPIO.BCM)
GPIO.setup(17, GPIO.OUT)
GPIO.output(17, GPIO.HIGH)
END

until /usr/bin/nmcli -t -f DEVICE,TYPE,STATE device | grep -q "wlan0:wifi:connected"; do
    echo "(RCRV) Waiting For Hotspot..."
    sleep 5
done

# Turn off LED1, turn on LED2
/usr/bin/python3 - <<END
import RPi.GPIO as GPIO
GPIO.setmode(GPIO.BCM)
GPIO.setup(17, GPIO.OUT)
GPIO.setup(18, GPIO.OUT)
GPIO.output(17, GPIO.LOW)
GPIO.output(18, GPIO.HIGH)
END

echo "(RCRV) Connected to Hotspot!"
echo "(RCRV) Starting app.py..."

cd /home/Saty/RCRV/rp-server
/usr/bin/python3 app.py
