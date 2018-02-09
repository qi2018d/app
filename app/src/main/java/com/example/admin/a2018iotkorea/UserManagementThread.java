package com.example.admin.a2018iotkorea;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Created by p on 2/4/2018.
 */

public class UserManagementThread extends AsyncTask<String, Void, String> {

    Context connContext;
    String resultCode;

    //Http connection thread constructor
    public  UserManagementThread(Context context) {
         connContext = context;
         resultCode = null;
    }

    //Actual data sent as params here
    //Use the JSON object to send data --> which includes the url,
    @Override
    protected String doInBackground(String... strings) {
        HttpURLConnection urlConnection;
        String data = strings[1];
        try {
            //Connect
            urlConnection = (HttpURLConnection) ((new URL(strings[0]).openConnection()));
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setReadTimeout(10000 /*milliseconds*/);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            urlConnection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            urlConnection.setRequestMethod("POST");

            //Write
            OutputStream outputStream = urlConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            writer.write(data);
            writer.close();
            outputStream.close();

            //Read
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));

            String line = null;
            StringBuilder sb = new StringBuilder();

            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }

            bufferedReader.close();
            resultCode = sb.toString();

        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultCode;
    }

    @Override
    protected void onPostExecute(String result) {
        Toast.makeText(connContext,"resultCode: " + resultCode , Toast.LENGTH_SHORT).show();
//        if(resultCode.contains("100"))
//        {
//            Toast.makeText(connContext,"Welcome!!!", Toast.LENGTH_SHORT).show();
////            Intent it_backActivity = new Intent(connContext, MainActivity.class);
////            it_backActivity.putExtra("ok", id);
//        }
//        else {
//            Toast.makeText(connContext,"ID or Pw is wrong!!!", Toast.LENGTH_SHORT).show();
//        }

    }


}
