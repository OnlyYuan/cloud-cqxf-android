package com.mydemo.test31.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "user_database.db";
    private static final int DATABASE_VERSION = 1;

    // 表名和列名
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PASSWORD = "password";

    public static final String COLUMN_POC_USERNAME = "pocUserName";

    public static final String COLUMN_POC_PASSWORD = "pocPassword";

    // 创建表的 SQL 语句
    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT NOT NULL, " +
                    COLUMN_PASSWORD + " TEXT NOT NULL);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        // 插入一些示例数据
        // db.execSQL("INSERT INTO " + TABLE_USERS + " (" + COLUMN_NAME + ", " + COLUMN_PASSWORD + ") VALUES ('张三', 'zhangsan@email.com')");
        // db.execSQL("INSERT INTO " + TABLE_USERS + " (" + COLUMN_NAME + ", " + COLUMN_PASSWORD + ") VALUES ('李四', 'lisi@email.com')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }
}