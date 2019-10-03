package com.ramsaysmith.ultrame;

import android.app.Notification;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import java.util.List;

@Dao
public interface NotificationDao {

    @Query("SELECT * FROM notificationitem ORDER BY date_received DESC LIMIT 50")
    List<NotificationContent.NotificationItem> getAll();

    @Query("SELECT COUNT(*) FROM notificationitem")
    Integer getNotificationCount();

    @Insert
    void insert(NotificationContent.NotificationItem notificationItem);

    @Delete
    void delete(NotificationContent.NotificationItem notificationItem);
}
