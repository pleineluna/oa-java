package com.wgq.service;

import java.io.File;
import java.util.List;

/**
 * Description:
 * 首先定义接口MailService.java，如下所示。接口中定义了四个方法，分别是用来发送简单文本邮件、带附件邮件、带图片的邮件、使用Themeleaf构建邮件模板的。
 */

public interface MailService {


    void sendSimpleMail(String mailFrom, String mailFromNick, String mailTo, String cc, String subject, String content);

    void sendMailWithAttachments(String mailFrom, String mailFromNick, String mailTo, String cc, String subject, String content,
                                 List<File> files);

    void sendMailWithImage(String mailFrom, String mailFromNick, String mailTo, String cc, String subject, String content,
                           String[] imagePaths, String[] imageId);

    void sendHtmlMailThymeLeaf(String mailFrom, String mailFromNick, String mailTo, String cc, String subject, String content);


}