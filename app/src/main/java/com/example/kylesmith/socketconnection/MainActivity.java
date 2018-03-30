package com.example.kylesmith.socketconnection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.VideoView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    // Kyle Smith
    public ImageView mImageView;
    public VideoView mVideoView;
    public String mediaPath;
    public Bitmap bitmap;
    public String videoPath;
    public String ipHost = "10.0.2.2";
    public int port = 8085;
    public byte[] imageBytes;

    private static final int IMAGE_PICKER_REQUEST = 1;
    private static final int VIDEO_PICKER_REQUEST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = (ImageView) findViewById(R.id.image);
        mVideoView = (VideoView) findViewById(R.id.video);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Kyle Smith
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedMediaUri = data.getData();

            if (selectedMediaUri.toString().contains("image")) {
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedMediaUri);
                    mImageView.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mediaPath = selectedMediaUri.getPath();
            } else if (selectedMediaUri.toString().contains("video")) {
                try {
                    videoPath = getRealPathFromURI(this, selectedMediaUri);
                    mVideoView.setVideoPath(selectedMediaUri.toString());
                    mVideoView.requestFocus();
                    mVideoView.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void checkForPhotoOnClick(View view) {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");
        startActivityForResult(pickIntent, IMAGE_PICKER_REQUEST);
    }

    public void checkForVideoOnClick(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        intent.setType("video/*");
        startActivityForResult(intent , VIDEO_PICKER_REQUEST);
    }

    public void attemptToSendPhotoOnClick(View view) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.execute();
    }

    public void attemptToSendVideoOnClick(View view) {
        SendVideo sendVideo = new SendVideo();
        sendVideo.execute();
    }

    @SuppressLint("StaticFieldLeak")
    public class SendPhoto extends AsyncTask<Void, Void, Void> {
        // Kyle Smith
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                InetAddress serverAddr = InetAddress.getByName(ipHost);
                Socket socket = new Socket(serverAddr, port);
                imageBytes = getBytesFromBitmap(bitmap);
                OutputStream outputStream = socket.getOutputStream();

                outputStream.write(imageBytes);
                outputStream.flush();
                socket.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class SendVideo extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            Socket sock = null;

            try{
                System.out.println("Connecting...");
                InetAddress serverAddr = InetAddress.getByName(ipHost);
                sock = new Socket(serverAddr, port);
                InputStream is = new FileInputStream(new File(videoPath));
                byte[] bytes = new byte[1024];

                OutputStream stream = sock.getOutputStream();

                int count = is.read(bytes, 0, 1024);
                while (count != -1){
                    stream.write(bytes, 0, 1024);
                    count = is.read(bytes, 0, 1024);
                }

                is.close();
                stream.close();
                sock.close();

            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }
    }

    public byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        return stream.toByteArray();
    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
