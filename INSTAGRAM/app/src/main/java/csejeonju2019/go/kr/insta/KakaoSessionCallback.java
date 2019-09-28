package csejeonju2019.go.kr.insta;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Toast;


import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kakao.auth.ISessionCallback;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.MeResponseCallback;
import com.kakao.usermgmt.response.model.User;
import com.kakao.usermgmt.response.model.UserProfile;
import com.kakao.util.exception.KakaoException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.firebase.ui.auth.AuthUI.getApplicationContext;

//19.09.23 MrJANG: for KAKAO
public class KakaoSessionCallback extends BaseActivity implements ISessionCallback, View.OnClickListener {

    //DB 변수 선언
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference databaseReference = firebaseDatabase.getReference();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    SignInActivity mSignActivity;
    static boolean isLoginKakao = false;

    String nickname;
    String email;
    String profileImagePath;
    String thumnailPath;
    String UUID;
    long id;
    String stringID;
    String kakaoType;








    //시간에 관한 변수 선언
    Date mDate = new Date(System.currentTimeMillis());
    String timestamp = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(mDate); //로그인 시간

    // 로그인에 성공한 상태
    @Override
    public void onSessionOpened() {
        requestMe();
    }

    // 로그인에 실패한 상태
    @Override
    public void onSessionOpenFailed(KakaoException exception) {
        Log.e("SessionCallback :: ", "onSessionOpenFailed : " + exception.getMessage());
    }

    // 사용자 정보 요청
    public void requestMe() {
        // 사용자정보 요청 결과에 대한 Callback

        UserManagement.getInstance().requestMe(new MeResponseCallback() {
            // 세션 오픈 실패. 세션이 삭제된 경우,
            @Override
            public void onSessionClosed(ErrorResult errorResult) {
                Log.e("SessionCallback :: ", "onSessionClosed : " + errorResult.getErrorMessage());
            }

            // 회원이 아닌 경우,
            @Override
            public void onNotSignedUp() {
                Log.e("SessionCallback :: ", "onNotSignedUp");
            }

            // 사용자정보 요청에 성공한 경우,
            @Override
            public void onSuccess(UserProfile userProfile) {
                Log.e("SessionCallback :: ", "onSuccess");

                nickname = userProfile.getNickname();
                email = userProfile.getEmail();
                profileImagePath = userProfile.getProfileImagePath();
                thumnailPath = userProfile.getThumbnailImagePath();
                UUID = userProfile.getUUID();
                id = userProfile.getId();
                stringID = String.valueOf(id);
                kakaoType = "kakao";

                Log.e("Profile : ", nickname + "");
                Log.e("Profile : ", email + "");
                Log.e("Profile : ", profileImagePath  + "");
                Log.e("Profile : ", thumnailPath + "");
                Log.e("Profile : ", UUID + "");
                Log.e("Profile : ", id + "");

                //realtime에 DB 넣기 시작!
                DatabaseReference ref = databaseReference.child("accounts");
                final KakaoAccount kakaoAccount = new KakaoAccount(stringID, timestamp,kakaoType);
                ref.push().setValue(kakaoAccount);

                //cloud DB에 넣기 시작!
                mAuth = FirebaseAuth.getInstance();
                db = FirebaseFirestore.getInstance();
                //mSignActivity = new SignInActivity();

                // Write new user

// Create a new user with a first and last name
                Map<String, Object> user = new HashMap<>();
                user.put("email", stringID+"@"+kakaoType);
                user.put("username", nickname);

// Add a new document with a generated ID
                db.collection("users")
                        .add(user)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d("sucess! firestore : ", "DocumentSnapshot added with ID: " + documentReference.getId());
                                KakaoSessionCallback.isLoginKakao = true;

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("failure firestore : ", "Error adding document", e);
                            }
                        });





            }

            // 사용자 정보 요청 실패
            @Override
            public void onFailure(ErrorResult errorResult) {
                Log.e("SessionCallback :: ", "onFailure : " + errorResult.getErrorMessage());
            }
        });
    }

    @Override
    public void onClick(View v) {

        Log.d("SingIn : ", "signIn");
        if (!validateForm()) return;

        showProgressDialog();
        email = KakaoAccount.AccessToken;
        kakaoType = "kakao";

        mAuth.signInWithEmailAndPassword(email, kakaoType)
                .addOnCompleteListener(SignInActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d("SingIn : ", "signIn:onComplete:" + task.isSuccessful());
                        hideProgressDialog();

                        if (task.isSuccessful()) {
                            onAuthSuccess(task.getResult().getUser());
                        } else {
                            Toast.makeText(SignInActivity.this, "Sign In Failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


}