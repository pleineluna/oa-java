package com.wgq.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wgq.entity.dto.AdmLeaveFormInfoDto;
import com.wgq.entity.AdmLeaveForm;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wgq.entity.SysUser;

import java.util.List;
import java.util.Map;

/**
 *  服务类
 */
public interface AdmLeaveFormService extends IService<AdmLeaveForm> {

    Map<String, String> baseInfo(SysUser sysUser);

    void create(AdmLeaveForm admLeaveForm);

    IPage<AdmLeaveFormInfoDto> getLeaveFormList(Page<AdmLeaveFormInfoDto> page, List<Long> idList);

    AdmLeaveFormInfoDto getLeaveFormById(Long id,Long userId);

    void audit(String taskId, Long operatorId, String result, String reason);

    AdmLeaveFormInfoDto infoForManagementModel(Long id);
}
