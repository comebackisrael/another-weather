package weather;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    Handler handler;
    TextView city;
    TextView temp;
    TextView sky;
    TextView gradus;
    TextView data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(weather.R.layout.activity_main);
        handler = new Handler();
        Toolbar toolbar = (Toolbar) findViewById(weather.R.id.toolbar);
        setSupportActionBar(toolbar);
        temp = (TextView) findViewById(weather.R.id.temperature);
        temp.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/HelveticaNeueCyr-UltraLight.otf"));
        sky = (TextView) findViewById(weather.R.id.sky);
        sky.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/weather.ttf"));
        gradus = (TextView) findViewById(weather.R.id.gradus);
        gradus.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/HelveticaNeueCyr-UltraLight.otf"));
        gradus.setText("\u00b0C");
        city = (TextView) findViewById(weather.R.id.city);
        data = (TextView) findViewById(weather.R.id.data);
        updateWeatherData(new CityPreference(MainActivity.this).getCity());
    }

    //меню
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(weather.R.menu.main_menu, menu);
        return true;
    }

    //обработка нажатия пункта меню
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == weather.R.id.action_settings) showInputDialog();
        return true;
    }

    //показать диалог выбора города
    private void showInputDialog() {
        AlertDialog.Builder chooseCity = new AlertDialog.Builder(this);
        chooseCity.setIcon(weather.R.mipmap.ic_launcher);
        chooseCity.setTitle(weather.R.string.choose_city);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        chooseCity.setView(input);
        chooseCity.setPositiveButton(getResources().getString(weather.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String city = input.getText().toString();
                updateWeatherData(city);
                new CityPreference(MainActivity.this).setCity(city);
            }
        });
        chooseCity.show();
    }
    private static final String OPEN_WEATHER_MAP_API = "http://api.openweathermap.org/data/2.5/weather?q=%s&units=metric";

    //делает запрос на сервер и получает от него данные
    //Возвращает объект JSON или null
    public static JSONObject getJSONData (Context context, String city){
        try{
            URL url = new URL (String.format(OPEN_WEATHER_MAP_API, city));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("x-api-key", context.getString(weather.R.string.open_weather_maps_app_id));
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuffer json = new StringBuffer(1024);
            String tmp = "";
            while ((tmp = bufferedReader.readLine()) != null)
                json.append(tmp).append("\n");
            bufferedReader.close();
            JSONObject data = new JSONObject(json.toString());
            if (data.getInt("cod") != 200)  return null;
            return data;
        } catch (Exception e) {
            return null;
        }
    }
    //Обновление/загрузка погодных данных
    private void updateWeatherData(final String city) {
        new Thread() {
            public void run() {
                final JSONObject json = getJSONData(MainActivity.this, city);
                if (json == null) {
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, MainActivity.this.getString(weather.R.string.place_not_found),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    handler.post(new Runnable() {
                        public void run() {
                            renderWeather(json);
                        }
                    });
                }
            }
        }.start();
    }

    //Обработка загруженных данных
    private void renderWeather(JSONObject json) {
        try {
            city.setText(json.getString("name").toUpperCase(Locale.US) + ", "
                    + json.getJSONObject("sys").getString("country"));
            JSONObject details = json.getJSONArray("weather").getJSONObject(0);
            JSONObject main = json.getJSONObject("main");
            temp.setText(String.format("%.1f", main.getDouble("temp")));
            DateFormat df = DateFormat.getDateTimeInstance();
            String updatedOn = df.format(new Date(json.getLong("dt") * 1000));
            data.setText(getResources().getString(weather.R.string.last_update) + " " + updatedOn);
            setWeatherIcon(details.getInt("id"), json.getJSONObject("sys").getLong("sunrise") * 1000,
                    json.getJSONObject("sys").getLong("sunset") * 1000);
        } catch (Exception e) {
            Log.e("Weather", "One or more fields not found in the JSON data");
        }
    }

    //Подстановка нужной иконки
    private void setWeatherIcon(int actualId, long sunrise, long sunset) {
        int id = actualId / 100;
        String icon = "";
        if (actualId == 800) {
            long currentTime = new Date().getTime();
            if (currentTime >= sunrise && currentTime < sunset) {
                icon = MainActivity.this.getString(weather.R.string.weather_sunny);
            } else {
                icon = MainActivity.this.getString(weather.R.string.weather_clear_night);
            }
        } else {
            Log.d("SimpleWeather", "id " + id);
            switch (id) {
                case 2:
                    icon = MainActivity.this.getString(weather.R.string.weather_thunder);
                    break;
                case 3:
                    icon = MainActivity.this.getString(weather.R.string.weather_drizzle);
                    break;
                case 5:
                    icon = MainActivity.this.getString(weather.R.string.weather_rainy);
                    break;
                case 6:
                    icon = MainActivity.this.getString(weather.R.string.weather_snowy);
                    break;
                case 7:
                    icon = MainActivity.this.getString(weather.R.string.weather_foggy);
                    break;
                case 8:
                    icon = MainActivity.this.getString(weather.R.string.weather_cloudy);
                    break;
            }
        }
        sky.setText(icon);
    }
}
