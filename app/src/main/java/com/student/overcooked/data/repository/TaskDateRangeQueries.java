package com.student.overcooked.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.student.overcooked.data.dao.TaskDao;
import com.student.overcooked.data.model.Task;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

final class TaskDateRangeQueries {

    static LiveData<List<Task>> dueToday(@NonNull TaskDao taskDao) {
        Date startOfDay = startOfToday();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startOfDay);
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Date endOfDay = calendar.getTime();
        return taskDao.getTasksDueBetween(startOfDay, endOfDay);
    }

    static LiveData<List<Task>> dueWithinDays(@NonNull TaskDao taskDao, int days) {
        Date startOfDay = startOfToday();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startOfDay);
        calendar.add(Calendar.DAY_OF_YEAR, days);
        Date end = calendar.getTime();
        return taskDao.getTasksDueBetween(startOfDay, end);
    }

    private static Date startOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private TaskDateRangeQueries() {
    }
}
