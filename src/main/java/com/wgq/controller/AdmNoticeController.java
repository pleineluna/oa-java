package com.wgq.controller;


import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wgq.common.lang.Result;
import com.wgq.entity.*;
import com.wgq.entity.dto.AdmNoticeDto;
import com.wgq.mapper.AdmClassMapper;
import com.wgq.mapper.AdmNoticeMapper;
import com.wgq.utils.FastDFSUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 前端控制器
 */
@RestController
@RequestMapping("/adm/notice")
public class AdmNoticeController extends BaseController {

    @Resource
    AdmNoticeMapper admNoticeMapper;

    @Resource
    AdmClassMapper admClassMapper;


    @Value("${fastdfs.nginx.host}")
    String nginxHost;

    @PostMapping("/notify")
    @PreAuthorize("hasAuthority('adm:notify:list:notify')")
    public Result notify(@Validated @RequestBody AdmNotice admNotice, Principal principal) {
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        String uuid = RandomUtil.randomString(10);
        AdmNotice admNoticeNew = new AdmNotice();
        admNoticeNew.setSubjectContext(admNotice.getSubjectContext());
        admNoticeNew.setContent(admNotice.getContent());
        admNoticeNew.setCreated(LocalDateTime.now());
        admNoticeNew.setStatu(1);
        //根据当前发布通知登录的用户得到身份角色【ROLE】，设置不同的通知类型
        List<SysRole> roleList = sysRoleService.list(new QueryWrapper<SysRole>()
                .inSql("id", "select role_id from sys_user_role where user_id =" + sysUser.getId()));
        if (roleList.get(0).getCode().equals("superAdmin")) { //目前还是角色用户一对一关系。
            admNoticeNew.setType(3); //type=3 书记发布的通知【面向全院】
        } else {
            admNoticeNew.setType(2);//type=2 导员发布的学院方面通知【面向班级】
        }
        admNoticeNew.setUuid(uuid);
        admNoticeNew.setPublisherId(sysUser.getId());
        admNoticeService.save(admNoticeNew);
        AdmNotice admNoticeUuid = admNoticeMapper.selectNoticeByUuid(uuid);
        //想办法把新创建的admNotice的id拿到并返回给前端(也可以返回uuid，这里我就返回id了直接)
        return Result.succ(admNoticeUuid.getId());
    }

    @PostMapping("/notify/fileUpload")
    // @RequestParam(value = "noticeId", defaultValue = "0") Long noticeId ；因为前端不一定必须上传文件，所以需要设置noticeId的默认值，否则当前端不上传文件时会报错。
    public Result fileUpload(@RequestParam("file") MultipartFile[] files, @RequestParam(value = "noticeId", defaultValue = "0") Long noticeId) {

        //@RequestParam("file") MultipartFile[] files,因为用了files，默认与前端upload的属性名“file”不一样，所以要标识一下
        if (files != null && files.length > 0) {
            String urls = "";
            for (MultipartFile file : files) {
                String filePath = FastDFSUtil.upload(file);//上传到服务器，在服务器中的地址
                System.out.println(filePath);
                String originalFilename = file.getOriginalFilename();
                //拼接字符串的开头一定是字符串，不要是String类型的变量！！！！  :::和---用来作为分割字符串标识
                urls += ("" + originalFilename + ":::" + nginxHost + filePath + "---");
            }
            AdmNotice admNotice = admNoticeMapper.selectById(noticeId);
            admNotice.setFileAddress(urls);
            //上面设置成功了之后数据库一直没有反应。。。。因为我就没有做出数据库的操作。。。。
            admNoticeService.updateById(admNotice);
            System.out.println(urls);
            System.out.println("上传成功");
            return Result.succ("上传成功");
        } else {
            System.out.println("文件未上传或上传失败");
            return Result.fail("文件未上传或上传失败");
        }
    }

