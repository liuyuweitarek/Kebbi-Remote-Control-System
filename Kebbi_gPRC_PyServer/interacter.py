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

    # motions = {
    #     "en":["Face Up", "Face Down", "Face Right", "Face Left", "Turn Right", "Turn Left", "Right Hand Up", "Left Hand Up", "Both Hands Up", "Walk", "Walk Back", "Walk Right", "Walk Left", "Push Ups", "Kick Ball", "Sit Up", "Brisk Walking", "Handstand", "Stand Up", "Sit Down", "Finish Mobile", "Shot Pose", "Enchant"],
    #     "zh":["向上看", "向下看", "向右看", "向左看", "向右转", "向左转", "举起右手", "举起左手", "举起双手", "走路", "后退", "向右走", "向左走", "俯卧撑", "踢球", "仰卧起坐", "快速走", "倒立", "站起来", "坐下", "结束外出姿势", "拍照姿势", "著迷"],
    #     "jp":["顔上向き", "顔下向き", "顔右向き", "顔左向き", "右向き", "左向き", "右手挙げ", "左手挙げ", "両手挙げ", "歩き", "後ろ歩き", "右歩き", "左歩き", "腕立て", "ボール蹴り", "腹筋", "早歩き", "逆立ち", "立つ", "座る", "お出かけ姿勢終了", "写真ポーズ", "メロメロ"]
    # }
    
    # dances = {
    #     "en":["A Little Night Music", "Under the Spreading Chestnut Tree", "Awa Odori", "Air Guitar", "Wind the Bobbin Up", "Greenville", "Tanuki Pup", "Symphony No.", "Jingle Bells", "Kung Fu", "80's Disco", "Sakura", "Military March", "Flamenco", "The Hula", "Baseball Cheer", "Che Che Kule", "Head Sholder Knees and Toes", "Japanese Drum", "Joy to the World", "Kabuki", "The Nutcracker", "Doll's Festival", "RoBoHoN Exercises", "Orpheus in the Underworld", "70's Disco", "I am an Ocean Boy", "RoBoHoN Ondo", "Air Violin", "Tap Dance", "Para Para Dance", "We Wish You a Merry Christmas", "Rabbit Dance", "Cossack Dance", "Lullaby", "Cheerleading", "Marching Band", "The Other Day I Met a Bear", "Radio Exercises", "RoBoHoN's Bootcamp", "Haka", "Chanbara", "Wotagei", "Silent Night", "Spring Sea"],
    #     "zh":["小夜曲", "在很大的栗子樹下", "阿波舞", "吉他空彈", "卷線歌", "打開結", "拳頭山的狸先生", "第九交響曲", "鈴兒響叮噹", "功夫", "八十年代迪斯科", "櫻花", "軍隊進行曲", "佛朗明哥", "草裙舞", "棒球助威", "加納民歌", "頭兒肩膀膝腳趾", "日本鼓", "普世歡騰", "歌舞伎", "胡桃夾子", "雛祭", "RoBoHoN體操", "天國與地獄", "七十年代迪斯科", "我是大海的兒子", "RoBoHoN集体舞", "小提琴空彈", "踢踏舞", "芭啦芭啦舞", "祝你聖誕快樂", "兔子舞", "哥薩克舞蹈", "搖籃曲", "啦啦隊", "遊行樂隊", "森林裡的熊先生", "廣播體操", "RoBoHoN的訓練兵営", "哈卡舞", "劍鬥", "御宅藝", "平安夜", "春海"],
    #     "jp":["アイネ・クライネ・ナハトムジーク", "大きな栗の木の下で", "阿波踊り", "エアギター", "糸巻きの歌", "結んで開いて", "げんこつ山の狸さん", "交響曲第９番", "ジングルベル", "カンフー", "80年代ディスコ", "さくら", "ミリタリーマーチ", "フラメンコ", "フラダンス", "野球応援", "チェッコリ", "体遊びの歌", "和太鼓", "もろびとこぞりて", "歌舞伎", "くるみ割り人形", "ひな祭り", "ロボホン体操", "天国と地獄", "70年代ディスコ", "我は海の子", "ロボホン音頭", "エアバイオリン", "タップダンス", "パラパラ", "おめでとうクリスマス", "うさぎのダンス", "コサックダンス", "子守唄", "チアリーディング", "マーチングバンド", "森のくまさん", "ラジオ体操", "ロボホンズブートキャンプ", "ハカ", "ちゃんばらごっこ", "ヲタ芸", "きよしこの夜", "春の海"]
    # }

    # expressions = ["INTERESTED", "DOUBTING", "PROUD", "DEFAULT", "HAPPY", "EXPECTING", "SHOCKED", "QUESTIONING", "IMPATIENT", "CONFIDENT", "ACTIVE", "PLEASED", "HELPLESS", "SERIOUS", "WORRIED", "PRETENDING", "LAZY", "AWARE_R", "TIRED", "SHY", "INNOCENT", "SINGING", "AWARE_L", "DEFAULT_STILL"]

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
    # def say(self, speech, listen=False, expression=None):
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