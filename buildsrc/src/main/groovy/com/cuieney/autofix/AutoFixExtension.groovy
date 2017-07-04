/*
 * Copyright (C) 2016 Baidu, Inc. All Rights Reserved.
 */
package com.cuieney.autofix

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.tasks.Input
@CompileStatic
class AutoFixExtension {

    @Input
    HashSet<String> includePackage = [];

    @Input
    HashSet<String> excludePackage = [];

    @Input
    HashSet<String> excludeClass = [];

    @Input
    String lastVersion

    public static AutoFixExtension getConfig(Project project) {
        AutoFixExtension config =
                project.getExtensions().findByType(AutoFixExtension.class);
        if (config == null) {
            config = new AutoFixExtension();
        }
        return config;
    }


}
