package ru.orgcom.picky;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;

public class CardsAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<CardsListItem> cardsListItems;
    private LayoutInflater mInflater;
    Intent intentOut=null;
    public boolean purgeMode=false;

    public CardsAdapter(Context context, ArrayList<CardsListItem> cardsListItems){
        this.context = context;
        this.cardsListItems = cardsListItems;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return cardsListItems.size();
    }

    @Override
    public Object getItem(int position) {
        return cardsListItems.get(position);
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
            return 0;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.card_list_item, null);
        }

        if (position<cardsListItems.size()) {

            int[] iid = new int[1];
            iid[0] = cardsListItems.get(position).id;
            convertView.findViewById(R.id.cloudOuterL).setTag(iid);

            ((TextView) convertView.findViewById(R.id.textViewSystemName)).setText(cardsListItems.get(position).title);
            if (cardsListItems.get(position).pic!=null && !cardsListItems.get(position).pic.equals(""))
                try {
                    //Log.d("djd","CardsAdapter: pic="+cardsListItems.get(position).pic);
                    ((ImageView) convertView.findViewById(R.id.cardIco)).setImageURI(Uri.parse(cardsListItems.get(position).pic));
                    //Glide.with(context).load(Uri.parse(cardsListItems.get(position).pic)).into(((ImageView) convertView.findViewById(R.id.cardIco)));
                } catch (Exception e) {
                    Log.e("djd",""+e);
                }
            else
                ((ImageView) convertView.findViewById(R.id.cardIco)).setImageResource(R.drawable.ico);

            if (purgeMode) {
                convertView.findViewById(R.id.deleteButton).setVisibility(View.VISIBLE);
                convertView.findViewById(R.id.deleteButton).setTag(iid);
                convertView.findViewById(R.id.deleteButton).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int[] iid = (int[]) view.getTag();
                        //Log.d("djd","iid="+iid[0]);
                        intentOut = new Intent("pickyCardsSelectorCall");
                        intentOut.putExtra("id", iid[0]);
                        intentOut.putExtra("delete",true);
                        animateMe(view);
                    }
                });
            }else
                convertView.findViewById(R.id.deleteButton).setVisibility(View.GONE);
        }

        return convertView;
    }

    void animateMe(View view){
        AnimatorSet animSetB=new AnimatorSet();
        animSetB.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (intentOut!=null)
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intentOut);
                intentOut=null;
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        ObjectAnimator anim1B = ObjectAnimator.ofFloat(view, "scaleY", 0.8f,1);
        anim1B.setDuration(200);
        ObjectAnimator anim2B = ObjectAnimator.ofFloat(view, "scaleX", 0.98f,1);
        anim2B.setDuration(200);
        animSetB.play(anim1B).with(anim2B);
        animSetB.setInterpolator(new BounceInterpolator());
        animSetB.start();
    }

}