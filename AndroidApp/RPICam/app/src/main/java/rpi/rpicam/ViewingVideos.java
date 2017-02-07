package rpi.rpicam;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.MediaController;
import android.widget.VideoView;

public class ViewingVideos extends AppCompatActivity {


    private VideoView videoView;
    private int position = 0;
    private MediaController mediaController;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewing_videos);


        videoView = (VideoView) findViewById(R.id.videoView);

        // Set the media controller buttons
        if (mediaController == null) {
            mediaController = new MediaController(ViewingVideos.this);

            // Set the videoView that acts as the anchor for the MediaController.
            mediaController.setAnchorView(videoView);


            // Set MediaController for VideoView
            videoView.setMediaController(mediaController);
        }


        try {
            // ID of video file.
            //int id = this.getRawResIdByName("myvideo");
            videoView.setVideoURI(Uri.parse(getIntent().getExtras().getString("filepath")));

        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }

        videoView.requestFocus();


        // When the video file ready for playback.
        videoView.setOnPreparedListener(new OnPreparedListener() {

            public void onPrepared(MediaPlayer mediaPlayer) {


                videoView.seekTo(position);
                if (position == 0) {
                    videoView.start();
                }

                // When video Screen change size.
                mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                    @Override
                    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {

                        // Re-Set the videoView that acts as the anchor for the MediaController
                        mediaController.setAnchorView(videoView);
                    }
                });
            }
        });

    }

    // When you change direction of phone, this method will be called.
    // It store the state of video (Current position)
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        // Store current position.
        savedInstanceState.putInt("CurrentPosition", videoView.getCurrentPosition());
        videoView.pause();
    }


    // After rotating the phone. This method is called.
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Get saved position.
        position = savedInstanceState.getInt("CurrentPosition");
        videoView.seekTo(position);
    }

}
//public class ViewingVideos extends AppCompatActivity {

    //@Override
    //protected void onCreate(Bundle savedInstanceState) {
        //super.onCreate(savedInstanceState);


        /*Bundle b = getIntent().getExtras();
        //if(b != null) {
        final String path = b.getString("path");
        setContentView(R.layout.activity_video_view);
        Toast.makeText(getBaseContext(),path,Toast.LENGTH_LONG).show();
        /*getWindow().setFormat(PixelFormat.UNKNOWN);
        android.widget.VideoView mVideoView2 = (android.widget.VideoView)findViewById(R.id.videoView1);
        String uriPath2 = path;
        Uri uri2 = Uri.parse(uriPath2);
        mVideoView2.setVideoURI(uri2);
        mVideoView2.requestFocus();
        mVideoView2.start();*/
        //VideoView videoView = (VideoView)findViewById(R.id.VideoView);
        //MediaController mediaController = new MediaController(this);
        // mediaController.setAnchorView(videoView);
        //videoView.setMediaController(mediaController);

        //videoView.setVideoPath(path);

        //videoView.start();

        /*VideoView videoView = (VideoView) findViewById(R.id.videoView);
        //MediaController controller = (MediaController) findViewById(R.id.controller);
        MediaController controller = new MediaController(this);

        controller.setAnchorView(videoView);
        controller.setMediaPlayer(videoView);
        controller.setMediaPlayer(videoView);
        controller.requestFocus();
        videoView.setMediaController(controller);
        videoView.setVisibility(View.VISIBLE);
        videoView.setVideoURI(Uri.parse(getIntent().getExtras().getString("filepath")));
        videoView.requestFocus();
        videoView.start();
        controller.show();
    }
}

*/