package com.lazamelezi.soundrecorder.about;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import com.lazamelezi.soundrecorder.R;

import java.util.ArrayList;
import java.util.List;

public class AboutModel {

    private final List<AboutModel> myAboutListModel = new ArrayList<>();
    private String contentID;
    private int icon;
    private String mTitle;
    private Context context;
    private Intent intent;
    private int tintColor;

    public AboutModel(Context context) {
        this.context = context;
    }

    public AboutModel() {
    }

    static boolean isAppInstalled(Context context, String appName) {

        PackageManager packageManager = context.getPackageManager();

        boolean installed = false;

        @SuppressLint("QueryPermissionsNeeded") List<PackageInfo> packageInfo = packageManager.getInstalledPackages(0);

        for (PackageInfo packageInfo1 : packageInfo) {
            if (packageInfo1.packageName.equals(appName)) {
                installed = true;
                break;
            }
        }


        return installed;
    }

    public int getTintColor() {
        return tintColor;
    }

    public void setTintColor(int tintColor) {
        this.tintColor = tintColor;
    }

    public List<AboutModel> getMyAboutListModel() {
        return myAboutListModel;
    }

    public Intent getIntent() {
        return intent;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    public void setContentID(String contentID) {
        this.contentID = contentID;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public void addNewItem(AboutModel model) {
        myAboutListModel.add(model);
    }



    public void addGitHub(String id, String title) {

        AboutModel gitHubElement = new AboutModel();

        gitHubElement.setTitle(title);
        gitHubElement.setIcon(R.drawable.ic_github);
        gitHubElement.setContentID(id);
        gitHubElement.setTintColor(context.getResources().getColor(R.color.gitHubColor));

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse(String.format("https://github.com/%s", id)));

        gitHubElement.setIntent(intent);
        addNewItem(gitHubElement);
    }

    @SuppressLint("QueryPermissionsNeeded")
    public void addEmail(String email, String title) {

        AboutModel emailElement = new AboutModel();
        emailElement.setTitle(title);
        emailElement.setIcon(R.drawable.ic_email);
        emailElement.setTintColor(context.getResources().getColor(R.color.emailColor));

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + email));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            emailElement.setIntent(intent);
        } else {
            Log.e("EmailIntent", "No email client found!");
        }
        addNewItem(emailElement);

    }




    public void addCustomItem(int icon, int youColor, String title) {

        AboutModel customElement = new AboutModel();
        customElement.setTitle(title);
        customElement.setIcon(icon);
        customElement.setTintColor(youColor);

        addNewItem(customElement);

    }

}
