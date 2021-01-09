package com.offcn.goods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.offcn.goods.service.TypeTemplateService;
import com.offcn.mapper.CustomMapper;
import com.offcn.mapper.TbItemCatMapper;
import com.offcn.mapper.TbSpecificationOptionMapper;
import com.offcn.mapper.TbTypeTemplateMapper;
import com.offcn.pojo.TbItemCat;
import com.offcn.pojo.TbSpecificationOption;
import com.offcn.pojo.TbSpecificationOptionExample;
import com.offcn.pojo.TbTypeTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TypeTemplateServiceImpl implements TypeTemplateService {

    @Autowired
    TbTypeTemplateMapper templateMapper;

    @Autowired
    CustomMapper customMapper;


    @Autowired
    TbItemCatMapper itemCatMapper;

    @Autowired
    TbSpecificationOptionMapper optionMapper;

    @Override
    public void add(TbTypeTemplate typeTemplate) {
        //参数校验

        //调用dao
        templateMapper.insert(typeTemplate);
    }

    @Override
    public Map getSelect2Data() {

        List<Map> brandListData = customMapper.brandListData();
        List<Map> specListData = customMapper.specListData();

        Map map = new HashMap();
        map.put("brandListData",brandListData);
        map.put("specListData",specListData);

        return map;
    }


    @Override
    public TbTypeTemplate getTemplateByC3Id(Long c3Id) {//根据商品分类id，查询到模板对象
        //参数：分类id
        //返回值：模板对象
        TbItemCat itemCat = itemCatMapper.selectByPrimaryKey(c3Id);
        Long typeId = itemCat.getTypeId();//模板id
        TbTypeTemplate template = templateMapper.selectByPrimaryKey(typeId);

        //模板关联的规格 [{"id":27,"text":"网络"},{"id":32,"text":"机身内存"}]
        String specIds = template.getSpecIds();
        //这个字符串中只有规格id和规格名称，没有规格选项，而我们页面需要显示当前规格的规格选项，所以我们要对这个字符串进行加工，为他添加一个规格选项
//        [{"id":27,"text":"网络" , "optionList":[{},{},{}]},{"id":32,"text":"机身内存"}]

        //加工---[ {id,text,optionList:[] },{id,text} ]
        String newSpecIds = this.processSpecIds(specIds);
        template.setSpecIds(newSpecIds);

        return template;
    }

    /**
     *
     * @param specIds  [{id:1,text:''}]
     * @return [{id:1,text:'' ,optionList:[{},{}] }]
     */
    //[ {id:1,text:''},{id,text} ]
    //[ {id,text ,optionList:[]},{id,text,optionList} ]
    private String processSpecIds(String specIds){
        //1.转成List<Map>
        List<Map> list = JSON.parseArray(specIds, Map.class);
        for (Map map : list) {
            long specId = Long.parseLong(map.get("id") + "");//规格id
            List<TbSpecificationOption> optionList = getOptionListBySpecId(specId);
            map.put("optionList",optionList);
        }
        String string = JSON.toJSONString(list);
        return string;
    }

    /**
     * 根据规格id查询规格选项集合
     * @param specId
     * @return
     */
    private List<TbSpecificationOption> getOptionListBySpecId(Long specId) {

//        SELECT * FROM `tb_specification_option` WHERE spec_id = ?
        // 规格  规格选项   1  N
        TbSpecificationOptionExample example = new TbSpecificationOptionExample();
        TbSpecificationOptionExample.Criteria criteria = example.createCriteria();

        criteria.andSpecIdEqualTo(specId);

        List<TbSpecificationOption> options = optionMapper.selectByExample(example);
        return options;
    }

}
