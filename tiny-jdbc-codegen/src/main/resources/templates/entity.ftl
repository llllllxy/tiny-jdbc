package ${packageName};

<#-- lombok 注解 -->
<#if enableLombok>
import lombok.Data;
</#if>
import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Id;
import org.tinycloud.jdbc.annotation.IdType;
import org.tinycloud.jdbc.annotation.Table;
<#-- 导入其他需要的类型 -->
<#list imports as importItem>
import ${importItem};
</#list>

<#-- 类注释：表注释可选，@author/@date 始终输出 -->
/**
<#if tableComment?has_content>
* ${tableComment}
*
</#if>
* @author ${author}
* @date ${createDate}
*/
<#if enableLombok>
@Data
</#if>
@Table("${tableName}")
public class ${className} {

<#-- 遍历列 -->
<#list columns as column>
    <#if column.comment?has_content || column.fillStage?has_content>
    /**
    <#if column.comment?has_content>
    * ${column.comment}
    </#if>
    <#if column.fillStage?has_content>
    * 自动填充（${column.fillStage}）：由 AuditMetaObjectHandler 赋值，无需手动设置
    </#if>
    */
    </#if>
    <#if column.primaryKey>
    @Id(idType = IdType.${column.idType})
    </#if>
    @Column(value = "${column.columnName}")
    private ${column.javaType} ${column.fieldName};

</#list>

<#-- 未启用 Lombok 时生成访问器，保证实体仍可被 ORM 读写 -->
<#if !enableLombok>
<#list columns as column>
    public ${column.javaType} get${column.fieldName?cap_first}() {
        return this.${column.fieldName};
    }

    public void set${column.fieldName?cap_first}(${column.javaType} ${column.fieldName}) {
        this.${column.fieldName} = ${column.fieldName};
    }

</#list>
</#if>
}
