package org.tinycloud.jdbc.sql;

import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Table("b_upload_file")
public class UploadFile implements Serializable {
    private static final long serialVersionUID = -1L;

    /**
     * 主键自增id
     */
    @Column(value = "id", primaryKey = true, autoIncrement = true)
    private Long id;

    /**
     * 文件id
     */
    @Column("file_id")
    private String fileId;

    /**
     * 文件原名称
     */
    @Column("file_name_old")
    private String fileNameOld;

    /**
     * 文件新名称
     */
    @Column("file_name_new")
    private String fileNameNew;

    /**
     * 文件路径
     */
    @Column("file_path")
    private String filePath;

    /**
     * 文件大小（单位kb）
     */
    @Column("file_size")
    private BigDecimal fileSize;

    /**
     * 文件大小（带单位）
     */
    @Column("file_size_str")
    private String fileSizeStr;

    /**
     * 文件后缀
     */
    @Column("file_suffix")
    private String fileSuffix;

    /**
     * 文件md5
     */
    @Column("file_md5")
    private String fileMd5;

    /**
     * 上传时间
     */
    @Column("created_at")
    private Date createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileNameOld() {
        return fileNameOld;
    }

    public void setFileNameOld(String fileNameOld) {
        this.fileNameOld = fileNameOld;
    }

    public String getFileNameNew() {
        return fileNameNew;
    }

    public void setFileNameNew(String fileNameNew) {
        this.fileNameNew = fileNameNew;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public BigDecimal getFileSize() {
        return fileSize;
    }

    public void setFileSize(BigDecimal fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileSizeStr() {
        return fileSizeStr;
    }

    public void setFileSizeStr(String fileSizeStr) {
        this.fileSizeStr = fileSizeStr;
    }

    public String getFileSuffix() {
        return fileSuffix;
    }

    public void setFileSuffix(String fileSuffix) {
        this.fileSuffix = fileSuffix;
    }

    public String getFileMd5() {
        return fileMd5;
    }

    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "UploadFile{" +
                "id=" + id +
                ", fileId='" + fileId + '\'' +
                ", fileNameOld='" + fileNameOld + '\'' +
                ", fileNameNew='" + fileNameNew + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileSize=" + fileSize +
                ", fileSizeStr='" + fileSizeStr + '\'' +
                ", fileSuffix='" + fileSuffix + '\'' +
                ", fileMd5='" + fileMd5 + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

}
