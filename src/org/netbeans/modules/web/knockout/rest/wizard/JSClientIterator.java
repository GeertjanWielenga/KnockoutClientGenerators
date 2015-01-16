package org.netbeans.modules.web.knockout.rest.wizard;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.event.ChangeListener;

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.api.project.libraries.Library;
import org.netbeans.modules.web.api.webmodule.WebModule;
import org.netbeans.modules.web.knockout.rest.wizard.RestPanel.JsUi;
import org.netbeans.modules.web.clientproject.api.MissingLibResourceException;
import org.netbeans.modules.web.clientproject.api.WebClientLibraryManager;
import org.netbeans.modules.web.clientproject.api.WebClientProjectConstants;
import org.netbeans.modules.websvc.rest.model.api.RestServiceDescription;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.WizardDescriptor.Panel;
import org.openide.WizardDescriptor.ProgressInstantiatingIterator;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;



/**
 * @author ads
 *
 */
public class JSClientIterator implements ProgressInstantiatingIterator<WizardDescriptor>{
    
    public static final String HELP_ID = "js.client.rest";               // NOI18N
    
    private static final Logger LOGGER = Logger.getLogger(JSClientIterator.class.getName());

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.Iterator#addChangeListener(javax.swing.event.ChangeListener)
     */
    @Override
    public void addChangeListener( ChangeListener listener ) {
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.Iterator#current()
     */
    @Override
    public Panel<WizardDescriptor> current() {
        return myPanels[myIndex];
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.Iterator#hasNext()
     */
    @Override
    public boolean hasNext() {
        return myIndex<myPanels.length-1;
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.Iterator#hasPrevious()
     */
    @Override
    public boolean hasPrevious() {
        return myIndex >0 ;
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.Iterator#name()
     */
    @Override
    public String name() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.Iterator#nextPanel()
     */
    @Override
    public void nextPanel() {
        if (! hasNext()) {
            throw new NoSuchElementException();
        }
        myIndex++;
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.Iterator#previousPanel()
     */
    @Override
    public void previousPanel() {
        if (!hasPrevious()) {
            throw new NoSuchElementException();
        }
        myIndex--;        
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.Iterator#removeChangeListener(javax.swing.event.ChangeListener)
     */
    @Override
    public void removeChangeListener( ChangeListener listener ) {
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.InstantiatingIterator#initialize(org.openide.WizardDescriptor)
     */
    @Override
    public void initialize( WizardDescriptor descriptor ) {
        myWizard = descriptor;
        myRestPanel = new RestPanel( descriptor );
        Project project = Templates.getProject( descriptor );
        Sources sources = ProjectUtils.getSources(project);
        
        String htmlPanelName = NbBundle.getMessage(JSClientIterator.class, 
                    "TXT_HtmlFile");        // NOI18N
        Panel panel = new HtmlPanel( descriptor );
        panel.getComponent().setName(htmlPanelName);
        FileObject projectDirectory = project.getProjectDirectory();
        WebModule webModule = WebModule.getWebModule(projectDirectory);
        if (webModule != null) {
            FileObject documentBase = webModule.getDocumentBase();
            if (documentBase != null) {
                Templates.setTargetFolder(myWizard, documentBase);
                myWizard.putProperty(HtmlPanel.PROP_DOCUMENT_BASE, documentBase);
            }
        } else {
            FileObject publicHtml = getRootFolder(project);
            if (publicHtml != null) {
                Templates.setTargetFolder(myWizard, publicHtml);
            }
        }
        
        myPanels = new WizardDescriptor.Panel[]{new FinishPanelDelegate(
                Templates.buildSimpleTargetChooser(project, 
                sources.getSourceGroups(Sources.TYPE_GENERIC)).
                    bottomPanel(myRestPanel).create()), panel};
        setSteps();
    }
    
    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.InstantiatingIterator#instantiate()
     */
    @Override
    public Set instantiate(ProgressHandle handle) throws IOException {
        handle.start();
        Project project = Templates.getProject(myWizard);
        
        Node restNode = myRestPanel.getRestNode();
        RestServiceDescription description = restNode.getLookup().lookup(
                RestServiceDescription.class);
        Boolean addBackbone = (Boolean)myWizard.getProperty(RestPanel.ADD_BACKBONE);
        FileObject existedBackbone = (FileObject)myWizard.getProperty(
                RestPanel.EXISTED_BACKBONE);
        FileObject existedJQuery = (FileObject)myWizard.getProperty(
                RestPanel.EXISTED_JQUERY);
        
        JsUi ui = (JsUi)myWizard.getProperty(RestPanel.UI);

        if ( existedBackbone == null ) {
            if ( addBackbone!=null && addBackbone ) {
                FileObject documentRoot = (FileObject)myWizard.getProperty(HtmlPanel.PROP_DOCUMENT_BASE);
                FileObject libs = FileUtil.createFolder((documentRoot == null ? getRootFolder(project) : documentRoot),
                        WebClientLibraryManager.LIBS);
                handle.progress(NbBundle.getMessage(JSClientGenerator.class, 
                        "TXT_CreateLibs"));                                 // NOI18N
                existedBackbone = addLibrary( libs , "knockout");        // NOI18N
                if ( existedJQuery == null ){
                    existedJQuery = addLibrary(libs, "jquery");  // NOI18N
                }
            }
        }
        
        FileObject targetFolder = Templates.getTargetFolder(myWizard);
        String targetName = Templates.getTargetName(myWizard);
        
        handle.progress(NbBundle.getMessage(JSClientGenerator.class, 
                "TXT_GenerateModel"));   // NOI18N
        JSClientGenerator generator = JSClientGenerator.create( description, ui );
        Map<String,String> map = generator.generate() ;
        
        FileObject templateFO = FileUtil.getConfigFile("Templates/ClientSide/korest.js");  //NOI18N
        DataObject templateDO = DataObject.find(templateFO);
        DataFolder dataFolder = DataFolder.findFolder(targetFolder);
        DataObject createdFile = templateDO.createFromTemplate(dataFolder, 
                targetName ,map);
        
        FileObject jsFile = createdFile.getPrimaryFile();
        
        File htmlFile = (File)myWizard.getProperty(
                HtmlPanel.HTML_FILE);
        if ( htmlFile != null ){
            handle.progress(NbBundle.getMessage(JSClientGenerator.class, 
                    "TXT_GenerateHtml"));                         // NOI18N
            /*FileObject html = */createHtml( htmlFile , jsFile, existedBackbone , 
                     existedJQuery , map , generator );
        }

        handle.finish();
        // TODO: add html ?
        return Collections.singleton(jsFile);
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.InstantiatingIterator#instantiate()
     */
    @Override
    public Set instantiate() throws IOException {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.InstantiatingIterator#uninitialize(org.openide.WizardDescriptor)
     */
    @Override
    public void uninitialize( WizardDescriptor descriptor ) {
        myPanels = null;
    }
    
    static FileObject getRootFolder(Project project){
        SourceGroup[] groups =  ProjectUtils.getSources(project).getSourceGroups(
                WebClientProjectConstants.SOURCES_TYPE_HTML5);
        if ( groups!= null && groups.length >0 ){
            return groups[0].getRootFolder();
        }
        else {
            return project.getProjectDirectory();
        }
    }
    
    private FileObject createHtml( File htmlFile, FileObject appFile, 
            FileObject backbone , FileObject jQuery,
            Map<String,String> map, JSClientGenerator generator ) throws IOException 
    {
        File parentFile = htmlFile.getParentFile();
        parentFile.mkdirs();
        FileObject folder = FileUtil.toFileObject(FileUtil.normalizeFile(parentFile));
        FileObject templateFO = FileUtil.getConfigFile("Templates/ClientSide/kojs.html");  //NOI18N
        DataObject templateDO = DataObject.find(templateFO);
        DataFolder dataFolder = DataFolder.findFolder(folder);
        String name = htmlFile.getName();
        if ( name.endsWith( HtmlPanelVisual.HTML)){
            name = name.substring(0 , name.length()-HtmlPanelVisual.HTML.length());
        }
        
        StringBuilder builder = new StringBuilder();
        
        if ( jQuery == null ){
            builder.append("<script src='http://cdnjs.cloudflare.com/ajax/libs/jquery/2.1.1/jquery.min.js'></script>\n");// NOI18N
        }
        else {
            String relativePath = getRelativePath(folder, jQuery);
            builder.append("<script src='");// NOI18N
            builder.append(relativePath);
            builder.append("'></script>\n");  // NOI18N
        }
        if ( backbone == null ){
            builder.append("<script src='http://cdnjs.cloudflare.com/ajax/libs/knockout/3.2.0/knockout-min.js'></script>\n");// NOI18N
        }
        else {
            String relativePath = getRelativePath(folder, backbone);
            builder.append("<script src='");// NOI18N
            builder.append(relativePath);
            builder.append("'></script>\n");  // NOI18N
        }
        
//        if ( generator.hasUi() ){
////            builder.append("<script src='");                                // NOI18N
////            builder.append(JSClientGenerator.TABLESORTER_URL);
////            builder.append("js/jquery.tablesorter.min.js'></script>\n");    // NOI18N
////            builder.append("<script src='");                                // NOI18N
////            builder.append(JSClientGenerator.TABLESORTER_URL);
////            builder.append("addons/pager/jquery.tablesorter.pager.js'>");   // NOI18N
////            builder.append("</script>\n");                                  // NOI18N
//        }
        
        String relativePath = getRelativePath(folder, appFile );
        builder.append("<script src='");    // NOI18N
        builder.append(relativePath);
        builder.append("'></script>");      // NOI18N
        map.put("script", builder.toString());  // NOI18N
        
        return templateDO.createFromTemplate(dataFolder, 
                name, map).getPrimaryFile();
    }
    
    private String getRelativePath(FileObject folder, FileObject file){
        String relativePath = FileUtil.getRelativePath(folder, file);
        if ( relativePath != null ){
            return relativePath;
        }
        Project project = Templates.getProject(myWizard);
        FileObject rootFolder = getRootFolder(project);
        relativePath = FileUtil.getRelativePath(rootFolder, file);
        if ( relativePath == null ){
            rootFolder = project.getProjectDirectory();
            relativePath = FileUtil.getRelativePath(rootFolder, file);
        }
        int upCount = getNestingCount(rootFolder, folder);
        StringBuilder builder = new StringBuilder();
        for(int i=0; i<upCount ; i++){
            builder.append("../");                          // NOI18N
        }
        builder.append(relativePath);
        return builder.toString();
    }
    
    private int getNestingCount(FileObject parent , FileObject child){
        int count=0;
        FileObject currentParent = child;
        while( !parent.equals( currentParent)){
            count++;
            currentParent = currentParent.getParent();
        }
        return count;
    }
    
    private void setSteps() {
        Object contentData = myWizard.getProperty(WizardDescriptor.PROP_CONTENT_DATA);  
        if ( contentData instanceof String[] ){
            String steps[] = (String[])contentData;
            String newSteps[] = new String[ steps.length +1];
            System.arraycopy(steps, 0, newSteps, 0, 1);
            newSteps[newSteps.length-2]=NbBundle.getMessage(JSClientIterator.class, 
                "TXT_JsFile");        // NOI18N
            newSteps[newSteps.length-1]=NbBundle.getMessage(JSClientIterator.class, 
                    "TXT_HtmlFile");        // NOI18N
            for( int i=0; i<myPanels.length; i++ ){
                Panel panel = myPanels[i];
                JComponent component = (JComponent)panel.getComponent();
                component.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, newSteps);
                component.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, i);
            }
        }
    }
    
    private FileObject addLibrary(FileObject libs, String libName ) {
        Library backbone = WebClientLibraryManager.getDefault().findLibrary(libName,
                null);    // NOI18N
        if ( backbone == null ){
            return null;
        }
        try {
            List<FileObject> files = WebClientLibraryManager.getDefault().addLibraries(new Library[]{backbone},
                    libs, null);
            if ( !files.isEmpty() ){
                return files.get(0);
            }
            return null;
        }
        catch(IOException e ){
            return null;
        }
        catch(MissingLibResourceException e ){
            List<FileObject> files = e.getResources();
            if ( !files.isEmpty() ){
                return files.get(0);
            }
            return null;
        }
    }
    
    private WizardDescriptor myWizard;
    private RestPanel myRestPanel;
    private WizardDescriptor.Panel[] myPanels;
    private int myIndex;

}
