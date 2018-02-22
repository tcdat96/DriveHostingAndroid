package com.example.cpu10661.hostdrivedemo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static android.content.ContentValues.TAG;

class Utils {
    @Nonnull
    static String getCurrentTimestamp() {
        return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    static void showOkDialog(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    static class DriveApiUtils {
        /**
         * this method is synchronous and must be called in background thread
         *
         * @return the destination folder ID
         */
        @Nonnull
        static String getSharedWithMeFolder(Drive service, String email) throws UserRecoverableAuthIOException {
            String folderId = "";
            FileList fileList = getFileList(service, "sharedWithMe = true");
            if (fileList != null) {
                for (File file : fileList.getFiles()) {
                    if (file.getName().equals(email)) {
                        folderId = file.getId();
                        break;
                    }
                }
            }
            return folderId;
        }

        /**
         * this method is synchronous and must be called in background thread
         *
         * @param folderId the specified folder's ID
         * @return a list of specified folder's children files
         */
        @Nonnull
        static ArrayList<String> getSharedWithMeFileList(Drive service, String folderId) throws UserRecoverableAuthIOException {
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
        static String getFileId(Drive service, String folderId, String fileName) throws UserRecoverableAuthIOException  {
            String q = String.format("'%s' in parents and name='%s'", folderId, fileName);
            FileList fileList = getFileList(service, q);
            if (fileList != null && !fileList.getFiles().isEmpty()) {
                return fileList.getFiles().get(0).getId();
            }
            return "";
        }

        /**
         * this method is synchronous and must be called in background thread
         *
         * @param q query string
         * @return FileList result of specified query string
         */
        @Nullable
        private static FileList getFileList(Drive service, String q) throws UserRecoverableAuthIOException {
            FileList fileList = null;
            try {
                Drive.Files.List list = service.files().list().setSpaces("drive");
                if (q != null && q.length() > 0) {
                    list.setQ(q);
                }
                fileList = list.execute();
            } catch (UserRecoverableAuthIOException e) {
                throw e;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return fileList;
        }

        @Nullable
        static String readFileContent(Drive service, @NonNull String fileId) {
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
    }
}
