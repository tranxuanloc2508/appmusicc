package com.example.musicapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.musicapp.Model.SlideAdapter;
import com.example.musicapp.Model.SlideItem;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class PlayerActivity extends AppCompatActivity {

    ViewPager2 viewPager2;
    DatabaseReference mref;
    TextView tv_songname,tv_songArtists;
    boolean play = true;
    ImageView Play,Pause,Prev,Next;
    SeekBar seekBar;
    TextView Pass,Due;
    Handler handler;
    String out,out2,downloadUrl;
    Integer totalTime;

    ProgressDialog progressDialog;

    ImageView Heart,Repeat,Download;
    Integer currentSongIndex =0 ;

    boolean RepeatSong = false;
    ArrayList<String> url = new ArrayList<>();
    ArrayList<String> song = new ArrayList<>();
    ArrayList<String> artists = new ArrayList<>();
    ArrayList<String> cover_image = new ArrayList<>();

    MediaPlayer mediaPlayer;

    List<SlideItem> sildeItems = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        viewPager2 = findViewById(R.id.viewpaperimageslider);

        mref = FirebaseDatabase.getInstance().getReference("songs");//vô thư mục "songs" trên firebase
        tv_songname = (TextView)findViewById(R.id.song);
        tv_songArtists = (TextView)findViewById(R.id.artists);

        seekBar = (SeekBar)findViewById(R.id.see_bar);
        Pass = (TextView)findViewById(R.id.tv_pass);
        Due = (TextView)findViewById(R.id.tv_due);

        handler = new Handler();

        Play = (ImageView)findViewById(R.id.play);
        Pause =(ImageView)findViewById(R.id.pause);
        Prev =(ImageView)findViewById(R.id.prev);
        Next = (ImageView)findViewById(R.id.next);

        Heart =(ImageView)findViewById(R.id.heart);
        Repeat =(ImageView)findViewById(R.id.repeat);
        Download =(ImageView)findViewById(R.id.download);

        mref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot ds: snapshot.getChildren())
                {

                    // add all  vao list
                    cover_image.add(ds.child("cover_image").getValue(String.class));
                    song.add(ds.child("song").getValue(String.class));
                    artists.add(ds.child("artists").getValue(String.class));
                    url.add(ds.child("url").getValue(String.class));
                }
                for(int i =0;i<cover_image.size();i++)
                {
                    // thêm url vào slideitems
                    sildeItems.add(new SlideItem(cover_image.get(i)));
                }

                //here
                viewPager2.setAdapter(new SlideAdapter(sildeItems));
                viewPager2.setClipToPadding(false);
                viewPager2.setClipChildren(false);

                // đặt giới hạn trang
                viewPager2.setOffscreenPageLimit(3);
                viewPager2.getChildAt(0).setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
                //Tạo biến cho trang và đặt lề cho trang tỷ lệ trang
                CompositePageTransformer compositePageTransformer = new CompositePageTransformer();
                compositePageTransformer.addTransformer(new MarginPageTransformer(40));
                compositePageTransformer.addTransformer(new ViewPager2.PageTransformer() {
                    @Override
                    public void transformPage(@NonNull View page, float position) {
                        page.setScaleY(1);
                    }
                });
                viewPager2.setPageTransformer(compositePageTransformer);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);

                init(viewPager2.getCurrentItem());

                currentSongIndex = viewPager2.getCurrentItem();
            }
        });

        Next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //play next song
                currentSongIndex = currentSongIndex +1;
                //next song value
                viewPager2.setCurrentItem(currentSongIndex);
                //goi hàm init cho btn_next
                init(currentSongIndex);
            }
        });
        Prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //play prev song
                currentSongIndex = currentSongIndex -1;
                //prev song value
                viewPager2.setCurrentItem(currentSongIndex);
                //goi hàm init cho prev
                init(currentSongIndex);
            }
        });

        Heart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String like = snapshot.child(String.valueOf(currentSongIndex+1)).child("like").getValue(String.class);
                        if(like.equals("0"))
                        {
                            //nếu mà click dô button heart thì giá trị sẽ từ 0->1 set imgae qa imageView
                            Heart.setImageResource(R.drawable.heart);
                            mref.child(String.valueOf(currentSongIndex+1)).child("like").setValue("1");
                        }
                        else {
                            //ko thích nhạc này từ 1->0
                            Heart.setImageResource(R.drawable.hear2);
                            mref.child(String.valueOf(currentSongIndex+1)).child("like").setValue("0");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });


        //tính năng lặp lại bài hat:: repeat button
        Repeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String repeat = snapshot.child(String.valueOf(currentSongIndex+1)).child("repeat").getValue(String.class);

                        if(repeat.equals("0"))
                        {
                            //nếu mà click dô button heart thì giá trị sẽ từ 0->1 set imgae qa imageView
                            Repeat.setImageResource(R.drawable.ic_baseline_repeat_one);
                            mref.child(String.valueOf(currentSongIndex+1)).child("repeat").setValue("1");
                            RepeatSong = true;
                            RepeatSongs();
                        }
                        else {
                            //ko thích nhạc này từ 1->0
                            Repeat.setImageResource(R.drawable.ic_baseline_repeat_one_24);
                            mref.child(String.valueOf(currentSongIndex+1)).child("repeat").setValue("0");
                            RepeatSong = false;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b)
                {
                    mediaPlayer.seekTo(i*1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


//        Download.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                try {
//                    final File localFile = File.createTempFile("images", "jpg");
//                    mref.getD(localFile).addOnSuccessListener(new OnSuccessListener() {
//                        @Override
//                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
//                            Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
//                            imageView.setImageBitmap(bitmap);
//                            dismissProgressDialog();
//                            showToast("Download successful!");
//                        }
//                    }).addOnFailureListener(new OnFailureListener() {
//                        @Override
//                        public void onFailure(@NonNull Exception exception) {
//                            dismissProgressDialog();
//                            showToast("Download Failed!");
//                        }
//                    });
//                } catch (IOException | IOException e ) {
//                    e.printStackTrace();
//                    Log.e("Main", "IOE Exception");
//                }
//            }
//        });
    }

    private void init(int currentItem) {
        try {
            if(mediaPlayer.isPlaying())
                mediaPlayer.reset();
        }catch (Exception e){}
        //tao play ẩn và pause ẩn đặt flag
        Pause.setVisibility(View.VISIBLE);
        Play.setVisibility(View.INVISIBLE);
        play= true;

        mref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //kieem tra loio~~
                String like = snapshot.child(String.valueOf(currentSongIndex+1)).child("like").getValue(String.class);
                String repeat =snapshot.child(String.valueOf(currentSongIndex+1)).child("repeat").getValue(String.class);
                 if(like.equals("0"))
                 {
                     Heart.setImageResource(R.drawable.hear2);
                 }
                 else
                 {
                     Heart.setImageResource(R.drawable.heart);
                 }
                 if (repeat.equals("0"))
                 {
                     Repeat.setImageResource(R.drawable.ic_baseline_repeat_one_24);
                     RepeatSong = false;
                 }
                 else
                 {
                     Repeat.setImageResource(R.drawable.ic_baseline_repeat_one);
                     RepeatSong = true;
                     
                     RepeatSongs();
                 }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        tv_songname.setText(song.get(currentItem));
        tv_songArtists.setText(artists.get(currentItem));
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(url.get(currentItem));
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mediaPlayer.start();
                    //taats ca chuc nang
                    initializeSeeBar();
                }
            });
        }catch (Exception e){e.printStackTrace();}
    }

    private void RepeatSongs() {
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (RepeatSong)
                {
                    mp.seekTo(0);
                    mp.start();
                }
            }
        });
    }

    private void initializeSeeBar() {
        seekBar.setMax(mediaPlayer.getDuration()/1000);// giới hạn tối đa của bài
        int mCurrentPosition = mediaPlayer.getCurrentPosition()/1000;
        seekBar.setProgress(mCurrentPosition);

        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(mediaPlayer !=null)
                {
                    int mCurrentPosition = mediaPlayer.getCurrentPosition()/1000;
                    seekBar.setProgress(mCurrentPosition);

                    //set pass textview
                    out = String.format("%02d:%02d",seekBar.getProgress()/60,seekBar.getProgress()%60);

                    Pass.setText(out);
                }
                handler.postDelayed(this,1000);
            }
        });
        totalTime = mediaPlayer.getDuration()/1000;
        out2 = String.format("%02d:%02d",totalTime/60,totalTime%60);
        Due.setText(out2);
    }

    public void playpausebutton(View v){

        //sau khi chọn play và pause kiểm tra cờ đầu nếu đúng thì sẽ dừng musci
        if(play)
        {
            play = false;
            Pause.setVisibility(View.INVISIBLE);
            Play.setVisibility(View.VISIBLE);
            mediaPlayer.pause();
        }
        else {
            play = true;
            Pause.setVisibility(View.VISIBLE);
            Play.setVisibility(View.INVISIBLE);
            mediaPlayer.start();
        }
    }
}