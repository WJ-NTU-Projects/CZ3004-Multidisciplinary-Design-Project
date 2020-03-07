#!/usr/bin/python
# -*- coding: utf-8 -*-
import os, sys
import serial
import time

from params import *

class Arduino:

	def __init__(self):
		self.baudrate = BAUD
		self.ser = 0

	def connect(self):
		#connect to serial port
		try:
			print("-------------------------------")
			print("Trying to connect to Arduino...")
			self.ser = serial.Serial(SER_PORT0, self.baudrate, timeout = 1)
			time.sleep(1)

			if(self.ser != 0):
				print ("Connected to Arduino!")
				self.read()
				return 1
		except Exception as e:
			self.ser = serial.Serial(SER_PORT1, self.baudrate, timeout = 1)
			#print "Arduino connection exception: %s" %str(e)
			return 1

	def write(self,msg):
		try:
			self.ser.write(msg)
#			self.write(str.encode(msg).decode('utf-8'))
#			print(bytes.decode(msg))
#			print(str.encode(msg).decode('utf-8'))
			print ("Write to Arduino: %s" %(msg))#write msg with | as end of msg
		except Exception as e:
			print ("Arduino write exception: %s" %str(e))

	def read(self):
		try:
			msg = self.ser.readline() #read msg from arduino sensors
			print ("Read from Arduino:",msg)
			return msg
		except Exception as e:
			print ("Arduino read exception: %s" %str(e))
