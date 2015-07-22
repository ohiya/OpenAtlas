/* DO NOT EDIT THIS FILE - it is machine generated */
#include <stdlib.h>
#include <android/log.h>
#include <sys/file.h>
#include <errno.h>
#define LOG_TAG "dexopt"

#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

char *dexoptBin = "/bin/dexopt";
void dexopt(const char* apkPath,const char* dexPath,const char* args );

JNIEXPORT void JNICALL Java_com_openatlas_dexopt_InitExecutor_dexopt(JNIEnv *env, jclass clazz, jstring japkPtah , jstring jdexPtah, jstring jargs){

	const char* apkPath = env->GetStringUTFChars(japkPtah, 0);

	const char* dexPath = env->GetStringUTFChars(jdexPtah, 0);
	const char* args = env->GetStringUTFChars(jargs, 0);

	dexopt(apkPath,dexPath,args);

	env->ReleaseStringUTFChars( japkPtah, apkPath);
	env->ReleaseStringUTFChars( jdexPtah, dexPath);
	env->ReleaseStringUTFChars( jargs, args);
}


void dexopt(const char* apkPath,const char* dexPath,const char* args ){


    char *ANDROID_ROOT = getenv("ANDROID_ROOT");
    int sizeANDROID_ROOT = 0;
    if (ANDROID_ROOT != NULL) {
        sizeANDROID_ROOT = strlen(ANDROID_ROOT);
    }
    else {
        __android_log_print(6, "DexInv", "ANDROID_ROOT not set, defaulting to /system");
        sizeANDROID_ROOT = 7;
        ANDROID_ROOT = "/system";
    }


    int sizedexoptBin = strlen(dexoptBin);
    char *dest = (char *) malloc(sizedexoptBin + sizeANDROID_ROOT + 1);
    strcpy(dest, ANDROID_ROOT);
    strcat(dest, dexoptBin);

    struct stat statApkPath;


    stat(apkPath, &statApkPath);
    int fdApk = open(apkPath, 0, 0);
    if (fdApk < 0) {

        __android_log_print(6, "DexInv", "DexInv cannot open '%s' for input\n", apkPath);
        return;
    }

    if (access(dexPath, 0) != -1) {
        __android_log_print(3, "DexInv", "Skip DexInv");
        return;
    }


    int fdDex = open(dexPath, 66, 420);

    if (fdDex >= 0) {
        int lockVal = flock(fdDex, 6);

        if (lockVal != 0) {


            char *error = strerror(errno);

            __android_log_print(6, "DexInv", "flock(%s) failed: %s\n", dexPath, error);

            flock(fdDex, 8);
            close(fdDex);
            return;
        }

        __android_log_print(3, "DexInv", "DexInv: --- BEGIN '%s' --- flags='%s'\n", apkPath, args);
        /*
         * Parse arguments.  We want:
         *   0. (name of dexopt command -- ignored)
         *   1. "--zip"
         *   2. zip fd (input, read-only)
         *   3. cache fd (output, read-write, locked with flock)
         *   4. filename of file being optimized (used for debug messages and
         *      for comparing against BOOTCLASSPATH -- does not need to be
         *      accessible or even exist)
         *
         * The BOOTCLASSPATH environment variable is assumed to hold the correct
         * boot class path.  If the filename provided appears in the boot class
         * path, the path will be truncated just before that entry (so that, if
         * you were to dexopt "core.jar", your bootclasspath would be empty).
         *
         * This does not try to normalize the boot class path name, so the
         * filename test won't catch you if you get creative.
         */




        pid_t pid = fork();
        if (pid > -1) {

            char fdZipBuf[16];
            char fdDexBuf[16];
            sprintf(fdZipBuf, "%d", fdApk);//
            sprintf(fdDexBuf, "%d", fdDex);
            execl(dest, dest, "--zip", fdZipBuf, fdDexBuf, args);

            char *error = strerror(errno);
            __android_log_print(6, "DexInv", "execl(%s) failed: %s\n", dest, error);
            exit(67);

        }

        int status;
        pid_t pidWaited;
        while (1) {
            pidWaited = waitpid(pid, &status, 0);

            if (pidWaited != -1)
                break;
            //			v11 = _errno(-1);
            //			if ( *(_DWORD *)v11 != 4 )
            //				break;
            puts("waitpid interrupted, retrying");
        }


        if (pidWaited == pid) {
            if (!(status << 25) && !((unsigned int) (status << 16) >> 24)) {
                int dexPathLock = flock(fdDex, 8);
                if (!dexPathLock) {
                    __android_log_print(3, "DexInv", "DexInv: ---  dexPathLock (success) ---\n");
                    close(dexPathLock);
                    close(fdDex);

                }

                __android_log_print(3, "DexInv", "DexInv: --- END '%s' (success) ---status%d\n",
                                    apkPath, status);
                return;
            }
            __android_log_print(5, "DexInv",
                                "DexInv: --- END '%s' --- status=0x%04x, process failed\n", apkPath,
                                status);
            // v13 = apkPath1;
            struct utimbuf dexPathutimbuf;
            if (!status) {

                //	int utime(const char * filename,struct utimbuf * buf);
                //  apkPath0 = v38;
                utime(dexPath, &dexPathutimbuf);
                int dexPathLock = flock(fdDex, 8);
                if (!dexPathLock) {
                    __android_log_print(3, "DexInv", "DexInv: ---  dexPathLock (success) ---\n");
                    close(dexPathLock);
                    close(fdDex);
                    return;
                }

                __android_log_print(6, "DexInv", "unlock(%s) failed: %s\n", dexPath,
                                    strerror(errno));
                //  goto LABEL_28;
            }
        }
        else {

            __android_log_print(5, "DexInv", "waitpid failed: wanted %d, got %d: %s\n", pidWaited,
                                pid, strerror(errno));

        }
    }




}
