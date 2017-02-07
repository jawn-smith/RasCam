package rpi.rpicam;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]
        sendNotification(remoteMessage.getData().get("title"),remoteMessage.getData().get("detail"));
    }
    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(String messageTitle,String messageBody) {
        //Intent intent = new Intent(this, StatusCheck.class);
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //PendingIntent pendingIntent = PendingIntent.getActivity(this,0 /* request code */, intent,PendingIntent.FLAG_UPDATE_CURRENT);

        long[] pattern = {500,500,500,500,500};

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        /*NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setContentTitle(messageTitle)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setVibrate(pattern)
                .setLights(Color.WHITE,1000,2000)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);*/

        /*DateFormat dateFormat = new SimpleDateFormat("MMddHHmm");
        Date date = new Date();

        Intent viewIntent = new Intent(this, StatusCheck.class);
        Bundle b = new Bundle();
        b.putString("DateTime", dateFormat.format(date));
        if (messageBody.contains("downstairs")){
            b.putString("Camera","Downstairs");
        }
        if (messageBody.contains("upstairs")){
            b.putString("Camera","Upstairs");
        }
        viewIntent.putExtras(b);*/

        Bundle b = new Bundle();
        b.putBoolean("goToMotion",true);
        Intent viewIntent = new Intent(this, StatusCheck.class);
        viewIntent.setAction(Intent.ACTION_VIEW);
        viewIntent.putExtras(b);
        PendingIntent viewMotion = PendingIntent.getActivity(this,0,viewIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_ic_notification)
                        .setContentTitle(messageTitle)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setVibrate(pattern)
                        .setLights(Color.WHITE,1000,2000)
                        .setContentIntent(viewMotion)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(messageBody));

        int id;
        if (messageTitle.contains("Security")) {
            builder.addAction(R.drawable.ic_stat_eyes, "View Captured Motion", viewMotion);
            id=0;
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(id, builder.build());
        }
        else if (messageTitle.contains("System")){
            id=1;
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(id, builder.build());
        }
        //int id = Integer.parseInt(dateFormat.format(date));
        //int id = 0;
        //NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //notificationManager.notify(id, builder.build());
    }

    private class ServerCommandListPhoto extends AsyncTask<String, String, ArrayList<String>> {

        ArrayList<String> result= new ArrayList<>();
        /*public AsyncResponse delegate = null;
        public ServerCommandListPhoto(AsyncResponse delegate){
            this.delegate = delegate;
        }*/
        private ProgressDialog dialog = new ProgressDialog(MyFirebaseMessagingService.this);

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
                Intent intent = new Intent(MyFirebaseMessagingService.this, MotionDetections.class);
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

