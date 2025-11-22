package com.palm.demo.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.api.stream.Device;
import com.api.stream.Frame;
import com.api.stream.Frames;
import com.api.stream.ICapturePalmCallback;
import com.api.stream.IDevice;
import com.api.stream.IOpenCallback;
import com.api.stream.IStream;
import com.api.stream.StreamType;
import com.api.stream.bean.BBox;
import com.api.stream.bean.CaptureFrame;
import com.api.stream.bean.CompareOutput;
import com.api.stream.bean.DeviceInfo;
import com.api.stream.bean.ExtraFrameInfo;
import com.api.stream.bean.ExtractOutput;
import com.api.stream.bean.ImageInstance;
import com.api.stream.enumclass.Hint;
import com.api.stream.manager.DtUsbDevice;
import com.api.stream.manager.DtUsbManager;
import com.api.stream.manager.UsbMapTable;
import com.api.stream.veinshine.IVeinshine;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.palm.common.opengl.GLDisplay;
import com.palm.common.opengl.GLFrameSurface;
import com.palm.demo.R;
import com.palm.demo.custom.DtRectRoiView;
import com.palm.demo.util.BitmapUtils;
import com.palm.demo.util.DialogUtils;
import com.palm.demo.util.FileUtils;
import com.palm.demo.util.IOUtils;
import com.palm.demo.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author BiCheng
 * @date 2024/3/15  17:56
 * @description:
 **/
