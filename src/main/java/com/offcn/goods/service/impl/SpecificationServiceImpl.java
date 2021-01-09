package com.offcn.goods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.offcn.goods.service.SpecificationService;
import com.offcn.mapper.TbSpecificationMapper;
import com.offcn.mapper.TbSpecificationOptionMapper;
import com.offcn.pojo.TbSpecification;
import com.offcn.pojo.TbSpecificationExample;
import com.offcn.pojo.TbSpecificationOption;
import com.offcn.pojo.TbSpecificationOptionExample;
import com.offcn.vo.PageResult;
import com.offcn.vo.SpecificationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SpecificationServiceImpl implements SpecificationService {

    @Autowired
    TbSpecificationMapper specificationMapper;

    @Autowired
    TbSpecificationOptionMapper optionMapper;


    @Override
    public PageResult search(Integer pageNum, Integer pageSize, TbSpecification specification) {

        //1、设置分页参数
        PageHelper.startPage(pageNum,pageSize);

        //2、查询条件
        TbSpecificationExample example = new TbSpecificationExample();
        TbSpecificationExample.Criteria criteria = example.createCriteria();
        if(specification!=null){
            if(!StringUtils.isEmpty(specification.getSpecName())){
                criteria.andSpecNameLike("%"+specification.getSpecName()+"%");
            }
        }

        //3、调用dao方法返回结果
        Page<TbSpecification> page = (Page<TbSpecification>) specificationMapper.selectByExample(example);

        PageResult pageResult = new PageResult();
        pageResult.setNum(page.getTotal());
        pageResult.setRows(page.getResult());

        return pageResult;
    }

    @Override
    public void add(SpecificationVO specificationVO) {

        //参数校验

        TbSpecification specification = specificationVO.getSpecification();//规格 == {specName:''}
        List<TbSpecificationOption> optionList = specificationVO.getOptionList();//   [ {optionName:'',orders:''},{},{} ]

        //调用dao分别添加规格和规格选项
        //1、添加规格
        int i = specificationMapper.insert(specification);

        //添加规格成功后返回主键id
        Long id = specification.getId();

        if(i>0){
            //规格添加成功后，需要添加规格选项
            for (TbSpecificationOption option : optionList) {
                //{optionName:'',orders:''}
                option.setSpecId(id);
                optionMapper.insert(option);
            }
        }
    }

    @Override
    public void deleteByIds(Long[] ids) {

        //参数校验
        if(ids==null || ids.length==0){
            throw new RuntimeException("参数不能为空");
        }

        //参数：规格id
        //删除规格以及规格选项
        for (Long id : ids) {
            int i = specificationMapper.deleteByPrimaryKey(id);//根据规格主键删除
            if(i>0){
                //删除规格选项 DELETE FROM `tb_specification_option` WHERE spec_id = 101(规格id)
                TbSpecificationOptionExample example = new TbSpecificationOptionExample();
                TbSpecificationOptionExample.Criteria criteria = example.createCriteria();
                //criteria.andIdEqualTo()
                criteria.andSpecIdEqualTo(id);
                optionMapper.deleteByExample(example);
            }
        }
    }

    @Override
    public void updateSave(SpecificationVO specificationVO) {
        TbSpecification specification = specificationVO.getSpecification();
        int i = specificationMapper.updateByPrimaryKey(specification);
        if(i>0){
            //修改规格选项
            // 1、先删除当前规格的规格选项
            TbSpecificationOptionExample example = new TbSpecificationOptionExample();
            TbSpecificationOptionExample.Criteria criteria = example.createCriteria();
            criteria.andSpecIdEqualTo(specification.getId());
//            criteria.andOptionNameLike("%aaa%");//  name like ?
            int m = optionMapper.deleteByExample(example);// delete from tb_spec_option where spec_id = ?

            // 2、重新添加规格选项
            if(m>0){
                List<TbSpecificationOption> optionList = specificationVO.getOptionList();
                for (TbSpecificationOption option : optionList) {
                    option.setSpecId(specification.getId());
                    optionMapper.insert(option);
                }
            }
        }
    }

    @Override
    public SpecificationVO findOne(Long id) {

        //参数校验
        if(id==null){
            throw  new RuntimeException("id不能为空");
        }

        TbSpecification specification = specificationMapper.selectByPrimaryKey(id);

        // select * from tb_spec_option where spec_id = ?(规格的id)
        TbSpecificationOptionExample example = new TbSpecificationOptionExample();
        TbSpecificationOptionExample.Criteria criteria = example.createCriteria();

        criteria.andSpecIdEqualTo(id);// where spec_id = xxxx


        List<TbSpecificationOption> options = optionMapper.selectByExample(example);

        SpecificationVO specificationVO = new SpecificationVO();
        specificationVO.setSpecification(specification);
        specificationVO.setOptionList(options);

        return specificationVO;
    }
}
