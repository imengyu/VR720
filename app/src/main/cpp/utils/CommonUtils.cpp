//
// Created by roger on 2020/12/21.
//

#include "CommonUtils.h"
#include "md5.h"

int appSignature[] = {
        217,111,122,103,116,107,116,219,109,111,111,213,215,215,120,118,
        109,217,111,210,113,208,217,109,217,219,116,210,213,118,109,217
};
int key1 = 36;
int key2 = 6;
int key3 = 103;

int CommonUtils::CheckAppSignature(JNIEnv *env, jobject context, int *buf) {

    char outPackageName[50];
    memset(outPackageName, 0, sizeof(outPackageName));

    jclass android_content_Context = env->GetObjectClass(context);
    jmethodID midGetPackageName = env->GetMethodID(android_content_Context, "getPackageName", "()Ljava/lang/String;");
    auto PackageName = (jstring)env->CallObjectMethod(context, midGetPackageName);

    bool ret = false;
    if(PackageName != nullptr) {
        // get UTF8 string & copy to dest
        const char* charBuff = env->GetStringUTFChars(PackageName, nullptr);
        strcpy(outPackageName, charBuff);

        env->ReleaseStringUTFChars(PackageName, charBuff);
        env->DeleteLocalRef(PackageName);
    }
    env->DeleteLocalRef(android_content_Context);

    if(!strcmp(outPackageName, "")) return false;

    char packageMd5[32];
    MD5String(outPackageName, packageMd5, 32);

    int *data = new int[32];
    for(int i = 0; i< 32; i++)
        data[i] = ((((int)packageMd5[i]) * key1 + ((int)packageMd5[i]) - key2) * (key1 / (key2 & key3))) / key3;

    int currentCount = 0;
    for(int i = 0; i< 32; i++)
        currentCount -= data[i] == appSignature[i] ? data[i] : -data[i];

    *buf = currentCount;
    delete[] data;

    return -(*buf);
}

