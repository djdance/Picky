package ru.orgcom.picky;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.database.Cursor;
import android.net.Uri;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nguyenhoanglam.imagepicker.activity.ImagePicker;
import com.nguyenhoanglam.imagepicker.activity.ImagePickerActivity;
import com.nguyenhoanglam.imagepicker.model.Image;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import omrecorder.AudioChunk;
import omrecorder.AudioSource;
import omrecorder.OmRecorder;
import omrecorder.PullTransport;
import omrecorder.Recorder;
import omrecorder.WriteAction;

public class TheCardActivity extends AppCompatActivity implements OnClickListener, AppBarLayout.OnOffsetChangedListener {
    String TAG = "djd";
    SharedPreferences prefs;
    public static SQLiteDatabase db;
    int dispWidth, dispHeight;
    float density;

    Toolbar toolbar;
    View lastViewClicked =null;
    ProgressBar mProgressView;
    String cardTitle="";
    int cardID=0;
    FloatingActionButton fab;
    CollapsingToolbarLayout collapsingToolbarLayout;
    Recorder recorder=null;
    SoundPool mSoundPool=null;
    int soundAnons,soundQuestion,soundWrong,soundRight;
    String SsoundAnons="";
    String SsoundQuestion="";
    String SsoundRight="";
    String SsoundWrong="";
    long soundLen;
    TextToSpeech tts=null;
    boolean ttsInited=false,utteranceDone=true,soundsChanged=true;
    BroadcastReceiver brTTS=null;
    SoundDuration soundDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                //Log.d(TAG, "exception: " + this.getClass().getName() + ", " + throwable.toString());
                final Writer result = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(result);
                throwable.printStackTrace(printWriter);
                final String stacktrace = "TheCardActivity: " + result.toString();
                Log.d(TAG, "exception: " + stacktrace);
                System.exit(0);
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        setContentView(R.layout.activity_the_card);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        dispWidth = metrics.widthPixels;
        dispHeight = metrics.heightPixels;
        density = metrics.scaledDensity;
        Log.d(TAG, "dispW=" + dispWidth + ",dispH=" + dispHeight + ", scaDens=" + density + ", dens=" + metrics.density);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        ((AppBarLayout)findViewById(R.id.app_bar)).addOnOffsetChangedListener(this);

        cardID = getIntent().getIntExtra("cardID", -1);
        soundDuration=new SoundDuration(getApplicationContext());
        connect2db();

        updateCardInfo();

