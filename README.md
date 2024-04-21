<h1 align="center">tiny-jdbc</h1>
<h1 align="center">一个优雅的 ORM 框架，轻量、灵活、高性能</h1>


<p align="center">
    <a href="https://github.com/llllllxy/tiny-jdbc-boot-starter/blob/master/LICENSE">
        <img src="https://img.shields.io/github/license/llllllxy/tiny-jdbc-boot-starter.svg?style=flat-square">
    </a>
	<a target="_blank" href="https://www.oracle.com/technetwork/java/javase/downloads/index.html">
		<img src="https://img.shields.io/badge/JDK-8+-blue.svg" />
	</a>
    <a href="https://github.com/llllllxy/tiny-jdbc-boot-starter/stargazers">
        <img src="https://img.shields.io/github/stars/llllllxy/tiny-jdbc-boot-starter?style=flat-square&logo=GitHub">
    </a>
    <a href="https://github.com/llllllxy/tiny-jdbc-boot-starter/network/members">
        <img src="https://img.shields.io/github/forks/llllllxy/tiny-jdbc-boot-starter?style=flat-square&logo=GitHub">
    </a>
    <a href="https://github.com/llllllxy/tiny-jdbc-boot-starter/watchers">
        <img src="https://img.shields.io/github/watchers/llllllxy/tiny-jdbc-boot-starter?style=flat-square&logo=GitHub">
    </a>
    <a href="https://github.com/llllllxy/tiny-jdbc-boot-starter/issues">
        <img src="https://img.shields.io/github/issues/llllllxy/tiny-jdbc-boot-starter.svg?style=flat-square&logo=GitHub">
    </a>
    <a href='https://gitee.com/leisureLXY/tiny-security-boot-starter'>
        <img src='https://gitee.com/leisureLXY/tiny-jdbc-boot-starter/badge/star.svg?theme=dark' alt='star' />
    </a>
    <br/>
</p>

## 1、简介

`tiny-jdbc-boot-starter`是一个基于`Spring Data JDBC`开发的轻量、灵活、高性能的数据库ORM框架，
在不改变原有功能的基础上，做了大量的增强，让操作数据库这件事变得更加简单！

### 优势

- **轻量级**：除了 Spring Data JDBC 本身，再无任何第三方依赖，轻量可靠
- **性能高**：基于高性能的Spring Data JDBC，性能基本无损耗
- **功能强**：既支持原生SQL操作、又支持实体类映射操作，BaseDao里封装了大量的通用方法，配合强大灵活的条件构造器，基本满足各类使用需求
- **支持 Lambda 形式调用**：条件构造器支持Lambda形式调用，编译期增强，无需再担心字段写错
- **支持主键自动生成**：内含多种主键生成策略（包括自增主键、UUID、雪花ID等，也支持自定义ID生成策略）
- **支持多种数据库分页方言**：包括MySQL、ORACLE、DB2、PostgreSql等多种常用数据库，无需配置，自动识别数据库类型

### 支持数据库

- 分页插件目前支持MySQL、MARIADB、ORACLE、DB2、PostgreSql、SQLite、H2、OceanBase、openGauss、informix、达梦数据库、瀚高数据库等常见数据库
- 其他操作支持任何使用标准 SQL 的数据库

## 2、快速开始

### 引入Maven依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<dependency>
    <groupId>top.lxyccc</groupId>
    <artifactId>tiny-jdbc-boot-starter</artifactId>
    <version>1.7.4</version>
</dependency>
```

### application.yml参数配置 
- **或application.properties**

```yaml
tiny-jdbc:
  # 打印banner，默认false
  banner: true
  # 分页器适配类型，可不配置，不配置的话会自动获取
  db-type: mysql
```

### 定义Entity实体类
- **实体类对应数据库的一张表**
- **实体类属性名必须使用`驼峰命名规则`，与数据库表字段一一映射**

```java

@Table("b_upload_file")
public class UploadFile implements Serializable {
    private static final long serialVersionUID = -1L;

    /**
     * @Id注解用于标记表的主键，目前只支持单主键，请不要在多个属性上设置此注解，会导致程序出错
     *
     * idType: 主键ID策略，目前支持以下7种 AUTO_INCREMENT、INPUT、OBJECT_ID、ASSIGN_ID、UUID、SEQUENCE、CUSTOM
     * value: 则代表的是sequence 序列的 sql 内容，idType=SEQUENCE时，必须设置此内容
     * 
     * 注意！
     * 如果设置为 AUTO_INCREMENT 自增主键 的话，则此字段必须为Long
     * 如果设置为 UUID 的话，则此属性类型必须为String
     * 如果设置为 OBJECT_ID 的话，则此属性类型必须为String
     * 如果设置为 ASSIGN_ID 的话，则此属性类型必须为String或者Long
     * 如果设置为 SEQUENCE 的话，则此属性类型必须为Integer或者Long，且必须设置value属性为查询序列值的SQL
     * 如果设置为 CUSTOM 的话，则需要自己实现 IdGeneratorInterface 接口并注册为bean
     * 
     * 
     * @Column注解用于标记属性和表字段的对应关系
     */
    @Id(idType = IdType.AUTO_INCREMENT)
    @Column(value = "id")
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

    ...
}
```

### 定义Dao类，继承自BaseDao
- **泛型一为对应实体类，泛型二实体类主键类型**

```java
import org.springframework.stereotype.Repository;
import org.tinycloud.jdbc.BaseDao;
import org.tinycloud.entity.UploadFile;

@Repository
public class UploadFileDao extends BaseDao<UploadFile, Long> {
    
    ...
}
```

### Service层注入使用

```java
import org.springframework.stereotype.Service;
import org.tinycloud.dao.UploadFileDao;

@Service
public class UploadFileService {

    @Autowired
    private UploadFileDao uploadFileDao;
    
    ...
}
```

## 3、注解说明

#### @Table
- 描述：表名注解，标识实体类对应的表
- 使用位置：实体类
```java
@Table("b_upload_file")
public class UploadFile implements Serializable {
 private static final long serialVersionUID = -1L;

 ...
}
```
|属性|类型|必须指定|默认值|描述|
|---|---|---|---|---|
| value  | String  |  是 | ""    | 对应数据库表名  |

#### @Id
- 描述：主键注解，标识实体类主键属性
- 使用位置：实体类属性
```java
 @Table("b_upload_file")
 public class UploadFile implements Serializable {
     private static final long serialVersionUID = -1L;
     
     @Id(idType = IdType.AUTO_INCREMENT)
     @Column(value = "id")
     private Long id;
     ...
 }
