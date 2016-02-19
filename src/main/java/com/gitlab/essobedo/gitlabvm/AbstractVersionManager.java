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
package com.gitlab.essobedo.gitlabvm;

import com.gitlab.essobedo.appma.exception.ApplicationException;
import com.gitlab.essobedo.appma.exception.TaskInterruptedException;
import com.gitlab.essobedo.appma.spi.Manageable;
import com.gitlab.essobedo.appma.spi.VersionManager;
import com.gitlab.essobedo.appma.task.Task;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Nicolas Filotto (nicolas.filotto@gmail.com)
 * @version $Id$
 * @since 1.0
 */
public abstract class AbstractVersionManager implements VersionManager {
    /**
     * The logger of the class.
     */
    private static final Logger LOG = Logger.getLogger(AbstractVersionManager.class.getName());

    /**
     * The default file size in case there is no way to get the size of the file to download.
     */
    private static final int DEFAULT_FILE_SIZE = 1024;

    private volatile Repository repository;
    private final String endpoint;

    public AbstractVersionManager() {
        this("https://gitlab.com");
    }

    AbstractVersionManager(final String endpoint) {
        this.endpoint = endpoint;
    }

    private Repository getRepository(final Manageable manageable) throws ApplicationException {
        if (repository == null) {
            synchronized (this) {
                if (repository == null) {
                    this.repository = new Repository(endpoint, createConfiguration(manageable));
                }
            }
        }
        return repository;
    }

    protected abstract ConnectionConfiguration createConfiguration(Manageable manageable) throws ApplicationException;

    @Override
    public Task<String> check(final Manageable manageable) throws ApplicationException {
        return new Task<String>(Localization.getMessage("check")) {
            @Override
            public boolean cancelable() {
                return true;
            }

            @Override
            public String execute() throws ApplicationException, TaskInterruptedException {
                final Repository repository = getRepository(manageable);
                updateMessage(Localization.getMessage("checking"));
                updateProgress(0, 1);
                final SortedSet<String> versions = repository.getVersions();
                if (isCanceled()) {
                    throw new TaskInterruptedException();
                }
                final String last = versions.last();
                updateProgress(1, 1);
                if (!manageable.version().equals(last)) {
                    return last;
                }
                return null;
            }
        };
    }

    @Override
    public Task<Void> store(final Manageable manageable, final OutputStream outputStream) throws ApplicationException {
        return new Task<Void>(Localization.getMessage("store")) {
            @Override
            public boolean cancelable() {
                return true;
            }

            @Override
            public Void execute() throws ApplicationException, TaskInterruptedException {
                final Repository repository = getRepository(manageable);
                updateMessage(Localization.getMessage("finding"));
                updateProgress(0, 1);
                final SortedSet<String> versions = repository.getVersions();
                if (isCanceled()) {
                    throw new TaskInterruptedException();
                }
                updateProgress(1, 1);
                updateMessage(Localization.getMessage("downloading"));
                try (final InputStream inputStream =  repository.getPatch(versions.last())) {
                    int size = 0;
                    try {
                        size = inputStream.available();
                    } catch (IOException e) {
                        if (LOG.isLoggable(Level.WARNING)) {
                            LOG.log(Level.WARNING, "Could not get the total amount of size to download");
                        }
                    }
                    final boolean unknownSize;
                    if (size > 0) {
                        unknownSize = false;
                        updateProgress(0, size);
                    } else {
                        unknownSize = true;
                        updateProgress(0, DEFAULT_FILE_SIZE);
                    }
                    final byte[] buffer = new byte[1024];
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
                        if (isCanceled()) {
                            throw new TaskInterruptedException();
                        }
                    }
                    if (unknownSize) {
                        updateProgress(DEFAULT_FILE_SIZE, DEFAULT_FILE_SIZE);
                    } else {
                        updateProgress(size, size);
                    }
                } catch (IOException e) {
                    throw new ApplicationException("Could not download the last version", e);
                }
                return null;
            }
        };
    }
}
