package launcher.sviete.pl.domlauncherapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class LauncherActivity extends AppCompatActivity {
    private final Handler mHideHandler = new Handler();
    private final Handler mClickHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    static final Integer WRITE_EXTERNAL = 0x1;
    static final Integer READ_EXTERNAL = 0x2;
    boolean doubleClick = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        mContentView = findViewById(R.id.fullscreen_content);
        ImageView imgAisDom = (ImageView) findViewById(R.id.fullscreen_content);

        imgAisDom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        doubleClick = false;
                    }
                };

                if (doubleClick) {
                    if (!isFinishing()){
                        Toast.makeText(LauncherActivity.this, "OK, root installation",
                                Toast.LENGTH_LONG).show();
                    }
                    rootInstallation();
                }else {
                    doubleClick =true;
                    mClickHandler.postDelayed(r, 500);
                }

            }

        });
    }

    private void rootInstallation(){
        appendLog("Checking the permissions...");
        askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL, true);
        askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE, READ_EXTERNAL, true);
        appendLog("copy the bootanimation...");
        copyFileToSdCard("bootanimation.zip");
        appendLog("copy the keylayout...");
        copyFileToSdCard("Generic.kl");
        appendLog("execute the run_as_root...");
        copyScriptToApp("run_as_root.sh");
        runScript("run_as_root.sh");
    }

    private void runScript(String file){
        //
        String pathFull = "/data/data/launcher.sviete.pl.domlauncherapp/" + file;
        Process p;
        appendLog("runScript: " + file);
        try {
            p = Runtime.getRuntime().exec(
                    new String[]{"su", "-c", pathFull}
            );
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                appendLog(e.getMessage());
            }
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                appendLog(s);
            }
            while ((s = stdError.readLine()) != null) {
                appendLog(s);
            }
        }  catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyScriptToApp(String sourceName){
        String pathFull = "/data/data/launcher.sviete.pl.domlauncherapp/" + sourceName;
        AssetManager assetManager = getAssets();
        try{
            InputStream in = assetManager.open(sourceName);
            FileOutputStream out = null;
            out = new FileOutputStream(pathFull);
            byte[] buff = new byte[1024];
            int read = 0;
            try {
                while ((read = in.read(buff)) > 0) {
                    out.write(buff, 0, read);
                }
            } finally {
                in.close();
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        File script = new File(pathFull);
        script.setExecutable(true, false);
    }


    private void copyFileToSdCard(String sourceName){
        AssetManager assetManager = getAssets();
        File SDCardRoot = Environment.getExternalStorageDirectory();
        File file = new File(SDCardRoot, sourceName);

        try{
            InputStream in = assetManager.open(sourceName);
            FileOutputStream out = null;
            out = new FileOutputStream(file);
            byte[] buff = new byte[1024];
            int read = 0;
            try {
                while ((read = in.read(buff)) > 0) {
                    out.write(buff, 0, read);
                }
            } finally {
                in.close();
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        delayedHide();
    }

    @Override
    protected void onStart() {
        super.onStart();
        delayedHide();
    }


    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.postDelayed(mHidePart2Runnable,0);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide() {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, 0);
    }

    // -------------------------------------
    // --- ais dom - installation part  ----
    // -------------------------------------

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // the command from AIS dom is send like
        // am start -n launcher.sviete.pl.domlauncherapp/.LauncherActivity -e command ais-dom-update
        if (getIntent().getStringExtra("command") != null) {
            handleCommandFromAisDom(getIntent().getStringExtra("command"));
        }
    }

    private void appendLog(String text){
        System.out.println(text);
        File SDCardRoot = Environment.getExternalStorageDirectory();
        File logFile = new File(SDCardRoot, "dom_launcher.log");
        if (!logFile.exists()){
            try{
                logFile.createNewFile();
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
        try{
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    void handleCommandFromAisDom(String command) {
        appendLog(command);
        if ("ais-dom-update".equals(command)){
            appendLog("Update ais-dom!");
            doTheUpdate();
        }

        if ("ais-dom-root-installation".equals(command)){
            appendLog("Root installation!");
            rootInstallation();
        }
    }


    private void askForPermission(String permission, Integer requestCode, Boolean info) {
        if (ContextCompat.checkSelfPermission(LauncherActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(LauncherActivity.this, new String[]{permission}, requestCode);
        } else {
            if (info == true) {
                appendLog("" + permission + " is already granted.");
            }
        }
    }


    private void downloadTheUpdate(){
        appendLog("downloadTheUpdate");
        try {
            URL url = new URL("https://www.powiedz.co/ota/android/AisPanelApp.apk");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            int status = urlConnection.getResponseCode();
            appendLog("Connection status : " + status);
            // normally, 3xx is redirect
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP
                        || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_SEE_OTHER){

                    // get redirect url from "location" header field
                    String newUrl = urlConnection.getHeaderField("Location");
                    // open the new connnection again
                    urlConnection = (HttpURLConnection) new URL(newUrl).openConnection();
                    appendLog("Redirect to URL : " + newUrl);
                }
            }
            urlConnection.connect();
            File SDCardRoot = Environment.getExternalStorageDirectory();
            File file = new File(SDCardRoot,"AisPanelApp.apk");
            FileOutputStream fileOutput = new FileOutputStream(file);
            InputStream inputStream = urlConnection.getInputStream();
            int totalSize = urlConnection.getContentLength();
            //int downloadedSize = 0;
            byte[] buffer = new byte[1024];
            int bufferLength = 0;
            appendLog("Total size to download: " + totalSize);
            while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
                fileOutput.write(buffer, 0, bufferLength);
                //downloadedSize += bufferLength;
                //appendLog("Download progress, downloaded: " + downloadedSize + " from: " + totalSize);

            }
            //close the output stream when done
            fileOutput.close();

            // go to the installation
            installTheUpdate();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void installTheUpdate() {
        // install
        appendLog("installTheUpdate");
        Process p;
        try {
            File SDCardRoot = Environment.getExternalStorageDirectory();
            File update = new File(SDCardRoot, "AisPanelApp.apk");

            // run the app updates
            p = Runtime.getRuntime().exec(
                    new String[]{"su","-c","pm install -r " +  update.getAbsolutePath()}
                    );

            try {
                p.waitFor();
                BufferedReader stdInput = new BufferedReader(new
                        InputStreamReader(p.getInputStream()));

                BufferedReader stdError = new BufferedReader(new
                        InputStreamReader(p.getErrorStream()));
                String s = null;
                while ((s = stdInput.readLine()) != null) {
                    appendLog(s);
                }
                while ((s = stdError.readLine()) != null) {
                    appendLog(s);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 2. open the app
            p = Runtime.getRuntime().exec(
                    new String[]{"su","-c", "am start -n pl.sviete.dom/.WelcomeActivity"}
                    );

            try {
                p.waitFor();
                BufferedReader stdInput = new BufferedReader(new
                        InputStreamReader(p.getInputStream()));

                BufferedReader stdError = new BufferedReader(new
                        InputStreamReader(p.getErrorStream()));
                String s = null;
                while ((s = stdInput.readLine()) != null) {
                    appendLog(s);
                }

                while ((s = stdError.readLine()) != null) {
                    appendLog(s);
                }
                if (p.exitValue() != 255) {
                    appendLog("all done..." + p.exitValue());

                }
                else {
                    // TODO Code to run on unsuccessful
                    appendLog("exitValue: " + p.exitValue());
                }
            } catch (InterruptedException e) {
                // TODO Code to run in interrupted exception
                appendLog("InterruptedException: " + e.toString());
            }
        } catch (IOException e) {
            // TODO Code to run in input/output exception
            appendLog("IOException: " + e.toString());
        }
    }

    private void doTheUpdate() {
        appendLog("doTheUpdate");
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    // download
                    downloadTheUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
}
