## AutoFix 
>belongs to own hotfix

## Integration
### Get Gradle Plugin
1. add following to the build.gradle of your root project.
>classpath 'com.cuieney:autofix:1.1.1'

	build.gradle maybe look like this:
	
	```
	buildscript {
	    repositories {
	        jcenter()
	    }
	    dependencies {
	        classpath 'com.android.tools.build:gradle:2.3.0'
            classpath 'com.cuieney:autofix:1.1.1'
	    }
	}
	```
2. add following to your build.gradle:

	>apply plugin: "com.cuieney.autofix"

### Get AutoFix SDK

* gradle dependency:

	```	
	dependencies {
		compile 'com.cuieney.library:fix:1.1.0'
	}
	```
	
### Use AutoFix SDK
1. add following to your application class:

	```
	@Override
	protected void attachBaseContext(Context base) {
	    super.attachBaseContext(base);
	    AutoFix.init(this);
	}
	```
2. load the patch file according to your needs:

	```
	     AutoFix.applyPatch(this,patch.jar);
    
	```
	**I plan to provide the management of patch file later.**

### ProGuard
* add follwing to you proguardFile if you are using proguard:

	>-keep class cn.cuieney.fix.** { *; }
