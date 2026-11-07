package com.momentum.habits;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Collections;
import java.util.Locale;

/** The widget and WebView share one on-device database via SharedPreferences. */
public final class WidgetStore {
    private static final String PREFS = "momentum_widget_shared_v2";
    private static final String KEY = "db";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final double EPS = 1e-8;

    public static final class Row {
        public final String id, title, emoji, unit, frequency;
        public final int color;
        public final double current, goal, step;
        public final boolean logOnly, completed;
        Row(String id, String title, String emoji, String unit, String frequency,
            double current, double goal, double step, int color,boolean logOnly,boolean completed) {
            this.id=id; this.title=title; this.emoji=emoji; this.unit=unit;
            this.frequency=frequency; this.current=current; this.goal=goal; this.step=step; this.color=color; this.logOnly=logOnly; this.completed=completed;
        }
    }

    private static final String[] PALETTE={"#7366E8","#20A67A","#298CD2","#D97706","#E45E83","#9166CE", "#0E9AA7","#D24D4D","#687A33","#6074AE","#566172","#9B5A38"};
    /** Allow only full opaque hex colours; never trust arbitrary JSON values. */
    private static int rowColor(JSONObject h,int originalIndex) {
        String supplied=h.optString("color","");
        if(!supplied.matches("#[A-Fa-f0-9]{6}"))supplied=PALETTE[originalIndex%PALETTE.length];
        try { return Color.parseColor(supplied); } catch (Exception e) { return Color.parseColor(PALETTE[0]); }
    }
    private WidgetStore() {}
    public static synchronized String getState(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "");
    }
    public static synchronized boolean saveState(Context context, String text) {
        try {
            JSONObject obj = new JSONObject(text);
            if (obj.optJSONArray("habits") == null || obj.optJSONObject("entries") == null) return false;
            return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putString(KEY, text).commit();
        } catch (JSONException e) { return false; }
    }
    private static JSONObject object(JSONObject parent, String key) throws JSONException {
        JSONObject child = parent.optJSONObject(key);
        if (child == null) { child = new JSONObject(); parent.put(key, child); }
        return child;
    }
    private static boolean isCount(JSONObject h) {
        String kind = h.optString("kind", "");
        if ("amount".equals(kind)) return false;
        if ("count".equals(kind)) return true;
        return h.optString("unit", "times").matches("(?i)times?|sessions?|runs?|rides?");
    }
    private static double quickAdd(JSONObject h) {
        if (isCount(h)) return 1.0;
        double n = h.optDouble("step", 1.0);
        return Double.isFinite(n) && n > 0 ? n : 1;
    }
    /** Match the browser's day scheduling and pause rules for a single local date. */
    static boolean isActiveOn(JSONObject habit,LocalDate date) {
        if(habit.optString("created","1970-01-01").compareTo(date.toString())>0)return false;
        JSONArray pauses=habit.optJSONArray("pauses");
        if(pauses!=null)for(int i=0;i<pauses.length();i++){
            JSONObject pause=pauses.optJSONObject(i);if(pause==null)continue;
            String start=pause.optString("start","9999-12-31"),end=pause.optString("end","");
            if(start.compareTo(date.toString())<=0&&(end.isEmpty()||end.compareTo(date.toString())>=0))return false;
        }
        String mode=habit.optString("scheduleMode","any");
        if("weekdays".equals(mode)){
            JSONArray days=habit.optJSONArray("weekdays");if(days==null)return true;
            for(int i=0;i<days.length();i++)if(days.optInt(i)==date.getDayOfWeek().getValue())return true;
            return false;
        }
        if("interval".equals(mode)){
            try {int every=Math.max(1,habit.optInt("intervalDays",2));
                LocalDate anchor=LocalDate.parse(habit.optString("created",date.toString()));
                return java.time.temporal.ChronoUnit.DAYS.between(anchor,date)%every==0;
            }catch(Exception ex){return false;}
        }
        return true;
    }
    static double periodTotal(JSONObject state,JSONObject h,LocalDate date) {
        JSONObject entries=state.optJSONObject("entries");
        String id=h.optString("id",""),freq=h.optString("frequency","daily");
        String aggregation=h.optString("aggregation","sum");
        if("sum".equals(aggregation) || isCount(h))return sum(entries,id,freq,date);
        JSONObject amounts=entries==null?null:entries.optJSONObject(id);
        if(amounts==null)return 0;
        LocalDate start=periodStart(freq,date);
        double result=0;String latest="";
        Iterator<String> dates=amounts.keys();
        while(dates.hasNext()){
            String key=dates.next();
            try{LocalDate day=LocalDate.parse(key);
                if(day.isBefore(start)||day.isAfter(date))continue;
                double v=amounts.optDouble(key,0);if(!Double.isFinite(v)||v<0)continue;
                if("max".equals(aggregation))result=Math.max(result,v);
                else if(key.compareTo(latest)>0){result=v;latest=key;}
            }catch(Exception ignored){}
        }
        return result;
    }
    static boolean goalReached(JSONObject state,JSONObject h,LocalDate date){
        double n=periodTotal(state,h,date),goal=h.optDouble("goal",1);
        if("atMost".equals(h.optString("goalMode","atLeast"))){
            if(n<=0||n>goal)return false;
            if("sum".equals(h.optString("aggregation","sum")))return false; // food budgets are scored when the period closes
            return true;
        }
        return n+EPS>=goal;
    }
    private static LocalDate periodStart(String frequency, LocalDate day) {
        switch(frequency) {
            case "weekly": return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "monthly": return day.withDayOfMonth(1);
            default: return day;
        }
    }
    private static double sum(JSONObject entries, String id, String frequency, LocalDate date) {
        if(entries==null)return 0;
        JSONObject logs = entries.optJSONObject(id);
        if (logs == null) return 0;
        LocalDate from = periodStart(frequency,date);
        double total=0;
        Iterator<String> dates = logs.keys();
        while (dates.hasNext()) {
            String dateKey = dates.next();
            try {
                LocalDate saved = LocalDate.parse(dateKey,DATE);
                if (!saved.isBefore(from) && !saved.isAfter(date)) {
                    double n=logs.optDouble(dateKey, 0);
                    if (Double.isFinite(n) && n > 0) total+=n;
                }
            } catch (Exception ignored) { }
        }
        return Math.round(total*1000.0)/1000.0;
    }
    private static int fallbackEvents(JSONObject habit, JSONObject entriesByHabit, String date) {
        double value = Math.max(0, entriesByHabit.optDouble(date,0));
        return isCount(habit) ? (int)Math.ceil(value) : (value > 0 ? 1 : 0);
    }
    private static JSONObject findHabit(JSONObject state, String id) {
        JSONArray habits = state.optJSONArray("habits");
        if (habits == null) return null;
        for(int i=0;i<habits.length();i++) {
            JSONObject h=habits.optJSONObject(i);
            if(h != null && id.equals(h.optString("id"))) return h;
        }
        return null;
    }
    public static synchronized List<Row> pending(Context context) {
        List<Row> rows=new ArrayList<>();
        try {
            String saved=getState(context);
            if(saved.isEmpty()) return rows;
            JSONObject state=new JSONObject(saved);
            JSONArray habits=state.optJSONArray("habits");
            JSONObject entries=state.optJSONObject("entries");
            if(habits==null||entries==null)return rows;
            LocalDate date=LocalDate.now();
            HashMap<String,Integer> widgetRanks=new HashMap<>();
            JSONArray order=state.optJSONArray("widgetOrder");
            if(order!=null)for(int j=0;j<order.length();j++){
                String orderedId=order.optString(j,"");
                if(!orderedId.isEmpty()&&!widgetRanks.containsKey(orderedId))widgetRanks.put(orderedId,j);
            }
            for(int i=0;i<habits.length();i++) {
                JSONObject h=habits.optJSONObject(i);
                if(h==null || !h.optBoolean("widgetVisible",true) || !isActiveOn(h,date))continue;
                String created=h.optString("created", "1970-01-01");
                if(created.compareTo(date.toString())>0)continue;
                double goal=h.optDouble("goal",1);
                if(!Double.isFinite(goal)||goal<=0)continue;
                String id=h.optString("id","");
                String frequency=h.optString("frequency","daily");
                if(id.isEmpty())continue;
                double current=periodTotal(state,h,date);
                boolean complete=goalReached(state,h,date);
                if(complete && h.optBoolean("hideWhenComplete",true))continue;
                boolean needsInput=!isCount(h)&&!"sum".equals(h.optString("aggregation","sum"));
                rows.add(new Row(id,h.optString("name","Habit"),h.optString("emoji","✓"),
                         h.optString("unit","times"),frequency,current,goal,quickAdd(h),rowColor(h,i),needsInput,complete));
            }
            // Listed order is independent of the order of habits elsewhere in the app.
            // Anything added after the user saved their ordering appears at the bottom.
            Collections.sort(rows,(a,b)->{
                int aa=widgetRanks.containsKey(a.id)?widgetRanks.get(a.id):Integer.MAX_VALUE;
                int bb=widgetRanks.containsKey(b.id)?widgetRanks.get(b.id):Integer.MAX_VALUE;
                return Integer.compare(aa,bb);
            });
        } catch(Exception ignored) { }
        return rows;
    }

    /** A widget tap adds one unit/session or the habit's configured quick-add amount. */
    public static synchronized boolean addProgress(Context context, String id) {
        try {
            String saved=getState(context);
            if(saved.isEmpty())return false;
            JSONObject state=new JSONObject(saved);
            JSONObject h=findHabit(state,id);
            if(h==null || !h.optBoolean("widgetVisible",true))return false;
            LocalDate today=LocalDate.now();
            if(!isActiveOn(h,today))return false;
            double goal=h.optDouble("goal",1),step=quickAdd(h);
            if(!isCount(h)&&!"sum".equals(h.optString("aggregation","sum")))return false;
            String frequency=h.optString("frequency","daily");
            JSONObject entries=object(state,"entries");
            if(goalReached(state,h,today))return false;
            String date=today.toString();
            JSONObject amounts=object(entries,id);
            double oldAmount=Math.max(0,amounts.optDouble(date,0));
            JSONObject events=object(state,"events");
            JSONObject counts=object(events,id);
            int oldEvents=counts.has(date)?Math.max(0,counts.optInt(date,0)):fallbackEvents(h,amounts,date);
            double updated=Math.round((oldAmount+step)*1000.0)/1000.0;
            amounts.put(date,updated);
            counts.put(date,oldEvents+1);
            JSONObject logs=object(state,"logs");
            JSONObject habitLogs=object(logs,id);
            JSONArray dayLogs=habitLogs.optJSONArray(date);
            if(dayLogs==null){dayLogs=new JSONArray();habitLogs.put(date,dayLogs);}
            dayLogs.put(step);
            return saveState(context,state.toString());
        } catch(Exception e) { return false; }
    }
    public static String number(double n) {
        DecimalFormat fmt=new DecimalFormat("#,##0.###",DecimalFormatSymbols.getInstance(Locale.UK));
        return fmt.format(n);
    }
}
