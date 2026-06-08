package com.example.projectlast;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean testLogin = getSharedPreferences("auth", MODE_PRIVATE)
                .getBoolean("test_login", false);
        if (!testLogin && FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), false);
        }
    }

    public void navigateToHome() {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        loadFragment(new HomeFragment(), false);
    }

    public void navigateToFriends() {
        loadFragment(new FriendFragment(), true);
    }

    public void navigateToMypage() {
        loadFragment(new MypageFragment(), true);
    }

    public void navigateToCalendar() {
        loadFragment(new CalendarFragment(), true);
    }

    public void navigateToGroupCalendar(String groupId) {
        loadFragment(GroupCalendarFragment.newInstance(groupId), true);
    }

    private void loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction tx = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment);
        if (addToBackStack) tx.addToBackStack(null);
        tx.commit();
    }
}
