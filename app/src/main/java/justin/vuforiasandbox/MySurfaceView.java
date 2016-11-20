package justin.vuforiasandbox;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.Type;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.justin.opencvcamera.ScriptC_black;
import com.justin.opencvcamera.ScriptC_blue;
import com.justin.opencvcamera.ScriptC_colorsplit;
import com.justin.opencvcamera.ScriptC_red;
import com.vuforia.Image;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Justin on 10/2/2016.
 */
public class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder holder;
    private UpdateThread thread=new UpdateThread();
    private FTCVuforia vuforia;
    private FTCCamera camera;
    private Bitmap RGB565Bitmap =Bitmap.createBitmap(1280, 720, Bitmap.Config.RGB_565);
    private ScriptC_colorsplit colorsplit;
    private ScriptC_blue blue;
    private ScriptC_red red;
    private ScriptC_black black;
    private RenderScript mRS;
    private Allocation mAllocationIn;
    private Allocation getmAllocationOut;
    private Object synch=new Object();
    private ArrayList<Path> contours=new ArrayList<>();
    private Bitmap RGBABitmap =Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
    private ScriptIntrinsicBlur blur;
    private ArrayList<double[]> circles=new ArrayList<>();





    public MySurfaceView(Context context) {
        super(context);
        getHolder().addCallback(this);
    }

    public MySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);

    }

    public MySurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        this.holder=holder;
        mRS=RenderScript.create(MainActivity.getActivity().getBaseContext());
        blur=ScriptIntrinsicBlur.create(mRS,Element.RGBA_8888(mRS));
        colorsplit=new ScriptC_colorsplit(mRS);
        red=new ScriptC_red(mRS);
        blue=new ScriptC_blue(mRS);
        black=new ScriptC_black(mRS);
        mAllocationIn = Allocation.createTyped(mRS, Type.createXY(mRS, Element.RGBA_8888(mRS), 1280, 720),
                                               Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_GRAPHICS_TEXTURE | Allocation.USAGE_SCRIPT);
        getmAllocationOut = Allocation.createTyped(mRS, Type.createXY(mRS, Element.RGBA_8888(mRS), 1280, 720),
                                               Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_GRAPHICS_TEXTURE | Allocation.USAGE_SCRIPT);
        vuforia=new FTCVuforia(MainActivity.getActivity());
        vuforia.addTrackables("FTC_2016-17.xml");
        vuforia.initVuforia();

        camera=new FTCCamera(1280,720);
