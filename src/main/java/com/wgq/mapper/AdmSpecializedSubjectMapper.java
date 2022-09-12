package com.wgq.mapper;

import com.wgq.entity.AdmSpecializedSubject;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface AdmSpecializedSubjectMapper extends BaseMapper<AdmSpecializedSubject> {

    AdmSpecializedSubject getAdmSpecializedSubjectByUserId( Long id);
}
