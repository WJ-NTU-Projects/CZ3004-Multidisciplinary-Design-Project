from bluetooth import *
import time

class AndroidV2:
    def __init__(self):
        self.uuid = "00001101-0000-1000-8000-00805F9B34FB"
        pass

    def connect(self):
        try:
            self.server_socket = BluetoothSocket(RFCOMM)
            self.server_socket.bind(("", 4))
            self.server_socket.listen(1)
            port = self.server_socket.getsockname()[1]
            advertise_service(self.server_socket, "SampleServer", service_id = self.uuid, service_classes = [self.uuid, SERIAL_PORT_CLASS], profiles = [SERIAL_PORT_PROFILE],)
            print("Waiting for Android connection...")
            self.client_socket, client_info = self.server_socket.accept()
            print("Connected to Android successfully.")
            return 1
        except Exception as e:
            print("Failed to connect to Android: %s" %str(e))

            try:
                self.client_socket.close()
                self.server_socket.close()
            except:
                print("ERROR")
            return 0

    def disconnect(self):
        try:
            self.client_socket.close()
            self.server_socket.close()
            print("Disconnected from Android successfully.")
        except:
            print("Failed to disconnect from Android: %s" %str(e))

    def reconnect(self):
        connected = 0
        connected = self.connect()

        while connected == 0:
            print("Attempting to reconnect to Android...")
            time.sleep(1)
            connected = self.connect()

    def write(self, message):
        try:
            self.client_socket.send(message)
        except Exception as e:
            print("Failed to write to Android: %s" %str(e))
            self.reconnect()

    def read(self):
        try:
            message = self.client_socket.recv(1024)
            return message
        except Exception as e:
            print("Failed to read from Android: %s" %str(e))
            self.reconnect()