//        camera.setListener(new FTCCamera.AllocationListener() {
//            @Override
//            public void onAllocationAvailable(Allocation allocation) {
//                allocation.copyTo(RGB565Bitmap);
//            }
//        });
        thread=new UpdateThread();
        thread.start();
    }


    private double CAMERA_FOV=61;
    private double TARGET_WIDTH=254;
    public void takeSnapshot(){
        synchronized (synch){
            contours.clear();
            circles.clear();
        }
        Image i=vuforia.getLastFrame();
        if(i!=null) {
            ByteBuffer buf=i.getPixels();
            RGB565Bitmap.copyPixelsFromBuffer(buf);
            Mat original=new Mat();
            //convert to rgba from rgb565
            Utils.bitmapToMat(RGB565Bitmap, original);
            Imgproc.cvtColor(original,original,Imgproc.COLOR_BGR2BGRA);
            Utils.matToBitmap(original, RGBABitmap);
            //split color onto blueMat Mat
            mAllocationIn.copyFrom(RGBABitmap);
            blur.setInput(mAllocationIn);
            blur.setRadius(10);
            getmAllocationOut.copyTo(RGBABitmap);
            blur.forEach(getmAllocationOut);
            getmAllocationOut.copyTo(RGBABitmap);
            blue.forEach_split(getmAllocationOut,mAllocationIn);
            mAllocationIn.copyTo(RGBABitmap);
            Mat blueMat=new Mat();
            Utils.bitmapToMat(RGBABitmap, blueMat);
            red.forEach_split(getmAllocationOut,mAllocationIn);
            mAllocationIn.copyTo(RGBABitmap);
            Mat redMat=new Mat();
            Utils.bitmapToMat(RGBABitmap, redMat);
            Utils.matToBitmap(redMat, RGBABitmap);
//            try {
//                File directory = MainActivity.getActivity().getBaseContext().getExternalFilesDir(null);
//                File image = new File(directory, Long.toString(System.currentTimeMillis()) + ".jpeg");
//                image.createNewFile();
//                FileOutputStream fos = new FileOutputStream(image);
//                RGBABitmap.compress(Bitmap.CompressFormat.JPEG, 50,fos) ;
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            //memes begin here
            HashMap<String, double[]> data = vuforia.getVuforiaData();
            if(data.containsKey("Wheels")){//gonna try to map the exact place in the image for the target based on its size and angle
                double[] wheelsData=data.get("Wheels");
                double yRotation=Math.toDegrees(wheelsData[1]);
                double totalDistance=Math.hypot(wheelsData[3],wheelsData[5]);
                double angleFromCameraDirection=Math.toDegrees(Math.atan2(wheelsData[3],wheelsData[5]));
                double horizontalPositionInImage=640+640*(angleFromCameraDirection/(CAMERA_FOV/2));
                if(horizontalPositionInImage>1280)horizontalPositionInImage=1280;
                if(horizontalPositionInImage<0)horizontalPositionInImage=0;
                double angleFromMiddleToSide=Math.toDegrees(Math.atan2(Math.sin(yRotation)*TARGET_WIDTH/2,totalDistance));
                double horizontalDeltaInImage=640*(angleFromMiddleToSide/(CAMERA_FOV/2));
                synchronized (synch){
                    circles.add(new double[]{horizontalPositionInImage,360,50});
                    circles.add(new double[]{horizontalPositionInImage+horizontalDeltaInImage,360,50});
                    circles.add(new double[]{horizontalPositionInImage-horizontalDeltaInImage,360,50});
                    Log.d("horizontalPosition",Double.toString(horizontalPositionInImage));
                    Log.d("horizontalDelta",Double.toString(horizontalDeltaInImage));
                }
            }
        }

//    public void takeSnapshot(){
//        synchronized (synch){
//            contours.clear();
//            circles.clear();
//        }
//        Image i=vuforia.getLastFrame();
//        if(i!=null) {
//            ByteBuffer buf=i.getPixels();
//            RGB565Bitmap.copyPixelsFromBuffer(buf);
//            Mat original=new Mat();
//            //convert to rgba from rgb565
//            Utils.bitmapToMat(RGB565Bitmap, original);
//            Imgproc.cvtColor(original,original,Imgproc.COLOR_BGR2BGRA);
//            Utils.matToBitmap(original, RGBABitmap);
//            //split color onto blueMat Mat
//            mAllocationIn.copyFrom(RGBABitmap);
//            blur.setInput(mAllocationIn);
//            blur.setRadius(10);
//            black.forEach_split(mAllocationIn,getmAllocationOut);
//            getmAllocationOut.copyTo(RGBABitmap);
//            Mat blackMat=new Mat();
//            Utils.bitmapToMat(RGBABitmap,blackMat);
//            Imgproc.cvtColor(blackMat,blackMat,Imgproc.COLOR_RGBA2GRAY);
//            Mat c=new Mat();
//            Imgproc.blur(blackMat,blackMat,new Size(5, 5));
//            Imgproc.HoughCircles(blackMat,c,Imgproc.HOUGH_GRADIENT,2,100,100,40,10,60);
//            Log.d("alive","==========================================================");
//            for(int j=0;j<c.cols();j++){
//                double[] center=c.get(0,j);
//                synchronized (synch) {
//                    circles.add(center);
//                    Log.d("circle center",Double.toString(center[0])+","+Double.toString(center[1]));
//                    Log.d("circle size",Double.toString(center[2]));
//                }
//            }
//            blur.forEach(getmAllocationOut);
//            getmAllocationOut.copyTo(RGBABitmap);
//            blue.forEach_split(getmAllocationOut,mAllocationIn);
//            mAllocationIn.copyTo(RGBABitmap);
//            Mat blueMat=new Mat();
//            Utils.bitmapToMat(RGBABitmap, blueMat);
//            red.forEach_split(getmAllocationOut,mAllocationIn);
//            mAllocationIn.copyTo(RGBABitmap);
//            Mat redMat=new Mat();
//            Utils.bitmapToMat(RGBABitmap, redMat);
//            Utils.matToBitmap(blackMat,RGBABitmap);
//            try {
//                File directory = MainActivity.getActivity().getBaseContext().getExternalFilesDir(null);
//                File image = new File(directory, Long.toString(System.currentTimeMillis()) + ".jpeg");
//                image.createNewFile();
//                FileOutputStream fos = new FileOutputStream(image);
//                RGBABitmap.compress(Bitmap.CompressFormat.JPEG, 50,fos) ;
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            //convert blueMat to grayscale for contours
//            Imgproc.cvtColor(blueMat,blueMat,Imgproc.COLOR_RGBA2GRAY);
//            Mat erode=new Mat(50,50,blueMat.type());
//            ArrayList<MatOfPoint> contours=new ArrayList<>();
//            Mat hierarchy=new Mat();
//            Imgproc.findContours(blueMat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
//            double blueAverage=0;
//            double index=0;
//            double blueArea=0;
//            for(MatOfPoint points:contours){
//                MatOfPoint2f points2f=new MatOfPoint2f();
//                MatOfPoint2f output=new MatOfPoint2f();
//                points.convertTo(points2f, CvType.CV_32FC2);
//                double arcLength=Imgproc.arcLength(points2f,true);
//                double area=Imgproc.contourArea(points);
//                if(area>30000) {
//                    index+=area;
//                    Imgproc.approxPolyDP(points2f, output, .01 * arcLength, true);
//                    blueAverage+=Imgproc.minAreaRect(points2f).center.x*area;
//                    Point[] pointArray = output.toArray();
//                    Path path = new Path();
//                    path.moveTo((float)pointArray[0].x+720,(float)pointArray[0].y);
//                    for(Point p:pointArray){
//                        path.lineTo((float)p.x+720,(float)p.y);
//                    }
//                    path.lineTo((float)pointArray[0].x+720,(float)pointArray[0].y);
//                    synchronized (synch){
//                        this.contours.add(path);
//                    }
//                }
//            }
//            blueArea=index;
//            if(index==0)blueAverage=0;
//            if(index>0)blueAverage/=index;
//
//            //repeat for red mat
//            Imgproc.cvtColor(redMat,redMat,Imgproc.COLOR_RGBA2GRAY);
//            contours=new ArrayList<>();
//            hierarchy=new Mat();
//            Imgproc.findContours(redMat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
//            double redAverage=0;
//            double redArea=0;
//            index=0;
//            for(MatOfPoint points:contours){
//                MatOfPoint2f points2f=new MatOfPoint2f();
//                MatOfPoint2f output=new MatOfPoint2f();
//                points.convertTo(points2f, CvType.CV_32FC2);
//                double arcLength=Imgproc.arcLength(points2f,true);
//                double area=Imgproc.contourArea(points);
//                if(area>30000) {
//                    index+=area;
//                    Imgproc.approxPolyDP(points2f, output, .01 * arcLength, true);
//                    redAverage+=Imgproc.minAreaRect(points2f).center.x*area;
//                    Point[] pointArray = output.toArray();
//                    Path path = new Path();
//                    path.moveTo((float)pointArray[0].x+720,(float)pointArray[0].y);
//                    for(Point p:pointArray){
//                        path.lineTo((float)p.x+720,(float)p.y);
//                    }
//                    path.lineTo((float)pointArray[0].x+720,(float)pointArray[0].y);
//                    synchronized (synch){
//                        this.contours.add(path);
//                    }
//                }
//            }
//            redArea=index;
//            if(index==0)redAverage=0;
//            if(index>0)redAverage/=index;
//            if(redAverage>0&&blueAverage>0) {
//                if (redAverage > blueAverage)
//                    Toast.makeText(MainActivity.getActivity().getBaseContext(), "Red is right", Toast.LENGTH_LONG).show();
//                if (redAverage < blueAverage)
//                    Toast.makeText(MainActivity.getActivity().getBaseContext(), "Red is left", Toast.LENGTH_LONG).show();
//            }else if(redAverage>0&&blueAverage==0&&redArea>100000){
//                Toast.makeText(MainActivity.getActivity().getBaseContext(), "All red", Toast.LENGTH_LONG).show();
//            }else if(blueAverage>0&&redAverage==0&&blueArea>100000){
//                Toast.makeText(MainActivity.getActivity().getBaseContext(), "All blue", Toast.LENGTH_LONG).show();
//            }else {
//                Toast.makeText(MainActivity.getActivity().getBaseContext(), "Inconclusive", Toast.LENGTH_LONG).show();
//            }
//        }
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        thread.setStop();
        try {
            vuforia.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
//    float rx=0,ry=0;
//    SensorEventListener sensorEventListener=new SensorEventListener() {
//
//        @Override
//        public void onSensorChanged(SensorEvent event) {
//            rx=event.values[0];
//            ry=event.values[1];
//        }
//
//        @Override
//        public void onAccuracyChanged(Sensor sensor, int accuracy) {
//
//        }
//    };
    private class UpdateThread extends Thread{
        private final float[] CENTER={10,900};
        private final float SCALE=.5f;
        private boolean running=true;
        public void setStop(){
            running=false;
        }

        public void run() {
            while (running) {
                try {
                    Canvas canvas = holder.lockCanvas();
                    Paint paint = new Paint();
                    canvas.drawColor(Color.WHITE);
                    paint.setColor(Color.BLACK);
                    paint.setTextSize(70);
                    HashMap<String, double[]> data = vuforia.getVuforiaData();
                    if(RGB565Bitmap !=null){
                        canvas.rotate(90,720,0);
                        canvas.drawBitmap(RGBABitmap, 720, 0, paint);
                        synchronized (synch){
                            paint.setStyle(Paint.Style.STROKE);
                            paint.setStrokeWidth(10);
                            paint.setColor(Color.GREEN);
                            for(Path path:contours) {
                                canvas.drawPath(path,paint);
                            }
                        }
                        canvas.rotate(-90,720,0);
                    }
                    synchronized (synch) {
                        for (double[] d : circles) {
                            canvas.drawCircle((float) (720.0-d[1]), (float) d[0], (float) d[2], paint);
                        }
                    }

                    if (data.containsKey("Wheels")) {
                        try {
                            //                        telemetry.addData("rotationx", data.get("resq")[0]);
                            //                        telemetry.addData("rotationy", data.get("resq")[1]);
                            //                        telemetry.addData("rotationz", data.get("resq")[2]);
                            //                        telemetry.addData("distancex", data.get("resq")[3]);
                            //                        telemetry.addData("distancey", data.get("resq")[4]);
                            //                        telemetry.addData("distancez", data.get("resq")[5]);

                            canvas.rotate(90+(float)Math.toDegrees(data.get("Wheels")[1]),CENTER[0] + (float) data.get("Wheels")[5] * SCALE,CENTER[1] + (float) data.get("Wheels")[3] * SCALE);
                            canvas.drawRect(CENTER[0] + (float) data.get("Wheels")[5] * SCALE-100,
                                              CENTER[1] + (float) data.get("Wheels")[3] * SCALE-10,
                                            CENTER[0] + (float) data.get("Wheels")[5] * SCALE+100,
                                            CENTER[1] + (float) data.get("Wheels")[3] * SCALE+10,paint);
                            canvas.drawText("Wheels",CENTER[0] + (float) data.get("Wheels")[5] * SCALE-100,
                                            CENTER[1] + (float) data.get("Wheels")[3] * SCALE+70,paint);
                            canvas.rotate(-90-(float)Math.toDegrees(data.get("Wheels")[1]),CENTER[0] + (float) data.get("Wheels")[5] * SCALE,CENTER[1] + (float) data.get("Wheels")[3] * SCALE);
                        } catch (NullPointerException n) {
                            n.printStackTrace();
                        }
                    }
                    canvas.drawCircle(CENTER[0], CENTER[1], 10, paint);
                    canvas.drawLine(CENTER[0], CENTER[1], CENTER[0] + 30, CENTER[1], paint);
                    holder.unlockCanvasAndPost(canvas);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
