package com.example.audioplay.services;

import android.content.Context;
import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String,Integer,Integer> {

    public static final int TYPE_SUCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;

    private DownloadListener listener;
    private Context context;

    private boolean isCanceled = false;
    private boolean isPaused = false;
    private int lastProgress;
    private long contentLength;
    private String fileName;

    public DownloadTask(DownloadListener listener, String fileName, Context context){
        this.fileName = fileName;
        this.listener = listener;
        this.context = context;
    }

    @Override
    protected Integer doInBackground(String... params) {
        InputStream is = null;
        RandomAccessFile savedFile = null;
        File file = null;
        try{
            long downloadedLength = 0;  //记录已下载的文件长度
            String downloadUrl = params[0];
            File directory = context.getFilesDir();
            file = new File(directory, fileName + ".mp3");
            if (file.exists()){
                downloadedLength = file.length();
            }
            long contentLength = getContentLength(downloadUrl);
            if (contentLength == 0){
                return TYPE_FAILED;
            }else if (contentLength == downloadedLength){
                return TYPE_SUCESS;
            }
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().addHeader("RANGE","bytes=" + downloadedLength + "-").url(downloadUrl).build();
            Response response = client.newCall(request).execute();
            if (response != null){
                is = response.body().byteStream();
                savedFile = new RandomAccessFile(file,"rw");
                savedFile.seek(downloadedLength);
                byte[] b = new byte[1024];
                int total = 0;
                int len;
                while ((len = is.read(b)) != -1){
                    if (isCanceled){
                        return TYPE_CANCELED;
                    }else if (isPaused){
                        return TYPE_PAUSED;
                    }else {
                        total += len;
                        savedFile.write(b,0,len);
                        int progress = (int) ((total + downloadedLength) * 100 / contentLength);
                        publishProgress(progress);
                    }
                }
                response.body().close();
                return TYPE_SUCESS;
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try{
                if (is != null){
                    is.close();
                }
                if (savedFile != null){
                    savedFile.close();
                }
                if (isCanceled && file != null){
                    file.delete();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if (progress > lastProgress){
            listener.onProgress(progress);
            lastProgress = progress;
        }
    }

    @Override
    protected void onPostExecute(Integer status) {
        switch (status){
            case TYPE_SUCESS:
                listener.onSucess();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
                break;
            default:
                break;
        }
    }

    public void pauseDownload(){
        isPaused = true;
    }

    public void cancelDownload(){
        isCanceled = true;
    }

    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(downloadUrl).build();
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful()){
            long contentLength = response.body().contentLength();
            response.close();
            return contentLength;
        }
        return 0;
    }
}
