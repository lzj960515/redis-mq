# draw-api

## Getting Start

1. 修改`application-local(test).yml`配置文件

   - 修改`spring.datasource`配置：地址、用户名、密码
   - 修改`spring.redis`配置：地址、用户名、密码

2. 启动


## 配置
- application-local.yaml 为本地开发使用配置文件
- application-test.yaml 为ci测试时使用配置文件

## 代码生成

通过代码生成器可快速的生成一套crud接口：controller，service，mapper，domain

使用方式：

1、克隆仓库[mybatis-plus-generator](https://github.com/lzj960515/kq-universal/mybatis-plus-generator)

2、依照README进行代码生成

3、将生成的代码拷贝到项目中

> 后续将做成idea插件

## Yapi生成

通过yapi生成的方式可免除既写代码又写yapi的烦恼。

使用方式：

`test`包下包含一个`YapiGeneratorTest`的类，根据实际请求修改配置，运行Main方法

> 由于yapi生成需要配套使用类似于openApi的注解，这点请参考[kq-universal-yapi-starter](https:/github.com/lzj960515/kq-universal/kq-universal-yapi-starter), 请放心，使用方式和openApi一毛一样，没有任何学习成本