```
|属性|类型|必须指定|默认值|描述|
|---|---|---|---|---|
| idType | String  |  是 | IdType.INPUT | 主键策略  |
| value  | String  |  否 |  ""  | 若 idType 类型是 sequence， value 则代表的是sequence 序列的 sql 内容，此时必填  |

##### IdType主键策略说明
|值|描述|
|---|---|
| INPUT            | insert 前自行 set 主键值  |  
| AUTO_INCREMENT   | 数据库 ID 自增 |  
| OBJECT_ID        | 自动设置 MongoDb objectId 作为主键值 |  
| ASSIGN_ID        | 自动设置 雪花ID 作为主键值 |  
| UUID             | 自动设置 UUID 作为主键值 |  
| SEQUENCE         | 自动设置 调用序列SQL结果 作为主键值 |  
| CUSTOM           | 自定义主键ID生成器，需自行实现 IdGeneratorInterface 接口，详见[自定义ID生成器](#idGen) | 

#### @Column
- 描述：字段注解，标识实体类属性和字段的对应关系
- 使用位置：实体类属性
```java
 @Table("b_upload_file")
 public class UploadFile implements Serializable {
     private static final long serialVersionUID = -1L;

     @Id(idType = IdType.AUTO_INCREMENT)
     @Column(value = "id")
     private Long id;
     
     @Column("file_id")
     private String fileId;

 }
