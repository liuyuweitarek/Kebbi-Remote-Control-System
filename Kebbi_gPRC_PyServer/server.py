"""
File: server.py
Author: Yuwei Liu
Institution: Modeling and Informatics Laboratory
Version: v0.1.0
"""

from interacter import Server
import time
import os
import sys
import argparse
import rpyc
import logging
from rpyc.utils.server import ThreadedServer
from threading import Thread
import subprocess

class MainLoop(object):
    
    logging.basicConfig(level=os.environ.get("LOGLEVEL","INFO"))
    log = logging.getLogger(__name__)

    def __init__(self, args):
        #Its better to always give the full path to open other scripts
        self.path = os.path.dirname(os.path.abspath(__file__))

        # #Start rpyc server (rpyc is a library to handle parallel processes and pass data between them)
        # server = ThreadedServer(MyService, port=args.rpyc_port, protocol_config=rpyc.core.protocol.DEFAULT_CONFIG)
        # t = Thread(target=server.start)
        # t.daemon = True
        # t.start()

        #Initialize grpc connection on given port and locale: en=English, zh=Chinese
        self.robot = Server(50051, 'zh')

        #Wait until the robot has connected
        while not self.robot.is_robot_connected:
            time.sleep(1)

        print('robot connected: ' + self.robot.robot_type + ':' + self.robot.locale)

    def start(self):
        sign = True
        while sign:
            inputText = input("Please type:")
            if inputText in ['exit', 'bye']:
                sign = False
                inputText = "好的 bye bye"
                self.robot.say(inputText, listen=False)
                input('Press enter again to terminate program...')
                os._exit(0)
            elif inputText in ['videostart']:
                self.robot.videoRecord('start')
            elif inputText in ['videostop']:
                video_filename = self.robot.videoRecord('stop')
                self.log("Video FileName:" + video_filename)
            else:
                self.robot.say(inputText, listen=True)
            
        



if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    args = parser.parse_args()
    mainLoop = MainLoop(args)
    mainLoop.start()
    