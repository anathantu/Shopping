package com.hust.tutu.controller;

import com.hust.tutu.domain.MiaoshaUser;
import com.hust.tutu.redis.OrderKey;
import com.hust.tutu.result.Result;
import com.hust.tutu.Vo.GoodsVo;
import com.hust.tutu.domain.MiaoshaOrder;
import com.hust.tutu.rabbitmq.MQSender;
import com.hust.tutu.rabbitmq.MiaoshaMessage;
import com.hust.tutu.redis.GoodsKey;
import com.hust.tutu.redis.MiaoshaKey;
import com.hust.tutu.redis.RedisService;
import com.hust.tutu.result.CodeMsg;
import com.hust.tutu.service.GoodsService;
import com.hust.tutu.service.MiaoshaService;
import com.hust.tutu.service.MiaoshaUserService;
import com.hust.tutu.service.OrderService;
import com.hust.tutu.validator.NeedLogin;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

@Controller
@RequestMapping("/miaosha")
public class MiaoshaController implements InitializingBean {

    @Autowired
    MiaoshaUserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    MiaoshaService miaoshaService;

    @Autowired
    MQSender sender;

    private HashMap<Long, Boolean> localOverMap = new HashMap<>();


    @RequestMapping(value="/reset", method=RequestMethod.GET)
    @ResponseBody
    public Result<Boolean> reset() {
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        for(GoodsVo goods : goodsList) {
            goods.setStockCount(10);
            redisService.set(GoodsKey.getMiaoshaGoodsStock, ""+goods.getGoods().getId(), 10);
            localOverMap.put(goods.getGoods().getId(), false);
        }
        redisService.delete(OrderKey.getMiaoshaOrderByUidGid);
        redisService.delete(MiaoshaKey.isGoodsOver);
        miaoshaService.reset(goodsList);
        return Result.success(true);
    }



    @RequestMapping(value="/{path}/do_miaosha",method = RequestMethod.POST)
    @ResponseBody
    @NeedLogin
    public Result<Integer> miaosha(MiaoshaUser user, @RequestParam("goodsId") long goodsId,
                                   @PathVariable("path") String path) {
        //验证path
        boolean check = miaoshaService.checkPath(user, goodsId, path);
        if(!check){
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }
        //利用缓存判断库存
        boolean over = localOverMap.get(goodsId);
        if(over){
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        //预减库存
        long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock,""+goodsId);
        if(stock<0){
            localOverMap.put(goodsId,true);
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }

        //判断是否已经秒杀到了
        MiaoshaOrder order=orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(),goodsId);
        if(order!=null){
            return Result.error(CodeMsg.REPEATE_MIAOSHA);
        }
        //入队
        MiaoshaMessage mm = new MiaoshaMessage();
        mm.setUser(user);
        mm.setGoodsId(goodsId);
        sender.sendMiaoshaMessage(mm);
        return Result.success(0);//排队中
    }

    /**
     * orderId：成功
     * -1：秒杀失败
     * 0： 排队中
     * */
    @RequestMapping(value="/result", method=RequestMethod.GET)
    @ResponseBody
    public Result<Long> miaoshaResult(MiaoshaUser user,
                                      @RequestParam("goodsId")long goodsId) {
        long result  =miaoshaService.getMiaoshaResult(user.getId(), goodsId);
        return Result.success(result);
    }

    @RequestMapping(value="/path", method=RequestMethod.GET)
    @ResponseBody
    @NeedLogin(paramPos = 2)
    public Result<String> getMiaoshaPath(HttpServletRequest request, MiaoshaUser user,
                                         @RequestParam("goodsId")long goodsId,
                                         @RequestParam(value="verifyCode", defaultValue="0")int verifyCode
    ) {
        boolean check = miaoshaService.checkVerifyCode(user, goodsId, verifyCode);
        if(!check) {
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }
        String path  =miaoshaService.createMiaoshaPath(user, goodsId);
        return Result.success(path);
    }

    @GetMapping("/verifyCode")
    @ResponseBody
    @NeedLogin(paramPos = 2)
    public Result<String> getMiaoshaVerifyCode(HttpServletResponse response,
                                               MiaoshaUser user, @RequestParam("goodsId")long goodsId){
        try {
            BufferedImage image  = miaoshaService.createVerifyCode(user, goodsId);
            OutputStream out = response.getOutputStream();
            ImageIO.write(image, "JPEG", out);
            out.flush();
            out.close();
            return null;
        }catch(Exception e) {
            e.printStackTrace();
            return Result.error(CodeMsg.MIAOSHA_FAIL);
        }
    }


    /**
     * 系统初始化
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        if(goodsList == null){
            return ;
        }
        for(GoodsVo goods : goodsList){
            redisService.set(GoodsKey.getMiaoshaGoodsStock,""+goods.getGoods().getId(),goods.getStockCount());
            localOverMap.put(goods.getGoods().getId(),false);
        }
    }
}
