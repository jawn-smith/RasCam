package rpi.rpicam;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Vector;

public class NotifClick extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notif_click);

        Bundle b = getIntent().getExtras();
        final String camera = b.getString("Camera");
        final String timestamp = b.getString("DateTime");

        new ServerCommandListPhoto().execute("ls -lt archive/ | awk '{print $9}' ","Loading Files");

        deleteFile("*mp4");
        for(File f: getFilesDir().listFiles())
            if(f.getName().endsWith("mp4"))
                f.delete();

        //startActivity(new Intent(NotifClick.this, StatusCheck.class));
    }

    private class loadFile extends AsyncTask<String, String, String> {

        private ProgressDialog dialog = new ProgressDialog(NotifClick.this);
        private ProgressDialog dialog2 = new ProgressDialog(NotifClick.this);

        String result="";

        protected void onPreExecute(){
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.setMessage("Locating File");
            dialog.show();

            dialog2.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog2.setIndeterminate(false);
            dialog2.setCancelable(false);
            dialog2.setMessage("Downloading");
            dialog2.setProgress(0);
        }

        @Override
        protected String doInBackground(String... args) {

            getFilesDir();
            getCacheDir();

            try{
                JSch js = new JSch();
                Session s = js.getSession("pi", "YourPublicIP", 69);
                s.setPassword("Have@good1");
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                s.setConfig(config);
                s.connect();

                Channel c = s.openChannel("exec");
                ChannelExec ce = (ChannelExec) c;

                ce.setCommand("./locate_motion.sh " + args[0] + " " + args[1]);
                ce.setErrStream(System.err);

                ce.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(ce.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result+=line;
                }

                while (!ce.isClosed()){
                    Thread.sleep(100);
                    Log.d("Waiting for ","command to complete");
                }

                ce.disconnect();
                s.disconnect();

                //return "user_pic.jpg";

                //final String filename = result;
                if (result.contains("null")){
                    result=result.replace("null","");
                }

            } catch (Exception e) {
                //Toast.makeText(getBaseContext(),"An error Occurred",Toast.LENGTH_LONG).show();
                return "Could not locate file";
            }
            runOnUiThread(new Runnable(){
                public void run() {
                    if (dialog.isShowing()){
                        dialog.dismiss();
                    }
                    Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG).show();
                    dialog2.show();
                }
            });

            try{

                if (result.contains("null")){
                    result=result.replace("null","");
                }

                Session session;
                Channel channel;
                ChannelSftp channelSftp;

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
                channelSftp.cd("/home/pi/archive");
                byte[] buffer = new byte[1024];
                BufferedInputStream bis = new BufferedInputStream(channelSftp.get(result));
                //File newFile = new File(args[0]);

                //get file size
                Vector list = channelSftp.ls(result);
                ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) list.firstElement();
                SftpATTRS attrs = lsEntry.getAttrs();
                double filesize=attrs.getSize();

                // read in the file
                OutputStream os = openFileOutput(result, Context.MODE_WORLD_READABLE);
                BufferedOutputStream bos = new BufferedOutputStream(os);
                int readCount;
                double progress=0.0;
//System.out.println("Getting: " + theLine);
                while( (readCount = bis.read(buffer)) > 0) {
                    //System.out.println("Writing: " );
                    bos.write(buffer, 0, readCount);
                    progress += 1024.0;
                    dialog2.setProgress((int) ((progress/filesize)*100));
                }
                bis.close();
                bos.close();
                os.close();
                return result;
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

            if (dialog.isShowing()){
                dialog.dismiss();
            }

            if (dialog2.isShowing()){
                dialog2.dismiss();
            }

            if (result.contains("Download Failed") || result.contains("Could not")) {
                Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG).show();
            }
            else {

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + getFilesDir() + "/" + result), "video/*");
                startActivity(intent);
                //startActivityForResult(intent,1);
            }
        }

    }


    private class ServerCommandListPhoto extends AsyncTask<String, String, ArrayList<String>> {

        ArrayList<String> result= new ArrayList<>();
        /*public AsyncResponse delegate = null;
        public ServerCommandListPhoto(AsyncResponse delegate){
            this.delegate = delegate;
        }*/
        private ProgressDialog dialog = new ProgressDialog(NotifClick.this);

        //@Override
        protected void onPreExecute(){
            //super.onPreExecute();
            //String message=args[1];
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.setMessage("Loading Files");
            dialog.show();
        }

        @Override
        protected ArrayList<String> doInBackground(String... args) {
            String command=args[0];
            try{
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

                BufferedReader reader = new BufferedReader(new InputStreamReader(ce.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.add(line);
                }

                ce.disconnect();
                s.disconnect();

                return result;

            } catch (Exception e) {
                Toast.makeText(getBaseContext(),"An error Ocurred",Toast.LENGTH_LONG).show();
                return result;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            //super.onPostExecute(String result);

            if (dialog.isShowing()){
                dialog.dismiss();
            }

            if (result != null) {

                //delegate.processFinish(result);
                Intent intent = new Intent(NotifClick.this, MotionDetections.class);
                Bundle b = new Bundle();
                b.putStringArrayList("motion_detections", result);
                intent.putExtras(b); //Put your id to your next Intent
                startActivity(intent);
                //finish();
            }
            else {
                Toast.makeText(getBaseContext(),"No motion detection frames were found on the server.",Toast.LENGTH_LONG).show();
            }
        }


    }

}
