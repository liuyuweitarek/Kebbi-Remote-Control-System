package ntu.mil.grpckebbi.Vision;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import java.io.ByteArrayOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


import ntu.mil.grpckebbi.MainActivity;
import ntu.mil.grpckebbi.R;

import static ntu.mil.grpckebbi.Vision.Constants.STREAM_OFF;
import static ntu.mil.grpckebbi.Vision.Constants.STREAM_PORT;
import static ntu.mil.grpckebbi.Vision.Constants.STREAM_STARTED;

public class VideoStreamService extends Service implements ImageReader.OnImageAvailableListener {
    private static final String TAG = VideoStreamService.class.getSimpleName();

    /** Preview Params */
    private static final int MINIMUM_PREVIEW_SIZE = 320;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(320, 240);
    private Bitmap rgbFrameBitmap = null;
    private Size mPreviewSize;
    private CaptureRequest.Builder mPreviewBuilder;
    protected int previewWidth = 0;
    protected int previewHeight = 0;

    /** Camera Params */
    private static final int ONGOING_NOTIFICATION_ID = 6660;
    private static final String CHANNEL_ID = "cam_service_channel_id";
    private static final String CHANNEL_NAME = "cam_service_channel_name";
    private String cameraId;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mPreviewSession;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private MediaRecorder mMediaRecorder;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /** Background Image Hadler */
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    /** UI */
    private View rootView = null;
    private AutoFitTextureView textureView = null;
    private WindowManager windowManager = null;

    /** Server */
    private ServerThread serverThread;

    /** Utils */
    private boolean shouldRecord = false;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;
    private Handler handler;
    private HandlerThread handlerThread;
    private VideoStreamListener videoStreamListener;

    /** Stream Buffer */
    private int[] rgbBytes = null;
    private boolean isProcessingFrame = false;
    private int yRowStride;
    private byte[][] yuvBytes = new byte[3][];


    /** Service Life Cycle */
    @Override
    public void onCreate() {
        handlerThread = new HandlerThread("video_stream");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        startForeground();
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }
    public class LocalBinder extends Binder {
        public VideoStreamService getService() {
            return VideoStreamService.this;
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        closeCamera();
        stopBackgroundThread();

        if(rootView != null)
            windowManager.removeView(rootView);

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            Log.e(TAG, "Exception!");
        }

        rgbFrameBitmap = null;
        serverThread.stop();
        setStreamState(STREAM_OFF);

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null)
            shouldRecord = intent.getBooleanExtra("record", false);
        startWithPreview();
        serverThread = new ServerThread(this, getLocalIpAddress(), STREAM_PORT, handler);
        Thread cThread = new Thread(serverThread);
        cThread.start();

