SDK = /opt/android-sdk
ANDROID_API = $(SDK)/platforms/android-8/android.jar
KEYSTORE = darmovzal.keystore
KEYNAME = darmovzal

all: build install start

build:
	ant clean debug

install:
	ant installd

start:
	adb shell am start -n cz.darmovzal.yoca/.StartActivity

release:
	test -f $(KEYSTORE) || (echo '*** Cannot find Google-Play keystore file: $(KEYSTORE) ***'; false)
	ant clean release
	cp bin/StartActivity-release-unsigned.apk YOCA.apk
	jarsigner -verbose -sigalg MD5withRSA -digestalg SHA1 -keystore $(KEYSTORE) YOCA.apk $(KEYNAME)
	$(SDK)/build-tools/*/zipalign -v 4 YOCA.apk YOCA_aligned.apk
	mv YOCA_aligned.apk YOCA.apk

update:
	$(SDK)/tools/android update project -t android-8 -p .

