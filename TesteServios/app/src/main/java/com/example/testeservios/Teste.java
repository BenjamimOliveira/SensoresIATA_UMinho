package com.example.testeservios;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Calendar;
import com.android.volley.toolbox.Volley;
import static android.provider.ContactsContract.CommonDataKinds.Website.URL;

public class Teste extends Service {

    private static Timer timer = new Timer();
    // -- Sensor vars
    SensorManager sm;
    SensorEventListener listener;
    Sensor light;
    int value_sensor;
    // -- enviar bd
    Boolean enviaParaBD;

    public Teste() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        System.out.println("Levou bind, se chegar aqui é milagre");
        return null;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        enviaParaBD = true;
        System.out.println("Serviço Criado!!!");
        // inicializar sensores
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        light = sm.getDefaultSensor(Sensor.TYPE_LIGHT);

        listener = new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                System.out.println("Accuracy changed, though luck bud");
            }
            @Override
            public void onSensorChanged(SensorEvent event) {

                try{
                    System.out.println("\tNEW VALUE: "+event.values[0]);
                    value_sensor = (int) event.values[0];
                }catch (Exception ex){
                }

            }
        };
        sm.registerListener(listener, light, SensorManager.SENSOR_DELAY_NORMAL);

        // inicializar timer
        timer.scheduleAtFixedRate(new getDataTimer(), 0, minutes2milli(10));
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        enviaParaBD = false;
        System.out.println("Serviço destruido!!!");
        sm.unregisterListener(listener);
    }

    private Integer minutes2milli(int minutes){
        return minutes*60*1000;
    }

    private class getDataTimer extends TimerTask {

        @Override
        public void run() {
            Date date = Calendar.getInstance().getTime();
            System.out.println("Value: " + value_sensor + " --- Time: " + Calendar.getInstance().getTime());
            try {
                dataToServer2(value_sensor, date);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String dateFormat(Date date){
        // 2020-11-13T22:25:05.195Z
        String formatDate = ( new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" ) ).format( date );
        formatDate = StringUtils.substringBefore(formatDate, ".");
        formatDate = formatDate.replace("T", "%20");
        return formatDate;
    }

    private void dataToServer(int sensorValue, Date date) throws IOException {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="http://iatauminho2021.000webhostapp.com/rest/put.php?";
        String valor = "valor=" + sensorValue;
        String hora = "&hora=" + dateFormat(date);
        String request = url + valor + hora;
        // http://iatauminho2021.000webhostapp.com/rest/put.php?valor=16&hora=2020-11-12%2010:23:20
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        System.out.println(response);
                        System.out.println(request);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("Não enviado");
                error.printStackTrace();
            }
        });
        queue.add(stringRequest);
    }

    private void dataToServer2(int sensorValue, Date date) throws IOException {
        if (!enviaParaBD) return;
        String url ="http://iatauminho2021.000webhostapp.com/rest/put.php?";
        String valor = "valor=" + sensorValue;
        String hora = "&hora=" + dateFormat(date);
        String request = url + valor + hora;
        java.net.URL url1 = new URL(request);
        HttpURLConnection urlConnection = (HttpURLConnection) url1.openConnection();
        try {
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            readStream(in);
            System.out.println("Dado inserido na database" + in);
        } finally {
            urlConnection.disconnect();
        }


    }

    private String readStream(InputStream is) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            int i = is.read();
            while(i != -1) {
                bo.write(i);
                i = is.read();
            }
            return bo.toString();
        } catch (IOException e) {
            return "";
        }
    }

}