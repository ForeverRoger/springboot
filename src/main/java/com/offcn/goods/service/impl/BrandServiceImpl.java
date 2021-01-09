package com.offcn.goods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.offcn.goods.service.BrandService;
import com.offcn.mapper.TbBrandMapper;
import com.offcn.pojo.TbBrand;
import com.offcn.pojo.TbBrandExample;
import com.offcn.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

//import org.springframework.stereotype.Service;

@Service // org.springframework.stereotype.Service === 不能被远程调用
//@Service
public class BrandServiceImpl implements BrandService {

    @Autowired
    TbBrandMapper brandMapper;

    //查询所有品牌
    @Override
    public List<TbBrand> findAll() {
        //查询所有品牌  select * from tb_brand ;
        List<TbBrand> brandList = brandMapper.selectByExample(null);
        return brandList;
    }

    //添加品牌
    @Override
    public void add(TbBrand brand) {
        //参数校验
        if(brand==null){
            throw  new RuntimeException("brand不能为空");
        }
        if(StringUtils.isEmpty(brand.getName())){
            throw new RuntimeException("皮牌名称不能为空");
        }
        if(StringUtils.isEmpty(brand.getFirstChar())){
            throw new RuntimeException("品牌首字母不能为空");
        }
        //调用dao层的方法
        int insert = brandMapper.insert(brand);
        //校验返回值
        if(insert<0){
            throw new RuntimeException("添加失败，未知错误");
        }
    }

    //根据id修改品牌
    @Override
    public void update(TbBrand brand) {
        //参数校验
        if(brand==null || StringUtils.isEmpty(brand.getId())){
            throw new RuntimeException("参数不合法");
        }

        //根据主键来进行修改，必须传递id主键的
        int i = brandMapper.updateByPrimaryKey(brand);
        if(i<0){
            throw new RuntimeException("修改失败");
        }
    }


    //批量删除
    @Override
    public void delete(Long[] ids) {
        //参数校验
        if(ids==null || ids.length==0){
            throw new RuntimeException("参数不合法");
        }
        //根据id逐个进行删除
//        for (Long id : ids) {
//            brandMapper.deleteByPrimaryKey(id);//根据主键id删除品牌
//        }
        //条件删除
        // delete from tb_brand where id in (1,2,3);
        TbBrandExample example = new TbBrandExample();//需要拼装品牌相关的条件，需要new一个品牌的Example对象
        TbBrandExample.Criteria criteria = example.createCriteria();
        List<Long> longs = Arrays.asList(ids);
        criteria.andIdIn(longs);//id in (1,2,3);
        int i = brandMapper.deleteByExample(example);// delete from tb_brand where id in (1,2,3);
        if(i<0){
            throw new RuntimeException("删除失败");
        }

    }


    //带条件的分页查询
    // select * from tb_brand where name like ? and firstChar = ? limit m,n
    // n = pageSize
    // m = 从第几条开始，(pageNum-1)*pageSize
    // 1,5 === 0,5
    // 2,5 === 5,5
    // 3,6 === 12,6
    @Override
    public PageResult search(int pageNum, int pageSize, TbBrand searchEntity) {

        //1、设置分页参数 ( 2,5 )
        PageHelper.startPage(pageNum,pageSize);

        //2、构造查询条件
        //有条件的增删改查，api的格式：
        //（1）new一个它的example对象
        TbBrandExample example = new TbBrandExample();
        // （2） 调用example中createCriteria()返回一个criteria
        TbBrandExample.Criteria criteria = example.createCriteria();
        // （3） 你的条件
        // where xxx in （xxx） ====  criteria.andXxxIn()
        // id = 1
//        criteria.andIdEqualTo();//等于
//        criteria.andIdNotEqualTo();//不等于
//        criteria.andIdGreaterThan();//大于

        // name like '%xxx%' criteria.andNameLike("%xxxx%");

        if(searchEntity!=null){
            String name = searchEntity.getName();// name like '%xxx%'
            String firstChar = searchEntity.getFirstChar();// firstChar =  X

            //name不等于null并且不等于空字符串，才拼装name的条件
            if(!StringUtils.isEmpty(name)){
                criteria.andNameLike("%"+name+"%");
            }

            if(!StringUtils.isEmpty(firstChar)){
                criteria.andFirstCharEqualTo(firstChar);
            }

        }

//        brandMapper.selectByPrimaryKey();//根据主键唯一查询
        //根据example查询，将返回值强转成Page对象
        Page<TbBrand> page = (Page<TbBrand>) brandMapper.selectByExample(example);//根据指定的条件查询

        List<TbBrand> result = page.getResult();//根据指定的分页参数和查询条件查询到当前页数据
        long total = page.getTotal();//表中的总行数（可以根据总行数计算总页数）

        //将当前页数据  和  总记录数封装成一个自定义PageResult对象
        PageResult pageResult = new PageResult();
        pageResult.setRows(result);
        pageResult.setNum(total);

        return pageResult;
    }

    @Override
    public TbBrand findOne(Long id) {
        return brandMapper.selectByPrimaryKey(id);
    }
}
