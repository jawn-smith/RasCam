package rpi.rpicam;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Vector;

public class DownstairsOptions extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downstairs_options);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        int color = Color.parseColor("#FFFFFF");
        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(),R.drawable.ic_power);
        drawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY);

        toolbar.setOverflowIcon(drawable);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.poweroff) {
            //return true;
            try {
                shutDown();
            }catch (Exception e){
                Toast.makeText(getBaseContext(),"Could not shut down",Toast.LENGTH_LONG).show();
            }
        }
        if (id == R.id.reboot) {
            try {
                Reboot();
            }catch (Exception e){
                Toast.makeText(getBaseContext(),"Could not reboot",Toast.LENGTH_LONG).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public void streamVideo(View view){
        new startStream("Initializing Stream").execute("./stream_downstairs.sh");

    }

    public void RecordVideo(View view){
        //AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        //String minutes="";
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Record Video");
        builder.setMessage("How many seconds of video would you like to record?");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String seconds = input.getText().toString();
                new RecordAVideo().execute(seconds);
                deleteFile("*mp4");
                for(File f: getFilesDir().listFiles())
                    if(f.getName().endsWith("mp4"))
                        f.delete();

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void TakePhoto(View view){
        //new ServerCommand("Taking Photo").execute("./take_photo_upstairs.sh");
        new TakeAPhoto().execute();
        deleteFile("*jpg");
        for(File f: getFilesDir().listFiles())
            if(f.getName().endsWith("jpg"))
                f.delete();
    }

    public void pauseSystem(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pausing Camera");
        builder.setMessage("For how many minutes would you like to pause the camera?");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String minutes = input.getText().toString();
                new ServerCommand("Pausing Camera").execute("./pause_downstairs.sh " + minutes);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void Reboot(){
        new ServerCommand("Rebooting").execute("./reboot_downstairs.sh");
    }

    public void shutDown(){
        new ServerCommand("Shutting Down").execute("./shutdown_downstairs.sh");
    }

    private class ServerCommand extends AsyncTask<String, String, String> {

        String result="";

        private ProgressDialog dialog = new ProgressDialog(DownstairsOptions.this);

        //@Override
        public ServerCommand(String message){
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
            String command=args[0];
            Log.d("Refreshed token",refreshedToken);
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
                    result+=line;
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
        protected void onPostExecute(String result) {
            //super.onPostExecute(String result);

            if (dialog.isShowing()){
                dialog.dismiss();
            }
            //String refreshedToken = FirebaseInstanceId.getInstance().getToken();
            Toast.makeText(getBaseContext(),result,Toast.LENGTH_LONG).show();
        }


    }

    private class TakeAPhoto extends AsyncTask<String, String, String> {

        private ProgressDialog dialog = new ProgressDialog(DownstairsOptions.this);
        private ProgressDialog dialog2 = new ProgressDialog(DownstairsOptions.this);

        protected void onPreExecute(){
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.setMessage("Taking Photo");
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

                ce.setCommand("./take_photo_downstairs.sh");
                ce.setErrStream(System.err);

                ce.connect();

                while (!ce.isClosed()){
                    Thread.sleep(100);
                    Log.d("Waiting for ","command to complete");
                }

                ce.disconnect();
                s.disconnect();

                //return "user_pic.jpg";


            } catch (Exception e) {
                //Toast.makeText(getBaseContext(),"An error Occurred",Toast.LENGTH_LONG).show();
                return "Could not take photo";
            }
            runOnUiThread(new Runnable(){
                public void run() {
                    if (dialog.isShowing()){
                        dialog.dismiss();
                    }

                    dialog2.show();
                }
            });

            try{
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
                channelSftp.cd("/home/pi/pictures");
                byte[] buffer = new byte[1024];
                BufferedInputStream bis = new BufferedInputStream(channelSftp.get("downstairs_user_pic.jpg"));
                //File newFile = new File(args[0]);

                //get file size
                Vector list = channelSftp.ls("downstairs_user_pic.jpg");
                ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) list.firstElement();
                SftpATTRS attrs = lsEntry.getAttrs();
                double filesize=attrs.getSize();

                // read in the file
                OutputStream os = openFileOutput("downstairs_user_pic.jpg", Context.MODE_WORLD_READABLE);
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
                return "downstairs_user_pic.jpg";
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
                intent.setDataAndType(Uri.parse("file://" + getFilesDir() + "/" + result), "image/*");
                startActivity(intent);
                //startActivityForResult(intent,1);
            }
        }

    }

    private class RecordAVideo extends AsyncTask<String, String, String> {

        private ProgressDialog dialog = new ProgressDialog(DownstairsOptions.this);
        private ProgressDialog dialog2 = new ProgressDialog(DownstairsOptions.this);

        protected void onPreExecute(){
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.setMessage("Recording Video");
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

                ce.setCommand("./record_video_downstairs.sh "+args[0]);
                ce.setErrStream(System.err);

                ce.connect();

                while (!ce.isClosed()){
                    Thread.sleep(100);
                    Log.d("Waiting for ","command to complete");
                }

                ce.disconnect();
                s.disconnect();

                //return "user_pic.jpg";


            } catch (Exception e) {
                //Toast.makeText(getBaseContext(),"An error Occurred",Toast.LENGTH_LONG).show();
                return "Could not record video";
            }
            runOnUiThread(new Runnable(){
                public void run() {
                    if (dialog.isShowing()){
                        dialog.dismiss();
                    }

                    dialog2.show();
                }
            });

            try{
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
                channelSftp.cd("/home/pi/videos");
                byte[] buffer = new byte[1024];
                BufferedInputStream bis = new BufferedInputStream(channelSftp.get("downstairs_user_vid.mp4"));
                //File newFile = new File(args[0]);

                //get file size
                Vector list = channelSftp.ls("downstairs_user_vid.mp4");
                ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) list.firstElement();
                SftpATTRS attrs = lsEntry.getAttrs();
                double filesize=attrs.getSize();

                // read in the file
                OutputStream os = openFileOutput("downstairs_user_vid.mp4", Context.MODE_WORLD_READABLE);
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
                return "downstairs_user_vid.mp4";
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

    private class startStream extends AsyncTask<String, String, String> {

        String result="";

        private ProgressDialog dialog = new ProgressDialog(DownstairsOptions.this);

        //@Override
        public startStream(String message){
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
            String command=args[0];
            Log.d("Refreshed token",refreshedToken);
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

                /*BufferedReader reader = new BufferedReader(new InputStreamReader(ce.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result+=line;
                }*/

                while (! ce.isClosed()){
                    Thread.sleep(500);
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
        protected void onPostExecute(String result) {
            //super.onPostExecute(String result);

            if (dialog.isShowing()){
                dialog.dismiss();
            }
            //String refreshedToken = FirebaseInstanceId.getInstance().getToken();
            Intent intent = new Intent(DownstairsOptions.this,Stream.class);
            Bundle b = new Bundle();
            b.putString("http","http://YourPublicIP:691");
            intent.putExtras(b);
            startActivity(intent);
        }


    }
}
