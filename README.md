## AutoFix 
>目前项目支持静态修复功能需要重启，集成简单，使用方便。

### 原理
这两篇文章就够了

[DexClassLoader热修复的入门到放弃](https://juejin.im/post/5951d5265188250d8f602225)

[手把手教你写热修复（HOTFIX）](https://juejin.im/post/595d02d5f265da6c375a90bf)

## 集成
### Get Gradle Plugin
1. 根据以下操作把代码填到你项目根目录的gradle中
>classpath 'com.cuieney:autofix:1.1.1'

	build.gradle maybe look like this:
	
	```
	buildscript {
	    repositories {
	        jcenter()
	    }
	    dependencies {
	        classpath 'com.android.tools.build:gradle:2.3.0'
            classpath 'com.cuieney.autofix:gradle:1.1.7'
	    }
	}
	```
2. 在你的build.gradle:中添加这样的代码块

	>apply plugin: "com.cuieney.autofix"

### Get AutoFix SDK

* gradle dependency:

	```	
	dependencies {
		compile 'com.cuieney.library:fix:1.1.2'
	}
	```
	
### 使用步骤
1. 在你的application中添加一下代码初始化:

	```
	@Override
	protected void attachBaseContext(Context base) {
	    super.attachBaseContext(base);
	    AutoFix.init(this);
	}
	```
2. 如果你需要加载补丁的话可以这样，每次需需要重启:

	```
	     AutoFix.applyPatch(this,patch.jar);
    
	```

### ProGuard
* 添加到你的proguardFile文件中:

	>-keep class cn.cuieney.fix.** { *; }
	
## 补丁制作
根据下面三步即可以完成补丁的制作

1.首选你的添加`auto_fix`extension到你的build.gradle中，只在你需要制作补丁的时候才用得到（不制作补丁注释即可）

```
auto_fix {
    lastVersion = '1'//顾名思义，上次一的版本号（就是说你当前Version是1，出现bug了，你把versioncode变成了2）然后这个lastVersion就填1
}

```

2.现在你已经改好bug了，需要获取相应的patch.jar把这个补丁打到app中，只需要编译一下项目即可，buildApk这个操作也是可以的

3.获取补丁push到手机中（如果项目已上线即push到服务器）


![Screen Shot 2017-07-05 at 8.20.17 PM.png](http://upload-images.jianshu.io/upload_images/3415839-1a5eb39c9d2f0ad8.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

这个就是你的补丁对应的目录，patch.jar就是补丁包（version `2`）代表你生成补丁当前的版本号

### TODO
- 增强兼容性
- 支持runtime
- 增加插件化跳转Act

### 补充
你会看到AutoFix SDK里面有这样一个类DynamicApk，这是一个beta版。用于插件化的工具。目前focus获取apk的资源文件，目前接口如下：
- getStringFromApk
- getBitmapFromApk
- getDrawableFromApk
- getMipmapFromApk
- getLayoutFromApk
- getColorFromApk
- getDimenFromApk

参数都一样(Context context, String apkPath, String name)，只介绍最后一个参数such as（R.drawable.thumb）这个name就是thumb


#### 问题
发现bug或好的建议欢迎 [issues](https://github.com/Cuieney/AutoFix/issues) or 
Email <cuieney@163.com> 
### License
F**K License

