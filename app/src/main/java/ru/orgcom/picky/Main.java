package ru.orgcom.picky;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

public class Main extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    String TAG = "djd";
    SharedPreferences prefs;
    public static SQLiteDatabase db;
    ListView cardsLV;
    ArrayList<CardsListItem> cardsItems;
    CardsAdapter cardsAdapter;
    BroadcastReceiver br1,br2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"Выбирайка запущена");
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        setContentView(R.layout.main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Cursor c = db.rawQuery("select id from cards", null);
                int count=c.getCount();
                c.close();
                db.execSQL("insert into cards (title,theme) values (\"Новая карточка №"+(count+1)+"\",0)");
                updateCards();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        br1 = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                final int cardID = intent.getIntExtra("id", 0);
                if (cardID>0) {
                    if (intent.getBooleanExtra("delete",false)){
                        new AlertDialog.Builder(Main.this)
                                .setTitle("Удалить карточку?")
                                .setMessage("Файл с картинкой и статистика сохранятся. Звук придётся перезаписывать")
                                .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        db.execSQL("delete from cards where id="+cardID);
                                        updateCards();
                                    }
                                })
                                .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                    }
                                })
                                .setCancelable(true)
                                .show();
                        return;
                    }
                    Intent intent1 = new Intent(Main.this, TheCardActivity.class);
                    intent.putExtra("cardID", cardID);
                    ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(Main.this, (View)findViewById(R.id.textViewSystemName), "currentCard");
                    startActivity(intent1, options.toBundle());
                    ///https://github.com/codepath/android_guides/wiki/Shared-Element-Activity-Transition
                    //mytoast(false, "Открываем карточку #" + cardID);
                }
            }
        };
        IntentFilter intFiltr1 = new IntentFilter("pickyCardsSelectorCall");
        LocalBroadcastManager.getInstance(this).registerReceiver(br1, intFiltr1);

        connect2db();

        cardsLV = (ListView) findViewById(R.id.cardsLV);
        cardsItems = new ArrayList<CardsListItem>();
        cardsAdapter = new CardsAdapter(getApplicationContext(), cardsItems);
        cardsLV.setAdapter(cardsAdapter);

        fab.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateCards();
            }
        },150);
    }
    @Override
    protected void onDestroy(){
        //Log.d(TAG,"onDestroy");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(br1);
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(br2);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            mytoast(false,"Будет сделано позднее");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_gallery) {
            mytoast(false,"Это пока единственный экран");
        } else if (id == R.id.nav_slideshow) {
            mytoast(false,"Будет сделано позднее");
        } else if (id == R.id.nav_manage) {
            mytoast(false,"Будет сделано позднее");
        } else if (id == R.id.nav_share) {
            mytoast(false,"Будет сделано позднее");
        } else if (id == R.id.nav_send) {
            mytoast(false,"Будет сделано позднее");
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    void updateCards(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            TransitionManager.beginDelayedTransition((ViewGroup) findViewById(android.R.id.content), new Fade());
        }
        cardsItems.clear();
        Cursor c = db.rawQuery("select * from cards", null);
        if (c!=null){
            if (c.getCount() > 0){
                c.moveToFirst();
                do {
                    cardsItems.add(new CardsListItem(c.getInt(c.getColumnIndex("id")),c.getInt(c.getColumnIndex("theme")),c.getString(c.getColumnIndex("title"))));
                } while (c.moveToNext());
            }else {
                mytoast(true,"Нет ни одной карточки");
            }
            c.close();
        }
        cardsAdapter.notifyDataSetChanged();
    }

    void connect2db(){
        String ppp=getDatabasePath("picky.db").getParent();//.getAbsolutePath();//getPath();
        //Log.d(TAG, "getDatabasePath=" + ppp);
        File dbfile = new File(ppp);//dir+"/doors.db");
        if (!dbfile.exists()){
            //Log.d(TAG,"ppp not exists,creating");
            dbfile.mkdir();
            //Log.d(TAG, "ppp created");
        } else {
            //Log.d(TAG,"ppp exists");
        }

        String ppp1=getDatabasePath("picky.db").getAbsolutePath();//getFilesDir().getAbsolutePath()+"/doors.db";//getDatabasePath("doors.db").getAbsolutePath();//getPath();
        //Log.d(TAG,"getDatabasePath="+ppp1);
        db = SQLiteDatabase.openDatabase(ppp1,
                null, SQLiteDatabase.CREATE_IF_NECESSARY);//openOrCreateDatabase(dbfile, null);
        if (!db.isOpen()){
            Log.d(TAG,"БД не открывается");
        }

        /////////////////////////////////////////////////
        int curDBver=52; //меняй когда меняешь базы
        boolean dropit=prefs.getInt("dbversion", 0)<curDBver;
        if (dropit) {
            try {
                db.execSQL("drop table cards");
            } catch (SQLiteException exception) {
                Log.d(TAG, "exception drop cards");
                exception.printStackTrace();
            }
            try {
                db.execSQL("create table cards ("
                        + "id integer primary key,"
                        + "pic text,"
                        + "title text,"
                        + "anons text,"
                        + "question text,"
                        + "wrong text,"
                        + "right text,"
                        + "theme integer"
                        + ");");
                //db.execSQL("create INDEX djd_polls_index ON djd_polls (battle);");
                Log.d(TAG, "создана новая база данных cards");
                ///*
                db.execSQL("insert into cards (title,theme) values (\"Стол\",0)");
                db.execSQL("insert into cards (title,theme) values (\"Качели\",0)");
                db.execSQL("insert into cards (title,theme) values (\"Заяц\",1)");
                db.execSQL("insert into cards (title,theme) values (\"Мышь\",1)");
                db.execSQL("insert into cards (title,theme) values (\"Лиса\",1)");
                //*/
            } catch (SQLiteException exception) {
                Log.e(TAG, "exception создания базы cards: "+exception.toString());
                exception.printStackTrace();
            }
            SharedPreferences.Editor ed = prefs.edit();
            ed.putInt("dbversion", curDBver);
            ed.commit();
        }
    }

    void mytoast(final boolean length_long,final String s){
        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.placeSnackBar)/**/, s, length_long?Snackbar.LENGTH_LONG:Snackbar.LENGTH_SHORT)
                .setAction("Action", null);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(Color.parseColor("#FF4081"));
        snackbar.show(); // Don’t forget to show! // */
    }
}
