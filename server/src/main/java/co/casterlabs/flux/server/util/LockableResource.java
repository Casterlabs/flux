package co.casterlabs.flux.server.util;

import java.util.concurrent.locks.ReentrantLock;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LockableResource<T> {
    private final ReentrantLock lock = new ReentrantLock();

    private volatile T resource;

    public void set(T resource) {
        this.lock.lock();
        try {
            this.resource = resource;
        } finally {
            this.lock.unlock();
        }
    }

    public T acquire() {
        this.lock.lock();
        return this.resource;
    }

    public T acquireUnsafe() {
        return this.resource;
    }

    public void release() {
        this.lock.unlock();
    }

}
