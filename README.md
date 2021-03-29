# Shopping
仿高并发场景下的一个抢购秒杀系统，主要是利用redis缓存和rabbitmq的异步消息来减轻服务器压力。

## 技术栈

### 后端框架

* SpringBoot/MyBatis/JSR303

### 前端框架

* Thymleaf/Bootstrap/JQuery

```
考虑用react重构前端
```

### 中间件

* RabbitMQ/Redis/Druid

## Redis的使用

项目中redis用字符类型来缓存对象，保存对象前，先将对象转为String类型。

### 分布式Session

* 生成随机的uuid作为cookie返回并在redis内存写入 
* 拦截器每次拦截方法，来重新获根据cookie获取对象 
* 获取到了对象后，可以判断用户是否登录。

### Redis预减库存

```
需要实现InitializingBean这个接口，并在afterPropertiesSet方法中可以在MiaoshaController这个Bean被初始化后被调用，可以使用将商品数据（主要是商品id和初始库存）先保存到redis中，并且初始化内存标记。
```

系统加载的时候将抢购的商品缓存在redis中. 后续秒杀时直接预减redis中的商品总数.

系统秒杀时返回排队

1. redis预减缓存,减少对数据库访问
2. 内存标记减少redis的访问

用HashMap作为一个内存标记

```java
for(GoodsVo goods : goodsList)
	localOverMap.put(goods.getGoods().getId(),false);
```

当这个产品的库存量小于0时，将这个标记设置为true，那么在秒杀前可以进行判断商品是否已经秒杀完了。

```java
//利用缓存判断库存
        boolean over = localOverMap.get(goodsId);
        if(over){
            return Result.error(CodeMsg.SESSION_ERROR);
        }
```



## 抢购的流程：

**前端发起请求到后端返回的过程**

1. 前端点击【立即秒杀】，向后端发送获取秒杀路径请求，附带输入的验证码，路径：/miaosha/path
2. 后端接受到请求，先验证验证码，再生成一个路径返回给前端
3. 这个路径是用uuid生成，并且在redis中保存这个路径

```java
redisService.set(MiaoshaKey.getMiaoshaPath, ""+user.getId() + "_"+ goodsId, str);
```

1. 前端拿到路径后，再对后端秒杀接口进行访问
2. 后端根据请求的goods_id和user_id到redis中获取存入的路径值pathOld，如果前端请求的path和pathOld相等，那么可以进行秒杀，否则返回。
3. 后端根据redis内存标记判断是不是该商品已经被秒杀完毕了，已经被秒杀完了的话就返回秒杀结束
4. 在redis中预减库存，如果预减库存后小于0，那么说明商品已卖完，那么返回。
5. 判断是否重复秒杀，如果没有秒杀的话，那么创建秒杀消息并**加入队列**，返回Result.success(0)，表明排队中。

```java
//判断是否已经秒杀到了
MiaoshaOrder order=orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(),goodsId);
    if(order!=null){
        return Result.error(CodeMsg.REPEATE_MIAOSHA);
}
这个方法实际上是判断缓存中是否有相关记录
return redisService.get(OrderKey.getMiaoshaOrderByUidGid, ""+userId+"_"+goodsId, MiaoshaOrder.class);
```

**后端接收到消息后的处理**

后端从消息队列接收到秒杀消息后，先访问数据库，检查被秒杀的商品库存是否小于等于0，是的话，直接返回。再访问redis缓存，查看是否已经缓存这个订单了，是的话，直接返回。最后，调用秒杀service，进行秒杀

1. 秒杀的流程加了@Transactional注解，流程就是减库存，下订单，写入秒杀订单。

```
减库存这里的sql的where中有库存大于0的判断
减库存成功了，就创建订单和秒杀订单（这一步加了@Transaction注解），并且在redis中存储秒杀订单信息
redisService.set(OrderKey.getMiaoshaOrderByUidGid, 
				""+user.getId()+"_"+goods.getGoods().getId(), miaoshaOrder);
减库存失败的话，在redis中设置标记
redisService.set(MiaoshaKey.isGoodsOver, ""+goodsId, true);
因为这表明商品已经卖完了。
```

> 前端在接收到返回值之后会对后台接口`miaosha/result`进行轮询，后端首先会从redis中查询是否有这个订单，如果有，说明秒杀成功，返回订单id。如果没有，采用语句`redisService.exists(MiaoshaKey.isGoodsOver, ""+goodsId)` 查询商品是否已经卖完了，是的话返回-1，表示失败。否则返回0，表示排队中。

## 采用的方案

### 防止卖超

1. 减少库存的时候，在sql中**判断库存是否大于0**

```java
@Update("update miaosha_goods set stock_count=stock_count-1 where goods_id = #{goodsId} and stock_count>0")
public int reduceStock(MiaoshaGoods miaoshaGoods);
```

1. 给miaoshaOrder表中的user_id和goods_id加上唯一索引，这样就从数据库层面上**防止用户重复购买**这个产品了
2. 采用redis预减库存：

在秒杀的情况下，高频率的去读写数据库，会严重造成性能问题。所以必须借助其他服务， 利用 redis 的**单线程预减库存**。比如商品有 100 件。那么我在 redis 存储一个 k,v。例如，每一个用户线程进来，key 值就减 1，等减到 0 的时候，全部拒绝剩下的请求。

那么也就是只有 100 个线程会进入到后续操作。所以一定不会出现超卖的现象。

## 实现的效果：

* Jmeter进行压力测试

* 最开始没有优化前，QPS只有900多。
* 优化之后，QPS达到了2000左右。

## 还需要改进的地方

- 没有考虑redis穿透的情况处理方案
- 没有做接口限流
- RabbitMQ的关于保证幂等性的方案还需要去完成
- 以后可以做一个redis集群和数据库读写分离来保证性能和可用性