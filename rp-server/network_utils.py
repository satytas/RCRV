import socket
import fcntl
import struct
import time

def get_ip_address(ifname):
    """Get the IP address of the specified network interface."""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        return socket.inet_ntoa(fcntl.ioctl(
            s.fileno(),
            0x8915,  # SIOCGIFADDR
            struct.pack('256s', ifname[:15].encode('utf-8'))
        )[20:24])
    except OSError as e:
        print(f"Error getting IP for {ifname}: {e}")
        return None

def get_broadcast_address(ifname):
    """Get the broadcast address of the specified network interface."""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        return socket.inet_ntoa(fcntl.ioctl(
            s.fileno(),
            0x8919,  # SIOCGIFBRDADDR
            struct.pack('256s', ifname[:15].encode('utf-8'))
        )[20:24])
    except OSError as e:
        print(f"Error getting broadcast address for {ifname}: {e}")
        return None

def broadcast_ip(interface, connected_flag):
    """Broadcast the server's IP address until a connection is established."""
    while not connected_flag[0]:
        ip_address = get_ip_address(interface)
        if ip_address is None:
            print(f"No IP address found for {interface}, retrying...")
            time.sleep(1)
            continue
        
        broadcast_address = get_broadcast_address(interface)
        if broadcast_address is None:
            print(f"No broadcast address found for {interface}, using default")
            broadcast_address = "255.255.255.255"
        
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        
        message = f"FLASK_SERVER:{ip_address}".encode('utf-8')
        print(f"Broadcasting: {message} to {broadcast_address}:5001")
        sock.sendto(message, (broadcast_address, 5001))
        sock.close()
        time.sleep(1)
