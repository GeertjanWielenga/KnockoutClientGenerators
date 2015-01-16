package org.netbeans.modules.web.knockout.rest.wizard;

import java.awt.Component;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openide.WizardDescriptor;
import org.openide.WizardDescriptor.Panel;
import org.openide.util.HelpCtx;


/**
 * @author ads
 *
 */
public class HtmlPanel implements Panel<WizardDescriptor> {
    
    public static final String HTML_FILE = "html-file";     // NOI18N
    
    final static String PROP_DOCUMENT_BASE ="document_base"; // NOI18N
    final static String PUBLIC_HTML ="public_html"; // NOI18N
    
    HtmlPanel(WizardDescriptor descriptor){
        myDescriptor = descriptor;
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
            myComponent = new HtmlPanelVisual( this );
        }
        return myComponent;
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.Panel#getHelp()
     */
    @Override
    public HelpCtx getHelp() {
        return new HelpCtx(JSClientIterator.HELP_ID);
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.Panel#isValid()
     */
    @Override
    public boolean isValid() {
        if ( myComponent != null ){
            return myComponent.valid();
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.Panel#readSettings(java.lang.Object)
     */
    @Override
    public void readSettings( WizardDescriptor descriptor ) {
        myComponent.read( descriptor );
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.Panel#removeChangeListener(javax.swing.event.ChangeListener)
     */
    @Override
    public void removeChangeListener( ChangeListener listener ) {
        myListeners.remove( listener );
    }

    /* (non-Javadoc)
     * @see org.openide.WizardDescriptor.Panel#storeSettings(java.lang.Object)
     */
    @Override
    public void storeSettings( WizardDescriptor descriptor ) {
        myComponent.store( descriptor );
    }
    
    WizardDescriptor getDescriptor(){
        return myDescriptor;
    }
    
    void fireChangeEvent(){
        for( ChangeListener listener : myListeners ){
            listener.stateChanged( new ChangeEvent(this));
        }
    }
    
    
    private final List<ChangeListener> myListeners = new CopyOnWriteArrayList<ChangeListener>();
    private HtmlPanelVisual myComponent;
    private final WizardDescriptor myDescriptor;
}
