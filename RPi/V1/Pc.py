import socket
import sys
import traceback
import errno
from params import *

class Pc:
    host=WIFI_IP
    port=WIFI_PORT

    def __init__(self,host=WIFI_IP,port=WIFI_PORT):
        self.host = host
        self.port = port
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1)
        print ("Socket Established")

        try:
            self.socket.bind((self.host, self.port))
        except socket.error as e:
            print ("Bind failed",e)
            sys.exit()
    
        print ("Bind completed")
    def connect(self):	
        self.socket.listen(3)
        print ("Waiting for connection from PC...")                
        self.client_sock, self.address = self.socket.accept()
        print ("Connected to PC @ ", self.address, "!")
    
            #receive the first message from client, know the client address
            #print ("PC Connected")
    
    def disconnect(self):
        try:
            self.socket.close()
            print("successfully disconnected from PC")
        except Exception as e:
            	print ("PC disconnection exception: %s" %str(e))
 
    def write(self,msg):
        try:
        	self.client_sock.sendto(msg, self.address)
        	print ("Write to PC from RPI: %s" %(msg))
        except ConnectionResetError as ce:
         	print("connection reset error"+ce)
         	print("attempting reconnection...")
         	self.connect()
        except socket.error as e:
         	if isinstance(e.args, tuple):
                	print ("errno is %d" %e[0])
                	if e[0] == errno.EPIPE:
                        	#remote peer disconnected
                        	print ("Detected remote disconnect, attempting reconnection")
                        	self.connect()                        
                	else:
                        	#for another error
                        	pass
         	else:
                	print ("socket error ",e)
         	sys.exit()
        except IOError as e:
            	print ("PC read exception",e)
            	print (traceback.format_exc())
            	pass

 
    def read(self):
    	try:
            msg = self.client_sock.recv(1024)
            print ("Read from PC to RPI: %s" %(msg))
            return msg
    	except socket.error as e:
            	if isinstance(e.args, tuple):
                    print ("errno is %d" %e[0])
                    if e[0] == errno.EPIPE:
				#remote peer disconnected
                        print ("Detected remote disconnect, attempting reconnection...")
                        self.connect()
                    else:
				#for another error
                        pass
            	else:
                    print("socket error ",e)
                    sys.exit()

    	except IOError as e:
            print ("PC read exception: ",e)
            print (traceback.format_exc())
            pass 