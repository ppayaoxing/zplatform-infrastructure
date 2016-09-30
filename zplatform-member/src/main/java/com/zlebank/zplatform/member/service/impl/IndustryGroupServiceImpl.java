/* 
 * IndustryGroupServiceImpl.java  
 * 
 * version TODO
 *
 * 2016年9月28日 
 * 
 * Copyright (c) 2016,zlebank.All rights reserved.
 * 
 */
package com.zlebank.zplatform.member.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.zlebank.zplatform.acc.bean.enums.CommonStatus;
import com.zlebank.zplatform.acc.bean.enums.Usage;
import com.zlebank.zplatform.acc.exception.AbstractBusiAcctException;
import com.zlebank.zplatform.commons.service.impl.AbstractBasePageService;
import com.zlebank.zplatform.commons.utils.BeanCopyUtil;
import com.zlebank.zplatform.member.bean.InduGroupMemberCreateBean;
import com.zlebank.zplatform.member.bean.IndustryGroupBean;
import com.zlebank.zplatform.member.bean.IndustryGroupCreatBean;
import com.zlebank.zplatform.member.bean.IndustryGroupQuery;
import com.zlebank.zplatform.member.bean.enums.BusinessActorType;
import com.zlebank.zplatform.member.dao.IndustryGroupDAO;
import com.zlebank.zplatform.member.pojo.PojoIndustryGroup;
import com.zlebank.zplatform.member.service.IndustryGroupMemberService;
import com.zlebank.zplatform.member.service.IndustryGroupService;

/**
 * Class Description
 *
 * @author houyong
 * @version
 * @date 2016年9月28日 上午9:42:47
 * @since 
 */
@Service
public class IndustryGroupServiceImpl extends AbstractBasePageService<IndustryGroupQuery,IndustryGroupBean> implements IndustryGroupService{
    @Autowired
    private IndustryGroupDAO industryGroupDao;
    @Autowired
    private IndustryGroupMemberService induGroupMemServiceImp;
    /**
     *
     * @param example
     * @return
     */
    @Override
    @Transactional(readOnly=true)
    protected long getTotal(IndustryGroupQuery example) {
        return industryGroupDao.count(example);
    }

    /**
     *
     * @param offset
     * @param pageSize
     * @param example
     * @return
     */
    @Override
    @Transactional(readOnly=true)
    protected List<IndustryGroupBean> getItem(int offset,
            int pageSize,
            IndustryGroupQuery queryBean) {
        List<PojoIndustryGroup> industryGroups=industryGroupDao.getItem(offset,pageSize,queryBean);
        List<IndustryGroupBean> result= new ArrayList<IndustryGroupBean>();
        for (PojoIndustryGroup pojoIndustryGroup : industryGroups) {
            IndustryGroupBean beanTemp=new IndustryGroupBean();
            beanTemp=BeanCopyUtil.copyBean(IndustryGroupBean.class, pojoIndustryGroup);
            result.add(beanTemp);
        }
        industryGroups=null;
        return result;
    }

    /**
     *
     * @param groupBean
     * @return 
     * @throws AbstractBusiAcctException 
     */
    @Override
    @Transactional(propagation=Propagation.REQUIRED)
    public String addGroup(IndustryGroupCreatBean groupBean) throws AbstractBusiAcctException {
        PojoIndustryGroup pojoInduGroup=new PojoIndustryGroup();
        pojoInduGroup=BeanCopyUtil.copyBean(PojoIndustryGroup.class, groupBean);
        pojoInduGroup.setInTime(new Date());
        pojoInduGroup.setStatus(CommonStatus.NORMAL);
        pojoInduGroup.setGroupCode(generateGroupCode(groupBean));
        pojoInduGroup=industryGroupDao.merge(pojoInduGroup);
        InduGroupMemberCreateBean induGroupMemberCreateBean=new InduGroupMemberCreateBean();
        induGroupMemberCreateBean.setGroupCode(pojoInduGroup.getGroupCode());
        induGroupMemberCreateBean.setGroupId(pojoInduGroup.getId());
        induGroupMemberCreateBean.setMemberId(pojoInduGroup.getMemberId());
        induGroupMemberCreateBean.setUsage(Usage.WAITSETTLE);
        induGroupMemServiceImp.addMemberToGroup(induGroupMemberCreateBean,false,BusinessActorType.ENTERPRISE.getCode());
        return pojoInduGroup.getGroupCode();
    }
    
    private String generateGroupCode(IndustryGroupCreatBean groupBean){
        String codePrefix=groupBean.getMemberId().substring(5);
        return codePrefix;
    }
    /**
     *
     * @param groupBean
     */
    @Override
    @Transactional(propagation=Propagation.REQUIRED)
    public void updateGroup(IndustryGroupBean groupBean) {
        PojoIndustryGroup pojoInduGroup=industryGroupDao.getByCode(groupBean);
        pojoInduGroup.setDrawable(groupBean.getDrawable());
        pojoInduGroup.setNote(groupBean.getNote());
        pojoInduGroup.setGroupName(groupBean.getGroupName());
        pojoInduGroup.setUpTime(new Date());
        pojoInduGroup.setRemarks(groupBean.getRemarks());
        industryGroupDao.update(pojoInduGroup);
    }

    /**
     *
     * @param memberId
     * @param instiCode
     * @return
     */
    @Override
    @Transactional(readOnly=true)
    public IndustryGroupBean queryGroupExist(String memberId, String instiCode) {
        IndustryGroupQuery queryBean=new IndustryGroupQuery();
        queryBean.setMemberId(memberId);
        queryBean.setInstiCode(instiCode);
        queryBean.setStatus(CommonStatus.NORMAL);
        PojoIndustryGroup pojoIndustryGroup=  industryGroupDao.queryGroup(queryBean);
        if (pojoIndustryGroup==null) {
            return null;
        }
        IndustryGroupBean beanTemp=new IndustryGroupBean();
        beanTemp=BeanCopyUtil.copyBean(IndustryGroupBean.class, pojoIndustryGroup);
        return beanTemp;
    }

    /**
     *
     * @param groupId
     * @param groupCode
     * @return
     */
    @Override
    @Transactional(readOnly=true)
    public IndustryGroupBean queryGroupByCodeOrId(long groupId,
            String groupCode) {
        IndustryGroupQuery queryBean=new IndustryGroupQuery();
        queryBean.setGroupCode(groupCode);
        queryBean.setId(groupId);
        queryBean.setStatus(CommonStatus.NORMAL);
        PojoIndustryGroup pojoIndustryGroup=  industryGroupDao.queryGroup(queryBean);
        if (pojoIndustryGroup==null) {
            return null;
        }
        IndustryGroupBean beanTemp=new IndustryGroupBean();
        beanTemp=BeanCopyUtil.copyBean(IndustryGroupBean.class, pojoIndustryGroup);
        return beanTemp;
    }

    
}
