package co.casterlabs.flux.server.util;

import java.util.concurrent.TimeUnit;

import co.casterlabs.rakurai.json.annotating.JsonClass;

public enum Profiler {
    // @formatter:off
    TUBE_PUBLISH, META_TUBE_PUBLISH,
    TUBE_GET,
    TUBE_REGISTER,
    TUBE_UNREGISTER,
    
    CLIENT_INCOMING,
    
    AUTHENTICATION,
    AUTHENTICATION_CHECK_SEND,
    AUTHENTICATION_CHECK_RECV, 

    PROTOCOL_JSON_PARSE,
    PROTOCOL_JSON_SERIALIZE,

    DAEMON_HTTP_POST, 
    DAEMON_HTTP_WEBSOCKET_CONNECT,
    
    FLUX_KEEPALIVE,
    // @formatter:on
    ;

    public static final long EXPIRE_AFTER = TimeUnit.SECONDS.toMillis(10);

    private final LockableResource<ProfileEntry[]> entries = new LockableResource<>(new ProfileEntry[10_000]);
    private volatile int entryPtr = 0;
    private volatile long total = 0;

    public ProfilerAverage average() {
        long now = System.currentTimeMillis();
        long counter = 0;
        long sum = 0;

        ProfileEntry[] times = this.entries.acquire();
        try {
            for (int idx = 0; idx < times.length; idx++) {
                ProfileEntry entry = times[idx];
                if (entry == null) continue;
                if (entry.expires > now) {
                    counter++;
                    sum += entry.timeTook;
                }
            }
        } finally {
            this.entries.release();
        }

        if (sum == 0 || counter == 0) {
            return new ProfilerAverage(this.total, counter, 0, now);
        }

        return new ProfilerAverage(this.total, counter, sum / counter, now);
    }

    public Profile start() {
        return new Profile(this, System.currentTimeMillis());
    }

    public record Profile(Profiler type, long start) {
        public void end() {
            long end = System.currentTimeMillis();
            ProfileEntry entry = new ProfileEntry(
                end - this.start,
                end + EXPIRE_AFTER
            );

            ProfileEntry[] times = this.type.entries.acquire();
            try {
                // We are allowed to overwrite entries, even if they may not be expired.
                // The idea is to be unobtrusive to the application. Stats are low priority.
                times[this.type.entryPtr] = entry;
                this.type.entryPtr = (this.type.entryPtr + 1) % times.length;
                this.type.total++;
            } finally {
                this.type.entries.release();
            }
        }
    }

    private record ProfileEntry(long timeTook, long expires) {
    }

    @JsonClass(exposeAll = true)
    public record ProfilerAverage(long total, long samples, long averageTime, long takenAt) {
    }

}
