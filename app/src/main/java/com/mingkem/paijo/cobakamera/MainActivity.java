package com.mingkem.paijo.cobakamera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    List<String> listVideo = new ArrayList<>();
    boolean isCapturingVideo = false;
    private int VIDEO_QUALITY = CamcorderProfile.QUALITY_1080P;
    private int VIDEO_MAX_DURATION = 600000; // Set max duration 60 sec
    private int VIDEO_MAX_SIZE = 10000000; // Set max file size 10MB
    private Camera mCameraVideo;
    private HqqCameraPreview hqqCameraPreview;
    private MediaRecorder mediaRecorder;
    private Button btnHold;
    private Button switchCamera;
    private Button btnMerge;
    private Button btnTakePhoto;
    private Context myContext;
    private TextView counterTxt;
    private int counterDown = 60;
    private LinearLayout layoutCameraPreview;
    private boolean cameraFront = false;
    private CountDownTimer countDownTimer = new CountDownTimer(60000, 1000) {
        @Override
        public void onTick(long l) {
            counterTxt.setText("00:".concat(String.valueOf(counterDown)));
            counterDown--;
            if (counterDown == 0){
                stopRecord();
            }
        }

        @Override
        public void onFinish() {
            //stopRecord();
        }
    };
    private Handler mHandler = new Handler();
    private int counter = 0;

    private Runnable mHoldButtonRun = new Runnable() {
        public void run() {
            counter++;
            mHandler.postAtTime(this, SystemClock.uptimeMillis() + 100);
            if (counter > 1) {
                if (!isCapturingVideo) {
                    startRecord();
                    btnHold.setText("Merekam");
                    countDownTimer.start();
                    isCapturingVideo = true;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myContext = this;
        initialize();
    }

    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            android.hardware.Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                cameraFront = true;
                break;
            }
        }
        return cameraId;
    }

    private int findBackFacingCamera() {
        int cameraId = -1;
        // Search for the back facing camera
        // get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        // for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                cameraFront = false;
                break;
            }
        }
        return cameraId;
    }

    public void onResume() {
        super.onResume();
        if (!hasCamera(myContext)) {
            Toast toast = Toast.makeText(myContext, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
            toast.show();
            finish();
        }
        if (mCameraVideo == null) {
            // if the front facing camera does not exist
            if (findFrontFacingCamera() < 0) {
                Toast.makeText(this, "No front facing camera found.", Toast.LENGTH_LONG).show();
                switchCamera.setVisibility(View.GONE);
            }
            mCameraVideo = Camera.open(findBackFacingCamera());
            hqqCameraPreview.refreshCamera(mCameraVideo);
        }
    }

    private boolean mergeMediaFiles(boolean isAudio/*, String sourceFiles[],String targetFile*/) {
        try {
            String mediaKey = isAudio ? "soun" : "vide";
            List<Movie> listMovies = new ArrayList<>();

            for (int i = 0; i < listVideo.size(); i++) {
                listMovies.add(MovieCreator.build(listVideo.get(i)));
            }

            List<Track> listTracks = new LinkedList<>();
            for (Movie movie : listMovies) {
                for (Track track : movie.getTracks()) {
                    if (track.getHandler().equals(mediaKey)) {
                        listTracks.add(track);
                    }
                }
            }
            Movie outputMovie = new Movie();
            if (!listTracks.isEmpty()) {
                outputMovie.addTrack(new AppendTrack(listTracks.toArray(new Track[listTracks.size()])));
            }

            String targetFile = "/sdcard/merge-hqq".concat(String.valueOf(System.currentTimeMillis())).concat(".mp4");

            Container container = new DefaultMp4Builder().build(outputMovie);
            FileChannel fileChannel = new RandomAccessFile(String.format(targetFile), "rw").getChannel();
            container.writeContainer(fileChannel);
            fileChannel.close();
            Toast.makeText(myContext, "Merged -> " + targetFile, Toast.LENGTH_LONG).show();
            return true;
        } catch (IOException e) {
            Log.e("TES", "Error merging media files. exception: " + e.getMessage());
            return false;
        }
    }

    private void initElement() {
        // init element
        btnHold = findViewById(R.id.btnHold);
        btnMerge = findViewById(R.id.btnMerge);
        layoutCameraPreview = findViewById(R.id.camera_preview);
        switchCamera = findViewById(R.id.button_ChangeCamera);
        counterTxt = findViewById(R.id.counterTxt);
        btnTakePhoto =  findViewById(R.id.btnTakePhoto);
    }

    private void initAction() {
        if (hqqCameraPreview == null)
            hqqCameraPreview = new HqqCameraPreview(myContext, mCameraVideo);
        layoutCameraPreview.addView(hqqCameraPreview);

        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCameraVideo != null){
                    mCameraVideo.takePicture(shutterCallback, pictureCallbackRaw, new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] bytes, Camera camera) {
                            Bitmap bitmapPicture = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            Bitmap correctBmp = Bitmap.createBitmap(bitmapPicture, 0, 0, bitmapPicture.getWidth(), bitmapPicture.getHeight(), null, true);
                        }
                    });
                }
            }
        });

        switchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get the number of cameras
                if (!isCapturingVideo) {
                    int camerasNumber = Camera.getNumberOfCameras();
                    if (camerasNumber > 1) {
                        // release the old camera instance
                        // switch camera, from the front and the back and vice versa
                        releaseCamera();
                        chooseCamera();
                    } else {
                        Toast toast = Toast.makeText(myContext, "Sorry, your phone has only one camera!", Toast.LENGTH_LONG);
                        toast.show();
                    }
                }
            }
        });

        btnHold.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    mHandler.removeCallbacks(mHoldButtonRun);
                    mHandler.postAtTime(mHoldButtonRun,
                            SystemClock.uptimeMillis() + 50);
                } else if (action == MotionEvent.ACTION_UP) {
                    mHandler.removeCallbacks(mHoldButtonRun);
                    if (isCapturingVideo) {
                        countDownTimer.cancel();
                        stopRecord();
                    }
                }
                return false;
            }
        });

        btnMerge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mergeMediaFiles(false);
            }
        });
    }

    public void initialize() {
        initElement();
        initAction();
    }

    public void chooseCamera() {
        // if the camera preview is the front
        if (cameraFront) {
            int cameraId = findBackFacingCamera();
            if (cameraId >= 0) {
                // open the backFacingCamera
                // set a picture callback
                // refresh the preview

                mCameraVideo = Camera.open(cameraId);
                // mPicture = getPictureCallback();
                hqqCameraPreview.refreshCamera(mCameraVideo);
            }
        } else {
            int cameraId = findFrontFacingCamera();
            if (cameraId >= 0) {
                // open the backFacingCamera
                // set a picture callback
                // refresh the preview

                mCameraVideo = Camera.open(cameraId);
                // mPicture = getPictureCallback();
                hqqCameraPreview.refreshCamera(mCameraVideo);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // when on Pause, release camera in order to be used from other
        // applications
        releaseCamera();
    }

    private boolean hasCamera(Context context) {
        // check if the device has camera
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    private void startRecord() {
        if (isCapturingVideo) return;

        if (!prepareMediaRecorder()) {
            Toast.makeText(MainActivity.this, "Fail in prepareMediaRecorder()!\n - Ended -", Toast.LENGTH_LONG).show();
            finish();
        }

        // work on UiThread for better performance
        runOnUiThread(new Runnable() {
            public void run() {
                // If there are stories, add them to the table
                try {
                    mediaRecorder.start();
                    Toast.makeText(MainActivity.this, "Mulai merekam", Toast.LENGTH_LONG).show();
                } catch (final Exception ex) {
                    // Log.i("---","Exception in thread");
                }
            }
        });

        isCapturingVideo = true;
    }

    private void stopRecord() {
        if (isCapturingVideo) {
            // stop isCapturingVideo and release camera
            mediaRecorder.stop(); // stop the isCapturingVideo
            releaseMediaRecorder(); // release the MediaRecorder object
            btnHold.setText("Rekam");
            Toast.makeText(MainActivity.this, "Video captured!", Toast.LENGTH_LONG).show();
            isCapturingVideo = false;
        }
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset(); // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            mCameraVideo.lock(); // lock camera for later use
        }
    }

    private boolean prepareMediaRecorder() {

        mediaRecorder = new MediaRecorder();

        mCameraVideo.unlock();
        mediaRecorder.setCamera(mCameraVideo);

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mediaRecorder.setProfile(CamcorderProfile.get(VIDEO_QUALITY));

        String fileName = "/sdcard/hqq".concat(String.valueOf(System.currentTimeMillis())).concat(".mp4");
        mediaRecorder.setOutputFile(fileName);
        listVideo.add(fileName);
        mediaRecorder.setMaxDuration(VIDEO_MAX_DURATION);
        mediaRecorder.setMaxFileSize(VIDEO_MAX_SIZE);

        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            releaseMediaRecorder();
            return false;
        }
        return true;

    }

    private void releaseCamera() {
        // stop and release camera
        if (mCameraVideo != null) {
            mCameraVideo.release();
            mCameraVideo = null;
        }
    }

    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback(){

        public void onShutter() {
            // TODO Auto-generated method stub
        }
    };

    Camera.PictureCallback pictureCallbackRaw = new Camera.PictureCallback(){

        public void onPictureTaken(byte[] arg0, Camera arg1) {
            // TODO Auto-generated method stub
        }
    };

    Camera.PictureCallback pictureCallbackJpg = new Camera.PictureCallback(){

        public void onPictureTaken(byte[] arg0, Camera arg1) {
            // TODO Auto-generated method stub
            Bitmap bitmapPicture = BitmapFactory.decodeByteArray(arg0, 0, arg0.length);
            Bitmap correctBmp = Bitmap.createBitmap(bitmapPicture, 0, 0, bitmapPicture.getWidth(), bitmapPicture.getHeight(), null, true);
        }
    };
}
