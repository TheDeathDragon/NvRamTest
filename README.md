## How to build

step 1 : build aosp project

step 2 : 

copy `out/target/common/obj/JAVA_LIBRARIES/framework_intermediates/classes-header.jar` to  `project/app/libs/framework.jar`

copy `out/target/common/obj/JAVA_LIBRARIES/vendor.mediatek.hardware.nvram-V1.0-java_intermediates/classes.jar `  to  `project/app/libs/nvram.jar`

step 3 : 

generate keystore

AOSP：

`build\target\product\security`

MTK：

`device\mediatek\security`

```bash
openssl pkcs8 -inform DER -nocrypt -in platform.pk8 -out platform.pem

openssl pkcs12 -export -in platform.x509.pem -inkey platform.pem -out platform.p8 -password pass:android -name android

keytool -importkeystore -deststorepass android -destkeystore .keystore -srckeystore platform.p8 -srcstoretype PKCS12 -srcstorepass android

keytool -list -v -keystore .keystore

mv .keystore platform.keystore
```

copy `platform.keystore` to `project/app/`

### How to install

Make sure you have unlock the device and build type is userdebug

```bash
adb root
adb shell remount
adb shell mkdir /system/priv-app/NvRamTest/
adb push .\NvRamTest.apk /system/priv-app/NvRamTest/
adb reboot
```

 