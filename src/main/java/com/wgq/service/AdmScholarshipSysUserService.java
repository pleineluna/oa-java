package com.wgq.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wgq.entity.AdmScholarshipSysUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wgq.entity.dto.AdmScholarshipSysUserDto;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 作者:kissshot.wang@foxmail.com
 * @since 2021-12-11
 */
public interface AdmScholarshipSysUserService extends IService<AdmScholarshipSysUser> {

    IPage<AdmScholarshipSysUserDto> scholarshipListNeedToExamine(Page page, List<Long> idList);

    AdmScholarshipSysUserDto scholarshipNeedToExamineById(Long id,Long loginUserId);

    void audit(String taskId, Long operatorId, String result, String review);

    List<AdmScholarshipSysUserDto> Myscholarship(String name, Long userId);

    AdmScholarshipSysUserDto myScholarshipinfo(Long id);

    IPage<AdmScholarshipSysUserDto> getApplyScholarshipList(Page page, String realname, String type);
}