        fab = (FloatingActionButton) findViewById(R.id.fabEdit);
        fab.setOnClickListener(this);
        //((AppBarLayout)findViewById(R.id.app_bar)).setExpanded(false, true); // second one for animation
        ((EditText)findViewById(R.id.cardTitleEditText)).setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b) {
                    fab.setImageResource(R.drawable.ic_mode_edit);
                    ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(), 0);
                    String newCardTitle=((EditText) view).getText().toString();
                    if (newCardTitle.equals("")){
                        mytoast(false,"У вас пустое название!");
                        return;
                    }
                    try {
                        db.execSQL("update cards set title=\""+newCardTitle+"\" where id="+cardID);
                        cardTitle = newCardTitle;
                        collapsingToolbarLayout.setTitle(cardTitle+" ");
                        if (findViewById(R.id.cardTitleTextInputLayout).getVisibility()!=View.GONE)
                            findViewById(R.id.cardTitleTextInputLayout).setVisibility(View.GONE);
                        soundsChanged=true;
                        updateCardInfo();
                    } catch (Exception e) {
                        mytoast(true,"Неподходящие символы в названии!");
                    }

                }
            }
        });
        ((EditText)findViewById(R.id.cardTitleEditText)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    TransitionManager.beginDelayedTransition((ViewGroup) findViewById(android.R.id.content), new Fade());
                }
                findViewById(R.id.cardTitleTextInputLayout).setVisibility(View.GONE);
                return true;
            }
        });

        //mTitle.setError(null);
        //showProgress(true);
    }
    void updateCardInfo(){
        if (soundsChanged) {
            soundAnons = 0;
            soundQuestion = 0;
            soundRight = 0;
            soundWrong = 0;
            SsoundAnons = "";
            SsoundQuestion = "";
            SsoundRight = "";
            SsoundWrong = "";
        }

        connect2db();
        Cursor c = db.rawQuery("select * from cards where id="+cardID, null);
        if (c.getCount() > 0){
            c.moveToFirst();

            cardTitle=c.getString(c.getColumnIndex("title"));
            collapsingToolbarLayout.setTitle(cardTitle+" ");
            if (c.getString(c.getColumnIndex("pic"))!=null && !c.getString(c.getColumnIndex("pic")).equals(""))
                try {
                    ((ImageView) findViewById(R.id.ivProductImage)).setImageURI(Uri.parse(c.getString(c.getColumnIndex("pic"))));
                    ((ImageView) findViewById(R.id.ivProductImageFull)).setImageURI(Uri.parse(c.getString(c.getColumnIndex("pic"))));
                } catch (Exception e) {
                    //Log.e("djd",""+e);
                }
            else {
                ((ImageView) findViewById(R.id.ivProductImage)).setImageResource(R.drawable.ico);
                ((ImageView) findViewById(R.id.ivProductImageFull)).setImageResource(R.drawable.ico);
            }

            if (soundsChanged) {
                soundsChanged=false;
                if (mSoundPool != null)
                    mSoundPool.release();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mSoundPool = new SoundPool.Builder().setMaxStreams(10).build();
                } else {
                    mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 1);
                }
            /*mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                public void onLoadComplete(final SoundPool soundPool,  int sampleId, int status) {
                    Log.d(TAG,"soundPool loaded "+sampleId+", status="+status);
                }
            });*/
                SsoundAnons = c.getString(c.getColumnIndex("anons"));
                SsoundQuestion = c.getString(c.getColumnIndex("question"));
                SsoundRight = c.getString(c.getColumnIndex("right"));
                SsoundWrong = c.getString(c.getColumnIndex("wrong"));
                //Log.d(TAG,"load sounds: "+SsoundAnons+","+SsoundQuestion+","+SsoundRight+","+SsoundWrong);
                try {
                    if (SsoundAnons != null && !SsoundAnons.equals("") && !SsoundAnons.equals("null")) {
                        //Log.d(TAG,"load anons sound "+SsoundAnons);
                        soundAnons = mSoundPool.load(Environment.getExternalStorageDirectory() + "/" + SsoundAnons, 1);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "load sound1 " + e);
                }
                try {
                    if (SsoundQuestion != null && !SsoundQuestion.equals("") && !SsoundQuestion.equals("null")) {
                        //Log.d(TAG,"load q sound "+SsoundQuestion);
                        soundQuestion = mSoundPool.load(Environment.getExternalStorageDirectory() + "/" + SsoundQuestion, 1);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "load sound2 " + e);
                }
                try {
                    if (SsoundRight != null && !SsoundRight.equals("") && !SsoundRight.equals("null")) {
                        //Log.d(TAG,"load right sound "+SsoundRight);
                        soundRight = mSoundPool.load(Environment.getExternalStorageDirectory() + "/" + SsoundRight, 1);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "load sound3 " + e);
                }
                try {
                    if (SsoundWrong != null && !SsoundWrong.equals("") && !SsoundWrong.equals("null")) {
                        //Log.d(TAG,"load wrong sound "+SsoundWrong);
                        soundWrong = mSoundPool.load(Environment.getExternalStorageDirectory() + "/" + SsoundWrong, 1);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "load sound4 " + e);
                }
            }
            if (!ttsInited && tts==null){
                initTTS();
            }
        } else {
            Toast toast= Toast.makeText(getApplicationContext(), "Ошибка, нет такой карточки", Toast.LENGTH_SHORT);
            toast.show();
            Log.d(TAG,"нет карточки "+cardID);
            supportFinishAfterTransition();
        }
        c.close();

        findViewById(R.id.recButton1).setEnabled(recorder==null && utteranceDone);
        findViewById(R.id.recButton2).setEnabled(recorder==null && utteranceDone);
        findViewById(R.id.recButton3).setEnabled(recorder==null && utteranceDone);
        findViewById(R.id.recButton4).setEnabled(recorder==null && utteranceDone);
        if (recorder==null) {
            ((ImageView) findViewById(R.id.recButton1)).clearColorFilter();
            ((ImageView) findViewById(R.id.recButton2)).clearColorFilter();
            ((ImageView) findViewById(R.id.recButton3)).clearColorFilter();
            ((ImageView) findViewById(R.id.recButton4)).clearColorFilter();
            ((ImageView)findViewById(R.id.recButton1)).setImageResource(android.R.drawable.ic_btn_speak_now);
            ((ImageView)findViewById(R.id.recButton2)).setImageResource(android.R.drawable.ic_btn_speak_now);
            ((ImageView)findViewById(R.id.recButton3)).setImageResource(android.R.drawable.ic_btn_speak_now);
            ((ImageView)findViewById(R.id.recButton4)).setImageResource(android.R.drawable.ic_btn_speak_now);
        }else {
            findViewById(R.id.playButton1).setEnabled(false);
            findViewById(R.id.playButton2).setEnabled(false);
            findViewById(R.id.playButton3).setEnabled(false);
            findViewById(R.id.playButton4).setEnabled(false);
            if (lastViewClicked.getId()==R.id.recButton1) {
                ((ImageView) findViewById(R.id.recButton1)).setColorFilter(Color.parseColor("#99ff0000"));
                ((ImageView) findViewById(R.id.recButton1)).setImageResource(android.R.drawable.ic_menu_save);
                findViewById(R.id.recButton1).setEnabled(true);
            }
            if (lastViewClicked.getId()==R.id.recButton2) {
                ((ImageView) findViewById(R.id.recButton2)).setColorFilter(Color.parseColor("#99ff0000"));
                ((ImageView) findViewById(R.id.recButton2)).setImageResource(android.R.drawable.ic_menu_save);
                findViewById(R.id.recButton2).setEnabled(true);
            }
            if (lastViewClicked.getId()==R.id.recButton3) {
                ((ImageView) findViewById(R.id.recButton3)).setColorFilter(Color.parseColor("#99ff0000"));
                ((ImageView) findViewById(R.id.recButton3)).setImageResource(android.R.drawable.ic_menu_save);
                findViewById(R.id.recButton3).setEnabled(true);
            }
            if (lastViewClicked.getId()==R.id.recButton4) {
                ((ImageView) findViewById(R.id.recButton4)).setColorFilter(Color.parseColor("#99ff0000"));
                ((ImageView) findViewById(R.id.recButton4)).setImageResource(android.R.drawable.ic_menu_save);
                findViewById(R.id.recButton4).setEnabled(true);
            }
        }
        findViewById(R.id.playButton1).setEnabled(recorder==null && utteranceDone && ((SsoundAnons!=null && !SsoundAnons.equals("") && !SsoundAnons.equals("null")) || ttsInited));
        findViewById(R.id.playButton2).setEnabled(recorder==null && utteranceDone && ((SsoundQuestion!=null && !SsoundQuestion.equals("") && !SsoundQuestion.equals("null")) || ttsInited));
        findViewById(R.id.playButton3).setEnabled(recorder==null && utteranceDone && ((SsoundWrong!=null && !SsoundWrong.equals("") && !SsoundWrong.equals("null")) || ttsInited));
        findViewById(R.id.playButton4).setEnabled(recorder==null && utteranceDone && ((SsoundRight!=null && !SsoundRight.equals("") && !SsoundRight.equals("null")) || ttsInited));
        if (utteranceDone){
            ((ImageView) findViewById(R.id.playButton1)).clearColorFilter();
            ((ImageView) findViewById(R.id.playButton2)).clearColorFilter();
            ((ImageView) findViewById(R.id.playButton3)).clearColorFilter();
            ((ImageView) findViewById(R.id.playButton4)).clearColorFilter();
        }else{
            if (lastViewClicked.getId()==R.id.playButton1) {
                ((ImageView) findViewById(R.id.playButton1)).setColorFilter(Color.parseColor("#99ff0000"));
            }
            if (lastViewClicked.getId()==R.id.playButton2) {
                ((ImageView) findViewById(R.id.playButton2)).setColorFilter(Color.parseColor("#99ff0000"));
            }
            if (lastViewClicked.getId()==R.id.playButton3) {
                ((ImageView) findViewById(R.id.playButton3)).setColorFilter(Color.parseColor("#99ff0000"));
            }
            if (lastViewClicked.getId()==R.id.playButton4) {
                ((ImageView) findViewById(R.id.playButton4)).setColorFilter(Color.parseColor("#99ff0000"));
            }
        }
        findViewById(R.id.delButton1).setEnabled(SsoundAnons!=null && !SsoundAnons.equals("") && !SsoundAnons.equals("null"));
        findViewById(R.id.delButton2).setEnabled(SsoundQuestion!=null && !SsoundQuestion.equals("") && !SsoundQuestion.equals("null"));
        findViewById(R.id.delButton3).setEnabled(SsoundWrong!=null && !SsoundWrong.equals("") && !SsoundWrong.equals("null"));
        findViewById(R.id.delButton4).setEnabled(SsoundRight!=null && !SsoundRight.equals("") && !SsoundRight.equals("null"));

        collapsingToolbarLayout.requestFocus();
    }


    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        //Log.d(TAG,"onOffsetChanged: verticalOffset="+verticalOffset+", range="+appBarLayout.getTotalScrollRange());
        if (Math.abs(verticalOffset)>appBarLayout.getTotalScrollRange()*0.3) {
            //  Collapsing
        } else {
        }
    }
    @Override
    protected void onDestroy(){
        //Log.d(TAG,"onDestroy");
        if (ttsInited) {
            tts.stop();
            tts.shutdown();
            if (brTTS!= null) {
                unregisterReceiver(brTTS);
                brTTS=null;
            }
            ttsInited = false;
        }
        if (db!=null && db.isOpen()){
            //Log.d(TAG, "main closes db");
            //postlog.post("main closes db...");
            db.close();
        }
        super.onDestroy();
    }
    @Override
    public void onBackPressed() {
        supportFinishAfterTransition();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.cards, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                supportFinishAfterTransition();
                return true;
            case R.id.action_delete:
                new AlertDialog.Builder(TheCardActivity.this)
                        .setTitle("Удалить карточку?")
                        .setMessage("Файл с картинкой и статистика сохранятся. Звук придётся перезаписывать")
                        .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                db.execSQL("delete from cards where id="+cardID);
                                Toast toast= Toast.makeText(getApplicationContext(), "Удалено", Toast.LENGTH_SHORT);
                                toast.show();
                                supportFinishAfterTransition();
                            }
                        })
                        .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        })
                        .setCancelable(true)
                        .show();
                break;
            case R.id.action_edit_title:
                updateFAB(false);
                break;
            case R.id.action_change_picture:
                changePic();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(500).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    void connect2db() {
        if (db!=null && db.isOpen())
            return;
        String ppp1 = getDatabasePath("picky.db").getAbsolutePath();//getFilesDir().getAbsolutePath()+"/doors.db";//getDatabasePath("doors.db").getAbsolutePath();//getPath();
        //Log.d(TAG,"getDatabasePath="+ppp1);
        db = SQLiteDatabase.openDatabase(ppp1, null, SQLiteDatabase.OPEN_READWRITE);
        if (!db.isOpen()) {
            mytoast(true,"Файл с данными не открывается");
            supportFinishAfterTransition();
        } else {
            //mytoast(true,"Загрузите картинку и запишите звук");
        }
    }
    void mytoast(final boolean length_long,final String s){
        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.placeSnackBarС)/**/, s, length_long?Snackbar.LENGTH_LONG:Snackbar.LENGTH_SHORT)
                .setAction("Action", null);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(Color.parseColor("#FF4081"));
        snackbar.show(); // Don’t forget to show! // */
    }
    @Override
    public void onRequestPermissionsResult(final int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 50:
            case 51:
                if (lastViewClicked !=null){
                    onClick(lastViewClicked);
                }
                break;
        }
    }

    @Override
    public void onClick(View view) {
        lastViewClicked =view;
        if (view==fab) {
            updateFAB(true);
        } else if (view.getId()==R.id.ivProductImage || view.getId()==R.id.ivProductImageFull) {
            changePic();
        } else if (view.getId()==R.id.recButton1 || view.getId()==R.id.recButton2 || view.getId()==R.id.recButton3 || view.getId()==R.id.recButton4) {
            if (recorder==null) {
                PackageManager pmanager = this.getPackageManager();
                if (!pmanager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)){
                    mytoast(true,"У вас не обнаружено микрофона");
                    return;
                }
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                        Snackbar.make(view, "Без записи приложение будeт озвучивать картинки голосом робота. Разрешить запись вашего голоса?",
                                Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.ok, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        ActivityCompat.requestPermissions(TheCardActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 50);
                                    }
                                })
                                .show();
                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},50);
                    }
                    return;
                }
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        Snackbar.make(view, "Без доступа к памяти не получится сохранить ваш голос. Разрешить запись на карту памяти?",
                                Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.ok, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        ActivityCompat.requestPermissions(TheCardActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 51);
                                    }
                                })
                                .show();
                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},51);
                    }
                    return;
                }

                File folder = new File(Environment.getExternalStorageDirectory() + "/pickyvoice");
                boolean success = true;
                if (!folder.exists()) {
                    success = folder.mkdir();
                }
                if (success) {
                    String addon="";
                    if (view.getId()==R.id.recButton1)
                        addon="anons";
                    else if (view.getId()==R.id.recButton2)
                        addon="question";
                    else if (view.getId()==R.id.recButton3)
                        addon="wrong";
                    else if (view.getId()==R.id.recButton4)
                        addon="right";
                    String s="/pickyvoice/"
                            +cardID
                            +"_"+addon+"_"
                            +(new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_SSS", Locale.getDefault())).format(new Date(System.currentTimeMillis()))
                            +".wav";
                    //Log.d(TAG,"rec: s="+s);
                    File file = new File(Environment.getExternalStorageDirectory(),s);
                    recorder = OmRecorder.wav(
                            new PullTransport.Noise(mic(), new PullTransport.OnAudioChunkPulledListener() {
                                @Override
                                public void onAudioChunkPulled(AudioChunk audioChunk) {
                                    animateVoice(lastViewClicked, (float) (audioChunk.maxAmplitude() / 200.0));
                                }
                            }, new WriteAction.Default(), new Recorder.OnSilenceListener() {
                                @Override
                                public void onSilence(long silenceTime) {
                                    //Log.e(TAG, String.valueOf(silenceTime));
                                    //mytoast(false, "Обрезана тишина " + silenceTime + " ");
                                }
                            }, 500), file);
                    try {
                        recorder.startRecording();
                    } catch (Exception e) {
                        mytoast(true,"Ошибка записи на карту памяти E1");
                        Log.e(TAG,"rec: "+e);
                        recorder=null;
                        updateCardInfo();
                        return;
                    }
                    db.execSQL("update cards set "+addon+"=\""+s+"\" where id="+cardID);
                    Log.d(TAG,"record: "+"update cards set "+addon+"=\""+s+"\" where id="+cardID);
                } else {
                    mytoast(true,"Ошибка записи на карту памяти E2");
                    recorder=null;
                }
                updateCardInfo();
            } else {
                try {
                    recorder.stopRecording();
                    recorder=null;
                } catch (Exception e) {
                    Log.e(TAG,"rec "+e);
                }
                view.postDelayed(new Runnable() {
                    @Override public void run() {
                        animateVoice(lastViewClicked,0);
                        soundsChanged=true;
                        updateCardInfo();
                    }
                },100);

            }
        } else if (view.getId()==R.id.playButton1 || view.getId()==R.id.playButton2 || view.getId()==R.id.playButton3 || view.getId()==R.id.playButton4 ) {
            //Log.d(TAG,"playbutton: soundAnons="+soundAnons);
            if ((view.getId()==R.id.playButton1 && soundAnons==0)
                    || (view.getId()==R.id.playButton2 && soundQuestion==0)
                    || (view.getId()==R.id.playButton3 && soundWrong==0)
                    || (view.getId()==R.id.playButton4 && soundRight==0)
                    ){
                if (tts.isLanguageAvailable(Locale.getDefault()) < 0) {
                    mytoast(false, "Нет синтезатора русской речи!");
                    return;
                }
                utteranceDone = false;
                updateCardInfo();
                new Thread(new Runnable() {
                    public void run() {
                        String s=cardTitle;
                        if (lastViewClicked.getId()==R.id.playButton1)
                            s="Посмотри, это - "+s+"!";
                        else if (lastViewClicked.getId()==R.id.playButton2)
                            s="Покажи, где "+s+"?";
                        else if (lastViewClicked.getId()==R.id.playButton3)
                            s="Неверно, это - "+s+"...";
                        else if (lastViewClicked.getId()==R.id.playButton4)
                            s="Правильно, это - "+s+"!!!";
                        if (tts.isLanguageAvailable(Locale.getDefault()) < 0) {
                            Log.d(TAG,"tts translit: "+(new Converter()).convert(s,false));
                            tts.speak((new Converter()).convert(s,false), TextToSpeech.QUEUE_FLUSH, null);
                        }else
                            tts.speak(s, TextToSpeech.QUEUE_FLUSH, null);

                    }
                }).start();
            }else {
                soundLen=3000;
                if (view.getId()==R.id.playButton1) {
                    soundLen=Math.max(2000,soundDuration.getSoundDuration(1,Environment.getExternalStorageDirectory()+"/"+SsoundAnons));
                    mSoundPool.play(soundAnons, 1, 1, 1, 0, 1f);
                }else if (view.getId()==R.id.playButton2) {
                    soundLen=Math.max(2000,soundDuration.getSoundDuration(1,Environment.getExternalStorageDirectory()+"/"+soundQuestion));
                    mSoundPool.play(soundQuestion, 1, 1, 1, 0, 1f);
                }else if (view.getId()==R.id.playButton3) {
                    soundLen=Math.max(2000,soundDuration.getSoundDuration(1,Environment.getExternalStorageDirectory()+"/"+soundWrong));
                    mSoundPool.play(soundWrong, 1, 1, 1, 0, 1f);
                }else if (view.getId()==R.id.playButton4) {
                    soundLen=Math.max(2000,soundDuration.getSoundDuration(1,Environment.getExternalStorageDirectory()+"/"+soundRight));
                    mSoundPool.play(soundRight, 1, 1, 1, 0, 1f);
                }
                if (soundLen>0){
                    new Thread(new Runnable() {
                        public void run() {
                            utteranceDone = false;
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    //Log.d(TAG,"soundLen="+soundLen);
                                    updateCardInfo();
                                }
                            });
                            try {
                                TimeUnit.MILLISECONDS.sleep(soundLen);
                            } catch (InterruptedException e) {
                                Log.e(TAG,""+e);
                            }
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    utteranceDone = true;
                                    updateCardInfo();
                                    //Log.d(TAG,"soundLen end");
                                }
                            });
                        }
                    }).start();

                }
            }
        } else if (view.getId()==R.id.delButton1 || view.getId()==R.id.delButton2 || view.getId()==R.id.delButton3 || view.getId()==R.id.delButton4) {
            new AlertDialog.Builder(TheCardActivity.this)
                    .setTitle("Удалить голос?")
                    .setMessage("Будет использоваться голос робота")
                    .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (lastViewClicked.getId()==R.id.delButton1)
                                db.execSQL("update cards set anons=\"\" where id="+cardID);
                            else if (lastViewClicked.getId()==R.id.delButton2)
                                db.execSQL("update cards set question=\"\" where id="+cardID);
                            else if (lastViewClicked.getId()==R.id.delButton3)
                                db.execSQL("update cards set wrong=\"\" where id="+cardID);
                            else if (lastViewClicked.getId()==R.id.delButton4)
                                db.execSQL("update cards set right=\"\" where id="+cardID);
                            mytoast(false,"Запись голоса удалена");
                            soundsChanged=true;
                            updateCardInfo();
                        }
                    })
                    .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    })
                    .setCancelable(true)
                    .show();
        } else if (view.getId()==R.id.saveButton){
            finish();//supportFinishAfterTransition();
        }
    }
    private void animateVoice(View view, float maxPeak) {
        view.setScaleX(1 + maxPeak);
        view.setScaleY(1 + maxPeak);
        //view.animate().scaleX(1 + maxPeak).scaleY(1 + maxPeak).setDuration(10).start();
    }
    private AudioSource mic() {
        return new AudioSource.Smart(MediaRecorder.AudioSource.MIC, AudioFormat.ENCODING_PCM_16BIT, AudioFormat.CHANNEL_IN_MONO, 44100);
    }
    void changePic(){
        //https://github.com/nguyenhoanglam/ImagePicker
        ImagePicker.create(TheCardActivity.this)
                .folderMode(true) // folder mode (false by default)
                .folderTitle("Замена картинки: выберите папку") // folder selection title
                .imageTitle("Замена картинки: выберите файл") // image selection title
                .single() // single mode
                //.multi() // multi mode (default mode)
                //.limit(10) // max images can be selected (999 by default)
                .showCamera(true) // show camera or not (true by default)
                .imageDirectory("Camera") // directory name for captured image  ("Camera" folder by default)
                //.origin(images) // original selected images, used in multi mode
                .start(123); // start image picker activity with request code
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 123 && resultCode == RESULT_OK && data != null) {
            ArrayList<Image> images = data.getParcelableArrayListExtra(ImagePickerActivity.INTENT_EXTRA_SELECTED_IMAGES);
            if (images.size()>0){
                String pic=images.get(0).getPath();
                Log.d(TAG,"pic path original="+pic);
                BitmapResizer br=new BitmapResizer();
                pic=br.getResizedPath(pic);
                db.execSQL("update cards set pic=\""+pic+"\" where id="+cardID);
                updateCardInfo();
            }
        }
    }
    void updateFAB(boolean animate){
        if (animate && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Animation rotation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotation.setRepeatCount(0);
            rotation.setDuration(200);
            rotation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    updateFAB(false);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            fab.startAnimation(rotation);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TransitionManager.beginDelayedTransition((ViewGroup) findViewById(android.R.id.content), new Slide());
        }
        if (findViewById(R.id.cardTitleTextInputLayout).getVisibility()==View.VISIBLE){
            findViewById(R.id.cardTitleTextInputLayout).setVisibility(View.GONE);
        } else {
            fab.setImageResource(R.drawable.ic_done_white);
            findViewById(R.id.cardTitleTextInputLayout).setVisibility(View.VISIBLE);
            ((EditText) findViewById(R.id.cardTitleEditText)).setText(cardTitle);
            findViewById(R.id.cardTitleEditText).postDelayed(new Runnable() {
                @Override
                public void run() {
                    findViewById(R.id.cardTitleEditText).requestFocus();
                    findViewById(R.id.cardTitleEditText).dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                    findViewById(R.id.cardTitleEditText).dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
                    ((EditText) findViewById(R.id.cardTitleEditText)).setSelection(((EditText) findViewById(R.id.cardTitleEditText)).getText().length());
                }
            }, 250);
        }
    }


    void initTTS() {
        collapsingToolbarLayout.setTitle(cardTitle+" - загружается синтезатор голоса...");
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    //if (tts.isLanguageAvailable(Locale.getDefault()) < 0) {
                    //    mytoast(false,"Нет синтезатора русской речи!");
                    //} //else {
                        new Thread(new Runnable() {
                            public void run() {
                                if (tts.isLanguageAvailable(Locale.getDefault()) < 0) {
                                    //mytoast(true,"Нет синтезатора русской речи!");
                                    //ttsInited = true;
                                } else {
                                    tts.setLanguage(Locale.getDefault());
                                    ttsInited = true;
                                }
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        updateCardInfo();
                                    }
                                });
                                brTTS = new BroadcastReceiver() {
                                    public void onReceive(Context p1, Intent p2) {
                                        //Log.d(TAG,"utterance! TTS ACTION_TTS_QUEUE_PROCESSING_COMPLETED, p2.getAction()="+p2.getAction()+", ? "+TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED);
                                        if (p2.getAction().equals(TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED) && tts != null) {
                                            utteranceDone = true;
                                            updateCardInfo();
                                        }
                                    }
                                };
                                registerReceiver(brTTS, new IntentFilter(TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED));
                            }
                        }).start();// * /

                    //}
                } else {
                    mytoast(false,"Нет синтезатора речи");
                }
            }
        });
    }

}

