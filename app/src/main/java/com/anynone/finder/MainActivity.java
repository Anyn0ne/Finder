package com.anynone.finder;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Layout;
import android.transition.Visibility;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.SharedPreferences;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.Constraints;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.nio.channels.FileChannel;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.CacheControl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;


public class MainActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private static final String TAG = MainActivity.class.getName();
    private Button btnRequest;
    private String app_token="yourapptoken";//app token (see freebox documentation)
    private RequestQueue mRequestQueue;
    private JsonObjectRequest[] mJsonRequests;
    private String url = "https://urltoyourfreebox:portnumber/"; //url to your freebox
    private String salt;
    private String appSelect = "";
    private String challenge;
    private String session_tok;
    private JSONObject connexion=new JSONObject();
    private HashMap utilisateurs;
    private HashMap connectes = new HashMap();
    Window window;
    Handler mHandler = new Handler();
    Runnable mHandlerTask = new Runnable() {
        @Override
        public void run() {
            new GetApps().execute();
            mHandler.postDelayed(this, 1000 * 2);
            window.setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.gray));
        }
    };





    /***
     *
     * @param contactNumber numéro de téléphone du contact ciblé
     * @param context contexte de l'app
     * @return identifiant du contact
     */
    Button btn;
    ImageView image;
    String id;


    private final static char[] hexArray = "0123456789abcdef".toCharArray();

    HttpURLConnection connection = null;
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static String hmacSha1(String appToken, String challenge) {
        try {
            byte[] keyBytes = appToken.getBytes("UTF-8");
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);

            byte[] rawHmac = mac.doFinal(challenge.getBytes("UTF-8"));

            // TODO Consider replacing with a simple hex encoder so we don't need commons-codec
            String hexBytes = bytesToHex(rawHmac);

            return hexBytes;

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private String get_chall(String url) throws IOException, JSONException {
        JSONObject reponse = new JSONObject(get_request(url));
        reponse = reponse.getJSONObject("result");
        return reponse.getString("challenge");
    }
    private  String get_sess_tok(String url, String json) throws IOException, JSONException {
        Log.d(TAG, "get_sess_tok: "+post_request(url, json));
        JSONObject reponse = new JSONObject(post_request(url, json));
        reponse = reponse.getJSONObject("result");
        return reponse.getString("session_token");
    }
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }



    /*private void get_sess(){

        int nbReq = 2;
        mRequestQueue = Volley.newRequestQueue(this);
        final HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,;q=0.8");
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Accept-Language", "en-US,en;q=0.5");
        headers.put("Host", "mafreebox.freebox.fr");
        headers.put("Upgrade-Insecure-Requests", "1");
        headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:98.0) Gecko/20100101 Firefox/98.0");
        headers.put("Via", "1.1 192.168.1.44");
        headers.put("X-Forwarded-For", "192.168.1.44");

        String url = "http://mafreebox.freebox.fr/api/v8/login/";
        //String Request initialized
        final HashMap<String, String> data = new LinkedHashMap<String, String>();


        mJsonRequests = new JsonObjectRequest[nbReq];




        /*mJsonRequests[0] = new JsonObjectRequest(Request.Method.GET, url, new JSONObject(headers), new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject reponsedec= response.getJSONObject("result");
                    challenge=reponsedec.getString("challenge");
                    Log.d(TAG, "onResponse: "+challenge);
                    Log.d(TAG, "onResponse: "+hmacSha1(app_token, challenge));
                    data.put("app_id", "fr.anynone.finder");
                    data.put("password", hmacSha1(app_token, challenge));
                    connexion.put("app_id", "fr.anynone.finder");
                    connexion.put("password", hmacSha1(app_token, challenge));
                    Log.d(TAG, "onResponse: "+connexion.toString());


                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Toast.makeText(getApplicationContext(),"Response :" + response, Toast.LENGTH_LONG).show();//display the response on screen


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG,"Error :" + error.toString());
            }
        });

        mJsonRequests[1] = new JsonObjectRequest(Request.Method.POST, "http://mafreebox.freebox.fr/api/v8/login/session/", connexion, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.d(TAG, "log_tok: "+connexion);
                    JSONObject reponsedec= response.getJSONObject("result");
                    session_tok=reponsedec.getString("session_token");
                    Log.d(TAG, "session_tok: "+session_tok);


                } catch (JSONException e) {
                    Log.d(TAG, "log_tok: "+connexion);

                    e.printStackTrace();
                }

                Toast.makeText(getApplicationContext(),"Response :" + response, Toast.LENGTH_LONG).show();//display the response on screen


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "onResponseError: "+connexion);
                Log.i(TAG,"Error :" + error.toString());
            }
        });




        mRequestQueue.add(mJsonRequests[0]);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "run: OOOO");
                mRequestQueue.add(mJsonRequests[1]);
            }
        }, 800);


    }*/
    /*public JSONObject getJSONObjectFromURL(String urlString) throws IOException, JSONException {
        HttpURLConnection urlConnection = null;
        URL url = new URL(urlString);
        urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Content-Type", "application/json; utf-8");
        urlConnection.setRequestProperty("Accept", "application/json");
        String cos=connexion.toString();
        urlConnection.setChunkedStreamingMode(0);
        urlConnection.setReadTimeout(10000 /* milliseconds  );
        urlConnection.setConnectTimeout(15000 /* milliseconds  );

        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);

        StringBuilder output = new StringBuilder();
        output.append("data=");
        //noinspection deprecation
        String enco=URLEncoder.encode(connexion.toString(),"UTF-8");
        output.append(enco.toCharArray());

        Log.i(TAG, "getJSONObjectFromURL: " + cos.getBytes("UTF-8"));
        BufferedOutputStream stream = new BufferedOutputStream(urlConnection.getOutputStream());
        stream.write(cos.getBytes("UTF-8"));
        stream.close();

        urlConnection.connect();

        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }
        br.close();

        String jsonString = sb.toString();
        System.out.println("JSON: " + jsonString);

        return new JSONObject(jsonString);
    }*/

    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    OkHttpClient client = new OkHttpClient();
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    void parseUsers(JSONObject appareils) throws JSONException {
        final TextView textViewToChange = (TextView) findViewById(R.id.appareilsCo);

        JSONArray array = appareils.getJSONArray("result");
        Window window = this.getWindow();

        Log.d(TAG, "------------------------------------");
        LinearLayout layout = findViewById(R.id.sltAppCo);
        LinearLayout mainLayout = findViewById(R.id.apps);
        Map users = getSharedPreferences("FinderPREFERENCES", MODE_PRIVATE).getAll();
        Log.i(TAG, "parseUsers: "+users);
        layout.removeAllViews();
        mainLayout.removeAllViews();
        for (int i = 0; i < array.length(); i++) {
            JSONObject objet = array.getJSONObject(i);
            if (objet.has("host")){
                JSONObject host = objet.getJSONObject("host");
                if (host.has("primary_name")){
                    String nomApp=host.getString("primary_name");
                    Log.d(TAG, "parseUsers: "+nomApp);
                    crtrl(nomApp, "", R.id.sltAppCo, R.color.btn_inactive, true);
                    if ((users.containsKey(nomApp))){
                        String content = users.get(nomApp).toString();
                        JSONObject json = new JSONObject(content);
                        crtrl(json.getString("name"), json.getString("picture"), R.id.apps, R.color.gray, false);

                    }
                }
            }

        }

    }
    int dp(float c){
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float dp = c;
        float fpixels = metrics.density * dp;
        int pixels = (int) (fpixels + 0.5f);
        return pixels;

    }
    @SuppressLint({"ResourceType", "NewApi"})
    void crtrl(String name, String image, int id, int bgcolor, boolean click){


        RelativeLayout layout = new RelativeLayout(this);
        CardView cardView = new CardView(this);
        ImageView imageView = new ImageView(this);
        TextView textView = new TextView(this);




        textView.setTextColor(getResources().getColor(R.color.white));
        textView.setTextSize(18);//unité par défaut en dp et non en pixels !!
        textView.setText(name);

        LayoutParams tlp= new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );

        tlp.leftMargin=dp(90);
        tlp.addRule(RelativeLayout.CENTER_VERTICAL);
        tlp.addRule(RelativeLayout.ALIGN_PARENT_START);

        textView.setLayoutParams(tlp);



        imageView.setId('1');
        if(image != ""){
            Bitmap bitmap = BitmapFactory.decodeFile(image);
            imageView.setImageBitmap(bitmap);

        }
        else{
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_baseline_devices_24);
            imageView.setImageBitmap(bitmap);
            imageView.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.gray)));
        }


        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        tlp= new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );
        tlp.leftMargin=0;
        tlp.width=dp(50);
        tlp.height=dp(50);
        tlp.removeRule(RelativeLayout.CENTER_VERTICAL);
        tlp.removeRule(RelativeLayout.ALIGN_PARENT_START);
        imageView.setLayoutParams(tlp);


        LinearLayout linearLayout = findViewById(id);
        tlp= new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );

        cardView.setRadius(dp(20));
        cardView.setCardElevation(dp(10));


        tlp.addRule(RelativeLayout.CENTER_VERTICAL);
        tlp.leftMargin=dp(20);


        cardView.setLayoutParams(tlp);


        tlp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);




        tlp.height = dp(90);
        tlp.width=dp(349);
        tlp.leftMargin=dp(30);
        tlp.topMargin=dp(20);
        layout.setGravity(Gravity.LEFT);
        layout.setBackground(getResources().getDrawable(R.drawable.radius_border));
        layout.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(bgcolor)));
        layout.setLayoutParams(tlp);
        cardView.addView(imageView);
        layout.addView(cardView);
        layout.addView(textView);

        linearLayout.addView(layout);

        if(click){
            layout.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    appSelect=textView.getText().toString();
                    Log.i(TAG, "onClick: "+appSelect);
                    if((textView.getText().toString()).equals(appSelect)){
                        int childcount = linearLayout.getChildCount();
                        for (int i=0; i < childcount; i++){
                            View v = linearLayout.getChildAt(i);
                            v.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.btn_inactive)));
                        }
                        layout.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.blue)));


                    }

                }
            });

        }
        if((textView.getText().toString()).equals(appSelect)){

            layout.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.blue)));

        }
    }




    String post_request(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json); // fonctionne

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .post(body)
                .build();
        okhttp3.Response response = client.newCall(request).execute();
        return response.body().string();
    }

    String get_request(String url)throws IOException {

        okhttp3.Request request = new okhttp3.Request.Builder()
                .cacheControl(new CacheControl.Builder().noCache().build())
                .url(url)
                .build();
        okhttp3.Response response = client.newCall(request).execute();

        return response.body().string();
    }
    String get_request(String url, boolean headers)throws IOException {

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .header("X-Fbx-App-Id", "fr.anynone.finder")
                .header("X-Fbx-App-Auth", session_tok)
                .build();
        okhttp3.Response response = client.newCall(request).execute();

        return response.body().string();
    }
    JSONObject get_appareils(String url) throws JSONException, IOException {
        //"http://mafreebox.freebox.fr/api/v8/wifi/ap/0/stations/?_dc=1647989754110"
        Log.d(TAG, "get_appareils: "+session_tok);



        return new JSONObject(get_request(url, true));
    }

    ActivityResultLauncher<String> sifgr = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(android.net.Uri uri) {
            ImageButton btn= findViewById(R.id.imageButton);
            btn.setImageURI(uri);
            btn.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Log.i(TAG, "onActivityResult: "+getPath(getBaseContext(), uri));
            File image = new File(getPath(getBaseContext(), uri));
            Long tsLong = System.currentTimeMillis();
            String ts = tsLong.toString();
            File dest = new File("/data/data/com.anynone.finder/profilePics/"+ts+"."+getExtension(getPath(getBaseContext(), uri)));
            Button sbmt = findViewById(R.id.button4);
            sbmt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        sharedPreferences = getSharedPreferences("FinderPREFERENCES", MODE_PRIVATE);
                        // Log.i(TAG, "onCreate: "+getSharedPreferences("FinderPREFERENCES", MODE_PRIVATE).getAll());

                        SharedPreferences.Editor editor=sharedPreferences.edit();

                        Long tsLong = System.currentTimeMillis();
                        String ts = tsLong.toString();

                        copyFile(image, dest);//copie du fichier dans un rep propre à l'app.
                        EditText input = findViewById(R.id.editTextTextPersonName);
                        String label = input.getText().toString();
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("name", label);
                        jsonObject.put("picture", dest);
                        editor.putString(appSelect, jsonObject.toString());
                        editor.commit();


                        //shared prefs : -nom appareilchoisi pour le tracking
                        //               -nom attribué
                        //               -url de la photo (dest)
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                    findViewById(R.id.ajouter).setVisibility(View.GONE);
                }
            });

        }

    });
    public static String getExtension(String path)
    {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        return extension;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences("FinderPREFERENCES", MODE_PRIVATE);
       // Log.i(TAG, "onCreate: "+getSharedPreferences("FinderPREFERENCES", MODE_PRIVATE).getAll());

        SharedPreferences.Editor editor=sharedPreferences.edit();
        //editor.putString("jonction", "ragnarok");
      //  editor.commit();



        window = this.getWindow();
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);



        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        setContentView(R.layout.activity_main);



        //get_sess();
        findViewById(R.id.ajouter).setVisibility(View.GONE);
        findViewById(R.id.rfsbtn).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                View appareilsCo = findViewById(R.id.appareilsCo);
                if(appareilsCo.getVisibility()==View.VISIBLE){
                    appareilsCo.setVisibility(View.GONE);
                }
                else{
                    appareilsCo.setVisibility(View.VISIBLE);
                }
                return true;
            }
        });
        findViewById(R.id.imageButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sifgr.launch("image/*");
            }
        });

        findViewById(R.id.rfsbtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.ajouter).setVisibility(view.VISIBLE);

            }
        });


        new GetChall().execute();


        /*Handler handl = new Handler();
        handl.postDelayed(new Runnable() {
            @Override
            public void run() {
                new ReqSess(challenge).execute();
            }
        }, 500);*/





    }
    private class ReqSess extends AsyncTask<Void, Void, Void> {

        private String chal;
        public ReqSess(String chall) {
            chal=chall;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                JSONObject json= new JSONObject();
                json.put("app_id", "fr.anynone.finder");
                Log.d(TAG, "doInBackground: CHAL"+chal);
                json.put("password", hmacSha1(app_token, chal));

                session_tok=get_sess_tok(url+"api/v8/login/session/", json.toString());
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {

            Log.d(TAG, "onPostExecute: "+session_tok);


            mHandlerTask.run();



        }
    }
    private class GetApps extends AsyncTask<Void, Void, Void> {
        JSONObject appareils=new JSONObject();

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                Log.d(TAG, "doInBackground: "+session_tok);
                appareils=get_appareils(url+"api/v8/wifi/ap/0/stations/?_dc=1647989754110");
                Log.d(TAG, "onPostExecuteApp: "+appareils.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid){

            try {
                Log.d(TAG, "run: "+appareils);
                parseUsers(appareils);
                window.setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.black));

            } catch (JSONException e) {
                e.printStackTrace();
                mHandler.removeCallbacks(mHandlerTask);// on arrête l'ancienne loop
                new GetChall().execute();//pour en creer une nouvelle


            }
            // This schedule a runnable task every 2 secs

        }
    }
    private class GetChall extends AsyncTask<Void, Void, Void> {


        @Override
        protected Void doInBackground(Void... voids) {

            try {
                challenge = get_chall(url+"api/v8/login/");
                Log.d(TAG, "doInBackground: " + challenge);
            } catch (IOException | JSONException e) {
                e.printStackTrace();

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            new ReqSess(challenge).execute();
        }
    }
    private class Finder{
        String imagePath;
        String name;
        String deviceName;

    }
    @Override
    public void onBackPressed(){
        findViewById(R.id.ajouter).setVisibility(View.GONE);
    }


}




