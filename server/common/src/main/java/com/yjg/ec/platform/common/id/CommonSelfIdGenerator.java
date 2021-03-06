package com.yjg.ec.platform.common.id;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yjg.ec.platform.common.exception.BusinessException;
import com.yjg.ec.platform.common.exception.ParamException;

public class CommonSelfIdGenerator implements IDGenerator{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CommonSelfIdGenerator.class);
	 
	public static final long SJDBC_EPOCH;
    
    private static final long SEQUENCE_BITS = 12L;
    
    private static final long WORKER_ID_BITS = 10L;
    
    private static final long SEQUENCE_MASK = (1 << SEQUENCE_BITS) - 1;
    
    private static final long WORKER_ID_LEFT_SHIFT_BITS = SEQUENCE_BITS;
    
    private static final long TIMESTAMP_LEFT_SHIFT_BITS = WORKER_ID_LEFT_SHIFT_BITS + WORKER_ID_BITS;
    
    private static final long WORKER_ID_MAX_VALUE = 1L << WORKER_ID_BITS;
    
    private static AbstractClock clock = AbstractClock.systemClock();
    
    private static long workerId;
    
    static {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2016, Calendar.NOVEMBER, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        SJDBC_EPOCH = calendar.getTimeInMillis();
//        initWorkerId();
    }
    
    private long sequence;
    
    private long lastTime;
    
//    static void initWorkerId() {
//        String workerId = System.getProperty("sjdbc.self.id.generator.worker.id");
//        if (!StringUtils.isNotBlank(workerId)) {
//            setWorkerId(Long.valueOf(workerId));
//            return;
//        }
//        workerId = System.getenv("SJDBC_SELF_ID_GENERATOR_WORKER_ID");
//        if (StringUtils.isBlank(workerId)) {
//            return;
//        }
//        setWorkerId(Long.valueOf(workerId));
//    }
    
    /**
     * 设置工作进程Id.
     * 
     * @param workerId 工作进程Id
     */
    public static void setWorkerId(final Long workerId) {
    	System.out.println("workerId:"+workerId);
        if(workerId >= 0L && workerId < WORKER_ID_MAX_VALUE){
        	CommonSelfIdGenerator.workerId = workerId;
        }else{
        	throw new ParamException("workid is invaild.");
        }
    }
    
    /**
     * 生成Id.
     * 
     * @return 返回@{@link Long}类型的Id
     */
    @Override
    public synchronized Number generateId() {
        long time = clock.millis();
        if(time<lastTime){
        	throw new BusinessException(String.format("Clock is moving backwards, last time is %d milliseconds, current time is %d milliseconds", lastTime, time));
        }
        if (lastTime == time) {
            if (0L == (sequence = ++sequence & SEQUENCE_MASK)) {
                time = waitUntilNextTime(time);
            }
        } else {
            sequence = 0;
        }
        lastTime = time;
        if(LOGGER.isDebugEnabled())
        	LOGGER.debug("{}-{}-{}", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(lastTime)), workerId, sequence);
        return ((time - SJDBC_EPOCH) << TIMESTAMP_LEFT_SHIFT_BITS) | (workerId << WORKER_ID_LEFT_SHIFT_BITS) | sequence;
    }
    
    private long waitUntilNextTime(final long lastTime) {
        long time = clock.millis();
        while (time <= lastTime) {
            time = clock.millis();
        }
        return time;
    }
}
