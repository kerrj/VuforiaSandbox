package justin.vuforiasandbox;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Type;
import android.support.v4.app.ActivityCompat;
import android.util.Size;
import android.widget.Toast;

import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.Arrays;

/**
 * Created by Justin on 10/13/2016.
 */
public class FTCCamera {
    /*
    This class sets up the camera for renderscript/opencv interaction and delivers Allocations in an interface each frame
     */
    public interface AllocationListener {
        public void onAllocationAvailable(Allocation allocation);
    }

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private String mCameraId;
    private Context context;
    private Handler mHandler;
    private Allocation mAllocationIn;
    private RenderScript mRS;
    private AllocationListener allocationListener;

    private Allocation.OnBufferAvailableListener listener = new Allocation.OnBufferAvailableListener() {
        @Override
        public void onBufferAvailable(Allocation a) {
            mAllocationIn.ioReceive();//mAllocationIn now has the current camera frame, is ready for processing
            allocationListener.onAllocationAvailable(mAllocationIn);
        }
    };


    public void setListener(AllocationListener listener) {
        this.allocationListener = listener;
    }

    public FTCCamera(int WIDTH, int HEIGHT) {
        mRS = RenderScript.create(MainActivity.getActivity().getBaseContext());
        context = MainActivity.getActivity().getBaseContext();
        mAllocationIn = Allocation.createTyped(mRS, Type.createXY(mRS, Element.RGBA_8888(mRS), WIDTH, HEIGHT),
                                               Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_IO_INPUT | Allocation.USAGE_GRAPHICS_TEXTURE | Allocation.USAGE_SCRIPT);
        //first initialize OpenCV
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, MainActivity.getActivity().getBaseContext(), new LoaderCallbackInterface() {
            @Override
            public void onManagerConnected(int status) {
                if (status == LoaderCallbackInterface.SUCCESS) {
                }
            }

            @Override
            public void onPackageInstall(int operation, InstallCallbackInterface callback) {

            }
        });
    }
    public void init(){
        startHandler();
        setupCamera();
    }


    private void startHandler() {
        HandlerThread mHandlerThread = new HandlerThread("CameraThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

    }


    private CameraDevice.StateCallback callback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int i) {

        }
    };

    private void setupCamera() {
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        try {
            for (String id : mCameraManager.getCameraIdList()) {
                CameraCharacteristics mCameraCharacteristics = mCameraManager.getCameraCharacteristics(id);
                if (mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    mCameraId = id;
                }
            }
            CameraCharacteristics mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap mStreamConfigurationMap = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = mStreamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888);

            if (ActivityCompat.checkSelfPermission(MainActivity.getActivity().getBaseContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mCameraManager.openCamera(mCameraId, callback, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }



    private void startPreview(){
        mAllocationIn.setOnBufferAvailableListener(listener);
        try {
            final CaptureRequest.Builder mCaptureRequestBuilder=mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(mAllocationIn.getSurface());
            mCameraDevice.createCaptureSession(Arrays.asList(mAllocationIn.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    try {
                        CaptureRequest mCaptureRequest = mCaptureRequestBuilder.build();
                        cameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, mHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


}
