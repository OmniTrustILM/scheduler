package db.migration;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RebrandJobDataMigrationTest {

    private final RebrandJobDataMigration migration = new RebrandJobDataMigration();

    @Test
    void replacesJobClassValue() throws Exception {
        byte[] serialized = serialize(Map.of("JOBCLASS", "com.czertainly.scheduler.job.DiscoveryJob"));

        byte[] result = migration.replaceInSerializedBytes(serialized);

        Map<?, ?> deserialized = deserialize(result);
        assertEquals("com.otilm.scheduler.job.DiscoveryJob", deserialized.get("JOBCLASS"));
    }

    @Test
    void replacesAllOccurrences() throws Exception {
        // Two entries both containing com.czertainly
        Map<String, String> input = new HashMap<>();
        input.put("JOBCLASS", "com.czertainly.scheduler.job.DiscoveryJob");
        input.put("OTHER", "com.czertainly.scheduler.job.OtherJob");
        byte[] serialized = serialize(input);

        byte[] result = migration.replaceInSerializedBytes(serialized);

        Map<?, ?> deserialized = deserialize(result);
        assertEquals("com.otilm.scheduler.job.DiscoveryJob", deserialized.get("JOBCLASS"));
        assertEquals("com.otilm.scheduler.job.OtherJob", deserialized.get("OTHER"));
    }

    @Test
    void returnsOriginalBytesWhenNoMatch() throws Exception {
        byte[] serialized = serialize(Map.of("JOBCLASS", "com.otilm.scheduler.job.DiscoveryJob"));

        byte[] result = migration.replaceInSerializedBytes(serialized);

        assertArrayEquals(serialized, result);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> deserialize(byte[] bytes) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (Map<String, String>) ois.readObject();
        }
    }

    private static byte[] serialize(Map<String, String> map) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(new HashMap<>(map));
        }
        return baos.toByteArray();
    }
}
