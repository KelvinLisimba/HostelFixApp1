package com.example.hostelfixapp.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.hostelfixapp.data.model.ChatMessage
import com.example.hostelfixapp.data.model.MaintenanceReport
import com.example.hostelfixapp.data.model.ReportCategory
import com.example.hostelfixapp.data.model.ReportStatus
import com.example.hostelfixapp.data.model.SyncState
import com.example.hostelfixapp.data.model.User
import com.example.hostelfixapp.data.model.UserRole

class HostelFixLocalDb(context: Context) : SQLiteOpenHelper(context, "hostelfix_database.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = """
            CREATE TABLE users (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                student_id TEXT,
                role TEXT NOT NULL,
                assigned_hostel TEXT,
                assigned_room TEXT,
                profile_pic_url TEXT,
                password TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
        """.trimIndent()

        val createReportsTable = """
            CREATE TABLE reports (
                report_id TEXT PRIMARY KEY,
                student_id TEXT NOT NULL,
                student_name TEXT NOT NULL,
                hostel_name TEXT NOT NULL,
                room_number TEXT NOT NULL,
                category TEXT NOT NULL,
                description TEXT NOT NULL,
                photo_url TEXT,
                status TEXT NOT NULL,
                assigned_staff_id TEXT,
                assigned_staff_name TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                is_confirmed_by_student INTEGER NOT NULL
            )
        """.trimIndent()

        val createChatMessagesTable = """
            CREATE TABLE chat_messages (
                message_id TEXT PRIMARY KEY,
                report_id TEXT NOT NULL,
                sender_id TEXT NOT NULL,
                sender_name TEXT NOT NULL,
                sender_role TEXT NOT NULL,
                text TEXT NOT NULL,
                photo_url TEXT,
                timestamp INTEGER NOT NULL,
                sync_state TEXT NOT NULL
            )
        """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createReportsTable)
        db.execSQL(createChatMessagesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS reports")
        db.execSQL("DROP TABLE IF EXISTS chat_messages")
        onCreate(db)
    }

    fun saveUser(user: User, passwordHash: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("id", user.id)
            put("name", user.name)
            put("email", user.email.lowercase())
            put("student_id", user.studentId)
            put("role", user.role.name)
            put("assigned_hostel", user.assignedHostel)
            put("assigned_room", user.assignedRoom)
            put("profile_pic_url", user.profilePicUrl)
            put("password", passwordHash)
            put("created_at", user.createdAt)
        }
        db.insertWithOnConflict("users", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getUserByEmail(email: String): Pair<User, String>? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM users WHERE LOWER(email) = ?", arrayOf(email.trim().lowercase()))
        return cursor.use {
            if (it.moveToFirst()) {
                val user = User(
                    id = it.getString(it.getColumnIndexOrThrow("id")),
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    email = it.getString(it.getColumnIndexOrThrow("email")),
                    studentId = it.getString(it.getColumnIndexOrThrow("student_id")) ?: "",
                    role = UserRole.valueOf(it.getString(it.getColumnIndexOrThrow("role"))),
                    assignedHostel = it.getString(it.getColumnIndexOrThrow("assigned_hostel")) ?: "",
                    assignedRoom = it.getString(it.getColumnIndexOrThrow("assigned_room")) ?: "",
                    profilePicUrl = it.getString(it.getColumnIndexOrThrow("profile_pic_url")),
                    createdAt = it.getLong(it.getColumnIndexOrThrow("created_at"))
                )
                val pass = it.getString(it.getColumnIndexOrThrow("password"))
                Pair(user, pass)
            } else {
                null
            }
        }
    }

    fun getUserById(id: String): User? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM users WHERE id = ?", arrayOf(id))
        return cursor.use {
            if (it.moveToFirst()) {
                User(
                    id = it.getString(it.getColumnIndexOrThrow("id")),
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    email = it.getString(it.getColumnIndexOrThrow("email")),
                    studentId = it.getString(it.getColumnIndexOrThrow("student_id")) ?: "",
                    role = UserRole.valueOf(it.getString(it.getColumnIndexOrThrow("role"))),
                    assignedHostel = it.getString(it.getColumnIndexOrThrow("assigned_hostel")) ?: "",
                    assignedRoom = it.getString(it.getColumnIndexOrThrow("assigned_room")) ?: "",
                    profilePicUrl = it.getString(it.getColumnIndexOrThrow("profile_pic_url")),
                    createdAt = it.getLong(it.getColumnIndexOrThrow("created_at"))
                )
            } else {
                null
            }
        }
    }

    fun updateProfilePicture(userId: String, photoUrl: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("profile_pic_url", photoUrl)
        }
        db.update("users", cv, "id = ?", arrayOf(userId))
    }

    fun saveReport(report: MaintenanceReport) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("report_id", report.reportId)
            put("student_id", report.studentId)
            put("student_name", report.studentName)
            put("hostel_name", report.hostelName)
            put("room_number", report.roomNumber)
            put("category", report.category.name)
            put("description", report.description)
            put("photo_url", report.photoUrl)
            put("status", report.status.name)
            put("assigned_staff_id", report.assignedStaffId)
            put("assigned_staff_name", report.assignedStaffName)
            put("created_at", report.createdAt)
            put("updated_at", report.updatedAt)
            put("is_confirmed_by_student", if (report.isConfirmedByStudent) 1 else 0)
        }
        db.insertWithOnConflict("reports", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllReports(): List<MaintenanceReport> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM reports ORDER BY created_at DESC", null)
        val list = mutableListOf<MaintenanceReport>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    MaintenanceReport(
                        reportId = it.getString(it.getColumnIndexOrThrow("report_id")),
                        studentId = it.getString(it.getColumnIndexOrThrow("student_id")),
                        studentName = it.getString(it.getColumnIndexOrThrow("student_name")),
                        hostelName = it.getString(it.getColumnIndexOrThrow("hostel_name")),
                        roomNumber = it.getString(it.getColumnIndexOrThrow("room_number")),
                        category = ReportCategory.valueOf(it.getString(it.getColumnIndexOrThrow("category"))),
                        description = it.getString(it.getColumnIndexOrThrow("description")),
                        photoUrl = it.getString(it.getColumnIndexOrThrow("photo_url")),
                        status = ReportStatus.valueOf(it.getString(it.getColumnIndexOrThrow("status"))),
                        assignedStaffId = it.getString(it.getColumnIndexOrThrow("assigned_staff_id")),
                        assignedStaffName = it.getString(it.getColumnIndexOrThrow("assigned_staff_name")),
                        createdAt = it.getLong(it.getColumnIndexOrThrow("created_at")),
                        updatedAt = it.getLong(it.getColumnIndexOrThrow("updated_at")),
                        isConfirmedByStudent = it.getInt(it.getColumnIndexOrThrow("is_confirmed_by_student")) == 1
                    )
                )
            }
        }
        return list
    }

    fun saveChatMessage(message: ChatMessage) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("message_id", message.messageId)
            put("report_id", message.reportId)
            put("sender_id", message.senderId)
            put("sender_name", message.senderName)
            put("sender_role", message.senderRole.name)
            put("text", message.text)
            put("photo_url", message.photoUrl)
            put("timestamp", message.timestamp)
            put("sync_state", message.syncState.name)
        }
        db.insertWithOnConflict("chat_messages", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getChatMessagesForReport(reportId: String): List<ChatMessage> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM chat_messages WHERE report_id = ? ORDER BY timestamp ASC", arrayOf(reportId))
        val list = mutableListOf<ChatMessage>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    ChatMessage(
                        messageId = it.getString(it.getColumnIndexOrThrow("message_id")),
                        reportId = it.getString(it.getColumnIndexOrThrow("report_id")),
                        senderId = it.getString(it.getColumnIndexOrThrow("sender_id")),
                        senderName = it.getString(it.getColumnIndexOrThrow("sender_name")),
                        senderRole = UserRole.valueOf(it.getString(it.getColumnIndexOrThrow("sender_role"))),
                        text = it.getString(it.getColumnIndexOrThrow("text")),
                        photoUrl = it.getString(it.getColumnIndexOrThrow("photo_url")),
                        timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp")),
                        syncState = SyncState.valueOf(it.getString(it.getColumnIndexOrThrow("sync_state")))
                    )
                )
            }
        }
        return list
    }

    fun getPendingSyncMessages(): List<ChatMessage> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM chat_messages WHERE sync_state = ?", arrayOf(SyncState.PENDING_SYNC.name))
        val list = mutableListOf<ChatMessage>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    ChatMessage(
                        messageId = it.getString(it.getColumnIndexOrThrow("message_id")),
                        reportId = it.getString(it.getColumnIndexOrThrow("report_id")),
                        senderId = it.getString(it.getColumnIndexOrThrow("sender_id")),
                        senderName = it.getString(it.getColumnIndexOrThrow("sender_name")),
                        senderRole = UserRole.valueOf(it.getString(it.getColumnIndexOrThrow("sender_role"))),
                        text = it.getString(it.getColumnIndexOrThrow("text")),
                        photoUrl = it.getString(it.getColumnIndexOrThrow("photo_url")),
                        timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp")),
                        syncState = SyncState.valueOf(it.getString(it.getColumnIndexOrThrow("sync_state")))
                    )
                )
            }
        }
        return list
    }

    fun markMessageAsSynced(messageId: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("sync_state", SyncState.SYNCED.name)
        }
        db.update("chat_messages", cv, "message_id = ?", arrayOf(messageId))
    }
}
