package com.wgq.mapper;

import com.wgq.entity.AdmClass;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 *
 */
public interface AdmClassMapper extends BaseMapper<AdmClass> {
    AdmClass getClassByUserId(Long userId);
}
