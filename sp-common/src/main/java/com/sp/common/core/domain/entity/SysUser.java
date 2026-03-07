package com.sp.common.core.domain.entity;

import java.util.Date;
import java.util.List;
import javax.validation.constraints.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sp.common.annotation.Excel;
import com.sp.common.annotation.Excel.ColumnType;
import com.sp.common.annotation.Excel.Type;
import com.sp.common.annotation.Excels;
import com.sp.common.core.domain.BaseEntity;
import com.sp.common.xss.Xss;

/**
 * 用户对象 sys_user
 *
 */
public class SysUser extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用戶 ID */
    @Excel(name = "用戶序號", type = Type.EXPORT, cellType = ColumnType.NUMERIC, prompt = "用戶編號")
    private Long userId;
    
    /** 部門 ID */
    @Excel(name = "部門編號", type = Type.IMPORT)
    private Long deptId;
    
    /** 部門父 ID */
    private Long parentId;
    
    /** 角色 ID */
    private Long roleId;

    /** 登錄名稱 */
    @Excel(name = "登錄名稱")
    private String loginName;
    
    /** 用戶名稱 */
    @Excel(name = "用戶名稱")
    private String userName;
    
    /** 用戶類型 */
    private String userType;
    
    /** 用戶郵箱 */
    @Excel(name = "用戶郵箱")
    private String email;
    
    /** 手機號碼 */
    @Excel(name = "手機號碼", cellType = ColumnType.TEXT)
    private String phonenumber;
    
    /** 用戶性別 */
    @Excel(name = "用戶性別", readConverterExp = "0=男，1=女，2=未知")
    private String sex;

    /** 用戶頭像 */
    private String avatar;

    /** 密碼 */
    private String password;

    /** 鹽加密 */
    private String salt;

    /** 賬號狀態（0 正常 1 停用） */
    @Excel(name = "賬號狀態", readConverterExp = "0=正常，1=停用")
    private String status;

    /** 刪除標誌（0 代表存在 2 代表刪除） */
    private String delFlag;

    /** 最後登錄 IP */
    @Excel(name = "最後登錄 IP", type = Type.EXPORT)
    private String loginIp;
    
    /** 最後登錄時間 */
    @Excel(name = "最後登錄時間", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", type = Type.EXPORT)
    private Date loginDate;

    /** 密碼最後更新時間 */
    private Date pwdUpdateDate;

    /** 部門對象 */
    @Excels({
        @Excel(name = "部門名稱", targetAttr = "deptName", type = Type.EXPORT),
        @Excel(name = "部門負責人", targetAttr = "leader", type = Type.EXPORT)
    })
    private SysDept dept;

    private List<SysRole> roles;

    /** 角色組 */
    private Long[] roleIds;

    /** 崗位組 */
    private Long[] postIds;

    public SysUser()
    {

    }

    public SysUser(Long userId)
    {
        this.userId = userId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public boolean isAdmin()
    {
        return isAdmin(this.userId);
    }

    public static boolean isAdmin(Long userId)
    {
        return userId != null && 1L == userId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public Long getRoleId()
    {
        return roleId;
    }

    public void setRoleId(Long roleId)
    {
        this.roleId = roleId;
    }

    @Xss(message = "登錄賬號不能包含腳本字符")
    @NotBlank(message = "登錄賬號不能為空")
    @Size(min = 0, max = 30, message = "登錄賬號長度不能超過 30 個字元")
    public String getLoginName()
    {
        return loginName;
    }

    public void setLoginName(String loginName)
    {
        this.loginName = loginName;
    }

    @Xss(message = "用戶暱稱不能包含腳本字符")
    @Size(min = 0, max = 30, message = "用戶暱稱長度不能超過 30 個字元")
    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getUserType()
    {
        return userType;
    }

    public void setUserType(String userType)
    {
        this.userType = userType;
    }

    @Email(message = "郵箱格式不正確")
    @Size(min = 0, max = 50, message = "郵箱長度不能超過 50 個字元")
    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    @Size(min = 0, max = 11, message = "手機號碼長度不能超過 11 個字元")
    public String getPhonenumber()
    {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber)
    {
        this.phonenumber = phonenumber;
    }

    public String getSex()
    {
        return sex;
    }

    public void setSex(String sex)
    {
        this.sex = sex;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public void setAvatar(String avatar)
    {
        this.avatar = avatar;
    }

    @JsonIgnore
    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    @JsonIgnore
    public String getSalt()
    {
        return salt;
    }

    public void setSalt(String salt)
    {
        this.salt = salt;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getLoginIp()
    {
        return loginIp;
    }

    public void setLoginIp(String loginIp)
    {
        this.loginIp = loginIp;
    }

    public Date getLoginDate()
    {
        return loginDate;
    }

    public void setLoginDate(Date loginDate)
    {
        this.loginDate = loginDate;
    }

    public Date getPwdUpdateDate()
    {
        return pwdUpdateDate;
    }

    public void setPwdUpdateDate(Date pwdUpdateDate)
    {
        this.pwdUpdateDate = pwdUpdateDate;
    }

    public SysDept getDept()
    {
        if (dept == null)
        {
            dept = new SysDept();
        }
        return dept;
    }

    public void setDept(SysDept dept)
    {
        this.dept = dept;
    }

    public List<SysRole> getRoles()
    {
        return roles;
    }

    public void setRoles(List<SysRole> roles)
    {
        this.roles = roles;
    }

    public Long[] getRoleIds()
    {
        return roleIds;
    }

    public void setRoleIds(Long[] roleIds)
    {
        this.roleIds = roleIds;
    }

    public Long[] getPostIds()
    {
        return postIds;
    }

    public void setPostIds(Long[] postIds)
    {
        this.postIds = postIds;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("userId", getUserId())
            .append("deptId", getDeptId())
            .append("loginName", getLoginName())
            .append("userName", getUserName())
            .append("userType", getUserType())
            .append("email", getEmail())
            .append("phonenumber", getPhonenumber())
            .append("sex", getSex())
            .append("avatar", getAvatar())
            .append("password", getPassword())
            .append("salt", getSalt())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("loginIp", getLoginIp())
            .append("loginDate", getLoginDate())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .append("dept", getDept())
			.append("roles", getRoles())
            .toString();
    }
}
