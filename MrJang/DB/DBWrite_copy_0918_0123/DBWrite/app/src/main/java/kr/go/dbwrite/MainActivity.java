package kr.go.dbwrite;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kakao.auth.KakaoSDK;
import com.kakao.auth.Session;
import com.kakao.usermgmt.LoginButton;
import com.kakao.util.KakaoUtilService;
import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginHandler;
import com.nhn.android.naverlogin.ui.view.OAuthLoginButton;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static com.kakao.auth.Session.getCurrentSession;
import static com.nhn.android.naverlogin.OAuthLogin.mOAuthLoginHandler;

public class MainActivity extends AppCompatActivity {

    //0914 MrJang modified 'private' -> 'public'
    public FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    public DatabaseReference databaseReference = firebaseDatabase.getReference();

    OAuthLogin mOAuthLoginModule;
    OAuthLoginButton mOAuthLoginButton;
    Context mContext;
    boolean firstLogin;
    GoogleSignInClient mGoogleSignInClient;
    String key;

    private LoginButton btn_kakao_login;        //kakao 0915 mrJang
    private SessionCallback callback;           //kakao 0915 mrJang

    // onActivityResult에서 종료된 Activity 구분에 필요한 상수
    public static final int REQUEST_CODE_MENU = 101, RC_SIGN_IN = 100;
    // Naver 로그인 핸들러
    private OAuthLoginHandler mOAuthLoginHandler = new OAuthLoginHandler() {
        @Override
        public void run(boolean success) {
            //Naver 로그인 성공
            if (success) {
                String accessToken = mOAuthLoginModule.getAccessToken(mContext);
                writeAccountInfo(accessToken, "Naver");
            }
            //Naver 로그인 실패
            else {
                String errorCode = mOAuthLoginModule.getLastErrorCode(mContext).getCode();
                String errorDesc = mOAuthLoginModule.getLastErrorDesc(mContext);
                Toast.makeText(mContext, "errorCode:" + errorCode
                        + ", errorDesc:" + errorDesc, Toast.LENGTH_SHORT).show();
            }
        }
    };


    //0914 MrJang modified the function type! "private->public"
    public void writeAccountInfo(final String token, String type) {
        key = databaseReference.child("accounts").push().getKey();
        Date mDate = new Date(System.currentTimeMillis());

        String timestamp = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(mDate); //로그인 시간

        Account account = new Account(token, timestamp, type);
        Map<String, Object> accValues = account.toMap();

        DatabaseReference ref = firebaseDatabase.getReference().child("accounts");
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                firstLogin = true;
                Account current = dataSnapshot.getValue(Account.class);
                if(token.equals(current.AccessToken)) {
                    //DatabaseReference updateRef = databaseReference.child("accounts");
                    Toast.makeText(getApplicationContext(), "You've already logged in!\n" + token, Toast.LENGTH_LONG).show();
                    firstLogin = false;
                    //Map<String, Object> accountUpdates = new Account(token, timestamp, current.LoginType).toMap();
                    //updateRef.updateChildrenAsync()
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/accounts/" + key, accValues);
        //childUpdates.put("/user-posts/" + token + "/" + key, accValues);
        if(firstLogin) databaseReference.updateChildren(childUpdates);
    }

    //구글 로그인 이후 핸들러
    private void handleSignInResult(@org.jetbrains.annotations.NotNull Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            Toast.makeText(this, "Google Login Successful!", Toast.LENGTH_LONG).show();
            GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
            if (acct != null) {
                String personEmail = acct.getEmail();
                writeAccountInfo(personEmail, "Google");
            }
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("TAG", "signInResult:failed code=" + e.getStatusCode());
            Toast.makeText(this, "Google Login Fail!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //구글 로그인 초기화
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_WIDE);
        //구글 로그인 버튼 리스너
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.sign_in_button:
                        signIn();
                        break;
                }
            }

            private void signIn() {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });


        //네이버 로그인 초기화
        mOAuthLoginModule = OAuthLogin.getInstance();
        mOAuthLoginModule.init(
                MainActivity.this
                , "0PSGjaLXb_f6WljImx1S"
                , "7mzm8tggH6"
                , "cseJeonju2019"
                //,OAUTH_CALLBACK_INTENT
                // SDK 4.1.4 버전부터는 OAUTH_CALLBACK_INTENT변수를 사용하지 않습니다.
        );
        mOAuthLoginButton = (OAuthLoginButton) findViewById(R.id.buttonOAuthLoginImg);
        mOAuthLoginButton.setOAuthLoginHandler(mOAuthLoginHandler);

        KakaoSDK.init(new KakaoSDKAdapter());
        callback = new SessionCallback();
        Session.getCurrentSession().addCallback(callback);
        Session.getCurrentSession().checkAndImplicitOpen();

    }

    //0916 MrJang : KAKAO DATABASE FUNCTION!
    public void callKakaoDatabase()
    {

    }

    public void onStart() {
        super.onStart();
        DatabaseReference ref = firebaseDatabase.getReference().child("accounts");
        ref = ref.child(ref.getKey());
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                DatabaseReference ref = firebaseDatabase.getReference("accounts");

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), "DB Read Failed! with " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    public void onDestroy(){
        mOAuthLoginModule.logout(mContext);
        mGoogleSignInClient.signOut();
        super.onDestroy();
        getCurrentSession().removeCallback(callback);
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        //0915 MrJang KAKAO
        if (getCurrentSession().handleActivityResult(requestCode, resultCode, data))
            return;

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }
}