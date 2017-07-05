/*
 * Copyright (C) 2016 Baidu, Inc. All Rights Reserved.
 */
package com.cuieney.autofix

import com.android.SdkConstants
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.transforms.ProGuardTransform
import com.cuieney.autofix.utils.AutoUtils
import com.cuieney.autofix.utils.NuwaProcessor
import com.cuieney.autofix.utils.NuwaSetUtils
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.DefaultDomainObjectSet
import proguard.gradle.ProGuardTask

class AutoFix implements Plugin<Project> {
    public static final String EXTENSION_NAME = "auto_fix";
    private static final String MAPPING_TXT = "mapping.txt"
    private static final String HASH_TXT = "hash.txt"

    public static AutoFixExtension autoConfig
    @Override
    public void apply(Project project) {
        DefaultDomainObjectSet<ApplicationVariant> variants
        if (project.getPlugins().hasPlugin(AppPlugin)) {
            variants = project.android.applicationVariants;

            project.extensions.create(EXTENSION_NAME, AutoFixExtension);
            applyTask(project, variants);
        }
    }

    private void applyTask(Project project, DomainObjectCollection<BaseVariant> variants) {

        project.afterEvaluate {

            autoConfig = AutoFixExtension.getConfig(project);

            def includePackage = autoConfig.includePackage
            def excludeClass = autoConfig.excludeClass

            variants.all { variant ->
                if (!variant.getBuildType().isMinifyEnabled()) {
                    println("不支持不开混淆的情况")
                    return;
                }

                //获取各种task
                def preDexTask = project.tasks.findByName(AutoUtils.getPreDexTaskName(project, variant))
                def dexTask = project.tasks.findByName(AutoUtils.getDexTaskName(project, variant))
                println(preDexTask + " preDexTask")
                def manifestFile = variant.outputs.processManifest.manifestOutputFile[0]


                Map hashMap = applyMapping(project, variant)
                //创建所需要的文件包
                def autoFixRootDir = new File("${project.projectDir}${File.separator}autofix${File.separator}version" + variant.getVersionCode())//project/autofix/version11
                def outputDir = new File("${autoFixRootDir}${File.separator}${dirName}")//project/autofix/version11/debug
                def patchDir = new File("${outputDir}${File.separator}patch")//project/autofix/autofix/debug/patch
                def hashFile = new File(outputDir, "${HASH_TXT}")//project/autofix/autofix/debug/hash.txt

                if (!autoFixRootDir.exists()) {
                    autoFixRootDir.mkdirs();
                }
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                }
                if (!patchDir.exists()) {
                    patchDir.mkdirs();
                }

                //制作patch补丁包
                def autoPatchTaskName = "applyAuto${variant.name.capitalize()}Patch"
                project.task(autoPatchTaskName) << {
                    if (patchDir) {
                        AutoUtils.makeDex(project, patchDir)
                    }
                }
                def autoPatchTask = project.tasks[autoPatchTaskName]

                //初始化一些工作添加不需要打桩的class文件和hashfile
                Closure prepareClosure = {
                    if (autoConfig.excludeClass == null) {
                        autoConfig.excludeClass = Sets.newHashSet();
                    }
                    def applicationClassName = AutoUtils.getApplication(manifestFile);

                    if (applicationClassName != null) {
                        applicationClassName = applicationClassName.replace(".", "/") + SdkConstants.DOT_CLASS
                        autoConfig.excludeClass.add(applicationClassName)
                    }

                    if (autoConfig.excludePackage == null) {
                        autoConfig.excludePackage = Sets.newHashSet();
                    }
                    //添加不打桩的文件目录
                    autoConfig.excludePackage.add("android/support/")

                    outputDir.mkdirs()
                    if (!hashFile.exists()) {
                        hashFile.createNewFile()
                    } else {
                        hashFile.delete()
                        hashFile.createNewFile()
                    }
                }


                //打桩等一些工作
                def autoJarBeforeDex = "autoJarBeforeDex${variant.name.capitalize()}"
                project.task(autoJarBeforeDex) << {
                    //获取build/intermediates/下的文件
                    Set<File> inputFiles = AutoUtils.getDexTaskInputFiles(project, variant, dexTask)
                    inputFiles.each { inputFile ->
                        def path = inputFile.absolutePath
                        if (path.endsWith(SdkConstants.DOT_JAR)) {
                            //对jar包进行打桩
                            NuwaProcessor.processJar(hashFile,hashMap,inputFile, patchDir, includePackage, excludeClass)
                        } else if (inputFile.isDirectory()) {
                            //intermediates/classes/debug 目录下面需要打桩的class
                            def extensions = [SdkConstants.EXT_CLASS] as String[]
                            //过滤不需要打桩的文件class
                            def inputClasses = FileUtils.listFiles(inputFile, extensions, true);
                            inputClasses.each {
                                inputClassFile ->
                                    def classPath = inputClassFile.absolutePath
                                    //过滤R文件和config文件
                                    if (classPath.endsWith(".class") && !classPath.contains("/R\$") && !classPath.endsWith("/R.class") && !classPath.endsWith("/BuildConfig.class")) {
                                        //引用nuwa而来的
                                        if (NuwaSetUtils.isIncluded(classPath, includePackage)) {
                                            if (!NuwaSetUtils.isExcluded(classPath, excludeClass)) {
                                                def bytes = NuwaProcessor.processClass(inputClassFile)

                                                if ("\\".equals(File.separator)) {
                                                    classPath = classPath.split("${dirName}\\\\")[1]
                                                } else {
                                                    classPath = classPath.split("${dirName}/")[1]
                                                }
                                                def hash = DigestUtils.shaHex(bytes)
                                                hashFile.append(AutoUtils.format(classPath, hash))
                                                //根据hash值来判断当前文件是否为差异文件需要做成patch吗？
                                                if (AutoUtils.notSame(hashMap,classPath, hash)) {
                                                    def file = new File("${patchDir}${File.separator}${classPath}")
                                                    file.getParentFile().mkdirs()
                                                    if (!file.exists()) {
                                                        file.createNewFile()
                                                    }
                                                    FileUtils.writeByteArrayToFile(file, bytes)
                                                }
                                            }

                                        }
                                    }

                            }
                        }
                    }
                }


                def autoJarBeforeDexTask = project.tasks[autoJarBeforeDex]

                autoJarBeforeDexTask.dependsOn dexTask.taskDependencies.getDependencies(dexTask)
                autoJarBeforeDexTask.doFirst(prepareClosure)
                autoPatchTask.dependsOn autoJarBeforeDexTask
                dexTask.dependsOn autoPatchTask
                //第一个是autoJarBeforeDexTask依赖dexTask依赖的任务（就是说dexTask之前的一个任务）
                //第二行和第三行autoJarBeforeDexTask这个任务执行前和执行后的的任务
                //第三行是autoPatchTask这个任务依赖于autoJarBeforeDexTask（autoJarBeforeDexTask结束了才能到autoPatchTask）


            }
        }
    }

    private static Map applyMapping(Project project, BaseVariant variant) {

        Map hashMap
        AutoFixExtension autoConfig = AutoFixExtension.getConfig(project);
        if (autoConfig.lastVersion != null) {

            def preVersionPath = new File("${project.projectDir}${File.separator}autofix${File.separator}version" + autoConfig.lastVersion)
            if (preVersionPath.exists()) {
                def hashFile = new File("${preVersionPath}${File.separator}${variant.dirName}${File.separator}${HASH_TXT}")
                hashMap = AutoUtils.parseMap(hashFile)
            }
            return hashMap;
        }
    }



}
