package com.offcn.goods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.offcn.enums.GoodsStatusEnum;
import com.offcn.goods.service.GoodsService;
import com.offcn.mapper.*;
import com.offcn.pojo.*;
import com.offcn.vo.GoodsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Transactional//类 ：所有方法都走事务
@Service
public class GoodsServiceImpl implements GoodsService {

    @Autowired
    TbGoodsMapper goodsMapper;

    @Autowired
    TbGoodsDescMapper goodsDescMapper;

    @Autowired
    TbItemMapper itemMapper;

    @Autowired
    TbBrandMapper brandMapper;

    @Autowired
    TbSellerMapper sellerMapper;

    @Autowired
    TbItemCatMapper itemCatMapper;

//    @Transactional
    @Override
    public void add(GoodsVO goodsVO) {
        //参数校验
        if(goodsVO==null){
            throw new RuntimeException("参数不合法");
        }

        // 为参数赋值
        //商品状态
        goodsVO.getGoods().setAuditStatus(GoodsStatusEnum.GOODS_INIT_STATUS.getStatus());// 0 初始状态  1 审核通过   2 驳回  （还有审核通过状态的商品才能够现实在商城前端）
        //is_marketable 上下架状态(0  1 )
        goodsVO.getGoods().setIsMarketable("0");
        goodsVO.getGoods().setIsDelete("0");// 1  被删除   0 未删除  （逻辑删除）

        //TODO（已完成，在添加item时已处理）
        //goodsVO.getGoods().setDefaultItemId(null); 能设置上默认sku么？？  当sku保存到数据库后，才可以回头再来设置默认sku的主键


        // 1 、 添加spu
        goodsMapper.insert(goodsVO.getGoods());//spu
        //spu添加成功后，需要返回一个主键，给后边数据使用
//        <selectKey resultType="java.lang.Long" order="AFTER" keyProperty="id">
//                SELECT LAST_INSERT_ID() AS id
//        </selectKey>
        Long id = goodsVO.getGoods().getId();//spu商品的主键


        //////////////////----------------------------------------------///////////////////////
        // 添加spu成功后，返回spu的主键；spu扩展表的主键和spu的主键是同一个（主键一对一）
        goodsVO.getGoodsDesc().setGoodsId(id);//设置spu扩展表的主键

        // 2、 添加 spu扩展表数据
        goodsDescMapper.insert(goodsVO.getGoodsDesc());

        //////////////////------------------------------------////////////////////////////


        // 3、 添加 SKU列表
        //添加规格选项（未完待续）
        //TODO
        List<TbItem> itemList = goodsVO.getItemList();
        for (TbItem item : itemList) {

            item.setTitle(createTitle(goodsVO,item));// sku商品的标题title===spu名称+所有规格选项名称  例如： 华为novo6se手机 黑色 联通4G 8G 128G
            item.setImage(getFirstItemImages(goodsVO.getGoodsDesc().getItemImages()));//当前sku的图片url （图片列表中的第一个图片的url读取出来赋值给它）
            item.setCategoryid(goodsVO.getGoods().getCategory3Id());//为当前sku设置商品分类（第三级商品分类的id）

            item.setCreateTime(new Date());
            item.setUpdateTime(new Date());

            item.setGoodsId(id);//设置当前sku所属的spuid
            item.setSellerId(goodsVO.getGoods().getSellerId());

            item.setCategory(itemCatMapper.selectByPrimaryKey(goodsVO.getGoods().getCategory3Id()).getName());// 第三级分类名称
            item.setBrand(brandMapper.selectByPrimaryKey(goodsVO.getGoods().getBrandId()).getName());//品牌名称
            item.setSeller(sellerMapper.selectByPrimaryKey(goodsVO.getGoods().getSellerId()).getName());//商家名称

//            item.setIsDefault();页面上的多个sku中，只能有一个sku的该字段为1

            int insert = itemMapper.insert(item);

            if(insert>0 && item.getIsDefault().equals("1")){
                //获取当前sku的主键,赋值给goods.default_item_id
                Long skuId = item.getId();
                TbGoods goods = goodsMapper.selectByPrimaryKey(id);
                goods.setDefaultItemId(skuId);
                goodsMapper.updateByPrimaryKey(goods);
            }
        }

    }

    @Override
    public List<TbGoods> getGoods(TbGoods searchEntity) {
        TbGoodsExample example = new TbGoodsExample();
        TbGoodsExample.Criteria criteria = example.createCriteria();

        //未删除的商品
        criteria.andIsDeleteEqualTo("0");

        if(searchEntity!=null){
            if(!StringUtils.isEmpty(searchEntity.getAuditStatus())){
                criteria.andAuditStatusEqualTo(searchEntity.getAuditStatus());
            }
            if(!StringUtils.isEmpty(searchEntity.getSellerId())){
                criteria.andSellerIdEqualTo(searchEntity.getSellerId());
            }
        }
        List<TbGoods> tbGoods = goodsMapper.selectByExample(example);
        return tbGoods;
    }