```
|属性|类型|必须指定|默认值|描述|
|---|---|---|---|---|
| value         | String  |  是 | ""    | 对应数据库字段名  |

## 4、BaseDao CRUD接口说明

### 查询操作

|方法|说明|
|---|---|
|`<F> List<F> select(String sql, Class<F> classz, Object... params);` |根据给定的sql和实体类型和参数，查询数据库并返回实体类对象列表|
|`List<T> select(String sql, Object... params);` |根据给定的sql和参数，查询数据库并返回实体类对象列表，类型使用的是xxxDao<T>的类型|
|`<F> F selectOne(String sql, Class<F> classz, Object... params);`|根据给定的sql和实体类型和参数，查询数据并返回一个实体类对象|
|`T selectOne(String sql, Object... params);`|根据给定的sql和参数，查询数据并返回一个实体类对象，类型使用的是xxxDao<T>的类型|
|`List<Map<String, Object>> selectMap(String sql, Object... params);`|根据给定的sql和参数，查询数据库并返回Map<String, Object>列表|
|`Map<String, Object> selectOneMap(String sql, Object... params);`|根据给定的sql和参数，查询数据并返回一个Map<String, Object>对象|
|`<T> T selectOneColumn(String sql, Class<T> clazz, Object... params);`|根据给定的sql和实体类型和参数，查询数据并返回一个值（常用于查count）|
|`Page<F> paginate(String sql, Class<F> clazz, Page<F> page, Object... params);`|执行分页查询，返回Page对象，类型使用的Class<F>传入的自定义类型|
|`Page<T> paginate(String sql, Page<T> page, Object... params);`|执行分页查询，返回Page对象，类型使用的是xxxDao<T>的类型|
|`T selectById(Object id);`|根据主键ID值，查询数据并返回一个实体类对象，类型使用的是xxxDao<T>的类型|
|`T selectByIds(List<ID> ids);`|根据主键ID值列表，查询数据并返回实体类对象列表，类型使用的是xxxDao<T>的类型|
|`T selectByIds(ID... id);`|根据主键ID值可变参数列表，查询数据并返回实体类对象列表，类型使用的是xxxDao<T>的类型|
|`List<T> select(T entity);`|实体类里面非null的属性作为查询条件，查询数据库并返回实体类对象列表，类型使用的是xxxDao<T>的类型|
|`List<T> select(QueryCriteria<T> criteria);`|根据查询构造器查询，返回多条，类型使用的是xxxDao<T>的类型|
|`List<T> select(LambdaQueryCriteria<T> lambdaCriteria);`|根据查询构造器(lambda)查询，返回多条，查询数据并返回一个实体类对象，类型使用的是xxxDao<T>的类型|
|`T selectOne(T entity);`|实体类里面非null的属性作为查询条件，查询数据并返回一个实体类对象，类型使用的是xxxDao<T>的类型|
|`T selectOne(QueryCriteria<T> criteria);`|根据查询构造器执行查询，返回一条，类型使用的是xxxDao<T>的类型|
|`T selectOne(LambdaQueryCriteria<T> lambdaCriteria);`|根据查询构造器(lambda)执行查询，返回一条，类型使用的是xxxDao<T>的类型|
|`Page<T> paginate(T entity, Page<T> page);`|根据实体类里面非null的属性作为查询条件，执行分页查询，类型使用的是xxxDao<T>的类型|
|`Page<T> paginate(QueryCriteria<T> criteria, Page<T> page);`|根据查询构造器执行分页查询，返回Page对象，类型使用的是xxxDao<T>的类型|
|`Page<T> paginate(LambdaQueryCriteria<T> lambdaCriteria, Page<T> page);`|根据查询构造器(lambda)执行分页查询，返回Page对象，类型使用的是xxxDao<T>的类型|
|`Long selectCount(QueryCriteria<T> criteria);`|根据查询构造器执行总记录数查询，返回符合条件的总记录数量|
|`Long selectCount(LambdaQueryCriteria<T> lambdaCriteria);`|根据查询构造器(lambda)执行总记录数查询，返回符合条件的总记录数量|
|`boolean exists(QueryCriteria<T> criteria);`|根据查询构造器执行查询记录是否存在，返回true或者false|
|`boolean exists(LambdaQueryCriteria<T> lambdaCriteria);`|根据查询构造器(lambda)执行查询记录是否存在，返回true或者false|

### 插入操作

|方法|说明|
|---|---|
|`int insert(String sql, final Object... params);`|根据提供的SQL语句和提供的参数，执行插入|
|`int insert(T entity);`|插入entity里的数据，忽略entity里值为null的属性，如果主键策略为assignId、uuid、objectId或custom，那将在entity里返回自动生成的主键值|
|`int insert(T entity, boolean ignoreNulls);`|插入entity里的数据，可选择是否忽略entity里值为null的属性，如果主键策略为assignId、uuid、objectId或custom，那将在entity里返回自动生成的主键值|
|`Long insertReturnAutoIncrement(T entity);`|插入entity里的数据，将忽略entity里属性值为null的属性，并且返回自增的主键|
|`Long insertReturnAutoIncrement(T entity, boolean ignoreNulls);`|插入entity里的数据，可选择是否忽略entity里值为null的属性，并且返回自增的主键|

### 更新操作

|方法|说明|
|---|---|
|`int update(String sql, final Object... params);`|根据提供的SQL语句和提供的参数，执行修改|
|`int updateById(T entity);`|根据entity内的主键值作为条件更新数据，默认忽略entity里值为null的属性|
|`int updateById(T entity, boolean ignoreNulls);`|根据entity内的主键值更新数据，可选择是否忽略entity里值为null的属性|
|`int update(T entity, UpdateCriteria<T> criteria);`|根据entity里的值和条件构造器，执行修改，默认忽略entity里值为null的属性|
|`int update(T entity, LambdaUpdateCriteria<T> criteria);`|根据entity里的值和条件构造器（lambda），执行修改，默认忽略entity里值为null的属性|
|`int update(T entity, boolean ignoreNulls, UpdateCriteria<T> criteria);`|根据entity里的值和条件构造器，执行修改，可选择是否忽略entity里值为null的属性|
|`int update(T entity, boolean ignoreNulls, LambdaUpdateCriteria<T> criteria);`|根据entity里的值和条件构造器（lambda），执行修改，可选择是否忽略entity里值为null的属性|
|`int update(UpdateCriteria<T> criteria);`|只根据条件构造器来更新数据，配合.set方法来使用|
|`int update(LambdaUpdateCriteria<T> criteria);`|只根据条件构造器（lambda）来更新数据，配合.set方法来使用|

#### 删除操作

|方法|说明|
|---|---|
|`int delete(String sql, final Object... params);` | 根据提供的SQL语句和提供的参数，执行删除 |
|`int deleteById(ID id);` | 根据主键ID进行删除，类型使用的是xxxDao<T, ID>的类型 |
|`int deleteByIds(List<ID> ids);` | 根据主键ID列表进行删除，类型使用的是xxxDao<T, ID>的类型 |
|`int deleteByIds(ID... ids);` | 根据主键ID可变参数列表进行删除，类型使用的是xxxDao<T, ID>的类型 |
|`int delete(T entity);`| 根据entity里的属性值进行删除，entity里不为null的属性，将作为where参数 |
|`int delete(UpdateCriteria<T> criteria);`| 根据条件构造器，将作为where参数 |
|`int delete(LambdaUpdateCriteria<T> criteria);`| 根据条件构造器（lambda），将作为where参数 |

## 5、条件构造器

### 使用说明

#### 条件构造器(AbstractCriteria & AbstractLambdaCriteria)
> QueryCriteria(LambdaQueryCriteria) 和 UpdateCriteria(LambdaUpdateCriteria) 的父类
> 用于生成 sql 的 where 条件

|方法|说明|示例|lambda示例|
|---|---|---|---|
|eq        | 等于 =      | eq("name", "张三") ---> AND name = '张三' | eq(User::getName, "张三") ---> AND name = '张三' |
|orEq      | 等于 =      | orEq("name", "张三") ---> OR name = '张三' | orEq(User::getName, "张三") ---> OR name = '张三' |
|notEq     | 不等于 <>   | notEq("name", "张三") ---> AND name <> '张三' | notEq(User::getName, "张三") ---> AND name <> '张三' |
|orNotEq   | 不等于 <>   | orNotEq("name", "张三") ---> OR name <> '张三' | orNotEq(User::getName, "张三") ---> OR name <> '张三' |
|isNull    | 等于null    | isNull("name") ---> AND name IS NULL | isNull(User::getName) ---> AND name IS NULL |
|orIsNull  | 等于null    | orIsNull("name") ---> OR name IS NULL | orIsNull(User::getName) ---> OR name IS NULL |
|isNotNull | 不等于null   | isNotNull("name") ---> AND name IS NOT NULL | isNotNull(User::getName) ---> AND name IS NOT NULL |
|orIsNotNull  | 不等于null    | orIsNotNull("name") ---> OR name IS NOT NULL | orIsNotNull(User::getName) ---> OR name IS NOT NULL |
|lt        | 小于 <      | lt("name", "张三") ---> AND name < '张三'  | lt(User::getName, "张三") ---> AND name < '张三' |
|orLt      | 小于 <      | orLt("name", "张三") ---> OR name < '张三'  | orLt(User::getName, "张三") ---> OR name < '张三' |
|lte       | 小于等于 <=  | lte("name", "张三") ---> AND name <= '张三' | lte(User::getName, "张三") ---> AND name <= '张三' |
|orLte     | 小于等于 <=  | orLte("name", "张三") ---> OR name <= '张三'  | orLte(User::getName, "张三") ---> OR name <= '张三' |
|gt        | 大于 >      | gt("name", "张三") ---> AND name > '张三'  | gt(User::getName, "张三") ---> AND name > '张三' |
|orGt      | 大于 >      | orGt("name", "张三") ---> OR name > '张三'  | orGt(User::getName, "张三") ---> OR name > '张三' |
|gte       | 大于等于 >=  | gte("name", "张三") ---> AND name >= '张三' | gte(User::getName, "张三") ---> AND name >= '张三' |
|orGte     | 大于等于 >=  | orGte("name", "张三") ---> OR name >= '张三'  | orGte(User::getName, "张三") ---> OR name >= '张三' |
|in        | SQL里的IN操作  | in("name", {"张三","李四"}) ---> AND name IN ('张三','李四')  | in(User::getName, "张三") ---> AND name IN ('张三','李四') |
|orIn      | SQL里的IN操作  | orIn("name", {"张三","李四"}) ---> OR name IN ('张三','李四')  | orIn(User::getName, "张三") ---> OR name IN ('张三','李四') |
|notIn     | SQL里的IN操作  | notIn("name", "张三") ---> AND name NOT IN ('张三','李四') | notIn(User::getName, "张三") ---> AND name NOT IN ('张三','李四') |
|orNotIn   | SQL里的IN操作  | orNotIn("name", {"张三","李四"}) ---> OR name NOT IN ('张三','李四')  | orNotIn(User::getName, "张三") ---> OR name NOT IN ('张三','李四') |
|between    | NOT BETWEEN 值1 AND 值2 | between("age", 10, 18) ---> AND (age BETWEEN 10 AND 18) | between(User::getAge, 10, 18) ---> AND (age BETWEEN 10 AND 18 ) |
|orBetween  | NOT BETWEEN 值1 AND 值2 | orBetween("age", 10, 18) ---> OR (age BETWEEN 10 AND 18) | orBetween(User::getAge, 10, 18) ---> OR (age BETWEEN 10 AND 18 ) |
|notBetween | NOT BETWEEN 值1 AND 值2 | notBetween("age", 10, 18) ---> AND (age NOT BETWEEN 10 AND 18) | notBetween(User::getAge, 10, 18) ---> AND (age NOT BETWEEN 10 AND 18 ) |
|orNotBetween| NOT BETWEEN 值1 AND 值2| orNotBetween("age", 10, 18) ---> OR (age NOT BETWEEN 10 AND 18) | orNotBetween(User::getAge, 10, 18) ---> OR (age NOT BETWEEN 10 AND 18 ) |
|like       | LIKE '%值%'     | like("name", "张三") ---> AND name LIKE '%张三%'  | like(User::getName, "张三") ---> AND name LIKE '%张三%' |
|orLike     | LIKE '%值%'     | orLike("name", "张三") ---> OR name LIKE '%张三%'  | orLike(User::getName, "张三") ---> OR name LIKE '%张三%' |
|notLike    | NOT LIKE '%值%' | notLike("name", "张三") ---> AND name NOT LIKE '%张三%'  | notLike(User::getName, "张三") ---> AND name NOT LIKE '%张三%' |
|orNotLike  | NOT LIKE '%值%' | orNotLike("name", "张三") ---> OR name NOT LIKE '%张三%'  | orNotLike(User::getName, "张三") ---> OR name NOT LIKE '%张三%' |
|like       | LIKE '%值%'     | like("name", "张三") ---> AND name LIKE '%张三%'  | like(User::getName, "张三") ---> AND name LIKE '%张三%' |
|orLike     | LIKE '%值%'     | orLike("name", "张三") ---> OR name LIKE '%张三%'  | orLike(User::getName, "张三") ---> OR name LIKE '%张三%' |
|notLike    | NOT LIKE '%值%' | notLike("name", "张三") ---> AND name NOT LIKE '%张三%'  | notLike(User::getName, "张三") ---> AND name NOT LIKE '%张三%' |
|orNotLike  | NOT LIKE '%值%' | orNotLike("name", "张三") ---> OR name NOT LIKE '%张三%'  | orNotLike(User::getName, "张三") ---> OR name NOT LIKE '%张三%' |
|leftLike     | LIKE '%值'     | leftLike("name", "张三") ---> AND name LIKE '%张三'  | leftLike(User::getName, "张三") ---> AND name LIKE '%张三' |
|orLeftLike   | LIKE '%值'     | orLeftLike("name", "张三") ---> OR name LIKE '%张三'  | orLeftLike(User::getName, "张三") ---> OR name LIKE '%张三' |
|notLeftLike  | NOT LIKE '%值' | notLeftLike("name", "张三") ---> AND name NOT LIKE '%张三'  | notLeftLike(User::getName, "张三") ---> AND name NOT LIKE '%张三' |
|orNotLeftLike| NOT LIKE '%值' | orNotLeftLike("name", "张三") ---> OR name NOT LIKE '%张三'  | orNotLeftLike(User::getName, "张三") ---> OR name NOT LIKE '%张三' |
|rightLike    | LIKE '值%'     | rightLike("name", "张三") ---> AND name LIKE '张三%'  | rightLike(User::getName, "张三") ---> AND name LIKE '张三%' |
|orRightLike  | LIKE '值%'     | orRightLike("name", "张三") ---> OR name LIKE '张三%'  | orRightLike(User::getName, "张三") ---> OR name LIKE '张三%' |
|notRightLike | NOT LIKE '值%' | notRightLike("name", "张三") ---> AND name NOT LIKE '张三%'  | notRightLike(User::getName, "张三") ---> AND name NOT LIKE '张三%' |
|orNotRightLike | NOT LIKE '值%' | orNotRightLike("name", "张三") ---> OR name NOT LIKE '张三%'  | orNotRightLike(User::getName, "张三") ---> OR name NOT LIKE '张三%' |
|or         |OR 嵌套| or(i -> i.eq("name", "张三").lt("age", 18)) ---> OR (name = '张三' AND age < 18) | or(i -> i.eq(User::getName, "张三").lt(User::getAge, 18)) ---> OR (name = '张三' AND age < 18) |
|and        |AND 嵌套| and(i -> i.eq("name", "张三").lt("age", 18)) ---> AND (name = '张三' AND age < 18) | and(i -> i.eq(User::getName, "张三").lt(User::getAge, 18)) ---> AND (name = '张三' AND age < 18) |

#### 查询构造器(QueryCriteria & LambdaQueryCriteria)
> 继承自条件构造器，可额外自定义查询的排序和字段内容，查询接口适用

|方法|说明|示例|lambda示例|
|---|---|---|---|
|orderBy    |排序，true=desc| orderBy("name", true) ---> ORDER BY name DESC  | orderBy(User::getName, true) ---> ORDER BY name DESC|
|orderBy    |排序，false=asc| orderBy("name") ---> ORDER BY name | orderBy(User::getName) ---> ORDER BY name |
|select     |设置查询字段| select("name", "age") ---> SELECT name,age | select(User::getName, User::getAge) ---> SELECT name,age |


##### QueryCriteria示例

```java
    List<Integer> ids = new ArrayList<Integer>(){{
        add(1);
        add(2);
        add(3);
    }};

    QueryCriteria<Person> criteria = new QueryCriteria<>();
    criteria.lt("age", 28);
    criteria.eq("created_at", new java.util.Date());
    criteria.in("id", ids);
    criteria.orderBy("age", true);
