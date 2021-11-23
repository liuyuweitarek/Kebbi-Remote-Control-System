# Evaluating and learning gRPC

> Author : Yuwei Liu
> 

## 實作gRPC : Andorid APP為Client |  Python為Server

1. 照著做 [https://grpc.io/docs/languages/python/quickstart/](https://grpc.io/docs/languages/python/quickstart/) 
    
    > Note: 文件中的流程大致是，我們會先定義一份gRPC協議的文件 .proto；再用Py套件"grpcio grpcio-tools"生成 Python Server Code；接著便可用以形成Server端
    > 
2. 過去我們使用的協定皆為Unary RPCs，也就是Client 發出單次Request後，會等待Server給Response。下面是機器人目前用的proto設定檔，其他協定解釋可見下方例子。
    
    ```protobuf
    /**
     * Send Message between  Kebbi and Python Server.
     */
    
     syntax = "proto3";
    
     option java_multiple_files = true;
     option java_package = "com.interaction.robot.interaction";
     option java_outer_classname = "InteractProto";
     option objc_class_prefix = "ST";
    
     package interaction;
    
     // The interact service definition.
     service Interact {
       // Sends connection confirm
       rpc RobotConnect (RobotConnectRequest) returns (RobotConnectReply) {}
    
       // Sends information
       rpc RobotSend (RobotInput) returns (RobotOutput) {}
    
     }
    
     // The robot connect request message contains the status
     message RobotConnectRequest {
       string status = 1;
     }
    
     // The robot connect response message contains the status
     message RobotConnectReply {
       string status = 1;
     }
    
     // The robot input message contains the utterance
     message RobotInput {
       string utterance = 1;
     }
    
     // The robot output info message contains the utterance
     message RobotOutput {
       string utterance = 1;
     }
    ```
    
    - Unary RPCs
        
        Client 發出單次Request後，會等待Server給Response。
        
        e.g. 機器人收發指令
        
        ```protobuf
        /* Unary RPCs */
        rpc RobotSend (RobotInput) returns (RobotOutput) {}
        ```
        
    - Server Stream RPCs
        
        Client 發出單次Request後，Server回覆串流資料，Client不斷讀取直到Server沒有串流資料發送。
        
        e.g. Server處理資料有時間延遲，Client希望單個request搞定並在讀取meta data中需要有對應的反應。
        
        ```protobuf
        /* Server Stream RPCs */
        rpc ServerStreamRPCs (Request_ClientInput) returns (stream Response_SevrerStreamData) {}
        ```
        
    - Client Stream RPCs
        
        Client 發出Request後傳送串流資料，Server不斷讀取直到Client沒有串流資料發送，再給出回覆。
        
        ```protobuf
        /* Client Stream RPC */
        rpc ClientStreamRPCs (stream Request_ClientStreamData) returns (Response_ServerOutput) {}
        ```
        
    - Bidirectional Stream RPCs
        
        Server和Client都以串流處理對方的資訊。
        
        ```protobuf
        /* Bidirectional Stream RPCs */
        rpc BidirectionalStreamRPCs (stream Request_ClientStreamData) returns (stream Response_SevrerStreamData) {}
        ```
        
    
    <aside>
    💡 除了Bidirectional Stream RPCs外，其他三種方式都能確保資料傳輸的順序性。Bidirectional Stream RPCs則需要自己在程式中定義。
    
    </aside>
    
3. 依照.proto生成Server檔
    
    ```bash
    python -m grpc_tools.protoc --proto_path={.proto_filepath} --python_out={generated_python_filepath} --grpc_python_out={generated_python_grpc__filepath}
    
    # (e.g.)
    # $ cd to the .proto filepath
    # $ python -m grpc_tools.protoc --proto_path=. ./interaction.proto --python_out=../../../../Kebbi_gPRC_PyServer/. --grpc_python_out=../../../../Kebbi_gPRC_PyServer/.
    ```
    
4. 使用Python Server的方式
    
    上一步會生成兩個檔案 *_pb2.py 和 *_pb2_grpc.py，前者為gRPC協定檔，後者為方便我們使用協定的gRPC Server物件。
    
    為了讓連線的Channel更乾淨，以及能擴充各種API，設計下面的目錄：
    
    - Server
        - interaction_pb2.py
        - interaction_pb2_grpc.py
        - interaction.py
            
            這邊放可互動的API、連線方式都包在interaction gRPC中，做成Server物件
            
            ```python
            class Server(pb2_grpc.InteractServicer):
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
            ```
            
        - server.py
            
            這邊使用API，並取用gRPC Server，定義整個Server端協定的互動方式，並加上其他服務
            
            ```python
            import rpyc
            from rpyc.utils.server import ThreadedServer
            from threading import Thread
            import subprocess
            
            class MainLoop(object):
                def __init__(self, args):
                    #Its better to always give the full path to open other scripts
                    self.path = os.path.dirname(os.path.abspath(__file__))
            				
                    #Initialize grpc connection on given port and locale: en=English, zh=Chinese
                    self.robot = Server(50051, 'zh')
            				
            				# 其他服務可以在此定義,下面是新增 rpyc server(平行處理進程的工具 e.g.機器人的相機取用)
            				# Start rpyc server (rpyc is a library to handle parallel processes and pass data between them)
                    # server = ThreadedServer(MyService, port=args.rpyc_port, protocol_config=rpyc.core.protocol.DEFAULT_CONFIG)
                    # t = Thread(target=server.start)
                    # t.daemon = True
                    # t.start()
                    
            				#Wait until the robot has connected
                    while not self.robot.is_robot_connected:
                        time.sleep(1)
            
                    print('robot connected: ' + self.robot.robot_type)
            
                def start(self):
                    input("Do what you want here... using API / other tools")
            
            if __name__ == "__main__":
                parser = argparse.ArgumentParser()
                args = parser.parse_args()
                mainLoop = MainLoop(args)
                mainLoop.start()
            ```
            
5.  使用 Android Java Client
    - gradle/build.gradle：引入gRPC到Global的環境建置Gradle中
        
        ```groovy
        // Top-level build file where you can add configuration options common to all sub-projects/modules.
        
        buildscript {
            
            repositories {
                google()
                jcenter()
            }
            dependencies {
                classpath 'com.android.tools.build:gradle:3.2.1'
        				
        				//<!Add Protobuf Classpath>
                classpath 'com.google.protobuf:protobuf-gradle-plugin:0.8.8'  
        				//--/>
                
        				// NOTE: Do not place your application dependencies here; they belong
                // in the individual module build.gradle files
            }
        }
        
        allprojects {
            repositories {
                google()
                jcenter()
            }
        }
        
        task clean(type: Delete) {
            delete rootProject.buildDir
        }
        ```
        
    - app/build.gradle : 引入gRPC到APP的環境建置Gradle中
        1.  這邊使用的grpc版本為1.4.0，須注意調整。
        2.  插入的部分會用 "//<!—Edit Here>--/>"標示(為免套件之間版本不兼容，仍是附上可以Work的完整gradle檔。但grpc需使用的部分請只看標記部份，Debug也從該部份下手)
        
        ```groovy
        apply plugin: 'com.android.application'
        apply plugin: 'com.google.protobuf'  //<Edit Here>
        
        //<!Edit Here>
        ext {
            grpcVersion = '1.4.0'
        }
        //--/>
        
        android {
            compileSdkVersion 30
            buildToolsVersion "30.0.3"
            defaultConfig {
                applicationId "ntu.mil.grpckebbi"
                minSdkVersion 26
                targetSdkVersion 30
                versionCode 1
                versionName "1.0"
                testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
            }
            buildTypes {
                release {
                    minifyEnabled false
                    proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
                }
            }
            compileOptions {
                sourceCompatibility = '1.8'
                targetCompatibility = '1.8'
            }
        }
        
        //<!Edit Here>
        protobuf {
            protoc {
                artifact = 'com.google.protobuf:protoc:3.3.0'
            }
            plugins {
                javalite {
                    artifact = "com.google.protobuf:protoc-gen-javalite:3.0.0"
                }
                grpc {
                    artifact = "io.grpc:protoc-gen-grpc-java:${grpcVersion}"
                }
            }
            generateProtoTasks {
                all().each { task ->
                    task.plugins {
                        javalite {}
                        grpc {
                            option 'lite'
                        }
                    }
                }
            }
        }
        //--/>
        
        dependencies {
            implementation fileTree(dir: 'libs', include: ['*.jar'])
            implementation 'androidx.appcompat:appcompat:1.3.1'
            implementation 'androidx.constraintlayout:constraintlayout:1.1.3'
            implementation 'com.google.code.gson:gson:2.8.5'
            testImplementation 'junit:junit:4.12'
            androidTestImplementation 'androidx.test.ext:junit:1.1.0'
            androidTestImplementation 'androidx.test.espresso:espresso-core:3.1.1'
        
            // Kebbi SDK
            implementation (name:'NuwaSDK-2021-07-08_1058_2.1.0.08_e21fe7', ext:'aar')
            implementation (name:'NuwaBLEInterface_2020-11-27_v1.0_62415eb_release', ext:'aar')
        		
        		//<!Edit Here>
            //gRPC Client
            implementation "io.grpc:grpc-okhttp:${grpcVersion}"
            implementation "io.grpc:grpc-protobuf-lite:${grpcVersion}"
            implementation "io.grpc:grpc-stub:${grpcVersion}"
            implementation 'javax.annotation:javax.annotation-api:1.3.2'
            protobuf 'com.google.protobuf:protobuf-java:3.3.1'
        		//--/>
        }
        
        // Add SDK_PATH
        repositories {
            flatDir {
                dirs'libs'
            }
        }
        ```
        
    -