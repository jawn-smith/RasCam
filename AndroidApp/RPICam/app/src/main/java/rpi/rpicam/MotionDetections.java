package rpi.rpicam;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewDebug;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Vector;

public class MotionDetections extends AppCompatActivity {

    Toolbar toolbar1, toolbar2;
    ArrayList<Model> model;
    ListView listView;
    String dir;
    boolean canArchive;
    VideoView videoView;

    private SwipeRefreshLayout swipeContainer;
    //ArrayAdapter<String> adapter;
    CustomAdapter adapterCB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motion_detections);

        toolbar1 = (Toolbar) findViewById(R.id.toolbar);
        toolbar2 = (Toolbar) findViewById(R.id.toolbar2);

        toolbar2.setVisibility(View.GONE);

        videoView = (VideoView) findViewById(R.id.videoView);

        setSupportActionBar(toolbar1);

        Bundle b = getIntent().getExtras();
        /*if( b == null ) {
            new ServerCommandListPhoto().execute("ls -lt archive/ | awk '{print $9}' ","Loading Files");
        }*/
        setTitle(b.getString("Title"));
        final ArrayList<String> files = b.getStringArrayList("fileList");
        dir = b.getString("fileDirectory");
        canArchive = b.getBoolean("canArchive");

        if (! (files == null)){
            files.remove(0);
        }

        //final ArrayList<String> niceFiles = files;

        model = new ArrayList<Model>(files.size());

        //String modelSize = ViewDebug.IntToString(model.size());

        for (int i=0;i<files.size();i++){
            model.add(new Model(false,false,niceFormat(files.get(i))));
        }

        //final Model[] innerModel = model;

        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new ServerCommandListPhoto().execute("ls -lt " + dir + " | awk '{print $9}' ","Loading Files");
            }
        });

        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_purple);

        // Get ListView object from xml
        listView = (ListView) findViewById(R.id.list);

        //adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,files);
        adapterCB = new CustomAdapter(this,R.layout.row,model);

        // Assign adapter to ListView
        listView.setAdapter(adapterCB);

        // ListView Item Click Listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //Model innerModel = null;

                String itemValue = model.get(position).filename;

                new scp().execute(vidFormat(itemValue));
                deleteFile("*mp4");
                for(File f: getFilesDir().listFiles())
                    if(f.getName().endsWith("mp4"))
                        f.delete();

            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                for (int i=0; i < parent.getCount();i++) {
                    view = parent.getChildAt(i);
                    model.get(i).cbShowing=true;
                    //view.findViewById(R.id.checkBox1).setVisibility(View.VISIBLE);
                }

                adapterCB.notifyDataSetChanged();

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        model.get(position).cbChecked = !model.get(position).cbChecked;

                        ((TextView) findViewById(R.id.numChecked)).setText(howManyChecked());

                        adapterCB.notifyDataSetChanged();

                    }
                });

                toolbar1.setVisibility(View.GONE);
                toolbar2.setVisibility(View.VISIBLE);

                ImageButton deleteButton = (ImageButton) findViewById(R.id.delete);
                ImageButton downloadButton = (ImageButton) findViewById(R.id.download);

                CheckBox selectAll = (CheckBox) findViewById(R.id.selectAll);

                TextView numChecked = (TextView) findViewById(R.id.numChecked);

                numChecked.setText(howManyChecked());

                deleteButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        deleteCheckedFiles(view);
                    }
                });

                if (canArchive) {
                    ImageButton saveButton = (ImageButton) findViewById(R.id.save);
                    saveButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View view) {
                            saveCheckedFiles(view);
                        }
                    });
                }
                else{
                    ImageButton saveButton = (ImageButton) findViewById(R.id.save);
                    saveButton.setVisibility(View.GONE);
                }

                downloadButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        downloadCheckedFiles(view);
                    }
                });

                selectAll.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        if (((CheckBox) view).isChecked()){
                            for (int i=0; i < model.size();i++) {
                                model.get(i).cbChecked = true;
                            }
                        }
                        else{
                            for (int i=0; i < model.size();i++) {
                                model.get(i).cbChecked = false;
                            }
                        }
                        ((TextView) findViewById(R.id.numChecked)).setText(howManyChecked());
                        adapterCB.notifyDataSetChanged();
                    }
                });

                return true;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (model.get(0).cbShowing){
            for (int i=0; i<model.size();i++){
                model.get(i).cbChecked=false;
                model.get(i).cbShowing=false;

            }

            adapterCB.notifyDataSetChanged();

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    //Model innerModel = null;

                    String itemValue = model.get(position).filename;

                    new scp().execute(vidFormat(itemValue));
                    deleteFile("*mp4");
                    for(File f: getFilesDir().listFiles())
                        if(f.getName().endsWith("mp4"))
                            f.delete();

                }
            });

            toolbar1.setVisibility(View.VISIBLE);
            toolbar2.setVisibility(View.GONE);

        }

        else if (videoView.getVisibility()==View.VISIBLE){
            videoView.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }

        else {
            Intent intent = new Intent(MotionDetections.this, StatusCheck.class);
            startActivity(intent);

        }

    }

    String howManyChecked(){
        int checked=0;

        for (int i=0; i < model.size();i++) {
            if (model.get(i).cbChecked){
                checked++;
            }
        }

        return Integer.toString(checked);

    }

    String niceFormat(String filename){
        filename = filename.replaceFirst("-"," ");
        filename = filename.replaceFirst("-"," ");
        filename = filename.replaceFirst("-","/");
        filename = filename.replaceFirst("-","/");
        filename = filename.replaceFirst("-"," ");
        filename = filename.replaceFirst("-",":");
        return filename.substring(0,filename.length()-4);
    }

    String vidFormat(String filename){
        filename = filename.replaceFirst(" ","-");
        filename = filename.replaceFirst(" ","-");
        filename = filename.replaceFirst("/","-");
        filename = filename.replaceFirst("/","-");
        filename = filename.replaceFirst(" ","-");
        filename = filename.replaceFirst(":","-");
        return filename+".mp4";
    }

    public void deleteCheckedFiles(View view){

        String command="cd "+dir+" ; rm ";

        for (int i=model.size()-1; i>=0;i--){
            if (model.get(i).cbChecked){
                command=command + vidFormat(model.get(i).filename)+" ";
                model.remove(i);
            }
        }

        command = command + "; printf 'Files Deleted'";

        new ServerCommandToast("Deleting Files").execute(command);

        adapterCB.notifyDataSetChanged();

        ((TextView) findViewById(R.id.numChecked)).setText(howManyChecked());

        //Toast.makeText(getBaseContext(),command,Toast.LENGTH_LONG).show();
    }

    public void saveCheckedFiles(View view){
        String command="cd "+dir+" ; mv ";

        for (int i=0; i<model.size();i++){
            if (model.get(i).cbChecked){
                command=command + vidFormat(model.get(i).filename)+" ";
                model.remove(i);
            }
        }

        command = command + " ../permanent_archive; printf 'Files Added to Permanent Archive'";

        new ServerCommandToast("Saving Files").execute(command);

        adapterCB.notifyDataSetChanged();

        ((TextView) findViewById(R.id.numChecked)).setText(howManyChecked());

    }

    public void downloadCheckedFiles(View view){
        ArrayList<String> files = new ArrayList<>(model.size());

        for (int i=0; i<model.size();i++){
            if (model.get(i).cbChecked){
                files.add(0,vidFormat(model.get(i).filename));
            }
        }

        new saveLocal().execute(files);

        ((TextView) findViewById(R.id.numChecked)).setText(howManyChecked());

    }

    private class scp extends AsyncTask<String, String, String> {

        private ProgressDialog dialog = new ProgressDialog(MotionDetections.this);

        //@Override
        protected void onPreExecute() {
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setIndeterminate(false);
            dialog.setCancelable(false);
            String message = "Downloading";
            dialog.setMessage(message);
            dialog.setProgress(0);
            //dialog.setMax(100);
            dialog.show();
        }


        @Override
        protected String doInBackground(String... args) {
            getFilesDir();
            getCacheDir();

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
                channelSftp.cd(dir);
                byte[] buffer = new byte[1024];
                BufferedInputStream bis = new BufferedInputStream(channelSftp.get(args[0]));
                //File newFile = new File(args[0]);

                //get file size
                Vector list = channelSftp.ls(args[0]);
                ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) list.firstElement();
                SftpATTRS attrs = lsEntry.getAttrs();
                double filesize=attrs.getSize();

                if (filesize > 1000000){
                    //Send a command to the server to initialize stream of the file

                    //return a flag to onPostExecute that will start the Stream class
                }

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
                os.close();
                return args[0];
                //return "Download Failed: " + Double.toString(filesize);
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

            if (result.contains("Download Failed")){
                Toast.makeText(getBaseContext(),result,Toast.LENGTH_LONG).show();
            }
            else {

                Bundle b = new Bundle();
                b.putString("filepath","file://" + getFilesDir() + "/" + result);

                Intent intent = new Intent(MotionDetections.this,ViewingVideos.class);
                intent.putExtras(b);

                startActivity(intent);

            }
        }

    }

    public class ServerCommandListPhoto extends AsyncTask<String, String, ArrayList<String>> {

        ArrayList<String> result= new ArrayList<>();

        private ProgressDialog dialog = new ProgressDialog(MotionDetections.this);

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
                Toast.makeText(getBaseContext(),"An error Occurred",Toast.LENGTH_LONG).show();
                return result;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            //super.onPostExecute(String result);

            /*if (dialog.isShowing()){
                dialog.dismiss();
            }*/

            result.remove(0);

            //final ArrayList<String> niceFiles = result;

            //model = new Model[result.size()];

            while (model.size() < result.size()){
                model.add(0,null);
            }

            if (model.get(model.size()-1).cbShowing) {

                for (int i = 0; i < result.size(); i++) {
                    //niceFiles.set(i,niceFormat(niceFiles.get(i)));
                    model.set(i, new Model(true, false, niceFormat(result.get(i))));

                }
            }
            else{
                for (int i = 0; i < result.size(); i++) {
                    //niceFiles.set(i,niceFormat(niceFiles.get(i)));
                    model.set(i, new Model(false, false, niceFormat(result.get(i))));

                }
            }

            //adapterCB.clear();

            adapterCB.notifyDataSetChanged();

            //adapterCB.addAll(model);

            swipeContainer.setRefreshing(false);
        }


    }

    private class ServerCommandToast extends AsyncTask<String, String, String> {

        String result="";

        private ProgressDialog dialog = new ProgressDialog(MotionDetections.this);

        //@Override
        public ServerCommandToast(String message){
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.setMessage(message);
            dialog.show();
        }

        @Override
        protected String doInBackground(String... args) {
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

    private class saveLocal extends AsyncTask<ArrayList<String>, ArrayList<String>, ArrayList<String>> {

        private ProgressDialog dialog = new ProgressDialog(MotionDetections.this);

        //@Override
        protected void onPreExecute() {
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setIndeterminate(false);
            dialog.setCancelable(false);
            String message = "Downloading";
            dialog.setMessage(message);
            dialog.setProgress(0);
            //dialog.setMax(100);
            dialog.show();
        }


        @Override
        protected ArrayList<String> doInBackground(ArrayList<String>... args) {
            getFilesDir();
            getCacheDir();

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
                channelSftp.cd(dir);
                byte[] buffer = new byte[1024];
                //File newFile = new File(args[0]);

                //get file size
                double filesize=0;
                for (int i=0;i<args[0].size();i++) {
                    Vector list = channelSftp.ls(args[0].get(i));
                    ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) list.firstElement();
                    SftpATTRS attrs = lsEntry.getAttrs();
                    filesize += attrs.getSize();
                }
                // read in the file
                for (int i=0;i<args[0].size();i++) {
                    BufferedInputStream bis = new BufferedInputStream(channelSftp.get(args[0].get(i)));
                    OutputStream os = openFileOutput(args[0].get(i), Context.MODE_WORLD_READABLE);
                    BufferedOutputStream bos = new BufferedOutputStream(os);
                    int readCount;
                    double progress = 0.0;
//System.out.println("Getting: " + theLine);
                    while ((readCount = bis.read(buffer)) > 0) {
                        //System.out.println("Writing: " );
                        bos.write(buffer, 0, readCount);
                        progress += 1024.0;
                        dialog.setProgress((int) ((progress / filesize) * 100));
                    }
                    bis.close();
                    bos.close();
                    os.close();
                }
                    return args[0];
            }catch (JSchException ex) {
                args[0].set(0,"Download Failed: JSch Exception");
                return args[0];
                //logger.error(ServerConstants.SFTP_REFUSED_CONNECTION, ex);
            } catch (SftpException ex) {
                args[0].set(0,"Download Failed: Sftp Exception");
                return args[0];
                // logger.error(ServerConstants.FILE_DOWNLOAD_FAILED, ex);
            } catch (IOException ex) {
                args[0].set(0,"Download Failed: IO Exception");
                return args[0];
                //logger.error(ServerConstants.FILE_NOT_FOUND, ex);
            } catch (Exception ex) {
                args[0].set(0,"Download Failed: Misc. Exception");
                return args[0];
                //logger.error(ServerConstants.ERROR, ex);
            }

        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            //super.onPostExecute(String result);

            if (dialog.isShowing()){
                dialog.dismiss();
            }

            String[] permissions = {android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE};

            ActivityCompat.requestPermissions(MotionDetections.this,permissions,0);

            //ActivityCompat.requestPermissions(saveLocal.this,new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE});

            if (result.get(0).contains("Download Failed")){
                Toast.makeText(getBaseContext(),result.get(0),Toast.LENGTH_LONG).show();
            }
            else {

                if (isExternalStorageWritable()){
                    for (int i=0; i < result.size();i++){
                        saveFile(result.get(i));
                    }

                    Toast.makeText(getBaseContext(),"Files Saved to Device",Toast.LENGTH_LONG).show();

                }

                else {
                    Toast.makeText(getBaseContext(),"External Storage is not Writable",Toast.LENGTH_LONG).show();
                }

            }
        }

    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private void saveFile(String filename){

        File root = android.os.Environment.getExternalStorageDirectory();
        // See http://stackoverflow.com/questions/3551821/android-write-to-sd-card-folder

        File dir = new File (root.getAbsolutePath());
        dir.mkdirs();
        File file = new File(dir, filename);

        try {
            FileOutputStream f = new FileOutputStream(file);

            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(),"File Not Found" + e.getMessage(),Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Toast.makeText(getBaseContext(),e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }
}
