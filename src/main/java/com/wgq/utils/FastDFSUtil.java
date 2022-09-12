package com.wgq.utils;

import org.csource.common.MyException;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


/**
 *
 */
@Component
public class FastDFSUtil {
    private static StorageClient1 client1;
    private static NameValuePair pairs[] = null;

    //建立StorageClient1
    static {
        try {
            ClientGlobal.initByProperties("fastdfs-client.properties");
            TrackerClient trackerClient = new TrackerClient();
            TrackerServer trackerServer = trackerClient.getConnection();
            ProtoCommon.activeTest(trackerServer.getSocket());
            StorageServer storageServer = null;
            client1 = new StorageClient1(trackerServer, storageServer);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
    }

    /**
     * upload上传方法
     * @param file MultipartFile类型文件
     * @return
     */
    public static String upload(MultipartFile file) {
        String oldName = file.getOriginalFilename();//以byte流形式传输，适合小文件，因为是占用的内存。
        try {
            return client1.upload_file1(file.getBytes(), oldName.substring(oldName.lastIndexOf(".") + 1), pairs);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
        return null;
    }

}
