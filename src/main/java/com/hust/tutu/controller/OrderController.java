package com.hust.tutu.controller;

import com.hust.tutu.Vo.GoodsVo;
import com.hust.tutu.domain.MiaoshaUser;
import com.hust.tutu.domain.OrderDetailVo;
import com.hust.tutu.domain.OrderInfo;
import com.hust.tutu.redis.RedisService;
import com.hust.tutu.result.CodeMsg;
import com.hust.tutu.result.Result;
import com.hust.tutu.service.GoodsService;
import com.hust.tutu.service.MiaoshaUserService;
import com.hust.tutu.service.OrderService;
import com.hust.tutu.validator.NeedLogin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    MiaoshaUserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    OrderService orderService;

    @Autowired
    GoodsService goodsService;

    @RequestMapping("/detail")
    @ResponseBody
    @NeedLogin
    public Result<OrderDetailVo> info(MiaoshaUser user,
                                      @RequestParam("orderId") long orderId){
        if(user==null){
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        OrderInfo order=orderService.getOrderById(orderId);
        if(order == null){
            return Result.error(CodeMsg.ORDER_NOT_EXIST);
        }
        long goodsId = order.getGoodsId();
        GoodsVo goods=goodsService.getGoodsVoByGoodsId(goodsId);
        OrderDetailVo vo = new OrderDetailVo();
        vo.setOrder(order);
        vo.setGoods(goods);
        return Result.success(vo);
    }
}
