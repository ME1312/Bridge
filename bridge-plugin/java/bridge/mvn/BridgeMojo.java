package bridge.mvn;

import bridge.asm.HierarchicalWriter;
import bridge.asm.HierarchyScanner;
import bridge.asm.TypeMap;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static bridge.mvn.ForkVisitor.*;

@Mojo(name = "bridge", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE, requiresDependencyCollection = ResolutionScope.COMPILE)
public final class BridgeMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(alias = "classpath", property = "bridge.classpath", defaultValue = "${project.build.outputDirectory}")
    private File classpath;

    @Parameter(alias = "includes", property = "bridge.includes", defaultValue = "**/*.class")
    private String[] includes;

    @Parameter(alias = "excludes", property = "bridge.excludes")
    private String[] excludes;

    @Parameter(alias = "flags", property = "bridge.flags")
    private String[] flags;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        int flags = 0;
        boolean lazy = true;
        for (String flag : this.flags) {
            switch (flag.replaceAll("[\\s\\-]", "_").toUpperCase(Locale.ROOT)) {
                case "NO_DEBUG":
                    flags |= NO_NAMED_LOCALS | NO_SOURCE_EXT | NO_SOURCE_NAMES | NO_MODULE_VERSIONS | NO_LINE_NUMBERS;
                    break;
                case "NO_NAMED_LOCALS":
                    flags |= NO_NAMED_LOCALS;
                    break;
                case "NO_SOURCE":
                    flags |= NO_SOURCE_EXT | NO_SOURCE_NAMES;
                    break;
                case "NO_SOURCE_NAMES":
                case "NO_SOURCE_NAME":
                    flags |= NO_SOURCE_NAMES;
                    break;
                case "NO_SOURCE_EXT":
                    flags |= NO_SOURCE_EXT;
                    break;
                case "NO_MODULE_VERSION":
                case "NO_MODULE_VERSIONS":
                    flags |= NO_MODULE_VERSIONS;
                    break;
                case "NO_LINE_NUMBERS":
                    flags |= NO_LINE_NUMBERS;
                    break;
                case "FORCE_COMPILE":
                case "FORCE_RECOMPILE":
                    lazy = false;
                    break;
                case "NO_COMPILE":
                case "NO_RECOMPILE":
                case "SKIP_COMPILE":
                case "SKIP_RECOMPILE":
                    log.warn("Skipped previously defined recompilation goal");
                    return;
                default:
                    log.warn("Unknown recompilation flag: " + flag);
            }
        }
        TypeMap types = new TypeMap();
        try {
            Set<String> unique = new HashSet<>();
            DirectoryScanner scan = new DirectoryScanner();
            String[] includes = new String[this.includes.length + 1];
            System.arraycopy(this.includes, 0, includes, 1, this.includes.length);
            includes[0] = "**/*.class";
            scan.setBasedir(classpath);
            scan.setIncludes(includes);
            scan.scan();
            includes = scan.getIncludedFiles();
            final long timestamp = classpath.lastModified();
            if (lazy && includes.length != 0) for (int i = 0;;) {
                File file = new File(classpath, includes[i]);
                if (file.exists() && file.lastModified() >= timestamp) break;
                if (++i == includes.length) {
                    log.info("Nothing to recompile");
                    return;
                }
            }

            log.info("Resolving class hierarchy...");
            long scantime = System.nanoTime();
            for (String path : includes) {
                File file = new File(classpath, path);
                if (file.exists()) {
                    if (log.isDebugEnabled()) log.debug(" + " + file);
                    unique.add(path.replace(File.separatorChar, '/'));
                    try (FileInputStream fis = new FileInputStream(file)) {
                        new ClassReader(fis).accept(new HierarchyVisitor(types), ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
                    }
                }
            }

            for (Artifact artifact : project.getArtifacts()) {
                File file = artifact.getFile();
                if (file.getName().endsWith(".jar") && file.exists()) {
                    log.info("  + " + file + " (" + artifact.getScope() + ')');
                    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
                        for (ZipEntry entry; (entry = zis.getNextEntry()) != null;) {
                            if (entry.getName().endsWith(".class") && unique.add(entry.getName())) {
                                new ClassReader(zis).accept(new HierarchyScanner(types), ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
                            }
                        }
                    }
                }
            }

            scantime = System.nanoTime() - scantime;
            log.info("");
            log.info("Building bridges...");
            scan = new DirectoryScanner();
            scan.setBasedir(classpath);
            scan.setIncludes(this.includes);
            scan.setExcludes(this.excludes);
            scan.scan();
            long comptime = System.nanoTime();
            for (String path : scan.getIncludedFiles()) {
                File in = new File(classpath, path);
                if (in.exists()) {
                    path = path.replace(File.separatorChar, '/');
                    if (log.isDebugEnabled()) log.debug("<- " + path);
                    try (FileInputStream is = new FileInputStream(in)) {
                        ClassNode code;
                        BridgeVisitor visitor;
                        new ClassReader(is).accept(visitor = new BridgeVisitor(code = new ClassNode(), types), ClassReader.EXPAND_FRAMES);
                        is.close();
                        int i;
                        if (visitor.bridges != 0 || visitor.invocations != 0 || visitor.adjustments != 0 || visitor.forks.size() != 1) {
                            StringBuilder msg = new StringBuilder(" -> ").append(
                                    ((i = path.lastIndexOf(visitor.name)) != -1 && path.indexOf('/', i + visitor.name.length()) == -1)?
                                            path.substring(0, i) + visitor.name.replace('/', '.') : path
                            );
                            if ((i = visitor.forks.size()) != 1) msg.append("  +").append(i - 1).append(" fork").append((i == 2)?"":"s");
                            if (visitor.bridges != 0) msg.append("  +").append(visitor.bridges).append(" bridge").append((visitor.bridges == 1)?"":"s");
                            if (visitor.invocations != 0) msg.append("  +").append(visitor.invocations).append(" invocation").append((visitor.invocations == 1)?"":"s");
                            if (visitor.adjustments != 0) msg.append("  +").append(visitor.adjustments).append(" adjustment").append((visitor.adjustments == 1)?"":"s");
                            log.info(msg.toString());
                        }
                        File out;
                        HierarchicalWriter writer;
                        for (Map.Entry<Integer, Boolean> e : visitor.forks.entrySet()) {
                            code.accept(new ForkVisitor(
                                    writer = new HierarchicalWriter(types, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS),
                                    visitor,
                                    i = e.getKey(),
                                    flags
                            ));
                            (out = ((e.getValue())? new File(classpath, "META-INF/versions/" + i + "/" + visitor.name + ".class") : in)).getParentFile().mkdirs();
                            try (FileOutputStream os = new FileOutputStream(out)) {
                                os.write(writer.toByteArray());
                            }
                        }
                    }
                }
            }

            comptime = System.nanoTime() - comptime;
            log.info("");
            log.info("Hierarchy resolved in " + humanize(scantime));
            log.info("Recompiled in " + humanize(comptime));
            classpath.setLastModified((Instant.now().getEpochSecond() * 1000) + 1000);
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }
    }

    private static String humanize(long timing) {
        if (timing < 1000000L) {
            return timing + "ns";
        } else if (timing < 1000000000L) {
            return String.format("%.2fms", timing / 1000000D);
        } else if (timing < 60000000000L) {
            return String.format("%.4fs", timing / 1000000000D);
        } else if (timing < 3600000000000L) {
            return String.format("%.4fm", timing / 60000000000D);
        } else if (timing < 86400000000000L) {
            return String.format("%.4fh", timing / 3600000000000D);
        } else if (timing < 604800000000000L) {
            return String.format("%.5fd", timing / 86400000000000D);
        } else {
            return String.format("%.5fw", timing / 604800000000000D);
        }
    }
}