        return START_STICKY;
    }

    /** Method that make CameraService don't be killed, while system sleeping. */
    private void startForeground(){
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction("STOP_CAM_SERVICE");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null)
                nm.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Background camera")
                .setContentText("Camera open in background. Click here to stop.")
                .setSmallIcon(R.drawable.ic_photocam)
                .setContentIntent(pendingIntent)
                .setTicker("Background camera")
                .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    /** Method that get IP for streaming server thread */
    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()&& inetAddress instanceof Inet4Address) { return inetAddress.getHostAddress(); }
                }
            }
        } catch (SocketException ex) {
            Log.e("ServerActivity", ex.toString());
        }
        return null;
    }

    /** Camera State Life Cycle */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            if(shouldRecord)
                stopRecordingVideo();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            stopSelf();
        }
    };

    /** Preview UI Listener (Not  really use in this project) */
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) { }
    };

    /** Setup Preview UI  */
    private void startWithPreview(){
        initOverlay();
        startBackgroundThread();
        if (textureView.isAvailable())
            openCamera(textureView.getWidth(), textureView.getHeight());
//            openCamera(160, 120);
        else
            textureView.setSurfaceTextureListener(mSurfaceTextureListener);
    }
    @SuppressLint("InflateParams")
    private void initOverlay(){
        LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (li != null) {
            rootView = li.inflate(R.layout.overlay_cam, null);
        }
        textureView = rootView.findViewById(R.id.texture_photo);

        int type;
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        else
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(type,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        if (windowManager != null)
            windowManager.addView(rootView, params);
    }
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /** Make camera service into background */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /** Camera utils */
    @SuppressWarnings("MissingPermission")
    // Open/Close camera
    private void openCamera(int width, int height) {
        setUpCameraOutputs();
        configureTransform(width, height);

        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS))
                throw new RuntimeException("Time out waiting to lock camera opening.");
            if (manager != null)
                manager.openCamera(cameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Toast.makeText(this, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            stopSelf();
        } catch (NullPointerException e) {
            Log.e(TAG, "Camera error");
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            closePreviewSession();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mMediaRecorder) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    // Previewing
    private void startPreview() {
        Log.d(TAG, "startPreview()");
        try {
            if (null == mCameraDevice || !textureView.isAvailable() || null == mPreviewSize)
                return;
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            Surface previewSurface = new Surface(texture);

            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder.addTarget(previewSurface);

            Log.i(TAG, "Opening camera preview: " + mPreviewSize.getWidth() + "x" + mPreviewSize.getHeight());
            setStreamState(STREAM_STARTED);

            ImageReader previewReader = ImageReader.newInstance(
                    mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 2);

            previewReader.setOnImageAvailableListener(this, mBackgroundHandler);
            mPreviewBuilder.addTarget(previewReader.getSurface());

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, previewReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            mPreviewSession = cameraCaptureSession;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Toast.makeText(getBaseContext(), "Failed", Toast.LENGTH_SHORT).show();
                        }
                    }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void updatePreview() {
        if (null == mCameraDevice)
            return;
        try {
            mPreviewBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void configureTransform(int viewWidth, int viewHeight) {

        if (null == textureView || null == mPreviewSize)
            return;

        WindowManager window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        int rotation = 0;
        if (window != null)
            rotation = window.getDefaultDisplay().getRotation();

        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());

        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }

        textureView.setTransform(matrix);
    }
    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

    // Dealing pixels
    private void setUpCameraOutputs(){
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try{
            if (manager == null)
                return;

            cameraId = manager.getCameraIdList()[0];
            final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            final StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            if (map != null)
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), DESIRED_PREVIEW_SIZE.getWidth(), DESIRED_PREVIEW_SIZE.getHeight());

            previewWidth = mPreviewSize.getWidth();
            previewHeight = mPreviewSize.getHeight();
            textureView.setAspectRatio(previewWidth, previewHeight);
            rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);

        }catch (final CameraAccessException e) {
            Log.e(TAG, "Camera access exception!");
        } catch (final NullPointerException e) {
            Log.e(TAG, "Null pointer exception!");
            Log.e(TAG, e.getMessage());
        }
    }
    private void stopRecordingVideo() {
        try{
            mMediaRecorder.stop();
            mMediaRecorder.reset();
        }catch(RuntimeException stopException){
            Log.d(TAG, "Stop Recording Fail");
        }
    }
    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
        final int minSize = Math.max(Math.min(width, height), MINIMUM_PREVIEW_SIZE);
        final Size desiredSize = new Size(width, height);

        boolean exactSizeFound = false;
        final List<Size> bigEnough = new ArrayList<>();
        for (final Size option : choices) {
            if (option.equals(desiredSize)) {
                exactSizeFound = true;
                break;
            }

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
    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }
    protected ByteArrayOutputStream getFrame() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rgbFrameBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream;
    }
    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null)
                yuvBytes[i] = new byte[buffer.capacity()];
            buffer.get(yuvBytes[i]);
        }
    }

    @Override
    public void onImageAvailable(ImageReader imageReader) {
        if (previewWidth == 0 || previewHeight == 0)
            return;
        if (rgbBytes == null)
            rgbBytes = new int[previewWidth * previewHeight];
        final Image image = imageReader.acquireNextImage();
        if (image == null)
            return;

        if (isProcessingFrame) {
            image.close();
            return;
        }

        isProcessingFrame = true;
        final Image.Plane[] planes = image.getPlanes();
        fillBytes(planes, yuvBytes);
        yRowStride = planes[0].getRowStride();
        final int uvRowStride = planes[1].getRowStride();
        final int uvPixelStride = planes[1].getPixelStride();

        imageConverter = () -> ImageUtils.convertYUV420ToARGB8888(
                yuvBytes[0],
                yuvBytes[1],
                yuvBytes[2],
                previewWidth,
                previewHeight,
                yRowStride,
                uvRowStride,
                uvPixelStride,
                rgbBytes);

        postInferenceCallback = () -> {
            image.close();
            isProcessingFrame = false;
        };

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
        readyForNextImage();
    }
    protected void readyForNextImage() {
        if (postInferenceCallback != null)
            postInferenceCallback.run();
    }


    /** Public Get VideoStreamService Methods */
    public void setStreamState(int streamState){
        Log.d(TAG, "setStreamState()");
        if(videoStreamListener != null)
            videoStreamListener.onStreamStateChange(streamState);
    }
    public void registerVideoStreamListener(VideoStreamListener videoStreamListener){
        this.videoStreamListener= videoStreamListener;
    }
}
