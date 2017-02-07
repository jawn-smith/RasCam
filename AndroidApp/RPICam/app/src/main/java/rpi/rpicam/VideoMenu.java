package rpi.rpicam;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.util.ArrayList;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import java.io.*;
import java.util.Vector;

public class VideoMenu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_menu);
        Bundle b = getIntent().getExtras();
        //if(b != null) {
        final ArrayList<String> videos = b.getStringArrayList("videos");

        setContentView(R.layout.activity_video_menu);
        if (videos != null) {
            for (int i = 0; i < videos.size() - 1; i++) {
                LinearLayout row = new LinearLayout(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                Button myButton = new Button(this);
                myButton.setText(videos.get(i + 1));
                myButton.setId(i);
                final int k = i + 1;

                myButton.setLayoutParams(params);
                LinearLayout ll = (LinearLayout) findViewById(R.id.videos);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                myButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        //Toast.makeText(view.getContext(), "Downloading ", Toast.LENGTH_SHORT).show();
                        new scp().execute(videos.get(k), videos.get(k));
                        deleteFile("*mp4");
                        for(File f: getFilesDir().listFiles())
                            if(f.getName().endsWith("mp4"))
                                f.delete();
                    }
                });

                if (i == 0) {
                    ll.addView(myButton, params);
                } else {
                    row.addView(myButton, params);
                    ll.addView(row, lp);
                }
            }
        }

    }

    private class scp extends AsyncTask<String, String, String> {

        public ProgressDialog dialog = new ProgressDialog(VideoMenu.this);

        //@Override
        protected void onPreExecute() {
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setProgress(0);
            dialog.setIndeterminate(false);
            dialog.setCancelable(false);
            String message = "Downloading";
            dialog.setMessage(message);
            dialog.show();
        }


        @Override
        protected String doInBackground(String... args) {
            getFilesDir();

            /*try {
                JSch ssh = new JSch();
                Session session = ssh.getSession("pi", "YourPublicIP", 69);
                // Remember that this is just for testing and we need a quick access, you can add an identity and known_hosts file to prevent
                // Man In the Middle attacks
                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);
                session.setPassword("Have@good1");

                session.connect();
                Channel channel = session.openChannel("sftp");
                channel.connect();

                ChannelSftp sftp = (ChannelSftp) channel;

                sftp.cd("/home/pi/videos");
                // If you need to display the progress of the upload, read how to do it in the end of the article

                // use the get method , if you are using android remember to remove "file://" and use only the relative path
                sftp.get("/home/pi/videos/" + args[0], "/storage/emulated/0/" + args[0]);

                Boolean success = true;

                if (success) {
                    // The file has been succesfully downloaded
                }

                channel.disconnect();
                session.disconnect();
                return "Download Successful";
            } catch (JSchException e) {
                return "Download Failed: JSCH Error";
                //System.out.println(e.getMessage().toString());
                //e.printStackTrace();
            } catch (SftpException e) {
                //return "Download Failed: SFTP Error";
                String result=e.getMessage();
                Log.d("Exception Message: ",result);
                Log.e("this went wrong",""+e);
                return result;
                //System.out.println(e.getMessage().toString());
                //e.printStackTrace();
            }
        }*/

            Session session;
            Channel channel;
            ChannelSftp channelSftp;

            try{
                JSch jsch = new JSch();
                session = jsch.getSession("pi","YourPublicIP", 69);
                session.setPassword("Have@good1");
                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);
                session.connect();
                channel = session.openChannel("sftp");
                channel.connect();
                channelSftp = (ChannelSftp)channel;
                channelSftp.cd("/home/pi/videos");
                byte[] buffer = new byte[1024];
                BufferedInputStream bis = new BufferedInputStream(channelSftp.get(args[0]));

                //get file size
                Vector list = channelSftp.ls(args[0]);
                ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) list.firstElement();
                SftpATTRS attrs = lsEntry.getAttrs();
                double filesize=attrs.getSize();

                // read in the file
                OutputStream os = openFileOutput(args[0], Context.MODE_WORLD_READABLE);
                BufferedOutputStream bos = new BufferedOutputStream(os);
                int readCount;
                double progress=0.0;
//System.out.println("Getting: " + theLine);
                while( (readCount = bis.read(buffer)) > 0) {
                    //System.out.println("Writing: " );
                    bos.write(buffer, 0, readCount);
                    progress += 1024.0;
                    dialog.setProgress((int) ((progress/filesize)*100));
                }

                bis.close();
                bos.close();
                return args[0];
            }catch (JSchException ex) {
                return "Download Failed: JSch Exception";
                //logger.error(ServerConstants.SFTP_REFUSED_CONNECTION, ex);
            } catch (SftpException ex) {
                return "Download Failed: Sftp Exception";
                // logger.error(ServerConstants.FILE_DOWNLOAD_FAILED, ex);
            } catch (IOException ex) {
                return "Download Failed: IO Exception";
                //logger.error(ServerConstants.FILE_NOT_FOUND, ex);
            } catch (Exception ex) {
                return "Download Failed: Misc. Exception";
                //logger.error(ServerConstants.ERROR, ex);
            }

        }



       @Override
       protected void onPostExecute(String result) {
           //super.onPostExecute(String result);

           if (dialog.isShowing()){
               dialog.dismiss();
           }
           //String refreshedToken = FirebaseInstanceId.getInstance().getToken();
           Toast.makeText(getBaseContext(),result,Toast.LENGTH_LONG).show();
           Intent intent = new Intent();
           intent.setAction(Intent.ACTION_VIEW);
           intent.setDataAndType(Uri.parse("file://" + getFilesDir() + "/" + result), "video/*");
           startActivity(intent);
           //deleteFile("file://" + getFilesDir() + "/" + result);
       }

    }
}