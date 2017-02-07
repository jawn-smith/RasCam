package rpi.rpicam;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;
import com.google.firebase.iid.FirebaseInstanceId;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Properties;

public class Stream extends AppCompatActivity implements IVLCVout.Callback, LibVLC.OnNativeCrashListener {
    public final static String TAG = "LibVLCAndroid/Stream";

    //public final static String LOCATION = "com.compdigitec.libvlcandroidsample.VideoActivity.location";

    private String mFilePath;

    String command;

    // display surface
    private SurfaceView mSurface;
    private SurfaceHolder holder;

    // media player
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);

        // Receive path to play from intent
        Bundle b = getIntent().getExtras();
        mFilePath = b.getString("http");
        //mFilePath = "http://YourPublicIP:691";

        if (mFilePath.contains("691")){
            command = "./end_stream_downstairs.sh";
        }
        else{
            command = "./end_stream_upstairs.sh";
        }

        Log.d(TAG, "Playing back " + mFilePath);

        mSurface = (SurfaceView) findViewById(R.id.surface);
        holder = mSurface.getHolder();
        //holder.addCallback(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    protected void onResume() {
        super.onResume();
        createPlayer(mFilePath);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    private void setSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        if (mVideoWidth * mVideoHeight <= 1)
            return;

        if(holder == null || mSurface == null)
            return;

        // get screen size
        int w = getWindow().getDecorView().getWidth();
        int h = getWindow().getDecorView().getHeight();

        // getWindow().getDecorView() doesn't always take orientation into
        // account, we have to correct the values
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (w > h && isPortrait || w < h && !isPortrait) {
            int i = w;
            w = h;
            h = i;
        }

        float videoAR = (float) mVideoWidth / (float) mVideoHeight;
        float screenAR = (float) w / (float) h;

        if (screenAR < videoAR)
            h = (int) (w / videoAR);
        else
            w = (int) (h * videoAR);

        // force surface buffer size
        holder.setFixedSize(mVideoWidth, mVideoHeight);

        // set display size
        LayoutParams lp = mSurface.getLayoutParams();
        lp.width = w;
        lp.height = h;
        mSurface.setLayoutParams(lp);
        mSurface.invalidate();
    }

    private void createPlayer(String media) {
        releasePlayer();
        try {
            if (media.length() > 0) {
                Toast toast = Toast.makeText(this, media, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0,
                        0);
                toast.show();
            }

            // Create LibVLC
            // TODO: make this more robust, and sync with audio demo
            ArrayList<String> options = new ArrayList<>();
            //options.add("--subsdec-encoding <encoding>");
            options.add("-vvv"); // verbosity
            libvlc = new LibVLC(options);
            libvlc.setOnNativeCrashListener(this);
            holder.setKeepScreenOn(true);

            // Create media player
            mMediaPlayer = new MediaPlayer(libvlc);
            mMediaPlayer.setEventListener(mPlayerListener);

            // Set up video output
            final IVLCVout vout = mMediaPlayer.getVLCVout();
            vout.setVideoView(mSurface);
            //vout.setSubtitlesView(mSurfaceSubtitles);
            vout.addCallback(this);
            vout.attachViews();

            Media m = new Media(libvlc, Uri.parse(media));
            mMediaPlayer.setMedia(m);
            mMediaPlayer.play();
        } catch (Exception e) {
            Toast.makeText(this, "Error creating player!", Toast.LENGTH_LONG).show();
        }
    }

    // TODO: handle this cleaner
    private void releasePlayer() {
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        holder = null;
        libvlc.release();
        libvlc = null;

        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    private MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);

    @Override
    public void onNewLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        if (width * height == 0)
            return;

        // store video size
        mVideoWidth = width;
        mVideoHeight = height;
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    public void onHardwareAccelerationError(IVLCVout vlcVout) {
        // Handle errors with hardware acceleration
        Log.e(TAG, "Error with hardware acceleration");
        this.releasePlayer();
        Toast.makeText(this, "Error with hardware acceleration", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSurfacesCreated(IVLCVout vout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vout) {

    }

    private static class MyPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<Stream> mOwner;

        public MyPlayerListener(Stream owner) {
            mOwner = new WeakReference<>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            Stream player = mOwner.get();

            switch(event.type) {
                case MediaPlayer.Event.EndReached:
                    Log.d(TAG, "MediaPlayerEndReached");
                    player.releasePlayer();
                    break;
                case MediaPlayer.Event.Playing:
                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:
                default:
                    break;
            }
        }
    }

    @Override
    public void onNativeCrash() {
        // Handle errors with hardware acceleration
        Log.e(TAG, "Native Crash");
        this.releasePlayer();
        Toast.makeText(this, "Native Crash", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed(){
        //new ServerCommand("Restoring Camera to Motion Detection Mode").execute(command);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Video Streaming");
        builder.setMessage("Would you like to save the video that you just streamed to the permanent archive?");

        // Set up the buttons
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (getIntent().getExtras().getString("http").contains("691")){
                    command="./end_stream_downstairs.sh; video_name=$( date +Downstairs-Stream-%m-%d-%Y-%H-%M ); MP4Box -add downstairs_stream.h264 $video_name.mp4; mv $video_name.mp4 permanent_archive; rm downstairs_stream.h264";
                }
                else{
                    command="./end_stream_upstairs.sh; video_name=$( date +Upstairs-Stream-%m-%d-%Y-%H-%M ); MP4Box -add upstairs_stream.h264 $video_name.mp4; mv $video_name.mp4 permanent_archive; rm upstairs_stream.h264";
                }

                new ServerCommand("Saving Files").execute(command);

            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                if (getIntent().getExtras().getString("http").contains("691")){
                    command="./end_stream_downstairs.sh";
                }
                else{
                    command="./end_stream_upstairs.sh";
                }

                new ServerCommand("Restoring Cameras to Motion Detection Mode").execute(command);            }
        });

        builder.show();

    }

    private class ServerCommand extends AsyncTask<String, String, String> {

        String result = "";

        private ProgressDialog dialog = new ProgressDialog(Stream.this);

        //@Override
        public ServerCommand(String message) {
            //super.onPreExecute();
            //String message=args[1];
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.setMessage(message);
            dialog.show();
        }

        @Override
        protected String doInBackground(String... args) {
            String refreshedToken = FirebaseInstanceId.getInstance().getToken();
            //String command="cat "+ refreshedToken + " >user_token.out";
            String command = args[0];
            Log.d("Refreshed token", refreshedToken);
            try {
                JSch js = new JSch();
                Session s = js.getSession("pi", "YourPublicIP", 69);
                s.setPassword("Have@good1");
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                s.setConfig(config);
                s.connect();

                Channel c = s.openChannel("exec");
                ChannelExec ce = (ChannelExec) c;

                ce.setCommand(command);
                ce.setErrStream(System.err);

                ce.connect();

                /*BufferedReader reader = new BufferedReader(new InputStreamReader(ce.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result += line;
                }*/

                //Thread.sleep(3000);

                while (! ce.isClosed()){
                    Thread.sleep(500);
                }

                ce.disconnect();
                s.disconnect();

                return result;


            } catch (Exception e) {
                Toast.makeText(getBaseContext(), "An error Ocurred", Toast.LENGTH_LONG).show();
                return result;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            //super.onPostExecute(String result);

            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            //String refreshedToken = FirebaseInstanceId.getInstance().getToken();
            //Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG).show();
            startActivity(new Intent(Stream.this,StatusCheck.class));
        }
    }

}