package medic.core;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;

import static medic.core.Utils.Dir.DATABASE;
import static medic.core.Utils.ERR_DOUBLE;
import static medic.core.Utils.ERR_INT;
import static medic.core.Utils.ERR_LONG;
import static medic.core.Utils.ERR_STRING;
import static medic.core.Utils.getFile;
import static medic.core.Utils.logError;

public class Database {
    private static final File DATABASE_FILE = getFile(DATABASE, "medic.db");

    public static boolean exists(String table) {
        MyCursor cursor = rawQuery("select count(*) from sqlite_master " +
                "where type=? and name=?", "table", table);
        return cursor.count != 0;
    }

    /**
     * 调用除查询语句以外的 SQL 语句.
     * <p>
     * 下面是一个示例：
     * execSQL("create table BOOK ("
     * + "id integer primary key autoincrement, "
     * + "name text, "
     * + "author text)");
     * 下面是另一个示例：
     * String delete_sql = "delete from test where _id = " + idString;
     * execSQL(sql);
     * <p>
     * 为了方便，直接使用同一个数据库。
     *
     * @param sql 要执行的 SQL 语句，一定不以";"结尾
     */
    public static boolean execSQL(String sql) {
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(DATABASE_FILE, null)) {
            db.execSQL(sql);
            return true;
        } catch (SQLException e) {
            logError(e);
            return false;
        }
    }

    /**
     * 调用除查询语句以外的 SQL 语句.
     * <p>
     * 下面是一个示例：
     * String delete_sql = "delete from test where _id = ?, name = ?";
     * execSQL(sql, new String[] {idString, nameString});
     * <p>
     * 为了方便，直接使用同一个数据库。
     *
     * @param sql      要执行的 SQL 语句，一定不以";"结尾。
     *                 不支持以分号分隔的多个语句。
     * @param bindArgs bindArgs仅支持byte[]，String，Long和Double。
     */
    public static boolean execSQL(String sql, Object... bindArgs) {
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(DATABASE_FILE, null)) {
            db.execSQL(sql, bindArgs);
            return true;
        } catch (SQLException e) {
            logError(e);
            return false;
        }
    }

    /**
     * 调用查询 SQL 语句.
     * <p>
     * 为了方便，直接使用同一个数据库。
     *
     * @param sql           要执行的 SQL 语句，一定不以";"结尾
     * @param selectionArgs 您可以在查询的 where 子句中包含 ?s，它将替换为selectionArgs中的值。
     *                      这些值将绑定为字符串。
     * @return 获取到的数据
     */
    public static MyCursor rawQuery(String sql, String... selectionArgs) {
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(DATABASE_FILE, null);
        Cursor cursor = db.rawQuery(sql, selectionArgs);
        MyCursor myCursor = new MyCursor(cursor);
        cursor.close();
        db.close();
        return myCursor;
    }


    /**
     * 向数据库插入一行.
     * <p>
     * 下面是一个示例：
     * ContentValues values = new ContentValues();
     * values.put("name", p.getName());
     * values.put("phone", p.getPhone());
     * insert("person", "name", values);
     *
     * @param table          表名
     * @param nullColumnHack 可选；可能为<code>null</code>。
     *                       SQL不允许在不命名至少一个列名的情况下插入完全空的行。
     *                       如果您提供的<code>values</code>为空，则未知列名，并且不能插入空行。
     *                       如果未设置为null，则<code>nullColumnHack</code>参数提供可为空的列的名称，
     *                       以在<code>values</code>为空的情况下显式插入NULL。
     * @param values         要插入的行的信息，以键值对“字段（即列名），值”的形式存储
     * @return 返回新插入行的ID，出错则返回-1
     */
    public static long insert(String table, String nullColumnHack, ContentValues values) {
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(DATABASE_FILE, null);
        long i = db.insert(table, nullColumnHack, values);
        db.close();
        return i;
    }

    public static long insert(String table, ContentValues values) {
        return insert(table, null, values);
    }

    /**
     * 删除数据库的某些行.
     *
     * @param table       表名
     * @param whereClause 删除时所用的 WHERE 语句，传入 null 表示删除该表全部内容
     * @param whereArgs   使用该参数替换 WHERE 语句中的 "?"
     * @return 有 WHERE 语句时，返回删除的行数；否则返回1。
     */
    public static int delete(String table, String whereClause, String... whereArgs) {
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(DATABASE_FILE, null);
        int i = db.delete(table, whereClause, whereArgs);
        db.close();
        return i;
    }

    public static class MyCursor {
        MyCursor(Cursor cursor) {
            count = cursor.getCount();
            columnCount = cursor.getColumnCount();
            columnNames = cursor.getColumnNames();
            data = new String[count][columnCount];
            for (int row = 0; row < count; row++) {
                cursor.moveToNext();
                for (int col = 0; col < columnCount; col++) {
                    data[row][col] = cursor.getString(col);
                }
            }
        }

        int count;
        int columnCount;
        String[] columnNames;
        String[][] data;

        public int getColumnIndex(String columnName) {
            for (int i = 0; i < columnCount; i++) {
                if (columnName.equals(columnNames[i])) {
                    return i;
                }
            }
            return ERR_INT;
        }

        public String getColumnName(int columnIndex) {
            try {
                return columnNames[columnIndex];
            } catch (RuntimeException e) {
                return ERR_STRING;
            }
        }

        public String getString(int row, int col) {
            try {
                return data[row][col];
            } catch (RuntimeException e) {
                return ERR_STRING;
            }
        }

        public int getInt(int row, int col) {
            try {
                return Integer.parseInt(data[row][col]);
            } catch (RuntimeException e) {
                return ERR_INT;
            }
        }

        public long getLong(int row, int col) {
            try {
                return Long.parseLong(data[row][col]);
            } catch (RuntimeException e) {
                return ERR_LONG;
            }
        }

        public double getDouble(int row, int col) {
            try {
                return Double.parseDouble(data[row][col]);
            } catch (RuntimeException e) {
                return ERR_DOUBLE;
            }
        }

        public boolean getBoolean(int row, int col) {
            try {
                return Boolean.parseBoolean(data[row][col]);
            } catch (RuntimeException e) {
                return false;
            }
        }
    }
}