// 等价于 WHERE age < 28 AND created_at = '2023-08-05 17:31:26' AND id IN (1,2,3) ORDER BY age DESC

    QueryCriteria<Person> criteria = new QueryCriteria<>();
    criteria.select("id", "name");
    criteria.lt("age",28);
// 等价于 SELECT id,name FROM xxxx WHERE age < 28
```

##### LambdaQueryCriteria示例

```java
    List<Long> ids = new ArrayList<Long>(){{
        add(1L);
        add(2L);
        add(3L);
    }};

  LambdaQueryCriteria<UploadFile> criteria = new LambdaQueryCriteria<>();
  criteria.lt(UploadFile::getFileId, "1000");
  criteria.eq(UploadFile::getFileMd5, "b8394b15e02c50b508b3e46cc120f0f5");
  criteria.in(UploadFile::getId, ids);
  criteria.orderBy(UploadFile::getCreatedAt, true);

// 等价于  WHERE file_id < '1000' AND file_md5 = 'b8394b15e02c50b508b3e46cc120f0f5' AND id IN (1,2,3) ORDER BY created_at DESC

  LambdaQueryCriteria<UploadFile> criteria = new LambdaQueryCriteria<>();
  criteria.select(UploadFile::getFileId, UploadFile::getFileMd5);
  criteria.lt(UploadFile::getFileId, "1000");
