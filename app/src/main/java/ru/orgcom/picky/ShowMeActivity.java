package ru.orgcom.picky;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.flexbox.FlexboxLayout;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;

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

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        dispWidth = metrics.widthPixels;
        dispHeight = metrics.heightPixels;
        density = metrics.scaledDensity;

        gameRL=(RelativeLayout) findViewById(R.id.gameRL);
        gameFlexLayout=(FlexboxLayout) findViewById(R.id.gameFlexboxLayout);

        connect2db();
        redrawAll();
    }

    @Override
    public void onResume() {
        super.onResume();
        hide();
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

    void redrawAll(){
        gameFlexLayout.removeAllViews();
        imageButtons.clear();

        int side= (int) Math.sqrt(dispWidth*dispHeight/buttonsAmount);
        while ((Math.floor(dispWidth/side)*Math.floor(dispHeight/side))<buttonsAmount && side>50){
            //Log.d(TAG,"ShowMeActivity: side="+side+", amounttt="+(Math.floor(dispWidth/side)*Math.floor(dispHeight/side)));
            side--;
        }
        Log.d(TAG,"ShowMeActivity: finally side="+side+", amounttt="+(Math.floor(dispWidth/side)*Math.floor(dispHeight/side)));

        Cursor c= db.rawQuery("select * from cards", null);
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
        askme();
    }

    void askme(){
        for (int i=0;i<imageButtons.size();i++){
            imageButtons.get(i).clearColorFilter();
        }
        int randI,tries=0;
        do {
            randI=(int) (Math.random()*imageButtons.size());
            tries++;
        } while (imageButtons.get(randI).getAlpha()<1 && tries<100);
        if (tries>=100){
            redrawAll();
            return;
        }
        imageButtons.get(randI).setAlpha(0.99f);
        cardId=(int)(imageButtons.get(randI).getTag());
        Cursor c= db.rawQuery("select * from cards where id="+cardId, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            sayIt(0,cardId);
            ignoreTaps=false;
        }
        c.close();
    }
    void sayIt(int casse, int id){
        //casse: 0 - ask
        //1 - wrong
        //2 - right
        String addon="title", prefix="";
        switch (casse){
            case 0:
                addon="anons";
                prefix="Покажи, где ";
                break;
            case 1:
                addon="wrong";
                prefix="Неправильно, это ";
                break;
            case 2:
                addon="right";
                prefix="Все верно, это ";
                break;
        }
        String voiceRec="";
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
                ((TextView)findViewById(R.id.hint)).setText(finalPrefix);
            }
        });
    }
    void analyze(View view){
        int id=(int)(view.getTag());
        Log.d(TAG,"Это id "+id);
        if (id!=cardId){
            //wrong
            ((ImageButton)view).setColorFilter(Color.parseColor("#99ff0000"));
            sayIt(1, id);
            sayIt(0, cardId);
            ignoreTaps=false;
        } else {
            //right
            sayIt(2, id);
            ((ImageButton)view).setColorFilter(Color.parseColor("#7700ff00"));
            winAnimation(view);
        }
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
        ObjectAnimator anim1B = ObjectAnimator.ofFloat(view, "scaleY", 0.8f,1);
        anim1B.setDuration(400);
        ObjectAnimator anim2B = ObjectAnimator.ofFloat(view, "scaleX", 0.8f,1);
        anim2B.setDuration(400);
        animSetB.play(anim1B).with(anim2B);
        animSetB.setInterpolator(new BounceInterpolator());
        animSetB.start();
    }
    @Override
    public void onClick(View view) {
        if (view.getTag()!=null && !ignoreTaps){
            ignoreTaps=true;
            animateMe(view);
        }
    }
    void winAnimation(View view){
        for (int i=0;i<imageButtons.size();i++){
            if (imageButtons.get(i)!=view) {
                imageButtons.get(i).setColorFilter(Color.parseColor("#99ff0000"));
            } else {
                imageButtons.get(i).bringToFront();
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
        anim1B.setDuration(600);
        anim1B.setRepeatCount(6);
        anim1B.setRepeatMode(ValueAnimator.REVERSE);
        ObjectAnimator anim2B = ObjectAnimator.ofFloat(view, "scaleX", 1.9f,1);
        anim2B.setDuration(600);
        anim2B.setRepeatCount(6);
        anim2B.setRepeatMode(ValueAnimator.REVERSE);
        animSetB.play(anim1B).with(anim2B);
        animSetB.setInterpolator(new AccelerateDecelerateInterpolator());
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
