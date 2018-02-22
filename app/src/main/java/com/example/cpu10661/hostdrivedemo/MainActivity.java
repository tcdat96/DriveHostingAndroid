package com.example.cpu10661.hostdrivedemo;

import android.accounts.Account;
import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import com.example.cpu10661.hostdrivedemo.Utils.DriveApiUtils;

import javax.annotation.Nonnull;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_ACCOUNT_PICKER = 1;
    private static final int REQUEST_AUTHORIZATION = 2;

    private GoogleAccountCredential mCredential = null;
    @Nonnull
    private Drive mService = getDriveService(null);
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
                saveDemoFileAsync();
            }
        });

        // add demo button
        Button refreshButton = findViewById(R.id.btn_refresh);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSharedWithMeFileListAsync();
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
                showFileContentAsync(fileName);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sign_out:
                signOut();
                break;
            case R.id.menu_revoke_access:
                revokeAccess();
                break;
        }
        return super.onOptionsItemSelected(item);
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
        GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (signInAccount != null) {
            initializeDriveClient(signInAccount);
        } else {
            chooseNewAccount();
        }
    }

    private void signOut() {
        mCredential = null;
        mService = getDriveService(null);

        mNameList.clear();
        mAdapter.notifyDataSetChanged();

        chooseNewAccount();
    }

    private void chooseNewAccount() {
        GoogleSignInClient googleSignInClient = getGoogleSignInClient();
        googleSignInClient.signOut();
        startActivityForResult(googleSignInClient.getSignInIntent(), REQUEST_ACCOUNT_PICKER);
    }

    private void revokeAccess() {
        GoogleSignInClient googleSignInClient = getGoogleSignInClient();
        googleSignInClient.revokeAccess()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(MainActivity.this,
                                getString(task.isSuccessful() ? R.string.revoke_successfully : R.string.revoke_failed),
                                Toast.LENGTH_SHORT).show();
                        signOut();
                    }
                });
    }

    private GoogleSignInClient getGoogleSignInClient() {
//        Scope driveScope = new Scope("https://www.googleapis.com/auth/drive");
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build();
        return GoogleSignIn.getClient(this, signInOptions);
    }

    /**
     * Continues the sign-in process, initializing the Drive clients with the current
     * user's account.
     */
    private void initializeDriveClient(GoogleSignInAccount signInAccount) {
        mCredential = GoogleAccountCredential.usingOAuth2(this,
                Collections.singletonList(DriveScopes.DRIVE))
                .setBackOff(new ExponentialBackOff());
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
        showSharedWithMeFileListAsync();
    }

    /**
     * this method looked up specified file, reads its content and show it in an OK dialog
     * this method will be executed asynchronously
     *
     * @param fileName name of specified file
     */
    private void showFileContentAsync(final String fileName) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    String fileId = DriveApiUtils.getFileId(mService, mSharedFolderId, fileName);
                    if (!fileId.isEmpty()) {
                        String content = DriveApiUtils.readFileContent(mService, fileId);
                        Utils.showOkDialog(MainActivity.this, fileName, content);
                    }
                } catch (Exception e) {
                    handleDriveException(e);
                }
            }
        });
    }

    private void showSharedWithMeFileListAsync() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    String accountName = mCredential != null ? mCredential.getSelectedAccountName() : "";
                    mSharedFolderId = DriveApiUtils.getSharedWithMeFolder(mService, accountName);
                    final ArrayList<String> nameList =
                            DriveApiUtils.getSharedWithMeFileList(mService, mSharedFolderId);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mNameList.clear();
                            mNameList.addAll(nameList);
                            mAdapter.notifyDataSetChanged();
                        }
                    });
                } catch (Exception e) {
                    handleDriveException(e);
                }
            }
        });
    }

    private void saveDemoFileAsync() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                final java.io.File demoFile = createLocalFile(Utils.getCurrentTimestamp());
                if (demoFile != null) {
                    try {
                        File file = DriveApiUtils.createNewFile(mService, mSharedFolderId, demoFile);
                        if (file != null) {
                            Log.d(TAG, "saveDemoFileAsync: " + file.getId());
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mNameList.add(demoFile.getName());
                                mAdapter.notifyDataSetChanged();
                            }
                        });
                    } catch (Exception e) {
                        handleDriveException(e);
                    }
                }
            }
        });
    }

    private void handleDriveException(Exception exception) {
        if (exception instanceof UserRecoverableAuthIOException) {
            UserRecoverableAuthIOException e = (UserRecoverableAuthIOException) exception;
            startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
        } else if (exception instanceof GoogleJsonResponseException) {
            GoogleJsonResponseException e = (GoogleJsonResponseException) exception;
            String message = String.format(Locale.getDefault(), "%d %s: %s", e.getStatusCode(),
                    e.getStatusMessage(), e.getDetails().getMessage());
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();

            if (e.getStatusCode() == 401) {
                signIn();
            }

            e.printStackTrace();
        } else {
            exception.printStackTrace();
        }
    }

    @Nullable
    private java.io.File createLocalFile(String fileName) {
        if (!isExternalStorageWritable()) {
            Toast.makeText(this, "External storage is not writable", Toast.LENGTH_SHORT).show();
            return null;
        }

        java.io.File file = new java.io.File(getExternalFilesDir(null), fileName);
        try {
            String accountName = mCredential != null ? mCredential.getSelectedAccountName() : "";
            String content = String.format("%s\n%s\n%s", getPackageName(), accountName, Utils.getCurrentTimestamp());
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
            case REQUEST_ACCOUNT_PICKER:
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
                    onDriveClientReady();
                } else {
                    Toast.makeText(this, getString(R.string.require_permission), Toast.LENGTH_SHORT)
                            .show();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
