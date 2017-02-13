package de.robv.android.xposed.installer.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.installer.XposedApp;

public class InstallApkUtil extends AsyncTask<Void, Void, Integer> {

    private final DownloadsUtil.DownloadInfo info;
    private final Context context;
    private RootUtil mRootUtil;
    private boolean enabled;
    private List<String> output = new LinkedList<String>();
    public InstallApkUtil(Context context, DownloadsUtil.DownloadInfo info) {
        this.context = context;
        this.info = info;

        mRootUtil = new RootUtil();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        SharedPreferences prefs = XposedApp.getPreferences();
        enabled = prefs.getBoolean("install_with_su", false);

        if (enabled)
            NotificationUtil.showModuleInstallingNotification(info.title);
            mRootUtil.startShell();
    }

    @Override
    protected Integer doInBackground(Void... params) {
        int returnCode = 0;
        if (enabled) {
            returnCode = mRootUtil.execute("pm install -r \"" + info.localFilename + "\"", output);
        }
        return returnCode;
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);

        if (enabled) {
            NotificationUtil.cancel(NotificationUtil.NOTIFICATION_MODULE_INSTALLING);
            StringBuilder out = new StringBuilder();
            for (Object o : output) {
                out.append(o.toString());
                out.append("\n");
            }

            Pattern failurePattern = Pattern.compile("(?m)^Failure\\s+\\[(.*?)\\]$");
            Matcher failureMatcher = failurePattern.matcher(out);

            if (result.equals(0)) {
                String title = "Installation Successful";
                String text = info.title + " installed successfully";
                NotificationUtil.showModuleInstallNotification(title, text);
            } else if (failureMatcher.find()) {
                String reason =  failureMatcher.group(1);
                String title = "Installation Failed";
                String text = info.title + " installation failed (" + reason + ")";
                NotificationUtil.showModuleInstallNotification(title, text);
            }
        }

        if (!enabled) {
            Intent installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            installIntent.setDataAndType(Uri.fromFile(new File(info.localFilename)), DownloadsUtil.MIME_TYPE_APK);
            installIntent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.getApplicationInfo().packageName);
            context.startActivity(installIntent);
        }
    }
}
