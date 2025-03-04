+++
pre = "<b>3.2. </b>"
title = "分布式事务"
weight = 2
chapter = true
+++

## 定义

事务四个特性 ACID（原子性、一致性、隔离性、持久性）。

- 原子性（Atomicity）指事务作为整体来执行，要么全部执行，要么全不执行；
- 一致性（Consistency）指事务应确保数据从一个一致的状态转变为另一个一致的状态；
- 隔离性（Isolation）指多个事务并发执行时，一个事务的执行不应影响其他事务的执行；
- 持久性（Durability）指已提交的事务修改数据会被持久保存。

在分布式的场景下，一个事务内，包含对多个数据节点的操作，分布式事务保证了在分布式场景下 ACID 的特性。

## 原理介绍

ShardingSphere 对外提供 begin/commit/rollback 传统事务接口，通过 LOCAL，XA，BASE 三种模式提供了分布式事务的能力，

### LOCAL 事务

LOCAL 模式基于 ShardingSphere 代理的数据库 `begin/commit/rolllback` 的接口实现，
对于一条逻辑 SQL，ShardingSphere 通过 `begin` 指令在每个被代理的数据库开启事务，并执行实际 SQL，并执行 `commit/rollback`。
由于每个数据节点各自管理自己的事务，它们之间没有协调以及通信的能力，也并不互相知晓其他数据节点事务的成功与否。
在性能方面无任何损耗，但在强一致性以及最终一致性方面不能够保证。

### XA 事务

XA 事务采用的是 X/OPEN 组织所定义的 [DTP 模型](http://pubs.opengroup.org/onlinepubs/009680699/toc.pdf) 所抽象的 AP（应用程序）, TM（事务管理器）和 RM（资源管理器） 概念来保证分布式事务的强一致性。
其中 TM 与 RM 间采用 XA 的协议进行双向通信，通过两阶段提交实现。
与传统的本地事务相比，XA 事务增加了准备阶段，数据库除了被动接受提交指令外，还可以反向通知调用方事务是否可以被提交。
`TM` 可以收集所有分支事务的准备结果，并于最后进行原子提交，以保证事务的强一致性。

![两阶段提交模型](https://shardingsphere.apache.org/document/current/img/transaction/overview.png)

XA 事务建立在 ShardingSphere 代理的数据库 xa start/end/prepare/commit/rollback/recover 的接口上。

对于一条逻辑 SQL，ShardingSphere 通过 `xa begin` 指令在每个被代理的数据库开启事务，内部集成 TM，用于协调各分支事务，并执行 `xa commit/rollback`。

基于 XA 协议实现的分布式事务，由于在执行的过程中需要对所需资源进行锁定，它更加适用于执行时间确定的短事务。
对于长事务来说，整个事务进行期间对数据的独占，将会对并发场景下的性能产生一定的影响。

### BASE 事务

如果将实现了 ACID 的事务要素的事务称为刚性事务的话，那么基于 BASE 事务要素的事务则称为柔性事务。
BASE 是基本可用、柔性状态和最终一致性这三个要素的缩写。

- 基本可用（Basically Available）保证分布式事务参与方不一定同时在线；
- 柔性状态（Soft state）则允许系统状态更新有一定的延时，这个延时对客户来说不一定能够察觉；
- 最终一致性（Eventually consistent）通常是通过消息传递的方式保证系统的最终一致性。

在 ACID 事务中对隔离性的要求很高，在事务执行过程中，必须将所有的资源锁定。
柔性事务的理念则是通过业务逻辑将互斥锁操作从资源层面上移至业务层面。
通过放宽对强一致性要求，来换取系统吞吐量的提升。

基于 ACID 的强一致性事务和基于 BASE 的最终一致性事务都不是银弹，只有在最适合的场景中才能发挥它们的最大长处。
Apache ShardingSphere 集成了 SEATA 作为柔性事务的使用方案。
可通过下表详细对比它们之间的区别，以帮助开发者进行技术选型。

|          | *LOCAL*       | *XA*              | *BASE*     |
| -------- | ------------- | ---------------- | ------------ |
| 业务改造  | 无             | 无               | 需要 seata server|
| 一致性    | 不支持         | 支持             | 最终一致       |
| 隔离性    | 不支持         | 支持             | 业务方保证     |
| 并发性能  | 无影响         | 严重衰退          | 略微衰退       |
| 适合场景  | 业务方处理不一致 | 短事务 & 低并发   | 长事务 & 高并发 |

## 相关参考

- [分布式事务的 YAML 配置](/cn/user-manual/shardingsphere-jdbc/yaml-config/rules/transaction/)
