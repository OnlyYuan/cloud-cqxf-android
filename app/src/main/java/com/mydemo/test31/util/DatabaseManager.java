package com.mydemo.test31.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mydemo.test31.data.User;
import com.mydemo.test31.helper.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private final DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    public DatabaseManager(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    /**
     * 查询所有用户
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        Cursor cursor = database.query(
                DatabaseHelper.TABLE_USERS,
                null, null, null, null, null, null
        );
        if (cursor != null && cursor.moveToFirst()) {
            do {
                User user = new User();
                user.setId(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)));
                user.setName(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_NAME)));
                user.setPassword(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PASSWORD)));
                users.add(user);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return users;
    }

    /**
     * 根据 ID 查询用户
     */
    public User getUserById(int id) {
        Cursor cursor = database.query(
                DatabaseHelper.TABLE_USERS,
                null,
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null
        );
        if (cursor != null && cursor.moveToFirst()) {
            User user = new User();
            user.setId(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)));
            user.setName(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_NAME)));
            user.setPassword(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PASSWORD)));
            cursor.close();
            return user;
        }
        return null;
    }

    /**
     * 添加新用户
     */
    public long addUser(User user) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_NAME, user.getName());
        values.put(DatabaseHelper.COLUMN_PASSWORD, user.getPassword());
        return database.insert(DatabaseHelper.TABLE_USERS, null, values);
    }


    public void updateUser(User user) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_POC_USERNAME, user.getPocUserName());
        values.put(DatabaseHelper.COLUMN_POC_PASSWORD, user.getPocPassword());
        database.update(DatabaseHelper.TABLE_USERS, values,DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(user.getId())});
    }

    public int resetTable() {
        return database.delete(DatabaseHelper.TABLE_USERS, null, null);
    }
}