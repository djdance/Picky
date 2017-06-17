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
import android.graphics.Rect;
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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

public class ShowMeActivity extends AppCompatActivity implements View.OnTouchListener,View.OnClickListener {
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
    ArrayList<ImageView> ImageViews=new ArrayList<>();
    int cardId=0;
    SoundDuration soundDuration;
    String kidName="";
    long lastUsed;
    SoundPool mSoundPool=null;
    SoundPool spTapper;
    int rawIdpluck;
    LinearLayout tapper;
    TextToSpeech tts=null;
    boolean ttsChecked=false,ttsInited=false;
    String reportsfiledir=Environment.getExternalStorageDirectory()+"/pickyreports", reportsFileName="";
    SimpleDateFormat _sdfWatchUID = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS");
    SimpleDateFormat _sdfWatchTime = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    boolean touched=false;
    int dummyTries=0;

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
        gameRL.setOnTouchListener(this);
        gameFlexLayout=(FlexboxLayout) findViewById(R.id.gameFlexboxLayout);

        soundDuration=new SoundDuration(getApplicationContext());
        tapper=(LinearLayout) findViewById(R.id.tapper);
        spTapper=new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
        rawIdpluck=spTapper.load(getApplicationContext(), getResources().getIdentifier("pluck", "raw", getPackageName()), 1);

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
        lastUsed=System.currentTimeMillis()+10000;


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
        lastUsed=System.currentTimeMillis()+5000;
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
        ImageViews.clear();

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

        String s="select * from cards where 1=1 ";
        if (!ttsChecked && !ttsInited && tts==null && prefs.getBoolean("allowRobot",true)) {
            //узнаем нужен ли ТТС
            ttsChecked = true;
            Cursor c= db.rawQuery(s, null);
            if (c.getCount() > 0) {
                c.moveToFirst();
                boolean isTTSneeded = false;
                //String SsoundAnons = "";
                String SsoundQuestion = "";
                String SsoundRight = "";
                String SsoundWrong = "";
                String pic = "";
                do {
                    pic = c.getString(c.getColumnIndex("pic"));
                    //SsoundAnons = c.getString(c.getColumnIndex("anons"));
                    SsoundQuestion = c.getString(c.getColumnIndex("question"));
                    SsoundRight = c.getString(c.getColumnIndex("right"));
                    SsoundWrong = c.getString(c.getColumnIndex("wrong"));
                    if (pic != null && !pic.equals("") && !pic.equals("null") &&
                            (/*SsoundAnons == null || SsoundAnons.equals("") || SsoundAnons.equals("null")
                                    ||*/ SsoundQuestion == null || SsoundQuestion.equals("") || SsoundQuestion.equals("null")
                                    || SsoundRight == null || SsoundRight.equals("") || SsoundRight.equals("null")
                                    || SsoundWrong == null || SsoundWrong.equals("") || SsoundWrong.equals("null")
                            )
                            ) {
                        isTTSneeded = true;
                        break;
                    }
                } while (c.moveToNext());
                if (isTTSneeded) {
                    Log.d(TAG, "загрузка TTS...");
                    gameFlexLayout.setVisibility(View.GONE);
                    findViewById(R.id.progressBar2).setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.hint)).setText("Загружается синтезированный голос. Ждите несколько секунд...");
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
                                    Log.d(TAG, "загрузка завершена, ttsInited=" + ttsInited);
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            ((TextView) findViewById(R.id.hint)).setText(ttsInited ? "Все готово!" : "Синтезатор голоса здесь отсутствует :(");
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
            c.close();
        }

