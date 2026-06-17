package db.migration;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;

public class RebrandJobDataMigration implements CustomTaskChange {

    @Override
    public void execute(Database database) throws CustomChangeException {
        try {
            JdbcConnection conn = (JdbcConnection) database.getConnection();

            try (var stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT sched_name, job_name, job_group, job_data FROM qrtz_job_details WHERE job_data IS NOT NULL")) {

                while (rs.next()) {
                    byte[] jobData = rs.getBytes("job_data");
                    byte[] updated = replaceInSerializedBytes(jobData);
                    if (!Arrays.equals(updated, jobData)) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE qrtz_job_details SET job_data = ? WHERE sched_name = ? AND job_name = ? AND job_group = ?")) {
                            ps.setBytes(1, updated);
                            ps.setString(2, rs.getString("sched_name"));
                            ps.setString(3, rs.getString("job_name"));
                            ps.setString(4, rs.getString("job_group"));
                            ps.executeUpdate();
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new CustomChangeException("Failed to migrate job_data", e);
        }
    }

    byte[] replaceInSerializedBytes(byte[] data) {
        byte[] oldBytes = "com.czertainly".getBytes(StandardCharsets.UTF_8);
        byte[] newBytes = "com.otilm".getBytes(StandardCharsets.UTF_8);

        // Replace all occurrences — a JobDataMap blob can contain the package name
        // in multiple places (class descriptors and string values).
        int pos;
        while ((pos = indexOf(data, oldBytes)) >= 0) {
            // Java serialization encodes strings with a 2-byte big-endian length
            // immediately before the string content (TC_STRING 0x74 / TC_CLASSDESC 0x72).
            int lengthPos = pos - 2;
            int oldLength = ((data[lengthPos] & 0xFF) << 8) | (data[lengthPos + 1] & 0xFF);
            int newLength = oldLength - (oldBytes.length - newBytes.length);

            byte[] result = new byte[data.length - (oldBytes.length - newBytes.length)];
            System.arraycopy(data, 0, result, 0, lengthPos);
            result[lengthPos] = (byte) ((newLength >> 8) & 0xFF);
            result[lengthPos + 1] = (byte) (newLength & 0xFF);
            System.arraycopy(newBytes, 0, result, lengthPos + 2, newBytes.length);
            System.arraycopy(data, pos + oldBytes.length, result, lengthPos + 2 + newBytes.length,
                    data.length - pos - oldBytes.length);
            data = result;
        }
        return data;
    }

    private int indexOf(byte[] data, byte[] pattern) {
        for (int i = 0; i <= data.length - pattern.length; i++) {
            if (matches(data, i, pattern)) return i;
        }
        return -1;
    }

    private boolean matches(byte[] data, int offset, byte[] pattern) {
        for (int j = 0; j < pattern.length; j++) {
            if (data[offset + j] != pattern[j]) return false;
        }
        return true;
    }

    @Override
    public String getConfirmationMessage() {
        // Logged by Liquibase after successful execution
        return "Job data rebranded from com.czertainly to com.otilm";
    }

    @Override
    public void setUp() throws SetupException {
        // No initialization needed
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
        // No resource loading needed
    }

    @Override
    public ValidationErrors validate(Database database) {
        // No pre-execution validation needed
        return new ValidationErrors();
    }
}
