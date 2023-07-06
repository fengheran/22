package com.example.audioplay;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.audioplay.list.Song;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MusicPlayer extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MusicPlayer";
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private Timer timer;
    private boolean isSeekBarChanging; //互斥变量，防止进度条与定时器冲突。
    SimpleDateFormat format;

    private TextView musicName, musicLength, musicCur;
    private SeekBar seekBar;

    String uri;
    private List<Song> songList;
    private int position;

    Button previous;
    Button play;
    Button next;

    private ObjectAnimator animator; //运用ObjectAnimator实现转动
    private ImageView pic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        //获取到intent传过来的数据
        Intent intent = getIntent();
        songList = (List<Song>) intent.getSerializableExtra("list");
        position = intent.getIntExtra("position", 0);

        format = new SimpleDateFormat("mm:ss");

        //唱片旋转效果
        pic = (ImageView) findViewById(R.id.pic);
        pic.setImageResource(songList.get(position).getPlayUri());
        animator = ObjectAnimator.ofFloat(pic, "rotation", 0f, 360.0f);
        animator.setDuration(100000 / 2);
        animator.setInterpolator(new LinearInterpolator()); //匀速
        animator.setRepeatCount(-1); //设置动画重复次数（-1代表一直转）
        animator.setRepeatMode(ValueAnimator.RESTART); //动画重复模式

        //监听按钮点击事件
        previous = (Button) findViewById(R.id.previous);
        play = (Button) findViewById(R.id.play);
        next = (Button) findViewById(R.id.next);

        previous.setOnClickListener(this);
        play.setOnClickListener(this);
        next.setOnClickListener(this);

        musicName = (TextView) findViewById(R.id.music_name);
        musicLength = (TextView) findViewById(R.id.music_length);
        musicCur = (TextView) findViewById(R.id.music_cur);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new MySeekBar());

        //初始化MediaPlayer
        initMediaPlayer(position);

        //开始播放
        play.setBackgroundResource(R.drawable.pause);
        mediaPlayer.start();
        animator.start();

        //监听播放时回调函数
        timer = new Timer();
        timer.schedule(new TimerTask() {

            Runnable updateUI = new Runnable() {
                @Override
                public void run() {
                    try {
                        musicCur.setText(format.format(mediaPlayer.getCurrentPosition()) + "");
                    } catch (Exception e) {
                        return;
                    }

                }
            };

            @Override
            public void run() {
                if (!isSeekBarChanging) {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    runOnUiThread(updateUI);
                }
            }
        }, 0, 50);
    }

    private void initMediaPlayer(final int position) {
        try {
            mediaPlayer.reset();
            // 在这里，我们将获取文件的名字，然后尝试从内部存储中查找该文件。
            String filename = songList.get(position).getName() + ".mp3";
            String filepath = searchFile(filename);
            if (filepath != null) {
                // 如果找到了文件，那么我们就使用这个文件来初始化MediaPlayer。
                mediaPlayer.setDataSource(filepath);
                mediaPlayer.prepare();
                mediaPlayer.setLooping(true);
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    public void onPrepared(MediaPlayer mp) {
                        seekBar.setMax(mediaPlayer.getDuration());
                        musicLength.setText(format.format(mediaPlayer.getDuration()) + "");
                        musicCur.setText("00:00");
                        musicName.setText(songList.get(position).getName());
                    }
                });
            } else {
                // 如果没有找到文件，那么我们就显示一个错误消息，并且不尝试播放音乐。
                Toast.makeText(MusicPlayer.this, "音乐文件未找到: " + filename, Toast.LENGTH_SHORT).show();
                Toast.makeText(MusicPlayer.this,"请返回上一页面进行下载",Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.previous) {//上一首
            if (position == 0) {
                Toast.makeText(MusicPlayer.this, "已经是第一首了", Toast.LENGTH_SHORT).show();
                return;
            } else {
                position--;
                initMediaPlayer(position);
                play.setBackgroundResource(R.drawable.pause);
                pic.setImageResource(songList.get(position).getPlayUri());
                mediaPlayer.start();
                animator.start();
            }
        } else if (id == R.id.play) {
            if (!mediaPlayer.isPlaying()) {
                //开始播放
                v.setBackgroundResource(R.drawable.pause);
                mediaPlayer.start();
                animator.resume();
            } else {
                //暂停播放
                v.setBackgroundResource(R.drawable.play);
                mediaPlayer.pause();
                animator.pause();
            }
        } else if (id == R.id.next) {//下一首
            if (position == songList.size()) {
                Toast.makeText(MusicPlayer.this, "已经是最后一首了", Toast.LENGTH_SHORT).show();
            } else {
                position++;
                initMediaPlayer(position);
                play.setBackgroundResource(R.drawable.pause);
                pic.setImageResource(songList.get(position).getPlayUri());
                mediaPlayer.start();
                animator.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //活动销毁的时候释放mediaPlayer
        isSeekBarChanging = true;
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

    }

    /*进度条处理*/
    public class MySeekBar implements SeekBar.OnSeekBarChangeListener {

        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
        }

        /*滚动时,应当暂停后台定时器*/
        public void onStartTrackingTouch(SeekBar seekBar) {
            isSeekBarChanging = true;
        }

        /*滑动结束后，重新设置值*/
        public void onStopTrackingTouch(SeekBar seekBar) {
            isSeekBarChanging = false;
            mediaPlayer.seekTo(seekBar.getProgress());
        }
    }

    //搜索文件是否在目录下，成功返回路径
    //搜索文件是否在应用内部存储下，成功返回路径
    private String searchFile(String keyword) {
        String result = null;
        File musicDir = new File(getFilesDir(), "");
        File[] files = musicDir.listFiles();
        for (File file : files) {
            if (file.getName().equals(keyword)) {
                result = file.getPath();
                break;
            }
        }
        return result;
    }


}

