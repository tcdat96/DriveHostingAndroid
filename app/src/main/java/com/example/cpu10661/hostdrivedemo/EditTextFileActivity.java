package com.example.cpu10661.hostdrivedemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;

import javax.annotation.Nullable;

import static com.example.cpu10661.hostdrivedemo.MainActivity.EXTRA_DATA_FILE_NAME;
import static com.example.cpu10661.hostdrivedemo.MainActivity.EXTRA_FILE_ID;

public class EditTextFileActivity extends AppCompatActivity {

    private EditText mDataEditText;
    private String mFileName;
    private String mFileId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_text_file);

        if (!validateIntentData()) {
            finish();
            return;
        }
        mFileName = getIntent().getStringExtra(EXTRA_DATA_FILE_NAME);
        mFileId = getIntent().getStringExtra(EXTRA_FILE_ID);

        initUiComponents();

        // populate EditText with data
        try {
            String content = Utils.readExternalFileContent(this, mFileName);
            mDataEditText.setText(content);
        } catch (IOException e) {
            Toast.makeText(this, R.string.error_retrieve_data, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void initUiComponents() {
        mDataEditText = findViewById(R.id.edt_data);

        Button uploadButton = findViewById(R.id.btn_upload);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Utils.createExternalFileContent(EditTextFileActivity.this,
                            mFileName, mDataEditText.getText().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent();
                intent.putExtra(EXTRA_DATA_FILE_NAME, mFileName);
                intent.putExtra(EXTRA_FILE_ID, mFileId);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private boolean validateIntentData() {
        if (getIntent() == null
                || !getIntent().hasExtra(EXTRA_DATA_FILE_NAME)
                || !getIntent().hasExtra(EXTRA_FILE_ID)) {
            finish();
            return false;
        }
        return true;
    }
}
