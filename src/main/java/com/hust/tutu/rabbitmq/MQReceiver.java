package com.hust.tutu.rabbitmq;

import com.hust.tutu.domain.MiaoshaUser;
import com.hust.tutu.redis.RedisService;
import com.hust.tutu.service.MiaoshaService;
import com.hust.tutu.Vo.GoodsVo;
import com.hust.tutu.domain.MiaoshaOrder;
import com.hust.tutu.service.GoodsService;
import com.hust.tutu.service.OrderService;
import com.hust.tutu.util.ObjectUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MQReceiver {

    @Autowired
    RedisService redisService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    MiaoshaService miaoshaService;

    @RabbitListener(queues=MQConfig.MIAOSHA_QUEUE)
    public void receive(String message) {
        MiaoshaMessage mm  = ObjectUtils.strToBean(message, MiaoshaMessage.class);
        MiaoshaUser user = mm.getUser();
        long goodsId = mm.getGoodsId();

        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        int stock = goods.getStockCount();
        if(stock <= 0) {
            return;
        }
        //判断是否已经秒杀到了
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
        if(order != null) {
            return;
        }
        //减库存 下订单 写入秒杀订单
        miaoshaService.miaosha(user, goods);
    }
}
