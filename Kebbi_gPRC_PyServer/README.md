1. Generate *_pb2.py *_pb2_grpc.py

$ cd {Your .proto file's PATH}
$ python -m grpc_tools.protoc --proto_path=. {Your .proto filename} --python_out={Your server's dir path} --grpc_python_out={Your server's dir path}
(e.g.)
$ python -m grpc_tools.protoc --proto_path=. ./interaction.proto --python_out=../../../../Kebbi_gPRC_PyServer/. --grpc_python_out=../../../../Kebbi_gPRC_PyServer/.
