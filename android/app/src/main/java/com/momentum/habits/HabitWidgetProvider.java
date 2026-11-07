package com.momentum.habits;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;
import java.time.LocalDate;
import java.time.ZoneId;

/** Scrollable Android home-screen widget; per-habit completed items may remain visible. */
public class HabitWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_ITEM="com.momentum.habits.WIDGET_ITEM";
    private static final String ACTION_MIDNIGHT="com.momentum.habits.WIDGET_MIDNIGHT";
    public static final String EXTRA_HABIT_ID="widget_habit_id";
    public static final String EXTRA_OPERATION="widget_operation";

    @Override public void onReceive(Context c, Intent intent) {
        super.onReceive(c,intent);
        if(intent==null)return;
        String action=intent.getAction();
        if(ACTION_ITEM.equals(action)) {
            String id=intent.getStringExtra(EXTRA_HABIT_ID);
            String operation=intent.getStringExtra(EXTRA_OPERATION);
            if(id==null)return;
            if("add".equals(operation)) {
                if(WidgetStore.addProgress(c,id)){refreshAll(c);ReminderReceiver.scheduleNext(c);}
            } else if("log".equals(operation)) {
                Intent open=new Intent(c,MainActivity.class);
                open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                open.putExtra(EXTRA_HABIT_ID,id);
                c.startActivity(open);
            }
        } else if(ACTION_MIDNIGHT.equals(action) || Intent.ACTION_TIME_CHANGED.equals(action)
                 || Intent.ACTION_TIMEZONE_CHANGED.equals(action) || Intent.ACTION_DATE_CHANGED.equals(action)
                 || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            refreshAll(c);
            scheduleMidnight(c);
            ReminderReceiver.scheduleNext(c);
        }
    }

    @Override public void onUpdate(Context c, AppWidgetManager mgr, int[] ids) {
        for(int id:ids)updateWidget(c,mgr,id);
        scheduleMidnight(c);
    }
    @Override public void onEnabled(Context c) { scheduleMidnight(c); }
    @Override public void onDisabled(Context c) {
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(am!=null)am.cancel(midnightPendingIntent(c));
    }
    public static void refreshAll(Context c) {
        AppWidgetManager mgr=AppWidgetManager.getInstance(c);
        int[] ids=mgr.getAppWidgetIds(new ComponentName(c,HabitWidgetProvider.class));
        for(int id:ids)updateWidget(c,mgr,id);
    }
    private static void updateWidget(Context c,AppWidgetManager mgr,int id) {
        RemoteViews v=new RemoteViews(c.getPackageName(), R.layout.widget_habits);
        int n=WidgetStore.pending(c).size();
        boolean emptyState=WidgetStore.getState(c).isEmpty();
        v.setTextViewText(R.id.widget_count,emptyState?"Tap to start":n==0?"All caught up!":n+" showing");
        v.setTextViewText(R.id.widget_empty,emptyState?"Open Momentum to add your habits":
                "✓ No habits to show right now.\nFinished goals reset on schedule.");
        Intent open=new Intent(c,MainActivity.class);
        PendingIntent openPI=PendingIntent.getActivity(c,id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.widget_header,openPI);
        Intent adapter=new Intent(c,WidgetRemoteViewsService.class);
        adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
        adapter.setData(Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME)));
        v.setRemoteAdapter(R.id.widget_habit_list,adapter);
        v.setEmptyView(R.id.widget_habit_list,R.id.widget_empty);
        Intent click=new Intent(c,HabitWidgetProvider.class);
        click.setAction(ACTION_ITEM);
        click.setData(Uri.parse("momentum://widget/"+id));
        int flags=PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT>=31?PendingIntent.FLAG_MUTABLE:0);
        v.setPendingIntentTemplate(R.id.widget_habit_list,PendingIntent.getBroadcast(c,id,click,flags));
        mgr.updateAppWidget(id,v);
        mgr.notifyAppWidgetViewDataChanged(id,R.id.widget_habit_list);
    }
    private static PendingIntent midnightPendingIntent(Context c) {
        Intent intent=new Intent(c,HabitWidgetProvider.class).setAction(ACTION_MIDNIGHT);
        return PendingIntent.getBroadcast(c,14303,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private static void scheduleMidnight(Context c) {
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(am==null)return;
        long next=LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()+30_000;
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,midnightPendingIntent(c));
    }
}
