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
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
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

public class splitStream extends AppCompatActivity implements IVLCVout.Callback, LibVLC.OnNativeCrashListener {
    public final static String TAG = "LibVLCAndroid/split";

    //public final static String LOCATION = "com.compdigitec.libvlcandroidsample.VideoActivity.location";

    private String mFilePath1, mFilePath2;

    // display surface
    private SurfaceView mSurface1, mSurface2;
    private SurfaceHolder holder1, holder2;

    // media player
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer1 = null,mMediaPlayer2=null;
    private int mVideoWidth;
    private int mVideoHeight;
    private final static int VideoSizeChanged = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_split_stream);

        // Receive path to play from intent
        Intent intent = getIntent();
        //mFilePath = intent.getExtras().getString("http");
        mFilePath1 = "http://YourPublicIP:691";
        mFilePath2 = "http://YourPublicIP:692";

        mSurface1 = (SurfaceView) findViewById(R.id.splitSurface1);
        holder1 = mSurface1.getHolder();

        mSurface2 = (SurfaceView) findViewById(R.id.splitSurface2);
        holder2 = mSurface2.getHolder();

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
        createPlayer(mFilePath1,mFilePath2);
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

        if(holder1 == null || mSurface1 == null || holder2 == null || mSurface2 == null)
            return;

        // get screen size
        int w = getWindow().getDecorView().getWidth();
        int h = getWindow().getDecorView().getHeight()/2;

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
        holder1.setFixedSize(mVideoWidth, mVideoHeight);
        holder2.setFixedSize(mVideoWidth, mVideoHeight);

        // set display size
        LayoutParams lp1 = mSurface1.getLayoutParams();
        lp1.width = w;
        lp1.height = h;
        mSurface1.setLayoutParams(lp1);
        mSurface1.invalidate();

        LayoutParams lp2 = mSurface2.getLayoutParams();
        lp2.width = w;
        lp2.height = h;
        mSurface2.setLayoutParams(lp2);
        mSurface2.invalidate();
    }

    private void createPlayer(String media1, String media2) {
        releasePlayer();
        try {

            // Create LibVLC
            // TODO: make this more robust, and sync with audio demo
            ArrayList<String> options = new ArrayList<>();
            //options.add("--subsdec-encoding <encoding>");
            options.add("-vvv"); // verbosity
            libvlc = new LibVLC(options);
            libvlc.setOnNativeCrashListener(this);
            holder1.setKeepScreenOn(true);
            holder2.setKeepScreenOn(true);

            // Create media player
            mMediaPlayer1 = new MediaPlayer(libvlc);
            mMediaPlayer1.setEventListener(mPlayerListener);
            mMediaPlayer2 = new MediaPlayer(libvlc);
            mMediaPlayer2.setEventListener(mPlayerListener);

            // Set up video output
            final IVLCVout vout1 = mMediaPlayer1.getVLCVout();
            final IVLCVout vout2 = mMediaPlayer2.getVLCVout();
            vout1.setVideoView(mSurface1);
            //vout.setSubtitlesView(mSurfaceSubtitles);
            vout1.addCallback(this);
            vout1.attachViews();
            vout2.setVideoView(mSurface2);
            //vout.setSubtitlesView(mSurfaceSubtitles);
            vout2.addCallback(this);
            vout2.attachViews();

            Media m1 = new Media(libvlc, Uri.parse(media1));
            mMediaPlayer1.setMedia(m1);
            mMediaPlayer1.play();

            Media m2 = new Media(libvlc, Uri.parse(media2));
            mMediaPlayer2.setMedia(m2);
            mMediaPlayer2.play();
        } catch (Exception e) {
            Toast.makeText(this, "Error creating player!", Toast.LENGTH_LONG).show();
        }
    }

    // TODO: handle this cleaner
    private void releasePlayer() {
        if (libvlc == null)
            return;
        mMediaPlayer1.stop();
        final IVLCVout vout1 = mMediaPlayer1.getVLCVout();
        vout1.removeCallback(this);
        vout1.detachViews();
        holder1 = null;

        mMediaPlayer2.stop();
        final IVLCVout vout2 = mMediaPlayer2.getVLCVout();
        vout2.removeCallback(this);
        vout2.detachViews();
        holder2 = null;

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
        private WeakReference<splitStream> mOwner;

        public MyPlayerListener(splitStream owner) {
            mOwner = new WeakReference<>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            splitStream player = mOwner.get();

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
        //new ServerCommand("Restoring Camera to Motion Detection Mode").execute("./end_splitStream.sh");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Video Streaming");
        builder.setMessage("Would you like to save the video that you just streamed to the permanent archive?");

        // Set up the buttons
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new ServerCommand("Saving Files").execute("./end_splitStream.sh; video_name=$( date +Downstairs-Stream-%m-%d-%Y-%H-%M ); MP4Box -add downstairs_stream.h264 $video_name.mp4; video_name=$( date +Upstairs-Stream-%m-%d-%Y-%H-%M ); MP4Box -add upstairs_stream.h264 $video_name.mp4; mv *Stream*mp4 permanent_archive/");

            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                new ServerCommand("Restoring Cameras to Motion Detection Mode").execute("./end_splitStream.sh");
            }
        });

        builder.show();

    }

    private class ServerCommand extends AsyncTask<String, String, String> {

        String result = "";

        private ProgressDialog dialog = new ProgressDialog(splitStream.this);

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
            startActivity(new Intent(splitStream.this,StatusCheck.class));
        }
    }

}