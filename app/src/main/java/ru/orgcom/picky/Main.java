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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatDrawableManager;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
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
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.features.ImagePickerActivity;
import com.esafirm.imagepicker.model.Image;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Locale;

import static android.R.attr.path;

public class Main extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, AdapterView.OnItemClickListener, View.OnClickListener {
    String TAG = "djd";
    SharedPreferences prefs;
    public static SQLiteDatabase db;
    ListView cardsLV;
    ArrayList<CardsListItem> cardsItems;
    CardsAdapter cardsAdapter;
    BroadcastReceiver br1,br2;
    FloatingActionButton fab;
    ArrayList<Image> images;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Выбирайка запущена, SDK=" + Build.VERSION.SDK_INT);
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                //Log.d(TAG, "exception: " + this.getClass().getName() + ", " + throwable.toString());
                final Writer result = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(result);
                throwable.printStackTrace(printWriter);
                String stacktrace = "GOmain: " + result.toString();
                Log.e(TAG, "exception: " + stacktrace);
                //Intent intent = new Intent(getApplicationContext(), checkService.class);
                //intent.putExtra("postlog", stacktrace);
                //startService(intent);
                System.exit(0);
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        setContentView(R.layout.main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

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
        cardsLV.setOnItemClickListener(this);
    }
    @Override
    public void onResume() {
        fab.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateCards();
            }
        },150);
        super.onResume();
    }
    @Override
    protected void onDestroy(){
        //Log.d(TAG,"onDestroy");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(br1);
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(br2);
        super.onDestroy();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 123 && resultCode == RESULT_OK && data != null) {
            //final ArrayList<Image> images = data.getParcelableArrayListExtra(ImagePickerActivity.INTENT_EXTRA_SELECTED_IMAGES);
            final ArrayList<Image> images = (ArrayList<Image>) ImagePicker.getImages(data);
            ((ProgressBar)findViewById(R.id.progressBar2)).setVisibility(View.VISIBLE);
            ((ProgressBar)findViewById(R.id.topProgressBar)).setVisibility(View.VISIBLE);
            ((ProgressBar)findViewById(R.id.topProgressBar)).setMax(images.size());
            new Thread(new Runnable() {
                public void run() {
                    for (int i=0; i<images.size();i++) {
                        final int finalI = i;
                        runOnUiThread(new Runnable() {
                            public void run() {
                                ((ProgressBar)findViewById(R.id.topProgressBar)).setProgress(finalI);
                            }
                        });
                        Cursor c = db.rawQuery("select id from cards", null);
                        c.moveToLast();
                        int lastId = c.getCount()>0?c.getInt(c.getColumnIndex("id")):0;
                        c.close();

                        String pic=images.get(i).getPath();
                        Log.d(TAG,"pic path original="+pic);
                        BitmapResizer br=new BitmapResizer();
                        pic=br.getResizedPath(pic);
                        db.execSQL("insert into cards (pic,title,theme) values (\""+pic+"\", \"Карточка " + (lastId + 1) + "\",0)");
                        runOnUiThread(new Runnable() {
                            public void run() {
                                updateCards();
                                fab.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        cardsLV.smoothScrollToPosition(cardsLV.getCount() - 1);
                                    }
                                }, 250);
                            }
                        });
                    }
                    runOnUiThread(new Runnable() {
                        public void run() {
                            ((ProgressBar)findViewById(R.id.topProgressBar)).setVisibility(View.GONE);
                            ((ProgressBar)findViewById(R.id.progressBar2)).setVisibility(View.GONE);
                        }
                    });
                }
            }).start();// * /
        }
    }


    @Override
    public void onClick(View view) {
        if (view==fab){
            //https://github.com/nguyenhoanglam/ImagePicker
            ImagePicker.create(Main.this)
                    .folderMode(true) // folder mode (false by default)
                    .folderTitle("Выберите папку") // folder selection title
                    .imageTitle("Укажите картинки") // image selection title
                    //.single() // single mode
                    .multi() // multi mode (default mode)
                    //.limit(10) // max images can be selected (999 by default)
                    .showCamera(true) // show camera or not (true by default)
                    .imageDirectory("Camera") // directory name for captured image  ("Camera" folder by default)
                    //.origin(images) // original selected images, used in multi mode
                    .start(123); // start image picker activity with request code
        }
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
            cardsAdapter.purgeMode=!cardsAdapter.purgeMode;
            item.setChecked(cardsAdapter.purgeMode);
            updateCards();
            fab.setVisibility(cardsAdapter.purgeMode?View.GONE:View.VISIBLE);
            return true;
        } else if (id == R.id.action_settings_all) {
            if (cardsAdapter.purgeMode) {
                cardsAdapter.purgeMode = false;
                fab.setVisibility(cardsAdapter.purgeMode?View.GONE:View.VISIBLE);
            }
            new AlertDialog.Builder(Main.this)
                    .setTitle("Удалить все карточки?")
                    .setMessage("Файлы с картинками останутся в памяти.")
                    .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            db.execSQL("delete from cards");
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
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_tester) {
            startActivity(new Intent(Main.this, ShowMeActivity.class));
        } else if (id == R.id.nav_manage) {
            startActivity(new Intent(Main.this, pref.class));
        } else if (id == R.id.nav_share) {
            openFolder();
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    void openFolder(){
        new AlertDialog.Builder(Main.this)
                .setTitle("Где лежат отчёты")
                .setMessage("Папка SDCARD/pickyreports/\n\nВ случае проблем подключите планшет к ПК в режим устройства хранения\n\nРасширенный менеджмент будет позднее.")
                .setPositiveButton("Открыть папку", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Uri selectedUri = Uri.parse(Environment.getExternalStorageDirectory() + "/pickyreports/");
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(selectedUri, "resource/folder");
                        if (intent.resolveActivityInfo(getPackageManager(), 0) != null) {
                            startActivity(intent);
                        } else {
                            // if you reach this place, it means there is no any file explorer app installed on your device
                            intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            intent.setDataAndType(selectedUri, "text/csv");
                            if (intent.resolveActivityInfo(getPackageManager(), 0) != null) {
                                startActivity(Intent.createChooser(intent, "Открыть папку"));
                            } else {
                                intent = new Intent();
                                intent.setAction(android.content.Intent.ACTION_VIEW);
                                File file = new File(Environment.getExternalStorageDirectory() + "/pickyreports/");
                                MimeTypeMap mime = MimeTypeMap.getSingleton();
                                String ext = file.getName().substring(file.getName().indexOf(".") + 1);
                                String type = mime.getMimeTypeFromExtension(ext);
                                intent.setDataAndType(Uri.fromFile(file), type);
                                if (intent.resolveActivityInfo(getPackageManager(), 0) != null) {
                                    startActivity(intent);
                                } else {
                                    mytoast(true,"Никак. Запускайте сторонний диспетчер папок.");
                                }

                            }
                        }
                    }
                })
                .setNegativeButton("Ок", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .setCancelable(true)
                .show();
    }
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        int[] iid = (int[]) view.getTag();
        if (iid!=null) {
            Log.d(TAG, "cardsList onItemClick id=" + iid[0]);

            Intent intent = new Intent(Main.this, TheCardActivity.class);
            intent.putExtra("cardID", iid[0]);

            Pair<View, String>[] transitionPairs = new Pair[3];
            transitionPairs[0] = Pair.create(findViewById(R.id.toolbar), "toolbar"); // Transition the Toolbar
            transitionPairs[1] = Pair.create(view, "currentCard"); // Transition the content_area (This will be the content area on the detail screen)
            transitionPairs[2] = Pair.create(view.findViewById(R.id.cardIco), "currentCardPic");
            // We also want to transition the status and navigation bar barckground. Otherwise they will flicker
            //transitionPairs[2] = Pair.create(findViewById(android.R.id.statusBarBackground), Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME);
            //transitionPairs[3] = Pair.create(findViewById(android.R.id.navigationBarBackground), Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME);
            Bundle b = ActivityOptionsCompat.makeSceneTransitionAnimation(Main.this, transitionPairs).toBundle();
            ActivityCompat.startActivity(Main.this, intent, b);

            //ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(Main.this, (View)findViewById(R.id.textViewSystemName), "currentCard");
            //startActivity(intent1, options.toBundle());
            ///https://github.com/codepath/android_guides/wiki/Shared-Element-Activity-Transition
            //mytoast(false, "Открываем карточку #" + cardID);
        }
    }

    void updateCards(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TransitionManager.beginDelayedTransition((ViewGroup) findViewById(android.R.id.content), new Slide());
        }
        cardsItems.clear();
        Cursor c = db.rawQuery("select * from cards", null);
        if (c!=null){
            if (c.getCount() > 0){
                c.moveToFirst();
                do {
                    cardsItems.add(new CardsListItem(c.getInt(c.getColumnIndex("id")),c.getInt(c.getColumnIndex("theme")),c.getString(c.getColumnIndex("pic")),c.getString(c.getColumnIndex("title"))));
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
        db = SQLiteDatabase.openDatabase(ppp1, null, SQLiteDatabase.CREATE_IF_NECESSARY);//openOrCreateDatabase(dbfile, null);
        if (!db.isOpen()){
            Log.d(TAG,"БД не открывается");
            mytoast(true,"Список карточек испорчен!");
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
