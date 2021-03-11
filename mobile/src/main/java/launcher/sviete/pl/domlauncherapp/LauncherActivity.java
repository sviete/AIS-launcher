package launcher.sviete.pl.domlauncherapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;


public class LauncherActivity extends AppCompatActivity {
    private final String TAG = LauncherActivity.class.getName();
    private final Handler mHideHandler = new Handler();
    private final Handler mClickHandler = new Handler();
    private View mContentView;
    static private int mClickNo = 0;
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
    private static final int WRITE_EXTERNAL = 0x1;
    boolean doubleClick = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        mContentView = findViewById(R.id.fullscreen_content);
        ImageView imgAisDom = (ImageView) findViewById(R.id.fullscreen_content);
        ImageView imgAisDomTerminal = (ImageView) findViewById(R.id.dom_terminal);
        ImageView imgAisDomFiles = (ImageView) findViewById(R.id.dom_files);
        ImageView imgTvSettings = (ImageView) findViewById(R.id.tv_settings);


        // try to add permission automatically
        if (!isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Log.i(TAG, "automatically add permission WRITE_EXTERNAL_STORAGE");
            // trying to add
            try {
                Process p = Runtime.getRuntime().exec(
                        new String[]{"su", "-c", "pm grant launcher.sviete.pl.domlauncherapp android.permission.WRITE_EXTERNAL_STORAGE"}
                );
                p.waitFor();
                int exitStatus = p.exitValue();
                Log.i(TAG, "automatically add permission WRITE_EXTERNAL_STORAGE return " + exitStatus);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // try to add permission automatically
        if (!isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Log.i(TAG, "automatically add permission READ_EXTERNAL_STORAGE");
            // trying to add
            try {
                Process p = Runtime.getRuntime().exec(
                        new String[]{"su", "-c", "pm grant launcher.sviete.pl.domlauncherapp android.permission.READ_EXTERNAL_STORAGE"}
                );
                p.waitFor();
                int exitStatus = p.exitValue();
                Log.i(TAG, "automatically add permission READ_EXTERNAL_STORAGE return " + exitStatus);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        imgAisDom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    // if we have permission than the files should be on sdcard and we should be able to start the app
                    mClickNo = mClickNo + 1;
                    startDomActivity();
                } else {
                    appendLog("ask for the permission to write on sdcard...");
                    askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL);
                }
            }

        });

        imgAisDomTerminal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startConsoleActivity();
            }

        });


        imgAisDomFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startFilesActivity();
            }

        });

        imgTvSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTvActivity();
            }

        });


        // hide the status bar onCreate
        try {
            Runtime.getRuntime().exec(
                    new String[]{"su","-c", "service call activity 42 s16 com.android.systemui"}
            );
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        // trying to check if file exists or not
        try {
            Process p = Runtime.getRuntime().exec(
                    new String[]{"su", "-c", "ls /data/data/pl.sviete.dom/files/home/AIS/configuration.yaml"}
            );
            p.waitFor();
            int exitStatus = p.exitValue();
            // exitStatus == 0 file exists
            if (exitStatus != 0){
                // exitStatus != 0 file not exists

                // check if we have installation command
                try {
                    Intent intent = getIntent();
                    // the command from AIS dom is send like
                    // am start -n launcher.sviete.pl.domlauncherapp/.LauncherActivity -e command ais-dom-update
                    if (intent.getStringExtra("command") != null) {
                        appendLog("installation command " + intent.getStringExtra("command"));
                    } else {
                        startDomActivity();
                    }
                } catch (Exception e){
                    e.printStackTrace();
                    startDomActivity();
                }
            }
            Log.i(TAG, "configuration.yaml " + exitStatus);
        } catch (Exception e) {
            Log.i(TAG, "configuration.yaml " +  e.toString());
            e.printStackTrace();
        }


    }

    private void startConsoleActivity() {
        Log.d(TAG, "startConsoleActivity Called");
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(new ComponentName("pl.sviete.dom","pl.sviete.termux.app.TermuxActivity"));
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void startDomActivity() {
        Log.d(TAG, "startDomActivity Called");
        // trying to check if ais_setup_wizard_done file exists or not
        if (mClickNo == 0) {
            try {
                Process p = Runtime.getRuntime().exec(
                        new String[]{"su", "-c", "ls /data/data/pl.sviete.dom/ais_setup_wizard_done"}
                );
                p.waitFor();
                int exitStatus = p.exitValue();
                // exitStatus == 0 file exists
                if (exitStatus != 0) {
                    // check if we have the sky setup app
                    Intent aisSkySetupWizardLaunchIntent = getApplicationContext().getPackageManager().getLaunchIntentForPackage("com.mbx.settingsmbox");
                    if (aisSkySetupWizardLaunchIntent != null) {
                        // if "com.mbx.settingsmbox" exists and we don't have a mark file
                        getApplicationContext().startActivity(aisSkySetupWizardLaunchIntent);
                        aisSkySetupWizardLaunchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        Log.i("AIS", "BroadcastReceiver StartActivity aisSkySetupWizardLaunchIntent");
                        return;
                    } else {
                        Log.i("AIS", "BroadcastReceiver no activity aisSkySetupWizardLaunchIntent");
                    }
                }
                Log.i(TAG, "ais_setup_wizard_done " + exitStatus);
            } catch (Exception e) {
                Log.i(TAG, "ais_setup_wizard_done " + e.toString());
                e.printStackTrace();
            }

            // disable settingsmbox app if enabled - this must be done form system app
            try {
                ApplicationInfo ai = getApplicationContext().getPackageManager().getApplicationInfo("com.mbx.settingsmbox", 0);
                if (ai.enabled) {
                    Process p = Runtime.getRuntime().exec(
                            new String[]{"su", "-c", "pm disable com.mbx.settingsmbox"}
                    );
                    p.waitFor();
                    int exitStatus = p.exitValue();
                    Log.i(TAG, "disable the com.mbx.settingsmbox exitStatus: " + exitStatus);
                }
            } catch (Exception e) {
                Log.i(TAG, "pm disable " + e.toString());
                e.printStackTrace();
            }
        }

        try{
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(new ComponentName("pl.sviete.dom","pl.sviete.dom.SplashScreenActivityMenu"));
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void startFilesActivity() {
        Log.d(TAG, "startFilesActivity Called");
        try{
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(new ComponentName("pl.sviete.dom.anexplorer.pro","dev.dworks.apps.anexplorer.DocumentsActivity"));
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void startTvActivity() {
        Log.d(TAG, "startTvActivity Called");
        try{
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(new ComponentName("com.android.tv.settings","com.android.tv.settings.MainSettings"));
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case WRITE_EXTERNAL: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startDomActivity();
                }
            }
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
    }


    @Override
    protected void onResume() {
        super.onResume();
        try {
            Intent intent = getIntent();
            // AIS dom info the command from AIS dom is send like
            // am start -n launcher.sviete.pl.domlauncherapp/.LauncherActivity -e command ais-dom-update
            //
            if (intent.getStringExtra("command") != null) {
                handleCommandFromAisDom(intent.getStringExtra("command"));
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        //
        try {
            ImageView imgAisDom = (ImageView) findViewById(R.id.fullscreen_content);
            imgAisDom.requestFocus();
            imgAisDom.requestFocusFromTouch();
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    private String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
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
            buf.append(getDateTime() + " " + text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    void handleCommandFromAisDom(String command) {
        appendLog("handleCommandFromAisDom: " + command);
        if ("ais-dom-update".equals(command)){
            appendLog("Update ais-dom!");
            doTheUpdate(false, false);
        }

        if ("ais-dom-update-beta".equals(command)){
            appendLog("Update ais-dom beta!");
            doTheUpdate(true, false);
        }

        if ("ais-dom-update-force".equals(command)){
            appendLog("Update ais-dom force!");
            doTheUpdate(false, true);
        }

        if ("ais-dom-update-beta-force".equals(command)){
            appendLog("Update ais-dom beta force!");
            doTheUpdate(true, true);
        }

    }


    private boolean isPermissionGranted(String permission) {
        if (ContextCompat.checkSelfPermission(LauncherActivity.this, permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private void askForPermission(String permission, Integer requestCode) {
        ActivityCompat.requestPermissions(LauncherActivity.this, new String[]{permission}, requestCode);
    }


    private void downloadTheUpdate(boolean beta, boolean force){
        appendLog("downloadTheUpdate");
        try {
            String apkBaseName = "AisPanelApp";
            if (beta) {
                apkBaseName = apkBaseName + "-test";
            }
            if (force) {
                apkBaseName = apkBaseName + "-force";
            }
            URL url = new URL("https://www.powiedz.co/ota/android/" + apkBaseName + ".apk");

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
            }
            //close the output stream when done
            fileOutput.close();

            // go to the installation
            installTheUpdate(force);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void installTheUpdate(boolean force) {
        // install
        appendLog("installTheUpdate force: " + force);
        Process p;
        int exitStatus = 0;

        if (force){
            // force update
            // 1. move apk data first
            try {
                p = Runtime.getRuntime().exec(
                        new String[]{"su", "-c", "mv /data/data/pl.sviete.dom/files  /data/data/launcher.sviete.pl.domlauncherapp/files"}
                );
                p.waitFor();
                exitStatus = p.exitValue();
                appendLog("installTheUpdate force move files folder exitStatus " + exitStatus);
                // exitStatus == 0 all OK
                appendLog("installTheUpdate force move files exitStatus " + exitStatus);

                // move .ais folder
                p = Runtime.getRuntime().exec(
                        new String[]{"su", "-c", "mv /data/data/pl.sviete.dom/.ais  /data/data/launcher.sviete.pl.domlauncherapp/.ais"}
                );
                p.waitFor();
                exitStatus = p.exitValue();
                appendLog("installTheUpdate force move .ais exitStatus " + exitStatus);

            } catch (Exception e) {
                appendLog("move apk data error " +  e.toString());
                e.printStackTrace();
                return;
            }
            // 2. uninstall dom apk
            // remove device admin
            try {
                appendLog("remove device admin");
                p = Runtime.getRuntime().exec(
                        new String[]{"su","-c", "am start -n pl.sviete.dom/.WelcomeActivity -a remove-device-admin"}
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
                        appendLog("remove-device-admin all done..." + p.exitValue());

                    }
                    else {
                        // TODO Code to run on unsuccessful
                        appendLog("remove-device-admin exitValue: " + p.exitValue());
                    }
                } catch (InterruptedException e) {
                    // TODO Code to run in interrupted exception
                    appendLog("remove-device-admin InterruptedException: " + e.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                appendLog("pm clear pl.sviete.dom");
                p = Runtime.getRuntime().exec(
                        new String[]{"su","-c","pm clear pl.sviete.dom"}
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

            } catch (Exception e) {
                appendLog("pm clear pl.sviete.dom " +  e.toString());
                e.printStackTrace();
            }


            try {
                appendLog("wait 5 seconds");
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                appendLog("pm uninstall pl.sviete.dom");
                p = Runtime.getRuntime().exec(
                        new String[]{"su","-c","pm uninstall pl.sviete.dom"}
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

            } catch (Exception e) {
                appendLog("pm uninstall pl.sviete.dom " +  e.toString());
                e.printStackTrace();
            }

            try {
                appendLog("wait 5 seconds");
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                // create new install session
                appendLog("pm install-create session");
                p = Runtime.getRuntime().exec(
                        new String[]{"su","-c","pm install-create"}
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

            } catch (Exception e) {
                appendLog("pm install-create " +  e.toString());
                e.printStackTrace();
            }



        }



        // do the normal installation
        try {
            File SDCardRoot = Environment.getExternalStorageDirectory();
            File update = new File(SDCardRoot, "AisPanelApp.apk");

            // run the app updates
            if (force) {
                appendLog("pm install " + update.getAbsolutePath());
                p = Runtime.getRuntime().exec(
                        new String[]{"su", "-c", "pm install -gf /sdcard/AisPanelApp.apk"}
                );
            } else {
                p = Runtime.getRuntime().exec(
                        new String[]{"su", "-c", "pm install -r " + update.getAbsolutePath()}
                );
            }

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
        } catch (IOException e) {
            // TODO Code to run in input/output exception
            appendLog("IOException: " + e.toString());
        }


        if (force){
            // force update
            // move apk data back
            try {
                p = Runtime.getRuntime().exec(
                        new String[]{"su", "-c", "mv /data/data/launcher.sviete.pl.domlauncherapp/files /data/data/pl.sviete.dom/files"}
                );
                p.waitFor();
                exitStatus = p.exitValue();
                appendLog("installTheUpdate force move files folder back exitStatus " + exitStatus);
                appendLog("move files folder data back " + exitStatus);
            } catch (Exception e) {
                appendLog("move files folder data back " +  e.toString());
                e.printStackTrace();
            }

            try {
                p = Runtime.getRuntime().exec(
                        new String[]{"su", "-c", "mv /data/data/launcher.sviete.pl.domlauncherapp/.ais /data/data/pl.sviete.dom/.ais"}
                );
                p.waitFor();
                exitStatus = p.exitValue();
                appendLog("installTheUpdate force move .ais folder back exitStatus " + exitStatus);
            } catch (Exception e) {
                appendLog("move .ais folder data back " +  e.toString());
                e.printStackTrace();
            }


            try {
                p = Runtime.getRuntime().exec(
                        new String[]{"su", "-c", "chown -R `stat /data/data/pl.sviete.dom -c %u:%g` /data/data/pl.sviete.dom/files"}
                );
                p.waitFor();
                exitStatus = p.exitValue();
                appendLog("installTheUpdate force chmod files folder " + exitStatus);
                // exitStatus == 0 all OK
            } catch (Exception e) {
                appendLog("installTheUpdate force chmod filse folder " +  e.toString());
                e.printStackTrace();
            }

            try {
                p = Runtime.getRuntime().exec(
                        new String[]{"su", "-c", "chown -R `stat /data/data/pl.sviete.dom -c %u:%g` /data/data/pl.sviete.dom/.ais"}
                );
                p.waitFor();
                exitStatus = p.exitValue();
                appendLog("installTheUpdate force chmod .ais folder " + exitStatus);
                // exitStatus == 0 all OK
                appendLog("installTheUpdate force chmod .ais folder " + exitStatus);
            } catch (Exception e) {
                appendLog("installTheUpdate force chmod .ais folder " +  e.toString());
                e.printStackTrace();
            }

        }


        // open the app
        try {
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
            e.printStackTrace();
        }
    }


    private void doTheUpdate(final boolean beta, final boolean force) {
        appendLog("");
        appendLog("");
        appendLog("-----------------------------");
        appendLog("-----------------------------");
        appendLog("doTheUpdate");
        appendLog("-----------------------------");
        appendLog("-----------------------------");

        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    // download
                    downloadTheUpdate(beta, force);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
}