    @Override
    public void updateAuditStatus(Long[] ids, String auditStatus) {
        for (Long id : ids) {
            TbGoods goods = goodsMapper.selectByPrimaryKey(id);
            goods.setAuditStatus(auditStatus);
            goodsMapper.updateByPrimaryKey(goods);
        }
    }

    @Override
    public void delete(Long[] ids) {
        for (Long id : ids) {
            TbGoods goods = goodsMapper.selectByPrimaryKey(id);
            goods.setIsDelete("1");//逻辑删除
            goodsMapper.updateByPrimaryKey(goods);
        }
    }

    @Override
    public void updateMarketable(Long id, String isMarketable) {
        //校验
        if(StringUtils.isEmpty(id) || StringUtils.isEmpty(isMarketable)){
            throw new RuntimeException("参数不能为空");
        }
        //商品的上下架====根据商品的id修改isMarketable字段。
        TbGoods goods = goodsMapper.selectByPrimaryKey(id);

        if(goods==null){
            throw new RuntimeException("未加载到指定的商品对象");
        }
        if(!goods.getAuditStatus().equals("1")){
            throw new RuntimeException("未审核通过的商品不能进行上下架，请耐心等待...");
        }
        goods.setIsMarketable(isMarketable);
        int i = goodsMapper.updateByPrimaryKey(goods);
        if(i<0){
            throw new RuntimeException(isMarketable.equals("0")?"下架失败":"上架失败");
        }
    }

    @Override
    public List<TbItem> getSkuListByGoodsId(Long goodsId) {

        // SELECT * FROM`tb_item` a  WHERE a.`goods_id` = ?
        TbItemExample example = new TbItemExample();
        TbItemExample.Criteria criteria = example.createCriteria();
        criteria.andGoodsIdEqualTo(goodsId);

        List<TbItem> list = itemMapper.selectByExample(example);

        return list;
    }


    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    TbTypeTemplateMapper typeTemplateMapper;

    @Autowired
    TbSpecificationOptionMapper optionMapper;

    @Override
    public void testCreateHash() {

        List<TbItemCat> itemCatList = itemCatMapper.selectByExample(null);
        for (TbItemCat itemCat : itemCatList) {
            String name = itemCat.getName();//分类名称
            Long typeId = itemCat.getTypeId();//模板id
            redisTemplate.boundHashOps("categoryHash").put(name,typeId);
        }

        /*-------------*/
        List<TbTypeTemplate> templateList = typeTemplateMapper.selectByExample(null);

        for (TbTypeTemplate template : templateList) {
            Long id = template.getId();//模板id

            String brandIds = template.getBrandIds();//[{"id":1,"text":"联想"},{"id":3,"text":"三星"},{"id":2,"text":"华为"},{"id":5,"text":"OPPO"},{"id":4,"text":"小米"},{"id":9,"text":"苹果"},{"id":8,"text":"魅族"},{"id":6,"text":"360"},{"id":10,"text":"VIVO"},{"id":11,"text":"诺基亚"},{"id":12,"text":"锤子"}]
            List<Map> brandList = JSON.parseArray(brandIds, Map.class);
            redisTemplate.boundHashOps("brandHash").put(id,brandList);

            String specIds = template.getSpecIds();
            List<Map> specList = JSON.parseArray(specIds, Map.class);
            for (Map map : specList) {
//                Long specId = (Long)map.get("id");
                Long specId  = Long.parseLong(map.get("id")+"");
                TbSpecificationOptionExample example = new TbSpecificationOptionExample();
                TbSpecificationOptionExample.Criteria criteria = example.createCriteria();
                criteria.andSpecIdEqualTo(specId);
                List<TbSpecificationOption> options = optionMapper.selectByExample(example);
                map.put("options",options);
            }
            redisTemplate.boundHashOps("specHash").put(id,specList);// id，text 缺少optionlist

        }

    }


    private String getFirstItemImages(String itemImages) {
        if(StringUtils.isEmpty(itemImages)){
            return "";
        }
        List<Map> list = JSON.parseArray(itemImages, Map.class);
        if(list.size()>0){
            Map map = list.get(0);
            Object url = map.get("url");
            return url+"";
        }
        return "";
    }

    private String createTitle(GoodsVO goodsVO,TbItem item) {
        String title = "";
        title = goodsVO.getGoods().getGoodsName();
        String spec = item.getSpec();// {"网络":"移动3G","机身内存":"16G" }
        Map<String,Object> map = JSON.parseObject(spec, Map.class);
        Set<String> set = map.keySet();
        for(String key : set){
            Object optionName = map.get(key);
            title += " " + optionName;
        }
        return title;
    }
}
