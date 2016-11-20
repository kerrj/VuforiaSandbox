#pragma version(1)
#pragma rs java_package_name(com.justin.opencvcamera)
#pragma RS_FP_IMPRECISE

uchar4 __attribute__ ((kernel)) split(uchar4 in, uint32_t x, uint32_t y){

    //rgb filter=========================================================
    //IN.B IS RED AND IN.R IS BLUE
    uchar4 pixelOut;
    if(in.r>in.g+40&&in.r>in.b+40){
        pixelOut=(uchar4){0,0,255,255};
    }else{
        pixelOut=(uchar4){0,0,0,255};
   }
   return pixelOut;

}




