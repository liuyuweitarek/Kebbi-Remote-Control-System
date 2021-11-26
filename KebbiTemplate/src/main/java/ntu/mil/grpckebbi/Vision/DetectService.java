package ntu.mil.grpckebbi.Vision;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import ntu.mil.grpckebbi.R;

import static ntu.mil.grpckebbi.Vision.Constants.MODE_FACE_DETECTION;
import static ntu.mil.grpckebbi.Vision.Constants.MODE_RECORD;

public class DetectService  extends Service implements ImageReader.OnImageAvailableListener {
    private static final String TAG = DetectService.class.getSimpleName();

    // Camera State
    private ImageReader imageReader;
    private CameraDevice cameraDevice;
    private String cameraId;
    private CameraCaptureSession session;
    private boolean isProcessingFrame = false;

    // Camera Utils Object
    private MediaRecorder mMediaRecorder;
    private final List<RecordListener> recordStateListeners = new ArrayList<>();
    private String outputFile;

    // Camera Utils Params
    private boolean rotateEnabled;
    private boolean showPreview;
    private int cameraMode = MODE_FACE_DETECTION;
    private final Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }
    private CountDownTimer detectTimeout;
    private Size mVideoSize;
    private int frame = 0;

    @Override
    public void onImageAvailable(ImageReader imageReader) {
        Image image = imageReader.acquireLatestImage();
        if (image == null)
            return;

        if (isProcessingFrame) {
            frame = 0;
            image.close();
            return;
        }

        frame ++;

        switch (cameraMode){
            default:
                if(graphicOverlay != null)
                    graphicOverlay.clear();
                image.close();
                break;
        }
    }

    //Camera Preview
    private Size previewSize;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(320, 240);
    private WindowManager windowManager = null;
    private View rootView = null;
    private AutoFitTextureView textureView = null;
    private GraphicOverlay graphicOverlay;

    // Camera Service Life Cycle
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");

        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        rotateEnabled = intent.getBooleanExtra("rotateEnabled", false);
        cameraMode = intent.getIntExtra("cameraMode", MODE_FACE_DETECTION);
        showPreview = intent.getBooleanExtra("showPreview", false);
        int timeout = intent.getIntExtra("timeout", -1);

        if (timeout > 0) {
            detectTimeout = new CountDownTimer(timeout * 1000, 1000) {
                @Override
                public void onTick(long l) {
                }

                @Override
                public void onFinish() {
                    switch (cameraMode) {
                        case MODE_RECORD:
                            for (RecordListener listener : recordStateListeners)
                                listener.onStateChange(null);
                            break;
                    }
                }
            }.start();
        }
        initializeCamera();
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public DetectService getService() {
            return DetectService.this;
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        closePreview();
        closeCamera();

        if(rootView != null){
            windowManager.removeView(rootView);
            rootView = null;
        }
    }

    // Camera Setup
    private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@Nullable CameraDevice camera) {
            mCameraOpenCloseLock.release();
            DetectService.this.cameraDevice = camera;
            openPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            cameraDevice = null;
            stopSelf();
        }
    };

    @SuppressLint("MissingPermission")
    private void initializeCamera(){
        getFrontCameraId();

        if(showPreview){
            setupCameraOutputs();
            configureTransform();
        }

        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            if(!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS))
                throw new RuntimeException("Camera timeout");
            manager.openCamera(cameraId, cameraStateCallback, null);
            imageReader = ImageReader.newInstance(DESIRED_PREVIEW_SIZE.getWidth(), DESIRED_PREVIEW_SIZE.getHeight(), ImageFormat.YUV_420_888, 4);
            imageReader.setOnImageAvailableListener(this, null);
        } catch (CameraAccessException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 16 / 9) {
                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }

    private void getFrontCameraId() {
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (manager == null)
                return;

            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cOrientation == CameraCharacteristics.LENS_FACING_FRONT) {
                    this.cameraId = cameraId;
                    break;
                }
            }
        } catch (final CameraAccessException e) {
            Log.e(TAG, "Camera access exception!");
        }
    }

    private void setupCameraOutputs(){
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try{
            if (manager == null)
                return;

            final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            final StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null)
                return;

            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), DESIRED_PREVIEW_SIZE.getWidth(), DESIRED_PREVIEW_SIZE.getHeight());
            prepareOverlay();
            textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());

        }catch (final CameraAccessException e) {
            Log.e(TAG, "Camera access exception!");
        } catch (final NullPointerException e) {
            Log.e(TAG, "Null pointer exception!");
            e.printStackTrace();
        }
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
        final int minSize = Math.max(Math.min(width, height), 90);
        final Size desiredSize = new Size(width, height);
        boolean exactSizeFound = false;
        final List<Size> bigEnough = new ArrayList<>();
        for (final Size option : choices) {
            if (option.equals(desiredSize))
                exactSizeFound = true;

            if (option.getHeight() >= minSize && option.getWidth() >= minSize)
                bigEnough.add(option);
        }

        if (exactSizeFound)
            return desiredSize;

        if (bigEnough.size() > 0)
            return Collections.min(bigEnough, new CompareSizesByArea());
        else
            return choices[0];
    }

    private void configureTransform() {
        WindowManager window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        int rotation = 0;
        if (window != null)
            rotation = window.getDefaultDisplay().getRotation();

        Matrix matrix = new Matrix();
        RectF viewRect;
        if(textureView != null)
            viewRect = new RectF(0, 0, textureView.getWidth(), textureView.getHeight());
        else
            viewRect = new RectF(0,0, DESIRED_PREVIEW_SIZE.getWidth(),DESIRED_PREVIEW_SIZE.getHeight());
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());

        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) textureView.getHeight() / previewSize.getHeight(),
                    (float) textureView.getWidth() / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }

        if(textureView != null)
            textureView.setTransform(matrix);
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();

            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if(null != session){
                session.close();
                session = null;
            }
            if (null != imageReader) {
                imageReader.close();
                imageReader = null;
            }
            if(null != mMediaRecorder){
                mMediaRecorder.release();
                mMediaRecorder = null;
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mCameraOpenCloseLock.release();
        }
    }


    // Camera Preview Setup
    private final CameraCaptureSession.StateCallback previewCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            DetectService.this.session = session;
            try {
                CaptureRequest request;

                try {
                    CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                    //Branch for image processing
                    builder.addTarget(imageReader.getSurface());
                    //Branch for displaying camera preview in overlay
                    if(showPreview){
                        SurfaceTexture texture = textureView.getSurfaceTexture();
                        if(texture != null){
                            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
                            builder.addTarget(new Surface(texture));
                        }
                    }
                    //Branch for video recorder
                    if(cameraMode == MODE_RECORD)
                        builder.addTarget(mMediaRecorder.getSurface());
                    request =  builder.build();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                    request = null;
                }

                if(request != null)
                    session.setRepeatingRequest(request, null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
        }
    };

    private void openPreview(){
        if (cameraDevice == null)
            return;

        try {
            closePreview();
            List<Surface> surfaces = new ArrayList<>();
            surfaces.add(imageReader.getSurface());

            if(showPreview){
                SurfaceTexture texture = textureView.getSurfaceTexture();
                if(texture != null){
                    texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
                    surfaces.add(new Surface(texture));
                }
            }

            if(cameraMode == MODE_RECORD){
                configureRecorder();
                surfaces.add(mMediaRecorder.getSurface());
                if(mMediaRecorder != null)
                    mMediaRecorder.start();
                for(RecordListener listener : recordStateListeners)
                    listener.onStateChange(null);
            }
            cameraDevice.createCaptureSession(surfaces, previewCallback, null);
        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }

    }

    private void closePreview() {
        try{
            for(RecordListener listener : recordStateListeners)
                listener.onStateChange(outputFile);

            if(session != null){
                session.stopRepeating();
                session.abortCaptures();
            }

            if(cameraMode == MODE_RECORD && mMediaRecorder != null){
                mMediaRecorder.stop();
                mMediaRecorder.reset();
            }

        }catch(RuntimeException | CameraAccessException e){
            Log.d(TAG, "fail");
            e.printStackTrace();
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    private void prepareOverlay(){
        graphicOverlay.setImageSourceInfo(previewSize.getWidth(), previewSize.getHeight(), true);
        graphicOverlay.clear();
    }


    // Output File Setup
    private String getOutputVideoFile(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "bgRec");
        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(new Date());
        File file = new File(mediaStorageDir.getPath() + File.separator + "VID_"+ timeStamp + ".mp4");

        ContentValues values = new ContentValues(3);
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.TITLE, file.getName());
        values.put(MediaStore.Video.Media.DATA, file.getAbsolutePath());
        getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

        return file.getAbsolutePath();
    }

    private int getRotationCompensation() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        int deviceRotation = wm.getDefaultDisplay().getRotation();
        int rotationCompensation = ORIENTATIONS.get(deviceRotation);
        try {
            CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
            int sensorOrientation = manager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.SENSOR_ORIENTATION);
            int lensFacing = manager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING);
            if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT)
                rotationCompensation = (sensorOrientation + rotationCompensation) % 360;
            else
                rotationCompensation = (sensorOrientation - rotationCompensation + 360) % 360;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return rotationCompensation;
    }


    // Final Setup Recorder
    private void configureRecorder() throws IOException {
        outputFile = getOutputVideoFile();
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setOutputFile(outputFile);
        //mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        //mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setOrientationHint(getRotationCompensation());
        mMediaRecorder.prepare();
    }

    // Public Method For adding Camera Service
    public void addRecordListener (@NonNull RecordListener listener){
        recordStateListeners.add(listener);
    }
}
