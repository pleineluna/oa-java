package com.wgq.service;

import com.wgq.entity.AdmNotice;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 服务类
 */
public interface AdmNoticeService extends IService<AdmNotice> {


    List<AdmNotice> getLeaveNoticeList(Long receiverId);

    List<AdmNotice> getMeetingNoticeList(Long receiverId);
}
