/*
 * Copyright (C) 2016 Baidu, Inc. All Rights Reserved.
 */
package com.cuieney.autofix.utils

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.objectweb.asm.*

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * Created by jixin.jia on 15/11/10.
 */
class NuwaProcessor {


    public
    static processJar(File hashFile,Map map,File jarFile, File patchDir, HashSet<String> includePackage, HashSet<String> excludeClass) {
        if (jarFile) {

            def optJar = new File(jarFile.getParent(), jarFile.name + ".opt")
            def file = new JarFile(jarFile);
            Enumeration enumeration = file.entries();
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(optJar));

            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement();
                String entryName = jarEntry.getName();
                ZipEntry zipEntry = new ZipEntry(entryName);

                InputStream inputStream = file.getInputStream(jarEntry);
                jarOutputStream.putNextEntry(zipEntry);

                if (shouldProcessClassInJar(entryName, includePackage, excludeClass)) {
                    def bytes = referHackWhenInit(inputStream);

                    jarOutputStream.write(bytes);
                    def hash = DigestUtils.shaHex(bytes)
                    hashFile.append(AutoUtils.format(entryName, hash))
                    if (AutoUtils.notSame(map,entryName, hash)) {
                        def entryFile = new File("${patchDir}${File.separator}${entryName}")
                        entryFile.getParentFile().mkdirs()
                        if (!entryFile.exists()) {
                            entryFile.createNewFile()
                        }
                        FileUtils.writeByteArrayToFile(entryFile, bytes)
                    }
                }
                else {
                    jarOutputStream.write(IOUtils.toByteArray(inputStream));
                }
                jarOutputStream.closeEntry();
            }

            jarOutputStream.close();

            file.close();

            if (jarFile.exists()) {
                jarFile.delete()
            }

            org.apache.commons.io.FileUtils.copyFile(optJar,jarFile);

            org.apache.commons.io.FileUtils.deleteQuietly(optJar)

        }

    }


    //引用你的预设class  把他当成要打桩的文件
    public static byte[] referHackWhenInit(InputStream inputStream) {
        ClassReader cr = new ClassReader(inputStream);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        boolean hasHackSuccess = false;
        boolean isInterface = false;
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM4, cw) {
            @Override
            void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces)
                //检查类型
                isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                mv = new MethodVisitor(Opcodes.ASM4, mv) {
                    @Override
                    void visitInsn(int opcode) {
                        if (("<init>".equals(name) || "<clinit>".equals(name)) && opcode == Opcodes.RETURN && !hasHackSuccess) {
                            //  第一次尝试hack
                            NuwaProcessor.hackProcess(mv)
                            hasHackSuccess = true;
                        }
                        super.visitInsn(opcode);
                    }
                }
                return mv;
            }

        };
        cr.accept(cv, 0);
        if (!hasHackSuccess && !isInterface) {
            // has not hack and not interface
            // 第二次尝试hack  这个有问题 先关闭
            //return addCinitAndHack(cr,cw);
            return cw.toByteArray();
        } else {
            return cw.toByteArray();
        }
    }

    /**
     * add clinit and  do hack
     * @param cr
     * @param cw
     * @return
     */
    private static byte[] addCinitAndHack(ClassReader cr, ClassWriter cw) {
        MethodVisitor constructor = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        constructor.visitCode();
        hackProcess(constructor);
        constructor.visitInsn(Opcodes.RETURN);
//        constructor.visitMaxs(1 + 2, 1);
        constructor.visitEnd();
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM4, cw) {};
        cr.accept(cv, 0);
        return cw.toByteArray();
    }

    /**
     * hack class
     * @param v
     */
    public static void hackProcess(MethodVisitor v) {
        Label l1 = new Label();
        v.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Boolean", "FALSE", "Ljava/lang/Boolean;");
        v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
        v.visitJumpInsn(Opcodes.IFEQ, l1);
        v.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        v.visitLdcInsn(Type.getType("Lcom/cuieney/autofix/Auto;"));
        v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V", false);
        v.visitLabel(l1);
    }


    public static boolean shouldProcessPreDexJar(String path) {
        return path.endsWith("classes.jar") &&
                !path.contains("com.android.support") &&
                !path.contains("/android/m2repository");
    }

    private
    static boolean shouldProcessClassInJar(String entryName, HashSet<String> includePackage, HashSet<String> excludeClass) {
        return entryName.endsWith(".class") &&
                !entryName.startsWith("com/cuieney/fix/") &&
                NuwaSetUtils.isIncluded(entryName, includePackage) &&
                !NuwaSetUtils.isExcluded(entryName, excludeClass) &&
                !entryName.contains("android/support/")
    }

    //通过文件的形式对file进行打桩
    public static byte[] processClass(File file) {
        println(file.getAbsolutePath() + "oye pro")
        def optClass = new File(file.getParent(), file.name + ".opt")

        FileInputStream inputStream = new FileInputStream(file);
        FileOutputStream outputStream = new FileOutputStream(optClass)

        def bytes = referHackWhenInit(inputStream);
        outputStream.write(bytes)
        inputStream.close()
        outputStream.close()
        if (file.exists()) {
            file.delete()
        }
        optClass.renameTo(file)
        return bytes
    }
}
