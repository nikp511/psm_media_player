package video.player.qrplayer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import info.hoang8f.android.segmented.SegmentedGroup;
import video.player.qrplayer.R;

public class WhatsappStatusActivity extends AppCompatActivity {

    SegmentedGroup segmented5;
    ViewPager viewPager;
    ImageView imgBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whatsapp_status);

        viewPager = findViewById(R.id.viewPager);
        imgBack = findViewById(R.id.imgBack);
        segmented5 = (SegmentedGroup) findViewById(R.id.segmented2);
        Button addBtn = (Button) findViewById(R.id.button21);
        Button removeBtn = (Button) findViewById(R.id.button22);


        MyPagerAdapter pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(0, true);
            }
        });
        removeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(1, true);
            }
        });
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    segmented5.check(R.id.button21);
                } else if (position == 1) {
                    segmented5.check(R.id.button22);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(WhatsappStatusActivity.this, HomeActivity.class));
        finish();
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
    }

    public class MyPagerAdapter extends FragmentPagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return new Fragment();
            } else {
                return new Fragment();
            }
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) {
                return "RECENT";
            } else {
                return "SAVED";
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