// 等价于 SELECT file_id,file_md5 FROM b_upload_file WHERE file_id < '1000'
```

#### 更新构造器(UpdateCriteria & LambdaUpdateCriteria)
> 继承自条件构造器，可额外自定义更新的字段内容和值，更新接口适用

|方法|说明|示例|lambda示例|
|---|---|---|---|
|set    |设置更新字段| set("name", "张三") ---> set name = '张三' | set(User::getName, "张三") ---> set name = '张三' |
|setIncrement    |设置字段自增| setIncrement("age", 1) ---> set age = age + 1 | setIncrement(User::getAge, 1) ---> set age = age + 1 |
|setDecrement    |设置字段自减| setDecrement("age", 1) ---> set age = age - 1 | setDecrement(User::getAge, 1) ---> set age = age - 1|

##### UpdateCriteria示例
```java
int num = projectInfoDao.update(new UpdateCriteria<TProjectInfo>()
                .set("created_at", new Date())
                .set("updated_by", "admin3")
                .eq("project_name", "批量项目1"));
// 等价于 UPDATE xxxx SET created_at = '2023-08-05 17:31:26', updated_by = 'admin3' WHERE project_name = '批量项目1'

TProjectInfo project1 = new TProjectInfo();
project1.setCreatedAt(new Date());
project1.setUpdatedBy("admin3");
int num = projectInfoDao.update(project1, new UpdateCriteria<TProjectInfo>().eq("project_name", "批量项目1"));
// 等价于 UPDATE xxxx SET created_at = '2023-08-05 17:31:26', updated_by = 'admin3' WHERE project_name = '批量项目1'
```

##### LambdaUpdateCriteria示例
```java
int num = projectInfoDao.update(new LambdaUpdateCriteria<TProjectInfo>()
                        .set(TProjectInfo::getEnableAt, new Date())
                        .set(TProjectInfo::getUpdatedBy, "admin2")
                .eq(TProjectInfo::getProjectName, "批量项目1"));
// 等价于 UPDATE xxxx SET enable_at = '2023-08-05 17:31:26', updated_by = 'admin2' WHERE project_name = '批量项目1'

TProjectInfo project1 = new TProjectInfo();
project1.setCreatedAt(new Date());
project1.setUpdatedBy("admin3");
int num = projectInfoDao.update(project1, new LambdaUpdateCriteria<TProjectInfo>().eq(TProjectInfo::getProjectName, "批量项目1"));
// 等价于 UPDATE xxxx SET created_at = '2023-08-05 17:31:26', updated_by = 'admin3' WHERE project_name = '批量项目1'
```

<h2 id="idGen"> 6、自定义ID生成器</h2>

需要实现 IdGeneratorInterface 接口，并且声明为 Bean 供 Spring 扫描注入

### 方式一：使用@Component注册成bean

```java
package org.example.config;

import org.springframework.stereotype.Component;
import org.tinycloud.jdbc.id.IdGeneratorInterface;

@Component
public class CustomIdGeneratorInterface implements IdGeneratorInterface {

    @Override
    public Object nextId(Object entity) {
        // 可以将当前传入的class全类名来作为业务键,
        // 或者通过反射获取表名进行业务Id调用生成
        String bizKey = entity.getClass().getName();
        // 自定义ID生成策略，推荐最终返回值使用包装类
        Long id = ....;
        // 返回生成的id值即可.
        return id;
    }
}
```

### 方式二：在@Configuration配置类中注册

```java
@Bean
public CustomIdGeneratorInterface idGenerator(){
    return new CustomIdGeneratorInterface();
}
```

## 7、一些示例
创建 ProjectInfoDao
```java
package org.example.mapper;

import org.example.entity.TProjectInfo;
import org.springframework.stereotype.Repository;
import org.tinycloud.jdbc.BaseDao;

@Repository
public class ProjectInfoDao extends BaseDao<TProjectInfo, Long> {

}
```

1. 查询操作

```java
@Autowired
private ProjectDao projectDao;

// 查询所有的项目，返回列表
List<TProjectInfo> projectList = projectDao.select("select * from t_project_info order by created_at desc");

// 自定义返回类型，返回列表
List<ProjectInfoVo> projectList = projectDao.select("select * from t_project_info order by created_at desc", ProjectInfoVo.class);

// 查询所有的项目，返回列表
List<TProjectInfo> projectList = projectDao.select("select * from t_project_info order by created_at desc");

// 查询所以的项目，返回Map列表
List<Map<String, Object>>projectList = projectDao.selectMap("select * from t_project_info order by created_at desc");

// 查询id=1的项目，返回列表
List<TProjectInfo> projectList = projectDao.select("select * from t_project_info where id = ? ", 1);

// 模糊查询项目，返回列表
List<TProjectInfo> projectList = projectDao.select("select * from t_project_info where project_name like CONCAT('%', ?, '%')", "测试项目");

// 查询id=1的项目，返回对象
TProjectInfo project = projectDao.selectOne("select * from t_project_info where id = ? ", 1);

// 查询记录数
Integer count = projectDao.selectOneColumn("select count(*) from t_project_info order by created_at desc", Integer.class));

// 分页查询id>100的记录，第一页，每页10个
Page<TProjectInfo> page = projectDao.paginate("select * from t_project_info order by created_at desc where id > ?",new Page<TProjectInfo>(1,10),100));

// 查询id=3的项目信息列表
TProjectInfo project = new TProjectInfo();
project.setId(3L);
List<TProjectInfo> projectList = projectDao.select(project);

// 查询id=3的项目信息
TProjectInfo project = new TProjectInfo();
project.setId(3L);
Project project = projectDao.selectOne(project);

// 查询id=3的项目信息
TProjectInfo project=projectDao.selectById(3L);

// 查询id=3和id=4的项目信息
List<Long> ids = new ArrayList<Long>();
ids.add(3L);
ids.add(4L);
TProjectInfo project=projectDao.selectByIds(ids)

