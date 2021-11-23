# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: interaction.proto
"""Generated protocol buffer code."""
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()




DESCRIPTOR = _descriptor.FileDescriptor(
  name='interaction.proto',
  package='interaction',
  syntax='proto3',
  serialized_options=b'\n!com.interaction.robot.interactionB\rInteractProtoP\001\242\002\002ST',
  create_key=_descriptor._internal_create_key,
  serialized_pb=b'\n\x11interaction.proto\x12\x0binteraction\"%\n\x13RobotConnectRequest\x12\x0e\n\x06status\x18\x01 \x01(\t\"#\n\x11RobotConnectReply\x12\x0e\n\x06status\x18\x01 \x01(\t\"\x1f\n\nRobotInput\x12\x11\n\tutterance\x18\x01 \x01(\t\" \n\x0bRobotOutput\x12\x11\n\tutterance\x18\x01 \x01(\t2\xa0\x01\n\x08Interact\x12R\n\x0cRobotConnect\x12 .interaction.RobotConnectRequest\x1a\x1e.interaction.RobotConnectReply\"\x00\x12@\n\tRobotSend\x12\x17.interaction.RobotInput\x1a\x18.interaction.RobotOutput\"\x00\x42\x39\n!com.interaction.robot.interactionB\rInteractProtoP\x01\xa2\x02\x02STb\x06proto3'
)




_ROBOTCONNECTREQUEST = _descriptor.Descriptor(
  name='RobotConnectRequest',
  full_name='interaction.RobotConnectRequest',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='status', full_name='interaction.RobotConnectRequest.status', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=34,
  serialized_end=71,
)


_ROBOTCONNECTREPLY = _descriptor.Descriptor(
  name='RobotConnectReply',
  full_name='interaction.RobotConnectReply',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='status', full_name='interaction.RobotConnectReply.status', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=73,
  serialized_end=108,
)


_ROBOTINPUT = _descriptor.Descriptor(
  name='RobotInput',
  full_name='interaction.RobotInput',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='utterance', full_name='interaction.RobotInput.utterance', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=110,
  serialized_end=141,
)


_ROBOTOUTPUT = _descriptor.Descriptor(
  name='RobotOutput',
  full_name='interaction.RobotOutput',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  create_key=_descriptor._internal_create_key,
  fields=[
    _descriptor.FieldDescriptor(
      name='utterance', full_name='interaction.RobotOutput.utterance', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=b"".decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR,  create_key=_descriptor._internal_create_key),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=143,
  serialized_end=175,
)

DESCRIPTOR.message_types_by_name['RobotConnectRequest'] = _ROBOTCONNECTREQUEST
DESCRIPTOR.message_types_by_name['RobotConnectReply'] = _ROBOTCONNECTREPLY
DESCRIPTOR.message_types_by_name['RobotInput'] = _ROBOTINPUT
DESCRIPTOR.message_types_by_name['RobotOutput'] = _ROBOTOUTPUT
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

RobotConnectRequest = _reflection.GeneratedProtocolMessageType('RobotConnectRequest', (_message.Message,), {
  'DESCRIPTOR' : _ROBOTCONNECTREQUEST,
  '__module__' : 'interaction_pb2'
  # @@protoc_insertion_point(class_scope:interaction.RobotConnectRequest)
  })
_sym_db.RegisterMessage(RobotConnectRequest)

RobotConnectReply = _reflection.GeneratedProtocolMessageType('RobotConnectReply', (_message.Message,), {
  'DESCRIPTOR' : _ROBOTCONNECTREPLY,
  '__module__' : 'interaction_pb2'
  # @@protoc_insertion_point(class_scope:interaction.RobotConnectReply)
  })
_sym_db.RegisterMessage(RobotConnectReply)

RobotInput = _reflection.GeneratedProtocolMessageType('RobotInput', (_message.Message,), {
  'DESCRIPTOR' : _ROBOTINPUT,
  '__module__' : 'interaction_pb2'
  # @@protoc_insertion_point(class_scope:interaction.RobotInput)
  })
_sym_db.RegisterMessage(RobotInput)

RobotOutput = _reflection.GeneratedProtocolMessageType('RobotOutput', (_message.Message,), {
  'DESCRIPTOR' : _ROBOTOUTPUT,
  '__module__' : 'interaction_pb2'
  # @@protoc_insertion_point(class_scope:interaction.RobotOutput)
  })
_sym_db.RegisterMessage(RobotOutput)


DESCRIPTOR._options = None

_INTERACT = _descriptor.ServiceDescriptor(
  name='Interact',
  full_name='interaction.Interact',
  file=DESCRIPTOR,
  index=0,
  serialized_options=None,
  create_key=_descriptor._internal_create_key,
  serialized_start=178,
  serialized_end=338,
  methods=[
  _descriptor.MethodDescriptor(
    name='RobotConnect',
    full_name='interaction.Interact.RobotConnect',
    index=0,
    containing_service=None,
    input_type=_ROBOTCONNECTREQUEST,
    output_type=_ROBOTCONNECTREPLY,
    serialized_options=None,
    create_key=_descriptor._internal_create_key,
  ),
  _descriptor.MethodDescriptor(
    name='RobotSend',
    full_name='interaction.Interact.RobotSend',
    index=1,
    containing_service=None,
    input_type=_ROBOTINPUT,
    output_type=_ROBOTOUTPUT,
    serialized_options=None,
    create_key=_descriptor._internal_create_key,
  ),
])
_sym_db.RegisterServiceDescriptor(_INTERACT)

DESCRIPTOR.services_by_name['Interact'] = _INTERACT

# @@protoc_insertion_point(module_scope)