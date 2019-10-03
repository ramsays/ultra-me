package com.ramsaysmith.ultrame;

import android.app.Notification;
import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class NotificationContent {

    /**
     * An array of sample (dummy) items.
     */
    public static final List<NotificationItem> ITEMS = new ArrayList<NotificationItem>();

    /**
     * A map of sample (dummy) items, by ID.
     */
    public static final Map<Integer, NotificationItem> ITEM_MAP = new HashMap<Integer, NotificationItem>();

    public static void addItem(NotificationItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
    }

    /**
     * A dummy item representing a piece of content.
     */
    @Entity
    public static class NotificationItem {
        @PrimaryKey(autoGenerate = true)
        public final Integer id;
        @ColumnInfo(name = "content")
        public final String content;
        @ColumnInfo(name = "date_received")
        public final Long dateReceived;

        public NotificationItem(Integer id, String content, Long dateReceived) {
            this.id = id;
            this.content = content;
            this.dateReceived = dateReceived;
        }

        @Override
        public String toString() {
            return content;
        }
    }
}
