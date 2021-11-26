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

class CameraState():
    camera_ready = False
    camera_close = False
    faces = 0

class CameraService(rpyc.Service):
    def exposed_camera_ready(self, value=None):
        if value is not None:
            camState.camera_ready = value
        return camState.camera_ready
    def exposed_camera_close(self, value=None):
        if value is not None:
            camState.camera_close = value
        return camState.camera_close
    def exposed_faces(self, value=None):
        if value is not None:
            camState.faces = value
        return camState.faces
camState = CameraState()

class MainLoop(object):
    logging.basicConfig(level=os.environ.get("LOGLEVEL","INFO"))
    log = logging.getLogger(__name__)

    def __init__(self, args):
        #Its better to always give the full path to open other scripts
        self.path = os.path.dirname(os.path.abspath(__file__))

        #Start rpyc server (rpyc is a library to handle parallel processes and pass data between them)
        server = ThreadedServer(CameraService, port=args.rpyc_port, protocol_config=rpyc.core.protocol.DEFAULT_CONFIG)
        t = Thread(target=server.start)
        t.daemon = True
        t.start()

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

            elif inputText == 'videostart':
                self.robot.videoRecord('start')

            elif inputText == 'videostop':
                video_filename = self.robot.videoRecord('stop')
                self.log.info("Video FileName:" + video_filename)

            elif inputText == 'streamstart':
                self.start_camera()
            
            elif inputText == 'streamstop':
                self.stop_camera()

            else:
                self.robot.say(inputText, listen=True)

    def start_camera(self):
        # If your run in terminal and want to see images, then you need to assign a display port.
        # vncserver
        # $export DISPLAY=localhost:{vncPORT} e.g. localhost:9
        # connect to your vnc, done

        port = self.robot.videoStream('start')
        address = 'http://' + self.robot.robot_ip + ':' + port
        print('video streaming on ' + address)

        
        subprocess.Popen(['python3', self.path + '/Camera/cv2/camera.py',
                        '--rpyc_port', str(args.rpyc_port),
                        '--address', address],
                        stderr=sys.stderr, stdout=sys.stdout)

        while not camState.camera_ready:
            time.sleep(0.1)

    def stop_camera(self):
        camState.camera_close = True
        self.robot.videoStream('stop')

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--rpyc_port', type=int, default=1234, help='rpyc port of parent')
    args = parser.parse_args()
    
    mainLoop = MainLoop(args)
    mainLoop.start()
    