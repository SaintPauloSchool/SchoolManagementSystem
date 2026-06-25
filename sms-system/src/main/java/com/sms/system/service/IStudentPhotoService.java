package com.sms.system.service;

import com.sms.system.entity.vo.StudentPhotoVO;

/**
 * 學生照片代理服務
 */
public interface IStudentPhotoService {

    /**
     * 按學生個人編號從學籍系統拉取照片
     *
     * @param studentProfileNumber 學生個人編號（純數字）
     * @return 照片數據，不存在或失敗時返回 null
     */
    StudentPhotoVO fetchPhoto(String studentProfileNumber);
}
