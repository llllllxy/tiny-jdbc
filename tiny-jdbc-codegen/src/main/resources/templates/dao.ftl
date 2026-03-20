package ${packageName};

import org.tinycloud.jdbc.BaseDao;
import ${entityPackageName}.${entityClassName};

/**
<#if tableComment?has_content>
* ${tableComment} DAO接口
<#else>
* ${className} DAO接口
</#if>
*
* @author tiny-jdbc
* @date ${createDate}
*/
public interface ${className} extends BaseDao<${entityClassName}, ${idType}> {

}