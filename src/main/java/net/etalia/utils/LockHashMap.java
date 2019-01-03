package net.etalia.utils;

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockHashMap<K, V> extends HashMap<K, V> {

	private final ReadWriteLock rwlock = new ReentrantReadWriteLock();
	private final Lock rlock = rwlock.readLock();
	private final Lock wlock = rwlock.writeLock();
	
	public void lockRead() {
		rlock.lock();
	}
	
	public void unlockRead() {
		rlock.unlock();
	}
	
	public void lockWrite() {
		wlock.lock();
	}
	
	public void unlockWrite() {
		wlock.unlock();
	}
	
}
