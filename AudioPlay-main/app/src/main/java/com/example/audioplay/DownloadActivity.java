package com.example.audioplay;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.audioplay.services.DownloadService;



public class DownloadActivity extends AppCompatActivity implements View.OnClickListener {

    private DownloadService.DownloadBinder downloadBinder;

    //创建匿名类来在活动中调用服务提供的各种方法
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            downloadBinder = (DownloadService.DownloadBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        //找到按钮并设置点击监听事件
        Button startDownload = (Button) findViewById(R.id.start_download);
        Button pauseDownload = (Button) findViewById(R.id.pause_download);
        Button cancelDownload = (Button) findViewById(R.id.cancel_download);
        startDownload.setOnClickListener(this);
        pauseDownload.setOnClickListener(this);
        cancelDownload.setOnClickListener(this);
        //启动下载服务
        Intent intent = new Intent(this,DownloadService.class);
        bindService(intent,connection,BIND_AUTO_CREATE);
    }

    //处理按钮点击监听事件
    @Override
    public void onClick(View v) {
        if (downloadBinder == null){
            return;
        }
        int id = v.getId();
        if (id == R.id.start_download) {
            String url = "https://freemusicarchive.org/track/empty-playground/download";
            downloadBinder.startDownload(url, "empty-playground.mp3");
        } else if (id == R.id.pause_download) {
            downloadBinder.pauseDownLoad();
        } else if (id == R.id.cancel_download) {
            downloadBinder.cancelDownload();
        }
    }

    protected void onDestroy(){
        super.onDestroy();
        unbindService(connection);
    }
}
