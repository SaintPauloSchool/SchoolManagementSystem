package com.sms.common.core.controller;

import java.beans.PropertyEditorSupport;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sms.common.core.domain.AjaxResult;
import com.sms.common.core.domain.AjaxResult.Type;
import com.sms.common.core.domain.entity.SysUser;
import com.sms.common.core.page.PageDomain;
import com.sms.common.core.page.TableDataInfo;
import com.sms.common.core.page.TableSupport;
import com.sms.common.utils.DateUtils;
import com.sms.common.utils.PageUtils;
import com.sms.common.utils.ServletUtils;
import com.sms.common.utils.SecurityUtils;
import com.sms.common.utils.StringUtils;
import com.sms.common.utils.sql.SqlUtil;

/**
 * web層通用數據處理
 *
 */
public class BaseController
{
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 將前臺傳遞過來的日期格式的字符串，自動轉化爲Date類型
     */
    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
        // Date 類型轉換
        binder.registerCustomEditor(Date.class, new PropertyEditorSupport()
        {
            @Override
            public void setAsText(String text)
            {
                setValue(DateUtils.parseDate(text));
            }
        });
        
        // LocalDateTime 類型轉換
        binder.registerCustomEditor(LocalDateTime.class, new PropertyEditorSupport()
        {
            @Override
            public void setAsText(String text)
            {
                if (StringUtils.isEmpty(text))
                {
                    setValue(null);
                }
                else
                {
                    try
                    {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        setValue(LocalDateTime.parse(text, formatter));
                    }
                    catch (Exception e)
                    {
                        // 嘗試其他格式
                        try
                        {
                            setValue(LocalDateTime.parse(text + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        }
                        catch (Exception ex)
                        {
                            logger.warn("日期格式轉換失敗: {}", text);
                            setValue(null);
                        }
                    }
                }
            }
        });
    }

    /**
     * 設置請求分頁數據
     */
    protected void startPage()
    {
        PageUtils.startPage();
    }

    /**
     * 設置請求排序數據
     */
    protected void startOrderBy()
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        if (StringUtils.isNotEmpty(pageDomain.getOrderBy()))
        {
            String orderBy = SqlUtil.escapeOrderBySql(pageDomain.getOrderBy());
            PageHelper.orderBy(orderBy);
        }
    }

    /**
     * 清理分頁的線程變量
     */
    protected void clearPage()
    {
        PageUtils.clearPage();
    }

    /**
     * 獲取request
     */
    public HttpServletRequest getRequest()
    {
        return ServletUtils.getRequest();
    }

    /**
     * 獲取response
     */
    public HttpServletResponse getResponse()
    {
        return ServletUtils.getResponse();
    }

    /**
     * 獲取session
     */
    public HttpSession getSession()
    {
        return getRequest().getSession();
    }

    /**
     * 響應請求分頁數據
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected TableDataInfo getDataTable(List<?> list)
    {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(0);
        rspData.setRows(list);
        rspData.setTotal(new PageInfo(list).getTotal());
        return rspData;
    }

    /**
     * 響應返回結果
     * 
     * @param rows 影響行數
     * @return 操作結果
     */
    protected AjaxResult toAjax(int rows)
    {
        return rows > 0 ? success() : error();
    }

    /**
     * 響應返回結果
     * 
     * @param result 結果
     * @return 操作結果
     */
    protected AjaxResult toAjax(boolean result)
    {
        return result ? success() : error();
    }

    /**
     * 返回成功
     */
    public AjaxResult success()
    {
        return AjaxResult.success();
    }

    /**
     * 返回失敗消息
     */
    public AjaxResult error()
    {
        return AjaxResult.error();
    }

    /**
     * 返回成功消息
     */
    public AjaxResult success(String message)
    {
        return AjaxResult.success(message);
    }

    /**
     * 返回成功數據
     */
    public static AjaxResult success(Object data)
    {
        return AjaxResult.success("操作成功", data);
    }

    /**
     * 返回失敗消息
     */
    public AjaxResult error(String message)
    {
        return AjaxResult.error(message);
    }

    /**
     * 返回錯誤碼消息
     */
    public AjaxResult error(Type type, String message)
    {
        return new AjaxResult(type, message);
    }

    /**
     * 頁面跳轉
     */
    public String redirect(String url)
    {
        return StringUtils.format("redirect:{}", url);
    }

    /**
     * 獲取用戶緩存信息
     */
    public SysUser getSysUser()
    {
        return SecurityUtils.getSysUser();
    }

    /**
     * 設置用戶緩存信息
     */
    public void setSysUser(SysUser user)
    {
        SecurityUtils.setSysUser(user);
    }

    /**
     * 獲取登錄用戶id
     */
    public Long getUserId()
    {
        SysUser user = getSysUser();
        if (user == null)
        {
            throw new IllegalStateException("未登錄或用戶信息不存在，請檢查 Token 是否有效");
        }
        return user.getUserId();
    }

    /**
     * 獲取登錄用戶名
     */
    public String getLoginName()
    {
        SysUser user = getSysUser();
        if (user == null)
        {
            throw new IllegalStateException("未登錄或用戶信息不存在，請檢查 Token 是否有效");
        }
        return user.getLoginName();
    }

    /**
     * 獲取用戶名
     */
    public String getUsername()
    {
        SysUser user = getSysUser();
        if (user == null)
        {
            throw new IllegalStateException("未登錄或用戶信息不存在，請檢查 Token 是否有效");
        }
        return user.getUserName();
    }

    /**
     * 獲取用戶類型
     */
    public String getUserType()
    {
        SysUser user = getSysUser();
        if (user == null)
        {
            throw new IllegalStateException("未登錄或用戶信息不存在，請檢查 Token 是否有效");
        }
        return user.getUserType();
    }

    /**
     * 獲取企業微信原始 userId 字串
     */
    public String getOpenUserId()
    {
        SysUser user = getSysUser();
        if (user == null)
        {
            throw new IllegalStateException("未登錄或用戶信息不存在，請檢查 Token 是否有效");
        }
        return user.getOpenUserId();
    }
}
