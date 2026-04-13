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

<#-- 表注释 -->
<#if tableComment?has_content>
/**
* ${tableComment}
*
* @author ${author}
* @date ${createDate}
*/
</#if>
<#if enableLombok>
@Data
</#if>
@Table(name = "${tableName}")
public class ${className} {

<#-- 遍历列 -->
<#list columns as column>
    <#if column.comment?has_content>
    /**
    * ${column.comment}
    */
    </#if>
    <#if column.primaryKey>
    @Id(idType = IdType.${column.idType})
    </#if>
    @Column(value = "${column.columnName}")
    private ${column.javaType} ${column.fieldName};

</#list>
}