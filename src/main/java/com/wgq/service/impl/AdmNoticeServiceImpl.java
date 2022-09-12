package com.wgq.service.impl;

import com.wgq.entity.AdmNotice;
import com.wgq.mapper.AdmNoticeMapper;
import com.wgq.service.AdmNoticeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 服务实现类
 */
@Service
public class AdmNoticeServiceImpl extends ServiceImpl<AdmNoticeMapper, AdmNotice> implements AdmNoticeService {
    @Resource
    AdmNoticeMapper admNoticeMapper;

    /**
     * 查询指定员工的Leave系统消息
     * @param receiverId 接收人的id
     * @return List<AdmNotice>
     */
    @Override
    public List<AdmNotice> getLeaveNoticeList(Long receiverId) {
        return admNoticeMapper.getLeaveNoticeListByReceiverId(receiverId);
    }

    /**
     * 查询指定员工的Meeting系统消息
     * @param receiverId 接收人的id
     * @return List<AdmNotice>
     */
    @Override
    public List<AdmNotice> getMeetingNoticeList(Long receiverId) {
        return admNoticeMapper.getMeetingNoticeListByReceiverId(receiverId);
    }

}
