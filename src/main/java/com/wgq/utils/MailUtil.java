package com.wgq.utils;

import com.wgq.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Description:
 */
@Component
public class MailUtil {

    @Resource
    MailService mailService;

    /**
     * 1、普通邮件发送
     * @param mailFrom 发件人要跟yml配置文件里填写的邮箱一致
     * @param mailTo 收件人
     * @param cc 抄送
     * @param mailFromNick 发件人昵称
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    @Async//使本方法变为异步调用
    public void SendSimpleMail(String mailFrom, String mailTo, String cc, String mailFromNick, String subject, String content) {

        String result = "发送邮件成功";
        try {
            mailService.sendSimpleMail(mailFrom, mailFromNick, mailTo, cc, subject, content);
        } catch (Exception e) {
            result = "发送邮件失败！";
            System.out.println(result);
            System.out.println(e);
        }
        System.out.println(result);
    }

    /**
     * 2、带附件的方法
     * @param mailFrom 发件人要跟yml配置文件里填写的邮箱一致
     * @param mailTo 收件人
     * @param cc 抄送
     * @param mailFromNick 发件人昵称
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    @Async//使本方法变为异步调用
    public void SendAttachment(String mailFrom, String mailTo, String cc, String mailFromNick, String subject, String content) {
        File imgFile = new File("src\\main\\java\\com\\ztt\\controller\\f1bdd00e8c.jpg");
        File txtFile = new File("src\\main\\java\\com\\ztt\\controller\\hello.txt");
        List<File> fileList = new ArrayList<>();
        fileList.add(imgFile);
        fileList.add(txtFile);

        // 发件人要跟yml配置文件里填写的邮箱一致

        String result = "发送邮件成功";
        try {
            mailService.sendMailWithAttachments(mailFrom, mailFromNick, mailTo, cc, subject, content, fileList);
        } catch (Exception e) {
            result = "发送邮件失败！";
            System.out.println(result);
            System.out.println(e);
        }
        System.out.println(result);
    }


    /**
     * 3、正文带图片
     * 这个方法实现了正文带图片的功能。不同于前两种的是，String content是html格式的文本，里面用cid标注静态资源（本文是src='cid:image01'），String[] imagePaths存储的是图片的路径，String[] imageId存储了每张图片的编号，这个编号是可以自己随便定义的，但是必须跟content里面使用的cid名称一致。
     * @param mailFrom 发件人要跟yml配置文件里填写的邮箱一致
     * @param mailTo 收件人
     * @param cc 抄送
     * @param mailFromNick 发件人昵称
     * @param subject 邮件主题
     */
    @Async//使本方法变为异步调用
    public void SendMailWithImage(String mailFrom, String mailTo, String cc, String mailFromNick, String subject) {
        // 图片路径
        String image01Path = "E:\\personal\\gittest\\学习项目库\\learning_project_library\\SpringBoot_mail\\src\\main\\java\\com\\ztt\\controller\\2ed0c0d5a2.jpg";
        String image02Path = "E:\\personal\\gittest\\学习项目库\\learning_project_library\\SpringBoot_mail\\src\\main\\java\\com\\ztt\\controller\\3bcd0b6866.jpg";
        String[] imageArr = new String[]{image01Path, image02Path};
        String[] imageIdArr = new String[]{"image01", "image02"};

        String result = "发送邮件成功";
        try {
            String contentHtml = "这是图片1:<div><img src='cid:image01'/></div>" +
                    "这是图片2:<div><img src='cid:image02'/></div>";
            mailService.sendMailWithImage(mailFrom, mailFromNick, mailTo, cc, subject, contentHtml, imageArr, imageIdArr);
        } catch (Exception e) {
            result = "发送邮件失败！";
            System.out.println(result);
            System.out.println(e);
        }
        System.out.println(result);
    }

    /**
     * 4、使用ThymeLeaf
     *
     * @param mailFrom 发件人要跟yml配置文件里填写的邮箱一致
     * @param mailTo 收件人
     * @param cc 抄送
     * @param mailFromNick 发件人昵称
     * @param subject 邮件主题
     * @param subjectContext 邮件副主题
     * @param noticeContent 邮件通知内容
     */
    // 注入TemplateEngine
    @Autowired
    TemplateEngine templateEngine;
    @Async//使本方法变为异步调用
    public void SendHtmlMailThymeLeaf(String mailFrom, String mailTo, String cc, String mailFromNick, String subject,String subjectContext,String noticeContent) {

        // 注意导入的包是org.thymeleaf.context
        Context context = new Context();
        context.setVariable("subjectContext",subjectContext );
        context.setVariable("noticeContent",noticeContent );
        String content = templateEngine.process("mailTemplate01.html", context);

        mailService.sendHtmlMailThymeLeaf(mailFrom, mailFromNick, mailTo, cc, subject, content);

        System.out.println("邮件发送成功");
    }

}
