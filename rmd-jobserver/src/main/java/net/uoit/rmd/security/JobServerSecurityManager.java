package net.uoit.rmd.security;

import net.uoit.rmd.Client;

import java.security.Permission;

/**
 * Provides security on a per-thread basis to sandbox jobs.
 * Only the class that executes jobs has the right to
 * enable and disable the security.
 */
public class JobServerSecurityManager extends SecurityManager {

    private final ThreadLocal<Boolean> flag = new ThreadLocal<Boolean>() {

        @Override
        protected Boolean initialValue() {
            return false;
        }

        @Override
        public void set(final Boolean value) {
            final SecurityManager securityManager = System.getSecurityManager();


            if (securityManager != null) {
                final Class[] classes = getClassContext();

                if (classes.length <= 4 || classes[4] != Client.class) {
                    throw new SecurityException("Job Thread cannot enable/disable security");
                }
            }

            super.set(value);
        }
    };

    @Override
    public void checkPermission(final Permission permission) {
        if (flag.get() && !"suppressAccessChecks".equals(permission.getName())) {
            super.checkPermission(permission);
        }
    }

    @Override
    public void checkPermission(final Permission permission, final Object context) {
        if (flag.get() && !"suppressAccessChecks".equals(permission.getName())) {
            super.checkPermission(permission, context);
        }
    }

    @Override
    public void checkCreateClassLoader() {
        // override to allow access
    }

    @Override
    public void checkPackageAccess(final String pkg) {
        // override to allow
    }

    public void setSecurity(final boolean enable) {
        flag.set(enable);
    }
}
