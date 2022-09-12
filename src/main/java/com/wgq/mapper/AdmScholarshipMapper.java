package com.wgq.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wgq.entity.AdmScholarship;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wgq.entity.dto.AdmScholarshipDto;

import java.util.List;

public interface AdmScholarshipMapper extends BaseMapper<AdmScholarship> {

    IPage<AdmScholarship> searchByKeyWord(Page<?> page,String name);

    List<AdmScholarship> searchByKeyWordStatuIsOpen(String name);

    AdmScholarship searchById(Long id);

    AdmScholarshipDto searchAdmScholarshipDtoById(Long id);



}

