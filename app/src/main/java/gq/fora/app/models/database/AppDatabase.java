package gq.fora.app.models.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import gq.fora.app.models.database.User;
import gq.fora.app.models.database.UserDao;

@Database(
        entities = {User.class},
        version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
}
