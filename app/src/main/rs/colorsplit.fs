#pragma version(1)
#pragma rs java_package_name(com.justin.opencvcamera)
#pragma RS_FP_IMPRECISE

uchar3 rgb;
//float XYZ_WHITE_REFERENCE_X = 95.047f;
//float XYZ_WHITE_REFERENCE_Y = 100.0f;
//float XYZ_WHITE_REFERENCE_Z = 108.883f;
//float XYZ_EPSILON = 0.008856f;
//float XYZ_KAPPA = 903.3f;

//float RED_A=80.109360f;
//float RED_B=67.220062f;

//float BLUE_A=79.196678f;
//float BLUE_B=-107.863686f;

uchar4 __attribute__ ((kernel)) split(uchar4 in, uint32_t x, uint32_t y){
    //lab filter==================================================
    //float sr = in.r / 255.0f;
    //sr = sr < 0.04045f ? sr / 12.92f : native_powr((float)(sr + 0.055f) / 1.055f, 2.4f);
    //float sg = in.g / 255.0f;
    //sg = sg < 0.04045f ? sg / 12.92f : native_powr((float)(sg + 0.055f) / 1.055f, 2.4f);
    //float sb = in.b / 255.0f;
    //sb = sb < 0.04045f ? sb / 12.92f : native_powr((float)(sb + 0.055f) / 1.055f, 2.4f);

    //float3 outXyz = (float3){(float)100.0f * (sr * 0.4124f + sg * 0.3576f + sb * 0.1805f),
    //                (float)100.0f * (sr * 0.2126f + sg * 0.7152f + sb * 0.0722f),
    //                (float)100.0f * (sr * 0.0193f + sg * 0.1192f + sb * 0.9505f)};

    //float x2 = pivotXyzComponent(outXyz.x / XYZ_WHITE_REFERENCE_X);
    //float y2 = pivotXyzComponent(outXyz.y / XYZ_WHITE_REFERENCE_Y);
    //float z2 = pivotXyzComponent(outXyz.z / XYZ_WHITE_REFERENCE_Z);
    //float3 outLab = (float3){(float)max((float)0, (float)116.0f * y2 - 16.0f),500.0f * (x2 - y2),200.0f * (y2 - z2)};
    //if(native_hypot(outLab.x-RED_A,outLab.y-RED_B)<50){
         //   return (uchar4){0,0,0,255};
        //}else{
        //   return (uchar4){255,255,255,255};
        //}
       //return (uchar4){0,0,0,255};


    //rgb filter=========================================================
    //IN.B IS RED AND IN.R IS BLUE
    uchar4 pixelOut;
    if(in.r>in.g+40&&in.r>in.b+20){
        pixelOut=(uchar4){0,0,255,255};
    }else if(in.b>in.g+40&&in.b>in.r+20){
        pixelOut=(uchar4){255,0,0,255};
    }else{
        pixelOut=(uchar4){0,0,0,255};
   }
   return pixelOut;
    //return (uchar4){255,0,0,0};

    //hsv filter========================================

    //float R=(float)in.r/255.000000f;
    //float G=(float)in.g/255.000000f;
    //float B=(float)in.b/255.000000f;

    //float Cmax=fmax(R,G);
    //Cmax=fmax(Cmax,B);

    //float Cmin=fmin(R,G);
    //Cmin=fmin(Cmin,B);

    //float delta=(float)Cmax-Cmin;

    //float H;
    //if(delta==0.0000000000f){
    //    H=0;
    //}else if(Cmax==R){
    //    H=(float)60.000000f*( (short)((G-B)/delta)%6);
    //}else if(Cmax==G){
    //    H=(float)60.000000f*( ((B-R)/delta)+2);
    //}else if(Cmax==B){
    //    H=(float)60.000000f*( ((R-G)/delta)+4);
   // }
   // if(x==640&&y==360){
    //    rsDebug("Cmax",Cmax);
     //   rsDebug("Cmin",Cmin);
      //  rsDebug("delta",delta);
      //  rsDebug("R",R);
      //  rsDebug("G",G);
      //  rsDebug("B",B);
      //  rsDebug("H",H);
   // }
    //uchar4 pixelOut;
    //if(H>345||H<15){
    //    pixelOut=(uchar4){255,255,255,255};
    //}else{
    //    pixelOut=(uchar4){0,0,0,255};
    //}
    //return pixelOut;
}




