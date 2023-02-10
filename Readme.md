## 消息定义

> 框架包含消息生成器 (Protostuff runtime generator for .proto files), 因此消息不需要再自己写.proto文件了
> 需要在代码中定义:

### 流程

> 编写数据结构定义的Pojo类 (或称之为DTO) 示例:

````java
/**
 * ProtobufMessage 注解定义该对象属于要生成.proto的Pojo类
 * messageType 代表类型号
 * cmd 代表命令号
 * resp true 代表服务器回复的消息 (或称之为服务端向客户端发送的消息), false 代表客户端向服务端发送的消息
 * desc 代表注释, 会写入.proto文件里 ()
 */
@ProtobufMessage(
        messageType = CoreMessageConst.Login.TYPE, 
        cmd = CoreMessageConst.Login.REQ_LOGIN,
        resp = false, 
        desc = "长连接认证消息")
public class ReqLogin {
    @ProtoDesc("通过Http请求换取的Token")
    public String token;
    @ProtoDesc("标记玩家是否已经重新链接")
    public boolean isReconnect;
    @ProtoDesc("用户已选择的玩家ID")
    public int playerId;
}
````
> 使用生成工具 org.alan.gen.ToOneFile 该工具带有3个参数
1. 参数1: 需要扫描的包全名, 可以用分号分割代表多个包名.
2. 参数2: 需要产出的.proto文件的绝对路径
3. 参数3: 固定为 org.alan.mars.protostuff.ProtobufMessage
4. 示例: java org.alan.gen.ToOneFile org.alan.mars.message;org.alan.mars.tips;com.xiaoxi.game.core.message
   F:\work\gameframework-core\ThirdTools\protoToCs\proto\core.proto
   org.alan.mars.protostuff.ProtobufMessage
> 生成的文件信息
````protobuf
//长连接认证消息 | 1000-4 | ReqLogin
message ReqLogin {
  optional string token = 1;  //通过Http请求换取的Token
  optional bool isReconnect = 2;  //标记玩家是否已经重新链接
  optional string deviceType = 3;  //登陆设备名称
  optional string deviceOS = 4;  //登陆系统和版本
}
````
### 消息命名规范
1. 请求响应类型: 请求消息(客户端发送给服务器的消息) 消息名称必须Req开头. 对应的响应消息 (服务器发送给客户端的消息)名称必须以Resp开头. 并且: 响应消息命令号 = 请求消息号 + 1
   > 例如: ReqLogin RespLogin
2. 服务器推送类型:  由服务器主动推送给客户端的消息. 比如公告信息等. 必须以 Push 开头
   > 例如: PushNotice
3. 客户端推送类型: 由客户端主动推送给服务器, 且并不期望服务器有回复的消息. 比如消除红点, 聊天, 发送错误日志等消息. 必须以Send开头
   > 例如: SendErrLog

### 消息监听事件Handler
````java

@Component //声明为 Spring 管理的Bean
@MessageType(CoreMessageConst.Login.TYPE) //声明需要监听的消息类型. 注意同一种消息类型只能由一个Handler来监听.
public class CertifyHandler {
   /**
    * 命令号处理方法
    * @param session 非必填 该对象可以获取到发送消息的用户的会话
    * @param req 请求的序列化对象. 该对象已经实例化好了可以直接使用
    * @return 如果方法为void代表该方法不需要服务器响应 否则就是服务端响应的内容
    */
    @Command(CoreMessageConst.Login.REQ_LOGIN)
    public RespLogin login(PFSession session, ReqLogin req){
        // ... some logic
       return new RespLogin();
    }
    
}
````