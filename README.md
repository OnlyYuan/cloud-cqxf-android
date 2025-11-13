# cloud-cqxf-android

消防-andorid

```agsl
Activity/Other Component calls startService(Intent)
                                |
                                v
                          Service is created
                                |
                                v
                          onCreate() <--- 只调用一次
                                |
                                v
                          onStartCommand() <--- 每次startService()都会调用
                                |
                                | (Service is running...)
                                |
        Activity/Other Component calls `stopSelf()` or `stopService()`
                                |
                                v
                          onDestroy() <--- 服务终止
```