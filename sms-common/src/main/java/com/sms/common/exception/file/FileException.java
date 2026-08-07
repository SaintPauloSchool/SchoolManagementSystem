package com.sms.common.exception.file;

import com.sms.common.exception.base.BaseException;

/**
 * 文件資訊異常類
 *
 */
public class FileException extends BaseException
{
    private static final long serialVersionUID = 1L;

    public FileException(String code, Object[] args)
    {
        super("file", code, args, null);
    }

}
