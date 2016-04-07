package org.netbeans.modules.web.knockout.rest.wizard.ui;

import java.util.Collection;
import org.netbeans.api.annotations.common.NonNull;
import org.openide.filesystems.FileObject;

/**
 * Provides an ability to get the web root folder for a file
 * within web-like project.
 *
 * Instance of this interface must be registered into project's lookup
 *
 * @author marekfukala
 */
public interface ProjectWebRootProvider {

    /**
     * Finds a web root for a file.
     *
     * @param file The file you wish to find a web root for.
     * @return A web root containing the searched file. The returned web root
     * must contain the searched file. Null is returned if no web root find for
     * the file.
     */
    public FileObject getWebRoot(FileObject file);

    /**
     * Finds all web roots for a project.
     * @return collection of web roots of the given project, can be empty but never {@code null}.
     * @since 1.57
     */
    @NonNull
    Collection<FileObject> getWebRoots();

}
