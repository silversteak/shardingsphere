+++
pre = "<b>3.3. </b>"
title = "读写分离"
weight = 3
chapter = true
+++

## 定义

读写分离也就是将数据库拆分为主库和从库，即主库负责处理事务性的增删改操作，从库负责处理查询操作的数据库架构。

## 对系统的影响

用户的系统中可能存在着复杂的主从关系数据库集群，因此应用程序需要接入多个数据源，这种方式就增加了系统维护的成本和业务开发的难度。ShardingSphere 通过读写分离功能，可以让用户像使用一个数据库一样去使用数据库集群，透明化读写分离带来的影响。

## 原理介绍

ShardingSphere 的读写分离主要依赖内核的相关功能。包括解析引擎和路由引擎。解析引擎将用户的 SQL 转化为 ShardingSphere 可以识别的 Statement 信息，路由引擎根据 SQL 的读写类型以及事务的状态来做 SQL 的路由。
在从库的路由中支持多种负载均衡算法，包括轮询算法、随机访问算法、权重访问算法等，用户也可以依据 SPI 机制自行扩展所需算法。如下图所示，ShardingSphere 识别到读操作和写操作，分别会路由至不同的数据库实例。

![原理介绍](https://shardingsphere.apache.org/document/current/img/readwrite-splitting/background.png)

## 相关参考

[Java API](/cn/user-manual/shardingsphere-jdbc/java-api/rules/readwrite-splitting)\
[YAML 配置](/cn/user-manual/shardingsphere-jdbc/yaml-config/rules/readwrite-splitting)\
[Spring Boot Starter](/cn/user-manual/shardingsphere-jdbc/spring-boot-starter/rules/readwrite-splitting)\
[Spring 命名空间](/cn/user-manual/shardingsphere-jdbc/spring-namespace/rules/readwrite-splitting)
