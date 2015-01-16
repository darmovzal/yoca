ANDROID_API = /opt/android-sdk-linux/platforms/android-8/android.jar
KEYSTORE = darmovzal.keystore
KEYNAME = darmovzal

all: build install start

build:
	ant clean debug

install:
	ant installd

start:
	adb shell am start -n cz.darmovzal.yoca.full/.StartActivity

release:
	test -f $(KEYSTORE) || (echo '*** Cannot find keystore file: $(KEYSTORE) ***'; false)
	ant clean release
	cp bin/YOCA-release-unsigned.apk YOCA.apk
	jarsigner -verbose -sigalg MD5withRSA -digestalg SHA1 -keystore $(KEYSTORE) YOCA.apk $(KEYNAME)
	zipalign -v 4 YOCA.apk YOCA_aligned.apk
	mv YOCA_aligned.apk YOCA.apk

