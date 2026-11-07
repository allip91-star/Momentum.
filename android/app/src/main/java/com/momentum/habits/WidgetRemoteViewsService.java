package com.momentum.habits;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import java.util.ArrayList;
import java.util.List;

/** Supplies an scrolling list that can also show completed habits. */
public class WidgetRemoteViewsService extends RemoteViewsService {
    @Override public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new HabitRowFactory(getApplicationContext());
    }
    private static class HabitRowFactory implements RemoteViewsFactory {
        private final android.content.Context context;
        private volatile List<WidgetStore.Row> rows=new ArrayList<>();
        HabitRowFactory(android.content.Context context) { this.context=context; }
        @Override public void onCreate() { onDataSetChanged(); }
        @Override public void onDataSetChanged() { rows=WidgetStore.pending(context); }
        @Override public void onDestroy() { rows.clear(); }
        @Override public int getCount() { return rows.size(); }
        @Override public RemoteViews getViewAt(int position) {
            List<WidgetStore.Row> snapshot=rows;
            if(position<0||position>=snapshot.size())return null;
            WidgetStore.Row h=snapshot.get(position);
            RemoteViews v=new RemoteViews(context.getPackageName(),R.layout.widget_habit_row);
            v.setTextViewText(R.id.widget_emoji,h.emoji);
            v.setInt(R.id.widget_color,"setBackgroundColor",h.color);
            v.setTextViewText(R.id.widget_name,h.title);
            v.setTextViewText(R.id.widget_progress,WidgetStore.number(h.current)+" / "+WidgetStore.number(h.goal)+" "+h.unit+" · "+h.frequency+(h.completed?" · Done ✓":""));
            v.setTextViewText(R.id.widget_plus,h.completed?"✓":h.logOnly?"Log":"+"+WidgetStore.number(h.step));
            Intent add=new Intent();
            add.putExtra(HabitWidgetProvider.EXTRA_HABIT_ID,h.id);
            add.putExtra(HabitWidgetProvider.EXTRA_OPERATION,h.completed||h.logOnly?"log":"add");
            v.setOnClickFillInIntent(R.id.widget_plus,add);
            Intent log=new Intent();
            log.putExtra(HabitWidgetProvider.EXTRA_HABIT_ID,h.id);
            log.putExtra(HabitWidgetProvider.EXTRA_OPERATION,"log");
            v.setOnClickFillInIntent(R.id.widget_habit_details,log);
            return v;
        }
        @Override public RemoteViews getLoadingView() { return null; }
        @Override public int getViewTypeCount() { return 1; }
        @Override public long getItemId(int position) { List<WidgetStore.Row> snapshot=rows; return position<snapshot.size()?snapshot.get(position).id.hashCode():position; }
        @Override public boolean hasStableIds() { return true; }
    }
}
