package com.pcom.paratrooper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

public class GraphActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new HistorySurfaceView(this));
    }

    class HistorySurfaceView extends SurfaceView implements SurfaceHolder.Callback{

        Path path;
        private volatile float vHeight = 0;
        private volatile float vWidth = 0;
        private volatile float timeHeight = 50;
        private volatile float zoom = 0.001f;
        private volatile float maxZoom = 1;
        private volatile float minZoom;
        private volatile float deltaX = 0;
        private volatile float deltaY = 0;
        private volatile long dt;
        private volatile float pixelHeight = 0;
        volatile SurfaceHolder surfaceHolder;

        public HistorySurfaceView(Context context) {
            super(context);
            surfaceHolder = getHolder();
            surfaceHolder.addCallback(this);
        }

        class UserHistory implements Comparable<UserHistory>{
            protected String name;
            protected int id;
            protected int size;
            protected Vector<Long> s;
            protected Vector<Long> f;
            boolean halved;
            long buf;
            public UserHistory(int nid){
                name = "";
                id = nid;
                size = 0;
                s = new Vector<>();
                f = new Vector<>();
                halved = false;
                buf = 0;
            }

            public int compareTo(UserHistory a)
            {
                return name.compareTo(a.name);
            }

            public String toString()
            {
                return (id + " " + name);
            }

            public void addS(long t)
            {
                halved = true;
                buf = t;
            }

            public void addF(long t)
            {
                if (halved)
                {
                    halved = false;
                    s.add(buf);
                    f.add(t);
                    size++;
                }
            }
        }

        class DrawingThread extends Thread{

            private SparseArrayCompat<UserHistory> usernames;
            private boolean finished = false;
            private GregorianCalendar today;
            private long now;

            public DrawingThread(Cursor r)
            {
                usernames = new SparseArrayCompat<>();
                int n = r.getCount();
                int idc = r.getColumnIndex(EventsContract.EventEntry.COLUMN_NAME_USER_ID);
                r.moveToFirst();
                for (int i = 0; i < n; i++)
                {
                    int id = r.getInt(idc);
                    UserHistory user = usernames.get(id);
                    if (user == null) {
                        usernames.put(id, new UserHistory(id));
                    }
                    r.moveToNext();
                }
                r.moveToFirst();
                int typec = r.getColumnIndex(EventsContract.EventEntry.COLUMN_NAME_TYPE);
                int timec = r.getColumnIndex(EventsContract.EventEntry.COLUMN_NAME_TIME);
                int extrac = r.getColumnIndex(EventsContract.EventEntry.COLUMN_NAME_EXTRA);
                dt = HistoryService.dt;
                dt /= 1000;
                today = new GregorianCalendar();
                now = new Date().getTime() / 1000;

                for (int i = 0; i < n; i++)
                {
                    int id = r.getInt(idc);
                    UserHistory user = usernames.get(id);
                    int tp = r.getInt(typec);
                    long t = r.getLong(timec);
                    int extra = r.getInt(extrac);
                    t /= 1000;
                    t = t - now + dt;
                    if (tp == 8)
                    {
                        user.addS(t);
                    }
                    else
                    {
                        if (extra == 1)
                            user.addF(t - 60 * 15);
                    }
                    r.moveToNext();
                }

                final StringBuilder userstring = new StringBuilder();
                for (int i = 0; i < usernames.size(); i++)
                {
                    if (i > 0)
                        userstring.append(',');
                    userstring.append(usernames.keyAt(i));
                }
                finished = false;
//                System.out.println(userstring.toString());
                VKRequest req = VKApi.users().get(VKParameters.from(VKApiConst.USER_IDS, userstring.toString()));
                req.executeWithListener(
                        new VKRequest.VKRequestListener() {
                            @Override
                            public void onComplete(VKResponse response) {
                                super.onComplete(response);
                                try {
                                    JSONArray res = response.json.getJSONArray("response");
                                    for (int i = 0; i < res.length(); i++)
                                    {
                                        JSONObject user = res.optJSONObject(i);
                                        String name = user.getString("first_name") + " " + user.getString("last_name");
                                        UserHistory hist = usernames.get(user.getInt("id"));
                                        if (hist != null)
                                        {
                                            hist.name = name;
                                        }
                                    }
                                    finished = true;
//                                    System.out.println(usernames);
                                } catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        }
                );
            }

            private static final float spacing = 100;

            @Override
            public void run()
            {
                while (!finished)
                    Thread.yield();
//                System.out.println(usernames);
                Vector<UserHistory> l = new Vector<>();
                for (int i = 0; i < usernames.size(); i++)
                    l.add(usernames.valueAt(i));
                Collections.sort(l);
                while (!this.isInterrupted()) {
                    Canvas cv = null;
                    try {
                        cv = surfaceHolder.lockCanvas();
                        pixelHeight = (usernames.size() + 1) * spacing + timeHeight;
                        cv.drawColor(Color.WHITE);
                        Paint linep = new Paint();
                        linep.setColor(Color.MAGENTA);
                        Paint textp = new Paint();
                        textp.setColor(Color.BLACK);
                        textp.setTextSize(30);
                        float yb = spacing;
                        float recth = 3;
                        for (int i = 0; i < l.size(); i++, yb+=spacing)
                        {
                            UserHistory h = l.get(i);
                            cv.drawText(h.name, 20, deltaY + yb - 35, textp);
                            for (int j = 0; j < h.size; j++)
                            {
                                cv.drawRect(new RectF(zoom * (deltaX + h.s.get(j)), deltaY + yb, zoom * (deltaX + h.f.get(j)), deltaY + yb + recth), linep);
                            }
                        }
//                        cv.drawText(((Float) zoom).toString() + " " + ((Float) deltaX).toString() + " " + ((Float) deltaY).toString(), 300, 300, textp);
                        int hour = today.get(Calendar.HOUR_OF_DAY);
                        int offt = today.get(Calendar.MINUTE) * 60 + today.get(Calendar.SECOND);
                        Paint greyp = new Paint();
                        greyp.setColor(Color.GRAY);
                        Paint redp = new Paint();
                        redp.setColor(Color.RED);
                        long x = dt - offt;
                        Integer h = hour;
                        while (x > 0)
                        {
                            Paint p;
                            if (h % 24 == 0)
                                p = redp;
                            else
                                p = greyp;
                            cv.drawLine(zoom * (deltaX + x), 0, zoom * (deltaX + x), vHeight, p);
                            x -= 60 * 60;
                            h--;
                        }

                        Paint wp = new Paint();
                        wp.setColor(Color.WHITE);
                        cv.drawRect(new RectF(0,vHeight - timeHeight, vWidth, vHeight), wp);
                        Paint timep = new Paint();
                        timep.setTextSize(20);
                        timep.setColor(Color.BLACK);
                        x = dt - offt;
                        h = hour;
                        while (x > 0)
                        {
                            cv.drawText(h.toString() + ":00", zoom * (deltaX + x), vHeight - timeHeight + 20, timep);
                            x -= 60 * 60;
                            h--;
                            if (h < 0)
                                h += 24;
                        }
                    } catch (NullPointerException e) {
                        break;
                    } finally {
                        if (cv != null)
                        {
                            surfaceHolder.unlockCanvasAndPost(cv);
                        }
                    }
                }
            }
        }

        private float setValue(float v, float minv, float maxv)
        {
            if (maxv < minv)
                return maxv;
            if (v < minv)
                return minv;
            if (v > maxv)
                return maxv;
            return v;
        }

        public double d, x1, y1, x2, y2, x0, y0;
        private boolean operationEnd = true;
        @Override
        public boolean onTouchEvent (MotionEvent event){
//            System.out.println(event);
            int action = event.getActionMasked();
            if (event.getPointerCount() == 1) {
                double x = event.getX();
                double y = event.getY();
                if (action == MotionEvent.ACTION_DOWN)
                {
                    x0 = x;
                    y0 = y;
                    operationEnd = false;
                } else if (action == MotionEvent.ACTION_MOVE && !operationEnd){
                    deltaX = setValue((float) (deltaX + (x - x0) / zoom), vWidth/zoom - dt, 0);
                    deltaY = setValue((float) (deltaY + (y - y0)), (vHeight - pixelHeight), 0);
                    x0 = x;
                    y0 = y;
                }
                return true;
            }
            if (event.getPointerCount() > 2)
            {
                operationEnd = true;
                return false;
            }
            x1 = event.getX(0);
            x2 = event.getX(1);
            y1 = event.getY(0);
            y2 = event.getY(1);
            double dx = x1 - x2;
            double dy = y1 - y2;
            if (action == MotionEvent.ACTION_POINTER_UP)
            {
                operationEnd = true;
                return false;
            }
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN)
            {
                d = Math.sqrt(dx * dx + dy * dy);
                operationEnd = false;
            } else if (action == MotionEvent.ACTION_MOVE && !operationEnd)
            {

                double d1 = Math.sqrt(dx * dx + dy * dy);
                zoom = setValue((float) (zoom * (d1 / d)), minZoom, maxZoom);
                zoom *= d1 / d;
                deltaX = setValue(deltaX, vWidth/zoom - dt, 0);
                deltaY = setValue(deltaY, (vHeight - pixelHeight), 0);
                d = d1;
            }
            return true;
        }

        Thread drawThread = null;

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
        {
            vHeight = height;
            vWidth = width;
            dt = (HistoryService.dt / 1000);
            minZoom = vWidth / ((float) dt);
            zoom = setValue(minZoom * 3, minZoom, maxZoom);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            SQLiteDatabase db = StartActivity.eventHelper.getReadableDatabase();
            Cursor r = db.query(
                    false,
                    EventsContract.EventEntry.TABLE_NAME,
                    new String[] {
                            EventsContract.EventEntry.COLUMN_NAME_TIME,
                            EventsContract.EventEntry.COLUMN_NAME_USER_ID,
                            EventsContract.EventEntry.COLUMN_NAME_TYPE,
                            EventsContract.EventEntry.COLUMN_NAME_EXTRA
                    },
                    null,
                    null,
                    null,
                    null,
                    EventsContract.EventEntry.COLUMN_NAME_TIME + " ASC",
                    null
            );

            drawThread = new DrawingThread(r);
            r.close();
            drawThread.start();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            boolean retry = true;
            drawThread.interrupt();
            while (retry) {
                try {
                    drawThread.join();
                    retry = false;
                } catch (InterruptedException e) {}
            }
        }

    }
}