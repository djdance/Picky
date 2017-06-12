package ru.orgcom.picky;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.flexbox.FlexboxLayout;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

public class ShowMeActivity extends AppCompatActivity implements View.OnClickListener {
    String TAG = "djd";
    SharedPreferences prefs;
    public static SQLiteDatabase db;
    int dispWidth, dispHeight;
    float density;

    private boolean mVisible=true, ignoreTaps=true;
    ActionBar actionBar;
    RelativeLayout gameRL;
    com.google.android.flexbox.FlexboxLayout gameFlexLayout;
    int buttonsAmount=4;
    ArrayList<ImageButton> imageButtons=new ArrayList<>();
    int cardId=0;
    SoundDuration soundDuration;
    String kidName="";

    SoundPool mSoundPool=null;
    TextToSpeech tts=null;
    boolean ttsChecked=false,ttsInited=false;
    String reportsfiledir=Environment.getExternalStorageDirectory()+"/pickyreports", reportsFileName="";
    SimpleDateFormat _sdfWatchUID = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS");
    SimpleDateFormat _sdfWatchTime = new SimpleDateFormat("dd.MM.yyyy HH:mm");

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
                final String stacktrace = "ShowMeActivity: " + result.toString();
                Log.d(TAG, "exception: " + stacktrace);
                System.exit(0);
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        setContentView(R.layout.activity_show_me);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        final File pfile = new File(reportsfiledir);//dir+"/doors.db");
        if (!pfile.exists()){
            pfile.mkdir();
            Log.d("djd", "created "+reportsfiledir);
        }

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        dispWidth = metrics.widthPixels;
        dispHeight = metrics.heightPixels;
        density = metrics.scaledDensity;

        gameRL=(RelativeLayout) findViewById(R.id.gameRL);
        gameFlexLayout=(FlexboxLayout) findViewById(R.id.gameFlexboxLayout);

        soundDuration=new SoundDuration(getApplicationContext());

