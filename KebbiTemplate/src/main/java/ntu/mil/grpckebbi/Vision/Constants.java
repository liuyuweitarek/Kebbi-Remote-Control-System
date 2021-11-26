package ntu.mil.grpckebbi.Vision;

public interface Constants {
        int STREAM_STARTED = 1;
        int STREAM_CONNECTED = 2;
        int STREAM_OFF = 3;
        int STREAM_CANCELED = 4;
        int STREAM_PORT = 9191;

        int MODE_RECORD = 1;
        int MODE_FACE_DETECTION = 2;
        int MODE_FACE_FINDING = 3;
        int MODE_FACE_FOLLOWING = 4;
        int MODE_POSE_DETECTION = 5;
        int MODE_OBJECT_DETECTION = 6;
}

