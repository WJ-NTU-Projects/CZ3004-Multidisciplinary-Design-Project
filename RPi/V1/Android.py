from bluetooth import *
from params import *
import time
 
class Android:
    
    def _init_(self):
        pass
    
    def connect(self,uuid):
        try:
            
            self.server_sock=BluetoothSocket( RFCOMM )
            #self.server_sock.allow_reuse_address = True
            #self.server_sock.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1)
            self.server_sock.bind(("",4))
            self.server_sock.listen(1)
            port = self.server_sock.getsockname()[1]

            advertise_service(self.server_sock, "SampleServer",
                                service_id = uuid,
                                service_classes = [ uuid, SERIAL_PORT_CLASS ],
                                profiles = [ SERIAL_PORT_PROFILE ],)

            print("-----------------------------------------")
            print( "Waiting connection from RFCOMM channel %d" % port)
            self.client_sock, client_info = self.server_sock.accept()
                        
            print ("Accepted connection from "+client_info[0])
            print ("Connected to Android!")
            return 1
        except Exception as e:
            print ("Bluetooth connection exception: %s" %str(e))
            try:
                print ("%s" %str(x))
                self.client_sock.close()
                self.server_sock.close()
            except:
                print ("Error")
            return 0
         
    def disconnect(self):
        try:
            self.client_sock.close()
            self.server_sock.close()
            print("successfully disconnected from Android")
        except Exception as e:
            print ("Bluetooth disconnection exception: %s" %str(e))
        
    def reconnect(self):
        connected = 0
        connected = self.connect(uuid)
        while connected == 0:
            print ("Attempting reconnection...")
            #self.disconnect()
            time.sleep(1)
            connected = self.connect(uuid)

    def write(self,msg):
        try:
            self.client_sock.send(msg)
            print ("Write to Android from RPI: %s" %(msg))
        except Exception as e:
            print ("Bluetooth write exception: %s" %str(e))
            self.reconnect()
 
    def read(self):
        try:
            msg = self.client_sock.recv(1024)
            print ("Read from Android to RPI: %s" %(msg))
            return (msg)
        except Exception as e:
            print ("Bluetooth read exception: %s" %str(e))
            self.reconnect()
