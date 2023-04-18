package com.test.upload;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * @author 何嘉豪
 */
@Getter
@Setter
@ToString
public class UploadResult {

    @JSONField(name = "A01_UpLoadFile")
    private List<UploadFile> uploadFile;

    @Getter
    @Setter
    @ToString
    public static class UploadFile{

        @JSONField(name = "OLDFILENAME")
        private String oldFileName;

        @JSONField(name = "NEWFILENAME")
        private String newFileName;
    }

}
