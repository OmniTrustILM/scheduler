package db.migration;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
                    if (updated.length > 0) {
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

    private byte[] replaceInSerializedBytes(byte[] data) {
        byte[] oldBytes = "com.czertainly".getBytes();
        byte[] newBytes = "com.otilm".getBytes();

        // Find the old string in the blob
        int pos = indexOf(data, oldBytes);
        if (pos < 0) return new byte[0];

        // The 2-byte length prefix is 2 bytes before the string content
        // (after the TC_STRING opcode 0x74)
        int lengthPos = pos - 2;
        int oldLength = ((data[lengthPos] & 0xFF) << 8) | (data[lengthPos + 1] & 0xFF);
        int newLength = oldLength - (oldBytes.length - newBytes.length);

        // Build new byte array
        byte[] result = new byte[data.length - (oldBytes.length - newBytes.length)];
        System.arraycopy(data, 0, result, 0, lengthPos);
        result[lengthPos] = (byte) ((newLength >> 8) & 0xFF);
        result[lengthPos + 1] = (byte) (newLength & 0xFF);
        System.arraycopy(newBytes, 0, result, lengthPos + 2, newBytes.length);
        System.arraycopy(data, pos + oldBytes.length, result, lengthPos + 2 + newBytes.length,
                data.length - pos - oldBytes.length);
        return result;
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
        return null;
    }
}
