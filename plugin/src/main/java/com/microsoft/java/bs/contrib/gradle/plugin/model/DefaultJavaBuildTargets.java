package com.microsoft.java.bs.contrib.gradle.plugin.model;

import java.io.Serializable;
import java.util.List;

import com.microsoft.java.bs.contrib.gradle.model.JavaBuildTargets;
import com.microsoft.java.bs.contrib.gradle.model.JavaBuildTarget;

public class DefaultJavaBuildTargets implements JavaBuildTargets, Serializable {
    private static final long serialVersionUID = 1L;

    private List<JavaBuildTarget> javaBuildTargets;

    public List<JavaBuildTarget> getJavaBuildTargets() {
        return javaBuildTargets;
    }

    public void setJavaBuildTargets(List<JavaBuildTarget> javaBuildTargets) {
        this.javaBuildTargets = javaBuildTargets;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((javaBuildTargets == null) ? 0 : javaBuildTargets.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefaultJavaBuildTargets other = (DefaultJavaBuildTargets) obj;
        if (javaBuildTargets == null) {
            if (other.javaBuildTargets != null)
                return false;
        } else if (!javaBuildTargets.equals(other.javaBuildTargets))
            return false;
        return true;
    }

}