// 查询id=3的项目信息
LambdaQueryCriteria<TProjectInfo> criteria = new LambdaQueryCriteria<>().eq(Project::getId, 3L);
TProjectInfo project = projectDao.selectOne(criteria);

// 查询id=3的项目信息
QueryCriteria<TProjectInfo> criteria = new QueryCriteria<>().eq("id",3L);
TProjectInfo project = projectDao.selectOne(criteria);

// 分页查询id=3的项目信息，第一页，每页10个
TProjectInfo project = new TProjectInfo();
project.setId(3L);
Page<TProjectInfo> page = projectDao.paginate(project,new Page<TProjectInfo>(1, 10));

// 分页查询id=3的项目信息，第一页，每页10个
QueryCriteria<TProjectInfo> criteria = new QueryCriteria()<>.eq("id", 3L);
Page<TProjectInfo> page = projectDao.paginate(criteria,new Page<TProjectInfo>(1, 10));

// 根据条件构造器构建复杂查询条件
// 等价于 `SELECT id,project_name ,del_flag,created_by,updated_by,created_at AS createdAt,updated_at,remark FROM t_project_info WHERE created_by = 'admin' AND del_flag >= 0 AND id IN (1, 5) OR (remark NOT LIKE '%XXX%') ORDER BY created_at DESC`
List<Long> ids = new ArrayList<>();
ids.add(1L);
ids.add(5L);
LambdaQueryCriteria<TProjectInfo> criteria = new LambdaQueryCriteria<>()
    .eq(TProjectInfo::getCreatedBy, "admin")
    .gte(TProjectInfo::getDelFlag, 0)
    .in(TProjectInfo::getId, ids)
    .or(i -> i.notLike(TProjectInfo::getRemark, "XXX"))
    .orderBy(TProjectInfo::getCreatedAt, true);
List<TProjectInfo> projectList = projectDao.select(criteria);


// 根据条件构造器查询记录数量，等价于 `SELECT COUNT(*) FROM t_project_info WHERE id = 1695713712801116162`
LambdaQueryCriteria<TProjectInfo> criteria = new LambdaQueryCriteria<>().eq(Project::getId,1695713712801116162L);
Long count=projectDao.selectCount(criteria);

// 根据条件构造器查询记录是否存在，等价于 `SELECT COUNT(*) FROM t_project_info WHERE id = 1695713712801116162`，然后再判断结果count是否大于0
LambdaQueryCriteria<TProjectInfo> criteria = new LambdaQueryCriteria<>().eq(Project::getId,1695713712801116162L);
boolean result = projectDao.exists(criteria)

```

2. 新增操作

```java
@Autowired
private ProjectDao projectDao;

// 使用sql插入一条数据
int result = projectDao.insert("insert t_into project_info(project_name, del_flag, remark) values (?,?,?)","测试项目",1,"XXXXXXX");

// 使用实体类插入一条数据，默认忽略null
TProjectInfo project = new TProjectInfo();
project.setProjectName("xxxx");
project.setDelFlag(1);
project.setCreatedBy("admin");
project.setRemark("XXXX");
int result=projectDao.insert(project);

// 获取生成的主键id，在主键策略为assignId、uuid或objectId时适用
Long id = project.getId();

// 使用实体类插入一条数据，不忽略null
int result = projectDao.insert(project,false);

// 获取生成的主键id，在主键策略为assignId、uuid或objectId时适用
Long id = project.getId();

// 使用实体类插入一条数据，并返回数据库自增的主键值，默认忽略null
int result = projectDao.insertReturnAutoIncrement(project);

```

3. 更新操作

```java
@Autowired
private ProjectDao projectDao;

// 使用sql插入一条数据
int result = baseDao.update(""update project_info set project_name=?where id=?"",new Object[]{"测试项目",1});

// 使用实体类更新一条数据，其中以主键Id值为where条件，默认忽略null，等价于 `update t_project_info set project_name='测试项目',del_flag=1,created_by='admin',remark='XXXX' where id=1`
TProjectInfo project = new TProjectInfo();
project.setId(1);
project.setProjectName("测试项目");
project.setDelFlag(1);
project.setCreatedBy("admin");
project.setRemark("XXXX");
int result = baseDao.updateById(project);

// 使用实体类更新一条数据，不忽略null，等价于 `update t_project_info set project_name='测试项目',del_flag=1,created_by='admin',remark='XXXX',updated_by=NULL,created_at=NULL,updated_at=NULL where id=1`
int result = baseDao.updateById(project, false);

// 根据条件构造器（Lambda）作为条件更新一条数据，默认忽略null，等价于 `update t_project_info set project_name='测试项目' where id=1`
TProjectInfo project2 = new TProjectInfo();
project2.setProjectName("测试项目");
LambdaUpdateCriteria<TProjectInfo> criteria = new LambdaUpdateCriteria<>().eq(TProjectInfo::getId(), 1L)
int result=baseDao.update(project2, criteria);

// 根据实体类内容和条件构造器作为条件更新一条数据，默认忽略null，等价于 `update t_project_info set project_name='测试项目' where id=1`
UpdateCriteria<TProjectInfo> criteria = new UpdateCriteria<>().eq("id", 1L)
int result = update.update(project2, criteria);

// 根据实体类内容和条件构造器作为条件更新一条数据，不忽略null，等价于 `update t_project_info set project_name='测试项目',del_flag=NULL,created_by=NULL,remark=NULL,updated_by=NULL,created_at=NULL,updated_at=NULL where id=1`
UpdateCriteria<TProjectInfo> criteria = new UpdateCriteria<>().eq("id", 1L)
int result = update.update(project2, false, criteria);

// 只根据条件构造器作为条件更新一条数据，等价于 `update t_project_info set project_name='测试项目' where id=1`
UpdateCriteria<TProjectInfo> criteria = new UpdateCriteria<>().set("project_name", "测试项目").eq("id",1L);
int result = baseDao.update(criteria);

// 只根据条件构造器（Lambda）作为条件更新一条数据，等价于 `update t_project_info set project_name='测试项目' where id=1`
LambdaUpdateCriteria<TProjectInfo> criteria = new LambdaUpdateCriteria<>().set(TProjectInfo::getProjectName, "测试项目").eq("id",1L);
int result = baseDao.update(criteria);
```

4. 删除操作

```java
@Autowired
private ProjectDao projectDao;

// 使用sql删除一条数据
int result = projectDao.delete(""delete from t_project_info where id=?"", new Object[]{1});

// 根据实体类非null内容作为where条件删除一条数据，等价于 `delete from t_project_info where id = 1`
TProjectInfo project = new TProjectInfo();
project.setId(1);
int result = baseDao.delete(project);

