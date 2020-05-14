package com.example.redislearn;

import com.example.redislearn.utils.RedisServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.sql.SQLOutput;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

@SpringBootTest
class RedisLearnApplicationTests {

    @Autowired
    private RedisServiceImpl redisService;


    /**
     * sinter 、sunion 、sdiff
     * redis 支持 Set集合的数据存储，其中有三个比较特殊的方法：
     *
     * sinter key [key …] 返回一个集合的全部成员，该集合是所有给定集合的交集。
     * sunion key [key …] 返回一个集合的全部成员，该集合是所有给定集合的并集。
     * sdiff key [key …] 返回所有给定 key 与第一个 key 的差集
     *
     */
    @Test
    void contextLoads() {

        redisService.set("teststet","world1");
    }

    @Autowired
    private JedisPool pool;


    /**
     * 方法不能是private
     * set交集：查找共同好友之类场景
     *
     */
    @Test
     void test1(){
        Jedis resource = pool.getResource();
        resource.sadd("set","test1");
        resource.sadd("set","test2");
        resource.sadd("set","test3");
        resource.sadd("set","test4");
        resource.sadd("set","only set");

        redisService.sadd("set1","test1");
        redisService.sadd("set1","test2");
        redisService.sadd("set1","test3");
        redisService.sadd("set1","only set1");


        redisService.sadd("set2","test1");
        redisService.sadd("set2","test2");


        //交集
        Set<String> sinter = redisService.sinter("set1", "set","set2");
        sinter.stream().forEach(s -> System.out.println("交集："+ s));

        //并集
        Set<String> sunion = redisService.sunion("set1", "set", "set2");
        sunion.stream().forEach(s -> System.out.println("并集："+s));

        //差集
        Set<String> sdiff = redisService.sdiff("set1", "set", "set2");
        sdiff.stream().forEach(s -> System.out.println("差集："+s));


        Set<String> set = resource.smembers("set");

        set.stream().forEach(s -> System.out.println("###:"+s));
    }

    @Test
    void remove(){
        Long srem = redisService.srem("set2", "test3");
        System.out.println("返回值："+srem);

    }


/**
 *
 *  redis实现队列=================================================================
 */


    /**
     * 1、利用list实现一个消息队列
     *list 还有个指令叫 blpop，在没有消息的时候，它会阻塞住直到消息到来
     */
    @Test
    void queue(){
       redisService.lpush("list1","a");
       redisService.lpush("list1","b");
       redisService.lpush("list1","c");


       while (redisService.llen("list1")>0){
           String list1 = redisService.rpop("list1");
           System.out.println("取出队列："+list1);

       }
        System.out.println("完成");

    }


    /**
     * 实现延时队列
     * 1、利用zset实现延迟队列，score为时间戳，value为任务内容
     * 2、ZRANGEBYSCORE 取出任务
     * 3、如果最小的分数小于等于当前时间戳，就将该任务取出来执行，否则休眠一段时间后再查询
     *  score从小到大排序
     */
    @Test
    void delayQueue() throws InterruptedException, ParseException {

        String date = "2020-05-14 18:00:00";
        String date1 = "2020-05-14 17:53:00";
        String date2 = "2020-05-14 17:54:00";
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date parse = format.parse(date);
        Date parse1 = format.parse(date1);
        Date parse2 = format.parse(date2);
        long l = System.currentTimeMillis();
        redisService.zadd("delaySet",parse.getTime(),"job1");
        long l1 = System.currentTimeMillis();
        redisService.zadd("delaySet",parse1.getTime(),"job2");
        long l2 = System.currentTimeMillis();
        redisService.zadd("delaySet",parse2.getTime(),"job3");

        Jedis resource = pool.getResource();
        //从一个分数区间取出值，指定个数
//        Set<String> delaySet = resource.zrangeByScore("delaySet", parse.getTime() - 3000000, parse.getTime() + 30000000, 0, 1);
        //从一个区间取出值，不指定个数
        Set<String> delaySet = resource.zrangeByScore("delaySet", parse.getTime() - 3000000, parse.getTime() + 30000000);


        delaySet.stream().forEach(s -> System.out.println("取出的任务如下："+s));


    }



/**
 *
 *  redis实现事务=================================================================
 *  redis事务没有原子性，某条命令失败不会回滚
 */




}
