"""
File: interacter.py
Author: Yuwei Liu
Institution: Modeling and Informatics Laboratory
Version: v0.1.0
"""

import os
import grpc
import time
from random import randrange
from concurrent import futures
from hanziconv import HanziConv
import socket
import logging
import json
import interaction_pb2
import interaction_pb2_grpc 




class Server(interaction_pb2_grpc.InteractServicer):
    """ 
    This is the only class in the interacter package.
    It creates a gRPC server with the computer's local ip address with a specified port.
    """

    logging.basicConfig(level=os.environ.get("LOGLEVEL","INFO"))
    log = logging.getLogger(__name__)
    
    locale = ''
    is_robot_connected = False
    robot_type = ''
    robot_ip = ''
    video_port = ''
    robot_command = None
    robot_response = ''

    ERR_EMPTY_ARGS = 'Cannot have empty args!'

    

    def __init__(self, port, locale):
        """ 
        The constructor for Server class. 
  
        Arguments: 
           port (int): The port where the server will be listening for incoming client connections. 
           locale (String): The language the robot should speak in. Currently English [en], Chinese [zh] and Japanese [jp] (RoBoHoN only) are supported.
        """

        # common_actions = ['take_photo', 'take_video', 'show_photo', 'show_video', 'random_dance', 'play_youtube']
        # self.robohon_actions = common_actions + ['dance', 'motion']
        # self.zenbo_actions = common_actions + ['show_overlay']

        self.server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
        interaction_pb2_grpc.add_InteractServicer_to_server(self, self.server)
        self.server.add_insecure_port('[::]:' + str(port))
        self.log.info('gRPC server started on address '+ self.get_ip() + ':' + str(port))
        
        if locale in ['zh','en','jp']:
            self.locale = locale
            self.server.start()
        else:
            raise ValueError('Interacter locale supports only Chinese [zh], English [en] or Japanese [jp].')

    def RobotConnect(self, request, context):
        """ 
        This method is called when a client connects to the server. 
        """

        self.is_robot_connected = True
        handshake = json.loads(request.status)
        self.robot_type = handshake["intent"]
        self.robot_ip = handshake["value"]
        
        return interaction_pb2.RobotConnectReply(status=self.locale)

    def RobotSend(self, request, context):
        """ 
        This method is called every time a message is received from the client. 
        If the robot returns an error message, it will be printed in the log.
        """

        if(request.utterance == 'locale_mismatch'):
            self.log.warning("Server and client locales do not match!")
        
        json_response = json.loads(request.utterance)

        if 'value' in json_response:
            self.robot_response = json_response['value']
        else:
            self.robot_response = None

        if json_response['intent'] == 'error':
            self.log.warning(self.robot_response)
        if self.locale == 'zh' and self.robot_response is not None:
            self.robot_response = self.toTraditional(self.robot_response)

        self.robot_command = None
        while self.robot_command == None:
            time.sleep(.2)

        return interaction_pb2.RobotOutput(utterance=self.robot_command)
    
    def say(self, speech, listen=False):
        output = {}

        if speech == '':
            listen = True
        
        output['value'] = speech
        
        if listen:
            output['intent'] = 'listen'
            self.log.info('say and listen: ' + speech)
        else:
            output['intent'] = 'say'
            self.log.info('say: ' + speech)

        self.robot_command = json.dumps(output)
        
        while self.robot_command != None:
            time.sleep(0.1)

        if listen:
            self.log.info('Robot Heard: ' + self.robot_response)
        
        return self.robot_response

    def videoRecord(self, value):
        """
        Use this method to enable/disable video streaming from the robot
        Arguments:
            value: "start": command the robot to start recording; 
                   "stop": command the robot to stop recording
        Returns:
            if value is "stop" and the video is succesfully stored, the robot will return its filename. Videos are stored in the Movies/bgRec directory.
        """
        if value == None:
            raise ValueError(self.ERR_EMPTY_ARGS)

        output = {}
        output['intent'] = 'video_record'
        output['value'] = value
        self.log.info('video record ' + value)
        self.robot_command = json.dumps(output)
        
        while self.robot_command != None:
            time.sleep(0.1)
        
        return self.robot_response

    def toTraditional(self, term):
            """
            This method converts simplified Chinese to traditional Chinese characters.
            """
            return HanziConv.toTraditional(term)
    
    def get_ip(self):
        """
        This method returns the current ip address of the computer.
        """
        
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        try:
            s.connect(('10.255.255.255', 1))
            IP = s.getsockname()[0]
        except:
            IP = '127.0.0.1'
        finally:
            s.close()
        return IP        