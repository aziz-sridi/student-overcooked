package com.student.overcooked.data.database;

import androidx.room.TypeConverter;

import com.student.overcooked.data.model.MemberRole;
import com.student.overcooked.data.model.Priority;
import com.student.overcooked.data.model.TaskStatus;
import com.student.overcooked.data.model.TaskType;

import java.util.Date;

/**
 * Type converters for Room database
 * Handles conversion between complex types and database-storable types
 */
public class Converters {

    // Date converters
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    // Priority converters
    @TypeConverter
    public static String fromPriority(Priority priority) {
        return priority == null ? null : priority.name();
    }

    @TypeConverter
    public static Priority toPriority(String value) {
        return value == null ? Priority.MEDIUM : Priority.fromString(value);
    }

    // TaskType converters
    @TypeConverter
    public static String fromTaskType(TaskType taskType) {
        return taskType == null ? null : taskType.name();
    }

    @TypeConverter
    public static TaskType toTaskType(String value) {
        return value == null ? TaskType.OTHER : TaskType.fromString(value);
    }

    // MemberRole converters
    @TypeConverter
    public static String fromMemberRole(MemberRole role) {
        return role == null ? null : role.name();
    }

    @TypeConverter
    public static MemberRole toMemberRole(String value) {
        if (value == null) return MemberRole.MEMBER;
        try {
            return MemberRole.valueOf(value);
        } catch (Exception e) {
            return MemberRole.MEMBER;
        }
    }

    // TaskStatus converters
    @TypeConverter
    public static String fromTaskStatus(TaskStatus status) {
        return status == null ? null : status.name();
    }

    @TypeConverter
    public static TaskStatus toTaskStatus(String value) {
        return value == null ? TaskStatus.NOT_STARTED : TaskStatus.fromString(value);
    }
}
