package com.example.cpu10661.hostdrivedemo;

import android.accounts.Account;
import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.example.cpu10661.hostdrivedemo.Utils.DriveApiUtils;

import static com.google.android.gms.drive.Drive.SCOPE_APPFOLDER;
import static com.google.android.gms.drive.Drive.SCOPE_FILE;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final int REQUEST_AUTHORIZATION = 2;

    // Handles high-level drive functions like sync
    private DriveClient mDriveClient;
    // Handle access to Drive resources/files.
    private DriveResourceClient mDriveResourceClient;
    private GoogleAccountCredential mCredential;
    private Drive mService;
    private String mSharedFolderId = null;

    private Handler mHandler;
    private ArrayAdapter<String> mAdapter;
    private ArrayList<String> mNameList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        HandlerThread handlerThread = new HandlerThread(getPackageName());
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());

        initUiComponents();
    }

    private void initUiComponents() {
        // add demo button
        Button addDemoButton = findViewById(R.id.btn_add_demo);
        addDemoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        saveDemoFile();
                    }
                });
            }
        });

        // file list view
        ListView nameListView = findViewById(R.id.lv_shared_with_me_files);
        mNameList = new ArrayList<>();
        mAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, mNameList);
        nameListView.setAdapter(mAdapter);
        nameListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String fileName = mNameList.get(position);
                showFileContent(fileName);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        signIn();
    }

    /**
     * Starts the sign-in process and initializes the Drive client.
     */
    private void signIn() {
        Set<Scope> requiredScopes = new HashSet<>(2);
        requiredScopes.add(SCOPE_FILE);
        requiredScopes.add(SCOPE_APPFOLDER);
        GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (signInAccount != null && signInAccount.getGrantedScopes().containsAll(requiredScopes)) {
            initializeDriveClient(signInAccount);
        } else {
            GoogleSignInOptions signInOptions =
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestScopes(SCOPE_FILE)
                            .requestScopes(SCOPE_APPFOLDER)
                            .requestEmail()
                            .build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, signInOptions);
            startActivityForResult(googleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
        }
    }

    /**
     * Continues the sign-in process, initializing the Drive clients with the current
     * user's account.
     */
    private void initializeDriveClient(GoogleSignInAccount signInAccount) {
        mDriveClient = com.google.android.gms.drive.Drive.getDriveClient(
                getApplicationContext(), signInAccount);
        mDriveResourceClient = com.google.android.gms.drive.Drive.getDriveResourceClient(
                getApplicationContext(), signInAccount);

        mCredential = GoogleAccountCredential.usingOAuth2(this,
                Collections.singletonList(DriveScopes.DRIVE));
        if (signInAccount.getAccount() != null) {
            String name = signInAccount.getAccount().name;
            String type = signInAccount.getAccount().type;
            mCredential.setSelectedAccount(new Account(name, type));
        }
        mService = getDriveService(mCredential);

        onDriveClientReady();
    }

    private Drive getDriveService(GoogleAccountCredential credential) {
        return new Drive
                .Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
                .build();
    }

    private void onDriveClientReady() {
        showSharedWithMeFileList();
    }

    /**
     * this method looked up specified file, reads its content and show it in an OK dialog
     * this method will be executed asynchronously
     *
     * @param fileName name of specified file
     */
    private void showFileContent(final String fileName) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String fileId = DriveApiUtils.getFileId(mService, mSharedFolderId, fileName);
                if (!fileId.isEmpty()) {
                    String content = DriveApiUtils.readFileContent(mService, fileId);
                    Utils.showOkDialog(MainActivity.this, fileName, content);
                }
            }
        });
    }

    private void showSharedWithMeFileList() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mSharedFolderId = DriveApiUtils.getSharedWithMeFolder(
                        mService, mCredential.getSelectedAccountName());
                final String[] nameList = DriveApiUtils.getSharedWithMeFileList(mService, mSharedFolderId);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mNameList.clear();
                        mNameList.addAll(Arrays.asList(nameList));
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    /**
     * this method is synchronous and must be called in background thread
     */
    private void saveDemoFile() {
        final String fileName = Utils.getCurrentTimestamp();
        java.io.File demoFile = createDemoFile(fileName);

        if (demoFile != null) {
            File fileMetadata = new File();
            fileMetadata.setName(fileName);
            fileMetadata.setParents(Collections.singletonList(mSharedFolderId));
            FileContent mediaContent = new FileContent("text/plain", demoFile);
            try {
                final File file = mService.files().create(fileMetadata, mediaContent)
                        .setFields("id, parents")
                        .execute();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mNameList.add(fileName);
                        mAdapter.notifyDataSetChanged();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    private java.io.File createDemoFile(String fileName) {
        if (!isExternalStorageWritable()) {
            Toast.makeText(this, "External storage is not writable", Toast.LENGTH_SHORT).show();
            return null;
        }

        java.io.File file = new java.io.File(getExternalFilesDir(null), fileName);
        try {
            String content = getPackageName() + "\n"
                    + mCredential.getSelectedAccountName() + "\n"
                    + Utils.getCurrentTimestamp();
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(content.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                if (resultCode != RESULT_OK) {
                    Log.e(TAG, "Sign-in failed.");
                    Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show();
                    return;
                }

                Task<GoogleSignInAccount> getAccountTask =
                        GoogleSignIn.getSignedInAccountFromIntent(data);
                if (getAccountTask.isSuccessful()) {
                    initializeDriveClient(getAccountTask.getResult());
                } else {
                    Log.e(TAG, "Sign-in failed.");
                    finish();
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == Activity.RESULT_OK) {
                    showSharedWithMeFileList();
                } else {
                    signIn();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
