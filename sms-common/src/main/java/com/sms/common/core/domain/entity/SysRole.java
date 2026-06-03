package com.sms.common.core.domain.entity;

import java.util.Set;
import javax.validation.constraints.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.sms.common.annotation.Excel;
import com.sms.common.annotation.Excel.ColumnType;
import com.sms.common.core.domain.BaseEntity;

/**
 * 角色表 sys_role
 *
 */
public class SysRole extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 角色ID */
    @Excel(name = "角色序號", cellType = ColumnType.NUMERIC)
    private Long roleId;

    /** 角色名稱 */
    @Excel(name = "角色名稱")
    private String roleName;

    /** 角色權限 */
    @Excel(name = "角色權限")
    private String roleKey;

    /** 角色排序 */
    @Excel(name = "角色排序", cellType = ColumnType.NUMERIC)
    private String roleSort;

    /** 數據範圍（1：所有數據權限；2：自定義數據權限；3：本部門數據權限；4：本部門及以下數據權限；5：僅本人數據權限） */
    @Excel(name = "數據範圍", readConverterExp = "1=所有數據權限,2=自定義數據權限,3=本部門數據權限,4=本部門及以下數據權限,5=僅本人數據權限")
    private String dataScope;

    /** 角色狀態（0正常 1停用） */
    @Excel(name = "角色狀態", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 刪除標誌（0代表存在 2代表刪除） */
    private String delFlag;

    /** 用戶是否存在此角色標識 默認不存在 */
    private boolean flag = false;

    /** 菜單組 */
    private Long[] menuIds;

    /** 部門組（數據權限） */
    private Long[] deptIds;

    /** 角色菜單權限 */
    private Set<String> permissions;

    public SysRole()
    {

    }

    public SysRole(Long roleId)
    {
        this.roleId = roleId;
    }

    public Long getRoleId()
    {
        return roleId;
    }

    public void setRoleId(Long roleId)
    {
        this.roleId = roleId;
    }

    public boolean isAdmin()
    {
        return isAdmin(this.roleId);
    }

    public static boolean isAdmin(Long roleId)
    {
        return roleId != null && 1L == roleId;
    }

    public String getDataScope()
    {
        return dataScope;
    }

    public void setDataScope(String dataScope)
    {
        this.dataScope = dataScope;
    }

    @NotBlank(message = "角色名稱不能爲空")
    @Size(min = 0, max = 30, message = "角色名稱長度不能超過30個字符")
    public String getRoleName()
    {
        return roleName;
    }

    public void setRoleName(String roleName)
    {
        this.roleName = roleName;
    }

    @NotBlank(message = "權限字符不能爲空")
    @Size(min = 0, max = 100, message = "權限字符長度不能超過100個字符")
    public String getRoleKey()
    {
        return roleKey;
    }

    public void setRoleKey(String roleKey)
    {
        this.roleKey = roleKey;
    }

    @NotBlank(message = "顯示順序不能爲空")
    public String getRoleSort()
    {
        return roleSort;
    }

    public void setRoleSort(String roleSort)
    {
        this.roleSort = roleSort;
    }

    public String getStatus()
    {
        return status;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public boolean isFlag()
    {
        return flag;
    }

    public void setFlag(boolean flag)
    {
        this.flag = flag;
    }

    public Long[] getMenuIds()
    {
        return menuIds;
    }

    public void setMenuIds(Long[] menuIds)
    {
        this.menuIds = menuIds;
    }

    public Long[] getDeptIds()
    {
        return deptIds;
    }

    public void setDeptIds(Long[] deptIds)
    {
        this.deptIds = deptIds;
    }

    public Set<String> getPermissions()
    {
        return permissions;
    }

    public void setPermissions(Set<String> permissions)
    {
        this.permissions = permissions;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("roleId", getRoleId())
            .append("roleName", getRoleName())
            .append("roleKey", getRoleKey())
            .append("roleSort", getRoleSort())
            .append("dataScope", getDataScope())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
