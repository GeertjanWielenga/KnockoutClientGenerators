package org.netbeans.modules.web.knockout.rest.wizard;

import java.awt.Component;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openide.WizardDescriptor;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;


/**
 * @author ads
 *
 */
public class RestPanel implements WizardDescriptor.FinishablePanel<WizardDescriptor> 
{
    public static enum JsUi {
        NO_UI(NbBundle.getMessage(RestPanel.class, "TXT_NoUi")),                // NOI18N
        TABLESORTER(NbBundle.getMessage(RestPanel.class, "TXT_TablesorterUi")); // NOI18N
        
        private JsUi( String displayName ){
            myDisplayName = displayName;
        }
        
        @Override
        public String toString(){
            return myDisplayName;
        }
        private String myDisplayName;
    }
    
    public final static String FILE_NAME = "js-file-name";                // NOI18N
    public final static String ADD_BACKBONE = "backbone";                 // NOI18N
    public final static String EXISTED_BACKBONE ="existed-backbone";      // NOI18N
    public final static String EXISTED_UNDERSCORE ="existed-underscore";  // NOI18N
    public final static String EXISTED_JQUERY ="existed-jquery";          // NOI18N
    public final static String UI ="ui";                                  // NOI18N    
    
    
    public RestPanel(WizardDescriptor descriptor) {
        myWizard = descriptor;
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.Panel#addChangeListener(javax.swing.event.ChangeListener)
     */
    @Override
    public void addChangeListener( ChangeListener listener ) {
        myListeners.add( listener );
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.Panel#getComponent()
     */
    @Override
    public Component getComponent() {
        if ( myComponent == null ){
            myComponent = new RestPanelVisual(this);
        }
        return myComponent;
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.Panel#getHelp()
     */
    @Override
    public HelpCtx getHelp() {
        return HelpCtx.DEFAULT_HELP;
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.Panel#isValid()
     */
    @Override
    public boolean isValid() {
        return myComponent.valid(myWizard);
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.Panel#readSettings(java.lang.Object)
     */
    @Override
    public void readSettings( WizardDescriptor wizard ) {
        myWizard = wizard;
        myComponent.read(wizard);
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.Panel#removeChangeListener(javax.swing.event.ChangeListener)
     */
    @Override
    public void removeChangeListener( ChangeListener listener ) {
        myListeners.remove(listener);
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.Panel#storeSettings(java.lang.Object)
     */
    @Override
    public void storeSettings( WizardDescriptor wizard ) {
        myComponent.store(wizard);
    }
    
    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.FinishablePanel#isFinishPanel()
     */
    @Override
    public boolean isFinishPanel() {
        return true;
    }

    void fireChangeEvent(){
        ChangeEvent event = new ChangeEvent(this);
        for( ChangeListener listener : myListeners ){
            listener.stateChanged(event);
        }
    }
    
    public Node getRestNode(){
        return myComponent.getRestNode();
    }
    
    WizardDescriptor getDescriptor(){
        return myWizard;
    }
    
    private RestPanelVisual myComponent;
    private List<ChangeListener> myListeners = new CopyOnWriteArrayList<ChangeListener>();
    private WizardDescriptor myWizard;

}
