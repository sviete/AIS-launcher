if [ -f /sdcard/Generic.kl ]; then
    mount -o rw,remount,rw /system
    echo "replace keylayout" >> "/sdcard/dom_launcher.log"
    # replace key-to-output mapping
    #[remote controller] => [kl] => [kcm] => [application]
    cp /sdcard/Generic.kl /system/usr/keylayout/Generic.kl
    rm /sdcard/Generic.kl
    mount -o ro,remount,ro /system
fi

if [ -f /sdcard/bootanimation.zip ]; then
    mount -o rw,remount,rw /system
    echo "changing bootanimation     " >> "/sdcard/dom_launcher.log"
    mv /system/media/bootanimation.zip /system/media/bootanimation.zip.back
    mv /sdcard/bootanimation.zip /system/media/bootanimation.zip
    chmod 644 /system/media/bootanimation.zip
    chown root:root /system/media/bootanimation.zip
    mount -o ro,remount,ro /system
fi

#if [ -f /sdcard/pl-pl_2.zip ]; then
#    cd /sdcard
#    unzip pl-pl_2.zip -d /data/data/com.google.android.tts/app_voices
#    echo "unzip /data/data/com.google.android.tts/app_voices" >> "/sdcard/dom_launcher.log"
#fi

