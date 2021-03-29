package com.hust.tutu.dao;

import com.hust.tutu.Vo.GoodsVo;
import com.hust.tutu.domain.MiaoshaGoods;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface GoodsDao {

    @Select("select g.*,mg.stock_count, mg.start_date, mg.end_date,mg.miaosha_price from miaosha_goods mg left join goods g on mg.goods_id = g.id where g.id = #{goodsId}")
    @Results(id="goodsVo",value={
            @Result(id=true,column = "id",property = "goods.id"),
            @Result(column = "goods_name",property = "goods.goodsName"),
            @Result(column = "goods_title",property = "goods.goodsTitle"),
            @Result(column = "goods_img",property = "goods.goodsImg"),
            @Result(column = "goods_detail",property = "goods.goodsDetail"),
            @Result(column = "goods_price",property = "goods.goodsPrice"),
            @Result(column="goods_stock",property = "goods.goodsStock")
    })
    public GoodsVo getGoodsVoByGoodsId(@Param("goodsId")long goodsId);

    @Select("select g.*,mg.stock_count, mg.start_date, mg.end_date,mg.miaosha_price from miaosha_goods mg left join goods g on mg.goods_id = g.id")
    @ResultMap("goodsVo")
    public List<GoodsVo> listGoodsVo();

    @Update("update miaosha_goods set stock_count=stock_count-1 where goods_id = #{goodsId} and stock_count>0")
    public int reduceStock(MiaoshaGoods miaoshaGoods);

    @Update("update miaosha_goods set stock_count = #{stockCount} where goods_id = #{goodsId}")
    public int resetStock(MiaoshaGoods g);
}
