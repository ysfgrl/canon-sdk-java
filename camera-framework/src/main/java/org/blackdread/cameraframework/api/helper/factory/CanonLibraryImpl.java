package org.blackdread.cameraframework.api.helper.factory;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import org.blackdread.camerabinding.jna.EdsdkLibrary;
import org.blackdread.cameraframework.api.CanonLibrary;
import org.blackdread.cameraframework.util.DllUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Optional;

/**
 * <p>Created on 2018/10/16.<p>
 *
 * @author Yoann CAPLAIN
 */
@NotThreadSafe
class CanonLibraryImpl implements CanonLibrary {

    private static final Logger log = LoggerFactory.getLogger(CanonLibraryImpl.class);

    private static final String JNA_PATH_PROPERTY = "jna.library.path";

    /*
     * Library needed to forward windows messages
     * TODO Not added for now, was part of previous project but not sure is necessary
     */
//    private static final User32 user32Lib;

    // fields are static but will change to instance fields

    static {
        final CodeSource codeSource = CanonLibraryImpl.class.getProtectionDomain().getCodeSource();
        try {
            final File tmp = new File(codeSource.getLocation().toURI().getPath());
            jarFile = tmp;
            log.info("Jar file is in {}, folder {}", tmp.getPath(), tmp.getParentFile().getPath());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to build jar file path");
        }
    }

    /**
     * no used yet, might be necessary in getLibPath()
     */
    private static final File jarFile;
    /**
     * no used yet, might be necessary in getLibPath()
     */
    private static final File jarDir = jarFile.getParentFile();

    /**
     * Instance of library
     */
    private EdsdkLibrary EDSDK = null;

    private Thread shutdownHookThread = null;

    private ArchLibrary archLibraryToUse = ArchLibrary.AUTO;

    CanonLibraryImpl() {
    }

    @Override
    public EdsdkLibrary edsdkLibrary() {
        initLibrary();
        return EDSDK;
    }

    @Override
    public ArchLibrary getArchLibraryToUse() {
        return archLibraryToUse;
    }

    @Override
    public void setArchLibraryToUse(final ArchLibrary archLibraryToUse) {
        this.archLibraryToUse = archLibraryToUse;
    }

    protected Optional<String> getLibPath() {
        final String jnaPath = System.getProperty(JNA_PATH_PROPERTY);
        if (jnaPath != null) {
            // user has specified himself the path, we follow what he gave
            return Optional.of(jnaPath);
        }

        switch (archLibraryToUse) {
            case AUTO:
                // is64Bit() already checks java runtime with "sun.arch.data.model" for 32 or 64
                if (Platform.is64Bit())
                    return Optional.of(DllUtil.DEFAULT_LIB_64_PATH);
                return Optional.of(DllUtil.DEFAULT_LIB_32_PATH);
            case FORCE_32:
                return Optional.of(DllUtil.DEFAULT_LIB_32_PATH);
            case FORCE_64:
                return Optional.of(DllUtil.DEFAULT_LIB_64_PATH);
            default:
                throw new IllegalStateException("Enum unknown: " + archLibraryToUse);
        }
    }

    protected void initLibrary() {
        if (EDSDK == null) {
            final String libPath = getLibPath()
                .orElseThrow(() -> new IllegalStateException("Could not init library, lib path not found"));
            if (Platform.isWindows()) {
                // no options for now
                EDSDK = Native.loadLibrary(libPath, EdsdkLibrary.class, new HashMap<>());
                registerCanonShutdownHook();
            }
            throw new IllegalStateException("Not supported OS");
        }
    }

    private void registerCanonShutdownHook() {
        final EdsdkLibrary edsdk = EDSDK;
        final Thread shutdownThread = new Thread(() ->
        {
            if (edsdk != null)
                edsdk.EdsTerminateSDK();
        });
        if (shutdownHookThread != null) {
            Runtime.getRuntime().removeShutdownHook(shutdownHookThread);
            shutdownHookThread.interrupt();
        }
        shutdownHookThread = shutdownThread;
        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }
}
