package org.netbeans.modules.web.knockout.rest.wizard.ui;

import java.util.Collection;
import java.util.Collections;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.util.Parameters;

/**
 * Clients uses this class to obtain a  web root for a file within a web-like project.
 *
 * @author marekfukala
 */
public final class ProjectWebRootQuery {

    private ProjectWebRootQuery() {
    }

    /**
     * Gets a web root for given file residing in a project.
     *
     * @param a file which you want to get a web root for
     * @return a found web root or null
     */
    public static FileObject getWebRoot(FileObject file) {
        if (file == null) {
            throw new NullPointerException("The file paramater cannot be null."); //NOI18N
        }

        Project project = FileOwnerQuery.getOwner(file);
        if (project != null) {
            ProjectWebRootProvider provider = project.getLookup().lookup(ProjectWebRootProvider.class);
            if (provider != null) {
                FileObject root = provider.getWebRoot(file);
                if(root == null) {
                    return null;
                }

                return root;
            }
        }
        return null;
    }

    /**
     * Gets all web roots for given project.
     *
     * @param project a project which you want to get web roots for
     * @return collection of web roots of the given project, can be empty but never {@code null}
     * @since 1.57
     */
    @NonNull
    public static Collection<FileObject> getWebRoots(@NonNull Project project) {
        Parameters.notNull("project", project); // NOI18N
        ProjectWebRootProvider provider = project.getLookup().lookup(ProjectWebRootProvider.class);
        if (provider == null) {
            return Collections.emptyList();
        }
        Collection<FileObject> webRoots = provider.getWebRoots();
        assert webRoots != null : "WebRoots cannot be null in " + provider.getClass().getName();
        return webRoots;
    }

}
