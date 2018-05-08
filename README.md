# Set up single-purpose devices  
  ![alt text](https://github.com/kosmologist/kiosksample/raw/master/screenshots/preview_fix.png)  
  
  You can configure Android 6.0 Marshmallow and later devices as corporate-owned, single-use (COSU) devices. These are Android devices used for a single purpose, such as digital signage, ticket printing, point of sale, or inventory management. To use Android devices as COSU devices, you need to develop Android apps that your customers can manage.  
  
  See more here: https://developer.android.com/work/cosu
  
  ## Running Sample
  
  To test sample app, you must provision device by any of the methods mentioned in above link. For the sake of this demo, you can provision this sample as device owner using the following adb command:
  
  ```
  adb shell dpm set-device-owner io.github.kosmologist.kioskappsample/.DeviceAdministratorReceiver
  ```
  