        findViewById(R.id.kidName).clearFocus();
        ((EditText)findViewById(R.id.kidNameEditText)).setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b) {
                    ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(), 0);
                    kidName=((EditText) view).getText().toString();
                    if (kidName.equals("")){
                        mytoast("Пустое имя. Отчёта не будет");
                        return;
                    }
                }
            }
        });
        ((EditText)findViewById(R.id.kidNameEditText)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                textView.clearFocus();
                return true;
            }
        });


        connect2db();
        redrawAll(true);


    }
    @Override
    public void onPause() {
        super.onPause();
        if (!kidName.equals(""))
            saveReport("Пауза в проверке. "+_sdfWatchTime.format(new Date()));
        hide();
    }
    @Override
    public void onResume() {
        super.onResume();
        hide();
    }
    @Override
    protected void onDestroy(){
        //Log.d(TAG,"onDestroy");
        if (ttsInited) {
            tts.stop();
            tts.shutdown();
            ttsInited = false;
        }
        if (db!=null && db.isOpen()){
            db.close();
            db=null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void redrawAll(final boolean withDimmer){
        gameFlexLayout.removeAllViews();
        imageButtons.clear();

        int side= (int) Math.sqrt(dispWidth*dispHeight/buttonsAmount);
        while ((Math.floor(dispWidth/side)*Math.floor(dispHeight/side))<buttonsAmount && side>50){
            //Log.d(TAG,"ShowMeActivity: side="+side+", amounttt="+(Math.floor(dispWidth/side)*Math.floor(dispHeight/side)));
            side--;
        }
        //Log.d(TAG,"ShowMeActivity: finally side="+side+", amounttt="+(Math.floor(dispWidth/side)*Math.floor(dispHeight/side)));

        if (db==null || !db.isOpen()) {
            finish();
            return;
        }
        Cursor c= db.rawQuery("select * from cards", null);
        if (c.getCount() > 0) {
            if (!ttsChecked && !ttsInited && tts==null) {
                //узнаем нужен ли ТТС
                boolean isTTSneeded = false;
                String SsoundAnons = "";
                String SsoundQuestion = "";
                String SsoundRight = "";
                String SsoundWrong = "";
                String pic = "";
                c.moveToFirst();
                do {
                    pic = c.getString(c.getColumnIndex("pic"));
                    SsoundAnons = c.getString(c.getColumnIndex("anons"));
                    SsoundQuestion = c.getString(c.getColumnIndex("question"));
                    SsoundRight = c.getString(c.getColumnIndex("right"));
                    SsoundWrong = c.getString(c.getColumnIndex("wrong"));
                    if (pic!=null && !pic.equals("") && !pic.equals("null") &&
                             ( SsoundAnons==null || SsoundAnons.equals("") || SsoundAnons.equals("null")
                            || SsoundQuestion==null || SsoundQuestion.equals("") || SsoundQuestion.equals("null")
                            || SsoundRight==null || SsoundRight.equals("") || SsoundRight.equals("null")
                            || SsoundWrong==null || SsoundWrong.equals("") || SsoundWrong.equals("null")
                             )
                            ){
                        isTTSneeded=true;
                        break;
                    }
                } while (c.moveToNext());
                ttsChecked=true;
                if (isTTSneeded){
                    Log.d(TAG,"загрузка TTS...");
                    gameFlexLayout.setVisibility(View.GONE);
                    findViewById(R.id.progressBar2).setVisibility(View.VISIBLE);
                    ((TextView)findViewById(R.id.hint)).setText("Загружается синтезированный голос. Ждите несколько секунд...");
                    tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                        @Override
                        public void onInit(final int status) {
                            new Thread(new Runnable() {
                                public void run() {
                                    if (status == TextToSpeech.SUCCESS) {
                                        if (tts.isLanguageAvailable(Locale.getDefault()) < 0) {
                                        } else {
                                            tts.setLanguage(Locale.getDefault());
                                            ttsInited = true;
                                        }
                                    }
                                    Log.d(TAG,"загрузка завершена, ttsInited="+ttsInited);
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            ((TextView)findViewById(R.id.hint)).setText(ttsInited?"Все готово!":"Синтезатор голоса здесь отсутствует :(");
                                            gameFlexLayout.setVisibility(View.VISIBLE);
                                            findViewById(R.id.progressBar2).setVisibility(View.GONE);
                                            redrawAll(withDimmer);
                                        }
                                    });
                                }
                            }).start();// * /
                        }
                    });
                    return;
                }
            }


            int i = 0, tries = 0, triesRnd, randomIndex;
            HashSet<Integer> cardsUsed = new HashSet<>();
            while (i < buttonsAmount && tries < 100) {
                tries++;
                c.moveToFirst();
                triesRnd = 0;
                do {
                    randomIndex = (int) (c.getCount() * Math.random());
                    triesRnd++;
                } while (cardsUsed.contains(randomIndex) && triesRnd < 100);
                for (int ii = 0; ii < randomIndex; ii++)
                    c.moveToNext();
                //Log.d(TAG,"randomI="+randomIndex+" for "+c.getString(c.getColumnIndex("title")));
                ImageButton bu = new ImageButton(this);
                bu.setOnClickListener(this);
                bu.setTag(c.getInt(c.getColumnIndex("id")));
                bu.setBackgroundColor(Color.TRANSPARENT);// dResource(R.drawable.cloudbox_blue);
                bu.setScaleType(ImageView.ScaleType.CENTER_CROP);
                int padd=(int)(15*density);
                bu.setPadding(padd,padd,padd,padd);
                ViewGroup.LayoutParams p1 = new ViewGroup.LayoutParams(side, side);
                bu.setLayoutParams(p1);
                if (c.getString(c.getColumnIndex("pic")) != null && !c.getString(c.getColumnIndex("pic")).equals("")) {
                    try {
                        bu.setImageURI(Uri.parse(c.getString(c.getColumnIndex("pic"))));
                        gameFlexLayout.addView(bu);
                        imageButtons.add(bu);
                        i++;
                        cardsUsed.add(randomIndex);
                        //Log.d(TAG,"tries="+tries+", triesRnd="+triesRnd);
                    } catch (Exception e) {
                        Log.e("djd", "grid " + e);
                    }
                }
            }
        }
        c.close();

        if (withDimmer){
            findViewById(R.id.dimmerRL).setVisibility(View.VISIBLE);
            findViewById(R.id.dimmerButton).setVisibility(View.VISIBLE);
            findViewById(R.id.kidName).setVisibility(View.VISIBLE);
        } else {
            askme();
        }
    }

    void askme(){
        //Log.d(TAG,"askme: imageButtons.size="+imageButtons.size());
        ignoreTaps=true;
        if (imageButtons.size()==0){
            mytoast("Нет ни одной карточки");
            finish();
            return;
        }
        for (int i=0;i<imageButtons.size();i++){
            imageButtons.get(i).clearColorFilter();
        }
        int randI,tries=0;
        do {
            randI=(int) (Math.random()*imageButtons.size());
            tries++;
        } while (imageButtons.get(randI).getAlpha()<1 && tries<100);
        if (tries>=100){
            redrawAll(false);
            return;
        }
        imageButtons.get(randI).setAlpha(0.99f);
        cardId=(int)(imageButtons.get(randI).getTag());
        if (db==null || !db.isOpen()) {
            finish();
            return;
        }
        Cursor c= db.rawQuery("select * from cards where id="+cardId, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            sayIt(0,cardId);
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    ignoreTaps=false;
                }
            }).start();
        }
        c.close();
    }
    long sayIt(int casse, int id){
        //casse: 0 - ask
        //1 - wrong
        //2 - right
        String addon="title", prefix="";
        switch (casse){
            case 0:
                addon="question";
                switch ((int)(Math.random()*5)){
                    case 0:
                        prefix="А теперь покажи, где ";
                        break;
                    case 1:
                        prefix="Найди, где здесь ";
                        break;
                    case 2:
                        prefix="Покажи скорее, где же ";
                        break;
                    case 3:
                        prefix="Внимание! Найди, где ";
                        break;
                    default:
                        prefix="Покажи, где ";
                }
                break;
            case 1:
                addon="wrong";
                switch ((int)(Math.random()*6)){
                    case 0:
                        prefix="Нет, это ";
                        break;
                    case 1:
                        prefix="Не угадал, это ";
                        break;
                    case 2:
                        prefix="Нет, ошибка! Ведь это ";
                        break;
                    case 3:
                        prefix="Ответ неверный. Это ";
                        break;
                    case 4:
                        prefix="Нет-нет, не здесь. Это ";
                        break;
                    default:
                        prefix="Неправильно, это ";
                }
                break;
            case 2:
                addon="right";
                switch ((int)(Math.random()*7)){
                    case 0:
                        prefix="Ура! Угадано! Это ";
                        break;
                    case 1:
                        prefix="Правильно, это ";
                        break;
                    case 2:
                        prefix="Это правильный ответ! Ура! Это ";
                        break;
                    case 3:
                        prefix="Отлично! Это действительно ";
                        break;
                    case 4:
                        prefix="Молодец! Вот здесь ";
                        break;
                    case 5:
                        prefix="Да! Конечно! Это и есть ";
                        break;
                    default:
                        prefix="Совершенно верно, это ";
                }
                break;
        }
        String voiceRec="";
        if (db==null || !db.isOpen()) {
            finish();
            return 0;
        }
        Cursor c= db.rawQuery("select "+addon+",title from cards where id="+id, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            voiceRec=c.getString(c.getColumnIndex(addon));
            prefix+=c.getString(c.getColumnIndex("title"));
        }
        c.close();
        Log.d(TAG,prefix+", "+voiceRec);
        final String finalPrefix = prefix;
        runOnUiThread(new Runnable() {
            public void run() {
                ((TextView)findViewById(R.id.hint)).setText(kidName+": "+finalPrefix);
            }
        });
        if (voiceRec==null || voiceRec.equals("") || voiceRec.equals("null")) {
            new Thread(new Runnable() {
                public void run() {
                    if (tts.isLanguageAvailable(Locale.getDefault()) < 0) {
                    } else
                        tts.speak(finalPrefix, TextToSpeech.QUEUE_FLUSH, null);

                }
            }).start();
            return 3000;
        }else {
            //записанный голос
            if (mSoundPool==null) {
                //mSoundPool.release();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mSoundPool = new SoundPool.Builder().setMaxStreams(10).build();
                } else {
                    mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 1);
                }
                mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                    @Override
                    public void onLoadComplete(SoundPool soundPool, int i, int i1) {
                        soundPool.play(i, 1, 1, 1, 0, 1f);
                    }
                });
            }
            mSoundPool.load(Environment.getExternalStorageDirectory()+"/"+voiceRec, 1);
            return Math.max(2000,soundDuration.getSoundDuration(1,Environment.getExternalStorageDirectory()+"/"+voiceRec));

        }
    }
    void analyze(View view){
        int id=(int)(view.getTag());
        //Log.d(TAG,"Это id "+id);
        if (id!=cardId){
            //wrong
            ((ImageButton)view).setColorFilter(Color.parseColor("#99ff0000"));
            wrongAnimation(view,false,sayIt(1, id));
        } else {
            //right
            sayIt(2, id);
            ((ImageButton)view).setColorFilter(Color.parseColor("#7700ff00"));
            winAnimation(view);
        }
        if (!kidName.equals(""))
            saveReport(getCardTitle(cardId)+"\t"+getCardTitle(id));
    }
    void saveReport(final String s){
        if (s.equals(""))
            return;
        new Thread(new Runnable() {
            public void run() {
                try {
                    Converter cnv=new Converter();
                    if (reportsFileName.equals("")) {
                        //reportsFileName = reportsfiledir + "/" + cnv.convert(kidName,true) + "_" + _sdfWatchUID.format(new Date()) + ".txt";
                        reportsFileName = reportsfiledir + "/" + kidName + "_" + _sdfWatchUID.format(new Date()) + ".txt";
                        Log.d(TAG,"saveReport: to ----> "+reportsFileName );
                    }
                    Log.d(TAG,"saveReport: "+s);
                    final PrintWriter pw = new PrintWriter(new FileWriter(reportsFileName, true));
                    pw.append(s+"\r\n");
                    pw.flush();
                    pw.close();
                } catch (IOException e) {
                    Log.e(TAG,"saveReport "+e);
                    mytoast("ОШИБКА ЗАПИСИ ОТЧЁТА!");
                }
            }
        }).start();
    }
    String getCardTitle(int id){
        String res="";
        Cursor c= db.rawQuery("select * from cards where id="+id, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            res=c.getString(c.getColumnIndex("title"));
        }
        c.close();
        return res;
    }
    void animateMe(final View view){
        AnimatorSet animSetB=new AnimatorSet();
        animSetB.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                analyze(view);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        ObjectAnimator anim1B = ObjectAnimator.ofFloat(view, "scaleY", 1, 0.8f);
        anim1B.setDuration(400);
        anim1B.setRepeatCount(1);
        anim1B.setRepeatMode(ValueAnimator.REVERSE);
        ObjectAnimator anim2B = ObjectAnimator.ofFloat(view, "scaleX", 1, 0.8f);
        anim2B.setDuration(400);
        anim2B.setRepeatCount(1);
        anim2B.setRepeatMode(ValueAnimator.REVERSE);
        animSetB.play(anim1B).with(anim2B);
        animSetB.setInterpolator(new AccelerateDecelerateInterpolator());
        animSetB.start();
    }
    @Override
    public void onClick(View view) {
        if (view.getTag()!=null && !ignoreTaps){
            ignoreTaps=true;
            animateMe(view);
        } else if (view.getId()==R.id.dimmerButton) {
            if (findViewById(R.id.progressBar2).getVisibility()==View.VISIBLE)
                return;
            findViewById(R.id.dimmerRL).setVisibility(View.GONE);
            findViewById(R.id.dimmerButton).setVisibility(View.GONE);
            findViewById(R.id.kidName).setVisibility(View.GONE);
            if (!kidName.equals(""))
                saveReport("Отчёт по испытуемому "+kidName+". "+_sdfWatchTime.format(new Date()));
            askme();
        }
    }
    void winAnimation(View view){
        for (int i=0;i<imageButtons.size();i++){
            if (imageButtons.get(i)!=view) {
                imageButtons.get(i).setColorFilter(Color.parseColor("#990099cc"));
            } else {
                //imageButtons.get(i).bringToFront();
            }
        }
        AnimatorSet animSetB=new AnimatorSet();
        animSetB.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                askme();
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        ObjectAnimator anim1B = ObjectAnimator.ofFloat(view, "scaleY", 1.9f,1);
        anim1B.setDuration(1000);
        anim1B.setRepeatCount(6);
        anim1B.setRepeatMode(ValueAnimator.REVERSE);
        ObjectAnimator anim2B = ObjectAnimator.ofFloat(view, "scaleX", 1.9f,1);
        anim2B.setDuration(1000);
        anim2B.setRepeatCount(6);
        anim2B.setRepeatMode(ValueAnimator.REVERSE);
        animSetB.play(anim1B).with(anim2B);
        animSetB.setInterpolator(new AccelerateDecelerateInterpolator());
        animSetB.start();
    }

    void wrongAnimation(final View view, final boolean finish, long soundDuration){
        AnimatorSet animSetB=new AnimatorSet();
        animSetB.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (!finish){
                    wrongAnimation(view, true,500);
                } else{
                    ignoreTaps = true;
                    sayIt(0, cardId);
                    ignoreTaps = false;
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        ObjectAnimator anim1B = !finish?ObjectAnimator.ofFloat(view, "rotation", -10,10):ObjectAnimator.ofFloat(view, "rotation", 10,0);
        if (!finish) {
            anim1B.setDuration(soundDuration/2-800); //800 - Для бесшовного
            anim1B.setRepeatCount(4);
            anim1B.setRepeatMode(ValueAnimator.REVERSE);
            animSetB.setInterpolator(new AccelerateDecelerateInterpolator());
        } else {
            anim1B.setDuration(soundDuration);
            animSetB.setInterpolator(new DecelerateInterpolator());
        }
        animSetB.play(anim1B);
        animSetB.start();
    }

    ////////////////
    private void hide() {
        // Hide UI first
        actionBar.hide();
        mVisible = false;
        /*
        gameRL.postDelayed(new Runnable() {
            @Override
            public void run() {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        },300);// */
    }

    private void show() {
        // Show the system bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;
        gameRL.postDelayed(new Runnable() {
            @Override
            public void run() {
                actionBar.show();
            }
        },300);
    }

    void connect2db() {
        if (db!=null && db.isOpen())
            return;
        String ppp1 = getDatabasePath("picky.db").getAbsolutePath();//getFilesDir().getAbsolutePath()+"/doors.db";//getDatabasePath("doors.db").getAbsolutePath();//getPath();
        //Log.d(TAG,"getDatabasePath="+ppp1);
        db = SQLiteDatabase.openDatabase(ppp1, null, SQLiteDatabase.OPEN_READWRITE);
        if (!db.isOpen()) {
            mytoast("Файл с карточками сломан и не открывается");
            finish();
        } else {
            //mytoast(true,"Загрузите картинку и запишите звук");
        }
    }

    void mytoast(final String s){
        runOnUiThread(new Runnable() {
            public void run() {
                Toast toast = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }
}
