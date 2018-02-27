package com.example.cpu10661.hostdrivedemo;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


@SuppressWarnings("WeakerAccess")
class Utils {
    @Nonnull
    public static String getCurrentTimestamp() {
        return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    @Nonnull
    public static String readExternalFileContent(Context context, String fileName)
            throws IOException{
        java.io.File file = new java.io.File(context.getExternalFilesDir(null), fileName);
        StringBuilder result = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;

        while ((line = br.readLine()) != null) {
            result.append(line);
            result.append('\n');
        }
        br.close();

        return result.toString();
    }

    @Nullable
    public static java.io.File createExternalFileContent(Context context, String fileName, String content)
            throws IOException {
        if (!isExternalStorageWritable()) {
            return null;
        }

        java.io.File file = new java.io.File(context.getExternalFilesDir(null), fileName);
        FileOutputStream outputStream = new FileOutputStream(file);
        outputStream.write(content.getBytes());
        outputStream.close();

        return file;
    }

    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /**
     * helper class for Drive API operations
     * all public methods are executed SYNCHRONOUSLY, and must be called in background thread
     */
    static class DriveApiUtils {

        @Nullable
        private static FileList getFileList(Drive service, String q)
                throws UserRecoverableAuthIOException, GoogleJsonResponseException {
            FileList fileList = null;
            try {
                Drive.Files.List list = service.files().list().setSpaces("drive");
                if (q != null && q.length() > 0) {
                    list.setQ(q);
                }
                fileList = list.execute();
            } catch (UserRecoverableAuthIOException | GoogleJsonResponseException e) {
                throw e;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return fileList;
        }

        @Nonnull
        public static String getSharedWithMeFolder(Drive service, String folderName)
                throws UserRecoverableAuthIOException, GoogleJsonResponseException {
            String folderId = "";
            FileList fileList = getFileList(service, "sharedWithMe = true");
            if (fileList != null) {
                for (File file : fileList.getFiles()) {
                    if (file.getName().equals(folderName)) {
                        folderId = file.getId();
                        break;
                    }
                }
            }
            return folderId;
        }

        @Nonnull
        public static ArrayList<String> getSharedWithMeFileList(Drive service, String folderId)
                throws UserRecoverableAuthIOException, GoogleJsonResponseException {
            String q = String.format("'%s' in parents", folderId);
            FileList fileList = getFileList(service, q);

            ArrayList<String> nameList = new ArrayList<>();
            if (fileList != null) {
                int totalFiles = fileList.getFiles().size();
                nameList = new ArrayList<>(totalFiles);
                for (int i = 0; i < totalFiles; i++) {
                    String fileName = fileList.getFiles().get(i).getName();
                    nameList.add(fileName);
                }
            }
            return nameList;
        }

        @Nonnull
        public static String getFileId(Drive service, String folderId, String fileName)
                throws UserRecoverableAuthIOException, GoogleJsonResponseException {
            String q = String.format("'%s' in parents and name='%s'", folderId, fileName);
            FileList fileList = getFileList(service, q);
            if (fileList != null && !fileList.getFiles().isEmpty()) {
                return fileList.getFiles().get(0).getId();
            }
            return "";
        }

        @Nullable
        public static String readFileContent(Drive service, @NonNull String fileId) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                service.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
                return convertInputStreamToString(is);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Nullable
        private static String convertInputStreamToString(@Nonnull InputStream inputStream) {
            try {
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                return result.toString("UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Nullable
        public static File updateFile(Drive service, @Nonnull String fileId, @Nonnull java.io.File newContent)
                throws UserRecoverableAuthIOException, GoogleJsonResponseException {
            File fileMetadata = new File();
            fileMetadata.setName(newContent.getName());
            FileContent mediaContent = new FileContent("text/plain", newContent);
            try {
                return service.files().update(fileId, fileMetadata, mediaContent)
                        .setFields("id")
                        .execute();
            } catch (UserRecoverableAuthIOException | GoogleJsonResponseException e) {
                throw e;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Nullable
        public static File createNewFile(Drive service, @Nonnull String folderId, @Nonnull java.io.File file)
                throws UserRecoverableAuthIOException, GoogleJsonResponseException {
            File fileMetadata = new File();
            fileMetadata.setName(file.getName());
            fileMetadata.setParents(Collections.singletonList(folderId));
            FileContent mediaContent = new FileContent("text/plain", file);
            try {
                return service.files().create(fileMetadata, mediaContent)
                        .setFields("id, parents")
                        .execute();
            } catch (UserRecoverableAuthIOException | GoogleJsonResponseException e) {
                throw e;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