    /**
     * 逻辑很简单，根据前端传来的索引type返回不同类型的通知，不传索引则返回所有通知。
     *
     * @param principal
     * @param type
     * @return
     */
    @GetMapping("/list")
    public Result list(Principal principal, String type) {
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        List<AdmNotice> noticeList = new ArrayList<>(); //所有通知
        List<AdmNotice> leaveList = admNoticeService.getLeaveNoticeList(sysUser.getId());//得到请假模块的通知，即adm_notece表中type=1的记录
        List<AdmNotice> collegeNoticeList;//得到通知模块的通知，即type=2、3的记录
        List<AdmNotice> meetingNoticeList = admNoticeService.getMeetingNoticeList(sysUser.getId());//得到会议模块的通知，即adm_notece表中type=4的记录
        List<AdmNotice> scholarshipList = admScholarshipService.getScholarshipNoticeList(sysUser.getId());
        //我数据库表设计的是只有学生才有所属班级，会有班级-用户关联中间表。而导员是作为班级表的字段guide_id来表示的
        AdmClass classByUserId = admClassMapper.getClassByUserId(sysUser.getId());
        if (classByUserId != null) {// 说明是学生，说明有导员
            collegeNoticeList = admNoticeMapper.getCollegeNotice(classByUserId.getGuideId());
        } else {//说明是导员、书记
            collegeNoticeList = admNoticeService.list(new QueryWrapper<AdmNotice>().eq("type", "2").or().eq("type", "3"));
        }
        for (AdmNotice admNotice : collegeNoticeList) {
            //新建的List files是属于一个admNotice的
            List<Map<Object, Object>> files = new ArrayList<>();
            //fileInfos是admNotice的fileAddress()属性，包含了用特殊符号按顺序拼接的所有文件的name和url

            //这个if判断的作用是：因为前端的学院通知不一定上传附件，所以如果不上传附件会出现空指针错误，所以先用null==？进行判断，如果确实没有附件那么就跳出循环并且进行下一次循环。
            if (null == admNotice.getFileAddress() || admNotice.getFileAddress().equals("")) {
                continue;
            }
            String[] fileInfos = admNotice.getFileAddress().split("---");
            for (String fileInfo : fileInfos) {
                //file是属于fileInfo字符串拆解出的一个file，属于Map类型，来记录单个的name和url（其实就是拆解fileInfo字符串得出name和url），Map类型，key为name，value为url。
                //这样循环可以遍历出多个file对象，然后每次都添加到对应的files里面，再把files赋给admNotice里面
                Map file = new HashMap();
                String[] simpleFileInfo = fileInfo.split(":::");
                file.put("name", simpleFileInfo[0]);
                file.put("url", simpleFileInfo[1]);
                files.add(file);
            }
            System.out.println(files.toString());
            admNotice.setFiles(files);
        }

        if (type.equals("1")) {
            return Result.succ(leaveList.stream().sorted(Comparator.comparing(AdmNotice::getCreated).reversed()).collect(Collectors.toList()));
        } else if (type.equals("2and3")) {
            return Result.succ(collegeNoticeList.stream().sorted(Comparator.comparing(AdmNotice::getCreated).reversed()).collect(Collectors.toList()));
        } else if (type.equals("4")) {
            return Result.succ(meetingNoticeList.stream().sorted(Comparator.comparing(AdmNotice::getCreated).reversed()).collect(Collectors.toList()));
        } else if (type.equals("5")) {
            return Result.succ(scholarshipList.stream().sorted(Comparator.comparing(AdmNotice::getCreated).reversed()).collect(Collectors.toList()));
        } else {
            noticeList.clear();
            noticeList.addAll(leaveList);
            noticeList.addAll(collegeNoticeList);
            noticeList.addAll(meetingNoticeList);
            //一定注意，这里的流处理不对源数据做修改，所以一定要存放到新的数据容器中！！！比如List<AdmNotice> newList = noticeList.stream().sorted(Comparator.comparing(AdmNotice::getCreated).reversed()).collect(Collectors.toList())
            //return的话就不用了，因为return就是返回处理的结果
            return Result.succ(noticeList.stream().sorted(Comparator.comparing(AdmNotice::getCreated).reversed()).collect(Collectors.toList()));
        }

    }

    @GetMapping("/listByRealname")
    public Result listByRealname(String realname, String type) {
        Page<AdmNoticeDto> admNoticeDtoPage = admNoticeMapper
                .selectNoticeByRealname(getPage(), realname, type);
        return Result.succ(admNoticeDtoPage);
    }

    @GetMapping("/listById")
    public Result listById(Long id) {
        AdmNoticeDto admNoticeDtoById = admNoticeMapper.selectAdmNoticeDtoById(id);
        //只有学院类型通知才有可能具有附件
        if ( null!=admNoticeDtoById.getFileAddress() && !admNoticeDtoById.getFileAddress().equals("")) {
            String[] fileInfos = admNoticeDtoById.getFileAddress().split("---");
            List<Map<Object, Object>> files = new ArrayList<>();
            for (String fileInfo : fileInfos) {
                Map file = new HashMap();
                String[] simpleFileInfo = fileInfo.split(":::");
                file.put("name", simpleFileInfo[0]);
                file.put("url", simpleFileInfo[1]);
                files.add(file);
                admNoticeDtoById.setFiles(files);
            }
        }
        return Result.succ(admNoticeDtoById);
    }

    @PostMapping("/delete")
    @Transactional//这种写操作一定要开启事务
    @PreAuthorize("hasAuthority('adm:Notice:management:delete')")
    public Result delete(@RequestBody Long[] ids) {
        //Arrays.asList() 数组转列表 因为mybatis-plus的removeByIds方法参数是list类型
        admNoticeService.removeByIds(Arrays.asList(ids));
        return Result.succ("删除成功");
    }
}
