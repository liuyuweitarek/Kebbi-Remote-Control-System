package ntu.mil.grpckebbi.Utils;

public class GrpcMessage {
    private  String Command;
    private  String Msg;

    public GrpcMessage(String command, String msg){
        this.Command = command;
        this.Msg = msg;
    }

    public GrpcMessage toGrpcMsg(String cmd, String msg){
        return new GrpcMessage(cmd, msg);
    }

    public String getCommand() {
        return this.Command;
    }

    public String getMsg() {
        return this.Msg;
    }
};

