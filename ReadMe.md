1PalmSDK-PUser Guide
V1.3.8-2024/09/09
Directory
1 SDK Introduction......................................................................................................................51.1 Overview....................................................................................................................................... 51.2 Directory structure........................................................................................................................51.3 Applicable system.........................................................................................................................52 SDK Usage process ...................................................................................................................62.1 Configuration description.............................................................................................................62.1.1 gradle Configure................................................................................................................................................62.1.2 Androidmanifest.xml Configure...................................................................................................................... 62.1.3 Flow chart...........................................................................................................................................................73 SDK Interface description.........................................................................................................83.1 Class file description.....................................................................................................................83.2 PalmSdk......................................................................................................................................103.2.1 Initialize............................................................................................................................................................ 103.2.2 Get SDK version number................................................................................................................................ 103.3 Device......................................................................................................................................... 103.3.1 Create device................................................................................................................................................... 103.3.2 Close device..................................................................................................................................................... 103.3.3 Obtain the number of devices ....................................................................................................................... 113.4 Device DeviceListener.................................................................................................................123.5 DtUsbManager DeviceStateListener........................................................................................... 123.6 IDevice........................................................................................................................................ 123.6.1 Open device..................................................................................................................................................... 123.6.2 Obtain the stream types supported by the device......................................................................................13
2
3.6.3 Create Stream..................................................................................................................................................133.6.4 Destroy Stream................................................................................................................................................ 133.6.5 Close device..................................................................................................................................................... 133.6.6 Reboot device..................................................................................................................................................133.7 IOpenCallback............................................................................................................................ 153.8 IStream....................................................................................................................................... 153.8.1 Get stream type............................................................................................................................................... 153.8.2 Apply for frame list space............................................................................................................................... 153.8.3 Get frame list....................................................................................................................................................153.8.4 Start stream..................................................................................................................................................... 163.8.5 Stop stream......................................................................................................................................................163.9 IVeinshine................................................................................................................................... 163.9.1 Get device information...................................................................................................................................163.9.2 Set Psensor threshold.....................................................................................................................................163.9.3 Set Led mode...................................................................................................................................................173.9.4 Upgrade............................................................................................................................................................173.9.5 Start heartbeat.................................................................................................................................................173.9.6 Stop heartbeat.................................................................................................................................................183.9.7 Obtain camera module temperature........................................................................................................... 183.9.8 Enable dimpalm algorithm module............................................................................................................. 183.9.9 Read license.....................................................................................................................................................183.9.10 Write license...................................................................................................................................................193.9.11 Get algorithm version................................................................................................................................... 193.9.12 Capture once................................................................................................................................................. 193.9.13 Continuous capture......................................................................................................................................193.9.14 Stop capture.................................................................................................................................................. 213.9.15 Obtain recognition threshold...................................................................................................................... 213.9.16 Start saving image.........................................................................................................................................213.9.17 Stop saving images ....................................................................................................................................... 213.9.18 Registration Interface................................................................................................................................... 233.9.19 Extract feature from image.......................................................................................................................... 233.9.20 Compare feature........................................................................................................................................... 233.10 Frames ...................................................................................................................................... 253.10.1 Get the total number of frames................................................................................................................... 25
33.10.2 Get frames ......................................................................................................................................................253.11 Frame........................................................................................................................................253.11.1 Get stream type............................................................................................................................................. 253.11.2 Get frame type............................................................................................................................................... 253.11.3 Get frame width.............................................................................................................................................253.11.4 Get frame height............................................................................................................................................263.11.5 Get frame index ............................................................................................................................................. 263.11.6 Get frame size................................................................................................................................................ 263.11.7 Get frame timestamp....................................................................................................................................263.11.8 Obtain raw data.............................................................................................................................................263.11.9 Get extra frame information........................................................................................................................ 263.12 BBox class ................................................................................................................................. 273.13 DeviceInfo class ........................................................................................................................ 273.14 ICapturePalmCallback.............................................................................................................. 273.14.1 Capture palm callback................................................................................................................................. 273.14.2 No palm callback captured..........................................................................................................................273.15 IHeartbeatListener.................................................................................................................... 293.15.1 Heartbeat callback........................................................................................................................................293.16 IUpgradeListener...................................................................................................................... 293.16.1 Upgrade start callback................................................................................................................................. 293.16.2 Upgrade progress callback.......................................................................................................................... 293.16.3 Upgrade successful callback....................................................................................................................... 293.16.4 Upgrade failure callback.............................................................................................................................. 293.16.5 Upgrade timeout callback........................................................................................................................... 293.17 CameraTemperature................................................................................................................313.18 HeartbeatParam....................................................................................................................... 313.19 CaptureFrame...........................................................................................................................323.20 FrameType enumeration.......................................................................................................... 333.21 Hint........................................................................................................................................... 333.22 StreamType.............................................................................................................................. 36
4
3.23 EnumRecognitionType............................................................................................................. 363.24 PalmRegisterOutput.................................................................................................................373.25 ExtraFrameInfo.........................................................................................................................383.26 ImageInstance..........................................................................................................................383.27 ExtractOutput...........................................................................................................................384 Error codes and descriptions ................................................................................................. 394.1 Universal error code................................................................................................................... 395 Revision Record...................................................................................................................... 426 Disclaimer............................................................................................................................... 427 Technical support...................................................................................................................428 Precautions .............................................................................................................................43
51 SDK Introduction
1.1 Overview
The PalmSDK is a software development toolkit developed based on the Veinshinecamera, which is currently suitable for the Android platform and provides a series of friendlyAPIs and simple application example programs for application developers. Users canobtainhigh-precision color and grayscale images based on this development package, whichfacilitates the development of applications such as biometric recognition andartificial
intelligence perception. This document introduces the usage of PalmSDK (Android) ontheAndroid platform. Including usage process, interface explanation, and example programusage. 1.2 Directory structure
The following table shows the directory structure and content descriptionof PalmSDKDirectory Description
doc Description document directoryexample Source code directory for sampleprograms
example-apk APK file directory for demolibs Lib file directory
assets Facial AlgorithmModel File Directory1.3 Applicable system
Type Environment
arm64-8a Android 6.0 and above
armabi-v7a Android 6.0 and above
6
2 SDK Usage process
2.1 Configuration description
2.1.1 gradle Configure
Copy the file to the corresponding libs directory of the project, and then configureit inthe gradle. reference is as follows:
android{
sourceSets {
main {
jniLibs.srcDirs = ["libs"]
}
}
repositories {
flatDir {
dirs 'libs'
}
}
}
dependencies {
implementation files('palm-android-sdk-v1.3.7.jar')
}
2.1.2 Androidmanifest.xml Configure
Add permissions to the manifest file
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" /><uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" /><uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS"tools:ignore="ProtectedPermissions" />
<uses-permission android:name="android.hardware.usb.host" />
<uses-permission android:name="android.hardware.usb.accessory" />
<uses-feature android:name="android.hardware.camera" />
<uses-feature
android:name="android.hardware.usb.host"
android:required="true" />
72.1.3 Flow chart
chart 1
8
3 SDK Interface description
3.1 Class file description
Class name Content Description
PalmSdk Environmental management class
Device Equipment management class
Device DeviceListener Device creation callback interfaceclassDtUsbManager DeviceStateListener Device status callback interface classIDevice Device Interface Class
IVeinshine Specific device interface classes
IOpenCallback Device Open Callback Interface ClassIStream Stream Interface Class
Frames Frame List Class
Frame Frame data class
DeviceInfo Camera information object class
FrameMode Frame mode enumeration class
FrameType Frame type enumeration class
StreamType Stream type enumeration class
CaptureFrame Capture callback frame object class
Hint Capture prompts enumerationclass
CameraTemperature Camera temperature object
HeartbeatParam Heartbeat parameter object
EnumRecognitionType Identifying type enumeration classesICapturePalmCallback Capture callback interface class
IUpgradeListener Upgrade callback interface class
IHeartbeat Listener Heartbeat callback interface class
PalmRegisterOutput Register palm interface result output classExtraFrameInfo Information class when registeringinterfaceinput parameters
9ImageInstance Image entity class
ExtractOutput Extract features fromimages output class
10
3.2 PalmSdk
3.2.1 Initialize
static void initialize()
Initialization method, please initialize once when the app starts
Parameters:
3.2.2 Get SDK version number
static String getSdkVersion ()
Parameters:
Return SDK version number
3.3 Device
3.3.1 Create device
static void create(@NonNull Context context,@NonNull DeviceListener
deviceCreateListener,DtUsbManager.DeviceStateListener deviceStateListener)
Parameters:
[in] context Context of Activity
[in] deviceCreatListener
Device creation result callback object, see 3.4<DeviceListener>interface description[in] deviceStateListener
Device status callback object, see3.5<DeviceStateListener>interface description3.3.2 Close device
void close(IDevice device)
Parameters:
[in] device Device that need to be close
113.3.3 Obtain the number of devices
int getDeviceCount(Context context)
Parameters:
[in] context Context
Return the number of inserted devices
12
3.4 Device DeviceListener
Device creation callback interface
void on DeviceCreated Success（
IDevice device, Int deviceIndex, Map<Long, IDevice>runningDevice, UsbMapTable DeviceType
Device creation success callback interfacevoid on DeviceCreate Failed (IDevice device) Device creation failure callback interfacevoid on DeviceDestroy (IDevice device) Device destruction callback interface3.5 DtUsbManager DeviceStateListener
Device status callback interface
void onDevicePermissionGranted (UsbDevice
usbDevice)
Device USB Permission GrantedCallbackInterface
void onDevicePermissionDenied (UsbDevice
usbDevice)
Device USB permission Deniedcallbackinterface
void on Attached (UsbDevice usbDevice) Device Attached Callback Interfacevoid on Detached (UsbDevice usbDevice) Device Detached callback interface3.6 IDevice
3.6.1 Open device
int open (IOpenCallback openCallback)
Parameters:
[in] openCallback Open callback, see 3.7<IOpenCallback>
Return success: 0
failure: see error code
133.6.2 Obtain the stream types supported by the device
List<StreamType>getDeviceSupportStreamType()
Parameters:
Return success: StreamType supported by the device, see 3.22<StreamType>
failure: see error code
3.6.3 Create Stream
IStream createStream (StreamType streamType)
Parameters:
[in] streamType Stream type
Return success: Stream object, see 3.8<IStream>
failure: Empty object
3.6.4 Destroy Stream
void destroyStream (IStream stream)
Parameters:
[in] stream Stream Object
3.6.5 Close device
int close()
Parameters:
Return success: 0
failure: see error code
3.6.6 Reboot device
int reboot()
14
Parameters:
Return success: 0
failure: see error code
153.7 IOpenCallback
Open callback interface
void onDownloadPrepare() Download preparation callback interfacevoid onDownloadProgress (int progress) Download progress callback interfacevoid onDownloadSuccess() Download successful callback interfacevoid onOpenSuccess() Open successful callback interfacevoid onOpenFailure (int errorCode) Open failed callback interface
3.8 IStream
3.8.1 Get stream type
StreamType getStreamType()
Parameters:
Return success: Stream type, see 3.22<StreamType>
failure: Empty object
3.8.2 Apply for frame list space
Frames allocateFrames()
Parameters:
Return success: Frame object, see 3.10<Frames>
failure: Empty object
3.8.3 Get frame list
int getFrames (Frames frames, int timeout)
Parameters:
[in] frames Get frames
[in] timeout Timeout time
16
Return success: 0
failure: see error code
3.8.4 Start stream
int start()
Parameters:
Return success: 0
failure: see error code
3.8.5 Stop stream
void stop()
Parameters:
3.9 IVeinshine
3.9.1 Get device information
DeviceInfo getDeviceInfo()
Parameters:
Return success: DeviceInfo object, see 3.13<DeviceInfo>
Failure: Empty object
3.9.2 Set Psensor threshold
int setPsensorDistanceThreshold (int nearThreshold, int farThreshold)
Parameters:
[in] nearThreshold Close range threshold
[in] farThreshold Long distance threshold
Return success: 0
failure: see error code
173.9.3 Set Led mode
int setLedMode (int mode)
Parameters:
[in] mode Light mode
Return success: 0
failure: see error code
3.9.4 Upgrade
int upgrade(@NonNull String upgradeFilePath, @NonNull IUpgradeListener listener)
Parameters:
[in] upgradeFilePath Upgrade file path
[in] listener
Upgrade callback listening, see 3.16<IUpgradeListener>
Return success: 0
failure: see error code
3.9.5 Start heartbeat
int startHeartbeat(@NonNull HeartbeatParam heartbeatParam, @NonNull
IHeartbeatListener listener)
Parameters:
[in] heartbeatParam Heartbeat parameters, see 3.18<HeartbeatParam>[in] listener
Heartbeat callback monitoring, see 3.15<IHeartbeatListener>
Return success: 0
failure: see error code
18
3.9.6 Stop heartbeat
int stopHeartbeat()
Parameters:
Return success: 0
failure: see error code
3.9.7 Obtain camera module temperature
int getCameraTemperature(@NonNull CameraTemperature temperature)
Parameters:
[in] temperature
Camera temperature object, see 3.17<CameraTemperature>
Return success: 0
failure: see error code
3.9.8 Enable dimpalm algorithm module
int enableDimPalm(String modelPath)
Parameters:
[in] modelPath Model path
Return success: 0
failure: see error code
3.9.9 Read license
String readLicense()
Parameters:
Return success:String license
failure:Empty String
193.9.10Write license
int writeLicense(String licenseContext)
Parameters:
[in] licenseContext License context
Return success:0
failure:see error code
3.9.11Get algorithm version
String getAlgorithmVersion()
Parameters:
Return success:Algorithm version
failure:Empty String
3.9.12Capture once
int capturePalmOnce(@NonNull ICapturePalmCallback capturePalmCallback, int
timeOut)
Parameters:
[in] capturePalmCallback
Capture callback monitoring, see 3.14<ICapturePalmCallback>
[in] timeOut Capture timeout
Return success: 0
failure: see error code
3.9.13Continuous capture
int capturePalm(@NonNull ICapturePalmCallback capturePalmCallback, int timeOut)
Parameters:
20
[in] capturePalmCallback
Capture callback monitoring, see 3.14<ICapturePalmCallback>
[in] timeOut Capture timeout
Return success: 0
failure: see error code
213.9.14Stop capture
int stopPalmCapture()
Parameters:
Return success: 0
failure: see error code
3.9.15Obtain recognition threshold
float getRecognitionThreshold(@NonNull EnumRecognitionType type)
Parameters:
[in] type
Identification type, see 3.23<EnumRecognitionType>
Return success: 0
failure: see error code
3.9.16Start saving image
int startSavePicture(@NonNull String picPath, int saveCount)
Parameters:
[in] picPath Save image path
[in] saveCount Number of stored images
Return success: 0
failure: see error code
3.9.17Stop saving images
int stopSavePicture()
Parameters:
Return success: 0
22
failure: see error code
233.9.18Registration Interface
PalmRegisterOutput registerPalm(ImageInstance rgbImg, @NonNull ImageInstanceirImg, ExtraFrameInfo info)
Parameters:
[in] rgbImg RGB image ,see 3.26<ImageInstance>
[in] irImg IR image ,see 3.26<ImageInstance>
[in] info Extra frame information, see 3.28<ExtractOutput>Return success: PalmRegisterOutput.result==0
failure: Empty object or result != 0
3.9.19Extract feature from image
ExtractOutput extractPalmFeaturesFromImg(ImageInstance rgbImg, ImageInstanceirImg)Parameters:
[in] rgbImg RGB image ,see 3.26<ImageInstance>
[in] irImg IR image ,see 3.26<ImageInstance>
Return success:ExtractOutput.result==0，see 3.28<ExtractOutput>
failure:Empty object or result != 0
3.9.20Compare feature
CompareOutput compareFeatureScore(byte[] rgbFeatureSrc, byte[] irFeatureSrc, byte[]
rgbFeatureDest, byte[] irFeatureDest)
参数：
[in] rgbFeatureSrc RGB Src feature
[in] irFeatureSrc IR Src feature
[in] rgbFeatureDest RGB Dest feature
[in] irFeatureDest IR Dest feature
24
Return success:CompareOutput.result==0, failure:Empty object or result != 0
253.10Frames
3.10.1Get the total number of frames
int getFrameCount()
Return the number of frames
3.10.2Get frames
Frame getFrame (int frameIndex)
Parameters:
[in] frameIndex Frame index
Return success: Frame object, see 3.11<Frame>
failure: null
3.11Frame
3.11.1Get stream type
StreamType getStreamType()
Return the StreamType of the frame, see 3.22<StreamType>
3.11.2Get frame type
FrameType getFrameType()
Return the FrameType of the frame, see 3.20<FrameType>
3.11.3Get frame width
int getWidth()
Return int type frame width
26
3.11.4Get frame height
int getHeight()
Return frame height of type int
3.11.5Get frame index
int getIndex()
Return int frame Index
3.11.6Get frame size
int getSize()
Return int frame size
3.11.7Get frame timestamp
long getTimestamp()
Return long type timestamp
3.11.8Obtain raw data
byte[] getRawData()
Return byte[]
3.11.9Get extra frame information
ExtraFrameInfo getExtraInfo()
Return ExtraFrameInfo, see 3.25<ExtraFrameInfo>
273.12 BBox class
BBox class attribute description
int x Top left vertex coordinates
int y Top left vertex coordinates
int w width
int h height
3.13 DeviceInfo class
Description of camera version information attributes
String devicename Device Name
String serialnum SN number
String palm_sdk_version Palm SDK version number
String firmware_version Firmware Version
int pid PID
int vid Vid
3.14 ICapturePalmCallback
3.14.1Capture palm callback
void onCaptureFrame (CaptureFrame frame)
Parameters:
[in] frame
Capture successfully callback data frames, see3.19<CaptureFrame>
3.14.2No palm callback captured
void onCapturePalmHint (Hint hint)
28
Parameters:
[in] hint Callback prompt for failed capture, see 3.21<Hint>
293.15 IHeartbeatListener
3.15.1Heartbeat callback
void onHeartbeatResult (boolean result)
Parameters:
[in] result Heartbeat callback
3.16 IUpgradeListener
3.16.1Upgrade start callback
void onUpgradeStart()
3.16.2Upgrade progress callback
void onUpgradeProgress (int progress)
Parameters:
[in] progress Upgrade progress callback
3.16.3Upgrade successful callback
void onUpgradeSuccess()
3.16.4Upgrade failure callback
void onUpgradeFailure (String msg)
Parameters:
[in] msg Failed callback information
3.16.5Upgrade timeout callback
void onUpgradeTimeout()
30
313.17 CameraTemperature
Description of camera temperature attributes
float temperatureMainBoard Main board temperature
float temperatureLedBoard Lamp panel temperature
float temperatureCpu CPU temperature
float temperatureRgbSensor RGB Sensor temperature
3.18 HeartbeatParam
Description of heartbeat parameter properties
int heartbeatInterval heartbeat interval
int FailTimes Number of failed retries
32
3.19CaptureFrame
Description of callback properties for successful capture results
int rgbCols Number of RGB image columns
int rgbRows RGB image row count
byte[] rgbData RGB image data (8UC3)
int irCols Number of IR image columns
int irRows IR Image Rows
byte[] irData IR image data (8UC1)
int palmRectX The x-coordinate of the upper left vertexofthe palm frame
int palmRectY The y-coordinate of the upper left vertexofthe palm frame
int palmRectW The width of the palmframe
int palmRectH The height of the palmframe
int palmCenterRectX The x-coordinate of the upper left vertexofthe palm center box
int palmCenterRectY The y-coordinate of the upper left vertexofthe palm center box
int palmCenterRectW The width of the palmcenter frameint palmCenterRectH The height of the palmcenter framebyte[] rgbFeature Palm RGB feature
byte[] irFeature Palmar IR feature
byte skeleton Palm Key Points Group
float score Palm score
int palmType Palm type (0: Left hand 1: Right hand)
333.20 FrameType enumeration
Description of FrameType enumeration values
INVALID_FRAME_TYPE Illegal frame type
RGB_FRAME RGB frame type
IR_FRAME IR frame type
3.21 Hint
Capture callback prompt enumeration class
int key Code for indicating prompt languageString value Description of prompt languageString chineseDescription Description of prompt Chinese languageEnum Key description chineseDescriptionNO_PALM_DETECTED 0 No palm detected 未检测到手掌BIG_POSE 1 Too big pose 角度过大QUALITY_ERROR 2 Please face your
palm towards the
camera
请将掌心面向镜头REGISTER_QUALITY_ERROR 3 Please open and
straighten your
palms
请张开并摆正手掌LIVENESS_ERROR 4 Please face your
palm towards the
camera
请将掌心面向镜头IR_DARKNESS 6 Please palm slightly
closer
手掌稍微靠近点IR_OVER_EXPOSE 7 Please keep your
palms slightly away
手掌稍微远离点
34
RGB_DARKNESS 8 Please palm slightly
closer
手掌稍微靠近点RGB_OVER_EXPOSE 9 Please keep your
palms slightly away
手掌稍微远离点REGISTER_IR_DARKNESS 10 Please palm slightly
closer
手掌稍微靠近点REGISTER_IR_OVER_EXPOSE 11 Please keep your
palms slightly away
手掌稍微远离点REGISTER_RGB_DARKNESS 12 Please palm slightly
closer
手掌稍微靠近点REGISTER_RGB_OVER_EXPOSE 13 Please keep your
palms slightly away
手掌稍微远离点LIVENESS_COLOR_GRAY_ERROR 14 Please face your
palm towards the
camera
请将掌心面向镜头RELIABILITY_IR_ERROR 15 Please ensure that
your palms are clear
and free from any
abnormalities
请确保手掌清晰无异常RELIABILITY_RGB_ERROR 16 Please ensure that
your palms are clear
and free from any
abnormalities
请确保手掌清晰无异常REGISTER_RELIABILITY_IR_ERROR 17 Please ensure that
your palms are clear
and free from any
abnormalities
请确保手掌清晰无异常REGISTER_RELIABILITY_RGB_ERROR 18 Please ensure that
your palms are clear
请确保手掌清晰无异常
35and free from any
abnormalities
UNEXPECTED_CENTER_BOX_POS 23 Please place your
palm in the center
of the screen
手掌请位于画面中心PALM_IS_MOVING 25 Please keep your
palm still
手掌请保持静止MANUALSTOP 0x4000 Manually stopped 手动停止TIMEOUT 0x8000 Capture timeout 抓拍超时INITIALIZING 0x8888 Initializing 初始化中
36
3.22 StreamType
StreamType enumeration value description
INVALID_STREAM_TYPE Illegal stream type
RGB RGB stream type
IR IR stream type
RGB_IR RGB and IR synchronous streamtypes3.23 EnumRecognitionType
Description of the enumeration values for the EnumRecognitionType
RGB RGB recognition type
IR IR recognition type
373.24 PalmRegisterOutput
Palm registration interface result output class
int result Result
float score Palm score
byte[] rgbFeature Palm RGB feature
byte[] irFeature Palm IR feature
byte[] skeleton Palm Key Points Group
int palmType Palm type (0: Left hand 1: Right hand)
int palmRectX The x-coordinate of the upper left vertexofthe palm frame
int palmRectY The y-coordinate of the upper left vertexofthe palm frame
int palmRectW The width of the palmframe
int palmRectH The height of the palmframe
int palmCenterRectX The x-coordinate of the upper left vertexofthe palm center box
int palmCenterRectY The y-coordinate of the upper left vertexofthe palm center box
int palmCenterRectW The width of the palmcenter frameint palmCenterRectH The height of the palmcenter frame
38
3.25 ExtraFrameInfo
Extra frame Information class
int[] pSensorValue Psensor value
int[] palmRoi Palm ROI
int lightMode Light mode
3.26 ImageInstance
ImageInstance class
int width The width of the image
int height The Height of the image
byte[] imgData The data of the image
ImageFormat format The format of the image(IMG_1C8BIT:8UC1IMG_3C8BIT:8UC3)
3.27 ExtractOutput
Extract feature from image result output class
int result Result
float score Palm score
byte[] rgbFeature Palm RGB feature
byte[] irFeature Palm IR feature
byte[] skeleton Palm Key Points Group
int palmType Palm type (0: Left hand 1: Right hand)
Shiyun Confidential
394 Error codes and descriptions
4.1 Universal error code
Error (Hexadecimal) Description
0x1 Unknown error
0x2 Unrealized
0x3 Invalid parameter
0x4 Not currently supported
0x5 Memory application failed
0x6 Invalid picture type
0x20010 Transfer failed
0x20012 The configuration file does not exist
0x21001 Unable to find device
0x21002 Null pointer
0x21003 Open failed
0x21004 Closing failed
0x21005 Open flow failure
0x21006 Failed to set/retrieve data
0x21007 Failed to detect data
0x21008 Failed to open IR camera
0x21009 Failed to open RGB camera
0x2100A Failed to obtain USB serial number
0x2100B The device is not running
0x2100C Device not turned on
0x2100D Driver error
0x2100E Camera not configured
0x2100F Stop stream fail
0x22001 Data size error
Shiyun Confidential
40
0x22002 Data not prepared
0x22004 Unsupported camera mode
0x22010 timeout
0x22011 Scan mode not set
0x22100 file does not exist
0x22101 Operation file failed
0x22102 Matching RGB data failed
0x22103 The upgraded version number has not changed
0x22104 The device is currently being upgraded
0x22105
Upgrade fail
0x22106 Set expose fail
0x22107 Get expose fail
0x23001 Failed to initialize facial algorithm
0x23002 Failed to initialize deep algorithm
0x23003 Invalid calibration size
0x23004 Failed to read flash
0x23005 Failed to obtain calibration
0x23006 Path error
0x23007 An error occurred
0x23008 Failed to obtain license
0x23009 Failed to init palm algorithm
0x24001 Camera component not found
0x24002 Failed to obtain RGB frames
0x24003 Failed to obtain IR frame
0x24004 Capturing in progress
0x24005 Preview open failed
0x24006 Preview read failed
0x25001 Algorithm not initialized
0x25002 Stream not start
0x25003 Device has opened
Shiyun Confidential
410x25004 Device not initialized
0x25005 Device is not in capture mode
0x25006 Invalid frame format
0x42019 SDK not configured successfully
0x42021 Unactivated algorithm
0x42023 Unopened flow
0x42024 Capture not enabled
0x80010 Operator creation failed
0x80011 Operator configuration file does not exist
0x80012 Model inference engine creation failed
0x80013 Algorithm pipeline creation failed
0x80014 The algorithm pipeline configuration file does not exist
0x80015 The pre - and post-processing module of the model doesnotexist
0x80016 Invalid running parameters
0x80017 Invalid configuration parameters
0x80020 No palm frame detected
0x80021 Failed to obtain the loop graph
0x80022 Failed to obtain norm graph
0x80024 Model inference failed
0x80030 Unexpected error occurred
0x80032 Null pointer occurred
0x80033 Light status error
Shiyun Confidential
42
5 Revision Record
Version Description Date Author
V1.0.1 SDK 1.0.1 version description document 2024/03/18
V1.0.2 SDK 1.0.2 version description document 2024/03/22
V1.1.0 SDK 1.1.0 version description document 2024/03/26
V1.1.1 SDK 1.1.1 version description document 2024/03/28
V1.1.2 SDK 1.1.2 version description document 2024/04/03
V1.1.5 SDK 1.1.5 version description document 2024/04/16
V1.1.6 SDK 1.1.6 version description document 2024/04/23
V1.2.0 SDK 1.2.0 version description document 2024/05/10
V1.2.2 SDK 1.2.2 version description document 2024/05/29
V1.3.0 SDK 1.3.0 version description document 2024/07/11
V1.3.3 SDK 1.3.3 version description document 2024/07/25
V1.3.7 SDK 1.3.7 version description document 2024/08/29
V1.3.8 SDK 1.3.8 version description document 2024/09/09
6 Disclaimer
The device application information and other similar content described inthis
publication are for your convenience only and may be replaced by updated information. Itisyour own responsibility to ensure that the application complies with technical specifications. 7 Technical support
You can obtain support through the following channels
 FAE support: Please contact our sales personnel for FAE support
Shiyun Confidential
438 Precautions
 Do not use other heat sources to heat this product.  Do not drop or impact this product to prevent damage to internal componentsandadecrease in accuracy; Improper operation may cause damage to internal
components.  Do not attempt to modify or disassemble this machine in any way to avoidmoduledamage and decreased accuracy.  After using the module for a period of time, it will generate heat, which is anormal
phenomenon. Heat dissipation treatment can be performed on the back of themodule.