/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.kogito.codegen.core.io;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.drools.io.ByteArrayResource;
import org.drools.io.FileSystemResource;
import org.drools.io.InternalResource;
import org.kie.api.io.Resource;
import org.kie.kogito.codegen.api.io.CollectedResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.drools.util.IoUtils.readBytesFromInputStream;
import static org.kie.api.io.ResourceType.determineResourceType;

public class CollectedResourceProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectedResourceProducer.class);
    /**
     * Property that controls whether Kogito Codegen should ignore hidden files. Defaults to true.
     */
    public static final String IGNORE_HIDDEN_FILES_PROP = "kogito.codegen.ignoreHiddenFiles";
    /**
     * Whether to consider a file. It checks for the {@link #IGNORE_HIDDEN_FILES_PROP} property and if the file is hidden or not.
     */
    private static final Predicate<File> CONSIDER_FILE = p -> {
        if (!shouldIgnoreHiddenFiles()) {
            return true;
        }
        return !p.isHidden();
    };

    private CollectedResourceProducer() {
        // utility class
    }

    /**
     * Returns a collection of CollectedResource from the given paths.
     * If a path is a jar, then walks inside the jar.
     */
    public static Collection<CollectedResource> fromPaths(Path... paths) {
        Collection<CollectedResource> resources = new ArrayList<>();

        for (Path path : paths) {
            if (path.toFile().isDirectory()) {
                Collection<CollectedResource> res = fromDirectory(path);
                resources.addAll(res);
            } else if (path.getFileName().toString().endsWith(".jar") || path.getFileName().toString().endsWith(".jar.original")) {
                Collection<CollectedResource> res = fromJarFile(path);
                resources.addAll(res);
            } else if (!path.toFile().exists()) {
                LOGGER.debug("Skipping '{}' because doesn't exist", path);
            } else {
                throw new IllegalArgumentException("Expected directory or archive, file given: " + path);
            }
        }

        return resources;
    }

    /**
     * Returns a collection of CollectedResource from the given jar file.
     */
    public static Collection<CollectedResource> fromJarFile(Path jarPath) {
        Collection<CollectedResource> resources = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(jarPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                InternalResource resource = new ByteArrayResource(readBytesFromInputStream(zipFile.getInputStream(entry)));
                resource.setSourcePath(entry.getName());
                resources.add(toCollectedResource(jarPath, entry.getName(), resource));
            }
            return resources;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns a collection of CollectedResource from the given directory.
     */
    public static Collection<CollectedResource> fromDirectory(Path path) {
        Collection<CollectedResource> resources = new ArrayList<>();
        try {
            Files.walkFileTree(path, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new CollectResourcesVisitor(path, resources));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return resources;
    }

    /**
     * Returns a collection of CollectedResource from the given files
     */
    public static Collection<CollectedResource> fromFiles(Path basePath, File... files) {
        Collection<CollectedResource> resources = new ArrayList<>();
        if (shouldIgnoreHiddenFiles() && basePath.toFile().isHidden()) {
            LOGGER.debug("Skipping directory because it's hidden: {}. You can disable this option by setting {} property to 'false'.", basePath, IGNORE_HIDDEN_FILES_PROP);
            return resources;
        }
        try (Stream<File> paths = Arrays.stream(files)) {
            paths.filter(CONSIDER_FILE.and(File::isFile))
                    .map(f -> toCollectedResource(basePath, f))
                    .forEach(resources::add);
        }
        return resources;
    }

    private static CollectedResource toCollectedResource(Path basePath, File file) {
        Resource resource = new FileSystemResource(file);
        return toCollectedResource(basePath, file.getName(), resource);
    }

    private static CollectedResource toCollectedResource(Path basePath, String resourceName, Resource resource) {
        resource.setResourceType(determineResourceType(resourceName));
        return new CollectedResource(basePath, resource);
    }

    private static boolean shouldIgnoreHiddenFiles() {
        return Boolean.parseBoolean(System.getProperty(IGNORE_HIDDEN_FILES_PROP, "true"));
    }

    private static class CollectResourcesVisitor extends SimpleFileVisitor<Path> {
        private final Collection<CollectedResource> resources;
        private final Path initialPath;

        public CollectResourcesVisitor(Path initialPath, Collection<CollectedResource> resources) {
            this.resources = resources;
            this.initialPath = initialPath;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (shouldIgnoreHiddenFiles() && Files.isHidden(dir)) {
                LOGGER.debug("Skipping directory because it's hidden: {}. You can disable this option by setting {} property to 'false'.", dir, IGNORE_HIDDEN_FILES_PROP);
                return FileVisitResult.SKIP_SUBTREE;
            }
            return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
            if (shouldIgnoreHiddenFiles() && Files.isHidden(path)) {
                LOGGER.debug("Skipping file because it's hidden: {}. You can disable this option by setting {} property to 'false'.", path, IGNORE_HIDDEN_FILES_PROP);
            } else {
                resources.add(toCollectedResource(initialPath, path.toFile()));
            }

            return super.visitFile(path, attrs);
        }
    }
}