// 根据主键id删除一条数据，等价于 `delete from t_project_info where id = 1`
int result = baseDao.deleteById(1L);

// 根据主键id列表删除多条数据，等价于 `delete from t_project_info where id in (1, 5)`
List<Long> ids = new ArrayList<Long>();
ids.add(1L);
ids.add(5L);
int result = baseDao.deleteByIds(ids);

// 根据主键id数组删除多条数据，等价于 `delete from t_project_info where id in (1, 5)`
List<Long> ids = new ArrayList<Long>();
int result = baseDao.deleteByIds(1L, 5L);

// 根据条件构造器（Lambda）作为查询条件删除一条数据，等价于 `delete from t_project_info where id = 1`
LambdaUpdateCriteria<TProjectInfo> criteria = new LambdaUpdateCriteria<>().eq(Project::getId(), 1L)
int result = baseDao.delete(criteria);

// 根据条件构造器作为查询条件删除一条数据，等价于 `delete from t_project_info where id = 1`
UpdateCriteria<TProjectInfo> criteria = new UpdateCriteria<>().eq("id", 1L)
int result = baseDao.delete(criteria);
``` 


## 8、安全说明
使用`QueryCriteria`和`UpdateCriteria`时应避免前端传入字段名，防止`SQL注入`的风险；
如若必须使用由前端传入的动态内容，如使用QueryCriteria.orderBy("任意前端传入字段")进行动态排序，推荐使用工具类 `SqlInjectionUtils.check(内容)` 先行验证字符串是否存在 `SQL注入`， 存在则拒绝操作。


## 9、SQL日志打印分析
**该功能依赖 p6spy 组件，需进行配置后方可使用**

**注意！**

- driver-class-name 为 p6spy 提供的驱动类 com.p6spy.engine.spy.P6SpyDriver
- url 前缀为 jdbc:p6spy 跟着冒号为对应数据库连接地址
- 该插件有性能损耗，不建议在生产环境中使用。
- 更多参考 p6spy 官方文档： https://p6spy.readthedocs.io/en/latest/index.html


1. 引入 `p6spy` 依赖

```xml
<dependency>
  <groupId>p6spy</groupId>
  <artifactId>p6spy</artifactId>
  <version>最新版本</version>
</dependency>
``` 

2. `application.yml` 配置，注意url的中间多了个p6spy
```yaml
spring:
  datasource:
    driver-class-name: com.p6spy.engine.spy.P6SpyDriver
    url: jdbc:p6spy:mysql://127.0.0.1:3306/tiny_jdbc_test?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    ...
```

3. `spy.properties` 配置

请配置 `logMessageFormat=org.tinycloud.jdbc.p6spy.P6SpyLogger`

其余根据提示信息自定义修改

```properties
#################################################################
# P6Spy Options File                                            #
# See documentation for detailed instructions                   #
# http://p6spy.github.io/p6spy/2.0/configandusage.html          #
# https://p6spy.readthedocs.io/en/latest/configandusage.html    #
#################################################################

#################################################################
# MODULES                                                       #
#                                                               #
# Module list adapts the modular functionality of P6Spy.        #
# Only modules listed are active.                               #
# (default is com.p6spy.engine.logging.P6LogFactory and         #
# com.p6spy.engine.spy.P6SpyFactory)                            #
# Please note that the core module (P6SpyFactory) can't be      #
# deactivated.                                                  #
# Unlike the other properties, activation of the changes on     #
# this one requires reload.                                     #
#################################################################
#modulelist=com.p6spy.engine.spy.P6SpyFactory,com.p6spy.engine.logging.P6LogFactory,com.p6spy.engine.outage.P6OutageFactory

################################################################
# CORE (P6SPY) PROPERTIES                                      #
################################################################

# A comma separated list of JDBC drivers to load and register.
# (default is empty)
#
# Note: This is normally only needed when using P6Spy in an
# application server environment with a JNDI data source or when
# using a JDBC driver that does not implement the JDBC 4.0 API
# (specifically automatic registration).
#driverlist=

# for flushing per statement
# (default is false)
#autoflush=false

# sets the date format using Java's SimpleDateFormat routine.
# In case property is not set, milliseconds since 1.1.1970 (unix time) is used (default is empty)
#dateformat=

# prints a stack trace for every statement logged
#stacktrace=false
# if stacktrace=true, specifies the stack trace to print
#stacktraceclass=

# determines if property file should be reloaded
# Please note: reload means forgetting all the previously set
# settings (even those set during runtime - via JMX)
# and starting with the clean table
# (default is false)
#reloadproperties=false

# determines how often should be reloaded in seconds
# (default is 60)
#reloadpropertiesinterval=60

# specifies the appender to use for logging
# Please note: reload means forgetting all the previously set
# settings (even those set during runtime - via JMX)
# and starting with the clean table
# (only the properties read from the configuration file)
# (default is com.p6spy.engine.spy.appender.FileLogger)
#appender=com.p6spy.engine.spy.appender.Slf4JLogger
appender=com.p6spy.engine.spy.appender.StdoutLogger
#appender=com.p6spy.engine.spy.appender.FileLogger

# name of logfile to use, note Windows users should make sure to use forward slashes in their pathname (e:/test/spy.log)
# (used for com.p6spy.engine.spy.appender.FileLogger only)
# (default is spy.log)
#logfile=spy.log

# append to the p6spy log file. if this is set to false the
# log file is truncated every time. (file logger only)
# (default is true)
#append=true

# class to use for formatting log messages (default is: com.p6spy.engine.spy.appender.SingleLineFormat)
logMessageFormat=org.tinycloud.jdbc.p6spy.P6SpyLogger

# Custom log message format used ONLY IF logMessageFormat is set to com.p6spy.engine.spy.appender.CustomLineFormat
# default is %(currentTime)|%(executionTime)|%(category)|connection%(connectionId)|%(sqlSingleLine)
# Available placeholders are:
#   %(connectionId)            the id of the connection
#   %(currentTime)             the current time expressing in milliseconds
#   %(executionTime)           the time in milliseconds that the operation took to complete
#   %(category)                the category of the operation
#   %(effectiveSql)            the SQL statement as submitted to the driver
#   %(effectiveSqlSingleLine)  the SQL statement as submitted to the driver, with all new lines removed
#   %(sql)                     the SQL statement with all bind variables replaced with actual values
#   %(sqlSingleLine)           the SQL statement with all bind variables replaced with actual values, with all new lines removed
#customLogMessageFormat=%(currentTime)|%(executionTime)|%(category)|connection%(connectionId)|%(sqlSingleLine)

