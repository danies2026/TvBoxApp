# 保留空文件，便于后续添加混淆规则

# Rhino（drpy / spider JS 运行时）相关类
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.** { *; }
-keep class org.mozilla.javascript.tools.debugger.** { *; }

# 数据模型（Gson 反射）
-keep class com.example.tvapp.data.model.** { *; }
