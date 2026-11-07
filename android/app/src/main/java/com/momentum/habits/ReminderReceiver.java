package com.momentum.habits;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.content.pm.PackageManager;
import org.json.JSONObject;
import org.json.JSONArray;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/** Optional, on-device reminders. No server, account or background tracking. */
public final class ReminderReceiver extends BroadcastReceiver {
    private static final String ACTION = "com.momentum.habits.NEXT_REMINDER";
    private static final String CHANNEL = "momentum-habits";
    private static final int REQUEST = 19741;
    private static PendingIntent pending(Context c,String habitId,long when) {
        Intent i=new Intent(c,ReminderReceiver.class).setAction(ACTION);
        i.putExtra("habit",habitId);
        i.putExtra("planned",when);
        return PendingIntent.getBroadcast(c,REQUEST,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private static long epoch(LocalDate date,LocalTime time) {
        return LocalDateTime.of(date,time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    private static boolean shouldRemindOn(JSONObject h,LocalDate date) {
        if(!WidgetStore.isActiveOn(h,date))return false;
        String frequency=h.optString("frequency","daily");
        if("daily".equals(frequency)||!"once".equals(h.optString("reminderMode","daily")))return true;
        if("weekly".equals(frequency))return date.getDayOfWeek().getValue()==h.optInt("reminderWeekday",1);
        if("monthly".equals(frequency)){
            int day=h.optInt("reminderMonthday",1);
            if(day==0)day=date.lengthOfMonth();
            return date.getDayOfMonth()==Math.min(Math.max(day,1),date.lengthOfMonth());
        }
        return false;
    }
    /** Schedule only the next event. We reschedule whenever habits change or an alarm fires. */
    public static void scheduleNext(Context c) {
        AlarmManager manager=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(manager==null)return;
        manager.cancel(pending(c,"",0));
        try {
            String saved=WidgetStore.getState(c);if(saved.isEmpty())return;
            JSONObject db=new JSONObject(saved);JSONArray habits=db.optJSONArray("habits");
            if(habits==null)return;
            long now=System.currentTimeMillis(),best=Long.MAX_VALUE;String bestId="";
            LocalDate today=LocalDate.now();
            for(int n=0;n<habits.length();n++){
                JSONObject h=habits.optJSONObject(n);
                if(h==null||!h.optBoolean("reminderEnabled",false))continue;
                LocalTime time;try{time=LocalTime.parse(h.optString("reminderTime",""));}catch(Exception e){continue;}
                for(int offset=0;offset<75;offset++){
                    LocalDate d=today.plusDays(offset);long scheduled=epoch(d,time);
                    if(scheduled<=now+1000||scheduled>=best||!shouldRemindOn(h,d))continue;
                    if(WidgetStore.goalReached(db,h,d))continue;
                    best=scheduled;bestId=h.optString("id","");
                }
            }
            JSONObject settings=db.optJSONObject("settings");
            if(settings!=null&&settings.optBoolean("weeklySummary",false)) {
                for(int i=0;i<8;i++){
                    LocalDate d=today.plusDays(i);
                    if(d.getDayOfWeek().getValue()!=1)continue;
                    long candidate=epoch(d,LocalTime.of(8,0));
                    if(candidate>now+1000&&candidate<best){best=candidate;bestId="__weekly_summary__";}
                }
            }
            if(best==Long.MAX_VALUE)return;
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,best,pending(c,bestId,best));
        }catch(Exception ignored){}
    }
    @Override public void onReceive(Context c,Intent intent){
        if(intent==null||!ACTION.equals(intent.getAction()))return;
        String id=intent.getStringExtra("habit");
        try {
            String saved=WidgetStore.getState(c);
            JSONObject db=new JSONObject(saved);
            JSONObject h=null;
            JSONArray habits=db.optJSONArray("habits");
            if(habits!=null)for(int i=0;i<habits.length();i++){
                JSONObject item=habits.optJSONObject(i);
                if(item!=null&&item.optString("id","").equals(id)){h=item;break;}
            }
            long when=intent.getLongExtra("planned",0);
            LocalDate onDate=java.time.Instant.ofEpochMilli(when).atZone(ZoneId.systemDefault()).toLocalDate();
            if("__weekly_summary__".equals(id)){
                JSONObject prefs=db.optJSONObject("settings");
                if(prefs!=null&&prefs.optBoolean("weeklySummary",false))notifyUser(c,"Momentum weekly review","Last week’s habit summary is ready in Progress.",id);
            }else if(h!=null&&h.optBoolean("reminderEnabled",false)&&shouldRemindOn(h,onDate)
                    &&!WidgetStore.goalReached(db,h,onDate)){
                notifyUser(c,"Time for " + h.optString("name","your habit"),"Your goal is still waiting — tap to open Momentum.",id);
            }
        }catch(Exception ignored){}
        finally { scheduleNext(c); }
    }
    private static void notifyUser(Context c,String title,String message,String id){
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm==null)return;
        if(Build.VERSION.SDK_INT>=33 && c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Habit reminders",NotificationManager.IMPORTANCE_DEFAULT));
        Intent launch=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tap=PendingIntent.getActivity(c,id.hashCode(),launch,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification notification=new Notification.Builder(c,CHANNEL).setContentTitle(title).setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher).setAutoCancel(true).setContentIntent(tap).build();
        nm.notify(id.hashCode(),notification);
    }
}