# format that is used for logging of the java.util.Date implementations (has to be compatible with java.text.SimpleDateFormat)
# (default is yyyy-MM-dd'T'HH:mm:ss.SSSZ)
#databaseDialectDateFormat=yyyy-MM-dd'T'HH:mm:ss.SSSZ

# format that is used for logging of the java.sql.Timestamp implementations (has to be compatible with java.text.SimpleDateFormat)
# (default is yyyy-MM-dd'T'HH:mm:ss.SSSZ)
#databaseDialectTimestampFormat=yyyy-MM-dd'T'HH:mm:ss.SSSZ

# format that is used for logging booleans, possible values: boolean, numeric
# (default is boolean)
#databaseDialectBooleanFormat=boolean

# Specifies the format for logging binary data. Not applicable if excludebinary is true.
# (default is com.p6spy.engine.logging.format.HexEncodedBinaryFormat)
#databaseDialectBinaryFormat=com.p6spy.engine.logging.format.PostgreSQLBinaryFormat
#databaseDialectBinaryFormat=com.p6spy.engine.logging.format.MySQLBinaryFormat
#databaseDialectBinaryFormat=com.p6spy.engine.logging.format.HexEncodedBinaryFormat

# whether to expose options via JMX or not
# (default is true)
#jmx=true

# if exposing options via jmx (see option: jmx), what should be the prefix used?
# jmx naming pattern constructed is: com.p6spy(.<jmxPrefix>)?:name=<optionsClassName>
# please note, if there is already such a name in use it would be unregistered first (the last registered wins)
# (default is none)
#jmxPrefix=

# if set to true, the execution time will be measured in nanoseconds as opposed to milliseconds
# (default is false)
#useNanoTime=false

#################################################################
# DataSource replacement                                        #
#                                                               #
# Replace the real DataSource class in your application server  #
# configuration with the name com.p6spy.engine.spy.P6DataSource #
# (that provides also connection pooling and xa support).       #
# then add the JNDI name and class name of the real             #
# DataSource here                                               #
#                                                               #
# Values set in this item cannot be reloaded using the          #
# reloadproperties variable. Once it is loaded, it remains      #
# in memory until the application is restarted.                 #
#                                                               #
#################################################################
#realdatasource=/RealMySqlDS
#realdatasourceclass=com.mysql.jdbc.jdbc2.optional.MysqlDataSource

#################################################################
# DataSource properties                                         #
#                                                               #
# If you are using the DataSource support to intercept calls    #
# to a DataSource that requires properties for proper setup,    #
# define those properties here. Use name value pairs, separate  #
# the name and value with a semicolon, and separate the         #
# pairs with commas.                                            #
#                                                               #
# The example shown here is for mysql                           #
#                                                               #
#################################################################
#realdatasourceproperties=port;3306,serverName;myhost,databaseName;jbossdb,foo;bar

#################################################################
# JNDI DataSource lookup                                        #
#                                                               #
# If you are using the DataSource support outside of an app     #
# server, you will probably need to define the JNDI Context     #
# environment.                                                  #
#                                                               #
# If the P6Spy code will be executing inside an app server then #
# do not use these properties, and the DataSource lookup will   #
# use the naming context defined by the app server.             #
#                                                               #
# The two standard elements of the naming environment are       #
# jndicontextfactory and jndicontextproviderurl. If you need    #
# additional elements, use the jndicontextcustom property.      #
# You can define multiple properties in jndicontextcustom,      #
# in name value pairs. Separate the name and value with a       #
# semicolon, and separate the pairs with commas.                #
#                                                               #
# The example shown here is for a standalone program running on #
# a machine that is also running JBoss, so the JNDI context     #
# is configured for JBoss (3.0.4).                              #
#                                                               #
# (by default all these are empty)                              #
#################################################################
#jndicontextfactory=org.jnp.interfaces.NamingContextFactory
#jndicontextproviderurl=localhost:1099
#jndicontextcustom=java.naming.factory.url.pkgs;org.jboss.naming:org.jnp.interfaces

#jndicontextfactory=com.ibm.websphere.naming.WsnInitialContextFactory
#jndicontextproviderurl=iiop://localhost:900

################################################################
# P6 LOGGING SPECIFIC PROPERTIES                               #
################################################################

# filter what is logged
# please note this is a precondition for usage of: include/exclude/sqlexpression
# (default is false)
#filter=false

# comma separated list of strings to include
# please note that special characters escaping (used in java) has to be done for the provided regular expression
# (default is empty)
#include=
# comma separated list of strings to exclude
# (default is empty)
#exclude=

# sql expression to evaluate if using regex
# please note that special characters escaping (used in java) has to be done for the provided regular expression
# (default is empty)
#sqlexpression=

#list of categories to exclude: error, info, batch, debug, statement,
#commit, rollback, result and resultset are valid values
# (default is info,debug,result,resultset,batch)
#excludecategories=info,debug,result,resultset,batch

#whether the binary values (passed to DB or retrieved ones) should be logged with placeholder: [binary] or not.
# (default is false)
#excludebinary=false

# Execution threshold applies to the standard logging of P6Spy.
# While the standard logging logs out every statement
# regardless of its execution time, this feature puts a time
# condition on that logging. Only statements that have taken
# longer than the time specified (in milliseconds) will be
# logged. This way it is possible to see only statements that
# have exceeded some high water mark.
# This time is reloadable.
#
# executionThreshold=integer time (milliseconds)
# (default is 0)
#executionThreshold=

################################################################
# P6 OUTAGE SPECIFIC PROPERTIES                                #
################################################################
# Outage Detection
#
# This feature detects long-running statements that may be indicative of
# a database outage problem. If this feature is turned on, it will log any
# statement that surpasses the configurable time boundary during its execution.
# When this feature is enabled, no other statements are logged except the long
# running statements. The interval property is the boundary time set in seconds.
# For example, if this is set to 2, then any statement requiring at least 2
# seconds will be logged. Note that the same statement will continue to be logged
# for as long as it executes. So if the interval is set to 2, and the query takes
# 11 seconds, it will be logged 5 times (at the 2, 4, 6, 8, 10 second intervals).
#
# outagedetection=true|false
# outagedetectioninterval=integer time (seconds)
#
# (default is false)
#outagedetection=false
# (default is 60)
#outagedetectioninterval=30
```