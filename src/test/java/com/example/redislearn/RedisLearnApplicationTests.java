package com.example.redislearn;

import com.example.redislearn.utils.RedisServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.*;

import javax.annotation.Resource;
import java.sql.SQLOutput;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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

        delaySet.stream().forEach(s -> {
            System.out.println("取出的任务如下："+s);
            redisService.zrem("delaySet",s);
        });

    }


    @Test
    void pubSub(){
        Jedis resource = null;

        try {
            resource = pool.getResource();

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(resource != null){
                resource.close();
            }
        }


    }



/**
 *
 *  redis实现事务=================================================================
 *  redis事务没有原子性，某条命令失败不会回滚
 */

    @Test
    void transaction(){
        Jedis resource = null;

        try {
            resource = pool.getResource();
            //获取事务
            Transaction multi = resource.multi();
            //往事务中添加命令
            multi.set("hello","work");
            multi.expire("hello",50);
            //执行
            multi.exec();

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(resource != null){
                resource.close();
            }
        }


    }


/**
 *
 *  redis 特殊数据类型 geo\hyberloglog\bitmap=================================================================
 *
 *  hyberloglog:  PFADD、pfcount、pfmerge 场景：大数据用，作为基数统计用，去重，可能有误差
 *
 *  redis 127.0.0.1:6379> PFADD runoobkey "redis"
 *
 * 1) (integer) 1
 *
 * redis 127.0.0.1:6379> PFADD runoobkey "mongodb"
 *
 * 1) (integer) 1
 *
 * redis 127.0.0.1:6379> PFADD runoobkey "mysql"
 *
 * 1) (integer) 1
 *
 * redis 127.0.0.1:6379> PFCOUNT runoobkey
 *
 * (integer) 3
 *
 *  统计注册 IP 数
 * 统计每日访问 IP 数
 * 统计页面实时 UV 数
 * 统计在线用户数
 * 统计用户每天搜索不同词条的个数
 *
 * bitmap: 作为布隆过滤器用，快速判断一个数是否存在\实现签到的场景
 * # SETBIT 会返回之前位的值（默认是 0）这里会生成 126 个位
 * coderknock> SETBIT testBit 125 1
 * (integer) 0
 * coderknock> SETBIT testBit 125 0
 * (integer) 1
 * coderknock> SETBIT testBit 125 1
 * (integer) 0
 * coderknock> GETBIT testBit 125
 * (integer) 1
 * coderknock> GETBIT testBit 100
 * (integer) 0
 * # SETBIT  value 只能是 0 或者 1  二进制只能是0或者1
 * coderknock> SETBIT testBit 618 2
 * (error) ERR bit is not an integer or out of range
 *
 *
 *  geo
 *
 *  1）增加地理位置信息  geo add key longitude latitude member
 *  向cars:locations中增加车辆编号为1以及车辆编号为2的位置信息。
 *  geoadd cars:locations 120.346111 31.556381 1 120.375821 31.560368 2
 *
 *  获取车辆编号为1的车辆位置信息
 *   geopos cars:locations 1
 *
 *   获取两个地理位置的距离
 *
 *   geodist cars:locations 1 2 km
 *
 *   获取指定位置范围的地理信息位置集合
 *   GEORADIUS key longitude latitude radius m|km|ft|mi [WITHCOORD] [WITHDIST] [WITHHASH] [ASC|DESC] [COUNT count]
 *
 *
 */



    @Test
    void hyberloglog(){

        Jedis resource = null;

        try {
            resource = pool.getResource();
            resource.pfadd("usercount","user1");
            resource.pfadd("usercount","user2");
            resource.pfadd("usercount","user3");
            resource.pfadd("usercount","user4");
            long usercount = resource.pfcount("usercount");
            System.out.println("在线人数:"+usercount);



        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(resource != null){
                resource.close();
            }
        }


    }

    /**
     * 做布隆过滤器用
     */
    @Test
    void bitmap(){
        Jedis resource = null;

        try {
            resource = pool.getResource();
            Boolean testBit = resource.setbit("testBit", 1, true);
            System.out.println("==========:"+testBit);
            Boolean testBit1 = resource.getbit("testBit", 1);
            System.out.println("==========:"+testBit1);

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(resource != null){
                resource.close();
            }
        }


    }



    @Test
    void geo(){
        Jedis resource = null;

        try {
            resource = pool.getResource();
            resource.geoadd("cars:locations",120.346111,31.556381,"1");
            resource.geoadd("cars:locations",120.246111,31.156381,"2");
            resource.geopos("cars:locations","1");
            Double geodist = resource.geodist("cars:locations", "1", "2", GeoUnit.KM);
            System.out.println("距离："+geodist);
            //查询附近有多少
            //经度120.375821纬度31.556381为中心查找5公里范围内的车辆
            //GEORADIUS cars:locations 120.375821 31.556381 5 km WITHCOORD WITHDIST WITHHASH  ASC COUNT 100
            List<GeoRadiusResponse> georadius = resource.georadius("cars:locations", 120.375821, 31.556381, 5, GeoUnit.KM);


        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(resource != null){
                resource.close();
            }
        }


    }

    /**
     *
     * 测试原子自增、自減，作为计数器使用，预减库存场景
     */

    @Test
    void incr(){
        Jedis resource = null;

        try {

            resource = pool.getResource();

            //--------------以下都是原子操作---------------
            //String类型的原子递增
            Long testInc = resource.incr("testInc");
            Long testInc1 = resource.incrBy("testInc",1);
            //hash也可以给某一个属性递增
            //所以如果是同一类可以使用hash作为计数结够，例如微博点赞计数
            resource.hincrBy("hashkey","hashfield1::",1);

            //sortedset
            resource.zincrby("key",1,"member");

            //hash field的模糊匹配
            ScanParams params = new ScanParams();
            params.match("*:like:total");
            ScanResult<Map.Entry<String, String>> scanResult = resource.hscan("weibo", "0", params);


        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(resource != null){
                resource.close();
            }
        }


    }

    @Test
    void decr(){
        Jedis resource = null;

        try {
            resource = pool.getResource();
            Long testInc = resource.decr("testInc");


        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(resource != null){
                resource.close();
            }
        }


    }



    //===================================使用lua脚本实现并发原子性===================


    /**
     * 获取分布式锁
     * @param key
     * @param value
     * @param mills
     * @return
     */
    public boolean lock(String key,String value,long mills){
        try(Jedis jedis=pool.getResource()){
            String result=jedis.set(key,value,"NX","PX",mills);

            if("OK".equals(result)){
                return true;
            }
            return false;
        }catch (Exception e){
            e.printStackTrace();
//            log.warn(e.getMessage());
            return false;
        }
    }

    /**
     * 释放分布式锁
     * @param key
     * @param value
     * @return
     */
    public boolean unlock(String key,String value){
        String script="if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        try(Jedis jedis=pool.getResource()){
            Object result=jedis.eval(script, Collections.singletonList(key),Collections.singletonList(value));

            if(result.equals(1L)){
                return true;
            }
            return false;
        }catch (Exception e){
            e.printStackTrace();
//            log.warn(e.getMessage());
            return false;
        }
    }









}
