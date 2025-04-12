
import serial

class UARTController:
    def __init__(self, port='/dev/serial0', baudrate=9600):
        """Initialize UART."""

        self.ser = None
        try:
            self.ser = serial.Serial(port, baudrate, timeout=1)
            print("UART connected")
        except serial.SerialException as e:
            print(f"UART error: {e}")

    def send_command(self, direction, scan_mode):
        """Send UART command."""

        if not self.ser or not self.ser.is_open:
            print("UART disconnected")
            return

        forward = 0
        backward = 0
        left = 0
        right = 0
        scan = 1 if scan_mode else 0

        if direction == 'UR':
            forward = 1
            right = 1
        elif direction == 'U':
            forward = 1
        elif direction == 'UL':
            forward = 1
            left = 1
        elif direction == 'L':
            left = 1
        elif direction == 'DL':
            backward = 1
            left = 1
        elif direction == 'D':
            backward = 1
        elif direction == 'DR':
            backward = 1
            right = 1
        elif direction == 'R':
            right = 1
        elif direction == 'None':
            pass
        else:
            print(f"Bad direction: {direction}")
            return

        command = (scan << 4) | (right << 3) | (left << 2) | (backward << 1) | forward

        try:
            self.ser.write(bytes([command]))
            print(f"UART : {bin(command)}")
        except serial.SerialException as e:
            print(f"Send error: {e}")

    def close(self):
        """Close UART."""

        if self.ser and self.ser.is_open:
            self.ser.close()
            print("UART closed")
