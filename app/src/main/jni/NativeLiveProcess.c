/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
#include <sys/types.h>
#include <dirent.h>
#include <sys/stat.h>
#include <unistd.h>
#include <pthread.h>
#include <netdb.h>
#include <signal.h>
#include <stdarg.h>
#include <assert.h>
#include <string.h>
#include <time.h>
#include <netinet/in.h>
#include <sys/param.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/file.h>
#include <netinet/in.h>
#include <netinet/ip.h>
#include <arpa/inet.h>
#include <arpa/telnet.h>
#include <stdio.h>
#include <signal.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <netdb.h>

/*
 * Class:     com_juju_app_media_jni_NativeLiveProcess
 * Method:    processMediaData
 * Signature: ([BLjava/lang/String;I)I
 */

int sock;

JNIEXPORT jint JNICALL Java_com_juju_app_media_jni_NativeLiveProcess_init
        (JNIEnv * env, jobject thiz, jstring jdata){
    int port = 8881;

    struct sockaddr_in sa;
    memset( &sa, 0, sizeof( sa ) );
    sa.sin_family = AF_INET;
    sa.sin_port = htons( port );
    sa.sin_addr.s_addr = inet_addr( "192.168.1.116" );

    int ret = 0;
    sock = socket( AF_INET, SOCK_STREAM, 0 );
    connect( sock, ( struct sockaddr* )&sa, sizeof( sa ) ) ;
    return 0;
}

/*
 * Class:     com_juju_app_media_jni_NativeLiveProcess
 * Method:    unInit
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_juju_app_media_jni_NativeLiveProcess_unInit
        (JNIEnv * env, jobject thiz){
    shutdown( sock, 2 );
    close( sock );
    return 0;
}

/*
 * Class:     com_juju_app_media_jni_NativeLiveProcess
 * Method:    start
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_juju_app_media_jni_NativeLiveProcess_start
        (JNIEnv * env, jobject thiz, jstring jdata){
    return 0;
}

/*
 * Class:     com_juju_app_media_jni_NativeLiveProcess
 * Method:    stop
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_juju_app_media_jni_NativeLiveProcess_stop
        (JNIEnv * env, jobject thiz, jint jchannel){
    return 0;
}

/*
 * Class:     com_juju_app_media_jni_NativeLiveProcess
 * Method:    inputVideoData
 * Signature: ([B)V
 */
JNIEXPORT void JNICALL Java_com_juju_app_media_jni_NativeLiveProcess_inputVideoData
        (JNIEnv * env, jobject thiz, jbyteArray jdata){
    jint size = (*env)->GetArrayLength(env,jdata );
    char * data = ( char * )(*env)->GetByteArrayElements(env, jdata, JNI_FALSE );

    jstring strencode = (*env)->NewStringUTF(env, "utf-8" );
    jclass clsstring = (*env)->FindClass(env,"java/lang/String");
    jmethodID mid = (*env)->GetMethodID(env, clsstring, "getBytes", "(Ljava/lang/String;)[B" );

     send( sock, ( char * )data, size, 0 );
    (*env)->ReleaseByteArrayElements(env, jdata, ( jbyte * )data, 0 );
}

/*
 * Class:     com_juju_app_media_jni_NativeLiveProcess
 * Method:    inputAudioData
 * Signature: ([B)V
 */
JNIEXPORT void JNICALL Java_com_juju_app_media_jni_NativeLiveProcess_inputAudioData
        (JNIEnv * env, jobject thiz, jbyteArray jdata){
}