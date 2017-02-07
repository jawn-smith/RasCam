package rpi.rpicam;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import com.google.firebase.iid.FirebaseInstanceId;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;


public class StatusCheck extends AppCompatActivity {

    String dir, listTitle;
    boolean canArchive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle b = getIntent().getExtras();

        if (b != null) {
            Boolean goToMotion = b.getBoolean("goToMotion");

            if (goToMotion) {
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                manager.cancel(0);
                dir="/home/pi/archive";
                canArchive=true;
                listTitle = "Motion Detection Frames";
                new ServerCommandListPhoto().execute("ls -lt " + dir + " | awk '{print $9}' ","Loading Files");
            }
        }

        setContentView(R.layout.activity_status_check);
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
        getMenuInflater().inflate(R.menu.menu_status_check, menu);
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
                Toast.makeText(getBaseContext(),"Could not shut down",Toast.LENGTH_LONG).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed(){
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void ViewStoredVideos(View view) {
        // somehow view the stored videos
        dir="/home/pi/videos";
        canArchive=true;
        listTitle="Videos";
        new ServerCommandListPhoto().execute("ls -lt " + dir + " | awk '{print $9}' ","Loading Files");    }

    public void ViewStoredPhotos(View view) {
        // somehow view the stored photos
        listTitle="Motion Detection Frames";
        dir="/home/pi/archive";
        canArchive=true;
        new ServerCommandListPhoto().execute("ls -lt " + dir + " | awk '{print $9}' ","Loading Files");
    }

    public void ViewPermanentArchive(View view) {
        // somehow view the stored photos
        listTitle="Permanent Archive";
        dir="/home/pi/permanent_archive";
        canArchive=false;
        new ServerCommandListPhoto().execute("ls -lt " + dir + " | awk '{print $9}' ","Loading Files");
    }

    public void streamVideo(View view){

        //new startStream().execute("downstairs");

        new startStream("Initializing Stream").execute("./splitStream.sh");

    }

    public void goUpstairs(View view) {
        startActivity(new Intent(StatusCheck.this, UpstairsOptions.class));

    }

    public void goDownstairs(View view) {
        startActivity(new Intent(StatusCheck.this, DownstairsOptions.class));

    }

    public void WholeSystemStatusCheck(View view) throws Exception {
        // check the status of the whole system
        new ServerCommandToast("Checking Status").execute("./whole_system_status_check.sh");

    }

    public void pauseSystem(View view) throws Exception {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pause System");
        builder.setMessage("For how many minutes would you like to pause the system?");

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
                new ServerCommandToast("Pausing System").execute("./pause_system.sh "+minutes);
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
        new ServerCommandToast("Rebooting").execute("./reboot_system.sh");
    }

    public void shutDown() throws Exception {
        // check the status of the whole system
        new ServerCommandToast("Shutting System Down").execute("./shutdown_system.sh");

    }

    private class ServerCommandToast extends AsyncTask<String, String, String> {

        String result="";

        private ProgressDialog dialog = new ProgressDialog(StatusCheck.this);

        //@Override
        public ServerCommandToast(String message){
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
            if (! result.isEmpty()) {
                Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG).show();
            }
        }


    }

    private class ServerCommandListPhoto extends AsyncTask<String, String, ArrayList<String>> {

    ArrayList<String> result= new ArrayList<>();
    /*public AsyncResponse delegate = null;
    public ServerCommandListPhoto(AsyncResponse delegate){
        this.delegate = delegate;
    }*/
    private ProgressDialog dialog = new ProgressDialog(StatusCheck.this);

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

        if (result.size() > 1) {

            //delegate.processFinish(result);
            Intent intent = new Intent(StatusCheck.this, MotionDetections.class);
            Bundle b = new Bundle();
            b.putStringArrayList("fileList", result);
            b.putString("fileDirectory",dir);
            b.putBoolean("canArchive",canArchive);
            b.putString("Title",listTitle);
            intent.putExtras(b); //Put your id to your next Intent
            startActivity(intent);
            //finish();
        }
        else {
            Toast.makeText(getBaseContext(),"No motion detection frames were found on the server.",Toast.LENGTH_LONG).show();
        }
    }


}

    private class startStream extends AsyncTask<String, String, String> {

        String result="";

        private ProgressDialog dialog = new ProgressDialog(StatusCheck.this);

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
            startActivity(new Intent(StatusCheck.this,splitStream.class));
        }


    }

}

