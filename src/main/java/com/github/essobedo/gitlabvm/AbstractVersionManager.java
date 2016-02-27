/*
 * Copyright (C) 2016 essobedo.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.github.essobedo.gitlabvm;

import com.github.essobedo.appma.exception.ApplicationException;
import com.github.essobedo.appma.exception.TaskInterruptedException;
import com.github.essobedo.appma.spi.Manageable;
import com.github.essobedo.appma.spi.VersionManager;
import com.github.essobedo.appma.task.Task;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Simple implementation of a {@link VersionManager} based on the gitlab API. It assumes that
 * you have a specific branch in your (private) gitlab project in which you have one directory
 * per version knowing that the name of the directories is the corresponding version id.
 * In each directory you have a patch file that is used to upgrade the application.
 *
 * <p>This implementation relies on the gitlab API to check if there is a directory in the branch
 * dedicated to the releases whose name comes after the current version id, if so it means that
 * there is a new version available and will retrieve the patch file available in the directory.
 *
 * @author Nicolas Filotto (nicolas.filotto@gmail.com)
 * @version $Id$
 * @since 1.0
 * @param <T> The type of application that this version manager supports.
 */
public abstract class AbstractVersionManager<T extends Manageable> implements VersionManager<T> {
    /**
     * The logger of the class.
     */
    private static final Logger LOG = Logger.getLogger(AbstractVersionManager.class.getName());

    /**
     * The default file size in case there is no way to get the size of the file to download.
     */
    private static final int DEFAULT_FILE_SIZE = 4096;

    /**
     * The gitlab repository.
     */
    private volatile Repository repository;

    /**
     * The end point of the gitlab repository.
     */
    private final String endpoint;

    /**
     * Constructs an {@code AbstractVersionManager} with the real end point to gitlab.
     */
    public AbstractVersionManager() {
        this("https://gitlab.com");
    }

    /**
     * Constructs an {@code AbstractVersionManager} with the specified end point to gitlab.
     * @param endpoint the end point to gitlab to use.
     */
    AbstractVersionManager(final String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Gives the gitlab repository that has been lazily created.
     * @param application the application for which we want to access to the corresponding gitlab repository.
     * @return the current gitlab repository.
     * @throws ApplicationException if the gitlab repository could not be created.
     */
    private Repository getRepository(final T application) throws ApplicationException {
        if (repository == null) {
            synchronized (this) {
                if (repository == null) {
                    this.repository = new Repository(endpoint, createConfiguration(application));
                }
            }
        }
        return repository;
    }

    /**
     * Creates the {@link ConnectionConfiguration} that will be used to access to the gitlab repository.
     * @param application the application for which we want to access to the corresponding gitlab repository.
     * @return the {@link ConnectionConfiguration} that will be used to access to the gitlab repository.
     * @throws ApplicationException if the {@link ConnectionConfiguration} could not be created.
     */
    protected abstract ConnectionConfiguration createConfiguration(T application) throws ApplicationException;

    @Override
    public Task<String> check(final T application) throws ApplicationException {
        return new CheckForUpdate(application);
    }

    @Override
    public Task<Void> store(final T application, final OutputStream outputStream) throws ApplicationException {
        return new StorePatch(application, outputStream);
    }

    /**
     * The inner class used to store the patch.
     */
    private class StorePatch extends Task<Void> {
        /**
         * The application for which we want to store the patch.
         */
        private final T application;
        /**
         * The stream in which it stores the content of the patch.
         */
        private final OutputStream outputStream;
        /**
         * Constructs a {@code StorePatch} with the specified application and output stream.
         *
         * @param application the application for which we do the task.
         * @param outputStream the output stream in which it stores the content of the patch.
         */
        protected StorePatch(final T application, final OutputStream outputStream) {
            super(Localization.getMessage("store"));
            this.application = application;
            this.outputStream = outputStream;
        }

        @Override
        public boolean cancelable() {
            return true;
        }

        @SuppressWarnings("PMD.PrematureDeclaration")
        @Override
        public Void execute() throws ApplicationException, TaskInterruptedException {
            final Repository repository = getRepository(application);
            updateMessage(Localization.getMessage("finding"));
            updateProgress(0, 1);
            final SortedSet<String> versions = repository.getVersions();
            if (isCanceled()) {
                throw new TaskInterruptedException();
            }
            updateProgress(1, 1);
            updateMessage(Localization.getMessage("downloading"));
            try (final InputStream inputStream =  repository.getPatch(versions.last())) {
                final int size = estimatePatchSize(inputStream);
                final boolean unknownSize = initDownloadingProgress(size);
                final byte[] buffer = new byte[4096];
                int length;
                int downloaded = 0;
                while ((length = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                    if (unknownSize) {
                        downloaded += (DEFAULT_FILE_SIZE - downloaded) / 100;
                        updateProgress(downloaded, DEFAULT_FILE_SIZE);
                    } else {
                        downloaded += length;
                        updateProgress(downloaded, size);
                    }
                    updateMessage(Localization.getMessage("downloaded", downloaded / 1024));
                    if (isCanceled()) {
                        throw new TaskInterruptedException();
                    }
                }
                endDownloadingProgress(size, unknownSize);
            } catch (IOException e) {
                throw new ApplicationException("Could not download the last version", e);
            }
            return null;
        }
        /**
         * Notifies that we have reached the end of the stream.
         * @param size the evaluated size of the patch.
         * @param unknownSize indicates whether the size of the patch could be evaluated.
         */
        private void endDownloadingProgress(final int size, final boolean unknownSize) {
            if (unknownSize) {
                updateProgress(DEFAULT_FILE_SIZE, DEFAULT_FILE_SIZE);
            } else {
                updateProgress(size, size);
            }
        }
        /**
         * Initializes the progress of the task according to the specified size.
         * @param size the estimated size of the patch.
         * @return {@code true} if the provided estimated {@code size} is {@code -1}, {@code false}
         * otherwise.
         */
        private boolean initDownloadingProgress(final int size) {
            final boolean unknownSize;
            if (size > 0) {
                unknownSize = false;
                updateProgress(0, size);
            } else {
                unknownSize = true;
                updateProgress(0, DEFAULT_FILE_SIZE);
            }
            return unknownSize;
        }
        /**
         * Estimates the size of the patch.
         * @param inputStream the content of the patch in stream.
         * @return The estimated size of the patch.
         */
        private int estimatePatchSize(final InputStream inputStream) {
            int size = 0;
            try {
                size = inputStream.available();
            } catch (IOException e) {
                if (LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING, "Could not get the total amount of size to download");
                }
            }
            return size;
        }
    }

    /**
     * The inner class allowing to check if a new version of the application exists.
     */
    private class CheckForUpdate extends Task<String> {
        /**
         * The application for which we want to check for an update.
         */
        private final T application;

        /**
         * Constructs a {@code CheckForUpdate} with the specified application.
         *
         * @param application the application for which we do the task.
         */
        protected CheckForUpdate(final T application) {
            super(Localization.getMessage("check"));
            this.application = application;
        }

        @Override
        public boolean cancelable() {
            return true;
        }

        @SuppressWarnings("PMD.PrematureDeclaration")
        @Override
        public String execute() throws ApplicationException, TaskInterruptedException {
            final Repository repository = getRepository(application);
            updateMessage(Localization.getMessage("checking"));
            final SortedSet<String> versions = repository.getVersions();
            if (isCanceled()) {
                throw new TaskInterruptedException();
            }
            final String last = versions.last();
            if (repository.versionComparator().compare(application.version(), last) < 0) {
                return last;
            }
            return null;
        }
    }
}