        if (tts==null || !ttsInited)
            s+=" and question<>'' and right<>'' and wrong<>''";
        Cursor c= db.rawQuery(s, null);
        if (c.getCount() > 0) {
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
                LayoutInflater inflater = getLayoutInflater();
                View view = inflater.inflate(R.layout.card_imageview_for_inflater, null, false);
                ImageView bu = (ImageView) view.findViewById(R.id.bu);
                //ImageView bu = new ImageView(this);
                //bu.setClickable(false);
                //bu.setOnClickListener(this);
                bu.setTag(c.getInt(c.getColumnIndex("id")));
                //bu.setBackgroundColor(Color.TRANSPARENT);
                //bu.setBackgroundResource(R.drawable.cloudbox_blue);
                RelativeLayout.LayoutParams p1 = new RelativeLayout.LayoutParams((int)(side-5*density), (int)(side-5*density));
                /*int padd=(int)(10*density);
                bu.setPadding(padd,padd,padd,padd);
                p1.setMargins(padd,padd,padd,padd);
                bu.setScaleType(ImageView.ScaleType.CENTER_CROP);
                bu.setAdjustViewBounds(true);
                bu.setCropToPadding(true);// */
                bu.setLayoutParams(p1);
                if (c.getString(c.getColumnIndex("pic")) != null && !c.getString(c.getColumnIndex("pic")).equals("")) {
                    try {
                        bu.setImageURI(Uri.parse(c.getString(c.getColumnIndex("pic"))));
                        gameFlexLayout.addView(bu);
                        ImageViews.add(bu);
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
            askme(false);
        }
    }

    void cleanCards(){
        for (int i = 0; i < ImageViews.size(); i++) {
            ImageViews.get(i).clearColorFilter();
            ImageViews.get(i).setBackgroundColor(Color.TRANSPARENT);
            if (ImageViews.get(i).getAlpha()<=0.8f)
                ImageViews.get(i).setAlpha(ImageViews.get(i).getAlpha()+0.8f);
        }
    }
    void askme(boolean askCurrent){
        //Log.d(TAG,"askme: ImageViews.size="+ImageViews.size());
        ignoreTaps=true;
        if (!askCurrent) {
            if (ImageViews.size() == 0) {
                mytoast("Нет ни одной карточки");
                finish();
                return;
            }
            cleanCards();
            int randI, tries = 0;
            do {
                randI = (int) (Math.random() * ImageViews.size());
                tries++;
            } while (ImageViews.get(randI).getAlpha() < 1 && tries < 100);
            if (tries >= 100) {
                redrawAll(false);
                return;
            }
            ImageViews.get(randI).setAlpha(0.99f);
            cardId = (int) (ImageViews.get(randI).getTag());
        } else {
            if (!prefs.getBoolean("colorhint",false))
                cleanCards();
        }
        /*if (db==null || !db.isOpen()) {
            finish();
            return;
        }
        Cursor c= db.rawQuery("select * from cards where id="+cardId, null);
        if (c.getCount() > 0) {
            c.moveToFirst();*/
            final long askDuration=sayIt(0,cardId);
        ignoreTaps=true;
            new Thread(new Runnable() {
                public void run() {
                    Log.d(TAG,"askme said, wait for "+askDuration);
                    try {
                        Thread.sleep(askDuration);
                    } catch (InterruptedException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    Log.d(TAG,"askme said, waited. Alllow to answer");
                    ignoreTaps=false;
                }
            }).start();
        //}
        //c.close();
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
                        prefix="Нет, это не ";
                        break;
                    case 1:
                        prefix="Не угадал, это не ";
                        break;
                    case 2:
                        prefix="Нет, ошибка! Это не ";
                        break;
                    case 3:
                        prefix="Ответ неверный. Не ";
                        break;
                    case 4:
                        prefix="Нет-нет, не здесь. Это не ";
                        break;
                    default:
                        prefix="Неправильно, это не ";
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
        Log.d(TAG,"sayIt: "+prefix+", "+voiceRec);
        final String finalPrefix = prefix;
        runOnUiThread(new Runnable() {
            public void run() {
                ((TextView)findViewById(R.id.hint)).setText(kidName+(kidName.equals("")?"":": ")+finalPrefix);
            }
        });
        if (voiceRec==null || voiceRec.equals("") || voiceRec.equals("null")) {
            new Thread(new Runnable() {
                public void run() {
                    if (tts==null || !ttsInited || tts.isLanguageAvailable(Locale.getDefault()) < 0) {
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
        ignoreTaps=true;
        if (id!=cardId){
            //wrong
            if (prefs.getBoolean("colorFill",true))
                ((ImageView)view).setColorFilter(Color.parseColor("#99ff0000"));
            ((ImageView)view).setBackgroundResource(R.drawable.cloudbox_red);
            wrongAnimation(view,false,sayIt(1, cardId));
        } else {
            //right
            if (prefs.getBoolean("colorFill",true))
                ((ImageView)view).setColorFilter(Color.parseColor("#7700ff00"));
            ((ImageView)view).setBackgroundResource(R.drawable.cloudbox_green);
            winAnimation(view,sayIt(2, cardId));
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
        analyze(view);
        AnimatorSet animSetB=new AnimatorSet();
        animSetB.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                //analyze(view);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        ObjectAnimator anim1B = ObjectAnimator.ofFloat(view, "scaleY", 1, 0.8f);
        anim1B.setDuration(250);
        anim1B.setRepeatCount(1);
        anim1B.setRepeatMode(ValueAnimator.REVERSE);
        ObjectAnimator anim2B = ObjectAnimator.ofFloat(view, "scaleX", 1, 0.8f);
        anim2B.setDuration(250);
        anim2B.setRepeatCount(1);
        anim2B.setRepeatMode(ValueAnimator.REVERSE);
        animSetB.play(anim1B).with(anim2B);
        animSetB.setInterpolator(new AccelerateDecelerateInterpolator());
        animSetB.start();
    }
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //Log.d(TAG, "onTouchEvent. Count=" + event.getPointerCount()+", view tag="+v.getTag().toString());
        lastUsed=System.currentTimeMillis()+5000;
        float X=-1, Y=-1;
        final int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN: {
                if (!touched){
                    final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                    //final int pointerId = event.getPointerId(pointerIndex);
                    X = event.getX(pointerIndex)+v.getLeft();
                    Y = event.getY(pointerIndex)+v.getTop();
                    //Log.d(TAG,"multi: view "+v.getTag().toString()+", pointer index="+pointerIndex+", pointerId="+pointerId);
                    //Log.d(TAG,"multi size="+event.getSize(pointerIndex));
                    //Log.d(TAG,"multi press="+event.getPressure(pointerIndex));
                    touched=true;
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                //Log.d(TAG,"ACTION_POINTER_UP");
                touched=false;
                break;
            } //*/
            case MotionEvent.ACTION_DOWN: {
                if (!touched){
                    X = event.getX()+v.getLeft();
                    Y = event.getY()+v.getTop();
                    //Log.d(TAG,"view "+v.getTag().toString());
                    //Log.d(TAG,"size="+event.getSize());
                    //Log.d(TAG,"press="+event.getPressure());
                    touched=true;
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                //Log.d(TAG,"ACTION_UP");
                touched=false;
                break;
            } //*/
        }
        if (X==-1 && Y==-1)
            return false;

        Rect r=new Rect();
        final ScaleAnimation up = new ScaleAnimation(0.7f, 1.0f, 0.7f, 1.0f);
        up.setFillAfter(true);
        up.setDuration(50);

        int i;
        for (i=0;i<ImageViews.size();i++){
            ImageViews.get(i).getGlobalVisibleRect(r);
            if (r!=null && r.contains((int) X, (int) Y)) {
                Log.d(TAG,"----------------------------> ImageViews["+i+"] попали");
                break;
            }
        }
        if (ignoreTaps || i<0 || i>=ImageViews.size()){
            dummyTries++;
            Log.d(TAG,"dummyTries="+dummyTries);
            if (dummyTries>20){
                dummyTries=0;
                Log.d(TAG,"dummy RESET НЕ НАЖИМАЙТЕ!!");
            }
            if (!ignoreTaps && prefs.getBoolean("allowPluck",true)) {
                spTapper.play(rawIdpluck, 1, 1, 1, 0, (float) (0.7f + (Math.random() * 0.6f)));
            }
            tap(X, Y);
        } else {
            ignoreTaps=true;
            animateMe(ImageViews.get(i));
            dummyTries=0;
        }

        //try {
        //} catch (Exception e) {
        //    Log.e(TAG, e.getMessage().toString());//e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        //}
        return true;

    }

    @Override
    public void onClick(View view) {
        /*if (view.getTag()!=null && !ignoreTaps){
            ignoreTaps=true;
            animateMe(view);
        } else */if (view.getId()==R.id.dimmerButton) {
            if (findViewById(R.id.progressBar2).getVisibility()==View.VISIBLE)
                return;
            findViewById(R.id.dimmerRL).setVisibility(View.GONE);
            findViewById(R.id.dimmerButton).setVisibility(View.GONE);
            findViewById(R.id.kidName).setVisibility(View.GONE);
            if (!kidName.equals(""))
                saveReport("Отчёт по испытуемому "+kidName+". "+_sdfWatchTime.format(new Date()));
            askme(false);
        }
    }

    public void tap(float X,float Y){
        tapper.setX(X);
        tapper.setY(Y);
        //tapper.setAlpha(0.5f);
        tapper.setVisibility(View.VISIBLE);

        float r= (float) (0.8f+Math.random()*0.4f);
        ObjectAnimator anim1 = ObjectAnimator.ofFloat(tapper, "scaleX", 0.3f,r);
        anim1.setInterpolator(new AnticipateOvershootInterpolator());//DecelerateInterpolator());
        ObjectAnimator anim2 = ObjectAnimator.ofFloat(tapper, "scaleY", 0.3f,r);
        anim2.setInterpolator(new AnticipateOvershootInterpolator());//BounceInterpolator());
        AnimatorSet animSet=new AnimatorSet();
        animSet.addListener(new Animator.AnimatorListener() {
            public void onAnimationStart(Animator animation) {
            }
            public void onAnimationEnd(Animator animation) {
                //tapper.setAlpha(0);
                tapper.setVisibility(View.GONE);
            }

            public void onAnimationCancel(Animator animation) {
            }

            public void onAnimationRepeat(Animator animation) {
            }
        });
        //animSet.play(anim3).after(anim2).after(anim1);
        //animSet.playTogether(anim2,anim1);
        animSet.play(anim2).with(anim1);
        animSet.setDuration(500).start();
    }
    void winAnimation(View view,long soundDuration){
        ignoreTaps=true;
        for (int i=0;i<ImageViews.size();i++){
            if (ImageViews.get(i)!=view) {
                //ImageViews.get(i).setColorFilter(Color.parseColor("#990099cc"));
                ImageViews.get(i).setAlpha(ImageViews.get(i).getAlpha()-0.8f);
            } else {
                //ImageViews.get(i).bringToFront();
            }
        }
        AnimatorSet animSetB=new AnimatorSet();
        animSetB.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                askme(false);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        ObjectAnimator anim1B = ObjectAnimator.ofFloat(view, "scaleY", 1,1.6f);
        anim1B.setDuration(soundDuration/5);
        anim1B.setRepeatCount(5);
        anim1B.setRepeatMode(ValueAnimator.REVERSE);
        ObjectAnimator anim2B = ObjectAnimator.ofFloat(view, "scaleX", 1,1.6f);
        anim2B.setDuration(soundDuration/5);
        anim2B.setRepeatCount(5);
        anim2B.setRepeatMode(ValueAnimator.REVERSE);
        animSetB.setStartDelay(500);
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
                    askme(true);
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        ObjectAnimator anim1B = !finish?ObjectAnimator.ofFloat(view, "translationX", -dispWidth/10,dispWidth/10):ObjectAnimator.ofFloat(view, "translationX", dispWidth/10,0);
        if (!finish) {
            anim1B.setDuration(soundDuration/2-800); //800 - Для бесшовного
            anim1B.setRepeatCount(4);
            anim1B.setRepeatMode(ValueAnimator.REVERSE);
            animSetB.setInterpolator(new AccelerateDecelerateInterpolator());
            animSetB.setStartDelay(500);
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
