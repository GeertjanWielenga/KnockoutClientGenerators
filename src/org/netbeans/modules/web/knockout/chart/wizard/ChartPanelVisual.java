package org.netbeans.modules.web.knockout.chart.wizard;

import javax.swing.JPanel;
import org.netbeans.modules.websvc.rest.client.RESTExplorerPanel;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

public final class ChartPanelVisual extends JPanel  {

    private static final String UNDERSCORE = "underscore";          // NOI18N
    private static final String UNDERSCORE_JS_ = UNDERSCORE+".js-"; // NOI18N
    private static final String JQUERY_JS = "jquery";               // NOI18N
    private static final String JQUERY_JS_ = JQUERY_JS+'-';

    private static final String BACKBONE = "backbone";              // NOI18N
    private static final String BACKBONE_JS_ = BACKBONE+".js-";     // NOI18N

    private static String REST_CLIENT = "RestClient";               // NOI18N
    private static String JS          = ".js";                      // NOI18N

    public ChartPanelVisual(ChartPanel panel) {
        myPanel = panel;
        initComponents();
        String jsName = suggestJsName(panel.getDescriptor());
        Templates.setTargetName(panel.getDescriptor(), jsName);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, "Chart Type:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addContainerGap(421, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addContainerGap(146, Short.MAX_VALUE))
        );

        getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(ChartPanelVisual.class, "LBL_RestSource")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    private void browseProjectServices() {
        RESTExplorerPanel panel = new RESTExplorerPanel();
        DialogDescriptor descriptor = new DialogDescriptor(panel,
                NbBundle.getMessage(ChartPanelVisual.class,"TTL_RESTResources")); //NOI18N
        panel.setDescriptor(descriptor);
        if (DialogDisplayer.getDefault().notify(descriptor).equals(NotifyDescriptor.OK_OPTION)) {
            myRestNode = panel.getSelectedService();
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel jLabel1;
    // End of variables declaration//GEN-END:variables

    void store(WizardDescriptor descriptor) {
        descriptor.putProperty(ChartPanel.EXISTED_BACKBONE, myBackbone);
        descriptor.putProperty(ChartPanel.EXISTED_UNDERSCORE, myUnderscore);
        descriptor.putProperty(ChartPanel.EXISTED_JQUERY, myJQuery);
    }

    void read(WizardDescriptor wizardDescriptor) {
        myBackbone = null;
        Object fileName = wizardDescriptor.getProperty(ChartPanel.FILE_NAME);
        String jsName=null;
        if ( fileName == null ){
            jsName = suggestJsName(wizardDescriptor);
        }
        else {
            jsName = fileName.toString();
        }

        Templates.setTargetName(wizardDescriptor, jsName);
    }

    private String suggestJsName(  WizardDescriptor descriptor ) {
        FileObject targetFolder = Templates.getTargetFolder(descriptor);

        String suggestName = REST_CLIENT;
        if ( targetFolder == null ){
            return suggestName;
        }
        FileObject restClient = null;
        int count =0;
        String result = null;
        while( true ){
            restClient = targetFolder.getFileObject(suggestName+JS);
            if ( restClient == null){
                result = suggestName;
                break;
            }
            else {
                count++;
                suggestName = REST_CLIENT+count;
            }
        }
        return result;
    }

    boolean valid(final WizardDescriptor wizardDescriptor) {
        if ( wizardDescriptor == null ){
            return true;
        }
        wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, null);
        if ( getRestNode() == null ){
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE,
                    NbBundle.getMessage(ChartPanelVisual.class, "ERR_NoRestResource"));    // NOI18N
            return false;
        }
        String targetName = Templates.getTargetName(wizardDescriptor);
        FileObject targetFolder = Templates.getTargetFolder(wizardDescriptor);
        if ( targetFolder!= null && targetFolder.getFileObject(targetName)!=null){
            wizardDescriptor.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE,
                    NbBundle.getMessage(ChartPanelVisual.class, "ERR_ExistedFile",targetName));    // NOI18N
            return false;
        }
        return true;
    }

    Node getRestNode(){
        return myRestNode;
    }

    private ChartPanel myPanel;
    private Node myRestNode;
    private FileObject myBackbone;
    private FileObject myUnderscore;
    private FileObject myJQuery;
}
