package com.azk.pulse.storage;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseProvider extends StorageProvider {
    Connection openConnection() throws SQLException;
}
