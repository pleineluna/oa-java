package com.wgq.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wgq.entity.AdmScholarshipSysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wgq.entity.dto.AdmScholarshipSysUserDto;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author 作者:kissshot.wang@foxmail.com
 * @since 2021-12-11
 */
public interface AdmScholarshipSysUserMapper extends BaseMapper<AdmScholarshipSysUser> {

    IPage<AdmScholarshipSysUserDto> selectPageVo(Page<AdmScholarshipSysUserDto> page, @Param("idList") List<Long> idList);

    AdmScholarshipSysUserDto selectAdmScholarshipSysUserDtoById(Long id);

    List<AdmScholarshipSysUserDto> searchMyByKeyWord(@Param("name") String name, @Param("userId") Long userId);

    IPage<AdmScholarshipSysUserDto> selectApplyScholarshipList(Page<AdmScholarshipSysUserDto> page, @Param("realname") String realname, @Param("type") String type);


}
