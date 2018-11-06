#include "com_example_anlan_qrtracker_MainActivity.h"
#include <android/bitmap.h>
//#include <opencv2/opencv.hpp>
#include <android/log.h>
#include <math.h>
/*
using namespace cv;
using namespace std;
 */

#define TAG "QRTRACKER"
#define printf(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

JNIEXPORT void JNICALL
Java_com_example_anlan_qrtracker_MainActivity_getEdge
        (JNIEnv *env, jobject obj, jobject bitmap) {
    AndroidBitmapInfo info;
    void *pixels;
    /*
    CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
    CV_Assert(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
              info.format == ANDROID_BITMAP_FORMAT_RGB_565);
    CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
    CV_Assert(pixels);
    if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        Mat temp(info.height, info.width, CV_8UC4, pixels);
        Mat gray;
        cvtColor(temp, gray, COLOR_RGBA2GRAY);
        Mat gb;
        GaussianBlur(gray, gb, CvSize(5, 5), 0);
        Mat edges;
        Canny(gray, edges, 100, 200);
        cvtColor(edges, temp, COLOR_GRAY2RGBA);
    }
    else {
        Mat temp(info.height, info.width, CV_8UC2, pixels);
        Mat gray;
        cvtColor(temp, gray, COLOR_RGBA2GRAY);
        Mat gb;
        GaussianBlur(gray, gb, CvSize(5, 5), 0);
        Mat edges;
        Canny(gray, edges, 100, 200);
        cvtColor(edges, temp, COLOR_GRAY2RGBA);
    }
    AndroidBitmap_unlockPixels(env, bitmap);
     */
}
