package ${packageName};

import org.tinycloud.jdbc.BaseDao;
import org.springframework.stereotype.Repository;
import ${entityPackageName}.${entityClassName};
<#list imports as importItem>
import ${importItem};
</#list>

/**
<#if tableComment?has_content>
* ${tableComment} DAO类
<#else>
* ${className} DAO类
</#if>
*
* @author ${author}
* @date ${createDate}
*/
@Repository
public class ${className} extends BaseDao<${entityClassName}, ${idType}> {

}
