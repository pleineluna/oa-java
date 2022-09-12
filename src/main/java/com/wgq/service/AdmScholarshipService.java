package com.wgq.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wgq.entity.AdmNotice;
import com.wgq.entity.AdmScholarship;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 作者:kissshot.wang@foxmail.com
 * @since 2021-12-10
 */
public interface AdmScholarshipService extends IService<AdmScholarship> {

    IPage<AdmScholarship> scholarshipList(Page<AdmScholarship> page, String name);

    List<AdmScholarship> scholarshipListStatuIsOpen(String name);

    AdmScholarship scholarshipInfo(Long id);

    void createScholarshipCategory(AdmScholarship admScholarship);

    void apply(String desc, Long scholarshipId, Long userId);

    List<AdmNotice> getScholarshipNoticeList(Long receiverId);
}
