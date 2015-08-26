APP_ABI := armeabi armeabi-v7a arm64-v8a
APP_PLATFORM := android-21

APP_CFLAGS := -DHAVE_CONFIG_H -DTHREADMODEL=POSIXTHREADS -DDEBUGLVL=0 -D__ANDROID__
APP_CFLAGS += -O3

APP_MODULES := jpeg libdjvu mupdf ebookdroid
