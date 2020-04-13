package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {


    ArrayList<String>  articleTitles = new ArrayList<String>();
    ArrayList<String> articleUrls = new ArrayList<String>();
    ArrayAdapter<String> arrayAdapter;

    SQLiteDatabase database;

    public class DownloadTask extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {

                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();

                while (data != -1) {
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }

                JSONArray jsonArray = new JSONArray(result);

                database.execSQL("DELETE FROM news");

                for (int i = 0; i < 12; i++) {

                    String articleId = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");

                    String articleInfo = "";

                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = urlConnection.getInputStream();
                    reader = new InputStreamReader(in);
                    data = reader.read();

                    while (data != -1) {
                        char current = (char) data;
                        articleInfo += current;
                        data = reader.read();
                    }

                    JSONObject jsonObject = new JSONObject(articleInfo);

                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                        String articleUrl = jsonObject.getString("url");
                        String articleTitle = jsonObject.getString("title");

                        //articleTitles.add(articleTitle);
                        //articleUrls.add(articleUrl);
                        //arrayAdapter.notifyDataSetChanged();

                       String sql = "INSERT INTO news (title, url) VALUES (?, ?)";
                       SQLiteStatement statement = database.compileStatement(sql);
                       statement.bindString(1, articleTitle);
                       statement.bindString(2, articleUrl);
                       statement.execute();
                    }

                }
                return result;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView) findViewById(R.id.listView);

        database = this.openOrCreateDatabase("news", MODE_PRIVATE, null);
        database.execSQL("CREATE TABLE IF NOT EXISTS news (title VARCHAR, url VARCHAR)");

        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, articleTitles);
        listView.setAdapter(arrayAdapter);

        updateListView();

        DownloadTask task = new DownloadTask();
        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch (Exception e) {
            e.printStackTrace();
        }




        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), ArticleActivity.class);
                intent.putExtra("url", articleUrls.get(position));
                startActivity(intent);
            }
        });
    }

    public void updateListView() {
        Cursor c = database.rawQuery("SELECT * FROM news", null);

        int titleIndex = c.getColumnIndex("title");
        int urlIndex = c.getColumnIndex("url");

        if(c.moveToFirst()) {
            articleTitles.clear();
            articleUrls.clear();

            do {
                articleTitles.add(c.getString(titleIndex));
                articleUrls.add(c.getString(urlIndex));
            }while(c.moveToNext());

            arrayAdapter.notifyDataSetChanged();
        }
    }
}
