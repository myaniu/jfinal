package com.jfinal.log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractLogFactory implements ILogFactory {
	private List<Log> logs = new ArrayList<Log>();
	private ReentrantLock lock = new ReentrantLock();

	public void setLevel(int level) {
		try{
			lock.lock();
			for(Log log : logs){
				log.setLevel(level);
			}
		}finally{
			lock.unlock();
		}
	}
	
	protected void addLog(Log log){
		try{
			lock.lock();
			this.logs.add(log);
		}finally{
			lock.unlock();
		}
	}
	
}