public class ExampleActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();

    private GLFrameSurface mGLIrView, mGLRgbView;

    GLDisplay rgbDisPlay, irDisPlay;

    private Button mBtnOpen, mBtnEnable, mBtnCapture, mBtnCaptureOnceSrc, mBtnCaptureOnceDest, mBtnStopCapture, mBtnCompare;
    private TextView mTvDeviceInfo;
    private Switch mSwitchStartStream;
    private Spinner mSpinnerStreamMode;
    private ExecutorService deviceThread1 = Executors.newSingleThreadExecutor();
    private ExecutorService workServices = Executors.newSingleThreadExecutor();
    private volatile IDevice mDevice = null;
    private Handler mainHandler;
    private ArrayAdapter<StreamType> mAdapterStreamType;
    private final List<StreamType> mListStreamType = new ArrayList<>();
    private StreamType currentStreamType = StreamType.INVALID_STREAM_TYPE;
    private volatile boolean mIsRunning;
    private volatile boolean mIsOpenCamera;

    private byte[] rgbFrameData1 = null;
    private byte[] irFrameData1 = null;
    private ExtraFrameInfo irFrameExtraInfo = null;
    private ExtraFrameInfo rgbFrameExtraInfo = null;
    private int irFrameW1;
    private int irFrameH1;
    private int rgbFrameW1;
    private int rgbFrameH1;

    public DtRectRoiView mRectRoiRgbView;
    public DtRectRoiView mRectRoiIrView;

    protected Bitmap mRgbBitmap, mIrBitmap, mDepthBitmap;
    private ImageView rgbImage, irImage, depthImage;
    private volatile EnableAlgorithmStatus algoStatus = EnableAlgorithmStatus.DISABLE;

    public static enum EnableAlgorithmStatus {
        DISABLE, ENABLE, INITIALIZING;
    }

    private static String dir = Environment.getExternalStorageDirectory() + File.separator + "HeyStar";
    private ExecutorService callbackServices = Executors.newSingleThreadExecutor();

    private static final String CAPTURE_SRC_RGB_PATH = dir + "/rgbSrc.png";
    private static final String CAPTURE_SRC_IR_PATH = dir + "/irSrc.png";
    private static final String CAPTURE_DEST_RGB_PATH = dir + "/rgbDest.png";
    private static final String CAPTURE_DEST_IR_PATH = dir + "/irDest.png";
    private String mCaptureRgbPath;
    private String mCaptureIrPath;

    private Dialog progressDialog;

    private static final String[] REQUEST_PERMISSION = new String[]{
            Permission.CAMERA,
            Permission.WRITE_EXTERNAL_STORAGE,
            Permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example);
        checkPermission();
        initView();
        rgbDisPlay = new GLDisplay();
        irDisPlay = new GLDisplay();
        mainHandler = new Handler();
    }

    private void checkPermission() {
        XXPermissions.with(this)
                // 申请单个权限
//                .permission(Permission.RECORD_AUDIO)
                // 申请多个权限
//                .permission(Permission.Group.CALENDAR)
                // 申请多个权限
                .permission(REQUEST_PERMISSION)
                // 设置权限请求拦截器（局部设置）
                //.interceptor(new PermissionInterceptor())
                // 设置不触发错误检测机制（局部设置）
                //.unchecked()
                .request(new OnPermissionCallback() {

                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                        if (!allGranted) {
                            runOnUiThread(() -> {
                                Toast.makeText(ExampleActivity.this, "Some permissions are not granted", Toast.LENGTH_SHORT).show();
                            });
                            finish();
                            return;
                        }

                        IOUtils.createFolder(dir);
                        IOUtils.createFolder(dir + File.separator + "models");
                        copyAssetsFile();
                    }

                    @Override
                    public void onDenied(@NonNull List<String> permissions, boolean doNotAskAgain) {
                        if (doNotAskAgain) {
                            Toast.makeText(ExampleActivity.this,
                                    "Permanently denied authorization, pls grant permission manually",
                                    Toast.LENGTH_SHORT).show();
                            // 如果是被永久拒绝就跳转到应用权限系统设置页面
                            XXPermissions.startPermissionActivity(ExampleActivity.this, permissions);
                        } else {
                            Toast.makeText(ExampleActivity.this, "Get permission failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void initView() {
        mGLRgbView = findViewById(R.id.gl_rgb);
        mGLIrView = findViewById(R.id.gl_ir);
        mGLIrView.post(() -> {
            mGLRgbView.setDisplay(mGLRgbView.getWidth(), mGLRgbView.getWidth() * 1024 / 720);
            mGLIrView.setDisplay(mGLIrView.getWidth(), mGLIrView.getWidth() * 1024 / 720);
        });

        mAdapterStreamType = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mListStreamType);

        mBtnOpen = findViewById(R.id.btn_open);
        mBtnEnable = findViewById(R.id.btn_enable);
        mBtnCapture = findViewById(R.id.btn_capture);
        mBtnCaptureOnceSrc = findViewById(R.id.btn_capture_once_src);
        mBtnCaptureOnceDest = findViewById(R.id.btn_capture_once_dest);
        mBtnStopCapture = findViewById(R.id.btn_stop_capture);
        mBtnCompare = findViewById(R.id.btn_compare);
        mTvDeviceInfo = findViewById(R.id.tv_device_info);
        mSwitchStartStream = findViewById(R.id.switch_stream);
        mSpinnerStreamMode = findViewById(R.id.spinner_stream_mode);
        mSpinnerStreamMode.setAdapter(mAdapterStreamType);
        rgbImage = findViewById(R.id.rgb_image);
        irImage = findViewById(R.id.ir_image);
        depthImage = findViewById(R.id.depth_image);
        mRectRoiRgbView = findViewById(R.id.rv_rectRgbPicView);
        mRectRoiRgbView.resizeSource(720, 1024);
        mRectRoiIrView = findViewById(R.id.rv_rectIrPicView);
        mRectRoiIrView.resizeSource(720, 1024);

        initListener();
    }

    private void copyAssetsFile() {
        String model = dir + File.separator + "models/palm_models_1.3.5.bin";
        if (FileUtils.isFileExists(model)) {
            return;
        }
        showProgressDialog("Copying resources...");
        callbackServices.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ResourceUtils.copyFileFromAssets("models", dir + File.separator + "models");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ExampleActivity.this, R.string.activity_example_model_copy_success, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                runOnUiThread(() -> dismissProgressDialog());
            }
        });
    }

    private void initListener() {
        mBtnOpen.setOnClickListener(view -> openDevice());
        mSwitchStartStream.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!mIsOpenCamera && isChecked) {
                showToast(getString(R.string.activity_example_camera_not_turn_on));
                mSwitchStartStream.setChecked(false);
                return;
            }
            if (mSpinnerStreamMode != null) {
                mSpinnerStreamMode.setEnabled(!isChecked);
            }
            if (!isChecked && mIsRunning) {
                mIsRunning = false;
                mainHandler.postDelayed(this::clearFrame, 200);
                return;
            }
            if (mIsOpenCamera && isChecked) {
                startStream();
            }

        });
        mSpinnerStreamMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentStreamType = mListStreamType.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mBtnEnable.setOnClickListener(view -> enableDimPalm());
        mBtnCaptureOnceSrc.setOnClickListener(view -> captureOnceSrc());
        mBtnCaptureOnceDest.setOnClickListener(view -> captureOnceDest());
        mBtnCapture.setOnClickListener(view -> capture());
        mBtnStopCapture.setOnClickListener(view -> stopCapture());
        mTvDeviceInfo.setOnLongClickListener(view -> writeLicense());
        mBtnCompare.setOnClickListener(view -> showCompareDialog());
    }

    private TextView tvResult;

    private void showCompareDialog() {
        if (!mIsOpenCamera) {
            showToast(getString(R.string.activity_example_camera_not_turn_on));
            return;
        }
        if (algoStatus == EnableAlgorithmStatus.DISABLE) {
            showToast(getString(R.string.activity_example_algo_not_init));
            return;
        }
        // 创建 AlertDialog.Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View customView = LayoutInflater.from(this).inflate(R.layout.dialog_compare_feature, null);
        tvResult = customView.findViewById(R.id.tv_result);
        builder.setView(customView);
        builder.setPositiveButton(R.string.activity_example_btn_compare, (dialog, which) -> {
        });
        builder.setNegativeButton(R.string.activity_example_btn_cancel, (dialog, which) -> {
        });

        // 创建并显示 AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(dialog ->
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    // 处理确定按钮的点击事件
                    tvResult.setText("");
                    int pid = 0;
                    if (mDevice != null) {
                        pid = ((IVeinshine) mDevice).getDeviceInfo().pid;
                    }
                    EditText edRgbSrc = customView.findViewById(R.id.ed_rgb_src);
                    EditText edRgbDest = customView.findViewById(R.id.ed_rgb_dest);
                    EditText edIrSrc = customView.findViewById(R.id.ed_ir_src);
                    EditText edIrDest = customView.findViewById(R.id.ed_ir_dest);
                    String pathRgbSrc = null;
                    String pathRgbDest = null;
                    if (pid != 0x2009) {
                        pathRgbSrc = edRgbSrc.getText().toString().trim();
                        pathRgbDest = edRgbDest.getText().toString().trim();
                        if (TextUtils.isEmpty(pathRgbSrc) || TextUtils.isEmpty(pathRgbDest)) {
                            Toast.makeText(ExampleActivity.this,
                                    getString(R.string.activity_example_image_path_is_empty), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        File fileRgbSrc = new File(pathRgbSrc);
                        File fileRgbDest = new File(pathRgbDest);
                        if (!fileRgbSrc.exists()) {
                            Toast.makeText(ExampleActivity.this,
                                    getString(R.string.activity_example_no_image), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (!fileRgbDest.exists()) {
                            Toast.makeText(ExampleActivity.this,
                                    getString(R.string.activity_example_no_image), Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    String pathIrSrc = edIrSrc.getText().toString().trim();
                    String pathIrDest = edIrDest.getText().toString().trim();
                    if (TextUtils.isEmpty(pathIrSrc) || TextUtils.isEmpty(pathIrDest)) {
                        Toast.makeText(ExampleActivity.this,
                                getString(R.string.activity_example_image_path_is_empty), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    File fileIrSrc = new File(pathIrSrc);
                    File fileIrDest = new File(pathIrDest);
                    if (!fileIrSrc.exists()) {
                        Toast.makeText(ExampleActivity.this,
                                getString(R.string.activity_example_no_image), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!fileIrDest.exists()) {
                        Toast.makeText(ExampleActivity.this,
                                getString(R.string.activity_example_no_image), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    doCompareFeature(pathRgbSrc, pathRgbDest, pathIrSrc, pathIrDest);
                }));
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    private void doCompareFeature(String pathRgbSrc, String pathRgbDest, String pathIrSrc, String pathIrDest) {
        // 子线程处理,这里演示用图片提取的特征值做比对
        new Thread(() -> {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;

            Bitmap bitmap;
            ImageInstance rgbSrcImageInstance = null;
            ImageInstance rgbDestImageInstance = null;
            if (pathRgbSrc != null) {
                bitmap = BitmapFactory.decodeFile(pathRgbSrc, options);
                int rgbSrcImageWidth = options.outWidth;
                int rgbSrcImageHeight = options.outHeight;
//    saveBitmap(bitmap, "/sdcard/palm/java_rgbSrc_bitmap.jpg")
                byte[] rgbDataSrc = convertJpegDataToRgb888(bitmap, rgbSrcImageWidth, rgbSrcImageHeight);
                rgbSrcImageInstance =
                        new ImageInstance(rgbSrcImageWidth, rgbSrcImageHeight, rgbDataSrc, ImageInstance.ImageFormat.IMG_3C8BIT);
            }
            bitmap = BitmapFactory.decodeFile(pathIrSrc, options);
            int irSrcImageWidth = options.outWidth;
            int irSrcImageHeight = options.outHeight;
//    saveBitmap(bitmap, "/sdcard/palm/java_irSrc_bitmap.jpg")
            byte[] irDataSrc = convertJpegDataToGray(bitmap, irSrcImageWidth, irSrcImageHeight);

            if (pathRgbDest != null) {
                bitmap = BitmapFactory.decodeFile(pathRgbDest, options);
                int rgbDestImageWidth = options.outWidth;
                int rgbDestImageHeight = options.outHeight;
//    saveBitmap(bitmap, "/sdcard/palm/java_rgbDest_bitmap.jpg")
                byte[] rgbDataDest = convertJpegDataToRgb888(bitmap, rgbDestImageWidth, rgbDestImageHeight);

                rgbDestImageInstance =
                        new ImageInstance(rgbDestImageWidth, rgbDestImageHeight, rgbDataDest, ImageInstance.ImageFormat.IMG_3C8BIT);
            }

            bitmap = BitmapFactory.decodeFile(pathIrDest, options);
            int irDestImageWidth = options.outWidth;
            int irDestImageHeight = options.outHeight;
//    saveBitmap(bitmap, "/sdcard/palm/java_irDest_bitmap.jpg")
            byte[] irDataDest = convertJpegDataToGray(bitmap, irDestImageWidth, irDestImageHeight);


            ImageInstance irSrcImageInstance =
                    new ImageInstance(irSrcImageWidth, irSrcImageHeight, irDataSrc, ImageInstance.ImageFormat.IMG_1C8BIT);


            ImageInstance irDestImageInstance =
                    new ImageInstance(irDestImageWidth, irDestImageHeight, irDataDest, ImageInstance.ImageFormat.IMG_1C8BIT);
            // 从IR源图像和目标图像提取特征值

            if (mDevice != null) {
                ExtractOutput srcExtractOutput =
                        ((IVeinshine) mDevice).extractPalmFeaturesFromImg(rgbSrcImageInstance, irSrcImageInstance);
                ExtractOutput destExtractOutput =
                        ((IVeinshine) mDevice).extractPalmFeaturesFromImg(rgbDestImageInstance, irDestImageInstance);
                if (srcExtractOutput == null || destExtractOutput == null ||
                        srcExtractOutput.result != 0 || destExtractOutput.result != 0) {
                    runOnUiThread(() -> {
                        if (tvResult != null) {
                            tvResult.setText(getString(R.string.activity_example_extra_feature_failed));
                        }
                    });
                    return;
                }
                CompareOutput compareOutput =
                        ((IVeinshine) mDevice).compareFeatureScore(
                                srcExtractOutput.rgbFeature,
                                srcExtractOutput.irFeature,
                                destExtractOutput.rgbFeature,
                                destExtractOutput.irFeature);
                runOnUiThread(() -> {
                    if (tvResult != null) {
                        tvResult.setText(compareOutput.toString());
                    }
                });

            }

        }).start();
    }

    private byte[] convertJpegDataToRgb888(Bitmap bitmap, int width, int height) {
        int[] pixelsData = new int[width * height];
        bitmap.getPixels(pixelsData, 0, width, 0, 0, width, height);

        // 创建一个字节数组来存储RGB888格式的数据
        byte[] imageData = new byte[pixelsData.length * 3]; // 一个像素占3个字节，分别代表红、绿、蓝通道

        // 将RGB888格式的数据转换为字节数组
        for (int i = 0; i < pixelsData.length; i++) {
            int pixelValue = pixelsData[i];
            imageData[i * 3 + 2] = (byte) ((pixelValue >> 16) & 0xFF); // 红色通道
            imageData[i * 3 + 1] = (byte) ((pixelValue >> 8) & 0xFF); // 绿色通道
            imageData[i * 3] = (byte) (pixelValue & 0xFF); // 蓝色通道
        }
        return imageData;
    }

    private byte[] convertJpegDataToGray(Bitmap bitmap, int width, int height) {
        int[] grayData = new int[width * height];
        bitmap.getPixels(grayData, 0, width, 0, 0, width, height);

        byte[] grayByteArray = new byte[width * height];
        for (int i = 0; i < grayData.length; i++) {
            int grayValue = (int) (0.299 * ((grayData[i] >> 16) & 0xFF)
                    + 0.587 * ((grayData[i] >> 8) & 0xFF)
                    + 0.114 * (grayData[i] & 0xFF));
            grayByteArray[i] = (byte) grayValue;
        }

        return grayByteArray;
    }


    private boolean writeLicense() {
        File file = new File(getExternalCacheDir() + File.separator + "license.bin");
        if (!file.exists()) {
            Toast.makeText(ExampleActivity.this, getString(R.string.activity_example_license_no_exist), Toast.LENGTH_SHORT).show();
            return true;
        }
        try {
            FileInputStream fis = new FileInputStream(file);
            int available = fis.available();
            byte[] buffer = new byte[available];
            fis.read(buffer);
            fis.close();

            // 处理二进制数据
            String content = new String(buffer, StandardCharsets.ISO_8859_1);
            File file1 = new File(getExternalCacheDir() + File.separator + "out1.bin");
            FileOutputStream outputStream = new FileOutputStream(file1);
            outputStream.write(content.getBytes(StandardCharsets.ISO_8859_1));
            outputStream.flush();
            outputStream.close();
            Log.e("LBC", "license:" + content);
            if (mDevice != null && mIsOpenCamera) {
                int ret = ((IVeinshine) mDevice).writeLicense(content);
                Toast.makeText(ExampleActivity.this,
                        "license write" + (ret == 0 ? getString(R.string.activity_example_success) : getString(R.string.activity_example_failure)), Toast.LENGTH_SHORT).show();
                Log.e("LBC", "readLicense:" + ((IVeinshine) mDevice).readLicense());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void showToast(String string) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(ExampleActivity.this, string, Toast.LENGTH_SHORT).show();
        } else {
            runOnUiThread(() -> Toast.makeText(ExampleActivity.this, string, Toast.LENGTH_SHORT).show());
        }
    }

    private void stopCapture() {
        if (algoStatus != EnableAlgorithmStatus.ENABLE) {
            showToast(getString(R.string.activity_example_algo_enable_first));
            return;
        }
        if (mDevice != null) {
            IVeinshine veinshine = (IVeinshine) mDevice;
            int ret = veinshine.stopPalmCapture();
            Log.e("LBC", "stopPalmCapture ret:" + ret);
            if (ret != 0) {
                showToast(getString(R.string.activity_example_stop_capture_failed) + ret);
            } else {
                showToast(getString(R.string.activity_example_stop_capture_success));
            }
            mainHandler.post(() -> {
                rgbImage.setWillNotDraw(true);
                hideRect(mRectRoiRgbView);
                irImage.setWillNotDraw(true);
                hideRect(mRectRoiIrView);
            });
        }
    }

    private void capture() {
        if (algoStatus != EnableAlgorithmStatus.ENABLE) {
            showToast(getString(R.string.activity_example_algo_enable_first));
            return;
        }
        if (mDevice != null) {
            mCaptureRgbPath = CAPTURE_SRC_RGB_PATH;
            mCaptureIrPath = CAPTURE_SRC_IR_PATH;
            IVeinshine veinshine = (IVeinshine) mDevice;
            int ret = veinshine.capturePalm(mCapturePalmCallback, 15000, false);
            Log.e("LBC", "capturePalm ret:" + ret);
            if (ret != 0) {
                showToast(getString(R.string.activity_example_capture_continuously_failed) + ret);
            }
        }
    }

    private void captureOnceSrc() {
        if (algoStatus != EnableAlgorithmStatus.ENABLE) {
            showToast(getString(R.string.activity_example_algo_enable_first));
            return;
        }
        if (mDevice != null) {
            mCaptureRgbPath = CAPTURE_SRC_RGB_PATH;
            mCaptureIrPath = CAPTURE_SRC_IR_PATH;
            IVeinshine veinshine = (IVeinshine) mDevice;
            int ret = veinshine.capturePalmOnce(mCapturePalmCallback, 15000, false);
            Log.e("LBC", "capturePalmOnce ret:" + ret);
            if (ret != 0) {
                showToast(getString(R.string.activity_example_capture_one_failed) + ret);
            }
        }
    }

    private void captureOnceDest() {
        if (algoStatus != EnableAlgorithmStatus.ENABLE) {
            showToast(getString(R.string.activity_example_algo_enable_first));
            return;
        }
        if (mDevice != null) {
            mCaptureRgbPath = CAPTURE_DEST_RGB_PATH;
            mCaptureIrPath = CAPTURE_DEST_IR_PATH;
            IVeinshine veinshine = (IVeinshine) mDevice;
            int ret = veinshine.capturePalmOnce(mCapturePalmCallback, 15000, false);
            Log.e("LBC", "capturePalmOnce ret:" + ret);
            if (ret != 0) {
                showToast(getString(R.string.activity_example_capture_one_failed) + ret);
            }
        }
    }

    private String path = "";

    private void enableDimPalm() {
        if (algoStatus == EnableAlgorithmStatus.ENABLE) {
            showToast(getString(R.string.activity_example_algo_already_enable));
            return;
        }
        if (algoStatus == EnableAlgorithmStatus.INITIALIZING) {
            showToast(getString(R.string.activity_example_initializing));
            return;
        }
        showPathDialog();
    }

    private void showPathDialog() {
        final EditText inputEdit = new EditText(ExampleActivity.this);
        // 可以放在sdcard的私有目录下或者其他有权限的目录下,这里演示放在sdcard自定义目录下
        inputEdit.setText("/sdcard/HeyStar/models/");
        AlertDialog.Builder builder = new AlertDialog.Builder(ExampleActivity.this);
        builder.setTitle("Model Path:")
//        .setIcon(android.R.drawable.ic_dialog_info)
                .setView(inputEdit)
                .setNegativeButton(getString(R.string.activity_example_btn_cancel), null);
        builder.setPositiveButton(getString(R.string.activity_example_btn_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                path = inputEdit.getText().toString();
                if (TextUtils.isEmpty(path)) {
                    showToast(getString(R.string.activity_example_please_input_path));
                    return;
                }
                workServices.execute(() -> {
                    if (algoStatus != EnableAlgorithmStatus.DISABLE) {
                        return;
                    }
                    if (mDevice != null) {
                        IVeinshine veinshine = (IVeinshine) mDevice;
                        algoStatus = EnableAlgorithmStatus.INITIALIZING;
                        int ret = veinshine.enableDimPalm(path);
                        if (ret == 0) {
                            algoStatus = EnableAlgorithmStatus.ENABLE;
                            showToast(getString(R.string.activity_example_algo_enable_success));
                            Log.e(TAG, "algo version:" + veinshine.getAlgorithmVersion());
                        } else {
                            showToast(getString(R.string.activity_example_algo_enable_failed) + ret);
                            algoStatus = EnableAlgorithmStatus.DISABLE;
                        }
                    }
                });
            }
        });
        builder.show();
    }


    protected void showRgbImage(ImageView image, byte[] data, int cols, int rows) {
        buildRgbBitmap(data, cols, rows);
        if (mRgbBitmap != null) {
            image.setWillNotDraw(false);
            image.setImageBitmap(mRgbBitmap);
            saveBitmap(mRgbBitmap, mCaptureRgbPath);
        }
    }

    protected void showIrImage(ImageView image, byte[] data, int cols, int rows) {
        buildIrBitmap(data, cols, rows);
        if (mIrBitmap != null) {
            image.setWillNotDraw(false);
            image.setImageBitmap(mIrBitmap);
            saveBitmap(mIrBitmap, mCaptureIrPath);
        }
    }

    private void saveBitmap(Bitmap bitmap, String path) {
        FileUtils.delete(path);
        BitmapUtils.save(bitmap, path, Bitmap.CompressFormat.PNG, 100);
    }

    protected void showRect(DtRectRoiView picView, CaptureFrame frame) {
        BBox box = new BBox(0, 0, 0, 0);
//    Log.i(TAG, "frame.palmRectX = " + frame.palmRectX);
//    Log.i(TAG, "frame.palmRectY = " + frame.palmRectY);
//    Log.i(TAG, "frame.palmRectW = " + frame.palmRectW);
//    Log.i(TAG, "frame.palmRectH = " + frame.palmRectH);
        box.x = frame.palmRectX;
        box.y = frame.palmRectY;
        box.w = frame.palmRectW;
        box.h = frame.palmRectH;
        picView.setRect(box, 0);
    }

    protected void hideRect(DtRectRoiView picView) {
        BBox box = new BBox(0, 0, 0, 0);
        picView.setRect(box, 0);
    }

    protected void buildRgbBitmap(byte[] data, int width, int height) {
        try {
            // RGBA 数组
            byte[] Bits = new byte[data.length / 3 * 4];
            int i;
            for (i = 0; i < data.length / 3; i++) {
                // 原理：4个字节表示一个灰度，则BGR  = 灰度值，最后一个Alpha = 0xff;
                Bits[i * 4] = data[i * 3 + 2];
                Bits[i * 4 + 1] = data[i * 3 + 1];
                Bits[i * 4 + 2] = data[i * 3];
                Bits[i * 4 + 3] = -1; // 0xff
            }
            // Bitmap.Config.ARGB_8888 表示：图像模式为8位
            if (mRgbBitmap == null) {
                Log.i(TAG, "buildRgbBitmap createBitmap");
                mRgbBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                mRectRoiRgbView.resizeSource(width, height);
            } else {
                if (mRgbBitmap.getHeight() != height || mRgbBitmap.getWidth() != width) {
                    mRgbBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    mRectRoiRgbView.resizeSource(width, height);
                }
            }
            Buffer buffer = ByteBuffer.wrap(Bits);
            buffer.rewind();
            mRgbBitmap.copyPixelsFromBuffer(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void buildIrBitmap(byte[] data, int width, int height) {
        try {
            byte[] Bits = new byte[data.length * 4]; // RGBA 数组
            int i;
            for (i = 0; i < data.length; i++) {
                // 原理：4个字节表示一个灰度，则RGB  = 灰度值，最后一个Alpha = 0xff;
                Bits[i * 4] = Bits[i * 4 + 1] = Bits[i * 4 + 2] = data[i];
                Bits[i * 4 + 3] = -1; // 0xff
            }
            // Bitmap.Config.ARGB_8888 表示：图像模式为8位
            if (mIrBitmap == null) {
                mIrBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                mRectRoiIrView.resizeSource(width, height);
            } else {
                if (mIrBitmap.getHeight() != height || mIrBitmap.getWidth() != width) {
                    mIrBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    mRectRoiIrView.resizeSource(width, height);
                }
            }
            mIrBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected byte[] raw16ToRaw8(byte[] inputData) {
        byte[] outputData = new byte[inputData.length / 2];
        for (int i = 0; i < outputData.length; i++) {
            if (inputData[2 * i + 1] > 0) {
                outputData[i] = (byte) (255);
            } else {
                outputData[i] = inputData[2 * i];
            }
        }
        return outputData;
    }


    protected void convert8bit(byte[] data, int width, int height) {
        try {
            // RGBA 数组
            byte[] Bits = new byte[data.length * 4];
            int i;
            for (i = 0; i < data.length; i++) {
                // 原理：4个字节表示一个灰度，则RGB  = 灰度值，最后一个Alpha = 0xff;
                Bits[i * 4] = Bits[i * 4 + 1] = Bits[i * 4 + 2] = data[i];
                Bits[i * 4 + 3] = -1; // 0xff
            }
            // Bitmap.Config.ARGB_8888 表示：图像模式为8位
            if (mDepthBitmap == null) {
                mDepthBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            }
            mDepthBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private ICapturePalmCallback mCapturePalmCallback = new ICapturePalmCallback() {
        @Override
        public void onCaptureFrame(CaptureFrame frame) {
            Log.e(TAG, "onCaptureFrame()");

            if (frame != null) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (frame.rgbData != null) {
                            showRgbImage(rgbImage, frame.rgbData, frame.rgbCols, frame.rgbRows);
                            showRect(mRectRoiRgbView, frame);
                        } else {
                            rgbImage.setWillNotDraw(true);
                            hideRect(mRectRoiRgbView);
                        }
                        if (frame.irData != null) {
                            showIrImage(irImage, frame.irData, frame.irCols, frame.irRows);
                            showRect(mRectRoiIrView, frame);
                        } else {
                            irImage.setWillNotDraw(true);
                            hideRect(mRectRoiIrView);
                        }
                    }
                });
            }

        }

        @Override
        public void onCapturePalmHint(Hint hint, HashMap<Integer, Float> hashMap) {
//      Log.e(TAG, "onCapturePalmHint()");
            if (hint == Hint.NO_PALM_DETECTED) {
                mainHandler.post(() -> {
                    rgbImage.setWillNotDraw(true);
                    hideRect(mRectRoiRgbView);
//          faceHintTextView.setText(hint.value);
                    irImage.setWillNotDraw(true);
                    hideRect(mRectRoiIrView);
                });
            }
            if (hint == Hint.TIMEOUT) {
                runOnUiThread(() -> Toast.makeText(ExampleActivity.this, getString(R.string.activity_example_capture_timeout), Toast.LENGTH_SHORT).show());
            }
        }
    };

    private void openDevice() {
        if (mIsOpenCamera) {
            Toast.makeText(ExampleActivity.this, getString(R.string.activity_example_camera_is_opened), Toast.LENGTH_SHORT).show();
            return;
        }
        Device.create(ExampleActivity.this, new Device.DeviceListener() {
            @Override
            public void onDeviceCreatedSuccess(IDevice device, int deviceIndex, Map<Long, IDevice> runningDevice, UsbMapTable.DeviceType deviceType) {
                Log.i(TAG, "onDeviceCreate, deviceType:" + deviceType + " deviceIndex:" + deviceIndex);
                deviceThread1.execute(() -> open(device, deviceIndex, runningDevice, deviceType));
            }

            @Override
            public void onDeviceCreateFailed(IDevice device) {

            }

            @Override
            public void onDeviceDestroy(IDevice device) {
                Log.i(TAG, "onDeviceDestroy()");
                if (mDevice != null) {
                    mDevice = null;
                }
                mIsOpenCamera = false;
                algoStatus = EnableAlgorithmStatus.DISABLE;
                mIsRunning = false;
                rgbFrameData1 = null;
                irFrameData1 = null;
                if (mListStreamType.size() > 0) {
                    mListStreamType.clear();
                    mainHandler.post(() -> {
                        mAdapterStreamType.notifyDataSetChanged();
                    });
                    mainHandler.postDelayed(() -> clearFrame(), 200);
                }

            }
        }, new DtUsbManager.DeviceStateListener() {
            @Override
            public void onDevicePermissionGranted(DtUsbDevice dtUsbDevice) {

            }

            @Override
            public void onDevicePermissionDenied(DtUsbDevice dtUsbDevice) {

            }

            @Override
            public void onAttached(DtUsbDevice dtUsbDevice) {
                Log.e(TAG, "onAttached()");

            }

            @Override
            public void onDetached(DtUsbDevice dtUsbDevice) {
                Log.e(TAG, "onDetached()");

                mIsOpenCamera = false;
                algoStatus = EnableAlgorithmStatus.DISABLE;
                mainHandler.post(() -> {
                    if (mTvDeviceInfo != null) {
                        mTvDeviceInfo.setText("Device Detached!");
                    }
                    if (mSwitchStartStream != null) {
                        mSwitchStartStream.setChecked(false);
                    }
                });

            }
        });
    }

    private void startStream() {
        if (mDevice != null) {
            Thread thread = generateStreamThread(mDevice, 1);
            thread.start();
        }
    }

    private Thread generateStreamThread(final IDevice device, int deviceIndex) {
        Thread streamThread = new Thread(() -> {
            if (currentStreamType != StreamType.INVALID_STREAM_TYPE) {
                IStream stream = device.createStream(currentStreamType);
                if (stream == null) {
                    mainHandler.post(() -> Toast.makeText(ExampleActivity.this, getString(R.string.activity_example_stream_create_failed), Toast.LENGTH_SHORT).show());
                    return;
                }
                Frames frames = stream.allocateFrames();
                int ret = stream.start();
                if (ret == 0) {
                    mIsRunning = true;
                } else {
                    mIsRunning = false;
                    return;
                }
                while (mIsRunning) {
                    int res = stream.getFrames(frames, 2000);
                    if (res != 0) {
                        Log.e(TAG, "get getFrame code: " + Integer.toHexString(res) + "deviceIndex:" + deviceIndex);
                        continue;
                    }

                    Frame frame1 = frames.getFrame(0);
                    Frame frame2 = frames.getFrame(1);

                    //渲染图像帧
                    onDrawFrame(frame1, frame2, deviceIndex);

                }
                if (mDevice != null) {
                    stream.stop();
                    device.destroyStream(stream);
                }
                Log.e("LBC", "getFrame thread exit");
            }
        });
        return streamThread;
    }

    private void onDrawFrame(Frame frame1, Frame frame2, int deviceIndex) {
        if (frame1 != null) {
            switch (frame1.getFrameType()) {
                case RGB_FRAME:
                    if (deviceIndex == 1) {
                        rgbFrameW1 = frame1.getWidth();
                        rgbFrameH1 = frame1.getHeight();
                        rgbFrameData1 = frame1.getRawData();
                        rgbFrameExtraInfo = frame1.getExtraInfo();
                    }
                    break;
                case IR_FRAME:
                    if (deviceIndex == 1) {
                        irFrameW1 = frame1.getWidth();
                        irFrameH1 = frame1.getHeight();
                        irFrameData1 = frame1.getRawData();
                        irFrameExtraInfo = frame1.getExtraInfo();
                    }
                    break;
                default:
            }
        }
        if (frame2 != null) {
            switch (frame2.getFrameType()) {
                case RGB_FRAME:
                    if (deviceIndex == 1) {
                        rgbFrameW1 = frame2.getWidth();
                        rgbFrameH1 = frame2.getHeight();
                        rgbFrameData1 = frame2.getRawData();
                        rgbFrameExtraInfo = frame2.getExtraInfo();
                    }
                    break;
                case IR_FRAME:
                    if (deviceIndex == 1) {
                        irFrameW1 = frame2.getWidth();
                        irFrameH1 = frame2.getHeight();
                        irFrameData1 = frame2.getRawData();
                        irFrameExtraInfo = frame2.getExtraInfo();
                    }
                    break;
                default:
            }
        }
        if (deviceIndex == 1) {
            if (null != irFrameData1 && null != irDisPlay && null != mGLIrView) {
                irDisPlay.render(mGLIrView,
                        0,
                        false,
                        irFrameData1,
                        irFrameW1,
                        irFrameH1,
                        2,
                        new int[]{irFrameExtraInfo.palmRoi[0],
                                irFrameExtraInfo.palmRoi[1],
                                irFrameExtraInfo.palmRoi[2],
                                irFrameExtraInfo.palmRoi[3]});
            }
            if (null != rgbFrameData1 && null != rgbDisPlay && null != mGLRgbView) {
                rgbDisPlay.render(mGLRgbView,
                        0,
                        false,
                        rgbFrameData1,
                        rgbFrameW1,
                        rgbFrameH1,
                        1,
                        new int[]{rgbFrameExtraInfo.palmRoi[0],
                                rgbFrameExtraInfo.palmRoi[1],
                                rgbFrameExtraInfo.palmRoi[2],
                                rgbFrameExtraInfo.palmRoi[3]});
            }
        }

    }

    private void open(IDevice device, int deviceIndex, Map<Long, IDevice> mRunningDevice, UsbMapTable.DeviceType deviceType) {
        device.open(new IOpenCallback() {
            @Override
            public void onDownloadPrepare() {
            }

            @Override
            public void onDownloadProgress(int progress) {

            }

            @Override
            public void onDownloadSuccess() {
            }

            @Override
            public void onOpenSuccess() {
                mIsOpenCamera = true;
                DeviceInfo deviceInfo = ((IVeinshine) device).getDeviceInfo();
                Log.d("LBC", "firmware version:" + deviceInfo.firmware_version);
                mainHandler.post(() -> {
                    if (mTvDeviceInfo != null) {
                        mTvDeviceInfo.setText("DeviceName:" + deviceInfo.device_name);
                    }
                });

                if (deviceIndex == 1) {
                    Log.e("LBC", "mDevice1");
                    mDevice = device;

                    List<StreamType> deviceSupportStreamTypeList = device.getDeviceSupportStreamType();
                    if (deviceSupportStreamTypeList.size() > 0) {
                        mListStreamType.clear();
                        mListStreamType.addAll(deviceSupportStreamTypeList);
                        mainHandler.post(() -> mAdapterStreamType.notifyDataSetChanged());
                    }
                }
            }

            @Override
            public void onOpenFail(int errorCode) {
                mainHandler.post(() -> Toast.makeText(ExampleActivity.this, "open device error:" + errorCode, Toast.LENGTH_SHORT).show());
            }
        });
    }


    private void clearFrame() {
        Log.e(TAG, "-------cleanFrame()------");
        if (null != rgbDisPlay && null != mGLRgbView) {
            rgbDisPlay.render(mGLRgbView, 0, false, new byte[rgbFrameW1 * rgbFrameH1 * 3],
                    rgbFrameW1, rgbFrameH1, 1);
        }
        if (null != irDisPlay && null != mGLIrView) {
            irDisPlay.render(mGLIrView, 0, false, new byte[irFrameW1 * irFrameH1],
                    irFrameW1, irFrameH1, 2);
        }
        rgbFrameData1 = null;
        irFrameData1 = null;
    }

    public void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = DialogUtils.createLoadingDialog(this, message, false);
        }
        progressDialog.show();
    }

    public void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public void onBackPressed() {

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGLRgbView.onPause();
        mGLIrView.onPause();
        rgbDisPlay.release();
        irDisPlay.release();
        rgbDisPlay = null;
        irDisPlay = null;

    }

}
