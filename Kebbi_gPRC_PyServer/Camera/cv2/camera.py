import rpyc
import os
import requests
import argparse
import time
import socket
import numpy as np
import cv2

class Camera(object):
    def __init__(self, args):
        self.path = os.path.dirname(os.path.abspath(__file__))

        #Connect to rpyc server
        self.conn = rpyc.connect("localhost", port=args.rpyc_port, config=rpyc.core.protocol.DEFAULT_CONFIG)
        self.parent = self.conn.root

        #Receive stream from address
        self.cap = cv2.VideoCapture(args.address)

    def main(self):
        current_faces = 0
        while(True):
            ret, frame = self.cap.read()
            cv2.imshow('frame',frame)
            
            if cv2.waitKey(1) & 0xFF == ord('q'):
                break

            #Signal camera is ready to main thread
            if not self.parent.camera_ready():
                self.parent.camera_ready(True)  

            #Close window when signaled by main thread
            if self.parent.camera_close():
                self.cap.release()
                cv2.destroyAllWindows()
                break

if __name__ == '__main__':
    parser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('--rpyc_port', type=int, help='rpyc port of parent')
    parser.add_argument('--address', type=str, help='address of stream')
    args = parser.parse_args()
    cam = Camera(args)
    cam.main()