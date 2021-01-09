package com.offcn.goods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.offcn.goods.service.ItemCatService;
import com.offcn.mapper.TbItemCatMapper;
import com.offcn.pojo.TbItemCat;
import com.offcn.pojo.TbItemCatExample;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service // com.alibaba.dubbo.config.annotation.Service == 可以被远程调用
public class ItemCatServiceImpl implements ItemCatService {

    @Autowired
    TbItemCatMapper itemCatMapper;

    @Override
    public List<TbItemCat> findAll() {
        return itemCatMapper.selectByExample(null);
    }

    @Override
    public List<TbItemCat> findByParentId(Long parentId) {
        TbItemCatExample example = new TbItemCatExample();
        TbItemCatExample.Criteria criteria = example.createCriteria();
        criteria.andParentIdEqualTo(parentId);
        List<TbItemCat> tbItemCats = itemCatMapper.selectByExample(example);
        return tbItemCats;
    }
}
