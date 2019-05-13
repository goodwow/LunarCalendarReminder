package com.young.lunarcalendarreminder;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    public static final String[] EVENT_PROJECTION = new String[]{
            CalendarContract.Calendars._ID,                           // 0
            CalendarContract.Calendars.ACCOUNT_NAME,                  // 1
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 2
            CalendarContract.Calendars.OWNER_ACCOUNT                  // 3
    };

    // The indices for the projection array above.
    private static final int PROJECTION_ID_INDEX = 0;
    private static final int PROJECTION_ACCOUNT_NAME_INDEX = 1;
    private static final int PROJECTION_DISPLAY_NAME_INDEX = 2;
    private static final int PROJECTION_OWNER_ACCOUNT_INDEX = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //查询日历
        queryCalendar();

        //添加日历
        Button btnAddCalendar = (Button) findViewById(R.id.btnAddCalendar);
        btnAddCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = (EditText) findViewById(R.id.txtName);
                String calendarName = editText.getText().toString();
                if (!calendarName.equals("")) {
                    if (calendarName.contains("生日")) {
                        addCalendar(calendarName, Color.RED);
                    } else {
                        addCalendar(calendarName, Color.YELLOW);
                    }
                    queryCalendar();
                }
            }
        });

        //删除日历
        Button btnDelCalendar = (Button) findViewById(R.id.btnDelCalendar);
        btnDelCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = (EditText) findViewById(R.id.txtName);
                if (!editText.getText().toString().equals("")) {
                    deleteCalendar(editText.getText().toString());
                    queryCalendar();
                }
            }
        });

        //添加月提醒
        Button btnAddMonthEvent = (Button) findViewById(R.id.btnAddMonthEvent);
        btnAddMonthEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText txtName = (EditText) findViewById(R.id.txtEventName);
                EditText txtDate = (EditText) findViewById(R.id.txtDate);
                EditText txtNumber = (EditText) findViewById(R.id.txtCalendarId);
                String desc = txtName.getText().toString();
                String date = txtDate.getText().toString();
                String number = txtNumber.getText().toString();
                if (!desc.equals("") && !date.equals("") && !number.equals("")) {
                    String[] temp = date.split("\\.");
                    if (temp.length == 2) {
                        addMonthEvent(Long.valueOf(number), desc, Integer.valueOf(temp[1]));
                        Toast tot = Toast.makeText(MainActivity.this, "操作成功", Toast.LENGTH_LONG);
                        tot.show();
                    }
                }
            }
        });

        //添加年提醒
        Button btnAddYearEvent = (Button) findViewById(R.id.btnAddYearEvent);
        btnAddYearEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText txtName = (EditText) findViewById(R.id.txtEventName);
                EditText txtDate = (EditText) findViewById(R.id.txtDate);
                EditText txtNumber = (EditText) findViewById(R.id.txtCalendarId);
                String desc = txtName.getText().toString();
                String date = txtDate.getText().toString();
                String number = txtNumber.getText().toString();
                if (!desc.equals("") && !date.equals("") && !number.equals("")) {
                    String[] temp = date.split("\\.");
                    if (temp.length == 2) {
                        addYearEvent(Long.valueOf(number), desc, Integer.valueOf(temp[0]), Integer.valueOf(temp[1]));
                        Toast tot = Toast.makeText(MainActivity.this, "操作成功", Toast.LENGTH_LONG);
                        tot.show();
                    }
                }
            }
        });
    }

    private void addYearEvent(Long calId, String title, int month, int day) {
        Lunar lunar = new Lunar();
        lunar.lunarYear = 2018;
        lunar.lunarMonth = month;
        lunar.lunarDay = day;
        for (int i = 0; i < 10; i++) {
            addEvent(calId, title, lunar);
            lunar.lunarYear++;
        }
    }

    private void addMonthEvent(Long calId, String title, int day) {
        Lunar lunar = new Lunar();
        lunar.lunarYear = 2018;
        lunar.lunarMonth = 9;
        lunar.lunarDay = day;
        for (int i = 1; i < 120; i++) {
            lunar.lunarMonth++;
            if (lunar.lunarMonth > 12) {
                lunar.lunarMonth = 1;
                lunar.lunarYear++;
            }
            addEvent(calId, title, lunar);
        }
    }

    private void addEvent(Long calId, String title, Lunar lunar) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Solar solar = LunarSolarConverter.LunarToSolar(lunar);
        Calendar beginTime = Calendar.getInstance();
        beginTime.set(solar.solarYear, solar.solarMonth - 1, solar.solarDay, 5, 0);    //注意，月份的下标是从0开始的
        long startMillis = beginTime.getTimeInMillis();    //插入日历时要取毫秒计时
        Calendar endTime = Calendar.getInstance();
        endTime.set(solar.solarYear, solar.solarMonth - 1, solar.solarDay, 6, 0);
        Long endMillis = endTime.getTimeInMillis();

        //插入日程
        ContentValues eValues = new ContentValues();  //插入事件
        eValues.put(CalendarContract.Events.DTSTART, startMillis);
        eValues.put(CalendarContract.Events.DTEND, endMillis);
        eValues.put(CalendarContract.Events.TITLE, title);
        eValues.put(CalendarContract.Events.DESCRIPTION, "");
        eValues.put(CalendarContract.Events.CALENDAR_ID, calId);
        eValues.put(CalendarContract.Events.EVENT_LOCATION, "");
        eValues.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
        Uri uri = getContentResolver().insert(CalendarContract.Events.CONTENT_URI, eValues);

        //插完日程之后必须再插入以下代码段才能实现提醒功能
        ContentValues rValues = new ContentValues();  //插入提醒，与事件配合起来才有效
        rValues.put("event_id", uri.getLastPathSegment());
        rValues.put("minutes", 10);    //提前10分钟提醒
        rValues.put("method", 1);    //如果需要有提醒,必须要有这一行
        getContentResolver().insert(CalendarContract.Reminders.CONTENT_URI, rValues);
    }

    public void deleteAllEvent(String calId) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        getContentResolver().delete(CalendarContract.Events.CONTENT_URI, CalendarContract.Events.CALENDAR_ID + " =? ", new String[]{calId});
    }

    private void queryCalendar() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Toast tot = Toast.makeText(MainActivity.this, "需要开启操作日历权限", Toast.LENGTH_LONG);
            tot.show();
            return;
        }

        List<String> data = new ArrayList<>();
        Cursor cur = getContentResolver().query(CalendarContract.Calendars.CONTENT_URI, EVENT_PROJECTION, null, null, null);
        while (cur.moveToNext()) {
            Long calID1 = 0L;
            String displayName = null;
            String accountName = null;
            String ownerName = null;

            calID1 = cur.getLong(PROJECTION_ID_INDEX);
            displayName = cur.getString(PROJECTION_DISPLAY_NAME_INDEX);
            accountName = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX);
            ownerName = cur.getString(PROJECTION_OWNER_ACCOUNT_INDEX);

            data.add(calID1 + "   " + displayName);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, data);
        ListView listView = (ListView) findViewById(R.id.lvCalendar);
        listView.setAdapter(adapter);
    }

    private long addCalendar(String name, int color) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return -1;
        }

        String CALENDARS_NAME = name;
        String CALENDARS_ACCOUNT_NAME = name;
        String CALENDARS_ACCOUNT_TYPE = CalendarContract.ACCOUNT_TYPE_LOCAL;
        String CALENDARS_DISPLAY_NAME = name;

        TimeZone timeZone = TimeZone.getDefault();
        ContentValues value = new ContentValues();
        value.put(CalendarContract.Calendars.NAME, CALENDARS_NAME);

        value.put(CalendarContract.Calendars.ACCOUNT_NAME, CALENDARS_ACCOUNT_NAME);
        value.put(CalendarContract.Calendars.ACCOUNT_TYPE, CALENDARS_ACCOUNT_TYPE);
        value.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CALENDARS_DISPLAY_NAME);
        value.put(CalendarContract.Calendars.VISIBLE, 1);
        value.put(CalendarContract.Calendars.CALENDAR_COLOR, color);

        value.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
        value.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
        value.put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, timeZone.getID());
        value.put(CalendarContract.Calendars.OWNER_ACCOUNT, CALENDARS_ACCOUNT_NAME);
        value.put(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND, 0);

        Uri calendarUri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, CALENDARS_ACCOUNT_NAME)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CALENDARS_ACCOUNT_TYPE)
                .build();

        Uri result = getContentResolver().insert(calendarUri, value);
        long id = result == null ? -1 : ContentUris.parseId(result);
        return id;
    }

    private void updateCalendar(long calID) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(CalendarContract.Calendars.NAME, "默认日历");
        values.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "默认日历");
        values.put(CalendarContract.Calendars.CALENDAR_COLOR, Color.GREEN);
        Uri updateUri = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calID);
        int rows = getContentResolver().update(updateUri, values, null, null);
    }

    private void deleteCalendar(String calId) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        getContentResolver().delete(CalendarContract.Calendars.CONTENT_URI, CalendarContract.Calendars._ID + " =? ", new String[]{calId});
    }
}
